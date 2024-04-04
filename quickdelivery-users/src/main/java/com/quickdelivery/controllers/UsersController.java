package com.quickdelivery.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickdelivery.abstarct.dto.UserDTO;
import com.quickdelivery.abstarct.dto.VehicleDTO;
import com.quickdelivery.abstarct.parameters.CHECK_STATUS;
import com.quickdelivery.services.interfaces.IUserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.HttpStatus;
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
    @Autowired
    private IUserServices userServices;
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
        return userServices.createNewUser(userDTO, vehicleDTO, ((StandardMultipartHttpServletRequest) request).getMultiFileMap(), locale);
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
        return userServices.updateNewUser(userDTO, vehicleDTO, ((StandardMultipartHttpServletRequest) request).getMultiFileMap(), locale);
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
    @GetMapping("/validateEmail{id}")
    public ResponseEntity<Void> validateUserEmail(@RequestParam(name = "id", required = true) Long id){
        CHECK_STATUS status = userServices.validateUserEmail(id);
        if(status.equals(CHECK_STATUS.OK)) {
            String frontendURL = "http://localhost:8080/";
            return ResponseEntity.status(HttpStatus.FOUND).header("Location", frontendURL).build();
        }else{
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    @GetMapping("/usersForValidation")
    public List<UserDTO> usersForValidation(){
        return userServices.findUsersForValidation();
    }
}
