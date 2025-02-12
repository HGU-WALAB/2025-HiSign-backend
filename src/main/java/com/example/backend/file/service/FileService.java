package com.example.backend.file.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    private final Path signatureStorageLocation;
    private final Path fileStorageLocation;

    public FileService(@Value("${file.signature-dir}") String signatureDir,
                       @Value("${file.document-dir}") String documentDir) {
        this.signatureStorageLocation = this.createDirectory(signatureDir);
        this.fileStorageLocation = this.createDirectory(documentDir);
    }

    public Path createDirectory(String dir) {
        Path directory = Paths.get(dir).toAbsolutePath().normalize();
        try {
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }
        } catch (IOException e) {
            throw new RuntimeException("디렉토리를 생성할 수 없습니다: " + dir, e);
        }
        return directory;
    }

    public String storeFile(byte[] fileData, String fileName, String fileType) {
        try {
            // 고유 파일 이름 생성
            String uniqueFileName = generateUniqueFileName(fileName);
            Path targetLocation;
            if(fileType.equalsIgnoreCase("SIGNATURE")) {
                targetLocation = signatureStorageLocation.resolve(uniqueFileName);
            } else if (fileType.equalsIgnoreCase("DOCUMENT")) {
                targetLocation = fileStorageLocation.resolve(uniqueFileName);
            } else {
                throw new RuntimeException("파일 저장 타입 오류:" + fileType);
            }
            Files.write(targetLocation, fileData);
            return uniqueFileName;
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 중 오류 발생: " + fileName, e);
        }
    }

    public byte[] readFile(String fileName, Path storageLocation) {
        try {
            Path filePath = storageLocation.resolve(fileName).normalize();
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("파일 읽기 중 오류 발생: " + fileName, e);
        }
    }

    public void deleteDocumentFile(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("파일 삭제 중 오류 발생: " + filePath, e);
        }
    }

    private String generateUniqueFileName(String originalFileName) {
        String onlyFileName = originalFileName.substring(0, originalFileName.lastIndexOf("."));
        String uniqueFileName = onlyFileName + UUID.randomUUID().toString();
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));

        return uniqueFileName + extension;
    }
}
