// CandidateListResponse.java
package com.pokenator.api.dto;
import java.util.List;
/** Envolve a lista de candidatos retornada pelo endpoint de candidatos. */
public record CandidateListResponse(List<CandidateDto> candidates) {}
