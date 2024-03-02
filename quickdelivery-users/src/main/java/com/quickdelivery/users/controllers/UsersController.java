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

import java.util.Locale;


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
                              @RequestParam(value = "files", required = false) MultipartFile[] userFiles,
                              @RequestParam("vehicle") String vehicle,
                              @RequestParam(value = "vehicleFiles", required = false) MultipartFile[] vehicleFiles,
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
        return userDTO;
        //return userServices.createNewUser(userDTO, userFiles, vehicleFiles, locale);
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
