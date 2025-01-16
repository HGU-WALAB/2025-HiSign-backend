package com.example.backend.document.service;

import com.example.backend.document.dto.DocumentDTO;
import com.example.backend.document.entity.Document;
import com.example.backend.document.repository.DocumentRepository;
import com.example.backend.file.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;
    private final FileService fileService;
    private final Path documentStorageLocation;

    public DocumentService(DocumentRepository documentRepository, FileService fileService, @Value("${file.document-dir}") String documentDir) {
        this.documentRepository = documentRepository;
        this.fileService = fileService;
        this.documentStorageLocation = fileService.createDirectory(documentDir);
    }

    public List<DocumentDTO> getAllDocuments() {
        return documentRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private DocumentDTO convertToDTO(Document document) {
        return DocumentDTO.builder()
                .id(document.getId())
                .memberId(document.getMember().getId())
                .fileName(document.getFileName())
                .filePath(document.getFilePath())
                .version(document.getVersion())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .status(document.getStatus())
                .build();
    }

    public Document uploadAndSaveDocument(MultipartFile file, Integer version) {
        // 1. 파일 저장
        String filePath;
        try {
            filePath = fileService.storeFile(file.getBytes(), file.getOriginalFilename(), documentStorageLocation);
        } catch (IOException e) {
            throw new RuntimeException("문서 파일 저장 중 오류 발생: " + file.getOriginalFilename(), e);
        }

        // 2. Document 엔티티 생성 및 저장
        Document document = new Document();
        document.setFileName(file.getOriginalFilename()); // 원래 파일 이름
        document.setFilePath(filePath); // 저장된 파일 경로
        document.setVersion(version); // 버전 설정
        document.setStatus(0); // 초기 상태 설정
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());

        return documentRepository.save(document);
    }
}

