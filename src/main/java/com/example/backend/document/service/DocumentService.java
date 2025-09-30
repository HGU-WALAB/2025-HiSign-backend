package com.example.backend.document.service;

import com.example.backend.auth.exception.DoNotExistException;
import com.example.backend.auth.util.EncryptionUtil;
import com.example.backend.document.dto.DocumentDTO;
import com.example.backend.document.entity.Document;
import com.example.backend.document.entity.HiddenDocument;
import com.example.backend.document.repository.DocumentRepository;
import com.example.backend.document.repository.HiddenDocumentRepository;
import com.example.backend.file.service.FileService;
import com.example.backend.member.entity.Member;
import com.example.backend.member.repository.MemberRepository;
import com.example.backend.signatureRequest.repository.SignatureRequestRepository;
import com.example.backend.signatureRequest.service.SignatureRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigInteger;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final FileService fileService;
    private final DocumentRepository documentRepository;
    private final MemberRepository memberRepository;
    private final SignatureRequestRepository signatureRequestRepository;
    private final HiddenDocumentRepository hiddenDocumentRepository;
    private final EncryptionUtil encryptionUtil;

    public Optional<Document> getDocumentById(Long documentId) {
        return documentRepository.findById(documentId);
    }

    public Document saveDocument(String requestName,MultipartFile file, String savedFileName, Member member, Integer IsRejectable, String description, Integer type) {

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
        document.setType(type);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        return documentRepository.save(document);
    }

    //요청한 문서 정보 API
    public List<Map<String, Object>> getDocumentsByUniqueId(String uniqueId) {
        List<Object[]> results = documentRepository.findDocumentsWithExpiration(uniqueId);
        LocalDateTime now = LocalDateTime.now();

        if (results == null || results.isEmpty()) {
            log.error("[ERROR] 요청한 문서 데이터가 존재하지 않음. uniqueId: {}", uniqueId);
            return new ArrayList<>();
        }

        List<Map<String, Object>> documents = new ArrayList<>();
        for (Object[] result : results) {
            try {
                Map<String, Object> docMap = new HashMap<>();
                BigInteger docIdRaw = (BigInteger) result[0];
                Long docId = docIdRaw.longValue();
                Byte statusByte = (Byte) result[3];
                Integer status = statusByte.intValue();
                Timestamp createdAtRaw = (Timestamp) result[2];
                LocalDateTime createdAt = createdAtRaw.toLocalDateTime();

                LocalDateTime expiredAt = null;
                if (result[5] != null) {
                    expiredAt = ((Timestamp) result[5]).toLocalDateTime();
                }
                if (expiredAt != null && expiredAt.isBefore(now) && status == 0) {
                    documentRepository.updateDocumentStatusToExpired(docId);
                    status = 4;
                }

                docMap.put("id", docId);
                docMap.put("fileName", result[1]);
                docMap.put("createdAt", createdAt);
                docMap.put("status", status);
                docMap.put("requestName", result[4] != null ? result[4] : "작업명 없음");
                docMap.put("expiredAt", expiredAt != null ? expiredAt : "미설정");

                String token = (String) result[6];
                if (token != null) {
                    try {
                        String encryptedToken = encryptionUtil.encryptUUID(token);
                        docMap.put("token", encryptedToken);
                    } catch (Exception e) {
                        log.error("[ERROR] 토큰 암호화 실패: {}", e.getMessage());
                        docMap.put("token", "암호화 실패");
                    }
                } else {
                    docMap.put("token", "토큰 없음");
                }

                documents.add(docMap);
            } catch (Exception e) {
                log.error("[ERROR] 요청한 문서 데이터 매핑 중 오류 발생: {}", e.getMessage());
            }
        }
        return documents;
    }

    @Transactional
    public List<Map<String, Object>> getDocumentsWithRequesterInfoBySignerEmail(String email) {
        String uniqueId = memberRepository.findUniqueIdByEmail(email);
        List<Object[]> results = documentRepository.findDocumentsBySignerEmailWithRequester(email, uniqueId);
        LocalDateTime now = LocalDateTime.now();

        if (results == null || results.isEmpty()) {
            log.error("[ERROR] 문서 데이터가 존재하지 않음. email: {}", email);
            return new ArrayList<>();
        }

        List<Map<String, Object>> documents = new ArrayList<>();
        for (Object[] result : results) {
            try {
                Map<String, Object> docMap = new HashMap<>();
                Long docId = ((Number) result[0]).longValue();
                String fileName = (String) result[1];
                LocalDateTime createdAt = ((Timestamp) result[2]).toLocalDateTime();
                Integer documentStatus = ((Number) result[3]).intValue();
                String requesterName = (String) result[4];
                String requestName = (String) result[5];
                Timestamp expiredAtRaw = result[6] != null ? (Timestamp) result[6] : null;
                LocalDateTime expiredAt = expiredAtRaw != null ? expiredAtRaw.toLocalDateTime() : null;

                if (expiredAt != null && expiredAt.isBefore(now) && documentStatus == 0) {
                    documentRepository.updateDocumentStatusToExpired(docId);
                    documentStatus = 4;
                }

                docMap.put("id", docId);
                docMap.put("fileName", fileName);
                docMap.put("createdAt", createdAt);
                docMap.put("status", documentStatus);
                docMap.put("requesterName", requesterName != null ? requesterName : "알 수 없음");
                docMap.put("requestName", requestName != null ? requestName : "작업명 없음");
                docMap.put("expiredAt", expiredAt != null ? expiredAt : "미설정");

                String token = (String) result[7];
                if (token != null) {
                    try {
                        String encryptedToken = encryptionUtil.encryptUUID(token);
                        docMap.put("token", encryptedToken);
                    } catch (Exception e) {
                        log.error("[ERROR] 토큰 암호화 실패: {}", e.getMessage());
                        docMap.put("token", "암호화 실패");
                    }
                } else {
                    docMap.put("token", "토큰 없음");
                }

                docMap.put("isRejectable", result[8] != null && ((Number) result[8]).intValue() == 1);
                docMap.put("signStatus", ((Number) result[9]).intValue());

                documents.add(docMap);
            } catch (Exception e) {
                log.error("[ERROR] 문서 데이터 매핑 중 오류 발생: {}", e.getMessage());
            }
        }

        return documents;
    }


    @Transactional
    public boolean deleteDocumentById(Long documentId, String uniqueId, String viewTypeRaw) {
        Optional<Document> documentOptional = documentRepository.findById(documentId);

        if (documentOptional.isPresent()) {
            HiddenDocument.ViewType viewType;
            try {
                viewType = HiddenDocument.ViewType.valueOf(viewTypeRaw.toLowerCase());  // 혹은 .toUpperCase(), 대소문자 주의
            } catch (IllegalArgumentException e) {
                log.error("[ERROR] 잘못된 viewType 요청: {}", viewTypeRaw);
                throw new IllegalArgumentException("유효하지 않은 viewType입니다: " + viewTypeRaw);
            }
            if (!hiddenDocumentRepository.existsByDocumentIdAndMemberIdAndViewType(documentId, uniqueId, viewType)) {
                HiddenDocument hidden = new HiddenDocument();
                hidden.setDocumentId(documentId);
                hidden.setMemberId(uniqueId);
                hidden.setViewType(viewType);
                hidden.setHiddenAt(LocalDateTime.now());
                hiddenDocumentRepository.save(hidden);
            }

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

    //문서 디테일 페이지 상세 정보 API
    public Map<String, Object> getDocumentInfo(Long documentId) {
        Optional<Document> documentOpt = documentRepository.findById(documentId);

        if (documentOpt.isPresent()) {
            Document document = documentOpt.get();
            Map<String, Object> documentDetails = new HashMap<>();

            String requesterName = document.getMember().getName();

            String rejectReason = signatureRequestRepository
                    .findRejectReasonByDocumentId(documentId)
                    .orElse("없음");

            String reviewRejectReason = document.getReviewRejectReason();
            String cancelReason = document.getCancelReason() != null ? document.getCancelReason() : "없음";

            LocalDateTime createdAt = document.getCreatedAt();

            String fileName = document.getFileName();
            String requestName = document.getRequestName();

            Integer status = document.getStatus();

            documentDetails.put("requesterName", requesterName);
            documentDetails.put("rejectReason", rejectReason);
            documentDetails.put("createdAt", createdAt);
            documentDetails.put("fileName", fileName);
            documentDetails.put("requestName", requestName);
            documentDetails.put("reviewRejectReason", reviewRejectReason);
            documentDetails.put("status", status);
            documentDetails.put("cancelReason", cancelReason);

            return documentDetails;
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "문서 정보를 찾을 수 없습니다.");
    }

    public List<Map<String, Object>> getAllAdminDocuments(String uniqueId) {
        List<Object[]> results = documentRepository.findAllDocumentsWhereTypeIsOne(uniqueId);

        List<Map<String, Object>> documents = new ArrayList<>();
        for (Object[] result : results) {
            Map<String, Object> docMap = new HashMap<>();
            docMap.put("id", result[0]);
            docMap.put("fileName", result[1]);
            docMap.put("createdAt", result[2]);
            docMap.put("status", result[3]);
            docMap.put("requesterName", result[4] != null ? result[4] : "알 수 없음");
            docMap.put("requestName", result[5] != null ? result[5] : "작업명 없음");
            docMap.put("expiredAt", result[6] != null ? result[6] : "미설정");
            docMap.put("isRejectable", result[7] != null ? result[7] : "0");
            docMap.put("updatedAt", result[8]);

            documents.add(docMap);
        }

        return documents;
    }

    public boolean requestCheckingById(Long id) {
        Optional<Document> documentOptional = documentRepository.findById(id);

        if (documentOptional.isPresent()) {
            Document document = documentOptional.get();

            document.setStatus(7);
            document.setUpdatedAt(LocalDateTime.now());

            documentRepository.save(document);
            return true;
        }
        return false;
    }
    @Transactional
    public void save(Document document) {
        documentRepository.save(document);
    }

    public void rejectDocument(Long documentId, String rejectReason) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문서입니다."));

        document.setReviewRejectReason(rejectReason);
        document.setStatus(2); // 2 = 거절됨
        document.setUpdatedAt(LocalDateTime.now());

        documentRepository.save(document);
    }
}
