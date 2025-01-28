package com.example.backend.signature.repository;

import com.example.backend.signature.entity.Signature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SignatureRepository extends JpaRepository<Signature, Long> {
    void deleteByDocumentId(Long documentId);
}
