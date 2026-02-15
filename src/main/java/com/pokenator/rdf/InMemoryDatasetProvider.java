package com.pokenator.rdf;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;

public class InMemoryDatasetProvider implements DatasetProvider {

    private final Dataset dataset;

    public InMemoryDatasetProvider() {
        this.dataset = DatasetFactory.createTxnMem();
    }

    @Override
    public Dataset getDataset() {
        return dataset;
    }
}
