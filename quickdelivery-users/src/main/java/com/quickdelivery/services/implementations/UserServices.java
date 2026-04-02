package com.quickdelivery.services.implementations;

import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.quickdelivery.abstarct.dto.AdminUserOverviewDTO;
import com.quickdelivery.PublicUrlResolver;
import com.quickdelivery.abstarct.dto.DocumentContentDTO;
import com.quickdelivery.abstarct.dto.DocumentDTO;
import com.quickdelivery.abstarct.dto.HttpEndpointMetricDTO;
import com.quickdelivery.abstarct.dto.ServiceMetricsDTO;
import com.quickdelivery.abstarct.dto.ServiceHttpBreakdownDTO;
import com.quickdelivery.abstarct.dto.UserOnboardingDTO;
import com.quickdelivery.abstarct.dto.UserDTO;
import com.quickdelivery.abstarct.dto.UserValidationPageDTO;
import com.quickdelivery.abstarct.dto.VehicleDTO;
import com.quickdelivery.abstarct.entities.Document;
import com.quickdelivery.abstarct.entities.User;
import com.quickdelivery.abstarct.entities.UserOnboarding;
import com.quickdelivery.abstarct.entities.Vehicle;
import com.quickdelivery.abstarct.helpers.GeoHelper;
import com.quickdelivery.abstarct.helpers.MailHelper;
import com.quickdelivery.abstarct.parameters.CHECK_STATUS;
import com.quickdelivery.abstarct.parameters.DOCUMENT_MATCH_STATUS;
import com.quickdelivery.abstarct.parameters.DOCUMENT_OCR_STATUS;
import com.quickdelivery.abstarct.parameters.DOCUMENT_STATUS;
import com.quickdelivery.abstarct.parameters.DOCUMENT_TYPE;
import com.quickdelivery.abstarct.parameters.DOCUMENT_VALIDATION_STATUS;
import com.quickdelivery.abstarct.parameters.EMAIL_TEMPLATE_TYPE;
import com.quickdelivery.abstarct.parameters.USER_ONBOARDING_STATUS;
import com.quickdelivery.abstarct.repositories.Documents;
import com.quickdelivery.abstarct.repositories.UserOnboardings;
import com.quickdelivery.abstarct.repositories.Users;
import com.quickdelivery.services.interfaces.IKeycloakProvisioningService;
import com.quickdelivery.services.interfaces.IUserServices;
import com.quickdelivery.security.UserUpdateTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.Timer;
import jakarta.mail.MessagingException;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.http.HttpStatus;

@Service
public class UserServices implements IUserServices {
    private static final String CUSTOMER = "CUSTOMER";
    private static final String DELIVERY_PERSON = "DELIVERY_PERSON";
    private static final String ROLE_CLIENT = "ROLE_CLIENT";
    private static final String ROLE_CLIENT_PRO = "ROLE_CLIENT_PRO";
    private static final String ROLE_LIVREUR = "ROLE_LIVREUR";
    private static final String FRONTEND_CLIENT_ID = "quickdelivery-front";
    private static final int IDENTITY_SYNC_RETRY_ATTEMPTS = 3;
    private static final int ONBOARDING_STEP_PROFILE = 1;
    private static final int ONBOARDING_STEP_ADDRESS = 2;
    private static final int ONBOARDING_STEP_DOCUMENTS = 3;
    private static final int ONBOARDING_STEP_VEHICLE = 4;
    private static final int ONBOARDING_STEP_SUMMARY = 5;
    private static final long MAX_DOCUMENT_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_DOCUMENT_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".pdf", ".webp");
    private static final Set<String> ALLOWED_DOCUMENT_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/webp",
            "application/pdf"
    );
    private static final String SYSTEM_REVIEWER = "SYSTEM";
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
    @Value("${quickdelivery.notifications.email.enabled:true}")
    private boolean emailNotificationsEnabled;
    @Value("${quickdelivery.notifications.email.cooldown-seconds:300}")
    private long emailNotificationCooldownSeconds;
    @Autowired
    private Users users;
    @Autowired
    private Documents documents;
    @Autowired
    private UserOnboardings userOnboardings;
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
    @Autowired
    private UserOnboardingValidationService userOnboardingValidationService;
    @Autowired
    private DocumentOcrOrchestrator documentOcrOrchestrator;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MeterRegistry meterRegistry;
    @Autowired
    @Qualifier("mailTaskExecutor")
    private Executor mailTaskExecutor;
    @Autowired
    @Qualifier("userAsyncTaskExecutor")
    private Executor userAsyncTaskExecutor;
    @Autowired
    private UserDocumentStorageService userDocumentStorageService;
    private final AtomicLong emailNotificationsDisabledUntilEpochMs = new AtomicLong(0L);

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "usersValidationPage", allEntries = true),
            @CacheEvict(value = "usersAdminMetrics", allEntries = true),
            @CacheEvict(value = "usersAdminUserOverview", allEntries = true),
            @CacheEvict(value = "usersAdminHttpBreakdown", allEntries = true)
    })
    public UserDTO createAccount(UserDTO user, Locale locale) {
        return recordUserOperation("createAccount", () -> {
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User payload is required");
            }
            validateOnboardingRequest(userOnboardingValidationService.validateForAccountCreate(user, locale));
            User existingUser = users.findByEmail(user.getEmailAddress());
            if (existingUser != null) {
                return resumeExistingAccountCreate(existingUser, user, locale);
            }

            boolean deliveryPerson = DELIVERY_PERSON.equalsIgnoreCase(user.getType());
            String rawPassword = user.getPassword();
            User userEntity = new User();
            userEntity.setType(user.getType());
            userEntity.setFirstName(user.getFirstName());
            userEntity.setLastName(user.getLastName());
            userEntity.setAge(user.getAge());
            userEntity.setBirthDate(user.getBirthDate());
            userEntity.setSex(user.getSex());
            userEntity.setEmailAddress(user.getEmailAddress());
            userEntity.setEmailAddressValidation(Boolean.TRUE.equals(user.getEmailAddressValidation()));
            userEntity.setPhone(user.getPhone());
            userEntity.setPhoneValidation(Boolean.TRUE.equals(user.getPhoneValidation()));
            userEntity.setPassword(rawPassword);
            userEntity.setDeliveryMode(deliveryPerson ? user.getDeliveryMode() : null);
            userEntity.setActiveAccount(!deliveryPerson);
            userEntity.setPersonalAddress(new HashSet<>());
            userEntity.setVehicles(new HashSet<>());
            userEntity.setDocument(new HashSet<>());
            userEntity.setPayments(new HashSet<>());

            users.saveAndFlush(userEntity);

            Timestamp now = new Timestamp(System.currentTimeMillis());
            UserOnboarding onboarding = loadOrCreateOnboarding(userEntity);
            onboarding.setAccountCreatedAt(now);
            onboarding.setLastUpdatedAt(now);
            if (deliveryPerson) {
                onboarding.setStatus(USER_ONBOARDING_STATUS.ACCOUNT_CREATED);
                onboarding.setCurrentStep(ONBOARDING_STEP_ADDRESS);
            } else {
                onboarding.setProfileCompletedAt(now);
                onboarding.setCompletedAt(now);
                onboarding.setStatus(USER_ONBOARDING_STATUS.COMPLETED);
                onboarding.setCurrentStep(ONBOARDING_STEP_PROFILE);
            }
            userOnboardings.save(onboarding);
            scheduleIdentityProvisioning(userEntity, rawPassword);
            UserDTO response = toUserDetailDTO(userEntity, onboarding);
            if (deliveryPerson) {
                attachResumeAccess(response, userEntity);
                runUserMailTask(() -> sendUserOnboardingResumeEmail(response, userEntity, locale));
            }
            return response;
        });
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "usersValidationPage", allEntries = true),
            @CacheEvict(value = "usersAdminMetrics", allEntries = true),
            @CacheEvict(value = "usersAdminUserOverview", allEntries = true),
            @CacheEvict(value = "usersAdminHttpBreakdown", allEntries = true)
    })
    public UserDTO saveOnboardingDraft(UserDTO user, VehicleDTO vehicleDTO, MultiValueMap<String, MultipartFile> filesMap, Locale locale, Integer step) {
        return recordUserOperation("saveOnboardingDraft", () -> {
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User payload is required");
            }
            int requestedStep = step == null ? ONBOARDING_STEP_ADDRESS : step;
            boolean deliveryPerson = DELIVERY_PERSON.equalsIgnoreCase(user.getType());
            if (!deliveryPerson) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft onboarding is only available for delivery persons");
            }

            sanitizeAddresses(user, true);
            User userEntity = resolveExistingUserForOnboarding(user);
            UserOnboarding onboarding = loadOrCreateOnboarding(userEntity);
            assertDraftOnboardingEditable(userEntity, onboarding);
            validateOnboardingRequest(userOnboardingValidationService.validateForDraftStep(
                    user,
                    vehicleDTO == null ? new VehicleDTO() : vehicleDTO,
                    filesMap,
                    userEntity,
                    locale,
                    requestedStep
            ));

            String filesPath = buildUserFilesPath(userEntity.getEmailAddress());
            mergeUserProfile(userEntity, user);
            geocodeAddressesIfPossible(user);
            if (requestedStep >= ONBOARDING_STEP_VEHICLE) {
                mergeVehicle(userEntity, vehicleDTO == null ? new VehicleDTO() : vehicleDTO);
            }

            MultiValueMap<String, MultipartFile> validFilesMap = applyUpdatedDocuments(userEntity, filesMap, filesPath, locale);
            users.saveAndFlush(userEntity);
            if (!validFilesMap.isEmpty()) {
                userDocumentStorageService.saveFiles(validFilesMap, filesPath, true);
                triggerOcrForUploadedDocuments(userEntity, validFilesMap);
            }

            Timestamp now = new Timestamp(System.currentTimeMillis());
            if (onboarding.getAccountCreatedAt() == null) {
                onboarding.setAccountCreatedAt(now);
            }
            int nextStep = resolveNextDraftStep(userEntity);
            if (nextStep >= ONBOARDING_STEP_DOCUMENTS) {
                onboarding.setProfileCompletedAt(now);
                onboarding.setStatus(USER_ONBOARDING_STATUS.PROFILE_COMPLETED);
            }
            if (nextStep >= ONBOARDING_STEP_VEHICLE || nextStep == ONBOARDING_STEP_SUMMARY) {
                onboarding.setDocumentsUploadedAt(now);
                onboarding.setStatus(USER_ONBOARDING_STATUS.DOCUMENTS_UPLOADED);
            }
            onboarding.setCurrentStep(nextStep);
            onboarding.setLastUpdatedAt(now);
            onboarding.setLastErrorCode(null);
            onboarding.setLastErrorMessage(null);
            userOnboardings.save(onboarding);

            UserDTO response = toUserDetailDTO(userEntity, onboarding);
            attachResumeAccess(response, userEntity);
            runUserMailTask(() -> sendUserOnboardingResumeEmail(response, userEntity, locale));
            return response;
        });
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "usersValidationPage", allEntries = true),
            @CacheEvict(value = "usersAdminMetrics", allEntries = true),
            @CacheEvict(value = "usersAdminUserOverview", allEntries = true),
            @CacheEvict(value = "usersAdminHttpBreakdown", allEntries = true)
    })
    public UserDTO completeOnboarding(UserDTO user, VehicleDTO vehicleDTO, MultiValueMap<String, MultipartFile> filesMap, Locale locale) {
        return recordUserOperation("completeOnboarding", () -> {
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User payload is required");
            }
            boolean deliveryPerson = DELIVERY_PERSON.equalsIgnoreCase(user.getType());
            sanitizeAddresses(user, deliveryPerson);
            User userEntity = resolveExistingUserForOnboarding(user);
            UserOnboarding onboarding = loadOrCreateOnboarding(userEntity);
            assertDraftOnboardingEditable(userEntity, onboarding);
            validateOnboardingRequest(userOnboardingValidationService.validateForUpdate(user, vehicleDTO, filesMap, userEntity, locale));
            String filesPath = buildUserFilesPath(userEntity.getEmailAddress());

            mergeUserProfile(userEntity, user);
            if (deliveryPerson) {
                geocodeAddressesIfPossible(user);
                mergeVehicle(userEntity, vehicleDTO == null ? new VehicleDTO() : vehicleDTO);
            } else {
                userEntity.setVehicles(new HashSet<>());
            }

            MultiValueMap<String, MultipartFile> validFilesMap = applyUpdatedDocuments(userEntity, filesMap, filesPath, locale);
            users.saveAndFlush(userEntity);
            if (!validFilesMap.isEmpty()) {
                userDocumentStorageService.saveFiles(validFilesMap, filesPath, true);
                triggerOcrForUploadedDocuments(userEntity, validFilesMap);
            }

            Timestamp now = new Timestamp(System.currentTimeMillis());
            if (onboarding.getAccountCreatedAt() == null) {
                onboarding.setAccountCreatedAt(now);
            }
            onboarding.setProfileCompletedAt(now);
            if (!validFilesMap.isEmpty()) {
                onboarding.setDocumentsUploadedAt(now);
            }
            if (deliveryPerson) {
                onboarding.setReadyForValidationAt(now);
                onboarding.setStatus(USER_ONBOARDING_STATUS.READY_FOR_VALIDATION);
                onboarding.setCurrentStep(ONBOARDING_STEP_SUMMARY);
            } else {
                onboarding.setCompletedAt(now);
                onboarding.setStatus(USER_ONBOARDING_STATUS.COMPLETED);
                onboarding.setCurrentStep(ONBOARDING_STEP_PROFILE);
            }
            onboarding.setLastUpdatedAt(now);
            onboarding.setLastErrorCode(null);
            onboarding.setLastErrorMessage(null);
            userOnboardings.save(onboarding);
            return toUserDetailDTO(userEntity);
        });
    }
    @Override
    @Caching(evict = {
            @CacheEvict(value = "usersValidationPage", allEntries = true),
            @CacheEvict(value = "usersAdminMetrics", allEntries = true),
            @CacheEvict(value = "usersAdminUserOverview", allEntries = true),
            @CacheEvict(value = "usersAdminHttpBreakdown", allEntries = true)
    })
    public UserDTO createNewUser(UserDTO user, VehicleDTO vehicleDTO, MultiValueMap<String, MultipartFile> filesMap, Locale locale) {
        return recordUserOperation("create", () -> {
            try {
                boolean deliveryPerson = DELIVERY_PERSON.equalsIgnoreCase(user.getType());
                String rawPassword = user.getPassword();
                sanitizeAddresses(user, deliveryPerson);
                validateOnboardingRequest(userOnboardingValidationService.validateForCreate(user, vehicleDTO, filesMap, locale));
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
                MultiValueMap<String, MultipartFile> validFilesMap = new org.springframework.util.LinkedMultiValueMap<>();
                filesMap.entrySet().stream()
                        .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty() && entry.getValue().get(0) != null)
                        .forEach(entry -> {
                            String fileName = entry.getKey();
                            MultipartFile file = entry.getValue().get(0);
                            DocumentValidationResult validationResult = validateDocumentFile(file, locale);
                            Document document = new Document();
                            document.setType(DOCUMENT_TYPE.valueOf(fileName));
                            document.setUser(userEntity);
                            applyValidationOutcome(document, validationResult);
                            if (!validationResult.valid()) {
                                userEntity.getDocument().add(document);
                                return;
                            }
                            String currentFileName = file.getOriginalFilename();
                            if (currentFileName == null || !currentFileName.contains(".")) {
                                return;
                            }
                            String newFileName = fileName+currentFileName.substring(currentFileName.lastIndexOf('.'));
                            document.setDocURL(resolveDocumentPath(filesPath, newFileName));
                            userEntity.getDocument().add(document);
                            validFilesMap.add(fileName, file);
                        });
                users.saveAndFlush(userEntity);
                try {
                    if (!validFilesMap.isEmpty()) {
                        userDocumentStorageService.saveFiles(validFilesMap, filesPath, false);
                    }
                    keycloakProvisioningService.provisionUser(userEntity, rawPassword);
                } catch (Exception externalFailure) {
                    rollbackFailedUserCreation(userEntity, filesPath);
                    throw externalFailure;
                }
                if (!validFilesMap.isEmpty()) {
                    triggerOcrForUploadedDocuments(userEntity, validFilesMap);
                }
                userEntity.getPersonalAddress().forEach(address -> address.getResidents().setPersonalAddress(new HashSet<>()));
                user.setId(userEntity.getId());
                user.setVersion(userEntity.getVersion());
                user.setPassword(null);
                user.setPasswordConfirmation(null);
                runUserMailTask(() -> sendUserAccountCreationEmail(user, userEntity, locale));
                return user;
            } catch (Exception exception) {
                logger.error("Unable to create user account for email={} type={}: {}", user.getEmailAddress(), user.getType(), exception.getMessage(), exception);
                throw exception;
            }
        });
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "usersValidationPage", allEntries = true),
            @CacheEvict(value = "usersAdminMetrics", allEntries = true),
            @CacheEvict(value = "usersAdminUserOverview", allEntries = true),
            @CacheEvict(value = "usersAdminHttpBreakdown", allEntries = true)
    })
    public UserDTO updateNewUser(UserDTO user, VehicleDTO vehicleDTO, MultiValueMap<String, MultipartFile> filesMap, Locale locale) {
        return recordUserOperation("update", () -> {
            try {
                boolean deliveryPerson = DELIVERY_PERSON.equalsIgnoreCase(user.getType());
                sanitizeAddresses(user, deliveryPerson);
                User userEntity = users.findById(user.getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                validateOnboardingRequest(userOnboardingValidationService.validateForUpdate(user, vehicleDTO, filesMap, userEntity, locale));
                String filesPath = buildUserFilesPath(user.getEmailAddress());

                mergeUserProfile(userEntity, user);
                if (deliveryPerson) {
                    geocodeAddressesIfPossible(user);
                    mergeVehicle(userEntity, vehicleDTO);
                } else {
                    userEntity.setPersonalAddress(new HashSet<>());
                    userEntity.setVehicles(new HashSet<>());
                }

                MultiValueMap<String, MultipartFile> validFilesMap = applyUpdatedDocuments(userEntity, filesMap, filesPath, locale);
                users.saveAndFlush(userEntity);
                if (!validFilesMap.isEmpty()) {
                    userDocumentStorageService.saveFiles(validFilesMap, filesPath, true);
                    triggerOcrForUploadedDocuments(userEntity, validFilesMap);
                }
                return toUserDetailDTO(userEntity);
            } catch (Exception exception) {
                logger.error("Unable to update user account for email={} type={}: {}", user.getEmailAddress(), user.getType(), exception.getMessage(), exception);
                throw exception;
            }
        });
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
    @Transactional(readOnly = true)
    public UserDTO findByID(Long id) {
        User user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        UserDTO userDTO = toUserDetailDTO(user);
        if (userDTO.getPersonalAddress() != null && !userDTO.getPersonalAddress().isEmpty()) {
            userDTO.setAddressAuto(userDTO.getPersonalAddress().get(0).toString());
        }
        return userDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO findByUpdateToken(String updateToken) {
        User user = resolveUserByUpdateToken(updateToken);
        return findByEmail(user.getEmailAddress());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "usersValidationPage", allEntries = true),
            @CacheEvict(value = "usersAdminMetrics", allEntries = true),
            @CacheEvict(value = "usersAdminUserOverview", allEntries = true),
            @CacheEvict(value = "usersAdminHttpBreakdown", allEntries = true)
    })
    public UserDTO userValidation(UserDTO user, Locale locale) {
        return recordUserOperation("validate", () -> {
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
                document.setReviewComment(resolveReviewComment(documentDTO, locale));
                document.setReviewedAt(new java.util.Date());
                document.setReviewedBy("ADMIN");
            });
            userFromDB.setDocument(new HashSet<>(existingDocuments.values()));
            users.saveAndFlush(userFromDB);
            scheduleIdentityStateSync(userFromDB);
            List<Document> rejectedDocuments = userFromDB.getDocument().stream()
                    .filter(document -> document.getDocumentStatus() == DOCUMENT_STATUS.REJECTED)
                    .collect(Collectors.toList());
            if(!rejectedDocuments.isEmpty()) {
                updateOnboardingForRejectedDocuments(userFromDB, rejectedDocuments);
                runUserMailTask(() -> sendUserDocumentsUpdateRequest(user, userFromDB, rejectedDocuments, locale));
            } else if (Boolean.TRUE.equals(userFromDB.getActiveAccount())) {
                markOnboardingCompleted(userFromDB);
                runUserMailTask(() -> sendUserAccountApprovedEmail(user, userFromDB, locale));
            }
            return toUserDetailDTO(userFromDB);
        });
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "usersValidationPage", allEntries = true),
            @CacheEvict(value = "usersAdminMetrics", allEntries = true),
            @CacheEvict(value = "usersAdminUserOverview", allEntries = true),
            @CacheEvict(value = "usersAdminHttpBreakdown", allEntries = true)
    })
    public void deleteUSer(UserDTO user) {
         users.delete(modelMapper.map(user,User.class));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "usersValidationPage", allEntries = true),
            @CacheEvict(value = "usersAdminMetrics", allEntries = true),
            @CacheEvict(value = "usersAdminUserOverview", allEntries = true),
            @CacheEvict(value = "usersAdminHttpBreakdown", allEntries = true)
    })
    public CHECK_STATUS validateUserEmail(Long id) {
        return recordUserOperation("validateEmail", () -> {
            User user = users.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            user.setEmailAddressValidation(true);
            users.saveAndFlush(user);
            keycloakProvisioningService.syncUserState(user);
            return CHECK_STATUS.OK;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO findByEmail(String email) {
        User user = users.findByEmail(email);
        if(user != null) {
            UserDTO userDTO = toUserDetailDTO(user);
            if (userDTO.getPersonalAddress() != null && !userDTO.getPersonalAddress().isEmpty()) {
                userDTO.setAddressAuto(userDTO.getPersonalAddress().get(0).toString());
            }
            return userDTO;
        }else
            return null;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "usersValidationPage", key = "T(java.lang.String).format('%s:%s', #page, #size)", sync = true)
    public UserValidationPageDTO findUsersForValidation(int page, int size) {
        return recordUserOperation("validationPage", () -> {
            int resolvedPage = Math.max(page, 0);
            int resolvedSize = Math.min(Math.max(size, 1), 100);
            Page<Long> userIdPage = users.findUserIdsForValidation(PageRequest.of(resolvedPage, resolvedSize));
            List<User> fetchedUsers = userIdPage.isEmpty()
                    ? List.of()
                    : users.findUsersForValidationByIds(userIdPage.getContent());
            Map<Long, User> usersById = fetchedUsers.stream()
                    .collect(Collectors.toMap(User::getId, currentUser -> currentUser));
            List<User> orderedUsers = userIdPage.getContent().stream()
                    .map(usersById::get)
                    .filter(Objects::nonNull)
                    .toList();
            Page<User> userPage = new PageImpl<>(orderedUsers, userIdPage.getPageable(), userIdPage.getTotalElements());
            List<UserDTO> items = userPage.getContent().stream()
                    .map(this::toUserValidationSummary)
                    .toList();

            UserValidationPageDTO response = new UserValidationPageDTO();
            response.setItems(items);
            response.setPage(userPage.getNumber());
            response.setSize(userPage.getSize());
            response.setTotalItems(userPage.getTotalElements());
            response.setTotalPages(userPage.getTotalPages());
            response.setTotalVehicles(users.countVehiclesForValidation());
            response.setTotalDocuments(users.countDocumentsForValidation());
            response.setTotalVehicleDocuments(users.countDocumentsForValidationByType(Set.of(DOCUMENT_TYPE.GRAY_CARD, DOCUMENT_TYPE.INSURANCE)));
            return response;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UserOnboardingDTO loadOnboardingStatus(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        UserOnboarding onboarding = userOnboardings.findByUserEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Onboarding not found"));
        return toUserOnboardingDTO(onboarding);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentContentDTO loadDocumentContent(Long documentId) {
        return recordUserOperation("documentContent", () -> {
            Document document = documents.findById(documentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
            if (document.getDocURL() == null || document.getDocURL().isBlank()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document file not found");
            }
            try {
                byte[] data = readDocumentBytes(document, document.getUser() == null ? null : document.getUser().getEmailAddress());
                recordUserDocumentContentSize(data.length);
                DocumentContentDTO response = new DocumentContentDTO();
                response.setData(data);
                response.setFileName(resolveDocumentFileName(document));
                response.setContentType(resolveDocumentContentType(document));
                return response;
            } catch (IOException exception) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document file not found");
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "usersAdminMetrics", key = "'singleton'", sync = true)
    public ServiceMetricsDTO loadAdminMetrics() {
        ServiceMetricsDTO metrics = new ServiceMetricsDTO();
        metrics.setServiceName("users-service");
        metrics.setUptimeSeconds(readGauge("process.uptime"));
        metrics.setHeapUsedMb(toMegabytes(readGauge("jvm.memory.used", "area", "heap")));
        metrics.setHeapMaxMb(toMegabytes(readGauge("jvm.memory.max", "area", "heap")));
        metrics.setCpuUsagePercent(toPercent(readGauge("system.cpu.usage")));
        metrics.setHttpRequestCount(sumMetric("http.server.requests"));
        metrics.setOperationCallCount(sumMetric("quickdelivery.users.operation.calls"));
        metrics.setAsyncQueueSize(sumMetric("quickdelivery.async.queue.size"));
        metrics.setAsyncActiveCount(sumMetric("quickdelivery.async.active.count"));
        metrics.setOcrProcessedCount(sumMetric("quickdelivery.ocr.document.processed"));
        return metrics;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "usersAdminUserOverview", key = "'singleton'", sync = true)
    public AdminUserOverviewDTO loadAdminUserOverview() {
        AdminUserOverviewDTO overview = new AdminUserOverviewDTO();
        overview.setRegisteredCustomers(users.countRegisteredUsersByType(CUSTOMER));
        overview.setRegisteredDeliveryPersons(users.countRegisteredUsersByType(DELIVERY_PERSON));

        try {
            Set<String> activeEmails = keycloakProvisioningService.loadActiveUserEmailsByClient(FRONTEND_CLIENT_ID).stream()
                    .map(email -> email.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());

            overview.setConnectedCustomers(users.countConnectedUsersByType(CUSTOMER, activeEmails));
            overview.setConnectedDeliveryPersons(users.countConnectedUsersByType(DELIVERY_PERSON, activeEmails));
        } catch (Exception exception) {
            logger.warn("Unable to load active Keycloak sessions for admin overview: {}", exception.getMessage());
        }

        return overview;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "usersAdminHttpBreakdown", key = "'singleton'", sync = true)
    public ServiceHttpBreakdownDTO loadAdminHttpBreakdown() {
        ServiceHttpBreakdownDTO breakdown = new ServiceHttpBreakdownDTO();
        breakdown.setServiceName("users-service");

        Map<String, HttpEndpointMetricDTO> aggregated = new LinkedHashMap<>();
        meterRegistry.find("http.server.requests").meters().forEach(meter -> collectHttpEndpointMetric(aggregated, meter));

        List<HttpEndpointMetricDTO> endpoints = aggregated.values().stream()
                .sorted(Comparator.comparingDouble(HttpEndpointMetricDTO::getRequestCount).reversed())
                .limit(12)
                .toList();
        breakdown.setEndpoints(endpoints);
        breakdown.setTotalRequestCount(aggregated.values().stream().mapToDouble(HttpEndpointMetricDTO::getRequestCount).sum());
        return breakdown;
    }

    private void sendUserAccountCreationEmail(UserDTO user, User userEntity, Locale locale){
        if (shouldSkipUserMail()) {
            return;
        }
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("recipientName", resolveRecipientName(user));
        templateModel.put("validationLink", buildValidationLink(userEntity.getId()));
        try {
            MailHelper.sendMessageUsingThymeleafTemplate(messageSource, templateResolver, userEntity.getEmailAddress(),messageSource.getMessage("email.subject.uservalidation", null, locale),templateModel,
                    locale, EMAIL_TEMPLATE_TYPE.NEW_DELIVERYPERSON_VALIDATION.getType(), null);
        } catch (Exception e) {
            handleUserMailFailure(e, userEntity.getEmailAddress());
            logger.warn("Unable to send validation email to {}: {}", userEntity.getEmailAddress(), e.getMessage(), e);
        }
    }

    private void sendUserOnboardingResumeEmail(UserDTO user, User userEntity, Locale locale) {
        if (shouldSkipUserMail() || user.getOnboarding() == null || user.getOnboarding().getResumeLink() == null) {
            return;
        }
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("recipientName", resolveRecipientName(user));
        templateModel.put("resumeLink", user.getOnboarding().getResumeLink());
        templateModel.put("currentStepLabel", localizeOnboardingStep(user.getOnboarding().getCurrentStep(), locale));
        try {
            MailHelper.sendMessageUsingThymeleafTemplate(
                    messageSource,
                    templateResolver,
                    userEntity.getEmailAddress(),
                    messageSource.getMessage("email.subject.userOnboardingResume", null, locale),
                    templateModel,
                    locale,
                    EMAIL_TEMPLATE_TYPE.DELIVERYPERSON_ONBOARDING_RESUME.getType(),
                    null
            );
        } catch (Exception e) {
            handleUserMailFailure(e, userEntity.getEmailAddress());
            logger.warn("Unable to send onboarding-resume email to {}: {}", userEntity.getEmailAddress(), e.getMessage(), e);
        }
    }

    private void sendUserDocumentsUpdateRequest(UserDTO user, User userEntity, List<Document> rejectedDocuments, Locale locale){
        if (shouldSkipUserMail()) {
            return;
        }
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("recipientName", resolveRecipientName(user));
        templateModel.put("updateLink", buildUserUpdateLink(userEntity));
        templateModel.put("rejectedDocuments", rejectedDocuments.stream()
                .map(document -> Map.of(
                        "label", formatDocumentType(document.getType(), locale),
                        "reason", Optional.ofNullable(document.getReviewComment())
                                .filter(comment -> !comment.isBlank())
                                .orElse(localizedMissingReviewReason(locale))
                ))
                .toList());
        try {
            MailHelper.sendMessageUsingThymeleafTemplate(messageSource, templateResolver, userEntity.getEmailAddress(),messageSource.getMessage("email.subject.userUpdateDocsRequest", null, locale),templateModel,
                    locale, EMAIL_TEMPLATE_TYPE.DELIVERYPERSON_DOCUPDATE_REQUEST.getType(), null);
        } catch (Exception e) {
            handleUserMailFailure(e, userEntity.getEmailAddress());
            logger.warn("Unable to send update-documents email to {}: {}", userEntity.getEmailAddress(), e.getMessage(), e);
        }
    }

    private void sendUserAccountApprovedEmail(UserDTO user, User userEntity, Locale locale){
        if (shouldSkipUserMail()) {
            return;
        }
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("recipientName", resolveRecipientName(user));
        templateModel.put("landingLink", resolveFrontendBaseUrl());
        try {
            MailHelper.sendMessageUsingThymeleafTemplate(messageSource, templateResolver, userEntity.getEmailAddress(),messageSource.getMessage("email.subject.userAccountApproved", null, locale),templateModel,
                    locale, EMAIL_TEMPLATE_TYPE.DELIVERYPERSON_ACCOUNT_APPROVED.getType(), null);
        } catch (Exception e) {
            handleUserMailFailure(e, userEntity.getEmailAddress());
            logger.warn("Unable to send account-approved email to {}: {}", userEntity.getEmailAddress(), e.getMessage(), e);
        }
    }

    private void runUserMailTask(Runnable task) {
        if (!emailNotificationsEnabled || shouldSkipUserMail()) {
            return;
        }
        submitUserAsyncTask(task, mailTaskExecutor, "user-mail");
    }

    private boolean shouldSkipUserMail() {
        return !emailNotificationsEnabled || System.currentTimeMillis() < emailNotificationsDisabledUntilEpochMs.get();
    }

    private void handleUserMailFailure(Exception exception, String recipient) {
        String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase(Locale.ROOT);
        if (message.contains("too many login attempts")
                || message.contains("authenticationfailed")
                || message.contains("authentication failed")) {
            long disabledUntil = System.currentTimeMillis() + (emailNotificationCooldownSeconds * 1000L);
            emailNotificationsDisabledUntilEpochMs.set(disabledUntil);
            logger.warn("Temporarily disabling user email notifications until {} after failure for {}",
                    new Date(disabledUntil), recipient);
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
            DocumentDTO documentDTO = toDocumentMetadata(document);
            userDTO.getDocument().put(document.getType(), documentDTO);
        });
        userDTO.setDocumentCount(userDTO.getDocument().size());
    }

    private UserDTO toUserValidationSummary(User user) {
        UserDTO userDTO = toBasicUserDTO(user);
        userDTO.setAddressAuto(resolveUserAddressAuto(user));
        userDTO.setDocument(new LinkedHashMap<>());
        userDTO.setDocumentCount(user.getDocument() == null ? 0 : user.getDocument().size());
        userDTO.setVehicles(user.getVehicles() == null ? new ArrayList<>() : user.getVehicles().stream()
                .map(this::toVehicleDTO)
                .toList());
        return userDTO;
    }

    private UserDTO toUserDetailDTO(User user) {
        UserDTO userDTO = toBasicUserDTO(user);
        userDTO.setAddressAuto(resolveUserAddressAuto(user));
        userDTO.setEmailAddressConfirmation(userDTO.getEmailAddress());
        userDTO.setPhoneConfirmation(userDTO.getPhone());
        attachDocumentsSafely(user, userDTO);
        userDTO.setVehicles(user.getVehicles() == null ? new ArrayList<>() : user.getVehicles().stream()
                .map(this::toVehicleDTO)
                .toList());
        userOnboardings.findByUserId(user.getId()).ifPresent(onboarding -> userDTO.setOnboarding(toUserOnboardingDTO(onboarding)));
        return userDTO;
    }

    private UserDTO toUserDetailDTO(User user, UserOnboarding onboarding) {
        UserDTO userDTO = toBasicUserDTO(user);
        userDTO.setAddressAuto(resolveUserAddressAuto(user));
        userDTO.setEmailAddressConfirmation(userDTO.getEmailAddress());
        userDTO.setPhoneConfirmation(userDTO.getPhone());
        attachDocumentsSafely(user, userDTO);
        userDTO.setVehicles(user.getVehicles() == null ? new ArrayList<>() : user.getVehicles().stream()
                .map(this::toVehicleDTO)
                .toList());
        if (onboarding != null) {
            userDTO.setOnboarding(toUserOnboardingDTO(onboarding));
        }
        return userDTO;
    }

    private UserDTO toBasicUserDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setVersion(user.getVersion());
        userDTO.setType(user.getType());
        userDTO.setFirstName(user.getFirstName());
        userDTO.setLastName(user.getLastName());
        userDTO.setAge(user.getAge());
        userDTO.setBirthDate(user.getBirthDate());
        userDTO.setSex(user.getSex());
        userDTO.setEmailAddress(user.getEmailAddress());
        userDTO.setEmailAddressValidation(user.getEmailAddressValidation());
        userDTO.setPhone(user.getPhone());
        userDTO.setPhoneValidation(user.getPhoneValidation());
        userDTO.setActiveAccount(user.getActiveAccount());
        userDTO.setDeliveryMode(user.getDeliveryMode());
        userDTO.setPassword(null);
        userDTO.setPasswordConfirmation(null);
        userDTO.setPersonalAddress(user.getPersonalAddress() == null ? new ArrayList<>() : user.getPersonalAddress().stream()
                .map(this::toAddressDTO)
                .toList());
        return userDTO;
    }

    private UserOnboardingDTO toUserOnboardingDTO(UserOnboarding onboarding) {
        return toUserOnboardingDTO(onboarding, false);
    }

    private UserOnboardingDTO toUserOnboardingDTO(UserOnboarding onboarding, boolean includeResumeAccess) {
        UserOnboardingDTO dto = new UserOnboardingDTO();
        dto.setUserId(onboarding.getUser() == null ? null : onboarding.getUser().getId());
        dto.setUserType(onboarding.getUser() == null ? null : onboarding.getUser().getType());
        dto.setStatus(onboarding.getStatus() == null ? null : onboarding.getStatus().name());
        dto.setAccountCreated(onboarding.getAccountCreatedAt() != null);
        dto.setProfileCompleted(onboarding.getProfileCompletedAt() != null);
        dto.setDocumentsUploaded(onboarding.getDocumentsUploadedAt() != null);
        dto.setReadyForValidation(onboarding.getReadyForValidationAt() != null);
        dto.setActiveAccount(onboarding.getUser() != null ? onboarding.getUser().getActiveAccount() : null);
        dto.setEmailAddressValidation(onboarding.getUser() != null ? onboarding.getUser().getEmailAddressValidation() : null);
        dto.setAccountCreatedAt(onboarding.getAccountCreatedAt());
        dto.setProfileCompletedAt(onboarding.getProfileCompletedAt());
        dto.setDocumentsUploadedAt(onboarding.getDocumentsUploadedAt());
        dto.setReadyForValidationAt(onboarding.getReadyForValidationAt());
        dto.setCompletedAt(onboarding.getCompletedAt());
        dto.setCurrentStep(resolveCurrentStep(onboarding));
        if (includeResumeAccess && onboarding.getUser() != null && isResumableOnboarding(onboarding.getUser(), onboarding)) {
            String resumeToken = userUpdateTokenService.generateToken(onboarding.getUser().getId());
            dto.setResumeToken(resumeToken);
            dto.setResumeLink(buildUserUpdateLink(resumeToken));
        }
        return dto;
    }

    private VehicleDTO toVehicleDTO(Vehicle vehicle) {
        VehicleDTO vehicleDTO = new VehicleDTO();
        vehicleDTO.setId(vehicle.getId());
        vehicleDTO.setVersion(vehicle.getVersion());
        vehicleDTO.setBrand(vehicle.getBrand());
        vehicleDTO.setModel(vehicle.getModel());
        vehicleDTO.setEnergyType(vehicle.getEnergyType());
        vehicleDTO.setRegistrationNumber(vehicle.getRegistrationNumber());
        vehicleDTO.setVehicleDocuments(new LinkedHashMap<>());
        return vehicleDTO;
    }

    private com.quickdelivery.abstarct.dto.AddressDTO toAddressDTO(com.quickdelivery.abstarct.entities.Address address) {
        com.quickdelivery.abstarct.dto.AddressDTO addressDTO = new com.quickdelivery.abstarct.dto.AddressDTO();
        addressDTO.setId(address.getId());
        addressDTO.setVersion(address.getVersion());
        addressDTO.setFirstName(address.getFirstName());
        addressDTO.setLastName(address.getLastName());
        addressDTO.setLine1(address.getLine1());
        addressDTO.setLine2(address.getLine2());
        addressDTO.setTown(address.getTown());
        addressDTO.setZipCode(address.getZipCode());
        addressDTO.setCountry(address.getCountry());
        addressDTO.setFloor(address.getFloor());
        addressDTO.setHasElevator(address.getHasElevator());
        addressDTO.setDateTime(address.getDateTime());
        addressDTO.setType(address.getType());
        addressDTO.setLatitude(address.getLatitude());
        addressDTO.setLongitude(address.getLongitude());
        addressDTO.setEmail(address.getEmail());
        addressDTO.setPhone(address.getPhone());
        addressDTO.setAddressAuto(addressDTO.toString());
        return addressDTO;
    }

    private String resolveUserAddressAuto(User user) {
        if (user.getPersonalAddress() == null || user.getPersonalAddress().isEmpty()) {
            return null;
        }
        return user.getPersonalAddress().stream()
                .findFirst()
                .map(address -> toAddressDTO(address).toString())
                .orElse(null);
    }

    private DocumentDTO toDocumentMetadata(Document document) {
        DocumentDTO documentDTO = modelMapper.map(document, DocumentDTO.class);
        documentDTO.setData(null);
        documentDTO.setFileName(resolveDocumentFileName(document));
        return documentDTO;
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

    private MultiValueMap<String, MultipartFile> applyUpdatedDocuments(User userEntity, MultiValueMap<String, MultipartFile> filesMap, String filesPath, Locale locale) {
        MultiValueMap<String, MultipartFile> validFilesMap = new org.springframework.util.LinkedMultiValueMap<>();
        if (filesMap == null || filesMap.isEmpty()) {
            return validFilesMap;
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
            DocumentValidationResult validationResult = validateDocumentFile(file, locale);
            String newFileName = fileName + currentFileName.substring(currentFileName.lastIndexOf('.'));
            Document document = documentsByType.get(documentType);
            if (document == null) {
                document = new Document();
                document.setType(documentType);
                document.setUser(userEntity);
                documentsByType.put(documentType, document);
            }
            applyValidationOutcome(document, validationResult);
            if (!validationResult.valid()) {
                return;
            }
            document.setDocURL(resolveDocumentPath(filesPath, newFileName));
            resetOcrState(document);
            validFilesMap.add(fileName, file);
        });

        userEntity.setDocument(new HashSet<>(documentsByType.values()));
        return validFilesMap;
    }

    private String resolveReviewComment(DocumentDTO documentDTO, Locale locale) {
        if (documentDTO == null || documentDTO.getDocumentStatus() != DOCUMENT_STATUS.REJECTED) {
            return null;
        }
        String reviewComment = documentDTO.getReviewComment();
        if (reviewComment == null) {
            return buildSuggestedReviewComment(documentDTO, locale);
        }
        String trimmedComment = reviewComment.trim();
        return trimmedComment.isEmpty() ? buildSuggestedReviewComment(documentDTO, locale) : trimmedComment;
    }

    private String buildSuggestedReviewComment(DocumentDTO documentDTO, Locale locale) {
        boolean french = locale != null && "fr".equalsIgnoreCase(locale.getLanguage());
        List<String> reasons = new ArrayList<>();

        if (documentDTO.getOcrErrorCode() != null && !documentDTO.getOcrErrorCode().isBlank()) {
            reasons.add(french
                    ? "Le document n'a pas pu etre analyse automatiquement : " + documentDTO.getOcrErrorCode() + "."
                    : "The document could not be analyzed automatically: " + documentDTO.getOcrErrorCode() + ".");
        }

        if (documentDTO.getMatchStatus() == DOCUMENT_MATCH_STATUS.MISMATCH) {
            List<String> mismatchedFields = extractMismatchedFields(documentDTO.getMatchDetails());
            if (mismatchedFields.isEmpty()) {
                reasons.add(french
                        ? "Les informations detectees ne correspondent pas au profil declare."
                        : "The detected information does not match the declared profile.");
            } else {
                reasons.add((french
                        ? "Les informations suivantes ne correspondent pas au profil declare : "
                        : "The following fields do not match the declared profile: ")
                        + String.join(", ", mismatchedFields) + ".");
            }
        } else if (documentDTO.getMatchStatus() == DOCUMENT_MATCH_STATUS.REVIEW_REQUIRED
                || documentDTO.getMatchStatus() == DOCUMENT_MATCH_STATUS.UNAVAILABLE) {
            reasons.add(french
                    ? "La correspondance automatique est insuffisante pour valider ce document."
                    : "Automatic matching is insufficient to validate this document.");
        }

        if (documentDTO.getOcrConfidenceScore() != null && documentDTO.getOcrConfidenceScore() < 1d) {
            reasons.add((french
                    ? "La confiance OCR est inferieure a 100% ("
                    : "OCR confidence is below 100% (")
                    + Math.round(documentDTO.getOcrConfidenceScore() * 100d)
                    + "%).");
        }

        if (reasons.isEmpty()) {
            reasons.add(localizedMissingReviewReason(locale));
        }

        return String.join(" ", reasons);
    }

    private List<String> extractMismatchedFields(String matchDetails) {
        if (matchDetails == null || matchDetails.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(matchDetails);
            JsonNode checks = root.path("checks");
            if (!checks.isArray()) {
                return List.of();
            }
            List<String> fields = new ArrayList<>();
            for (JsonNode check : checks) {
                if ("mismatch".equalsIgnoreCase(check.path("status").asText())) {
                    String field = check.path("field").asText();
                    if (!field.isBlank()) {
                        fields.add(field);
                    }
                }
            }
            return fields;
        } catch (Exception exception) {
            logger.debug("Unable to parse match details for rejection suggestion: {}", exception.getMessage());
            return List.of();
        }
    }

    private String formatDocumentType(DOCUMENT_TYPE documentType, Locale locale) {
        boolean french = locale != null && "fr".equalsIgnoreCase(locale.getLanguage());
        if (documentType == null) {
            return french ? "Document" : "Document";
        }
        return switch (documentType) {
            case ID -> french ? "Piece d'identite" : "Identity document";
            case DRIVER_LICENCE -> french ? "Permis de conduire" : "Driver licence";
            case GRAY_CARD -> french ? "Carte grise" : "Vehicle registration";
            case INSURANCE -> french ? "Assurance" : "Insurance";
            case USER_COMPANY_INSURANCE -> french ? "Assurance d'entreprise" : "Company insurance";
            case USER_COMPANY_EXTRACT -> french ? "Extrait d'entreprise" : "Company extract";
            case PICTURE -> french ? "Photo" : "Picture";
            case RIB -> "RIB";
            default -> documentType.name();
        };
    }

    private String localizedMissingReviewReason(Locale locale) {
        boolean french = locale != null && "fr".equalsIgnoreCase(locale.getLanguage());
        return french
                ? "Merci de renvoyer un document plus lisible et conforme."
                : "Please upload a clearer and compliant document.";
    }

    private String buildUserFilesPath(String emailAddress) {
        String safeDirectoryName = emailAddress == null ? "unknown" : emailAddress.replace('.', '_');
        return Paths.get(userDocPath, safeDirectoryName).toString();
    }

    private void rollbackFailedUserCreation(User userEntity, String filesPath) {
        if (userEntity != null && userEntity.getId() != null) {
            try {
                users.deleteById(userEntity.getId());
                users.flush();
            } catch (Exception cleanupException) {
                logger.error("Unable to rollback persisted user {} after external failure: {}",
                        userEntity.getEmailAddress(), cleanupException.getMessage(), cleanupException);
            }
        }
        if (filesPath == null || filesPath.isBlank()) {
            return;
        }
        userDocumentStorageService.deleteDirectory(filesPath);
    }

    private User resolveExistingUserForOnboarding(UserDTO user) {
        String resumeToken = user.getOnboarding() == null ? null : user.getOnboarding().getResumeToken();
        if (resumeToken != null && !resumeToken.isBlank()) {
            try {
                return resolveUserByUpdateToken(resumeToken);
            } catch (ResponseStatusException exception) {
                if (exception.getStatusCode() == HttpStatus.UNAUTHORIZED
                        || exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                    logger.warn("Ignoring stale onboarding resume token for user id={} email={}",
                            user.getId(), user.getEmailAddress());
                } else {
                    throw exception;
                }
            }
        }
        if (user.getId() != null) {
            return users.findById(user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        }
        if (user.getEmailAddress() != null && !user.getEmailAddress().isBlank()) {
            User existingUser = users.findByEmail(user.getEmailAddress());
            if (existingUser != null) {
                return existingUser;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }

    private UserOnboarding loadOrCreateOnboarding(User userEntity) {
        return userOnboardings.findByUserId(userEntity.getId()).orElseGet(() -> {
            UserOnboarding onboarding = new UserOnboarding();
            onboarding.setUser(userEntity);
            onboarding.setStatus(USER_ONBOARDING_STATUS.ACCOUNT_CREATED);
            onboarding.setLastUpdatedAt(new Timestamp(System.currentTimeMillis()));
            onboarding.setCurrentStep(ONBOARDING_STEP_ADDRESS);
            return onboarding;
        });
    }

    private void scheduleIdentityProvisioning(User userEntity, String rawPassword) {
        User identitySnapshot = buildIdentitySnapshot(userEntity);
        runAfterCommitAsync(() -> executeIdentityTask(
                        identitySnapshot.getId(),
                        () -> keycloakProvisioningService.provisionUser(identitySnapshot, rawPassword),
                        "identity-provisioning"
                ),
                userAsyncTaskExecutor,
                "identity-provisioning-dispatch");
    }

    private void scheduleIdentityStateSync(User userEntity) {
        User identitySnapshot = buildIdentitySnapshot(userEntity);
        runAfterCommitAsync(() -> executeIdentityTask(
                        identitySnapshot.getId(),
                        () -> keycloakProvisioningService.syncUserState(identitySnapshot),
                        "identity-sync"
                ),
                userAsyncTaskExecutor,
                "identity-sync-dispatch");
    }

    private void executeIdentityTask(Long userId, Runnable task, String taskName) {
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= IDENTITY_SYNC_RETRY_ATTEMPTS; attempt++) {
            try {
                task.run();
                clearOnboardingFailure(userId);
                return;
            } catch (Exception exception) {
                lastFailure = exception;
                logger.warn("Async task {} failed for user {} on attempt {}/{}: {}",
                        taskName, userId, attempt, IDENTITY_SYNC_RETRY_ATTEMPTS, exception.getMessage());
                if (attempt < IDENTITY_SYNC_RETRY_ATTEMPTS) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(250L * attempt);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        lastFailure = interruptedException;
                        break;
                    }
                }
            }
        }
        if (lastFailure != null) {
            markOnboardingFailure(userId, "IDENTITY_SYNC_FAILED", lastFailure.getMessage());
        }
    }

    private void runAfterCommitAsync(Runnable task, Executor executor, String taskName) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submitUserAsyncTask(task, executor, taskName);
                }
            });
            return;
        }
        submitUserAsyncTask(task, executor, taskName);
    }

    private User buildIdentitySnapshot(User userEntity) {
        User snapshot = new User();
        snapshot.setId(userEntity.getId());
        snapshot.setType(userEntity.getType());
        snapshot.setFirstName(userEntity.getFirstName());
        snapshot.setLastName(userEntity.getLastName());
        snapshot.setEmailAddress(userEntity.getEmailAddress());
        snapshot.setEmailAddressValidation(userEntity.getEmailAddressValidation());
        snapshot.setActiveAccount(userEntity.getActiveAccount());
        return snapshot;
    }

    private void markOnboardingFailure(Long userId, String errorCode, String errorMessage) {
        if (userId == null) {
            return;
        }
        userOnboardings.findByUserId(userId).ifPresent(onboarding -> {
            onboarding.setStatus(USER_ONBOARDING_STATUS.FAILED);
            onboarding.setLastErrorCode(errorCode);
            onboarding.setLastErrorMessage(errorMessage);
            onboarding.setLastUpdatedAt(new Timestamp(System.currentTimeMillis()));
            userOnboardings.save(onboarding);
        });
    }

    private void clearOnboardingFailure(Long userId) {
        if (userId == null) {
            return;
        }
        userOnboardings.findByUserId(userId).ifPresent(onboarding -> {
            boolean shouldPersist = onboarding.getLastErrorCode() != null
                    || onboarding.getLastErrorMessage() != null
                    || onboarding.getStatus() == USER_ONBOARDING_STATUS.FAILED;
            if (!shouldPersist) {
                return;
            }
            onboarding.setLastErrorCode(null);
            onboarding.setLastErrorMessage(null);
            onboarding.setLastUpdatedAt(new Timestamp(System.currentTimeMillis()));
            if (onboarding.getStatus() == USER_ONBOARDING_STATUS.FAILED) {
                if (DELIVERY_PERSON.equalsIgnoreCase(onboarding.getUser().getType())) {
                    onboarding.setStatus(USER_ONBOARDING_STATUS.READY_FOR_VALIDATION);
                } else {
                    onboarding.setStatus(USER_ONBOARDING_STATUS.COMPLETED);
                }
            }
            userOnboardings.save(onboarding);
        });
    }

    private String resolveDocumentPath(String directoryPath, String fileName) {
        Path resolvedPath = Paths.get(directoryPath).resolve(fileName);
        return resolvedPath.toString();
    }

    private byte[] readDocumentBytes(Document document, String emailAddress) throws IOException {
        String originalLocation = document.getDocURL();
        String normalizedPathValue = originalLocation.replace("\\", java.io.File.separator);
        if (!originalLocation.equals(normalizedPathValue) && userDocumentStorageService.exists(normalizedPathValue)) {
            logger.info("Normalized legacy document path for user {} and type {} from {} to {}",
                    emailAddress, document.getType(), originalLocation, normalizedPathValue);
            document.setDocURL(normalizedPathValue);
            documents.save(document);
            return userDocumentStorageService.readBytes(normalizedPathValue);
        }
        return userDocumentStorageService.readBytes(originalLocation);
    }

    private String resolveDocumentFileName(Document document) {
        if (document == null || document.getDocURL() == null || document.getDocURL().isBlank()) {
            return null;
        }
        return Paths.get(document.getDocURL()).getFileName().toString();
    }

    private String resolveDocumentContentType(Document document) {
        String fileName = resolveDocumentFileName(document);
        if (fileName == null || !fileName.contains(".")) {
            return "application/octet-stream";
        }
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    private String buildUserUpdateLink(User user) {
        return buildUserUpdateLink(userUpdateTokenService.generateToken(user.getId()));
    }

    private String buildUserUpdateLink(String updateToken) {
        return resolveFrontendBaseUrl() + "/userSignInPage?updateToken=" + updateToken;
    }

    private void attachResumeAccess(UserDTO userDTO, User userEntity) {
        if (userDTO.getOnboarding() == null || userEntity == null) {
            return;
        }
        String resumeToken = userUpdateTokenService.generateToken(userEntity.getId());
        userDTO.getOnboarding().setResumeToken(resumeToken);
        userDTO.getOnboarding().setResumeLink(buildUserUpdateLink(resumeToken));
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
        userOnboardings.findByUserId(user.getId()).ifPresent(onboarding -> {
            if (EnumSet.of(USER_ONBOARDING_STATUS.READY_FOR_VALIDATION, USER_ONBOARDING_STATUS.COMPLETED).contains(onboarding.getStatus())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Onboarding is locked");
            }
        });
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

    private void validateOnboardingRequest(UserOnboardingValidationService.ValidationResult validationResult) {
        if (validationResult.valid()) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join(" ", validationResult.messages()));
    }

    private UserDTO resumeExistingAccountCreate(User existingUser, UserDTO incomingUser, Locale locale) {
        UserOnboarding onboarding = loadOrCreateOnboarding(existingUser);
        if (!isResumableOnboarding(existingUser, onboarding)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        }

        existingUser.setFirstName(incomingUser.getFirstName());
        existingUser.setLastName(incomingUser.getLastName());
        existingUser.setAge(incomingUser.getAge());
        existingUser.setBirthDate(incomingUser.getBirthDate());
        existingUser.setSex(incomingUser.getSex());
        existingUser.setPhone(incomingUser.getPhone());
        existingUser.setPassword(incomingUser.getPassword());
        existingUser.setDeliveryMode(incomingUser.getDeliveryMode());
        users.saveAndFlush(existingUser);

        Timestamp now = new Timestamp(System.currentTimeMillis());
        onboarding.setStatus(USER_ONBOARDING_STATUS.ACCOUNT_CREATED);
        onboarding.setCurrentStep(Math.max(resolveCurrentStep(onboarding), ONBOARDING_STEP_ADDRESS));
        onboarding.setLastUpdatedAt(now);
        onboarding.setLastErrorCode(null);
        onboarding.setLastErrorMessage(null);
        userOnboardings.save(onboarding);
        scheduleIdentityProvisioning(existingUser, incomingUser.getPassword());

        UserDTO response = toUserDetailDTO(existingUser, onboarding);
        attachResumeAccess(response, existingUser);
        runUserMailTask(() -> sendUserOnboardingResumeEmail(response, existingUser, locale));
        return response;
    }

    private void assertDraftOnboardingEditable(User user, UserOnboarding onboarding) {
        if (!isResumableOnboarding(user, onboarding)) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "Onboarding is locked");
        }
    }

    private int resolveNextDraftStep(User userEntity) {
        String deliveryMode = userEntity.getDeliveryMode() == null ? "" : userEntity.getDeliveryMode().trim().toUpperCase(Locale.ROOT);
        Set<DOCUMENT_TYPE> uploadedTypes = userEntity.getDocument() == null
                ? Set.of()
                : userEntity.getDocument().stream()
                .filter(document -> document.getDocumentStatus() != DOCUMENT_STATUS.REJECTED)
                .map(Document::getType)
                .collect(Collectors.toSet());
        boolean hasResidence = userEntity.getPersonalAddress() != null
                && userEntity.getPersonalAddress().stream().anyMatch(address ->
                address.getLine1() != null && !address.getLine1().isBlank()
                        && address.getTown() != null && !address.getTown().isBlank()
                        && address.getZipCode() != null && !address.getZipCode().isBlank()
                        && address.getCountry() != null && !address.getCountry().isBlank());
        if (!hasResidence) {
            return ONBOARDING_STEP_ADDRESS;
        }

        boolean hasUserDocuments = uploadedTypes.containsAll(requiredUserDocuments(deliveryMode));
        if (!hasUserDocuments) {
            return ONBOARDING_STEP_DOCUMENTS;
        }

        if (requiresVehicleDetails(deliveryMode)) {
            boolean hasVehicleDetails = userEntity.getVehicles() != null
                    && userEntity.getVehicles().stream().anyMatch(vehicle ->
                    vehicle.getRegistrationNumber() != null && !vehicle.getRegistrationNumber().isBlank()
                            && vehicle.getBrand() != null && !vehicle.getBrand().isBlank()
                            && vehicle.getModel() != null && !vehicle.getModel().isBlank()
                            && vehicle.getEnergyType() != null && !vehicle.getEnergyType().isBlank());
            boolean hasVehicleDocuments = uploadedTypes.containsAll(requiredVehicleDocuments(deliveryMode));
            if (!hasVehicleDetails || !hasVehicleDocuments) {
                return ONBOARDING_STEP_VEHICLE;
            }
        }
        return ONBOARDING_STEP_SUMMARY;
    }

    private Set<DOCUMENT_TYPE> requiredUserDocuments(String deliveryMode) {
        if ("CAR".equals(deliveryMode) || "SCOOTER".equals(deliveryMode)) {
            return EnumSet.of(
                    DOCUMENT_TYPE.ID,
                    DOCUMENT_TYPE.PICTURE,
                    DOCUMENT_TYPE.DRIVER_LICENCE,
                    DOCUMENT_TYPE.USER_COMPANY_EXTRACT,
                    DOCUMENT_TYPE.USER_COMPANY_INSURANCE
            );
        }
        return EnumSet.of(
                DOCUMENT_TYPE.ID,
                DOCUMENT_TYPE.PICTURE,
                DOCUMENT_TYPE.USER_COMPANY_EXTRACT,
                DOCUMENT_TYPE.USER_COMPANY_INSURANCE
        );
    }

    private Set<DOCUMENT_TYPE> requiredVehicleDocuments(String deliveryMode) {
        if (!requiresVehicleDetails(deliveryMode)) {
            return Set.of();
        }
        return EnumSet.of(DOCUMENT_TYPE.GRAY_CARD, DOCUMENT_TYPE.INSURANCE);
    }

    private boolean requiresVehicleDetails(String deliveryMode) {
        return "CAR".equals(deliveryMode) || "SCOOTER".equals(deliveryMode);
    }

    private boolean isResumableOnboarding(User user, UserOnboarding onboarding) {
        return user != null
                && DELIVERY_PERSON.equalsIgnoreCase(user.getType())
                && !Boolean.TRUE.equals(user.getActiveAccount())
                && onboarding != null
                && EnumSet.of(
                        USER_ONBOARDING_STATUS.ACCOUNT_CREATED,
                        USER_ONBOARDING_STATUS.PROFILE_COMPLETED,
                        USER_ONBOARDING_STATUS.DOCUMENTS_UPLOADED
                ).contains(onboarding.getStatus());
    }

    private int resolveCurrentStep(UserOnboarding onboarding) {
        if (onboarding == null) {
            return ONBOARDING_STEP_PROFILE;
        }
        if (onboarding.getCurrentStep() != null && onboarding.getCurrentStep() > 0) {
            return onboarding.getCurrentStep();
        }
        return switch (onboarding.getStatus()) {
            case ACCOUNT_CREATED -> ONBOARDING_STEP_ADDRESS;
            case PROFILE_COMPLETED -> ONBOARDING_STEP_DOCUMENTS;
            case DOCUMENTS_UPLOADED -> ONBOARDING_STEP_VEHICLE;
            case READY_FOR_VALIDATION, COMPLETED -> ONBOARDING_STEP_SUMMARY;
            case FAILED -> ONBOARDING_STEP_DOCUMENTS;
        };
    }

    private String localizeOnboardingStep(Integer step, Locale locale) {
        int resolvedStep = step == null ? ONBOARDING_STEP_PROFILE : step;
        boolean french = locale != null && "fr".equalsIgnoreCase(locale.getLanguage());
        return switch (resolvedStep) {
            case ONBOARDING_STEP_ADDRESS -> french ? "Adresse" : "Address";
            case ONBOARDING_STEP_DOCUMENTS -> french ? "Documents" : "Documents";
            case ONBOARDING_STEP_VEHICLE -> french ? "Vehicule" : "Vehicle";
            case ONBOARDING_STEP_SUMMARY -> french ? "Recapitulatif" : "Summary";
            default -> french ? "Profil" : "Profile";
        };
    }

    private void updateOnboardingForRejectedDocuments(User user, List<Document> rejectedDocuments) {
        UserOnboarding onboarding = loadOrCreateOnboarding(user);
        onboarding.setStatus(USER_ONBOARDING_STATUS.FAILED);
        onboarding.setCurrentStep(resolveRejectedDocumentsStep(rejectedDocuments));
        onboarding.setLastUpdatedAt(new Timestamp(System.currentTimeMillis()));
        userOnboardings.save(onboarding);
    }

    private int resolveRejectedDocumentsStep(List<Document> rejectedDocuments) {
        boolean hasVehicleDocuments = rejectedDocuments.stream()
                .map(Document::getType)
                .anyMatch(type -> type == DOCUMENT_TYPE.GRAY_CARD || type == DOCUMENT_TYPE.INSURANCE);
        return hasVehicleDocuments ? ONBOARDING_STEP_VEHICLE : ONBOARDING_STEP_DOCUMENTS;
    }

    private void markOnboardingCompleted(User user) {
        UserOnboarding onboarding = loadOrCreateOnboarding(user);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        onboarding.setStatus(USER_ONBOARDING_STATUS.COMPLETED);
        onboarding.setCompletedAt(now);
        onboarding.setCurrentStep(ONBOARDING_STEP_SUMMARY);
        onboarding.setLastUpdatedAt(now);
        onboarding.setLastErrorCode(null);
        onboarding.setLastErrorMessage(null);
        userOnboardings.save(onboarding);
    }

    private void triggerOcrForUploadedDocuments(User userEntity, MultiValueMap<String, MultipartFile> validFilesMap) {
        List<Long> documentIds = userEntity.getDocument().stream()
                .filter(document -> document.getId() != null)
                .filter(document -> document.getDocURL() != null && !document.getDocURL().isBlank())
                .filter(document -> validFilesMap.containsKey(document.getType().name()))
                .filter(this::shouldTriggerOcr)
                .map(Document::getId)
                .toList();

        if (documentIds.isEmpty()) {
            logger.info("No uploaded documents eligible for OCR for user {}", userEntity.getEmailAddress());
            return;
        }

        Runnable trigger = () -> documentIds.forEach(documentOcrOrchestrator::processDocumentAsync);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    logger.info("Triggering OCR after commit for user {} on documents {}", userEntity.getEmailAddress(), documentIds);
                    submitUserAsyncTask(trigger, userAsyncTaskExecutor, "user-ocr");
                }
            });
            return;
        }

        logger.info("Triggering OCR immediately for user {} on documents {}", userEntity.getEmailAddress(), documentIds);
        submitUserAsyncTask(trigger, userAsyncTaskExecutor, "user-ocr");
    }

    private boolean shouldTriggerOcr(Document document) {
        DOCUMENT_OCR_STATUS status = document.getOcrStatus();
        return status == null
                || status == DOCUMENT_OCR_STATUS.PENDING
                || status == DOCUMENT_OCR_STATUS.FAILED
                || status == DOCUMENT_OCR_STATUS.DISABLED;
    }

    private void resetOcrState(Document document) {
        document.setOcrStatus(DOCUMENT_OCR_STATUS.PENDING);
        document.setOcrProvider(null);
        document.setOcrConfidenceScore(null);
        document.setOcrExtractedData(null);
        document.setOcrDocumentType(null);
        document.setOcrDetectedDocumentType(null);
        document.setOcrTypeConsistent(null);
        document.setOcrLastName(null);
        document.setOcrFirstName(null);
        document.setOcrBirthDate(null);
        document.setOcrExpiryDate(null);
        document.setOcrRegistrationNumber(null);
        document.setOcrBrand(null);
        document.setOcrModel(null);
        document.setOcrEnergyType(null);
        document.setOcrHolderName(null);
        document.setOcrCompanyName(null);
        document.setOcrSiren(null);
        document.setOcrInsuranceKind(null);
        document.setOcrIban(null);
        document.setOcrBic(null);
        document.setOcrProcessedAt(null);
        document.setOcrErrorCode(null);
        document.setMatchStatus(null);
        document.setMatchScore(null);
        document.setMatchDetails(null);
    }

    private void submitUserAsyncTask(Runnable task, Executor executor, String taskName) {
        try {
            CompletableFuture.runAsync(task, executor);
        } catch (RejectedExecutionException exception) {
            logger.warn("Dropping async task {} because the executor is saturated", taskName);
        }
    }

    private DocumentValidationResult validateDocumentFile(MultipartFile file, Locale locale) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            return invalidValidation("EMPTY_FILE", localizedValidationMessage("EMPTY_FILE", locale));
        }

        if (file.getSize() > MAX_DOCUMENT_SIZE_BYTES) {
            return invalidValidation("FILE_TOO_LARGE", localizedValidationMessage("FILE_TOO_LARGE", locale));
        }

        String originalFilename = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        int extensionIndex = originalFilename.lastIndexOf('.');
        if (extensionIndex < 0) {
            return invalidValidation("MISSING_EXTENSION", localizedValidationMessage("MISSING_EXTENSION", locale));
        }

        String extension = originalFilename.substring(extensionIndex).toLowerCase(Locale.ROOT);
        if (!ALLOWED_DOCUMENT_EXTENSIONS.contains(extension)) {
            return invalidValidation("INVALID_EXTENSION", localizedValidationMessage("INVALID_EXTENSION", locale));
        }

        String contentType = Optional.ofNullable(file.getContentType()).orElse("").toLowerCase(Locale.ROOT);
        if (!contentType.isBlank() && !ALLOWED_DOCUMENT_CONTENT_TYPES.contains(contentType)) {
            return invalidValidation("INVALID_CONTENT_TYPE", localizedValidationMessage("INVALID_CONTENT_TYPE", locale));
        }

        return new DocumentValidationResult(true, DOCUMENT_VALIDATION_STATUS.PENDING_MANUAL_REVIEW, "PENDING_MANUAL_REVIEW",
                localizedValidationMessage("PENDING_MANUAL_REVIEW", locale));
    }

    private void applyValidationOutcome(Document document, DocumentValidationResult validationResult) {
        document.setValidatedAutomatically(Boolean.TRUE);
        document.setValidationStatus(validationResult.status());
        document.setValidationCode(validationResult.code());
        document.setValidationDetails(validationResult.details());
        clearNormalizedOcrFields(document);
        if (validationResult.valid()) {
            document.setDocumentStatus(DOCUMENT_STATUS.PENDING_VALIDATION);
            document.setReviewComment(null);
            document.setReviewedAt(null);
            document.setReviewedBy(null);
            document.setOcrStatus(DOCUMENT_OCR_STATUS.PENDING);
            document.setOcrProvider(null);
            document.setOcrConfidenceScore(null);
            document.setOcrExtractedData(null);
            document.setOcrProcessedAt(null);
            document.setOcrErrorCode(null);
            document.setMatchStatus(null);
            document.setMatchScore(null);
            document.setMatchDetails(null);
            return;
        }
        document.setDocumentStatus(DOCUMENT_STATUS.REJECTED);
        document.setReviewComment(validationResult.details());
        document.setReviewedAt(new Date());
        document.setReviewedBy(SYSTEM_REVIEWER);
        document.setOcrStatus(DOCUMENT_OCR_STATUS.DISABLED);
        document.setOcrProvider(null);
        document.setOcrConfidenceScore(null);
        document.setOcrExtractedData(null);
        document.setOcrProcessedAt(new Date());
        document.setOcrErrorCode(validationResult.code());
        document.setMatchStatus(DOCUMENT_MATCH_STATUS.NOT_APPLICABLE);
        document.setMatchScore(null);
        document.setMatchDetails(null);
    }

    private DocumentValidationResult invalidValidation(String code, String details) {
        return new DocumentValidationResult(false, DOCUMENT_VALIDATION_STATUS.INVALID_FILE, code, details);
    }

    private String localizedValidationMessage(String code, Locale locale) {
        boolean french = locale != null && "fr".equalsIgnoreCase(locale.getLanguage());
        return switch (code) {
            case "EMPTY_FILE" -> french ? "Fichier vide ou illisible." : "Empty or unreadable file.";
            case "FILE_TOO_LARGE" -> french ? "Fichier trop volumineux. Taille maximale autorisée : 10 Mo." : "File too large. Maximum allowed size: 10 MB.";
            case "MISSING_EXTENSION" -> french ? "Extension de fichier absente ou invalide." : "Missing or invalid file extension.";
            case "INVALID_EXTENSION" -> french ? "Format de fichier non autorisé. Formats acceptés : PNG, JPG, JPEG, WEBP, PDF." : "Unsupported file format. Allowed formats: PNG, JPG, JPEG, WEBP, PDF.";
            case "INVALID_CONTENT_TYPE" -> french ? "Type de fichier non autorisé." : "Unsupported file content type.";
            default -> french ? "Document contrôlé automatiquement. En attente de revue manuelle." : "Document checked automatically. Pending manual review.";
        };
    }

    private record DocumentValidationResult(boolean valid,
                                            DOCUMENT_VALIDATION_STATUS status,
                                            String code,
                                            String details) {
    }

    private void clearNormalizedOcrFields(Document document) {
        document.setOcrDocumentType(null);
        document.setOcrDetectedDocumentType(null);
        document.setOcrTypeConsistent(null);
        document.setOcrLastName(null);
        document.setOcrFirstName(null);
        document.setOcrBirthDate(null);
        document.setOcrExpiryDate(null);
        document.setOcrRegistrationNumber(null);
        document.setOcrBrand(null);
        document.setOcrModel(null);
        document.setOcrEnergyType(null);
        document.setOcrHolderName(null);
        document.setOcrCompanyName(null);
        document.setOcrSiren(null);
        document.setOcrInsuranceKind(null);
        document.setOcrIban(null);
        document.setOcrBic(null);
    }

    private <T> T recordUserOperation(String operation, ThrowingSupplier<T> action) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            T result = action.get();
            stopUserOperationTimer(sample, operation, "success");
            incrementUserOperationCounter(operation, "success");
            return result;
        } catch (RuntimeException exception) {
            stopUserOperationTimer(sample, operation, "failure");
            incrementUserOperationCounter(operation, "failure");
            throw exception;
        } catch (Exception exception) {
            stopUserOperationTimer(sample, operation, "failure");
            incrementUserOperationCounter(operation, "failure");
            throw new RuntimeException(exception);
        }
    }

    private void stopUserOperationTimer(Timer.Sample sample, String operation, String result) {
        sample.stop(Timer.builder("quickdelivery.users.operation.duration")
                .tag("operation", operation)
                .tag("result", result)
                .register(meterRegistry));
    }

    private void incrementUserOperationCounter(String operation, String result) {
        meterRegistry.counter("quickdelivery.users.operation.calls",
                "operation", operation,
                "result", result).increment();
    }

    private void recordUserDocumentContentSize(int sizeInBytes) {
        DistributionSummary.builder("quickdelivery.users.document.content.bytes")
                .baseUnit("bytes")
                .register(meterRegistry)
                .record(sizeInBytes);
    }

    private Double readGauge(String metricName, String... tags) {
        try {
            var search = meterRegistry.find(metricName);
            for (int index = 0; index + 1 < tags.length; index += 2) {
                search = search.tag(tags[index], tags[index + 1]);
            }
            if (search.gauge() != null) {
                return search.gauge().value();
            }
        } catch (Exception ignored) {
            // Ignore unavailable runtime metrics.
        }
        return null;
    }

    private Double sumMetric(String metricName) {
        try {
            return meterRegistry.find(metricName)
                    .meters()
                    .stream()
                    .flatMap(meter -> StreamSupport.stream(meter.measure().spliterator(), false))
                    .mapToDouble(measurement -> ((Measurement) measurement).getValue())
                    .sum();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void collectHttpEndpointMetric(Map<String, HttpEndpointMetricDTO> aggregated, Meter meter) {
        String uri = meter.getId().getTag("uri");
        if (shouldIgnoreHttpUri(uri)) {
            return;
        }
        String method = Optional.ofNullable(meter.getId().getTag("method")).orElse("GET");
        double count = readStatistic(meter, Statistic.COUNT);
        if (count <= 0d) {
            return;
        }
        double maxResponseTimeMs = readStatistic(meter, Statistic.MAX) * 1000d;
        String key = method + " " + uri;
        HttpEndpointMetricDTO endpointMetric = aggregated.computeIfAbsent(key, ignored -> {
            HttpEndpointMetricDTO metric = new HttpEndpointMetricDTO();
            metric.setMethod(method);
            metric.setEndpoint(uri);
            return metric;
        });
        endpointMetric.setRequestCount(endpointMetric.getRequestCount() + count);
        endpointMetric.setMaxResponseTimeMs(Math.max(endpointMetric.getMaxResponseTimeMs(), maxResponseTimeMs));
    }

    private double readStatistic(Meter meter, Statistic statistic) {
        return StreamSupport.stream(meter.measure().spliterator(), false)
                .filter(measurement -> measurement.getStatistic() == statistic)
                .mapToDouble(Measurement::getValue)
                .findFirst()
                .orElse(0d);
    }

    private boolean shouldIgnoreHttpUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return true;
        }
        String normalizedUri = uri.toLowerCase(Locale.ROOT);
        return normalizedUri.startsWith("/actuator")
                || normalizedUri.startsWith("/users/v1/admin")
                || normalizedUri.contains("/admin/http-breakdown")
                || normalizedUri.contains("/admin/metrics")
                || normalizedUri.contains("/admin/log-insights")
                || normalizedUri.contains("/admin/user-overview")
                || "unknown".equals(normalizedUri)
                || "/error".equals(normalizedUri);
    }

    private Double toMegabytes(Double valueInBytes) {
        return valueInBytes == null ? null : valueInBytes / (1024d * 1024d);
    }

    private Double toPercent(Double ratio) {
        return ratio == null ? null : ratio * 100d;
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
