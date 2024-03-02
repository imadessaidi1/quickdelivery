package com.quickdelivery.abstarct.helpers;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

public class FileHelper {

    public static void saveFilesInParallel(MultipartFile[] files, ExecutorService executorService, String path) {
        if (files != null && files.length > 0) {
            Stream.of(files)
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
