package com.pokenator.api.dto;

public record GameStepResponse(
        String kind,                 // "QUESTION" | "GUESS" | "NO_CANDIDATES"
        Long remainingCandidates,    // nullable
        ApiQuestion question,        // nullable
        String guessUri,             // nullable
        String guessLabel            // nullable
) {}
