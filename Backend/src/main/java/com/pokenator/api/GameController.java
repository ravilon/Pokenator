package com.pokenator.api;

import com.pokenator.akinator.model.GameState;
import com.pokenator.akinator.service.GameService;
import com.pokenator.api.dto.AnswerRequest;
import com.pokenator.api.dto.ApiQuestion;
import com.pokenator.api.dto.CandidateDto;
import com.pokenator.api.dto.CandidateListResponse;
import com.pokenator.api.dto.GameStartResponse;
import com.pokenator.api.dto.GameStepResponse;
import com.pokenator.repository.SpeciesRepository;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class GameController {

    private final GameService gameService;
    private final SpeciesRepository speciesRepository;
    private final Dataset dataset;

    public GameController(GameService gameService, SpeciesRepository speciesRepository, Dataset dataset) {
        this.gameService = gameService;
        this.speciesRepository = speciesRepository;
        this.dataset = dataset;
    }

    @GetMapping("/game/{sessionId}/candidates")
    public CandidateListResponse listCandidates(@PathVariable String sessionId) {
        GameState state = gameService.getState(sessionId);
        List<String> uris = speciesRepository.listCandidates(dataset, state.getConstraints(), 1000);
        List<CandidateDto> list = new ArrayList<>();
        for (String uri : uris) {
            String label = speciesRepository.getEnglishLabel(dataset, uri);
            list.add(new CandidateDto(uri, label));
        }
        return new CandidateListResponse(list);
    }


    @GetMapping("/health")
    public String health() {
        return "ok";
    }

    @PostMapping("/game/start")
    public GameStartResponse start() {
        var start = gameService.start();
        var q = start.question();

        return new GameStartResponse(
                start.sessionId(),
                new ApiQuestion(q.text(), q.kind().name(), q.predicateUri(), q.objectUri())
        );
    }

    @PostMapping("/game/{sessionId}/answer")
    public GameStepResponse answer(
            @PathVariable String sessionId,
            @RequestBody AnswerRequest req
    ) {
        if (req == null || req.answer() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field 'answer' is required: YES|NO|UNKNOWN");
        }

        GameService.NextStepResult step;
        try {
            step = gameService.answer(sessionId, req.answer());
        } catch (RuntimeException ex) {
            // Ex.: sessionId não encontrado no SessionManager
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid sessionId", ex);
        }

        // Java 21 record pattern matching
        if (step instanceof GameService.NextStepResult.Guess(String speciesUri)) {
            String label = speciesRepository.getEnglishLabel(dataset, speciesUri);

            return new GameStepResponse(
                    "GUESS",
                    null,
                    null,
                    speciesUri,
                    label
            );
        }

        if (step instanceof GameService.NextStepResult.NoCandidates) {
            return new GameStepResponse(
                    "NO_CANDIDATES",
                    null,
                    null,
                    null,
                    null
            );
        }

        if (step instanceof GameService.NextStepResult.NextQuestion(var question, long remainingCandidates)) {
            return new GameStepResponse(
                    "QUESTION",
                    remainingCandidates,
                    new ApiQuestion(question.text(), question.kind().name(), question.predicateUri(), question.objectUri()),
                    null,
                    null
            );
        }

        // Fallback (não deveria acontecer)
        return new GameStepResponse(
                "NO_CANDIDATES",
                null,
                null,
                null,
                null
        );
    }
}
