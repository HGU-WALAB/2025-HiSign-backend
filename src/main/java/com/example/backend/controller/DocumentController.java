package com.example.backend.controller;

import com.example.backend.dto.DocumentDTO;
import com.example.backend.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @GetMapping("/api/documents")
    public List<DocumentDTO> getAllDocuments() {
        return documentService.getAllDocuments();
    }
}

