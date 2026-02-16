package com.pokenator.sparql;

public final class Prefixes {

    private Prefixes() {}

    public static String prefix(String name, String iri) {
        return "PREFIX " + name + ": <" + iri + ">\n";
    }
}
