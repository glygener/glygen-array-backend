package org.glygen.array.persistence.dao;

import org.glygen.array.persistence.SettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingsRepository extends JpaRepository<SettingEntity, String> {
	
	SettingEntity findByName(String name);

}
