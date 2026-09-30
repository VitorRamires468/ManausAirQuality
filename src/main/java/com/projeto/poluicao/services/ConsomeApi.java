package com.projeto.poluicao.services;

import com.projeto.poluicao.dto.AirQualityDataDto;
import com.projeto.poluicao.dto.OpenMeteoResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ConsomeApi {

    private final RestClient openMeteoRestClient;

    public ConsomeApi(RestClient openMeteoRestClient) {
        this.openMeteoRestClient = openMeteoRestClient;
    }

    public AirQualityDataDto buscarQualidadeAr(double latitude, double longitude) {

        OpenMeteoResponseDTO response = openMeteoRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/air-quality")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("hourly", "pm2_5")
                        .build())
                .retrieve()
                .body(OpenMeteoResponseDTO.class);

        if (response == null || response.current() == null) {
            throw new RuntimeException("Não foi possível obter os dados da Open-Meteo.");
        }

        Double pm25 = response.current().pm25();
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
