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
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class UserDocumentStorageService {
    private static final Logger logger = LoggerFactory.getLogger(UserDocumentStorageService.class);

    private final String storageMode;
    private final String bucketName;
    private final S3Client s3Client;

    public UserDocumentStorageService(@Value("${quickdelivery.user-docs.mode:local}") String storageMode,
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

    public byte[] readBytes(String documentLocation) throws IOException {
        if (documentLocation == null || documentLocation.isBlank()) {
            throw new IOException("Document location is blank");
        }

        Path primaryPath = Paths.get(documentLocation);
        if (Files.exists(primaryPath)) {
            return Files.readAllBytes(primaryPath);
        }

        String normalizedPathValue = documentLocation.replace("\\", File.separator);
        Path normalizedPath = Paths.get(normalizedPathValue);
        if (Files.exists(normalizedPath)) {
            return Files.readAllBytes(normalizedPath);
        }

        if (isS3Enabled()) {
            String key = toStorageKey(documentLocation);
            try {
                ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build());
                return response.asByteArray();
            } catch (NoSuchKeyException exception) {
                throw new IOException("Document file not found in S3 at key " + key, exception);
            } catch (S3Exception exception) {
                throw new IOException("Unable to read document from S3", exception);
            }
        }

        throw new IOException("Document file not found at " + documentLocation);
    }

    public boolean exists(String documentLocation) {
        if (documentLocation == null || documentLocation.isBlank()) {
            return false;
        }
        Path primaryPath = Paths.get(documentLocation);
        if (Files.exists(primaryPath)) {
            return true;
        }

        Path normalizedPath = Paths.get(documentLocation.replace("\\", File.separator));
        if (Files.exists(normalizedPath)) {
            return true;
        }

        if (!isS3Enabled()) {
            return false;
        }

        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(toStorageKey(documentLocation))
                    .build());
            return true;
        } catch (S3Exception exception) {
            return false;
        }
    }

    public void deleteDirectory(String directoryPath) {
        if (directoryPath == null || directoryPath.isBlank()) {
            return;
        }
        if (isS3Enabled()) {
            deleteDirectoryFromS3(directoryPath);
        }
        deleteDirectoryLocally(directoryPath);
    }

    private void saveFilesToS3(MultiValueMap<String, MultipartFile> filesMap, String directoryPath) {
        List<Map.Entry<String, List<MultipartFile>>> entries = filesMap.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue()))
                .toList();

        entries.parallelStream().forEach(entry -> {
            List<MultipartFile> multipartFiles = entry.getValue();
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
            String newFileName = entry.getKey() + originalFilename.substring(originalFilename.lastIndexOf('.'));
            String location = Paths.get(directoryPath).resolve(newFileName).toString();
            String key = toStorageKey(location);
            try {
                s3Client.putObject(PutObjectRequest.builder()
                                .bucket(bucketName)
                                .key(key)
                                .contentType(file.getContentType())
                                .build(),
                        RequestBody.fromBytes(file.getBytes()));
            } catch (Exception exception) {
                throw new IllegalStateException("Unable to store document in S3 at key " + key, exception);
            }
        });
    }

    private void saveFilesLocally(MultiValueMap<String, MultipartFile> filesMap, String directoryPath, boolean isForUpdate) {
        File userDirectory = new File(directoryPath);
        boolean dirCreation = userDirectory.mkdirs();
        if (!dirCreation && !isForUpdate && !userDirectory.exists()) {
            throw new IllegalStateException("Unable to create local document directory " + directoryPath);
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
                throw new IllegalStateException("Unable to store document locally", exception);
            }
        });
    }

    private void deleteDirectoryFromS3(String directoryPath) {
        String prefix = toStorageKey(Paths.get(directoryPath).resolve("placeholder").toString());
        prefix = prefix.substring(0, prefix.lastIndexOf('/') + 1);
        final String s3Prefix = prefix;
        try {
            s3Client.listObjectsV2Paginator(builder -> builder.bucket(bucketName).prefix(s3Prefix))
                    .contents()
                    .forEach(object -> s3Client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(object.key())
                            .build()));
        } catch (Exception exception) {
            logger.warn("Unable to cleanup S3 documents under {}: {}", s3Prefix, exception.getMessage());
        }
    }

    private void deleteDirectoryLocally(String directoryPath) {
        Path userFilesDirectory = Paths.get(directoryPath);
        if (!Files.exists(userFilesDirectory)) {
            return;
        }
        try (var pathStream = Files.walk(userFilesDirectory)) {
            pathStream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    logger.warn("Unable to delete {} while cleaning up local user documents: {}", path, exception.getMessage());
                }
            });
        } catch (IOException exception) {
            logger.warn("Unable to cleanup local files at {}: {}", directoryPath, exception.getMessage());
        }
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
