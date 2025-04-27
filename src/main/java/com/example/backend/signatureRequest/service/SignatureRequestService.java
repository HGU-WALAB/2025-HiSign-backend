package com.example.backend.signatureRequest.service;

import com.example.backend.signatureRequest.DTO.SignerDTO;
import com.example.backend.signatureRequest.entity.SignatureRequest;
import com.example.backend.signatureRequest.repository.SignatureRequestRepository;
import com.example.backend.document.repository.DocumentRepository;
import com.example.backend.document.entity.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SignatureRequestService {

    private final SignatureRequestRepository signatureRequestRepository;
    private final DocumentRepository documentRepository;

    public SignatureRequestService(SignatureRequestRepository signatureRequestRepository,
                                   DocumentRepository documentRepository) {
        this.signatureRequestRepository = signatureRequestRepository;
        this.documentRepository = documentRepository;
    }

    public List<SignatureRequest> createSignatureRequests(Document document, List<SignerDTO> signers, String password) {
        List<SignatureRequest> requests = signers.stream().map(signer -> {
            String token = UUID.randomUUID().toString();
            return SignatureRequest.builder()
                    .document(document)
                    .signerEmail(signer.getEmail())
                    .signerName(signer.getName())
                    .token(token)
                    .createdAt(LocalDateTime.now())
                    .expiredAt(LocalDateTime.now().plusDays(7))
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
            document.setStatus(2);
            documentRepository.save(document);
        }

        return true;
    }
}