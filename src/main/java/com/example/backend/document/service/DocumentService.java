package com.example.backend.document.service;

import com.example.backend.document.dto.DocumentDTO;
import com.example.backend.document.entity.Document;
import com.example.backend.document.repository.DocumentRepository;
import com.example.backend.file.service.FileService;
import com.example.backend.member.entity.Member;
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

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
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

    public Document saveDocument(MultipartFile file, String filePath, Integer version) {
        Member member = new Member();
        member.setId(1L);

        // Document 엔티티 생성 및 저장
        Document document = new Document();
        document.setMember(member);
        document.setFileName(file.getOriginalFilename()); // 원래 파일 이름
        document.setFilePath(filePath); // 저장된 파일 경로
        document.setVersion(version); // 버전 설정
        document.setStatus(0); // 초기 상태 설정
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());

        return documentRepository.save(document);
    }
}

