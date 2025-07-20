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

import java.awt.Color;
import com.lowagie.text.pdf.PdfGState;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final DocumentService documentService;
    private final FileService fileService;
    private static final Color[] DEFAULT_COLORS = {
            new Color(0xFF, 0x6B, 0x6B), // #FF6B6B
            new Color(0x4E, 0xCD, 0xC4), // #4ECDC4
            new Color(0xFF, 0xD9, 0x3D), // #FFD93D
            new Color(0x1A, 0x53, 0x5C), // #1A535C
            new Color(0xFF, 0x9F, 0x1C), // #FF9F1C
            new Color(0x6A, 0x4C, 0x93)  // #6A4C93
    };

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

    public byte[] generateReviewDocument(Long documentId, List<Signature> signatures) throws IOException, DocumentException {
        Document document = documentService.getDocumentById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다."));

        Path originalPdfPath = fileService.getDocumentFilePath(document.getSavedFileName());
        PdfReader reader = new PdfReader(originalPdfPath.toString());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfStamper stamper = new PdfStamper(reader, outputStream);

        Map<String, Color> signerColorMap = new HashMap<>();
        int colorIndex = 0;

        for (Signature sig : signatures) {
            String email = sig.getSignerEmail();
            if (!signerColorMap.containsKey(email)) {
                Color color = DEFAULT_COLORS[colorIndex % DEFAULT_COLORS.length];
                signerColorMap.put(email, color);
                colorIndex++;
            }
        }

        for (Signature signature : signatures) {
            String email = signature.getSignerEmail();
            Color color = signerColorMap.get(email);
            boolean isCompleted = signature.getStatus() == 1;
            if (isCompleted) {
                addSignatureToPdf(stamper, signature, reader);
                drawSolidBorderBox(stamper, signature, reader, color);
            } else {
                drawEmptySignatureBox(stamper, signature, color);
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

    private void drawEmptySignatureBox(PdfStamper stamper, Signature signature, Color strokeColor) {
        int pageNumber = signature.getPageNumber();
        PdfContentByte canvas = stamper.getOverContent(pageNumber);
        canvas.saveState();

        // ✅ PDF 페이지 크기
        float pageWidth = stamper.getReader().getPageSize(pageNumber).getWidth();
        float pageHeight = stamper.getReader().getPageSize(pageNumber).getHeight();

        // ✅ 프론트 기준 크기
        float frontEndPageWidth = 800;
        float frontEndPageHeight = 1131.5184378570948F;

        float scaleX = pageWidth / frontEndPageWidth;
        float scaleY = pageHeight / frontEndPageHeight;

        float x = signature.getX() * scaleX;
        float y = (frontEndPageHeight - signature.getY() - signature.getHeight()) * scaleY;
        float width = signature.getWidth() * scaleX;
        float height = signature.getHeight() * scaleY;
        float radius = 6f;

        PdfGState gState = new PdfGState();
        gState.setFillOpacity(0.125f);
        canvas.setGState(gState);

        canvas.setColorFill(strokeColor);
        canvas.setColorStroke(strokeColor);
        canvas.setLineWidth(2f);
        canvas.setLineDash(4f, 4f);

        drawRoundedRect(canvas, x, y, width, height, radius);
        canvas.fillStroke();

        canvas.restoreState();
    }

    private void drawSolidBorderBox(PdfStamper stamper, Signature signature, PdfReader reader, Color strokeColor) {
        int pageNumber = signature.getPageNumber();
        PdfContentByte canvas = stamper.getOverContent(pageNumber);
        canvas.saveState();

        // 좌표 변환
        float pageWidth = reader.getPageSize(pageNumber).getWidth();
        float pageHeight = reader.getPageSize(pageNumber).getHeight();
        float frontEndPageWidth = 800;
        float frontEndPageHeight = 1131.5184378570948F;

        float scaleX = pageWidth / frontEndPageWidth;
        float scaleY = pageHeight / frontEndPageHeight;

        float x = signature.getX() * scaleX;
        float y = (frontEndPageHeight - signature.getY() - signature.getHeight()) * scaleY;
        float width = signature.getWidth() * scaleX;
        float height = signature.getHeight() * scaleY;
        float radius = 6f;

        // 스타일 설정
        canvas.setColorStroke(strokeColor);
        canvas.setLineWidth(2f);
        canvas.setLineDash(0f); // 실선

        drawRoundedRect(canvas, x, y, width, height, radius);
        canvas.stroke(); // 배경 없이 테두리만

        canvas.restoreState();
    }



    private void drawRoundedRect(PdfContentByte canvas, float x, float y, float width, float height, float radius) {
        float r = radius;
        float b = 0.4477f; // Bézier approximation constant for round corners

        // 시작
        canvas.moveTo(x + r, y);

        // 하단 오른쪽 코너
        canvas.lineTo(x + width - r, y);
        canvas.curveTo(x + width - r * (1 - b), y, x + width, y + r * (1 - b), x + width, y + r);

        // 우측 상단 코너
        canvas.lineTo(x + width, y + height - r);
        canvas.curveTo(x + width, y + height - r * (1 - b), x + width - r * (1 - b), y + height, x + width - r, y + height);

        // 좌측 상단 코너
        canvas.lineTo(x + r, y + height);
        canvas.curveTo(x + r * (1 - b), y + height, x, y + height - r * (1 - b), x, y + height - r);

        // 좌측 하단 코너
        canvas.lineTo(x, y + r);
        canvas.curveTo(x, y + r * (1 - b), x + r * (1 - b), y, x + r, y);

        canvas.closePath();
    }


}
