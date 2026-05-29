package com.tu.backend.secret;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tu.backend.common.BusinessException;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class SecretCryptoServiceTest {

    @Test
    void encryptsAndDecryptsWithRandomIv() {
        SecretCryptoService service = new SecretCryptoService(propertiesWithKey());

        String first = service.encrypt("sk-test");
        String second = service.encrypt("sk-test");

        assertThat(first).startsWith("v1:");
        assertThat(second).startsWith("v1:");
        assertThat(first).isNotEqualTo(second);
        assertThat(service.decrypt(first)).isEqualTo("sk-test");
        assertThat(service.decrypt(second)).isEqualTo("sk-test");
    }

    @Test
    void rejectsMissingMasterKey() {
        SecretCryptoService service = new SecretCryptoService(new SecretProperties());

        assertThatThrownBy(() -> service.encrypt("secret"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("secret encryption key is not configured");
    }

    @Test
    void rejectsWrongMasterKeyLength() {
        SecretProperties properties = new SecretProperties();
        properties.setEncryptionKey(Base64.getEncoder().encodeToString("short".getBytes()));
        SecretCryptoService service = new SecretCryptoService(properties);

        assertThatThrownBy(() -> service.encrypt("secret"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("secret encryption key must be 32 bytes");
    }

    static SecretProperties propertiesWithKey() {
        SecretProperties properties = new SecretProperties();
        properties.setEncryptionKey(Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes()));
        return properties;
    }
}
