package com.example.backend.pdf.service;

import com.example.backend.file.service.FileService;
import com.example.backend.document.entity.Document;
import com.example.backend.document.service.DocumentService;
import com.example.backend.signature.DTO.SignatureDTO;
import com.example.backend.signature.entity.Signature;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final DocumentService documentService;
    private final FileService fileService;

    @Transactional(rollbackFor = Exception.class)
    public byte[] generateSignedDocument(Long documentId, List<Signature> signatures) throws IOException, DocumentException {
        Document document = documentService.getDocumentById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다."));

        Path originalPdfPath = fileService.getDocumentFilePath(document.getSavedFileName());
        PdfReader reader = new PdfReader(originalPdfPath.toString());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfStamper stamper = new PdfStamper(reader, outputStream);

        for (Signature signature : signatures) {
            boolean isCompleted = signature.getStatus() == 1;
            if (isCompleted) {
                addSignatureToPdf(stamper, signature, reader);
            }
        }

        stamper.close();
        reader.close();

        return outputStream.toByteArray();
    }

    private void addSignatureToPdf(PdfStamper stamper, Signature signature, PdfReader reader) throws IOException, DocumentException {
        PdfContentByte content = stamper.getOverContent(signature.getPageNumber());

        // PDF 페이지 크기 가져오기
        float pageWidth = reader.getPageSize(signature.getPageNumber()).getWidth();
        float pageHeight = reader.getPageSize(signature.getPageNumber()).getHeight();

        // 프론트엔드 페이지 크기 (예: width=800, height=800)
        float frontEndPageWidth = 800;
        float frontEndPageHeight = 1131.5184378570948F;

        // 좌표 변환
        float scaleX = pageWidth / frontEndPageWidth;
        float scaleY = pageHeight / frontEndPageHeight;

        float pdfX = signature.getX() * scaleX;
        float pdfY = (frontEndPageHeight - signature.getY() - signature.getHeight()) * scaleY;
        float pdfWidth = signature.getWidth() * scaleX;
        float pdfHeight = signature.getHeight() * scaleY;

        Path imagePath = fileService.getSignatureFilePath(signature.getImageName());

        if (Files.exists(imagePath)) {
            Image signatureImage = Image.getInstance(imagePath.toString());
            signatureImage.scaleAbsolute(pdfWidth, pdfHeight);
            signatureImage.setAbsolutePosition(pdfX, pdfY);
            content.addImage(signatureImage);
        } else {
            throw new IOException("서명 이미지 파일을 찾을 수 없습니다: " + imagePath);
        }
    }
}
