package com.example.backend.controller;

import com.example.backend.entity.Document;
import com.example.backend.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private DocumentRepository documentRepository;

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String directoryPath = Paths.get(uploadDir, "templates", "image").toString();
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String filePath = Paths.get(directoryPath, file.getOriginalFilename()).toString();

            file.transferTo(new File(filePath));

            Document document = new Document();
            document.setFilePath(filePath);
            document.setVersion(1);
            document.setCreatedAt(LocalDateTime.now());
            document.setUpdatedAt(LocalDateTime.now());
            document.setStatus(1);

            documentRepository.save(document);

            return "File uploaded and path saved to database successfully: " + filePath;
        } catch (IOException e) {
            return "File upload failed: " + e.getMessage();
        }
    }
}
