package org.glygen.array.persistence.dao;

import java.util.List;

import org.glygen.array.persistence.GraphPermissionEntity;
import org.glygen.array.persistence.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GraphPermissionRepository extends JpaRepository<GraphPermissionEntity, Long> {
	
	List<GraphPermissionEntity> findByUser(UserEntity user);
}
