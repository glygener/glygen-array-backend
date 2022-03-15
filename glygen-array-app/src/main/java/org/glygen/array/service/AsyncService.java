package org.glygen.array.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.OtherGlycan;
import org.glygen.array.persistence.rdf.SequenceDefinedGlycan;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.SmallMoleculeLinker;
import org.glygen.array.persistence.rdf.data.FileWrapper;
import org.glygen.array.persistence.rdf.data.Intensity;
import org.glygen.array.persistence.rdf.data.Slide;
import org.glygen.array.view.ErrorMessage;

public interface AsyncService {

    CompletableFuture<List<Intensity>> parseProcessDataFile(String datasetId, FileWrapper file, Slide slide, UserEntity user);
    CompletableFuture<String> importSlideLayout (SlideLayout slideLayout, ErrorMessage errorMessage, UserEntity user);
    
}
