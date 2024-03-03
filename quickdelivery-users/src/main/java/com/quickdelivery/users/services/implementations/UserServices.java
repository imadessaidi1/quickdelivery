package com.quickdelivery.users.services.implementations;

import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.quickdelivery.abstarct.dto.UserDTO;
import com.quickdelivery.abstarct.dto.VehicleDTO;
import com.quickdelivery.abstarct.entities.Document;
import com.quickdelivery.abstarct.entities.User;
import com.quickdelivery.abstarct.entities.Vehicle;
import com.quickdelivery.abstarct.helpers.FileHelper;
import com.quickdelivery.abstarct.helpers.GeoHelper;
import com.quickdelivery.abstarct.parameters.DOCUMENT_TYPE;
import com.quickdelivery.abstarct.repositories.Documents;
import com.quickdelivery.abstarct.repositories.Users;
import com.quickdelivery.users.services.interfaces.IUserServices;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    @Value("${user.docs.directory}")
    private String userDocPath;
    @Value("${mapquest.key}")
    private String userVehiclePath;
    @Autowired
    private Users users;
    @Autowired
    private Documents documents;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private GeoApiContext geoApiContext;
    @Override
    public UserDTO createNewUser(UserDTO user, VehicleDTO vehicleDTO, MultiValueMap<String, MultipartFile> filesMap, Locale locale) {
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
        Vehicle vehicle = modelMapper.map(vehicleDTO, Vehicle.class);
        vehicle.setUser(userEntity);
        userEntity.getVehicles().add(vehicle);
        String filesPath = userDocPath+(user.getEmailAddress().replace('.','_'));
        filesMap.entrySet().stream()
                .forEach(entry -> {
                    String fileName = entry.getKey();
                    MultipartFile file = entry.getValue().get(0);
                    String currentFileName = file.getOriginalFilename();
                    String newFileName = fileName+currentFileName.substring(currentFileName.lastIndexOf('.'));
                    Document document = new Document();
                    document.setType(DOCUMENT_TYPE.valueOf(fileName));
                    document.setDocURL(filesPath+"\\"+newFileName);
                    document.setUser(userEntity);
                    userEntity.getDocument().add(document);
                });
        users.save(userEntity);
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        saveFilesInParallel(filesMap, executorService, userDocPath, user.getEmailAddress(), userEntity.getId());
        executorService.shutdown();
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

    private void saveFilesInParallel(MultiValueMap<String, MultipartFile> filesMap, ExecutorService executorService, String path, String userEmail, Long userID) {
        if (filesMap != null) {
            File userDirectory = new File(path+(userEmail.replace('.','_')));
            boolean dirCreation = userDirectory.mkdir();
            if(dirCreation){
                filesMap.entrySet().stream()
                        .forEach(entry -> {
                            String fileName = entry.getKey();
                            MultipartFile file = entry.getValue().get(0);
                            executorService.submit(() -> {
                                saveFile(file, userDirectory.getPath(), fileName, userID);
                            });
                        });
            }
        }
    }

    private void saveFile(MultipartFile file,String filePath, String fileName, Long userID) {
        if (!file.isEmpty()) {
            try {
                String currentFileName = file.getOriginalFilename();
                String newFileName = fileName+currentFileName.substring(currentFileName.lastIndexOf('.'));
                byte[] bytes = file.getBytes();
                Path path = Paths.get(filePath).resolve(newFileName);
                Files.write(path, bytes);
                User user = users.findById(userID).get();
                Document document = new Document();
                document.setType(DOCUMENT_TYPE.valueOf(fileName));
                document.setDocURL(filePath+"/"+newFileName);
                document.setUser(user);
                user.getDocument().add(document);
                users.save(user);
                System.out.println("Document enregistré : "+document.getId());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
