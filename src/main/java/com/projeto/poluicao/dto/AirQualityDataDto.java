package com.projeto.poluicao.dto;

public record AirQualityDataDto(
        double latitude,
        double longitude,
        Double pm25,
        String status
) {
}
