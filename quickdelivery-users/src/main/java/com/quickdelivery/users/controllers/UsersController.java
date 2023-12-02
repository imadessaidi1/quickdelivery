package com.quickdelivery.users.controllers;

import com.quickdelivery.abstarct.dto.UserDTO;
import com.quickdelivery.users.services.interfaces.IUserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/users/v1")
public class UsersController {
    //@Value("${spring.application.name}")
    private String appname;
    //@Value("${spring.neo4j.uri}")
    private String neo4jUri;
    @Autowired
    private IUserServices userServices;
    @PostMapping("/create")
    public UserDTO createUser(@RequestBody UserDTO user){
        return userServices.createNewUser(user);
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
