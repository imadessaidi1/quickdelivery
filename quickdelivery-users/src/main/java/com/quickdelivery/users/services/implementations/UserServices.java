package com.quickdelivery.users.services.implementations;

import com.quickdelivery.abstarct.dto.UserDTO;
import com.quickdelivery.abstarct.entities.User;
import com.quickdelivery.abstarct.helpers.GeoHelper;
import com.quickdelivery.abstarct.repositories.Users;
import com.quickdelivery.users.services.interfaces.IUserServices;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;

@Service
@Transactional
public class UserServices implements IUserServices {
    @Value("${mapquest.geocode.url.part1}")
    private String mapQuestURL1;
    @Value("${mapquest.geocode.url.part2}")
    private String mapQuestURL2;
    @Value("${mapquest.key}")
    private String mapQUestKey;
    @Autowired
    private Users users;
    @Autowired
    private ModelMapper modelMapper;
    @Override
    public UserDTO createNewUser(UserDTO user) {
        user.getPersonalAddress().stream().forEach(addressDTO -> GeoHelper.AdressGeoCoding(addressDTO, mapQuestURL1, mapQUestKey, mapQuestURL2));
        User userEntity = modelMapper.map(user,User.class);
        userEntity.getPersonalAddress().stream().forEach(address -> address.setResidents(userEntity));
        users.save(userEntity);
        userEntity.getPersonalAddress().stream().forEach(address -> address.getResidents().setPersonalAddress(new HashSet<>()));
        user.setId(userEntity.getId());
        user.setVersion(userEntity.getVersion());
        return user;
    }

    @Override
    public UserDTO findByID(Long id) {
        return modelMapper.map(users.findById(id).get(),UserDTO.class);
    }

    @Override
    public UserDTO updateUser(UserDTO user) {
        User userEntity = users.save(modelMapper.map(user,User.class));
        return modelMapper.map(userEntity,UserDTO.class);
    }

    @Override
    public void deleteUSer(UserDTO user) {
         users.delete(modelMapper.map(user,User.class));
    }
}
