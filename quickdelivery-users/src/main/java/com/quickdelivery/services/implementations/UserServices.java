package com.quickdelivery.services.implementations;

import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.quickdelivery.PublicUrlResolver;
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
import com.quickdelivery.services.interfaces.IKeycloakProvisioningService;
import com.quickdelivery.services.interfaces.IUserServices;
import com.quickdelivery.security.UserUpdateTokenService;
import jakarta.mail.MessagingException;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;

@Service
@Transactional
public class UserServices implements IUserServices {
    private static final String DELIVERY_PERSON = "DELIVERY_PERSON";
    private static final Logger logger = LoggerFactory.getLogger(UserServices.class);

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
    @Value("${quickdelivery.frontend.base-url:}")
    private String frontendBaseUrl;
    @Value("${quickdelivery.gateway.base-url:}")
    private String gatewayBaseUrl;
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
    @Autowired
    private IKeycloakProvisioningService keycloakProvisioningService;
    @Autowired
    private UserUpdateTokenService userUpdateTokenService;
    @Override
    public UserDTO createNewUser(UserDTO user, VehicleDTO vehicleDTO, MultiValueMap<String, MultipartFile> filesMap, Locale locale) {
        try {
            boolean deliveryPerson = DELIVERY_PERSON.equalsIgnoreCase(user.getType());
            String rawPassword = user.getPassword();
            sanitizeAddresses(user, deliveryPerson);
            User userEntity = modelMapper.map(user,User.class);
            if (deliveryPerson) {
                geocodeAddressesIfPossible(user);
                userEntity.getPersonalAddress().forEach(address -> address.setResidents(userEntity));
                Vehicle vehicle = modelMapper.map(vehicleDTO, Vehicle.class);
                vehicle.setUser(userEntity);
                userEntity.getVehicles().add(vehicle);
            } else {
                userEntity.setPersonalAddress(new HashSet<>());
                userEntity.setVehicles(new HashSet<>());
            }
            String filesPath = buildUserFilesPath(user.getEmailAddress());
            filesMap.entrySet().stream()
                    .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty() && entry.getValue().get(0) != null)
                    .forEach(entry -> {
                        String fileName = entry.getKey();
                        MultipartFile file = entry.getValue().get(0);
                        String currentFileName = file.getOriginalFilename();
                        if (currentFileName == null || !currentFileName.contains(".")) {
                            return;
                        }
                        String newFileName = fileName+currentFileName.substring(currentFileName.lastIndexOf('.'));
                        Document document = new Document();
                        document.setType(DOCUMENT_TYPE.valueOf(fileName));
                        document.setDocURL(resolveDocumentPath(filesPath, newFileName));
                        document.setDocumentStatus(DOCUMENT_STATUS.PENDING_VALIDATION);
                        document.setUser(userEntity);
                        userEntity.getDocument().add(document);
                    });
            users.save(userEntity);
            keycloakProvisioningService.provisionUser(userEntity, rawPassword);
            if (!filesMap.isEmpty()) {
                FileHelper.saveFilesInParallel(filesMap, filesPath, false);
            }
            userEntity.getPersonalAddress().forEach(address -> address.getResidents().setPersonalAddress(new HashSet<>()));
            user.setId(userEntity.getId());
            user.setVersion(userEntity.getVersion());
            user.setPassword(null);
            user.setPasswordConfirmation(null);
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            executorService.submit(() -> {
                sendUserAccountCreationEmail(user, userEntity, locale);
            });
            return user;
        } catch (Exception exception) {
            logger.error("Unable to create user account for email={} type={}: {}", user.getEmailAddress(), user.getType(), exception.getMessage(), exception);
            throw exception;
        }
    }

    @Override
    public UserDTO updateNewUser(UserDTO user, VehicleDTO vehicleDTO, MultiValueMap<String, MultipartFile> filesMap, Locale locale) {
        try {
            boolean deliveryPerson = DELIVERY_PERSON.equalsIgnoreCase(user.getType());
            sanitizeAddresses(user, deliveryPerson);
            User userEntity = users.findById(user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            String filesPath = buildUserFilesPath(user.getEmailAddress());

            mergeUserProfile(userEntity, user);
            if (deliveryPerson) {
                geocodeAddressesIfPossible(user);
                mergeVehicle(userEntity, vehicleDTO);
            } else {
                userEntity.setPersonalAddress(new HashSet<>());
                userEntity.setVehicles(new HashSet<>());
            }

            applyUpdatedDocuments(userEntity, filesMap, filesPath);
            users.save(userEntity);
            if (!filesMap.isEmpty()) {
                FileHelper.saveFilesInParallel(filesMap, filesPath, true);
            }
            return modelMapper.map(userEntity, UserDTO.class);
        } catch (Exception exception) {
            logger.error("Unable to update user account for email={} type={}: {}", user.getEmailAddress(), user.getType(), exception.getMessage(), exception);
            throw exception;
        }
    }

    @Override
    public UserDTO updateUserByToken(String updateToken, UserDTO user, VehicleDTO vehicleDTO, MultiValueMap<String, MultipartFile> filesMap, Locale locale) {
        User existingUser = resolveUserByUpdateToken(updateToken);
        user.setId(existingUser.getId());
        user.setVersion(existingUser.getVersion());
        user.setActiveAccount(existingUser.getActiveAccount());
        user.setEmailAddressValidation(existingUser.getEmailAddressValidation());
        return updateNewUser(user, vehicleDTO, filesMap, locale);
    }

    @Override
    public UserDTO findByID(Long id) {
        User user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return modelMapper.map(user,UserDTO.class);
    }

    @Override
    public UserDTO findByUpdateToken(String updateToken) {
        User user = resolveUserByUpdateToken(updateToken);
        return findByEmail(user.getEmailAddress());
    }

    @Override
    public UserDTO userValidation(UserDTO user, Locale locale) {
        User userFromDB = users.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        userFromDB.setActiveAccount(user.getActiveAccount());
        userFromDB.setEmailAddressValidation(user.getEmailAddressValidation());
        Map<DOCUMENT_TYPE, Document> existingDocuments = userFromDB.getDocument().stream()
                .collect(Collectors.toMap(Document::getType, document -> document, (left, right) -> left, LinkedHashMap::new));
        user.getDocument().forEach((documentType, documentDTO) -> {
            Document document = existingDocuments.get(documentType);
            if (document == null) {
                document = new Document();
                document.setType(documentType);
                document.setUser(userFromDB);
                existingDocuments.put(documentType, document);
            }
            document.setDocumentStatus(documentDTO.getDocumentStatus());
            document.setReviewComment(normalizeReviewComment(documentDTO.getReviewComment(), documentDTO.getDocumentStatus()));
            document.setReviewedAt(new java.util.Date());
            document.setReviewedBy("ADMIN");
        });
        userFromDB.setDocument(new HashSet<>(existingDocuments.values()));
        users.save(userFromDB);
        keycloakProvisioningService.syncUserState(userFromDB);
        List<Document> rejectedDocuments = userFromDB.getDocument().stream()
                .filter(document -> document.getDocumentStatus() == DOCUMENT_STATUS.REJECTED)
                .collect(Collectors.toList());
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        if(!rejectedDocuments.isEmpty()) {
            executorService.submit(() -> {
                sendUserDocumentsUpdateRequest(user, userFromDB, rejectedDocuments, locale);
            });
        } else if (Boolean.TRUE.equals(userFromDB.getActiveAccount())) {
            executorService.submit(() -> {
                sendUserAccountApprovedEmail(user, userFromDB, locale);
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
        User user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setEmailAddressValidation(true);
        users.save(user);
        keycloakProvisioningService.syncUserState(user);
        return CHECK_STATUS.OK;
    }

    @Override
    public UserDTO findByEmail(String email) {
        User user = users.findByEmail(email);
        if(user != null) {
            UserDTO userDTO = modelMapper.map(user, UserDTO.class);
            if (userDTO.getPersonalAddress() != null && !userDTO.getPersonalAddress().isEmpty()) {
                userDTO.setAddressAuto(userDTO.getPersonalAddress().get(0).toString());
            }
            userDTO.setEmailAddressConfirmation(userDTO.getEmailAddress());
            userDTO.setPhoneConfirmation(userDTO.getPhone());
            userDTO.setPassword(null);
            userDTO.setPasswordConfirmation(null);
            attachDocumentsSafely(user, userDTO);
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
            if (userDTO.getPersonalAddress() != null && !userDTO.getPersonalAddress().isEmpty()) {
                userDTO.setAddressAuto(userDTO.getPersonalAddress().get(0).toString());
            }
            attachDocumentsSafely(user, userDTO);
            userDTO.setPassword(null);
            userDTO.setPasswordConfirmation(null);
            userDTOList.add(userDTO);
        });
        return userDTOList;
    }

    private void sendUserAccountCreationEmail(UserDTO user, User userEntity, Locale locale){
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("recipientName", resolveRecipientName(user));
        templateModel.put("validationLink", buildValidationLink(userEntity.getId()));
        try {
            MailHelper.sendMessageUsingThymeleafTemplate(messageSource, templateResolver, userEntity.getEmailAddress(),messageSource.getMessage("email.subject.uservalidation", null, locale),templateModel,
                    locale, EMAIL_TEMPLATE_TYPE.NEW_DELIVERYPERSON_VALIDATION.getType(), null);
        } catch (Exception e) {
            logger.warn("Unable to send validation email to {}: {}", userEntity.getEmailAddress(), e.getMessage(), e);
        }
    }

    private void sendUserDocumentsUpdateRequest(UserDTO user, User userEntity, List<Document> rejectedDocuments, Locale locale){
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("recipientName", resolveRecipientName(user));
        templateModel.put("updateLink", buildUserUpdateLink(userEntity));
        try {
            MailHelper.sendMessageUsingThymeleafTemplate(messageSource, templateResolver, userEntity.getEmailAddress(),messageSource.getMessage("email.subject.userUpdateDocsRequest", null, locale),templateModel,
                    locale, EMAIL_TEMPLATE_TYPE.DELIVERYPERSON_DOCUPDATE_REQUEST.getType(), null);
        } catch (Exception e) {
            logger.warn("Unable to send update-documents email to {}: {}", userEntity.getEmailAddress(), e.getMessage(), e);
        }
    }

    private void sendUserAccountApprovedEmail(UserDTO user, User userEntity, Locale locale){
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("recipientName", resolveRecipientName(user));
        templateModel.put("landingLink", resolveFrontendBaseUrl());
        try {
            MailHelper.sendMessageUsingThymeleafTemplate(messageSource, templateResolver, userEntity.getEmailAddress(),messageSource.getMessage("email.subject.userAccountApproved", null, locale),templateModel,
                    locale, EMAIL_TEMPLATE_TYPE.DELIVERYPERSON_ACCOUNT_APPROVED.getType(), null);
        } catch (Exception e) {
            logger.warn("Unable to send account-approved email to {}: {}", userEntity.getEmailAddress(), e.getMessage(), e);
        }
    }

    private void sanitizeAddresses(UserDTO user, boolean deliveryPerson) {
        if (user.getPersonalAddress() == null) {
            user.setPersonalAddress(new ArrayList<>());
            return;
        }

        user.setPersonalAddress(user.getPersonalAddress().stream()
                .filter(Objects::nonNull)
                .filter(address -> deliveryPerson || hasAddressContent(address))
                .collect(Collectors.toList()));
    }

    private void geocodeAddressesIfPossible(UserDTO user) {
        user.getPersonalAddress().forEach(address -> {
            try {
                GeoHelper.AddressGeoCoding(geoApiContext, address);
            } catch (IOException | InterruptedException | ApiException e) {
                logger.warn("Unable to geocode address for {}: {}. Continuing without coordinates.",
                        user.getEmailAddress(), e.getMessage());
            }
        });
    }

    private boolean hasAddressContent(com.quickdelivery.abstarct.dto.AddressDTO address) {
        return hasText(address.getLine1())
                || hasText(address.getTown())
                || hasText(address.getZipCode())
                || hasText(address.getCountry());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String resolveRecipientName(UserDTO user) {
        String firstName = Objects.toString(user.getFirstName(), "").trim();
        String lastName = Objects.toString(user.getLastName(), "").trim();
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        return Objects.toString(user.getEmailAddress(), "");
    }

    private String buildValidationLink(Long userId) {
        return resolveGatewayBaseUrl() + "/users/v1/validateEmail?id=" + userId;
    }

    private void attachDocumentsSafely(User user, UserDTO userDTO) {
        user.getDocument().forEach(document -> {
            DocumentDTO documentDTO = modelMapper.map(document, DocumentDTO.class);
            if (document.getDocURL() != null && !document.getDocURL().isBlank()) {
                try {
                    documentDTO.setData(readDocumentBytes(document, user.getEmailAddress()));
                } catch (IOException e) {
                    logger.warn("Document file missing or unreadable for user {} and type {} at {}",
                            user.getEmailAddress(), document.getType(), document.getDocURL(), e);
                }
            }
            userDTO.getDocument().put(document.getType(), documentDTO);
        });
    }

    private void mergeUserProfile(User userEntity, UserDTO user) {
        userEntity.setVersion(user.getVersion());
        userEntity.setType(user.getType());
        userEntity.setFirstName(user.getFirstName());
        userEntity.setLastName(user.getLastName());
        userEntity.setAge(user.getAge());
        userEntity.setBirthDate(user.getBirthDate());
        userEntity.setSex(user.getSex());
        userEntity.setEmailAddress(user.getEmailAddress());
        userEntity.setPhone(user.getPhone());
        userEntity.setPhoneValidation(user.getPhoneValidation());
        userEntity.setEmailAddressValidation(user.getEmailAddressValidation());
        userEntity.setActiveAccount(user.getActiveAccount());
        userEntity.setDeliveryMode(DELIVERY_PERSON.equalsIgnoreCase(user.getType()) ? user.getDeliveryMode() : null);
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            userEntity.setPassword(user.getPassword());
        }
        Set<com.quickdelivery.abstarct.entities.Address> addresses = user.getPersonalAddress().stream()
                .map(addressDTO -> {
                    com.quickdelivery.abstarct.entities.Address address = modelMapper.map(addressDTO, com.quickdelivery.abstarct.entities.Address.class);
                    address.setResidents(userEntity);
                    return address;
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
        userEntity.setPersonalAddress(addresses);
        Set<com.quickdelivery.abstarct.entities.Payment> payments = (user.getPaymentModes() == null ? Collections.<com.quickdelivery.abstarct.entities.Payment>emptySet() : user.getPaymentModes().values().stream()
                .map(paymentDTO -> modelMapper.map(paymentDTO, com.quickdelivery.abstarct.entities.Payment.class))
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        userEntity.setPayments(payments);
        userEntity.getPayments().forEach(payment -> payment.setHolderInApp(userEntity));
    }

    private void mergeVehicle(User userEntity, VehicleDTO vehicleDTO) {
        Vehicle vehicle = userEntity.getVehicles().stream().findFirst().orElseGet(() -> {
            Vehicle newVehicle = new Vehicle();
            newVehicle.setUser(userEntity);
            userEntity.getVehicles().add(newVehicle);
            return newVehicle;
        });
        vehicle.setVersion(vehicleDTO.getVersion());
        vehicle.setRegistrationNumber(vehicleDTO.getRegistrationNumber());
        vehicle.setBrand(vehicleDTO.getBrand());
        vehicle.setModel(vehicleDTO.getModel());
        vehicle.setEnergyType(vehicleDTO.getEnergyType());
    }

    private void applyUpdatedDocuments(User userEntity, MultiValueMap<String, MultipartFile> filesMap, String filesPath) {
        if (filesMap == null || filesMap.isEmpty()) {
            return;
        }

        Map<DOCUMENT_TYPE, Document> documentsByType = userEntity.getDocument().stream()
                .collect(Collectors.toMap(Document::getType, document -> document, (left, right) -> left, LinkedHashMap::new));

        filesMap.forEach((fileName, multipartFiles) -> {
            if (multipartFiles == null || multipartFiles.isEmpty() || multipartFiles.get(0) == null) {
                return;
            }
            MultipartFile file = multipartFiles.get(0);
            String currentFileName = file.getOriginalFilename();
            if (currentFileName == null || !currentFileName.contains(".")) {
                return;
            }

            DOCUMENT_TYPE documentType = DOCUMENT_TYPE.valueOf(fileName);
            String newFileName = fileName + currentFileName.substring(currentFileName.lastIndexOf('.'));
            Document document = documentsByType.get(documentType);
            if (document == null) {
                document = new Document();
                document.setType(documentType);
                document.setUser(userEntity);
                documentsByType.put(documentType, document);
            }
            document.setDocURL(resolveDocumentPath(filesPath, newFileName));
            document.setDocumentStatus(DOCUMENT_STATUS.PENDING_VALIDATION);
            document.setReviewComment(null);
            document.setReviewedAt(null);
            document.setReviewedBy(null);
        });

        userEntity.setDocument(new HashSet<>(documentsByType.values()));
    }

    private String normalizeReviewComment(String reviewComment, DOCUMENT_STATUS documentStatus) {
        if (documentStatus != DOCUMENT_STATUS.REJECTED) {
            return null;
        }
        if (reviewComment == null) {
            return null;
        }
        String trimmedComment = reviewComment.trim();
        return trimmedComment.isEmpty() ? null : trimmedComment;
    }

    private String buildUserFilesPath(String emailAddress) {
        String safeDirectoryName = emailAddress == null ? "unknown" : emailAddress.replace('.', '_');
        return Paths.get(userDocPath, safeDirectoryName).toString();
    }

    private String resolveDocumentPath(String directoryPath, String fileName) {
        Path resolvedPath = Paths.get(directoryPath).resolve(fileName);
        return resolvedPath.toString();
    }

    private byte[] readDocumentBytes(Document document, String emailAddress) throws IOException {
        Path primaryPath = Paths.get(document.getDocURL());
        if (Files.exists(primaryPath)) {
            return Files.readAllBytes(primaryPath);
        }

        String normalizedPathValue = document.getDocURL().replace("\\", java.io.File.separator);
        Path normalizedPath = Paths.get(normalizedPathValue);
        if (Files.exists(normalizedPath)) {
            logger.info("Normalized legacy document path for user {} and type {} from {} to {}",
                    emailAddress, document.getType(), document.getDocURL(), normalizedPath);
            document.setDocURL(normalizedPath.toString());
            documents.save(document);
            return Files.readAllBytes(normalizedPath);
        }

        throw new IOException("Document file not found at " + document.getDocURL());
    }

    private String buildUserUpdateLink(User user) {
        return resolveFrontendBaseUrl() + "/userSignInPage?updateToken=" + userUpdateTokenService.generateToken(user.getId());
    }

    private User resolveUserByUpdateToken(String updateToken) {
        Long userId = userUpdateTokenService.validateAndExtractUserId(updateToken);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid update token");
        }
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (Boolean.TRUE.equals(user.getActiveAccount())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account already validated");
        }
        if (!DELIVERY_PERSON.equalsIgnoreCase(user.getType())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Public update is not available for this account");
        }
        return user;
    }

    private String resolveFrontendBaseUrl() {
        String configuredBaseUrl = frontendBaseUrl;
        if (configuredBaseUrl == null || configuredBaseUrl.isBlank()) {
            configuredBaseUrl = userUpdateLink;
        }
        if (configuredBaseUrl != null && configuredBaseUrl.contains("/userSignInPage")) {
            configuredBaseUrl = configuredBaseUrl.substring(0, configuredBaseUrl.indexOf("/userSignInPage"));
        }
        return PublicUrlResolver.resolvePreferredFrontendBaseUrl(configuredBaseUrl);
    }

    private String resolveGatewayBaseUrl() {
        String configuredBaseUrl = gatewayBaseUrl;
        if (configuredBaseUrl == null || configuredBaseUrl.isBlank()) {
            configuredBaseUrl = emailValidationLink;
        }
        if (configuredBaseUrl != null && configuredBaseUrl.contains("/users/v1/validateEmail")) {
            configuredBaseUrl = configuredBaseUrl.substring(0, configuredBaseUrl.indexOf("/users/v1/validateEmail"));
        }
        return PublicUrlResolver.resolvePreferredGatewayBaseUrl(configuredBaseUrl);
    }
}
