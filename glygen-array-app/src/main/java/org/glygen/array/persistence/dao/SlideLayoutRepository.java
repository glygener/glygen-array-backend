package org.glygen.array.persistence.dao;

import org.glygen.array.persistence.SlideLayoutEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlideLayoutRepository extends JpaRepository<SlideLayoutEntity, String> {
    SlideLayoutEntity findByUri(String uri);
}
