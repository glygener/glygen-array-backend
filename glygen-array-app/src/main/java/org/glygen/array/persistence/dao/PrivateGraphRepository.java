package org.glygen.array.persistence.dao;

import org.glygen.array.persistence.PrivateGraphEntity;
import org.glygen.array.persistence.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivateGraphRepository extends JpaRepository<PrivateGraphEntity, Long> {

	PrivateGraphEntity findByUser(UserEntity user);
}
