package com.example.backend.mail.service;

import com.example.backend.auth.util.EncryptionUtil;
import com.example.backend.document.entity.Document;
import com.example.backend.signatureRequest.entity.SignatureRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    @Value("${custom.host.client}")
    private String client;
    @Value("${spring.mail.username}")
    private String emailAdress;
    private final JavaMailSender mailSender;
    private final EncryptionUtil encryptionUtil;


    public void sendSignatureRequestEmails(String senderName, String requestName ,List<SignatureRequest> requests, String password) throws Exception {
        for (SignatureRequest request : requests) {
            String recipientEmail = request.getSignerEmail();
            String token = request.getToken();
            String documentName = request.getDocument().getFileName();
            String description = request.getDocument().getDescription();
            String encryptedToken = encryptionUtil.encryptUUID(token);
            //배포되었을 시에 서명 url에 basename "/hisign"이 추가되어야함
            String signatureUrl =  client +"/hisign"+ "/checkEmail?token=" + encryptedToken;

            sendEmail(requestName, senderName ,recipientEmail, documentName, description, signatureUrl,password);
        }
    }

    public void sendSignatureRequestEmailsWithoutPassword (String senderName, String requestName ,List<SignatureRequest> requests) throws Exception {
        for (SignatureRequest request : requests) {
            String recipientEmail = request.getSignerEmail();
            String token = request.getToken();
            String documentName = request.getDocument().getFileName();
            String description = request.getDocument().getDescription();
            String encryptedToken = encryptionUtil.encryptUUID(token);
            //배포되었을 시에 서명 url에 basename "/hisign"이 추가되어야함
            String signatureUrl =  client +"/hisign"+ "/checkEmail?token=" + encryptedToken;

            sendEmail(requestName, senderName ,recipientEmail, documentName, description, signatureUrl,"NONE");
        }
    }

    public void sendEmail(String requestName, String from, String to, String documentName, String description, String signatureUrl, String password) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 이메일 기본 설정
            helper.setTo(to);
            helper.setSubject("[서명 요청] " + requestName);
            helper.setFrom(emailAdress);

            // ✅ 비밀번호 블록 생성 (password가 있을 때만)
            String passwordBlock = "";
            if (password != null && !"NONE".equals(password)) {
                passwordBlock =
                        "<div style='background-color:#fff6e5; padding:15px; border-radius:5px; border-left:5px solid #ff9900; margin:15px 0;'>"
                                + "<p style='font-size:16px; font-weight:bold; color:#ff9900; margin:0;'>🔐 접근 비밀번호:</p>"
                                + "<p style='font-size:18px; font-weight:bold; color:#333; margin:5px 0 0 0; text-align:center;'>" + password + "</p>"
                                + "</div>";
            }

            // ✅ 최종 이메일 내용
            String emailContent = "<!DOCTYPE html>"
                    + "<html lang='ko'>"
                    + "<head>"
                    + "<meta charset='UTF-8'>"
                    + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                    + "<style>"
                    + "  body { margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f8fb; }"
                    + "  .container { max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 10px; padding: 20px; box-shadow: 0px 4px 10px rgba(0,0,0,0.1); }"
                    + "  .btn { background-color: #0366d6; color: #ffffff; text-decoration: none; padding: 12px 20px; border-radius: 5px; font-size: 18px; display: inline-block; margin-top: 20px; }"
                    + "  .highlight-block { background-color: #fff6e5; padding: 15px; border-radius: 5px; border-left: 5px solid #ff9900; margin: 15px 0; }"
                    + "  .request-block { background-color: #eef6ff; padding: 15px; border-radius: 5px; border-left: 5px solid #0366d6; margin: 15px 0; }"
                    + "  @media (max-width: 600px) {"
                    + "    .container { padding: 10px; }"
                    + "    .btn { font-size: 16px; padding: 10px 16px; }"
                    + "  }"
                    + "</style>"
                    + "</head>"
                    + "<body>"
                    + "<div class='container'>"
                    + "<h2 style='color:#0366d6; text-align:center;'>HISign 전자 서명 요청</h2>"
                    + "<p style='font-size:16px; color:#333;'>안녕하세요, 사랑 · 겸손 · 봉사 정신의 한동대학교 전자 서명 서비스 <b>HISign</b>입니다.</p>"
                    + "<p style='font-size:16px; color:#333;'><b>" + from + "</b>님으로부터 <b>'" + documentName + "'</b> 문서의 서명 요청이 도착하였습니다.</p>"
                    + "<p style='font-size:16px; color:#333;'>아래 링크를 클릭하여 서명을 진행해 주세요.</p>"

// ✅ passwordBlock 삽입 (만약 있을 경우)
                    + passwordBlock

                    + "<div class='request-block'>"
                    + "<p style='font-size:16px; font-weight:bold; color:#0366d6;'>📌 요청사항:</p>"
                    + "<p style='font-size:16px; color:#333; font-style:italic; font-weight:bold;'>" + description + "</p>"
                    + "</div>"

                    + "<div style='text-align:center;'>"
                    + "<a href='" + signatureUrl + "' class='btn' style='color: #ffffff;>서명하기</a>"
                    + "</div>"

                    + "<p style='font-size:14px; color:#666; text-align:center;'>※ 본 메일은 자동 발송되었으며 회신이 불가능합니다.</p>"
                    + "</div>"
                    + "</body>"
                    + "</html>";

            helper.setText(emailContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 전송 실패: " + e.getMessage(), e);
        }
    }

    public void sendCompletedSignatureMail(String recipientEmail, Document document, byte[] pdfData) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            message.removeHeader("In-Reply-To");
            message.removeHeader("References");
            message.setHeader("Message-ID", "<" + UUID.randomUUID() + "@hisign.domain>");
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipientEmail);
            helper.setSubject("[서명 완료] " + document.getRequestName());
            helper.setFrom(emailAdress);

            // ✅ 메일 내용 (반응형 적용)
            String emailContent = "<!DOCTYPE html>"
                    + "<html lang='ko'>"
                    + "<head>"
                    + "<meta charset='UTF-8'>"
                    + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                    + "<style>"
                    + "  body { margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f8fb; }"
                    + "  .container { max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 10px; padding: 20px; box-shadow: 0px 4px 10px rgba(0,0,0,0.1); }"
                    + "  @media (max-width: 600px) {"
                    + "    .container { padding: 10px; }"
                    + "  }"
                    + "</style>"
                    + "</head>"
                    + "<body>"
                    + "<div class='container'>"
                    + "<h2 style='color:#0366d6; text-align:center;'>HISign 서명 완료 안내</h2>"
                    + "<p style='font-size:16px; color:#333;'>안녕하세요, 사랑 · 겸손 · 봉사 정신의 한동대학교 전자 서명 서비스 <b>HISign</b>입니다.</p>"
                    + "<p style='font-size:16px; color:#333;'><b>" + safeText(document.getMember().getName()) + "</b>님이 요청한 <b>'" + safeText(document.getFileName()) + "'</b> 문서의 서명이 <b>완료</b>되었습니다.</p>"

// ✅ 타입 1이 아닌 경우에만 "첨부 안내 문구" 삽입
                    + (document.getType() != 1
                    ? "<p style='font-size:16px; color:#333;'>완료된 서명 문서가 첨부되어 있으니 확인해 주세요.</p>"
                    : "<p style='font-size:16px; color:#333;'>완료된 서명이 정상적으로 접수되었습니다.</p>")

                    + "<p style='font-size:14px; color:#666; text-align:center;'>※ 본 메일은 자동 발송되었으며, 회신이 불가능합니다.</p>"
                    + "</div>"
                    + "</body>"
                    + "</html>";

            helper.setText(emailContent, true);

            // ✅ 타입이 1이 아닐 때만 PDF 파일 첨부
            if (document.getType() != 1) {
                String fileName = document.getFileName();
                if (!fileName.toLowerCase().endsWith(".pdf")) {
                    fileName += ".pdf";
                }
                helper.addAttachment(fileName, new ByteArrayResource(pdfData));
            } else {
                log.info("✅ 타입 1 문서: PDF 첨부 생략");
            }

            mailSender.send(message);
        } catch (MailSendException e) {
            log.error("❌ 이메일 전송 실패 (SMTP 문제): {}", e.getMessage(), e);
            throw new RuntimeException("이메일 전송 중 SMTP 오류 발생", e);
        } catch (MessagingException e) {
            log.error("⚠️ 이메일 메시지 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("이메일 메시지 생성 실패", e);
        }
    }
    private String safeText(String input) {
        return input != null ? input : "";
    }
}
