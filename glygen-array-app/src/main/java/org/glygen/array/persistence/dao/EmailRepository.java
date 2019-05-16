package org.glygen.array.persistence.dao;

import org.glygen.array.persistence.EmailChangeEntity;
import org.glygen.array.persistence.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailRepository extends JpaRepository<EmailChangeEntity, Long> {

	EmailChangeEntity findByUser (UserEntity user);
}
