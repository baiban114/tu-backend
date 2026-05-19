package com.tu.backend.reference.repository;

import com.tu.backend.reference.entity.InternalReferenceRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface InternalReferenceRecordRepository extends JpaRepository<InternalReferenceRecordEntity, String> {

    List<InternalReferenceRecordEntity> findByPageIdInOrderByUpdatedAtDescCreatedAtDesc(Collection<String> pageIds);

    List<InternalReferenceRecordEntity> findAllByOrderByUpdatedAtDescCreatedAtDesc();

    void deleteByPageId(String pageId);

    void deleteByPageIdIn(Collection<String> pageIds);
}
