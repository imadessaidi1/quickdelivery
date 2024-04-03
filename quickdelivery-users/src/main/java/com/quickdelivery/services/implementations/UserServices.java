package com.quickdelivery.services.implementations;

import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.quickdelivery.abstarct.dto.DocumentDTO;
import com.quickdelivery.abstarct.dto.UserDTO;
import com.quickdelivery.abstarct.dto.VehicleDTO;
import com.quickdelivery.abstarct.entities.Document;
import com.quickdelivery.abstarct.entities.User;
import com.quickdelivery.abstarct.entities.Vehicle;
import com.quickdelivery.abstarct.helpers.FileHelper;
import com.quickdelivery.abstarct.helpers.GeoHelper;
import com.quickdelivery.abstarct.helpers.MailHelper;
import com.quickdelivery.abstarct.parameters.CHECK_STATUS;
import com.quickdelivery.abstarct.parameters.DOCUMENT_STATUS;
import com.quickdelivery.abstarct.parameters.DOCUMENT_TYPE;
import com.quickdelivery.abstarct.parameters.EMAIL_TEMPLATE_TYPE;
import com.quickdelivery.abstarct.repositories.Documents;
import com.quickdelivery.abstarct.repositories.Users;
import com.quickdelivery.services.interfaces.IUserServices;
import jakarta.mail.MessagingException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServices implements IUserServices {
    @Value("${mapquest.geocode.url.part1}")
    private String mapQuestURL1;
    @Value("${mapquest.geocode.url.part2}")
    private String mapQuestURL2;
    @Value("${mapquest.key}")
    private String mapQUestKey;
    @Value("${user.docs.directory}")
    private String userDocPath;
    @Value("${mapquest.key}")
    private String userVehiclePath;
    @Value("${email.validation.link}")
    private String emailValidationLink;
    @Value("${email.userUpdate.link}")
    private String userUpdateLink;
    @Autowired
    private Users users;
    @Autowired
    private Documents documents;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private GeoApiContext geoApiContext;
    @Autowired
    private ResourceBundleMessageSource messageSource;
    @Autowired
    @Qualifier("myTemplateResolver")
    private ITemplateResolver templateResolver;
    @Override
    public UserDTO createNewUser(UserDTO user, VehicleDTO vehicleDTO, MultiValueMap<String, MultipartFile> filesMap, Locale locale) {
        User userEntity = modelMapper.map(user,User.class);
        user.getPersonalAddress().stream().forEach(address -> {
            try {
                GeoHelper.AddressGeoCoding(geoApiContext, address);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ApiException e) {
                throw new RuntimeException(e);
            }
        });
        userEntity.getPersonalAddress().stream().forEach(address -> address.setResidents(userEntity));
        Vehicle vehicle = modelMapper.map(vehicleDTO, Vehicle.class);
        vehicle.setUser(userEntity);
        userEntity.getVehicles().add(vehicle);
        String filesPath = userDocPath+(user.getEmailAddress().replace('.','_'));
        filesMap.entrySet().stream()
                .forEach(entry -> {
                    String fileName = entry.getKey();
                    MultipartFile file = entry.getValue().get(0);
                    String currentFileName = file.getOriginalFilename();
                    String newFileName = fileName+currentFileName.substring(currentFileName.lastIndexOf('.'));
                    Document document = new Document();
                    document.setType(DOCUMENT_TYPE.valueOf(fileName));
                    document.setDocURL(filesPath+"\\"+newFileName);
                    document.setUser(userEntity);
                    userEntity.getDocument().add(document);
                });
        users.save(userEntity);
        FileHelper.saveFilesInParallel(filesMap, userDocPath, user.getEmailAddress());
        userEntity.getPersonalAddress().stream().forEach(address -> address.getResidents().setPersonalAddress(new HashSet<>()));
        user.setId(userEntity.getId());
        user.setVersion(userEntity.getVersion());
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        executorService.submit(() -> {
            sendUserAccountCreationEmail(user, userEntity, locale);
        });
        return user;
    }

    @Override
    public UserDTO findByID(Long id) {
        return modelMapper.map(users.findById(id).get(),UserDTO.class);
    }

    @Override
    public UserDTO userValidation(UserDTO user, Locale locale) {
        User userFromDB = users.findById(user.getId()).get();
        userFromDB.setActiveAccount(user.getActiveAccount());
        Set<Document> documentListFromFront = new HashSet<>();
        user.getDocument().keySet().stream().forEach(documentType -> {
            Document document = modelMapper.map(user.getDocument().get(documentType), Document.class);
            document.setUser(userFromDB);
            documentListFromFront.add(document);
        });
        userFromDB.setDocument(documentListFromFront);
        users.save(userFromDB);
        List<Document> rejectedDocuments = userFromDB.getDocument().stream()
                .filter(document -> document.getDocumentStatus() == DOCUMENT_STATUS.REJECTED)
                .collect(Collectors.toList());
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        if(!rejectedDocuments.isEmpty()) {
            executorService.submit(() -> {
                sendUserDocumentsUpdateRequest(user, userFromDB, rejectedDocuments, locale);
            });
        }
        return modelMapper.map(userFromDB,UserDTO.class);
    }

    @Override
    public void deleteUSer(UserDTO user) {
         users.delete(modelMapper.map(user,User.class));
    }

    @Override
    public CHECK_STATUS validateUserEmail(Long id) {
        User user = users.findById(id).get();
        user.setActiveAccount(true);
        user.setEmailAddressValidation(true);
        users.save(user);
        return CHECK_STATUS.OK;
    }

    @Override
    public UserDTO findByEmail(String email) {
        User user = users.findByEmail(email);
        if(user != null) {
            UserDTO userDTO = modelMapper.map(user, UserDTO.class);
            userDTO.setAddressAuto(userDTO.getPersonalAddress().get(0).toString());
            userDTO.setEmailAddressConfirmation(userDTO.getEmailAddress());
            userDTO.setPhoneConfirmation(userDTO.getPhone());
            userDTO.setPasswordConfirmation(userDTO.getPassword());
            user.getDocument().stream().forEach(document -> {
                DocumentDTO documentDTO = modelMapper.map(document, DocumentDTO.class);
                /*try {
                    documentDTO.setData(Files.readAllBytes(Paths.get(document.getDocURL())));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }*/
                userDTO.getDocument().put(document.getType(), documentDTO);
            });
            return userDTO;
        }else
            return null;
    }

    @Override
    public List<UserDTO> findUsersForValidation() {
        List<User> userList = users.findUsersForValidation();
        List<UserDTO> userDTOList = new ArrayList<>();
        userList.stream().forEach(user -> {
            UserDTO userDTO = modelMapper.map(user, UserDTO.class);
            userDTO.setAddressAuto(userDTO.getPersonalAddress().get(0).toString());
            user.getDocument().stream().forEach(document -> {
                DocumentDTO documentDTO = modelMapper.map(document, DocumentDTO.class);
                try {
                    documentDTO.setData(Files.readAllBytes(Paths.get(document.getDocURL())));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                userDTO.getDocument().put(document.getType(), documentDTO);
            });
            userDTOList.add(userDTO);
        });
        return userDTOList;
    }

    private void sendUserAccountCreationEmail(UserDTO user, User userEntity, Locale locale){
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("recipientName", user.getPersonalAddress().get(0).getFirstName()+" "+user.getPersonalAddress().get(0).getLastName());
        templateModel.put("validationLink", emailValidationLink+userEntity.getId());
        try {
            MailHelper.sendMessageUsingThymeleafTemplate(messageSource, templateResolver, userEntity.getEmailAddress(),messageSource.getMessage("email.subject.uservalidation", null, locale),templateModel,
                    locale, EMAIL_TEMPLATE_TYPE.NEW_DELIVERYPERSON_VALIDATION.getType(), null);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendUserDocumentsUpdateRequest(UserDTO user, User userEntity, List<Document> rejectedDocuments, Locale locale){
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("recipientName", user.getPersonalAddress().get(0).getFirstName()+" "+user.getPersonalAddress().get(0).getLastName());
        templateModel.put("updateLink", userUpdateLink+user.getEmailAddress());
        try {
            MailHelper.sendMessageUsingThymeleafTemplate(messageSource, templateResolver, userEntity.getEmailAddress(),messageSource.getMessage("email.subject.userUpdateDocsRequest", null, locale),templateModel,
                    locale, EMAIL_TEMPLATE_TYPE.DELIVERYPERSON_DOCUPDATE_REQUEST.getType(), null);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
