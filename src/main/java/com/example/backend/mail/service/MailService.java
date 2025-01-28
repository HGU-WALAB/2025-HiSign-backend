package com.example.backend.mail.service;

import com.example.backend.signatureRequest.entity.SignatureRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.List;

@Service
public class MailService {
    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendSignatureRequestEmails(List<SignatureRequest> requests) {
        for (SignatureRequest request : requests) {
            String recipientEmail = request.getSignerEmail();
            String token = request.getToken();
            String documentName = request.getDocument().getFileName();
            //String signatureUrl = "https://your-site.com/sign?token=" + token;
            String signatureUrl = "http://localhost:8080/sign?token=" + token;

            sendEmail(recipientEmail, documentName, signatureUrl);
        }
    }

    private void sendEmail(String to, String documentName, String signatureUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("서명 요청: " + documentName);
            helper.setText("<p>안녕하세요,</p>"
                    + "<p>아래 링크를 클릭하여 서명을 진행해 주세요:</p>"
                    + "<p><a href='" + signatureUrl + "'>서명 링크</a></p>", true);
            helper.setFrom("HISign@gmail.com");

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 전송 실패: " + e.getMessage(), e);
        }
    }
}
