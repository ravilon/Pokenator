package com.pokenator;

import com.pokenator.akinator.model.Answer;
import com.pokenator.akinator.service.GameService;
import com.pokenator.akinator.service.QuestionSelector;
import com.pokenator.akinator.session.SessionManager;
import com.pokenator.config.PokemonKgConfig;
import com.pokenator.rdf.DatasetProvider;
import com.pokenator.rdf.InMemoryDatasetProvider;
import com.pokenator.rdf.RdfLoader;
import com.pokenator.repository.SpeciesRepository;
import com.pokenator.sparql.JenaSparqlClient;
import com.pokenator.sparql.SparqlClient;
import org.apache.jena.query.Dataset;

import java.util.Scanner;

public class App {

    public static void main(String[] args) {

        // Load dataset once
        DatasetProvider provider = new InMemoryDatasetProvider();
        Dataset dataset = provider.getDataset();

        RdfLoader loader = new RdfLoader();
        loader.loadOntology(dataset, PokemonKgConfig.ONTOLOGY_PATH);
        loader.loadDataset(dataset, PokemonKgConfig.DATASET_PATH);

        // Infra + repos + services
        SparqlClient sparql = new JenaSparqlClient(20000);
        SpeciesRepository speciesRepo = new SpeciesRepository(sparql);

        SessionManager sessions = new SessionManager();
        QuestionSelector selector = new QuestionSelector(speciesRepo);
        GameService game = new GameService(dataset, sessions, speciesRepo, selector);

        // Start game
        var start = game.start();
        String sessionId = start.sessionId();

        System.out.println("Session: " + sessionId);
        System.out.println(start.question().text());

        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.print("Answer (y/n): ");
            String in = sc.nextLine().trim().toLowerCase();

            Answer ans = in.startsWith("y") ? Answer.YES : Answer.NO;

            var step = game.answer(sessionId, ans);

            if (step instanceof GameService.NextStepResult.Guess(String speciesUri)) {
                System.out.println("My guess: " + speciesUri);
                break;
            } else if (step instanceof GameService.NextStepResult.NoCandidates) {
                System.out.println("No candidates left (constraints too strict).");
                break;
            } else if (step instanceof GameService.NextStepResult.NextQuestion(
                    com.pokenator.akinator.model.Question question, long remainingCandidates
            )) {
                System.out.println("Remaining candidates: " + remainingCandidates);
                System.out.println(question.text());
            }
        }
    }
}
