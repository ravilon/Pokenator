package com.pokenator.akinator.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameState {

    private final String sessionId;
    private final List<Constraint> constraints = new ArrayList<>();
    private final Set<String> askedKeys = new HashSet<>();

    private Question lastQuestion;
    private int step;

    public GameState(String sessionId) {
        this.sessionId = sessionId;
        this.step = 0;
    }

    public String getSessionId() { return sessionId; }
    public List<Constraint> getConstraints() { return constraints; }

    /** Returns true if added; false if it was already present (same predicate+object+answer). */
    public boolean addConstraint(Constraint constraint) {
        if (constraints.contains(constraint)) return false;
        constraints.add(constraint);
        return true;
    }

    /** A key for "asked question" to avoid repetition even before answering. */
    public boolean markAsked(String predicateUri, String objectUri) {
        return askedKeys.add(predicateUri + "|" + objectUri);
    }

    public boolean wasAsked(String predicateUri, String objectUri) {
        return askedKeys.contains(predicateUri + "|" + objectUri);
    }

    public Question getLastQuestion() { return lastQuestion; }
    public void setLastQuestion(Question lastQuestion) { this.lastQuestion = lastQuestion; }

    public int getStep() { return step; }
    public void incrementStep() { step++; }
}
