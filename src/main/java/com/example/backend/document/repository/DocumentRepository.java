package com.example.backend.document.repository;

import com.example.backend.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByMember_UniqueId(String uniqueId);

    @Query("SELECT d FROM Document d JOIN SignatureRequest s ON d.id = s.document.id WHERE s.signerEmail = :email")
    List<Document> findDocumentsBySignerEmail(@Param("email") String email);

    @Query(value =
            "SELECT DISTINCT d.id, d.file_name, d.created_at, d.status, d.request_name, sr.expired_at " +
                    "FROM document d " +
                    "JOIN member m ON d.unique_id = m.unique_id " + // ← JOIN 추가
                    "LEFT JOIN signature_request sr ON d.id = sr.document_id " +
                    "WHERE m.unique_id = :uniqueId " +              // ← member 기준 비교
                    "AND NOT EXISTS ( " +
                    "    SELECT 1 FROM hidden_document h " +
                    "    WHERE h.document_id = d.id" +
                    "    AND h.member_id = :uniqueId " +
                    "    AND h.view_type = 'sent' " +
                    ") " +
                    "ORDER BY d.created_at DESC",
            nativeQuery = true)
    List<Object[]> findDocumentsWithExpiration(@Param("uniqueId") String uniqueId);

    @Query(value =
            "SELECT DISTINCT d.id, d.file_name, d.created_at, d.status AS document_status, " +
                    "       m.name AS requester_name, d.request_name, sr.expired_at, " +
                    "       sr.token, d.is_rejectable, sr.status AS request_status " +
                    "FROM document d " +
                    "JOIN member m ON d.unique_id = m.unique_id " +
                    "JOIN signature_request sr ON d.id = sr.document_id " +
                    "WHERE sr.signer_email = :email " +
                    "AND NOT EXISTS ( " +
                    "    SELECT 1 FROM hidden_document h " +
                    "    WHERE h.document_id = d.id " +
                    "      AND h.member_id = :uniqueId " +
                    "      AND h.view_type = 'received' " +
                    ") " +
                    "ORDER BY d.created_at DESC",
            nativeQuery = true)
    List<Object[]> findDocumentsBySignerEmailWithRequester(@Param("email") String email, @Param("uniqueId") String uniqueId);

    @Query(value =
            "SELECT DISTINCT d.id, d.file_name, d.created_at, d.status, m.name AS requester_name, " +
                    "       d.request_name, sr.expired_at, d.is_rejectable " +
                    "FROM document d " +
                    "JOIN member m ON d.unique_id = m.unique_id " +
                    "JOIN signature_request sr ON d.id = sr.document_id " +
                    "WHERE d.type = 1 " +
                    "AND NOT EXISTS ( " +
                    "    SELECT 1 FROM hidden_document h " +
                    "    WHERE h.document_id = d.id " +
                    "      AND h.member_id = :uniqueId " +
                    "      AND h.view_type = 'admin' " +
                    ") " +
                    "ORDER BY d.created_at DESC",
            nativeQuery = true)
    List<Object[]> findAllDocumentsWhereTypeIsOne(@Param("uniqueId") String uniqueId);



    @Modifying
    @Transactional
    @Query("UPDATE Document d SET d.status = 4 WHERE d.id = :documentId")
    void updateDocumentStatusToExpired(@Param("documentId") Long documentId);


    @Modifying
    @Transactional
    @Query("UPDATE Document d SET d.status = 2 WHERE d.id = :documentId")
    int updateDocumentStatusToRejected(@Param("documentId") Long documentId);



}

