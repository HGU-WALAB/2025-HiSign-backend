package com.example.backend.signatureRequest.controller;

import com.example.backend.document.entity.Document;
import com.example.backend.document.service.DocumentService;
import com.example.backend.mail.service.MailService;
import com.example.backend.member.service.MemberService;
import com.example.backend.signature.DTO.SignatureDTO;
import com.example.backend.signature.service.SignatureService;
import com.example.backend.signatureRequest.DTO.SignatureRequestDTO;
import com.example.backend.signatureRequest.DTO.SignerDTO;
import com.example.backend.signatureRequest.entity.SignatureRequest;
import com.example.backend.signatureRequest.service.SignatureRequestService;
import com.example.backend.member.entity.Member;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/signature-requests")
public class SignatureRequestController {

    private final DocumentService documentService;
    private final MemberService memberService;
    private final SignatureService signatureService;
    private final SignatureRequestService signatureRequestService;
    private final MailService mailService;

    public SignatureRequestController(DocumentService documentService,
                                      MemberService memberService,
                                      SignatureService signatureService,
                                      SignatureRequestService signatureRequestService,
                                      MailService mailService) {
        this.documentService = documentService;
        this.memberService = memberService;
        this.signatureService = signatureService;
        this.signatureRequestService = signatureRequestService;
        this.mailService = mailService;
    }

    @PostMapping("/request")
    public ResponseEntity<String> submitSignatureRequest(@RequestBody SignatureRequestDTO requestDto) {
        // 1. 문서 조회
        Document document = documentService.getDocumentById(requestDto.getDocumentId());
        if (document == null) {
            return ResponseEntity.badRequest().body("문서를 찾을 수 없습니다.");
        }

        // 2. 서명자 정보 확인 및 추가
        List<Member> signers = memberService.findOrCreateMembers(requestDto.getSigners());

        // 3. 서명 요청 생성 및 저장
        List<SignatureRequest> requests = signatureRequestService.createSignatureRequests(document, signers);

        // 4. 서명 필드 저장
        for (SignatureRequest request : requests) {
            for (SignatureDTO fieldDto : requestDto.getSignatureFields()) {
                signatureService.createSignatureRegion(
                        document,
                        request.getSigner(),
                        fieldDto.getType(),
                        fieldDto.getPosition().getPageNumber(),
                        fieldDto.getPosition().getX(),
                        fieldDto.getPosition().getY(),
                        fieldDto.getWidth(),
                        fieldDto.getHeight()
                );
            }
        }

        //5. 메일 전송
        mailService.sendSignatureRequestEmails(requests);

        return ResponseEntity.ok("서명 요청이 성공적으로 생성되었습니다.");
    }
}
