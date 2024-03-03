package com.quickdelivery.services.interfaces;

import com.quickdelivery.abstarct.dto.UserDTO;
import com.quickdelivery.abstarct.dto.VehicleDTO;
import com.quickdelivery.abstarct.parameters.CHECK_STATUS;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

public interface IUserServices {
    public UserDTO createNewUser(UserDTO user, VehicleDTO vehicleDTO, MultiValueMap<String, MultipartFile> filesMap, Locale locale);
    public UserDTO findByID(Long id);
    public UserDTO updateUser(UserDTO user);
    public void deleteUSer(UserDTO user);
    public CHECK_STATUS validateUserEmail(Long id);
    UserDTO findByEmail(String email);
}
