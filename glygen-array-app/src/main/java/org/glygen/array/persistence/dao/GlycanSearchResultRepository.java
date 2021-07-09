package org.glygen.array.persistence.dao;

import org.glygen.array.persistence.GlycanSearchResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlycanSearchResultRepository extends JpaRepository<GlycanSearchResultEntity, String> {
    GlycanSearchResultEntity findBySequence (String sequence);
}
