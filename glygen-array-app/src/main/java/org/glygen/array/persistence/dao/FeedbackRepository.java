package org.glygen.array.persistence.dao;

import org.glygen.array.persistence.FeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<FeedbackEntity, Long> {

}
