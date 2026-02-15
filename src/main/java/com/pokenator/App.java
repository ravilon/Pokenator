package com.pokenator;

import com.pokenator.config.PokemonKgConfig;
import com.pokenator.rdf.DatasetProvider;
import com.pokenator.rdf.InMemoryDatasetProvider;
import com.pokenator.rdf.RdfLoader;
import com.pokenator.repository.SpeciesRepository;
import com.pokenator.sparql.JenaSparqlClient;
import com.pokenator.sparql.SparqlClient;
import org.apache.jena.query.Dataset;

public class App {

    public static void main(String[] args) {

        // Infra
        DatasetProvider provider = new InMemoryDatasetProvider();
        Dataset dataset = provider.getDataset();

        RdfLoader loader = new RdfLoader();
        loader.loadOntology(dataset, PokemonKgConfig.ONTOLOGY_PATH);
        loader.loadDataset(dataset, PokemonKgConfig.DATASET_PATH);

        // Query layer
        SparqlClient sparql = new JenaSparqlClient();
        SpeciesRepository speciesRepo = new SpeciesRepository(sparql);

        // Testes
        long total = speciesRepo.countSpecies(dataset);
        System.out.println("Species total: " + total);

        System.out.println("Species sample: " + speciesRepo.listSpecies(dataset, 10));

        System.out.println("Top predicates for Species:");
        speciesRepo.topPredicatesForSpecies(dataset, 15)
                .forEach(p -> System.out.println(" - " + p.predicateUri() + " => " + p.count()));
    }
}
