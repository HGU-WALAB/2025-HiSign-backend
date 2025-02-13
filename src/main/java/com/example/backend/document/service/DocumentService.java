package com.example.backend.document.service;

import com.example.backend.document.dto.DocumentDTO;
import com.example.backend.document.entity.Document;
import com.example.backend.document.repository.DocumentRepository;
import com.example.backend.file.service.FileService;
import com.example.backend.member.entity.Member;
import com.example.backend.member.repository.MemberRepository;
import com.example.backend.signature.repository.SignatureRepository;
import com.example.backend.signatureRequest.repository.SignatureRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final FileService fileService;
    private final DocumentRepository documentRepository;
    private final MemberRepository memberRepository;
    private final SignatureRequestRepository signatureRequestRepository;

    public Optional<Document> getDocumentById(Long documentId) {
        return documentRepository.findById(documentId);
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

    public Document saveDocument(String requestName,MultipartFile file, String savedFileName, Member member) {

        Optional<Member> existingMember = memberRepository.findByUniqueId(member.getUniqueId());

        if(existingMember.isPresent()) member = existingMember.get();
        else member = memberRepository.save(member);

        // Document 엔티티 생성 및 저장
        Document document = new Document();
        document.setRequestName(requestName);
        document.setMember(member);
        document.setFileName(file.getOriginalFilename()); // 원래 파일 이름
        document.setSavedFileName(savedFileName); // 저장된 파일 이름
        document.setStatus(0); // 초기 상태 설정
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());

        return documentRepository.save(document);
    }

    public List<DocumentDTO> getDocumentsByUniqueId(String uniqueId) {
        List<Document> documents = documentRepository.findByMember_UniqueId(uniqueId);
        return documents.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<DocumentDTO> getDocumentsBySignerEmail(String email) {
        List<Document> documents = documentRepository.findDocumentsBySignerEmail(email);

        System.out.println("조회된 문서 개수: " + documents.size() + "개, 조회한 이메일: " + email);

        return documents.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional
    public boolean cancelRequest(Long documentId) {
        Optional<Document> documentOptional = documentRepository.findById(documentId);

        if (documentOptional.isPresent()) {
            Document document = documentOptional.get();
            document.setStatus(3);  // 상태를 '취소됨(3)'으로 설정
            document.setUpdatedAt(LocalDateTime.now());
            documentRepository.save(document);
            return true;
        }

        return false;  // 문서를 찾지 못한 경우
    }

    @Transactional
    public boolean deleteDocumentById(Long documentId) {
        Optional<Document> documentOptional = documentRepository.findById(documentId);

        if (documentOptional.isPresent()) {
            Document document = documentOptional.get();

            document.setStatus(5);
            document.setDeletedAt(LocalDateTime.now());

            // 문서 상태 삭제로 변경
            documentRepository.save(document);
            // 관련 서명 요청 삭제 상태로 변경
            signatureRequestRepository.updateRequestStatusToDeleted(documentId);

            return true;
        }
        return false;
    }

    // 🔹 문서를 조회하고 파일 리소스를 반환하는 메서드 (컨트롤러에서 ResponseEntity 생성)
    public Optional<Resource> loadFileAsResource(Long documentId) {
        Optional<Document> documentOpt = documentRepository.findById(documentId);

        if (!documentOpt.isPresent()) {
            return Optional.empty();
        }

        Document document = documentOpt.get();
        Path filePath = fileService.getDocumentFilePath(document.getSavedFileName()).normalize();
        Resource resource = new FileSystemResource(filePath.toString());

        return resource.exists() ? Optional.of(resource) : Optional.empty();
    }

    public String getOriginalFileName(Long documentId) {
        return documentRepository.findById(documentId)
                .map(Document::getFileName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다."));
    }
}
