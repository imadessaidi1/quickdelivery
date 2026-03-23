package com.quickdelivery.abstarct.helpers;

import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileHelper {

    public static void saveFilesInParallel(MultiValueMap<String, MultipartFile> filesMap, String path, boolean isForUpdate) {
        if (filesMap != null) {
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            File userDirectory = new File(path);
            boolean dirCreation = userDirectory.mkdirs();
            if(dirCreation || isForUpdate){
                filesMap.entrySet().stream()
                        .forEach(entry -> {
                            String fileName = entry.getKey();
                            MultipartFile file = entry.getValue().get(0);
                            executorService.submit(() -> {
                                saveFile(file, userDirectory.getPath(), fileName);
                            });
                        });
            }
            executorService.shutdown();
        }
    }

    private static void saveFile(MultipartFile file,String filePath, String fileName) {
        if (!file.isEmpty()) {
            try {
                String currentFileName = file.getOriginalFilename();
                String newFileName = fileName+currentFileName.substring(currentFileName.lastIndexOf('.'));
                byte[] bytes = file.getBytes();
                Path path = Paths.get(filePath).resolve(newFileName);
                Files.write(path, bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
