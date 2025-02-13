package com.example.backend.document.repository;

import com.example.backend.document.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    // 📌 해당 문서의 최신 버전 번호 가져오기
    @Query("SELECT MAX(d.version) FROM DocumentVersion d WHERE d.document.id = :documentId")
    Optional<Integer> findLatestVersionByDocumentId(Long documentId);
}
