package com.projeto.poluicao.services;

import com.projeto.poluicao.dto.AirQualityDataDto;
import com.projeto.poluicao.dto.OpenMeteoResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ConsomeApi {

    private final RestClient openMeteoRestClient;

    public ConsomeApi(RestClient openMeteoRestClient) {
        this.openMeteoRestClient = openMeteoRestClient;
    }

    public AirQualityDataDto buscarQualidadeAr(double latitude, double longitude) {
        LocalDateTime horaRequisicao = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);

        OpenMeteoResponseDTO response = openMeteoRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/air-quality")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("hourly", "pm2_5")
                        .queryParam("timezone", "America/Sao_Paulo")
                        .queryParam("forecast_days", 1)
                        .build())
                .retrieve()
                .body(OpenMeteoResponseDTO.class);

        if (response == null || response.current() == null) {
            throw new RuntimeException("Não foi possível obter os dados da Open-Meteo.");
        }

        int index = response.current().time().indexOf(horaRequisicao);

        Double pm25 = response.current().pm25().get(index);
        String classificacao = classificarNivelPm25(pm25);

        return new AirQualityDataDto(latitude, longitude, pm25, classificacao);
    }

    private String classificarNivelPm25(Double pm25) {
        if (pm25 == null) return "DESCONHECIDO";
        if (pm25 <= 12.0) return "BOA";
        if (pm25 <= 35.4) return "MODERADA";
        if (pm25 <= 55.4) return "INADEQUADA";
        if (pm25 <= 150.4) return "MUITO_RUIM";
        return "CRITICA";
    }
}
