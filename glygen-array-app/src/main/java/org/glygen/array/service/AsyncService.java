package org.glygen.array.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.LinkerType;
import org.glygen.array.persistence.rdf.SlideLayout;
import org.glygen.array.persistence.rdf.data.FileWrapper;
import org.glygen.array.persistence.rdf.data.Intensity;
import org.glygen.array.persistence.rdf.data.Slide;
import org.glygen.array.util.ParserConfiguration;
import org.glygen.array.view.Confirmation;
import org.glygen.array.view.ErrorMessage;

public interface AsyncService {

    CompletableFuture<List<Intensity>> parseProcessDataFile(String datasetId, FileWrapper file, Slide slide, UserEntity user);
    CompletableFuture<String> importSlideLayout (SlideLayout slideLayout, ErrorMessage errorMessage, UserEntity user);
    CompletableFuture<Confirmation> addGlycansFromExportFile(byte[] contents,
            Boolean noGlytoucanRegistration, UserEntity user, ErrorMessage errorMessage);
    CompletableFuture<Confirmation> addGlycanFromCSVFile(byte[] contents, Boolean noGlytoucanRegistration,
            UserEntity user, ErrorMessage errorMessage, ParserConfiguration config);
    CompletableFuture<Confirmation> addGlycanFromTextFile(byte[] contents, Boolean noGlytoucanRegistration,
            UserEntity user, ErrorMessage errorMessage, String format, String delimeter);
    CompletableFuture<Confirmation> addGlycanFromLibraryFile(byte[] contents, Boolean noGlytoucanRegistration,
            UserEntity user, ErrorMessage errorMessage);
    CompletableFuture<Confirmation> addFeaturesFromExportFile(byte[] contents, UserEntity user, ErrorMessage errorMessage);
    CompletableFuture<Confirmation> addLinkersFromExportFile(byte[] contents, LinkerType type, UserEntity user, ErrorMessage errorMessage);
    
}
