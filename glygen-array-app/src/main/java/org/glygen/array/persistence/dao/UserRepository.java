package org.glygen.array.persistence.dao;

import org.glygen.array.persistence.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

	public UserEntity findByEmailIgnoreCase(String email);
	public UserEntity findByUsernameIgnoreCase(String username);
}