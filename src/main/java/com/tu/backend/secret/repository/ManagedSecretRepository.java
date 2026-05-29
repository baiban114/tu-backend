package com.tu.backend.secret.repository;

import com.tu.backend.secret.entity.ManagedSecretEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManagedSecretRepository extends JpaRepository<ManagedSecretEntity, String> {

    Optional<ManagedSecretEntity> findByScopeAndKey(String scope, String key);

    void deleteByScopeAndKey(String scope, String key);
}
