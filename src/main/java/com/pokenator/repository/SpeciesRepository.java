package com.pokenator.repository;

import com.pokenator.akinator.model.Answer;
import com.pokenator.akinator.model.Constraint;
import com.pokenator.config.PokemonKgConfig;
import com.pokenator.sparql.Prefixes;
import com.pokenator.sparql.SparqlClient;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SpeciesRepository {

    private static final String FEATURES_SPECIES = PokemonKgConfig.POKEMON_NS + "featuresSpecies";

    private final SparqlClient sparql;

    public SpeciesRepository(SparqlClient sparql) {
        this.sparql = sparql;
    }

    // =========================================================
    // BASIC QUERIES
    // =========================================================

    public long countSpecies(Dataset dataset) {

        StringBuilder sb = new StringBuilder();
        sb.append(Prefixes.prefix("pokemon", PokemonKgConfig.POKEMON_NS));
        sb.append("SELECT (COUNT(DISTINCT ?s) AS ?total)\n");
        sb.append("WHERE { GRAPH ?g { ?s a pokemon:Species . } }");

        QuerySolution row = sparql.selectOne(dataset, sb.toString());
        if (row == null) return 0;

        return row.getLiteral("total").getLong();
    }

    public String discoverPredicateToGeneration(Dataset dataset) {

        StringBuilder sb = new StringBuilder();
        sb.append(Prefixes.prefix("pokemon", PokemonKgConfig.POKEMON_NS));
        sb.append("SELECT ?p (COUNT(DISTINCT ?s) AS ?n)\n");
        sb.append("WHERE {\n");
        sb.append("  GRAPH ?g {\n");
        sb.append("    ?s a pokemon:Species .\n");
        sb.append("    ?s ?p ?gen .\n");
        sb.append("    ?gen a pokemon:Generation .\n");
        sb.append("  }\n");
        sb.append("}\n");
        sb.append("GROUP BY ?p\n");
        sb.append("ORDER BY DESC(?n)\n");
        sb.append("LIMIT 1\n");

        QuerySolution row = sparql.selectOne(dataset, sb.toString());
        if (row == null || !row.contains("p")) return null;
        return row.getResource("p").getURI();
    }

    public List<ValueCount> countByInversePredicate(
            Dataset dataset,
            String inversePredicateUri,   // e.g. pokemon:featuresSpecies
            String subjectClassUri,       // e.g. pokemon:Generation
            List<Constraint> constraints,
            int limit
    ) {

        StringBuilder sb = new StringBuilder();
        sb.append(Prefixes.prefix("pokemon", PokemonKgConfig.POKEMON_NS));
        sb.append("SELECT ?x (COUNT(DISTINCT ?s) AS ?n)\n");
        sb.append("WHERE {\n");
        sb.append("  GRAPH ?g {\n");
        sb.append("    ?s a pokemon:Species .\n");

        appendConstraints(sb, constraints);

        sb.append("    ?x <").append(inversePredicateUri).append("> ?s .\n");
        sb.append("    ?x a <").append(subjectClassUri).append("> .\n");
        sb.append("  }\n");
        sb.append("}\n");
        sb.append("GROUP BY ?x\n");
        sb.append("ORDER BY DESC(?n)\n");
        sb.append("LIMIT ").append(limit);

        List<QuerySolution> rows = sparql.select(dataset, sb.toString());

        List<ValueCount> out = new ArrayList<>();
        for (QuerySolution r : rows) {
            String valueUri = r.getResource("x").getURI();
            long n = r.getLiteral("n").getLong();
            out.add(new ValueCount(valueUri, n));
        }
        return out;
    }


    public List<String> listSpecies(Dataset dataset, int limit) {

        StringBuilder sb = new StringBuilder();
        sb.append(Prefixes.prefix("pokemon", PokemonKgConfig.POKEMON_NS));
        sb.append("SELECT DISTINCT ?s\n");
        sb.append("WHERE { GRAPH ?g { ?s a pokemon:Species . } }\n");
        sb.append("LIMIT ").append(limit);

        List<QuerySolution> rows = sparql.select(dataset, sb.toString());

        List<String> out = new ArrayList<>();
        for (QuerySolution r : rows) {
            RDFNode node = r.get("s");
            if (node != null && node.isResource()) {
                out.add(node.asResource().getURI());
            }
        }
        return out;
    }

    // =========================================================
    // DIAGNOSTIC
    // =========================================================

    public List<PredicateCount> topPredicatesForSpecies(Dataset dataset, int limit) {

        StringBuilder sb = new StringBuilder();
        sb.append(Prefixes.prefix("pokemon", PokemonKgConfig.POKEMON_NS));
        sb.append("SELECT ?p (COUNT(*) AS ?n)\n");
        sb.append("WHERE {\n");
        sb.append("  GRAPH ?g {\n");
        sb.append("    ?s a pokemon:Species .\n");
        sb.append("    ?s ?p ?o .\n");
        sb.append("  }\n");
        sb.append("}\n");
        sb.append("GROUP BY ?p\n");
        sb.append("ORDER BY DESC(?n)\n");
        sb.append("LIMIT ").append(limit);

        List<QuerySolution> rows = sparql.select(dataset, sb.toString());

        List<PredicateCount> out = new ArrayList<>();
        for (QuerySolution r : rows) {
            String p = r.getResource("p").getURI();
            long n = r.getLiteral("n").getLong();
            out.add(new PredicateCount(p, n));
        }
        return out;
    }

    // =========================================================
    // AKINATOR CORE QUERIES
    // =========================================================

    public long countCandidates(Dataset dataset, List<Constraint> constraints) {

        StringBuilder sb = new StringBuilder();
        sb.append(Prefixes.prefix("pokemon", PokemonKgConfig.POKEMON_NS));
        sb.append("SELECT (COUNT(DISTINCT ?s) AS ?total)\n");
        sb.append("WHERE {\n");
        sb.append("  GRAPH ?g {\n");
        sb.append("    ?s a pokemon:Species .\n");

        appendConstraints(sb, constraints);

        sb.append("  }\n");
        sb.append("}");

        QuerySolution row = sparql.selectOne(dataset, sb.toString());
        if (row == null) return 0;

        return row.getLiteral("total").getLong();
    }

    public List<ValueCount> countByPredicateValue(
            Dataset dataset,
            String predicateUri,
            List<Constraint> constraints,
            int limit
    ) {

        StringBuilder sb = new StringBuilder();
        sb.append(Prefixes.prefix("pokemon", PokemonKgConfig.POKEMON_NS));
        sb.append("SELECT ?o (COUNT(DISTINCT ?s) AS ?n)\n");
        sb.append("WHERE {\n");
        sb.append("  GRAPH ?g {\n");
        sb.append("    ?s a pokemon:Species .\n");

        appendConstraints(sb, constraints);

        sb.append("    ?s <").append(predicateUri).append("> ?o .\n");
        sb.append("  }\n");
        sb.append("}\n");
        sb.append("GROUP BY ?o\n");
        sb.append("ORDER BY DESC(?n)\n");
        sb.append("LIMIT ").append(limit);

        List<QuerySolution> rows = sparql.select(dataset, sb.toString());

        List<ValueCount> out = new ArrayList<>();
        for (QuerySolution r : rows) {
            String valueUri = r.getResource("o").getURI();
            long n = r.getLiteral("n").getLong();
            out.add(new ValueCount(valueUri, n));
        }
        return out;
    }

    public Optional<String> getOneCandidate(Dataset dataset, List<Constraint> constraints) {

        StringBuilder sb = new StringBuilder();
        sb.append(Prefixes.prefix("pokemon", PokemonKgConfig.POKEMON_NS));
        sb.append("SELECT DISTINCT ?s\n");
        sb.append("WHERE {\n");
        sb.append("  GRAPH ?g {\n");
        sb.append("    ?s a pokemon:Species .\n");

        appendConstraints(sb, constraints);

        sb.append("  }\n");
        sb.append("}\n");
        sb.append("LIMIT 1");

        List<QuerySolution> rows = sparql.select(dataset, sb.toString());

        if (rows.isEmpty()) return Optional.empty();

        RDFNode node = rows.get(0).get("s");
        if (node != null && node.isResource()) {
            return Optional.of(node.asResource().getURI());
        }

        return Optional.empty();
    }

    // =========================================================
    // LABEL SUPPORT
    // =========================================================

    public String getEnglishLabel(Dataset dataset, String uri) {

        StringBuilder sb = new StringBuilder();
        sb.append(Prefixes.prefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#"));
        sb.append("SELECT ?label WHERE {\n");
        sb.append("  GRAPH ?g {\n");
        sb.append("    <").append(uri).append("> rdfs:label ?label .\n");
        sb.append("    FILTER(lang(?label) = \"en\" || lang(?label) = \"\")\n");
        sb.append("  }\n");
        sb.append("}\n");
        sb.append("LIMIT 1");

        QuerySolution row = sparql.selectOne(dataset, sb.toString());

        if (row != null && row.contains("label")) {
            return row.getLiteral("label").getString();
        }

        return localName(uri);
    }

    public List<String> listCandidates(Dataset dataset, List<Constraint> constraints, int limit) {

        StringBuilder sb = new StringBuilder();
        sb.append(Prefixes.prefix("pokemon", PokemonKgConfig.POKEMON_NS));
        sb.append("SELECT DISTINCT ?s\n");
        sb.append("WHERE {\n");
        sb.append("  GRAPH ?g {\n");
        sb.append("    ?s a pokemon:Species .\n");
        appendConstraints(sb, constraints);
        sb.append("  }\n");
        sb.append("}\n");
        sb.append("LIMIT ").append(limit);

        List<QuerySolution> rows = sparql.select(dataset, sb.toString());

        List<String> out = new ArrayList<>();
        for (QuerySolution r : rows) {
            RDFNode node = r.get("s");
            if (node != null && node.isResource()) {
                out.add(node.asResource().getURI());
            }
        }
        return out;
    }


    // =========================================================
    // INTERNAL HELPERS
    // =========================================================

    private void appendConstraints(StringBuilder sb, List<Constraint> constraints) {

        for (Constraint c : constraints) {

            boolean isInverseGeneration = FEATURES_SPECIES.equals(c.predicateUri());

            if (c.answer() == Answer.YES) {
                if (isInverseGeneration) {
                    // <Generation> featuresSpecies ?s
                    sb.append("    <").append(c.objectUri()).append("> <")
                            .append(FEATURES_SPECIES)
                            .append("> ?s .\n");
                } else {
                    sb.append("    ?s <").append(c.predicateUri()).append("> <")
                            .append(c.objectUri()).append("> .\n");
                }
            }

            if (c.answer() == Answer.NO) {
                if (isInverseGeneration) {
                    sb.append("    FILTER NOT EXISTS { <").append(c.objectUri()).append("> <")
                            .append(FEATURES_SPECIES)
                            .append("> ?s }\n");
                } else {
                    sb.append("    FILTER NOT EXISTS { ?s <").append(c.predicateUri()).append("> <")
                            .append(c.objectUri()).append("> }\n");
                }
            }
        }
    }


    private String localName(String uri) {
        int hash = uri.lastIndexOf('#');
        int slash = uri.lastIndexOf('/');
        int idx = Math.max(hash, slash);
        if (idx >= 0 && idx + 1 < uri.length()) {
            return uri.substring(idx + 1);
        }
        return uri;
    }

    // =========================================================
    // RECORDS
    // =========================================================

    public record PredicateCount(String predicateUri, long count) {}
    public record ValueCount(String valueUri, long count) {}
}
