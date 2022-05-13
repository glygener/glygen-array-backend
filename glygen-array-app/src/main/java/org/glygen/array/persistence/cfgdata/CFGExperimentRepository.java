package org.glygen.array.persistence.cfgdata;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CFGExperimentRepository extends JpaRepository<Experiment, Long> {
    Experiment findByPrimScreen (String experimentId);
}
