package com.tu.backend.secret;

import com.tu.backend.secret.entity.ManagedSecretEntity;
import com.tu.backend.secret.repository.ManagedSecretRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManagedSecretService {

    private final ManagedSecretRepository repository;
    private final SecretCryptoService cryptoService;

    public ManagedSecretService(ManagedSecretRepository repository, SecretCryptoService cryptoService) {
        this.repository = repository;
        this.cryptoService = cryptoService;
    }

    @Transactional
    public ManagedSecretEntity save(String scope, String key, String displayName, String plaintext) {
        ManagedSecretEntity entity = repository.findByScopeAndKey(scope, key).orElseGet(() -> {
            ManagedSecretEntity created = new ManagedSecretEntity();
            created.setId(scope + ":" + key);
            created.setScope(scope);
            created.setKey(key);
            return created;
        });
        entity.setDisplayName(displayName);
        entity.setEncryptedValue(cryptoService.encrypt(plaintext));
        entity.setAlgorithm(SecretCryptoService.ALGORITHM);
        entity.setKeyVersion(SecretCryptoService.KEY_VERSION);
        return repository.save(entity);
    }

    @Transactional(readOnly = true)
    public Optional<String> getValue(String scope, String key) {
        return repository.findByScopeAndKey(scope, key)
            .map(ManagedSecretEntity::getEncryptedValue)
            .map(cryptoService::decrypt);
    }

    @Transactional(readOnly = true)
    public boolean exists(String scope, String key) {
        return repository.findByScopeAndKey(scope, key).isPresent();
    }

    @Transactional
    public void delete(String scope, String key) {
        repository.deleteByScopeAndKey(scope, key);
    }
}
