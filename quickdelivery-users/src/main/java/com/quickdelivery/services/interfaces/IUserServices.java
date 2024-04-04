package com.quickdelivery.services.interfaces;

import com.quickdelivery.abstarct.dto.UserDTO;
import com.quickdelivery.abstarct.dto.VehicleDTO;
import com.quickdelivery.abstarct.parameters.CHECK_STATUS;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;

public interface IUserServices {
    UserDTO createNewUser(UserDTO user, VehicleDTO vehicleDTO, MultiValueMap<String, MultipartFile> filesMap, Locale locale);
    UserDTO updateNewUser(UserDTO user, VehicleDTO vehicleDTO, MultiValueMap<String, MultipartFile> filesMap, Locale locale);
    UserDTO findByID(Long id);
    UserDTO userValidation(UserDTO user, Locale locale);
    void deleteUSer(UserDTO user);
    CHECK_STATUS validateUserEmail(Long id);
    UserDTO findByEmail(String email);

    List<UserDTO> findUsersForValidation();
}
