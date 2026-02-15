package com.pokenator.sparql;

import org.apache.jena.query.*;

import java.util.ArrayList;
import java.util.List;

public class JenaSparqlClient implements SparqlClient {

    private final long timeoutMillis;

    public JenaSparqlClient(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public JenaSparqlClient() {
        this.timeoutMillis = 20000; // 20 segundos
    }

    @Override
    public List<QuerySolution> select(Dataset dataset, String sparql) {

        Query query = QueryFactory.create(sparql);

        QueryExecutionBuilder builder =
                QueryExecution.dataset(dataset)
                        .query(query)
                        .timeout(timeoutMillis); // <-- long

        try (QueryExecution qexec = builder.build()) {

            ResultSet rs = qexec.execSelect();
            List<QuerySolution> out = new ArrayList<>();

            while (rs.hasNext()) {
                out.add(rs.next());
            }

            return out;
        }
    }

    @Override
    public boolean ask(Dataset dataset, String sparql) {

        Query query = QueryFactory.create(sparql);

        QueryExecutionBuilder builder =
                QueryExecution.dataset(dataset)
                        .query(query)
                        .timeout(timeoutMillis); // <-- long

        try (QueryExecution qexec = builder.build()) {
            return qexec.execAsk();
        }
    }
}
