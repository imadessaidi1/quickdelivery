package com.quickdelivery.users.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickdelivery.abstarct.dto.PackageDTO;
import com.quickdelivery.abstarct.dto.UserDTO;
import com.quickdelivery.abstarct.dto.VehicleDTO;
import com.quickdelivery.users.services.interfaces.IUserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import java.util.Locale;
import java.util.Map;


@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/users/v1")
public class UsersController {
    //@Value("${spring.application.name}")
    private String appname;
    //@Value("${spring.neo4j.uri}")
    private String neo4jUri;
    @Autowired
    private IUserServices userServices;
    @PostMapping("/create")
    public UserDTO createUser(MultipartHttpServletRequest request, @RequestParam("user") String user,
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
        userServices.createNewUser(userDTO, vehicleDTO, ((StandardMultipartHttpServletRequest) request).getMultiFileMap(), locale);
        return userDTO;
        //return
    }
    @PutMapping("/update")
    public UserDTO updateUser(@RequestBody UserDTO user){
        return userServices.updateUser(user);
    }
    @DeleteMapping("/update")
    public void deleteUser(@RequestBody UserDTO user){
        userServices.deleteUSer(user);
    }
    @GetMapping("/user{id}")
    public UserDTO findUserById(@RequestParam(name = "id", required = true) Long id){
        return userServices.findByID(id);
    }

    @GetMapping("/hello")
    public String findUserById(){
        return "hello App : "+appname+", URI : "+neo4jUri;
    }

}
