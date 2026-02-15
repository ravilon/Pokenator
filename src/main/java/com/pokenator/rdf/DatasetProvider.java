package com.pokenator.rdf;

import org.apache.jena.query.Dataset;

public interface DatasetProvider {
    Dataset getDataset();
}
