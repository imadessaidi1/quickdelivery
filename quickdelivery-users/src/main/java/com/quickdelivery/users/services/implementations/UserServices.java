package com.quickdelivery.users.services.implementations;

import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.quickdelivery.abstarct.dto.UserDTO;
import com.quickdelivery.abstarct.entities.User;
import com.quickdelivery.abstarct.helpers.FileHelper;
import com.quickdelivery.abstarct.helpers.GeoHelper;
import com.quickdelivery.abstarct.repositories.Users;
import com.quickdelivery.users.services.interfaces.IUserServices;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Transactional
public class UserServices implements IUserServices {
    @Value("${mapquest.geocode.url.part1}")
    private String mapQuestURL1;
    @Value("${mapquest.geocode.url.part2}")
    private String mapQuestURL2;
    @Value("${mapquest.key}")
    private String mapQUestKey;
    @Value("${mapquest.key}")
    private String userDocPath;
    @Value("${mapquest.key}")
    private String userVehiclePath;
    @Autowired
    private Users users;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private GeoApiContext geoApiContext;
    @Override
    public UserDTO createNewUser(UserDTO user, MultipartFile[] userFiles, MultipartFile[] vehicleFiles, Locale locale) {
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        FileHelper.saveFilesInParallel(userFiles, executorService, userDocPath);
        FileHelper.saveFilesInParallel(vehicleFiles, executorService, userVehiclePath);
        executorService.shutdown();

        user.getPersonalAddress().stream().forEach(addressDTO -> {
            try {
                GeoHelper.AddressGeoCoding(geoApiContext, addressDTO);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ApiException e) {
                throw new RuntimeException(e);
            }
        });
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
