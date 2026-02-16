package com.pokenator.sparql;

import org.apache.jena.query.*;
import org.apache.jena.system.Txn;

import java.util.ArrayList;
import java.util.List;

public class JenaSparqlClient implements SparqlClient {

    private final long timeoutMillis;

    public JenaSparqlClient(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public JenaSparqlClient() {
        this(20_000L);
    }

    @Override
    public List<QuerySolution> select(Dataset dataset, String sparql) {
        if (dataset == null) throw new IllegalArgumentException("dataset is null");
        if (sparql == null || sparql.isBlank()) throw new IllegalArgumentException("sparql is blank");

        return Txn.calculateRead(dataset, () -> {
            Query query = QueryFactory.create(sparql);

            QueryExecutionBuilder builder = QueryExecution
                    .dataset(dataset)
                    .query(query)
                    .timeout(timeoutMillis); // long

            try (QueryExecution qexec = builder.build()) {
                ResultSet rs = qexec.execSelect();
                List<QuerySolution> out = new ArrayList<>();

                while (rs.hasNext()) {
                    out.add(rs.nextSolution());
                }

                return out;
            }
        });
    }

    @Override
    public boolean ask(Dataset dataset, String sparql) {
        if (dataset == null) throw new IllegalArgumentException("dataset is null");
        if (sparql == null || sparql.isBlank()) throw new IllegalArgumentException("sparql is blank");

        return Txn.calculateRead(dataset, () -> {
            Query query = QueryFactory.create(sparql);

            QueryExecutionBuilder builder = QueryExecution
                    .dataset(dataset)
                    .query(query)
                    .timeout(timeoutMillis); // long

            try (QueryExecution qexec = builder.build()) {
                return qexec.execAsk();
            }
        });
    }
}
