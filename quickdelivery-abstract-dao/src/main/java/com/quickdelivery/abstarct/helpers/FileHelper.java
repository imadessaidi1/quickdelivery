package com.quickdelivery.abstarct.helpers;

import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

public class FileHelper {

    public static void saveFilesInParallel(MultiValueMap<String, MultipartFile> filesMap, ExecutorService executorService, String path) {
        if (filesMap != null) {
            filesMap.entrySet().stream()
                    .flatMap(entry -> entry.getValue().stream())
                        .forEach(file -> executorService.submit(() -> saveFile(file, path)));
        }
    }

    private static void saveFile(MultipartFile file,String filePath) {
        if (!file.isEmpty()) {
            try {
                byte[] bytes = file.getBytes();
                Path path = Paths.get(filePath + file.getOriginalFilename());
                Files.write(path, bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
