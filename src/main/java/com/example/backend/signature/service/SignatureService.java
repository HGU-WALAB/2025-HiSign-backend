package com.example.backend.signature.service;

import com.example.backend.document.entity.Document;
import com.example.backend.document.repository.DocumentRepository;
import com.example.backend.document.service.DocumentService;
import com.example.backend.file.service.FileService;
import com.example.backend.member.entity.Member;
import com.example.backend.member.repository.MemberRepository;
import com.example.backend.signature.DTO.SignatureDTO;
import com.example.backend.signature.entity.Signature;
import com.example.backend.signature.repository.SignatureRepository;
import com.example.backend.signatureRequest.DTO.SignerDTO;
import com.example.backend.signatureRequest.entity.SignatureRequest;
import com.example.backend.signatureRequest.repository.SignatureRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SignatureService {

    private final DocumentRepository documentRepository;
    private final SignatureRepository signatureRepository;
    private final SignatureRequestRepository signatureRequestRepository;

    private final FileService fileService;

    public SignatureService(SignatureRepository signatureRepository, DocumentRepository documentRepository, FileService fileService, SignatureRequestRepository signatureRequestRepository) {
        this.signatureRepository = signatureRepository;
        this.documentRepository = documentRepository;
        this.fileService = fileService;
        this.signatureRequestRepository = signatureRequestRepository;
    }

    public void createSignatureRegion(Document document, String signerEmail, int type, int pageNumber, float x, float y, float width, float height) {
        Signature signature = Signature.builder()
                .document(document)
                .signerEmail(signerEmail)
                .signedAt(null)
                .type(type)
                .imageName(null)
                .textData(null)
                .status(0)  // 대기 상태 설정
                .pageNumber(pageNumber)
                .x(x)
                .y(y)
                .width(width)
                .height(height)
                .build();

        signatureRepository.save(signature);
    }

    public List<SignatureDTO> getSignatureFields(Long documentId, String signerEmail) {
        return signatureRepository.findByDocumentIdAndSignerEmail(documentId, signerEmail)
                .stream()
                .map(SignatureDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveSignatures(SignerDTO signerDTO, Long documentId) {
        // 📌 문서 정보 조회 (없는 경우 예외 처리)
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 문서를 찾을 수 없습니다. ID: " + documentId));

        // 📌 기존 서명 데이터 조회
        List<Signature> existingSignatures = signatureRepository.findByDocumentIdAndSignerEmail(documentId, signerDTO.getEmail());

        // 📌 기존 서명을 맵으로 변환 (페이지 번호 + 좌표 기준)
        Map<String, Signature> signatureMap = existingSignatures.stream()
                .collect(Collectors.toMap(
                        s -> s.getPageNumber() + "_" + s.getX() + "_" + s.getY(), // ✅ 기존 서명의 고유 키
                        s -> s
                ));

        try{
        // 📌 새로운 서명 데이터 처리 (업데이트 또는 새로 추가)
        List<Signature> updatedSignatures = signerDTO.getSignatureFields().stream()
                .map(dto -> {
                    String key = dto.getPosition().getPageNumber() + "_" + dto.getPosition().getX() + "_" + dto.getPosition().getY();
                    Signature existingSignature = signatureMap.get(key);

                    if (existingSignature != null) {
                        // ✅ 기존 서명 업데이트
                        existingSignature.setImageName(dto.getImageName());
                        existingSignature.setTextData(dto.getTextData());
                        existingSignature.setSignedAt(LocalDateTime.now()); // ✅ 서명된 시간 업데이트
                        existingSignature.setStatus(1);
                        existingSignature.setWidth(dto.getWidth());
                        existingSignature.setHeight(dto.getHeight());
                        return existingSignature;
                    } else {
                        // ✅ 새로운 서명 추가
                        return Signature.builder()
                                .document(document)
                                .signerEmail(dto.getSignerEmail())
                                .signedAt(LocalDateTime.now()) // ✅ 새 서명의 경우 현재 시간 저장
                                .type(dto.getType())
                                .imageName(dto.getImageName())
                                .textData(dto.getTextData())
                                .status(1)
                                .pageNumber(dto.getPosition().getPageNumber())
                                .x(dto.getPosition().getX())
                                .y(dto.getPosition().getY())
                                .width(dto.getWidth())
                                .height(dto.getHeight())
                                .description(null)
                                .build();
                    }
                })
                .collect(Collectors.toList());

        // 📌 서명 데이터 저장 (업데이트된 기존 서명 + 새로운 서명)
        signatureRepository.saveAll(updatedSignatures);

        completeSignatureRequest(documentId,signerDTO.getEmail());
    } catch (Exception e) {
            // ✅ 첫 번째 서명 필드에서 이미지 파일명 추출 (파일이 하나만 업로드되므로 한 번만 가져오면 됨)
            String uploadedImageName = signerDTO.getSignatureFields().stream()
                    .filter(dto -> dto.getImageName() != null && !dto.getImageName().isEmpty()) // ✅ imageName이 존재하는 경우만 필터링
                    .map(SignatureDTO::getImageName) // ✅ imageName 값만 추출
                    .findFirst() // ✅ 첫 번째 값 가져오기
                    .orElse(null); // ✅ 없으면 null 반환

            // ✅ 예외 발생 시 업로드된 파일 삭제
        if (uploadedImageName != null) {
            deleteUploadedFile(uploadedImageName);
        }
        throw e; // 예외 다시 던지기 (트랜잭션 롤백)
    }}

    private void deleteUploadedFile(String fileName) {
        try {
            fileService.deleteFile(fileName,"SIGNATURE");
            System.out.println("❌ 업로드된 서명 이미지 삭제됨: " + fileName);
        } catch (Exception ex) {
            System.err.println("⚠️ 파일 삭제 실패: " + fileName);
        }

    }

    public void completeSignatureRequest(Long documentId, String signerEmail) {
        // 📌 해당 문서의 서명 요청을 가져옴
        List<SignatureRequest> signatureRequests = signatureRequestRepository.findByDocumentIdAndSignerEmail(documentId, signerEmail);

        if (signatureRequests.isEmpty()) {
            throw new IllegalArgumentException("해당 문서에 대한 서명 요청이 존재하지 않습니다.");
        }

        // ✅ 해당 서명 요청을 "완료(1)" 상태로 변경
        for (SignatureRequest request : signatureRequests) {
            request.setStatus(1); // 상태 완료
        }
        signatureRequestRepository.saveAll(signatureRequests);

        // 📌 해당 문서의 모든 서명 요청이 완료되었는지 확인
        boolean allCompleted = signatureRequestRepository
                .findByDocumentId(documentId)
                .stream()
                .allMatch(request -> request.getStatus() == 1);

        // ✅ 문서 상태도 업데이트 (모든 서명 요청이 완료된 경우)
        if (allCompleted) {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 문서를 찾을 수 없습니다. ID: " + documentId));

            document.setStatus(1); // 문서 상태 완료
            documentRepository.save(document);
        }
    }
}
