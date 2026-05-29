package com.tu.backend.secret;

import com.tu.backend.common.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class SecretCryptoService {

    public static final String ALGORITHM = "AES/GCM/NoPadding";
    public static final String KEY_VERSION = "v1";
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public SecretCryptoService(SecretProperties properties) {
        this.properties = properties;
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return KEY_VERSION + ":"
                + Base64.getEncoder().encodeToString(iv) + ":"
                + Base64.getEncoder().encodeToString(encrypted);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(50020, "failed to encrypt secret");
        }
    }

    public String decrypt(String encryptedValue) {
        try {
            String[] parts = encryptedValue == null ? new String[0] : encryptedValue.split(":", 3);
            if (parts.length != 3 || !KEY_VERSION.equals(parts[0])) {
                throw new BusinessException(50021, "invalid encrypted secret format");
            }
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] encrypted = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, masterKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(50022, "failed to decrypt secret");
        }
    }

    private SecretKeySpec masterKey() {
        String encoded = properties.getEncryptionKey();
        if (encoded == null || encoded.isBlank()) {
            throw new BusinessException(50023, "secret encryption key is not configured");
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(encoded.trim());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(50024, "secret encryption key must be base64");
        }
        if (key.length != KEY_LENGTH_BYTES) {
            throw new BusinessException(50025, "secret encryption key must be 32 bytes");
        }
        return new SecretKeySpec(key, "AES");
    }
}
