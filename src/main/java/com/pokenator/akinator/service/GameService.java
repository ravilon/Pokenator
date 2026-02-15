package com.pokenator.akinator.service;

import com.pokenator.akinator.model.Answer;
import com.pokenator.akinator.model.Constraint;
import com.pokenator.akinator.model.GameState;
import com.pokenator.akinator.model.Question;
import com.pokenator.akinator.model.QuestionKind;
import com.pokenator.akinator.session.SessionManager;
import com.pokenator.repository.SpeciesRepository;
import org.apache.jena.query.Dataset;

import java.util.List;

public class GameService {

    private final Dataset dataset; // global (read-only)
    private final SessionManager sessions;
    private final SpeciesRepository speciesRepo;
    private final QuestionSelector selector;

    public GameService(Dataset dataset,
                       SessionManager sessions,
                       SpeciesRepository speciesRepo,
                       QuestionSelector selector) {
        this.dataset = dataset;
        this.sessions = sessions;
        this.speciesRepo = speciesRepo;
        this.selector = selector;
    }

    public StartGameResult start() {
        GameState state = sessions.create();
        Question q = selector.nextQuestion(dataset, state);

        // If selector couldn't build anything, fallback to guess
        if (isNoQuestion(q)) {
            q = buildGuessQuestion(state);
        }

        state.setLastQuestion(q);
        state.incrementStep();
        return new StartGameResult(state.getSessionId(), q);
    }

    public NextStepResult answer(String sessionId, Answer answer) {
        GameState state = sessions.get(sessionId);
        Question last = state.getLastQuestion();

        // 1) If last was a GUESS, handle differently
        if (last != null && last.kind() == QuestionKind.GUESS) {

            if (answer == Answer.YES) {
                // Confirmed!
                return NextStepResult.guess(last.objectUri()); // objectUri holds species URI
            }

            // User said NO -> guess the other one if we have 2
            List<String> candidates = speciesRepo.listCandidates(dataset, state.getConstraints(), 2);

            if (candidates.size() == 2) {
                String other = candidates.get(0).equals(last.objectUri()) ? candidates.get(1) : candidates.get(0);
                return NextStepResult.guess(other);
            }

            // No other candidate available
            return NextStepResult.noCandidates();
        }

        // 2) If there is no actionable question, generate next or guess
        if (last == null || last.objectUri() == null || last.objectUri().isBlank()) {
            return nextOrGuess(state);
        }

        // 3) Normal HAS_VALUE flow: add constraint from last question
        state.addConstraint(new Constraint(last.predicateUri(), last.objectUri(), answer));

        long remaining = speciesRepo.countCandidates(dataset, state.getConstraints());

        // 4) If small set, switch to GUESS mode immediately
        if (remaining <= 2) {
            Question gq = buildGuessQuestion(state);
            state.setLastQuestion(gq);
            state.incrementStep();
            return NextStepResult.nextQuestion(gq, remaining);
        }

        // 5) Otherwise keep asking questions
        Question next = selector.nextQuestion(dataset, state);

        // If selector got stuck, fallback to guess
        if (isNoQuestion(next)) {
            Question gq = buildGuessQuestion(state);
            state.setLastQuestion(gq);
            state.incrementStep();
            return NextStepResult.nextQuestion(gq, remaining);
        }

        state.setLastQuestion(next);
        state.incrementStep();
        return NextStepResult.nextQuestion(next, remaining);
    }

    private NextStepResult nextOrGuess(GameState state) {
        long remaining = speciesRepo.countCandidates(dataset, state.getConstraints());

        if (remaining <= 2) {
            Question gq = buildGuessQuestion(state);
            state.setLastQuestion(gq);
            state.incrementStep();
            return NextStepResult.nextQuestion(gq, remaining);
        }

        Question q = selector.nextQuestion(dataset, state);
        if (isNoQuestion(q)) {
            Question gq = buildGuessQuestion(state);
            state.setLastQuestion(gq);
            state.incrementStep();
            return NextStepResult.nextQuestion(gq, remaining);
        }

        state.setLastQuestion(q);
        state.incrementStep();
        return NextStepResult.nextQuestion(q, remaining);
    }

    private boolean isNoQuestion(Question q) {
        if (q == null) return true;
        if (q.text() == null) return true;
        String t = q.text().toLowerCase();
        return t.contains("couldn't find a good next question")
                || t.contains("could not find a good next question")
                || q.objectUri() == null
                || q.objectUri().isBlank() && q.kind() == QuestionKind.HAS_VALUE;
    }

    private Question buildGuessQuestion(GameState state) {
        List<String> candidates = speciesRepo.listCandidates(dataset, state.getConstraints(), 2);

        if (candidates.isEmpty()) {
            return new Question(
                    java.util.UUID.randomUUID().toString(),
                    "No candidates left.",
                    QuestionKind.GUESS,
                    "",
                    ""
            );
        }

        String guessUri = candidates.get(0);
        String label = speciesRepo.getEnglishLabel(dataset, guessUri);

        return new Question(
                java.util.UUID.randomUUID().toString(),
                "Is it " + label + "?",
                QuestionKind.GUESS,
                "",
                guessUri
        );
    }

    public record StartGameResult(String sessionId, Question question) {}

    public sealed interface NextStepResult
            permits NextStepResult.NextQuestion, NextStepResult.Guess, NextStepResult.NoCandidates {

        static NextQuestion nextQuestion(Question q, long remaining) { return new NextQuestion(q, remaining); }
        static Guess guess(String speciesUri) { return new Guess(speciesUri); }
        static NoCandidates noCandidates() { return new NoCandidates(); }

        record NextQuestion(Question question, long remainingCandidates) implements NextStepResult {}
        record Guess(String speciesUri) implements NextStepResult {}
        record NoCandidates() implements NextStepResult {}
    }
}
