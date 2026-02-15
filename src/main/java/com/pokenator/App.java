package com.pokenator;

import com.pokenator.config.PokemonKgConfig;
import com.pokenator.rdf.DatasetProvider;
import com.pokenator.rdf.InMemoryDatasetProvider;
import com.pokenator.rdf.RdfLoader;
import org.apache.jena.query.Dataset;

public class App {

    public static void main(String[] args) {

        DatasetProvider provider = new InMemoryDatasetProvider();
        Dataset dataset = provider.getDataset();

        RdfLoader loader = new RdfLoader();

        loader.loadOntology(dataset, PokemonKgConfig.ONTOLOGY_PATH);
        loader.loadDataset(dataset, PokemonKgConfig.DATASET_PATH);

        System.out.println("Dataset carregado com sucesso.");
    }
}
