package com.pokenator.rdf;

import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import java.io.InputStream;

public class RdfLoader {

    public void loadOntology(Dataset dataset, String resourcePath) {
        try (InputStream in = getStream(resourcePath)) {
            RDFDataMgr.read(dataset.getDefaultModel(), in, Lang.TURTLE);
        } catch (Exception e) {
            throw new RuntimeException("Erro carregando ontologia", e);
        }
    }

    public void loadDataset(Dataset dataset, String resourcePath) {
        try (InputStream in = getStream(resourcePath)) {
            RDFDataMgr.read(dataset, in, Lang.NQUADS);
        } catch (Exception e) {
            throw new RuntimeException("Erro carregando dataset", e);
        }
    }

    private InputStream getStream(String path) {
        InputStream in = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(path);

        if (in == null)
            throw new IllegalArgumentException("Resource não encontrado: " + path);

        return in;
    }
}
