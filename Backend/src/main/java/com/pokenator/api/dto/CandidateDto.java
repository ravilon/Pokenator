// CandidateDto.java
package com.pokenator.api.dto;
/**
 * Representa um candidato a espécie de Pokémon. Contém o URI interno
 * e o rótulo em inglês.
 */
public record CandidateDto(String uri, String label) {}
