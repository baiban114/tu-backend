package com.tu.backend.storage.repository;

import com.tu.backend.storage.entity.FileAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileAssetRepository extends JpaRepository<FileAssetEntity, String> {
}
