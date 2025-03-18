package com.example.backend.auth.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class EncryptionUtil {

    private final byte[] SECRET_KEY;

    public EncryptionUtil(@Value("${custom.aes.secret}") String aesSecret) {
        this.SECRET_KEY = Base64.getDecoder().decode(aesSecret); // Base64 디코딩
        if (SECRET_KEY.length != 16 && SECRET_KEY.length != 24 && SECRET_KEY.length != 32) {
            throw new IllegalArgumentException("AES 키 길이가 올바르지 않습니다. (16, 24, 32 바이트 중 하나여야 함)");
        }
    }

    // 🔹 UUID 암호화 (URL-Safe Base64 적용)
    public String encryptUUID(String uuid) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // ✅ 패딩 적용
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encryptedBytes = cipher.doFinal(uuid.getBytes());

        // ✅ URL-Safe Base64 인코딩 적용
        return Base64.getUrlEncoder().encodeToString(encryptedBytes);
    }

    // 🔹 UUID 복호화 (URL-Safe Base64 디코딩 적용)
    public String decryptUUID(String encryptedUuid) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // ✅ 패딩 적용
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY, "AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);

        // ✅ URL-Safe Base64 디코딩 후 복호화
        byte[] decodedBytes = Base64.getUrlDecoder().decode(encryptedUuid);
        return new String(cipher.doFinal(decodedBytes));
    }
}
