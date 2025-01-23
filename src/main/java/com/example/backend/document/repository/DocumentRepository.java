package com.example.backend.document.repository;

import com.example.backend.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByMember_UniqueId(String uniqueId);
}

