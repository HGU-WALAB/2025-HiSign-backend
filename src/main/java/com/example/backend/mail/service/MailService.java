package com.example.backend.mail.service;

import com.example.backend.signatureRequest.entity.SignatureRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.List;

@Service
public class MailService {

    @Value("${custom.host.client}")
    private String client;
    @Value("${spring.mail.username}")
    private String emailAdress;
    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendSignatureRequestEmails(String senderName, String requestName ,List<SignatureRequest> requests) {
        for (SignatureRequest request : requests) {
            String recipientEmail = request.getSignerEmail();
            String token = request.getToken();
            String documentName = request.getDocument().getFileName();
            String signatureUrl =  client + "/sign?token=" + token;

            sendEmail(requestName, senderName ,recipientEmail, documentName, signatureUrl);
        }
    }

    public void sendEmail(String requestName, String from, String to, String documentName, String signatureUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 이메일 설정
            helper.setTo(to);
            helper.setSubject("서명 요청: " + requestName);
            helper.setFrom(emailAdress); // 보내는 사람을 전달받은 값으로 설정

            // 이메일 내용 (HTML)
            String emailContent = "<div style='background-color:#f4f8fb; padding:30px; font-family:Arial, sans-serif;'>"
                    + "<div style='max-width:600px; background-color:#ffffff; border-radius:10px; padding:20px; margin:auto; box-shadow:0px 4px 10px rgba(0,0,0,0.1);'>"
                    + "<h2 style='color:#0366d6; text-align:center;'>히즈사인(HISign) 전자 서명 요청</h2>"
                    + "<p style='font-size:16px; color:#333;'>안녕하세요, 사랑 · 겸손 · 봉사 정신의 한동대학교 전자 서명 서비스, <b>히즈사인(HISign)</b>입니다.</p>"
                    + "<p style='font-size:16px; color:#333;'><b>" + from + "</b>님으로부터 <b>'" + documentName + "'</b> 문서의 서명 요청이 도착하였습니다.</p>"
                    + "<p style='font-size:16px; color:#333;'>아래 링크를 클릭하여 서명을 진행해 주세요:</p>"
                    + "<div style='text-align:center; margin:20px 0;'>"
                    + "<a href='" + signatureUrl + "' style='background-color:#0366d6; color:#ffffff; text-decoration:none; padding:12px 20px; border-radius:5px; font-size:18px; display:inline-block;'>서명하기</a>"
                    + "</div>"
                    + "<p style='font-size:14px; color:#666; text-align:center;'>※ 본 메일은 자동 발송되었으며, 회신이 불가능합니다.</p>"
                    + "</div>"
                    + "</div>";

            helper.setText(emailContent, true); // HTML 템플릿 적용

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 전송 실패: " + e.getMessage(), e);
        }
    }
}
