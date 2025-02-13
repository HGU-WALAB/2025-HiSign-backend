package com.example.backend.file.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileService {

    private final Path signatureStorageLocation;
    private final Path documentStorageLocation;
    private final Path signedDocumentStorageLocation;

    public FileService(@Value("${file.signature-dir}") String signatureDir,
                       @Value("${file.document-dir}") String documentDir,
                       @Value("${file.signed-document-dir}") String signedDocumentDir) {
        this.signatureStorageLocation = this.createDirectory(signatureDir);
        this.documentStorageLocation = this.createDirectory(documentDir);
        this.signedDocumentStorageLocation = this.createDirectory(signedDocumentDir);
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

    public String storeFile(MultipartFile file, String fileType) {
        try {
            // 고유한 파일 이름 생성
            String uniqueFileName = generateUniqueFileName(Objects.requireNonNull(file.getOriginalFilename()));

            // 저장할 경로 설정
            Path targetLocation;
            if ("DOCUMENT".equalsIgnoreCase(fileType)) {
                targetLocation = documentStorageLocation.resolve(uniqueFileName);
            } else if ("SIGNATURE".equalsIgnoreCase(fileType)) {
                targetLocation = signatureStorageLocation.resolve(uniqueFileName);
            } else {
                throw new RuntimeException("파일 저장 타입 오류: " + fileType);
            }

            // 파일 저장
            file.transferTo(targetLocation.toFile());

            return uniqueFileName; // 저장된 파일 경로 반환
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 중 오류 발생: " + file.getOriginalFilename(), e);
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

    public void deleteFile(Path filePath) {
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

    // ✅ 파일 저장 경로 반환
    public Path getSignatureFilePath(String fileName) {
        return signatureStorageLocation.resolve(fileName);
    }

    public Path getDocumentFilePath(String fileName) {
        return documentStorageLocation.resolve(fileName);
    }

    public Path getSignedDocumentFilePath(String fileName) {
        return signedDocumentStorageLocation.resolve(fileName);
    }
}
