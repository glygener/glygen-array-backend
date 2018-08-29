package org.glygen.array.persistence.dao;

import org.glygen.array.persistence.WebAccessLoggingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebAccessLoggingDAO extends JpaRepository<WebAccessLoggingEntity, Long> {

}
