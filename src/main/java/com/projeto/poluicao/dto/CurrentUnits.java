package com.projeto.poluicao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CurrentUnits(
        String time,
        int interval,
        @JsonProperty("pm2_5")
        double pm25
        ) {

}
