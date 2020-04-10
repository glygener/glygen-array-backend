package org.glygen.array.service;

import java.sql.SQLException;
import java.util.List;

import org.glygen.array.exception.SparqlException;
import org.glygen.array.persistence.UserEntity;
import org.glygen.array.persistence.rdf.data.ArrayDataset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(value="sesameTransactionManager") 
public class ArrayDatasetRepositoryImpl extends GlygenArrayRepositoryImpl implements ArrayDatasetRepository {

    @Override
    public String addArrayDataset(ArrayDataset dataset, UserEntity user) throws SparqlException, SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ArrayDataset getArrayDataset(String datasetId, UserEntity user) throws SparqlException, SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<ArrayDataset> getArrayDatasetByUser(UserEntity user) throws SparqlException, SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteArrayDataset(String datasetId, UserEntity user) throws SparqlException, SQLException {
        // TODO Auto-generated method stub
        
    }

}
