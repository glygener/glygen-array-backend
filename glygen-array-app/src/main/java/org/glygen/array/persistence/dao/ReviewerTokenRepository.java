package org.glygen.array.persistence.dao;

import org.glygen.array.persistence.ReviewerTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewerTokenRepository extends JpaRepository<ReviewerTokenEntity, Long> {
	ReviewerTokenEntity findByToken (String token);
}
