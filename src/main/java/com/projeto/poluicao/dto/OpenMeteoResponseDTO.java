package com.projeto.poluicao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenMeteoResponseDTO(
        double latitude,
        double longitude,
        @JsonProperty("hourly")
        CurrentUnits current
) {
}
