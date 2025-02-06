package com.example.backend.document.repository;

import com.example.backend.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByMember_UniqueId(String uniqueId);

    @Query("SELECT d FROM Document d JOIN SignatureRequest s ON d.id = s.document.id WHERE s.signerEmail = :email")
    List<Document> findDocumentsBySignerEmail(@Param("email") String email);
}

