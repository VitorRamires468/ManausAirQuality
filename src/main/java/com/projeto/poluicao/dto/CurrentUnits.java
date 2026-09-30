package com.projeto.poluicao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record CurrentUnits(
        List<LocalDateTime> time,
        @JsonProperty("pm2_5")
        List<Double> pm25
        ) {

}
