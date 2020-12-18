package org.glygen.array.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.Spot;
import org.glygen.array.persistence.rdf.data.FileWrapper;
import org.glygen.array.persistence.rdf.data.Intensity;
import org.glygen.array.persistence.rdf.data.Measurement;

public interface AsyncService {

    CompletableFuture<List<Intensity>> parseProcessDataFile(String datasetId, FileWrapper file, UserEntity user);
    CompletableFuture<Map<Measurement, Spot>> parseRawDataFile(FileWrapper file, SlideLayout layout, Double powerLevel);

}
