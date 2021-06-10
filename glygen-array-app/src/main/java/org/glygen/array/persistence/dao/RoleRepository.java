package org.glygen.array.persistence.dao;

import java.util.List;

import org.glygen.array.persistence.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

	RoleEntity findByRoleName(String roleName);
	
	@Modifying
    @Query("select r.roleName from roles r")
    List<String> listRoleNames();
}
