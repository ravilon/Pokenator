package com.pokenator.repository;

import com.pokenator.config.PokemonKgConfig;
import com.pokenator.sparql.Prefixes;
import com.pokenator.sparql.SparqlClient;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;

import java.util.ArrayList;
import java.util.List;

public class SpeciesRepository {

    private final SparqlClient sparql;

    public SpeciesRepository(SparqlClient sparql) {
        this.sparql = sparql;
    }

    public long countSpecies(Dataset dataset) {

        StringBuilder sb = new StringBuilder();
        sb.append(Prefixes.prefix("pokemon", PokemonKgConfig.POKEMON_NS));
        sb.append("SELECT (COUNT(DISTINCT ?s) AS ?total)\n");
        sb.append("WHERE { GRAPH ?g { ?s a pokemon:Species . } }");

        QuerySolution row = sparql.selectOne(dataset, sb.toString());
        if (row == null) return 0;

        return row.getLiteral("total").getLong();
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

    /**
     * Diagnóstico: quais predicados mais aparecem em Species (bom pra definir "perguntas candidatas").
     */
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

    public record PredicateCount(String predicateUri, long count) {}
}
