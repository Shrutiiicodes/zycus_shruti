package com.stockpulse.recommendation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SuggestionDecisionRequest {
    @NotNull
    private Decision decision;

    public enum Decision { ACCEPT, REJECT }
}