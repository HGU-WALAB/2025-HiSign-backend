package com.example.backend.document.service;

import com.example.backend.auth.util.EncryptionUtil;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final FileService fileService;
    private final DocumentRepository documentRepository;
    private final MemberRepository memberRepository;
    private final SignatureRequestRepository signatureRequestRepository;
    private final EncryptionUtil encryptionUtil;

    public Optional<Document> getDocumentById(Long documentId) {
        return documentRepository.findById(documentId);
    }

    private DocumentDTO convertToDTO(Document document) {
        return DocumentDTO.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .requestName(document.getRequestName())
                .memberId(document.getMember().getId())
                .savedFileName(document.getSavedFileName())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .status(document.getStatus())
                .isRejectable(document.getIsRejectable())
                .description(document.getDescription())
                .build();
    }

    public Document saveDocument(String requestName,MultipartFile file, String savedFileName, Member member, Integer IsRejectable, String description) {

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
        document.setIsRejectable(IsRejectable);
        document.setDescription(description);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        return documentRepository.save(document);
    }

    public List<Map<String, Object>> getDocumentsByUniqueId(String uniqueId) {
        List<Object[]> results = documentRepository.findDocumentsWithExpiration(uniqueId);

        if (results == null || results.isEmpty()) {
            System.out.println("[ERROR] 요청한 문서 데이터가 존재하지 않음. uniqueId: " + uniqueId);
            return new ArrayList<>();
        }

        List<Map<String, Object>> documents = new ArrayList<>();
        for (Object[] result : results) {
            try {
                Map<String, Object> docMap = new HashMap<>();
                docMap.put("id", result[0]);
                docMap.put("fileName", result[1]);
                docMap.put("createdAt", result[2]);
                docMap.put("status", result[3]);
                docMap.put("requestName", result[4] != null ? result[4] : "작업명 없음");
                docMap.put("expiredAt", result[5] != null ? result[5] : "미설정");

                documents.add(docMap);
            } catch (Exception e) {
                System.out.println("[ERROR] 요청한 문서 데이터 매핑 중 오류 발생: " + e.getMessage());
            }
        }
        return documents;
    }

    public List<Map<String, Object>> getDocumentsWithRequesterInfoBySignerEmail(String email) {
        List<Object[]> results = documentRepository.findDocumentsBySignerEmailWithRequester(email);

        if (results == null || results.isEmpty()) {
            System.out.println("[ERROR] 문서 데이터가 존재하지 않음. email: " + email);
            return new ArrayList<>();
        }

        List<Map<String, Object>> documents = new ArrayList<>();

        for (Object[] result : results) {
            try {
                Map<String, Object> docMap = new HashMap<>();
                docMap.put("id", result[0]);
                docMap.put("fileName", result[1]);
                docMap.put("createdAt", result[2]);
                docMap.put("status", result[3]);
                docMap.put("requesterName", result[4] != null ? result[4] : "알 수 없음");
                docMap.put("requestName", result[5] != null ? result[5] : "작업명 없음");
                docMap.put("expiredAt", result[6] != null ? result[6] : "미설정");
                String token = result.length > 6 ? (String) result[7] : null;
                if (token != null) {
                    try {
                        String encryptedToken = encryptionUtil.encryptUUID(token);
                        docMap.put("token", encryptedToken); // 🔹 암호화된 토큰 저장
                    } catch (Exception e) {
                        System.out.println("[ERROR] 토큰 암호화 실패: " + e.getMessage());
                        docMap.put("token", "암호화 실패");
                    }
                } else {
                    docMap.put("token", "토큰 없음");
                }
                docMap.put("isRejectable", result[8] != null ? result[8] : "0");

                documents.add(docMap);
            } catch (Exception e) {
                System.out.println("[ERROR] 문서 데이터 매핑 중 오류 발생: " + e.getMessage());
            }
        }
        return documents;
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
