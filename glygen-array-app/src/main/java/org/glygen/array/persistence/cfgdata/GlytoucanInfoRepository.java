package org.glygen.array.persistence.cfgdata;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GlytoucanInfoRepository extends JpaRepository<GlytoucanInfo, Long> {

    List<GlytoucanInfo> findByGlytoucanId(String glytoucanId);

}
