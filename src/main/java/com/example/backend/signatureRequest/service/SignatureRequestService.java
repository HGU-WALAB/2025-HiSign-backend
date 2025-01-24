package com.example.backend.signatureRequest.service;

import com.example.backend.member.entity.Member;
import com.example.backend.signatureRequest.entity.SignatureRequest;
import com.example.backend.signatureRequest.repository.SignatureRequestRepository;
import com.example.backend.document.entity.Document;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SignatureRequestService {

    private final SignatureRequestRepository signatureRequestRepository;

    public SignatureRequestService(SignatureRequestRepository signatureRequestRepository) {
        this.signatureRequestRepository = signatureRequestRepository;
    }

    public List<SignatureRequest> createSignatureRequests(Document document, List<Member> signers) {
        List<SignatureRequest> requests = signers.stream().map(signer -> {
            String token = UUID.randomUUID().toString();
            return SignatureRequest.builder()
                    .document(document)
                    .signer(signer) // 서명자 추가
                    .token(token)
                    .createdAt(LocalDateTime.now())
                    .expiredAt(LocalDateTime.now().plusDays(7))
                    .status(0)  // 대기 상태
                    .build();
        }).collect(Collectors.toList());

        return signatureRequestRepository.saveAll(requests);
    }

}
