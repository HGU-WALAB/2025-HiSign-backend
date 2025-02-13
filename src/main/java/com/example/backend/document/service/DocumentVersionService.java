package com.example.backend.document.service;

import com.example.backend.document.entity.Document;
import com.example.backend.document.entity.DocumentVersion;
import com.example.backend.document.repository.DocumentRepository;
import com.example.backend.document.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DocumentVersionService {

    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentRepository documentRepository;

    // 📌 최신 버전 조회 후 다음 버전 반환
    @Transactional(readOnly = true)
    public int getNextVersion(Long documentId) {
        return documentVersionRepository.findLatestVersionByDocumentId(documentId)
                .map(v -> v + 1)
                .orElse(1); // 문서에 대한 첫 번째 버전은 1로 설정
    }

    // 📌 문서 버전 정보 저장
    @Transactional
    public void saveDocumentVersion(Long documentId, String signerEmail, String versionedFileName) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 문서를 찾을 수 없습니다. ID: " + documentId));

        DocumentVersion documentVersion = DocumentVersion.builder()
                .document(document)
                .version(getNextVersion(documentId)) // 📌 자동 증가 버전 적용
                .signedBy(signerEmail)
                .signedAt(LocalDateTime.now())
                .fileName(versionedFileName) // 📌 버전이 포함된 파일명 저장
                .build();

        documentVersionRepository.save(documentVersion);
    }
}
