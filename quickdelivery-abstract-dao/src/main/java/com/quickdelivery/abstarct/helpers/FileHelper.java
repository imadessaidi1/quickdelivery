package com.quickdelivery.abstarct.helpers;

import com.quickdelivery.abstarct.entities.Document;
import com.quickdelivery.abstarct.parameters.DOCUMENT_TYPE;
import com.quickdelivery.abstarct.repositories.Users;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

public class FileHelper {

    public static List<Document> saveFilesInParallel(MultiValueMap<String, MultipartFile> filesMap, ExecutorService executorService, String path, String userID) {
        List<Document> documentList = new ArrayList<>();
        if (filesMap != null) {
            File userDirectory = new File(path+(userID.replace('.','_')));
            boolean dirCreation = userDirectory.mkdir();
            if(dirCreation){
                filesMap.entrySet().stream()
                        .forEach(entry -> {
                            String fileName = entry.getKey();
                            MultipartFile file = entry.getValue().get(0);
                            executorService.submit(() -> {
                                Document document = saveFile(file, userDirectory.getPath(), fileName);
                                if(document != null)
                                    documentList.add(document);
                            });
                        });
            }
        }
        return documentList;
    }

    private static Document saveFile(MultipartFile file,String filePath, String fileName) {
        if (!file.isEmpty()) {
            Document document = new Document();
            try {
                String currentFileName = file.getOriginalFilename();
                String newFileName = fileName+currentFileName.substring(currentFileName.lastIndexOf('.'));
                byte[] bytes = file.getBytes();
                Path path = Paths.get(filePath).resolve(newFileName);
                Files.write(path, bytes);
                document.setType(DOCUMENT_TYPE.valueOf(fileName));
                document.setDocURL(filePath+"\\"+newFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return document;
        }
        return null;
    }

}
