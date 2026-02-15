package com.pokenator.sparql;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QuerySolution;
import java.util.List;

public interface SparqlClient {

    /**
     * Executa SELECT e retorna todas as linhas já materializadas (desacoplado do lifecycle do QueryExecution).
     */
    List<QuerySolution> select(Dataset dataset, String sparql);

    /**
     * Executa ASK.
     */
    boolean ask(Dataset dataset, String sparql);

    /**
     * Utilitário: executa SELECT esperando 1 linha, 1 coluna literal/uri.
     */
    default QuerySolution selectOne(Dataset dataset, String sparql) {
        List<QuerySolution> rows = select(dataset, sparql);
        return rows.isEmpty() ? null : rows.getFirst();
    }
}
