package com.quickdelivery.services.implementations;

import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.quickdelivery.abstarct.dto.UserDTO;
import com.quickdelivery.abstarct.dto.VehicleDTO;
import com.quickdelivery.abstarct.entities.Document;
import com.quickdelivery.abstarct.entities.User;
import com.quickdelivery.abstarct.entities.Vehicle;
import com.quickdelivery.abstarct.helpers.FileHelper;
import com.quickdelivery.abstarct.helpers.GeoHelper;
import com.quickdelivery.abstarct.helpers.MailHelper;
import com.quickdelivery.abstarct.parameters.CHECK_STATUS;
import com.quickdelivery.abstarct.parameters.DOCUMENT_TYPE;
import com.quickdelivery.abstarct.repositories.Documents;
import com.quickdelivery.abstarct.repositories.Users;
import com.quickdelivery.services.interfaces.IUserServices;
import jakarta.mail.MessagingException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    @Autowired
    private Users users;
    @Autowired
    private Documents documents;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private GeoApiContext geoApiContext;
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
    public UserDTO updateUser(UserDTO user) {
        User userEntity = users.save(modelMapper.map(user,User.class));
        return modelMapper.map(userEntity,UserDTO.class);
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
        User user = users.finByEmail(email);
        if(user != null)
            return modelMapper.map(user, UserDTO.class);
        else
            return null;
    }

    private void sendUserAccountCreationEmail(UserDTO user, User userEntity, Locale locale){
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("recipientName", user.getPersonalAddress().get(0).getFirstName()+" "+user.getPersonalAddress().get(0).getLastName());
        templateModel.put("validationLink", emailValidationLink+userEntity.getId());
        try {
            MailHelper.sendMessageUsingThymeleafTemplate(userEntity.getEmailAddress(),"Email Validation",templateModel, locale, "newdeliveryperson-mailvalidation-template-thymeleaf.html", null);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
