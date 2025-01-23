package com.example.backend.document.service;

import com.example.backend.document.dto.DocumentDTO;
import com.example.backend.document.entity.Document;
import com.example.backend.document.repository.DocumentRepository;
import com.example.backend.member.entity.Member;
import com.example.backend.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final Path documentStorageLocation;
    private final MemberRepository memberRepository;

    @Autowired
    public DocumentService(DocumentRepository documentRepository, @Value("${file.document-dir}") String documentDir, MemberRepository memberRepository) {
        this.documentRepository = documentRepository;
        this.documentStorageLocation = Paths.get(documentDir); // 파일 저장 경로
        this.memberRepository = memberRepository;
    }

    public Document getDocumentById(Long documentId) {
        return documentRepository.findById(documentId).orElse(null);
    }

    private DocumentDTO convertToDTO(Document document) {
        return DocumentDTO.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .memberId(document.getMember().getId())
                .savedFileName(document.getSavedFileName())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .status(document.getStatus())
                .build();
    }

    public Document saveDocument(MultipartFile file, String savedFileName, Member member) {

        Optional<Member> existingMember = memberRepository.findByUniqueId(member.getUniqueId());

        if(existingMember.isPresent()) member = existingMember.get();
        else member = memberRepository.save(member);

        // Document 엔티티 생성 및 저장
        Document document = new Document();
        document.setMember(member);
        document.setFileName(file.getOriginalFilename()); // 원래 파일 이름
        document.setSavedFileName(savedFileName); // 저장된 파일 이름
        document.setStatus(0); // 초기 상태 설정
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());

        return documentRepository.save(document);
    }

    // getStorageLocation 메서드를 추가하여 documentStorageLocation 반환
    public Path getStorageLocation() {
        return documentStorageLocation;
    }

    public List<DocumentDTO> getDocumentsByUniqueId(String uniqueId) {
        List<Document> documents = documentRepository.findByMember_UniqueId(uniqueId);

        return documents.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
}
