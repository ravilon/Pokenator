package com.pokenator.config;

import com.pokenator.akinator.service.GameService;
import com.pokenator.akinator.service.QuestionSelector;
import com.pokenator.akinator.session.SessionManager;
import com.pokenator.rdf.InMemoryDatasetProvider;
import com.pokenator.rdf.RdfLoader;
import com.pokenator.repository.SpeciesRepository;
import com.pokenator.sparql.JenaSparqlClient;
import com.pokenator.sparql.SparqlClient;
import org.apache.jena.query.Dataset;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppBeans {

    @Bean
    public Dataset dataset() {
        Dataset dataset = new InMemoryDatasetProvider().getDataset();
        RdfLoader loader = new RdfLoader();
        loader.loadOntology(dataset, PokemonKgConfig.ONTOLOGY_PATH);
        loader.loadDataset(dataset, PokemonKgConfig.DATASET_PATH);
        return dataset;
    }

    @Bean
    public SparqlClient sparqlClient() {
        return new JenaSparqlClient(20000L);
    }

    @Bean
    public SpeciesRepository speciesRepository(SparqlClient sparqlClient) {
        return new SpeciesRepository(sparqlClient);
    }

    @Bean
    public SessionManager sessionManager() {
        return new SessionManager();
    }

    @Bean
    public QuestionSelector questionSelector(SpeciesRepository speciesRepository) {
        return new QuestionSelector(speciesRepository);
    }

    @Bean
    public GameService gameService(
            Dataset dataset,
            SessionManager sessionManager,
            SpeciesRepository speciesRepository,
            QuestionSelector questionSelector
    ) {
        return new GameService(dataset, sessionManager, speciesRepository, questionSelector);
    }
}
