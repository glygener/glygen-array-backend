package org.glygen.array.persistence.dao;

import org.glygen.array.persistence.WebEventLoggingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebEventLoggingDAO extends JpaRepository<WebEventLoggingEntity, Long> {

}
