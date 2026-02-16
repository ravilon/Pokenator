package com.pokenator.akinator.model;

public record Question(
        String id,
        String text,
        QuestionKind kind,
        String predicateUri,
        String objectUri
) {}
