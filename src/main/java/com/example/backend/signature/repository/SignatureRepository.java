package com.example.backend.signature.repository;

import com.example.backend.signature.entity.Signature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public interface SignatureRepository extends JpaRepository<Signature, Long> {
    void deleteByDocumentId(Long documentId);

    List<Signature> findByDocumentIdAndSignerEmail(Long id, String email);

    List<Signature> findByDocumentId(Long documentId);

    // 📌 특정 문서에 대한 서명자의 이메일 목록 조회
    @Query("SELECT DISTINCT s.signerEmail FROM Signature s WHERE s.document.id = :documentId")
    List<String> findSignerEmailsByDocumentId(@Param("documentId") Long documentId);

    boolean existsBySignerEmailAndStatus(String signerEmail, int status);

    Optional<Signature> findFirstBySignerEmailAndTypeAndImageNameIsNotNullOrderBySignedAtDesc(String signerEmail, int type);

    @Query("SELECT COUNT(s) > 0 FROM Signature s WHERE s.signerEmail = :signerEmail AND s.saveConsent = true AND s.status = 1 AND s.imageName IS NOT NULL")
    boolean existsSavedSignature(@Param("signerEmail") String signerEmail);

}
