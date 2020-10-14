package org.glygen.array.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.data.FileWrapper;
import org.glygen.array.persistence.rdf.data.Intensity;

public interface AsyncService {

    CompletableFuture<List<Intensity>> parseProcessDataFile(String datasetId, FileWrapper file, UserEntity user);

}
