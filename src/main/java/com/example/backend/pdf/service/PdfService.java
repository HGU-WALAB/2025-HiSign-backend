package com.example.backend.pdf.service;

import com.example.backend.file.service.FileService;
import com.example.backend.document.entity.Document;
import com.example.backend.document.service.DocumentService;
import com.example.backend.document.service.DocumentVersionService;
import com.example.backend.signature.DTO.SignatureDTO;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final DocumentService documentService;
    private final DocumentVersionService documentVersionService;
    private final FileService fileService;

    @Transactional
    public void signDocument(Long documentId, List<SignatureDTO> signatureDTOs) throws IOException, DocumentException {
        Document document = documentService.getDocumentById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다."));

        // 📌 최신 버전 조회 후 다음 버전 결정
        int nextVersion = documentVersionService.getNextVersion(documentId);

        // 📌 원본 문서 경로
        Path originalPdfPath = fileService.getDocumentFilePath(document.getSavedFileName());

        // 📌 새로운 버전 파일명 생성 (예: contract_v1.pdf, contract_v2.pdf)
        String versionedFileName = document.getSavedFileName().replace(".pdf", "_v" + nextVersion + ".pdf");
        Path signedPdfPath = fileService.getSignedDocumentFilePath(versionedFileName);

        PdfReader reader = new PdfReader(originalPdfPath.toString());
        PdfStamper stamper = new PdfStamper(reader, Files.newOutputStream(Paths.get(signedPdfPath.toString())));

        for (SignatureDTO signatureDTO : signatureDTOs) {
            addSignatureToPdf(stamper, signatureDTO);
        }

        stamper.close();
        reader.close();

        // 📌 문서 버전 테이블에 저장
        documentVersionService.saveDocumentVersion(documentId, signatureDTOs.get(0).getSignerEmail(), versionedFileName);
    }

    private void addSignatureToPdf(PdfStamper stamper, SignatureDTO signatureDTO) throws IOException, DocumentException {
        PdfContentByte content = stamper.getOverContent(signatureDTO.getPosition().getPageNumber());

        Path imagePath = fileService.getSignatureFilePath(signatureDTO.getImageName());

        if (Files.exists(imagePath)) {
            Image signatureImage = Image.getInstance(imagePath.toString());
            signatureImage.scaleAbsolute(signatureDTO.getWidth(), signatureDTO.getHeight());
            signatureImage.setAbsolutePosition(signatureDTO.getPosition().getX(), signatureDTO.getPosition().getY());
            content.addImage(signatureImage);
        } else {
            throw new IOException("서명 이미지 파일을 찾을 수 없습니다: " + imagePath);
        }
    }
}
