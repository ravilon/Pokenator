package com.pokenator.akinator.service;

import com.pokenator.akinator.model.Constraint;
import com.pokenator.akinator.model.GameState;
import com.pokenator.akinator.model.Question;
import com.pokenator.akinator.model.QuestionKind;
import com.pokenator.config.PokemonKgConfig;
import com.pokenator.repository.SpeciesRepository;
import org.apache.jena.query.Dataset;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class QuestionSelector {

    private final SpeciesRepository speciesRepo;

    // Direct predicates (Species -> value)
    private static final String HAS_TYPE     = PokemonKgConfig.POKEMON_NS + "hasType";
    private static final String HAS_COLOUR   = PokemonKgConfig.POKEMON_NS + "hasColour";
    private static final String HAS_SHAPE    = PokemonKgConfig.POKEMON_NS + "hasShape";
    private static final String IN_EGG_GROUP = PokemonKgConfig.POKEMON_NS + "inEggGroup";
    private static final String FOUND_IN     = PokemonKgConfig.POKEMON_NS + "foundIn";

    // Inverse predicate (Generation -> Species)
    private static final String FEATURES_SPECIES = PokemonKgConfig.POKEMON_NS + "featuresSpecies";
    private static final String CLASS_GENERATION = PokemonKgConfig.POKEMON_NS + "Generation";

    private static final int LIMIT_VALUES = 40;
    private static final int TOP_K_RANDOM = 4;

    public QuestionSelector(SpeciesRepository speciesRepo) {
        this.speciesRepo = speciesRepo;
    }

    public Question nextQuestion(Dataset dataset, GameState state) {

        long total = speciesRepo.countCandidates(dataset, state.getConstraints());
        if (total <= 1) {
            return new Question(UUID.randomUUID().toString(),
                    "I think I already know which Pokémon it is.",
                    QuestionKind.HAS_VALUE,
                    HAS_TYPE,
                    "");
        }

        // Families (direct)
        List<Family> families = List.of(
                new Family(HAS_TYPE,     "type"),
                new Family(HAS_COLOUR,   "color"),
                new Family(HAS_SHAPE,    "shape"),
                new Family(IN_EGG_GROUP, "egg group"),
                new Family(FOUND_IN,     "habitat")
        );

        List<CandidateQuestion> candidates = new ArrayList<>();

        // 1) Direct families
        for (Family f : families) {
            candidates.addAll(buildCandidatesForFamily(dataset, state, f, total));
        }

        // 2) Generation via inverse predicate: ?gen featuresSpecies ?s . ?gen a Generation .
        candidates.addAll(buildGenerationCandidates(dataset, state, total));

        if (candidates.isEmpty()) {
            return new Question(UUID.randomUUID().toString(),
                    "I couldn't find a good next question.",
                    QuestionKind.HAS_VALUE,
                    HAS_TYPE,
                    "");
        }

        // Rank by best split, then pick randomly among top K to reduce linearity
        List<CandidateQuestion> ranked = candidates.stream()
                .sorted(Comparator.comparingLong(c -> Math.abs((total / 2) - c.yesCount)))
                .toList();

        int k = Math.min(TOP_K_RANDOM, ranked.size());
        CandidateQuestion chosen = ranked.get(ThreadLocalRandom.current().nextInt(k));

        state.markAsked(chosen.predicateUri, chosen.objectUri);

        return new Question(
                UUID.randomUUID().toString(),
                chosen.text,
                QuestionKind.HAS_VALUE,
                chosen.predicateUri,
                chosen.objectUri
        );
    }

    private List<CandidateQuestion> buildCandidatesForFamily(
            Dataset dataset,
            GameState state,
            Family family,
            long totalCandidates
    ) {
        // Do not ask values already constrained for that predicate (YES or NO)
        Set<String> constrainedValues = state.getConstraints().stream()
                .filter(c -> family.predicateUri.equals(c.predicateUri()))
                .map(Constraint::objectUri)
                .collect(Collectors.toSet());

        List<SpeciesRepository.ValueCount> counts =
                speciesRepo.countByPredicateValue(dataset, family.predicateUri, state.getConstraints(), LIMIT_VALUES);

        List<SpeciesRepository.ValueCount> filtered = counts.stream()
                .filter(vc -> !constrainedValues.contains(vc.valueUri()))
                .filter(vc -> !state.wasAsked(family.predicateUri, vc.valueUri()))
                .toList();

        if (filtered.isEmpty()) {
            filtered = counts.stream()
                    .filter(vc -> !constrainedValues.contains(vc.valueUri()))
                    .toList();
        }

        if (filtered.isEmpty()) return List.of();

        List<CandidateQuestion> out = new ArrayList<>();

        for (SpeciesRepository.ValueCount vc : filtered) {
            long yes = vc.count();
            if (yes <= 0 || yes >= totalCandidates) continue;

            String label = normalizeLabel(speciesRepo.getEnglishLabel(dataset, vc.valueUri()));
            String questionText = buildQuestionText(family.questionNoun, label);

            out.add(new CandidateQuestion(family.predicateUri, vc.valueUri(), yes, questionText));
        }

        return out;
    }

    private List<CandidateQuestion> buildGenerationCandidates(Dataset dataset, GameState state, long totalCandidates) {

        // do not repeat generations already constrained/asked
        Set<String> constrained = state.getConstraints().stream()
                .filter(c -> FEATURES_SPECIES.equals(c.predicateUri()))
                .map(Constraint::objectUri)
                .collect(Collectors.toSet());

        List<SpeciesRepository.ValueCount> genCounts =
                speciesRepo.countByInversePredicate(
                        dataset,
                        FEATURES_SPECIES,      // inverse predicate
                        CLASS_GENERATION,      // ?gen a Generation
                        state.getConstraints(),
                        LIMIT_VALUES
                );

        List<CandidateQuestion> out = new ArrayList<>();

        for (SpeciesRepository.ValueCount vc : genCounts) {
            if (constrained.contains(vc.valueUri())) continue;
            if (state.wasAsked(FEATURES_SPECIES, vc.valueUri())) continue;

            long yes = vc.count();
            if (yes <= 0 || yes >= totalCandidates) continue;

            String label = normalizeLabel(speciesRepo.getEnglishLabel(dataset, vc.valueUri()));
            String text = "Is it from " + label + "?";

            out.add(new CandidateQuestion(FEATURES_SPECIES, vc.valueUri(), yes, text));
        }

        return out;
    }

    private String buildQuestionText(String noun, String valueLabel) {
        if ("egg group".equals(noun)) {
            return "Is it in the " + valueLabel + " egg group?";
        }
        if ("habitat".equals(noun)) {
            return "Is it found in " + valueLabel + "?";
        }
        return "Is it " + valueLabel + " " + noun + "?";
    }

    private String normalizeLabel(String label) {
        if (label == null) return "";
        String cleaned = label.trim();

        cleaned = cleaned.replace("PokéType:", "")
                .replace("PokeType:", "")
                .replace("Type:", "")
                .replace("EggGroup:", "")
                .replace("Egg group:", "")
                .replace("Shape:", "")
                .replace("Colour:", "")
                .replace("Color:", "")
                .replace("Habitat:", "")
                .replace("Location:", "")
                .replace("Generation:", "")
                .trim();

        cleaned = cleaned.replace("Pokédex:", "").trim();
        cleaned = cleaned.replaceAll("\\s+", " ");
        return cleaned;
    }

    private record Family(String predicateUri, String questionNoun) {}
    private record CandidateQuestion(String predicateUri, String objectUri, long yesCount, String text) {}
}
