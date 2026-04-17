package com.quickdelivery.controllers;

import com.quickdelivery.abstarct.dto.AdminUserOverviewDTO;
import com.quickdelivery.abstarct.dto.AddressBookEntryDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickdelivery.PublicUrlResolver;
import com.quickdelivery.abstarct.dto.DocumentContentDTO;
import com.quickdelivery.abstarct.dto.PublicRegistrationStatusDTO;
import com.quickdelivery.abstarct.dto.ServiceHttpBreakdownDTO;
import com.quickdelivery.abstarct.dto.ServiceLogInsightsDTO;
import com.quickdelivery.abstarct.dto.ServiceMetricsDTO;
import com.quickdelivery.abstarct.dto.UserAccountCreateRequestDTO;
import com.quickdelivery.abstarct.dto.UserOnboardingDTO;
import com.quickdelivery.abstarct.dto.UserDTO;
import com.quickdelivery.abstarct.dto.UserValidationPageDTO;
import com.quickdelivery.abstarct.dto.VehicleDTO;
import com.quickdelivery.abstarct.parameters.CHECK_STATUS;
import com.quickdelivery.abstarct.security.CaptchaVerificationService;
import com.quickdelivery.observability.RuntimeLogMonitor;
import com.quickdelivery.services.interfaces.IAddressBookService;
import com.quickdelivery.services.interfaces.IUserServices;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import java.util.List;
import java.util.Locale;


@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@EnableAspectJAutoProxy
@RequestMapping("/users/v1")
public class UsersController {
    private static final Logger logger = LoggerFactory.getLogger(UsersController.class);
    @Autowired
    private IUserServices userServices;
    @Autowired
    private IAddressBookService addressBookService;
    @Autowired
    private RuntimeLogMonitor runtimeLogMonitor;
    @Autowired
    private CaptchaVerificationService captchaVerificationService;
    @Value("${quickdelivery.frontend.base-url:}")
    private String frontendBaseUrl;
    @PostMapping("/create")
    public UserDTO createUser(MultipartHttpServletRequest request, @RequestParam("user") String user,
                              @RequestParam("vehicle") String vehicle,
                              @RequestParam("locale") Locale locale){
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        UserDTO userDTO = null;
        VehicleDTO vehicleDTO = null;
        try {
            userDTO = objectMapper.readValue(user, UserDTO.class);
            vehicleDTO = objectMapper.readValue(vehicle, VehicleDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        StandardMultipartHttpServletRequest multipartRequest = (StandardMultipartHttpServletRequest) request;
        logger.info("createUser multipart file keys: {}", multipartRequest.getMultiFileMap().keySet());
        return userServices.createNewUser(userDTO, vehicleDTO, multipartRequest.getMultiFileMap(), locale);
    }
    @PostMapping("/create-account")
    public UserDTO createAccount(@RequestBody UserAccountCreateRequestDTO request,
                                 @RequestHeader(value = "X-Captcha-Token", required = false) String captchaToken,
                                 HttpServletRequest servletRequest) {
        captchaVerificationService.validateOrThrow(
                captchaToken,
                captchaVerificationService.resolveClientIp(servletRequest.getHeader("X-Forwarded-For"), servletRequest.getRemoteAddr()),
                "users-create-account"
        );
        Locale locale = request.getLocale() == null ? Locale.getDefault() : request.getLocale();
        return userServices.createAccount(request.getUser(), locale);
    }
    @PostMapping("/complete-onboarding")
    public UserDTO completeOnboarding(MultipartHttpServletRequest request,
                                      @RequestParam("user") String user,
                                      @RequestParam("vehicle") String vehicle,
                                      @RequestParam("locale") Locale locale) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        UserDTO userDTO;
        VehicleDTO vehicleDTO;
        try {
            userDTO = objectMapper.readValue(user, UserDTO.class);
            vehicleDTO = objectMapper.readValue(vehicle, VehicleDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        StandardMultipartHttpServletRequest multipartRequest = (StandardMultipartHttpServletRequest) request;
        logger.info("completeOnboarding multipart file keys: {}", multipartRequest.getMultiFileMap().keySet());
        return userServices.completeOnboarding(userDTO, vehicleDTO, multipartRequest.getMultiFileMap(), locale);
    }
    @PostMapping("/save-onboarding-draft")
    public UserDTO saveOnboardingDraft(MultipartHttpServletRequest request,
                                       @RequestParam("user") String user,
                                       @RequestParam("vehicle") String vehicle,
                                       @RequestParam("locale") Locale locale,
                                       @RequestParam("step") Integer step) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        UserDTO userDTO;
        VehicleDTO vehicleDTO;
        try {
            userDTO = objectMapper.readValue(user, UserDTO.class);
            vehicleDTO = objectMapper.readValue(vehicle, VehicleDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        StandardMultipartHttpServletRequest multipartRequest = (StandardMultipartHttpServletRequest) request;
        logger.info("saveOnboardingDraft step={} multipart file keys: {}", step, multipartRequest.getMultiFileMap().keySet());
        return userServices.saveOnboardingDraft(userDTO, vehicleDTO, multipartRequest.getMultiFileMap(), locale, step);
    }
    @PostMapping("/update")
    public UserDTO updateUser(MultipartHttpServletRequest request, @RequestParam("user") String user,
                              @RequestParam("vehicle") String vehicle,
                              @RequestParam("locale") Locale locale){
        ObjectMapper objectMapper = new ObjectMapper();
        UserDTO userDTO = null;
        VehicleDTO vehicleDTO = null;
        try {
            userDTO = objectMapper.readValue(user, UserDTO.class);
            vehicleDTO = objectMapper.readValue(vehicle, VehicleDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        StandardMultipartHttpServletRequest multipartRequest = (StandardMultipartHttpServletRequest) request;
        logger.info("updateUser multipart file keys: {}", multipartRequest.getMultiFileMap().keySet());
        return userServices.updateNewUser(userDTO, vehicleDTO, multipartRequest.getMultiFileMap(), locale);
    }
    @PostMapping("/public-update")
    public UserDTO publicUpdateUser(MultipartHttpServletRequest request, @RequestParam("updateToken") String updateToken,
                                    @RequestParam("user") String user,
                                    @RequestParam("vehicle") String vehicle,
                                    @RequestParam("locale") Locale locale){
        ObjectMapper objectMapper = new ObjectMapper();
        UserDTO userDTO = null;
        VehicleDTO vehicleDTO = null;
        try {
            userDTO = objectMapper.readValue(user, UserDTO.class);
            vehicleDTO = objectMapper.readValue(vehicle, VehicleDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        StandardMultipartHttpServletRequest multipartRequest = (StandardMultipartHttpServletRequest) request;
        logger.info("publicUpdateUser multipart file keys: {}", multipartRequest.getMultiFileMap().keySet());
        return userServices.updateUserByToken(updateToken, userDTO, vehicleDTO, multipartRequest.getMultiFileMap(), locale);
    }
    @PutMapping("/validateUser")
    public UserDTO validateUser(@RequestParam("user") String user,
                              @RequestParam("locale") Locale locale){
        ObjectMapper objectMapper = new ObjectMapper();
        UserDTO userDTO = null;
        try {
            userDTO = objectMapper.readValue(user, UserDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return userServices.userValidation(userDTO, locale);
    }
    @DeleteMapping("/update")
    public void deleteUser(@RequestBody UserDTO user){
        userServices.deleteUSer(user);
    }
    @GetMapping("/user{id}")
    public UserDTO findUserById(@RequestParam(name = "id", required = true) Long id){
        return userServices.findByID(id);
    }
    @GetMapping("/userByEmail{email}")
    public UserDTO findUserByEmail(@RequestParam(name = "email", required = true) String email){
        return userServices.findByEmail(email);
    }
    @GetMapping("/public-registration-status")
    public PublicRegistrationStatusDTO publicRegistrationStatus(@RequestParam(name = "email") String email) {
        return userServices.loadPublicRegistrationStatus(email);
    }
    @GetMapping("/public-update-profile")
    public UserDTO findUserByUpdateToken(@RequestParam(name = "updateToken", required = true) String updateToken){
        return userServices.findByUpdateToken(updateToken);
    }
    @GetMapping("/validateEmail")
    public ResponseEntity<Void> validateUserEmail(@RequestParam(name = "id", required = true) Long id){
        CHECK_STATUS status = userServices.validateUserEmail(id);
        if(status.equals(CHECK_STATUS.OK)) {
            String frontendURL = PublicUrlResolver.resolvePreferredFrontendBaseUrl(frontendBaseUrl);
            return ResponseEntity.status(HttpStatus.FOUND).header("Location", frontendURL).build();
        }else{
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    @GetMapping("/usersForValidation")
    public UserValidationPageDTO usersForValidation(@RequestParam(name = "page", defaultValue = "0") int page,
                                                    @RequestParam(name = "size", defaultValue = "12") int size){
        return userServices.findUsersForValidation(page, size);
    }
    @GetMapping("/onboarding-status")
    public UserOnboardingDTO onboardingStatus(@RequestParam(name = "email") String email) {
        return userServices.loadOnboardingStatus(email);
    }

    @GetMapping("/address-book")
    public List<AddressBookEntryDTO> searchAddressBook(@RequestParam(name = "ownerUserId") Long ownerUserId,
                                                       @RequestParam(name = "q", defaultValue = "") String query,
                                                       @RequestParam(name = "limit", required = false) Integer limit) {
        return addressBookService.search(ownerUserId, query, limit);
    }

    @PostMapping("/address-book")
    public AddressBookEntryDTO saveAddressBookEntry(@RequestParam(name = "ownerUserId") Long ownerUserId,
                                                    @RequestBody AddressBookEntryDTO request) {
        return addressBookService.save(ownerUserId, request);
    }

    @GetMapping("/document-content")
    public ResponseEntity<byte[]> documentContent(@RequestParam(name = "documentId") Long documentId) {
        DocumentContentDTO documentContent = userServices.loadDocumentContent(documentId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(documentContent.getContentType()));
        if (documentContent.getFileName() != null && !documentContent.getFileName().isBlank()) {
            headers.setContentDispositionFormData("inline", documentContent.getFileName());
        }
        return new ResponseEntity<>(documentContent.getData(), headers, HttpStatus.OK);
    }

    @GetMapping("/admin/metrics")
    public ServiceMetricsDTO adminMetrics() {
        return userServices.loadAdminMetrics();
    }

    @GetMapping("/admin/user-overview")
    public AdminUserOverviewDTO adminUserOverview() {
        return userServices.loadAdminUserOverview();
    }

    @GetMapping("/admin/http-breakdown")
    public ServiceHttpBreakdownDTO adminHttpBreakdown() {
        return userServices.loadAdminHttpBreakdown();
    }

    @GetMapping("/admin/log-insights")
    public ServiceLogInsightsDTO adminLogInsights() {
        return runtimeLogMonitor.snapshot("users-service");
    }
}
