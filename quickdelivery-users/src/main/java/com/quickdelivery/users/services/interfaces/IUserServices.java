package com.quickdelivery.users.services.interfaces;

import com.quickdelivery.abstarct.dto.UserDTO;

public interface IUserServices {
    public UserDTO createNewUser(UserDTO user);
    public UserDTO findByID(Long id);
    public UserDTO updateUser(UserDTO user);
    public void deleteUSer(UserDTO user);
}
