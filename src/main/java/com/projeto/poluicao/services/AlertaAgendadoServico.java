package com.projeto.poluicao.services;

import com.projeto.poluicao.bot.ManausAirBot;
import com.projeto.poluicao.dto.AirQualityDataDto;
import com.projeto.poluicao.model.HistoricoAlerta;
import com.projeto.poluicao.model.Usuario;
import com.projeto.poluicao.repository.HistoricoAlertaRepository;
import com.projeto.poluicao.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.config.FixedRateTask;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlertaAgendadoServico {
    
    private final UsuarioRepository usuarioRepository;
    private final HistoricoAlertaRepository historicoAlertaRepository;
    private final ConsomeApi consome;
    private final ManausAirBot manausAirBot;

    public AlertaAgendadoServico(UsuarioRepository usuarioRepository, HistoricoAlertaRepository historicoAlertaRepository, ConsomeApi consome, ManausAirBot manausAirBot) {
        this.usuarioRepository = usuarioRepository;
        this.historicoAlertaRepository = historicoAlertaRepository;
        this.consome = consome;
        this.manausAirBot = manausAirBot;
    }

    // Executa a cada 1 hora (3600000 ms).
    @Scheduled(fixedRate = 3600000)
    public void verificarEMandarAlertas() {

        List<Usuario> usuarios = usuarioRepository.findAll();

        for (Usuario usuario : usuarios) {
            if (usuario.getLocalizacao() == null) {
                continue; // Pula usuários sem localização cadastrada
            }

            try {
                // No JTS/PostGIS: Y é a Latitude e X é a Longitude
                double lat = usuario.getLocalizacao().getY();
                double lon = usuario.getLocalizacao().getX();

                AirQualityDataDto dadosAr = consome.buscarQualidadeAr(lat, lon);

                // Verifica se a qualidade do ar exige envio de alerta
                if (deveDispararAlerta(dadosAr.status())) {
                    processarEnvioAlerta(usuario, dadosAr);
                }

            } catch (Exception e) {
                throw new RuntimeException("Erro ao verificar qualidade do ar para o usuário ID: " + usuario.getId());
            }
        }
    }

    private boolean deveDispararAlerta(String status) {
        // Dispara alertas para níveis "INADEQUADA", "MUITO_RUIM" ou "CRITICA"
        return List.of("INADEQUADA", "MUITO_RUIM", "CRITICA").contains(status);
    }

    private void processarEnvioAlerta(Usuario usuario, AirQualityDataDto dadosAr) throws TelegramApiException {
        // Evita disparar spam: verifica se já enviamos um alerta idêntico nas últimas 3 horas
        LocalDateTime limiteFrequencia = LocalDateTime.now().minusHours(3);

        if (historicoAlertaRepository
                .existsByUsuarioAndClassificacaoAndDataEnvioAfter(usuario, dadosAr.status(), limiteFrequencia)) return;

        // 1. Monta a mensagem preventiva
        String mensagem = montarMensagemAlerta(usuario.getNome(), dadosAr);

        // 2. Dispara a mensagem via Bot do Telegram
        manausAirBot.enviarMensagem(usuario.getTelegramChatId(), mensagem);

        // 3. Salva no Histórico de Alertas do Banco de Dados
        HistoricoAlerta historico = HistoricoAlerta.builder()
                .usuario(usuario)
                .nivelPm25(dadosAr.pm25())
                .classificacao(dadosAr.status())
                .build();

        historicoAlertaRepository.save(historico);
    }

    private String montarMensagemAlerta(String nome, AirQualityDataDto dadosAr) {
        String orientacao;
        switch (dadosAr.status()) {
            case "INADEQUADA":
                orientacao = "⚠️ *Recomendação:* Evite atividades físicas intensas ao ar livre e feche as janelas em horários de maior densidade de fumaça.";
                break;
            case "MUITO_RUIM":
                orientacao = "🚨 *Recomendação:* Evite exposição prolongada ao ar livre e mantenha janelas fechadas. Use purificadores ou umidificadores se possível.";
                break;
            case "CRITICA":
            default:
                orientacao = "🛑 *ALERTA GRAVE:* Nível crítico de fumaça! Mantenha-se em local fechado e use máscaras PFF2/N95 se precisar sair.";
                break;
        }

        return String.format(
                "⚠️ *ALERTA PREVENTIVO DE QUALIDADE DO AR*\n\n" +
                        "Olá, *%s*! Detectamos um nível elevado de poluição por fumaça na sua região.\n\n" +
                        "• *Nível de PM2.5:* `%.2f µg/m³`\n" +
                        "• *Situação:* *%s*\n\n" +
                        "%s",
                nome, dadosAr.pm25(), dadosAr.status(), orientacao);
    }
    
}
