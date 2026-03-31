package com.quickdelivery.services.interfaces;

import com.quickdelivery.abstarct.dto.AdminUserOverviewDTO;
import com.quickdelivery.abstarct.dto.DocumentContentDTO;
import com.quickdelivery.abstarct.dto.ServiceHttpBreakdownDTO;
import com.quickdelivery.abstarct.dto.ServiceMetricsDTO;
import com.quickdelivery.abstarct.dto.UserOnboardingDTO;
import com.quickdelivery.abstarct.dto.UserDTO;
import com.quickdelivery.abstarct.dto.UserValidationPageDTO;
import com.quickdelivery.abstarct.dto.VehicleDTO;
import com.quickdelivery.abstarct.parameters.CHECK_STATUS;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;

public interface IUserServices {
    UserDTO createAccount(UserDTO user, Locale locale);
    UserDTO saveOnboardingDraft(UserDTO user, VehicleDTO vehicleDTO, MultiValueMap<String, MultipartFile> filesMap, Locale locale, Integer step);
    UserDTO completeOnboarding(UserDTO user, VehicleDTO vehicleDTO, MultiValueMap<String, MultipartFile> filesMap, Locale locale);
    UserDTO createNewUser(UserDTO user, VehicleDTO vehicleDTO, MultiValueMap<String, MultipartFile> filesMap, Locale locale);
    UserDTO updateNewUser(UserDTO user, VehicleDTO vehicleDTO, MultiValueMap<String, MultipartFile> filesMap, Locale locale);
    UserDTO updateUserByToken(String updateToken, UserDTO user, VehicleDTO vehicleDTO, MultiValueMap<String, MultipartFile> filesMap, Locale locale);
    UserDTO findByID(Long id);
    UserDTO findByUpdateToken(String updateToken);
    UserDTO userValidation(UserDTO user, Locale locale);
    void deleteUSer(UserDTO user);
    CHECK_STATUS validateUserEmail(Long id);
    UserDTO findByEmail(String email);
    UserValidationPageDTO findUsersForValidation(int page, int size);
    UserOnboardingDTO loadOnboardingStatus(String email);
    DocumentContentDTO loadDocumentContent(Long documentId);
    ServiceMetricsDTO loadAdminMetrics();
    AdminUserOverviewDTO loadAdminUserOverview();
    ServiceHttpBreakdownDTO loadAdminHttpBreakdown();
}
