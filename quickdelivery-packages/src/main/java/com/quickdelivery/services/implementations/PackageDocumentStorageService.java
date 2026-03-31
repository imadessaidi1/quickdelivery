package com.quickdelivery.services.implementations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Locale;

@Service
public class PackageDocumentStorageService {
    private static final Logger logger = LoggerFactory.getLogger(PackageDocumentStorageService.class);

    private final String storageMode;
    private final String bucketName;
    private final S3Client s3Client;

    public PackageDocumentStorageService(@Value("${quickdelivery.package-docs.mode:local}") String storageMode,
                                         @Value("${quickdelivery.aws.s3.bucket:}") String bucketName,
                                         ObjectProvider<S3Client> s3ClientProvider) {
        this.storageMode = storageMode == null ? "local" : storageMode.trim().toLowerCase(Locale.ROOT);
        this.bucketName = bucketName == null ? "" : bucketName.trim();
        this.s3Client = s3ClientProvider.getIfAvailable();
    }

    public void saveFiles(MultiValueMap<String, MultipartFile> filesMap, String directoryPath, boolean isForUpdate) {
        if (filesMap == null || filesMap.isEmpty()) {
            return;
        }
        if (isS3Enabled()) {
            saveFilesToS3(filesMap, directoryPath);
            return;
        }
        saveFilesLocally(filesMap, directoryPath, isForUpdate);
    }

    public void writeBytes(String location, byte[] bytes, String contentType) throws IOException {
        if (isS3Enabled()) {
            try {
                s3Client.putObject(PutObjectRequest.builder()
                                .bucket(bucketName)
                                .key(toStorageKey(location))
                                .contentType(contentType)
                                .build(),
                        RequestBody.fromBytes(bytes));
                return;
            } catch (S3Exception exception) {
                throw new IOException("Unable to store package document in S3", exception);
            }
        }
        Path path = Paths.get(location);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, bytes);
    }

    public byte[] readBytes(String location) throws IOException {
        if (location == null || location.isBlank()) {
            throw new IOException("Document location is blank");
        }
        Path path = Paths.get(location);
        if (Files.exists(path)) {
            return Files.readAllBytes(path);
        }
        Path normalizedPath = Paths.get(location.replace("\\", File.separator));
        if (Files.exists(normalizedPath)) {
            return Files.readAllBytes(normalizedPath);
        }
        if (isS3Enabled()) {
            try {
                ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(toStorageKey(location))
                        .build());
                return response.asByteArray();
            } catch (NoSuchKeyException exception) {
                throw new IOException("Package document file not found in S3", exception);
            } catch (S3Exception exception) {
                throw new IOException("Unable to read package document from S3", exception);
            }
        }
        throw new IOException("Package document file not found");
    }

    public boolean exists(String location) {
        if (location == null || location.isBlank()) {
            return false;
        }
        if (Files.exists(Paths.get(location)) || Files.exists(Paths.get(location.replace("\\", File.separator)))) {
            return true;
        }
        if (!isS3Enabled()) {
            return false;
        }
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(toStorageKey(location))
                    .build());
            return true;
        } catch (S3Exception exception) {
            return false;
        }
    }

    public Path materializeToTempFile(String location) throws IOException {
        Path localPath = Paths.get(location);
        if (Files.exists(localPath)) {
            return localPath;
        }
        byte[] data = readBytes(location);
        String fileName = localPath.getFileName() == null ? "package-document.tmp" : localPath.getFileName().toString();
        String prefix = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        String suffix = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : ".tmp";
        if (prefix.length() < 3) {
            prefix = "pkg";
        }
        Path tempFile = Files.createTempFile(prefix + "-", suffix);
        Files.write(tempFile, data);
        tempFile.toFile().deleteOnExit();
        return tempFile;
    }

    public void deleteDirectory(String directoryPath) {
        if (directoryPath == null || directoryPath.isBlank()) {
            return;
        }
        if (isS3Enabled()) {
            String prefix = toStorageKey(Paths.get(directoryPath).resolve("placeholder").toString());
            String s3Prefix = prefix.substring(0, prefix.lastIndexOf('/') + 1);
            try {
                s3Client.listObjectsV2Paginator(builder -> builder.bucket(bucketName).prefix(s3Prefix))
                        .contents()
                        .forEach(object -> s3Client.deleteObject(DeleteObjectRequest.builder()
                                .bucket(bucketName)
                                .key(object.key())
                                .build()));
            } catch (Exception exception) {
                logger.warn("Unable to cleanup package docs in S3 under {}: {}", s3Prefix, exception.getMessage());
            }
        }
        Path directory = Paths.get(directoryPath);
        if (!Files.exists(directory)) {
            return;
        }
        try (var pathStream = Files.walk(directory)) {
            pathStream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    logger.warn("Unable to delete {} during package cleanup: {}", path, exception.getMessage());
                }
            });
        } catch (IOException exception) {
            logger.warn("Unable to cleanup package directory {}: {}", directoryPath, exception.getMessage());
        }
    }

    private void saveFilesToS3(MultiValueMap<String, MultipartFile> filesMap, String directoryPath) {
        filesMap.forEach((fileName, multipartFiles) -> {
            if (multipartFiles == null || multipartFiles.isEmpty() || multipartFiles.get(0) == null) {
                return;
            }
            MultipartFile file = multipartFiles.get(0);
            if (file.isEmpty()) {
                return;
            }
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                return;
            }
            String newFileName = fileName + originalFilename.substring(originalFilename.lastIndexOf('.'));
            String location = Paths.get(directoryPath).resolve(newFileName).toString();
            try {
                s3Client.putObject(PutObjectRequest.builder()
                                .bucket(bucketName)
                                .key(toStorageKey(location))
                                .contentType(file.getContentType())
                                .build(),
                        RequestBody.fromBytes(file.getBytes()));
            } catch (Exception exception) {
                throw new IllegalStateException("Unable to store package document in S3", exception);
            }
        });
    }

    private void saveFilesLocally(MultiValueMap<String, MultipartFile> filesMap, String directoryPath, boolean isForUpdate) {
        File directory = new File(directoryPath);
        boolean dirCreation = directory.mkdirs();
        if (!dirCreation && !isForUpdate && !directory.exists()) {
            throw new IllegalStateException("Unable to create package directory " + directoryPath);
        }
        filesMap.forEach((fileName, multipartFiles) -> {
            if (multipartFiles == null || multipartFiles.isEmpty() || multipartFiles.get(0) == null) {
                return;
            }
            MultipartFile file = multipartFiles.get(0);
            if (file.isEmpty()) {
                return;
            }
            try {
                String currentFileName = file.getOriginalFilename();
                if (currentFileName == null || !currentFileName.contains(".")) {
                    return;
                }
                String newFileName = fileName + currentFileName.substring(currentFileName.lastIndexOf('.'));
                Files.write(Paths.get(directoryPath).resolve(newFileName), file.getBytes());
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to store package document locally", exception);
            }
        });
    }

    private boolean isS3Enabled() {
        return "s3".equals(storageMode) && !bucketName.isBlank() && s3Client != null;
    }

    private String toStorageKey(String location) {
        String normalized = location.replace("\\", "/").trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }
}
