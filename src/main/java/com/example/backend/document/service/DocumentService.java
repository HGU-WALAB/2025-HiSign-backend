package com.example.backend.document.service;

import com.example.backend.document.dto.DocumentDTO;
import com.example.backend.document.entity.Document;
import com.example.backend.document.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    public List<DocumentDTO> getAllDocuments() {
        return documentRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private DocumentDTO convertToDTO(Document document) {
        DocumentDTO dto = new DocumentDTO();
        dto.setId(document.getId());
        dto.setFilePath(document.getFilePath());
        dto.setVersion(document.getVersion());
        dto.setCreatedAt(document.getCreatedAt());
        dto.setUpdatedAt(document.getUpdatedAt());
        dto.setStatus(document.getStatus());
        return dto;
    }
}

