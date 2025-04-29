package com.example.backend.signatureRequest.repository;

import com.example.backend.signatureRequest.entity.SignatureRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface SignatureRequestRepository extends JpaRepository<SignatureRequest, Long> {
    @Query("SELECT s FROM SignatureRequest s WHERE s.document.id = :documentId")
    List<SignatureRequest> findByDocumentId(@Param("documentId") Long documentId);
    ;

    void deleteByDocumentId(Long documentId);

    @Modifying
    @Transactional
    @Query("UPDATE SignatureRequest s SET s.status = 2 WHERE s.document.id = :documentId")
    int updateRequestStatusToRejected(@Param("documentId") Long documentId);

    @Modifying
    @Transactional
    @Query("UPDATE SignatureRequest s SET s.status = 5 WHERE s.document.id = :documentId")
    int updateRequestStatusToDeleted(@Param("documentId") Long documentId);

    Optional<SignatureRequest> findByToken(String token);

    List<SignatureRequest> findByDocumentIdAndSignerEmail(Long documentId, String signerEmail);

    @Query("SELECT distinct sr.signerName, sr.signerEmail, sr.status, s.signedAt " +
            "FROM SignatureRequest sr " +
            "LEFT JOIN Signature s ON sr.document.id = s.document.id AND sr.signerEmail = s.signerEmail " +
            "WHERE sr.document.id = :documentId")
    List<Object[]> findSignerInfoWithSignedAt(@Param("documentId") Long documentId);

    @Query("SELECT s.rejectReason FROM SignatureRequest s WHERE s.document.id = :documentId")
    Optional<String> findRejectReasonByDocumentId(@Param("documentId") Long documentId);
}
