package com.example.backend.signature.repository;

import com.example.backend.signature.entity.Signature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public interface SignatureRepository extends JpaRepository<Signature, Long> {
    void deleteByDocumentId(Long documentId);

    List<Signature> findByDocumentIdAndSignerEmail(Long id, String email);
}
