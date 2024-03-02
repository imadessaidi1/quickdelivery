package com.quickdelivery.users.services.interfaces;

import com.quickdelivery.abstarct.dto.UserDTO;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

public interface IUserServices {
    public UserDTO createNewUser(UserDTO user, MultipartFile[] userFiles, MultipartFile[] vehicleFiles, Locale locale);
    public UserDTO findByID(Long id);
    public UserDTO updateUser(UserDTO user);
    public void deleteUSer(UserDTO user);
}
