package com.tu.backend.taskintegration.repository;

import com.tu.backend.taskintegration.entity.IntegrationConnectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IntegrationConnectionRepository extends JpaRepository<IntegrationConnectionEntity, String> {
    List<IntegrationConnectionEntity> findByProviderOrProviderIsNullOrderByUpdatedAtDescCreatedAtDesc(String provider);

    Optional<IntegrationConnectionEntity> findFirstByProviderAndEnabledTrueOrderByUpdatedAtDescCreatedAtDesc(String provider);

    Optional<IntegrationConnectionEntity> findFirstByProviderOrderByUpdatedAtDescCreatedAtDesc(String provider);
}
