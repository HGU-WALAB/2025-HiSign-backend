package com.example.backend.document.repository;

import com.example.backend.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByMember_UniqueId(String uniqueId);

    @Query("SELECT d FROM Document d JOIN SignatureRequest s ON d.id = s.document.id WHERE s.signerEmail = :email")
    List<Document> findDocumentsBySignerEmail(@Param("email") String email);

    @Modifying
    @Transactional
    @Query("UPDATE Document d SET d.status = 2 WHERE d.id = :documentId")
    int updateDocumentStatusToRejected(@Param("documentId") Long documentId);

    @Query("SELECT d.id, d.fileName, d.createdAt, d.status, m.name AS requesterName " +
            "FROM Document d " +
            "JOIN SignatureRequest s ON d.id = s.document.id " +
            "JOIN Member m ON d.member.id = m.id " +
            "WHERE s.signerEmail = :email")
    List<Object[]> findDocumentsBySignerEmailWithRequester(@Param("email") String email);

}

