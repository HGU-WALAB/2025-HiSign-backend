package com.example.backend.signatureRequest.repository;

import com.example.backend.signatureRequest.entity.SignatureRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SignatureRequestRepository extends JpaRepository<SignatureRequest, Long> {
    List<SignatureRequest> findByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);
}
