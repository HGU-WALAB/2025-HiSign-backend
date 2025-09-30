package com.example.backend.signatureRequest.service;

import com.example.backend.auth.util.EncryptionUtil;
import com.example.backend.document.service.DocumentService;
import com.example.backend.mail.service.MailService;
import com.example.backend.signature.DTO.SignatureDTO;
import com.example.backend.signature.repository.SignatureRepository;
import com.example.backend.signature.service.SignatureService;
import com.example.backend.signatureRequest.DTO.SignerDTO;
import com.example.backend.signatureRequest.entity.SignatureRequest;
import com.example.backend.signatureRequest.repository.SignatureRequestRepository;
import com.example.backend.document.repository.DocumentRepository;
import com.example.backend.document.entity.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignatureRequestService {

    private final SignatureService signatureService;
    private final MailService mailService;
    private final DocumentService documentService;
    private final SignatureRequestRepository signatureRequestRepository;
    private final DocumentRepository documentRepository;
    private final EncryptionUtil encryptionUtil;

    public List<SignatureRequest> createSignatureRequests(Document document, List<SignerDTO> signers, String password, LocalDateTime expiredAt) {
        List<SignatureRequest> requests = signers.stream().map(signer -> {
            String token = UUID.randomUUID().toString();
            return SignatureRequest.builder()
                    .document(document)
                    .signerEmail(signer.getEmail())
                    .signerName(signer.getName())
                    .token(token)
                    .createdAt(LocalDateTime.now())
                    .expiredAt(expiredAt)
                    .status(0)  // 대기 상태
                    .password(password)
                    .build();
        }).collect(Collectors.toList());

        return signatureRequestRepository.saveAll(requests);
    }

    @Transactional
    public boolean cancelSignatureRequest(Long documentId, String reason) {
        List<SignatureRequest> requests = signatureRequestRepository.findByDocumentId(documentId);

        if (requests.isEmpty()) {
            return false;
        }

        requests.forEach(request -> request.setStatus(3));
        signatureRequestRepository.saveAll(requests);

        Optional<Document> documentOptional = documentRepository.findById(documentId);
        if (documentOptional.isPresent()) {
            Document document = documentOptional.get();
            document.setStatus(3);
            document.setCancelReason(reason);
            document.setUpdatedAt(LocalDateTime.now());
            documentRepository.save(document);
        }

        return true;
    }


    @Transactional
    public boolean rejectSignatureRequest(Long documentId, String reason) {
        List<SignatureRequest> requests = signatureRequestRepository.findByDocumentId(documentId);

        if (requests.isEmpty()) {
            return false;
        }

        requests.forEach(request -> {
            request.setStatus(2);
            request.setRejectReason(reason);
        });

        signatureRequestRepository.saveAll(requests);

        Optional<Document> documentOptional = documentRepository.findById(documentId);
        if (documentOptional.isPresent()) {
            Document document = documentOptional.get();
            document.setStatus(6);
            document.setUpdatedAt(LocalDateTime.now());
            documentRepository.save(document);
        }

        return true;
    }

    public List<SignatureRequest> getSignatureRequestsByDocument(Document document) {
        return signatureRequestRepository.findByDocument(document);
    }

    @Transactional
    public void saveSignatureRequestAndFields(Document document, List<SignerDTO> signers, String password, LocalDateTime expiredAt) {
        // 1. 서명 요청 생성
        createSignatureRequests(document, signers, password, expiredAt);

        // 2. 서명 필드 저장
        for (SignerDTO signer : signers) {
            for (SignatureDTO signatureField : signer.getSignatureFields()) {
                signatureService.createSignatureRegion(
                        document,
                        signer.getEmail(),
                        signatureField.getType(),
                        signatureField.getPosition().getPageNumber(),
                        signatureField.getPosition().getX(),
                        signatureField.getPosition().getY(),
                        signatureField.getWidth(),
                        signatureField.getHeight()
                );
            }
        }

    }

    @Transactional
    public void saveRequestsAndSendMail(Document document, List<SignerDTO> signers, String password, String senderName, LocalDateTime expiredAt) {

        // 2. 서명 요청 + 필드 저장
        List<SignatureRequest> requests = createSignatureRequests(document, signers, password, expiredAt);

        for (SignerDTO signer : signers) {
            for (SignatureDTO signatureField : signer.getSignatureFields()) {
                signatureService.createSignatureRegion(
                        document,
                        signer.getEmail(),
                        signatureField.getType(),
                        signatureField.getPosition().getPageNumber(),
                        signatureField.getPosition().getX(),
                        signatureField.getPosition().getY(),
                        signatureField.getWidth(),
                        signatureField.getHeight()
                );
            }
        }

        // 3. 메일 전송 (트랜잭션 안에서)
        try {
            mailService.sendSignatureRequestEmails(senderName, document.getRequestName(), requests, password);
        } catch (Exception e) {
            log.error("❌ 메일 발송 실패 - 트랜잭션 롤백", e);
            throw new RuntimeException("메일 발송 실패로 롤백", e);
        }
    }

    public String findTokenByDocumentIdAndEmail(Long documentId, String email) throws Exception {
        SignatureRequest request = signatureRequestRepository
                .findByDocumentIdAndSignerEmail(documentId, email)
                .orElseThrow(() -> new NoSuchElementException("서명 요청 없음"));
        return encryptionUtil.encryptUUID(request.getToken());
    }

}