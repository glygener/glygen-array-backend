package org.glygen.array.service;

import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.BlockLayout;
import org.glygen.array.persistence.rdf.Feature;
import org.glygen.array.persistence.rdf.Glycan;
import org.glygen.array.persistence.rdf.Linker;

public interface AddToRepositoryService {
    String addLinker (Linker linker, Boolean unknown, UserEntity user);
    String addFeature(Feature feature, UserEntity user);
    String addBlockLayout (BlockLayout layout, Boolean noFeatureCheck, UserEntity user);
    String getSequenceFromGlytoucan(String glytoucanId);
    String addGlycan(Glycan glycan, UserEntity user, Boolean noGlytoucanRegistration, Boolean bypassGlytoucanCheck);

}
