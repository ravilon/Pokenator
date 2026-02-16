package com.pokenator.api.dto;

public record ApiQuestion(
        String text,
        String kind,
        String predicateUri,
        String objectUri
) {}
