package com.projeto.poluicao.bot;

import com.projeto.poluicao.dto.AirQualityDataDto;
import com.projeto.poluicao.model.Usuario;
import com.projeto.poluicao.repository.UsuarioRepository;
import com.projeto.poluicao.services.ConsomeApi;
import com.projeto.poluicao.util.GeoUtils;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.location.Location;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.Optional;

@Component
public class ManausAirBot implements SpringLongPollingBot, LongPollingUpdateConsumer {

    private final String botToken;
    private final TelegramClient telegramClient;
    private final UsuarioRepository usuarioRepository;
    private final ConsomeApi consome;

    public ManausAirBot(
            @Value("${telegram.bot.token}") String botToken,
            TelegramClient telegramClient,
            UsuarioRepository usuarioRepository,
            ConsomeApi consome) {
        System.out.println(botToken);
        this.botToken = botToken;
        this.telegramClient = telegramClient;
        this.usuarioRepository = usuarioRepository;
        this.consome = consome;
    }

    @Override
    public String getBotToken() {
        return this.botToken;
    }


    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }


    @Override
    public void consume(List<Update> updates){
        for (Update update : updates) {
            try {
                processarUpdate(update);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
        for (Update update : updates) {
            if(update.hasMessage() && update.getMessage().hasText()) {
                String message_text = update.getMessage().getText();
                long chat_id = update.getMessage().getChatId();

                SendMessage message = SendMessage // Create a message object
                        .builder()
                        .chatId(chat_id)
                        .text(message_text)
                        .build();
                try {
                    telegramClient.execute(message); // Sending our message object to user
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void processarUpdate(Update update) throws TelegramApiException {
        if (!update.hasMessage()) return;

        Long chatId = update.getMessage().getChatId();
        String nomeUsuario = update.getMessage().getFrom().getFirstName();

        // 1. Tratamento caso o usuário envie a Localização via GPS pelo Telegram
        if (update.getMessage().hasLocation()) {
            Location location = update.getMessage().getLocation();
            salvarLocalizacaoUsuario(chatId, nomeUsuario, location.getLatitude(), location.getLongitude());
            return;
        }

        // 2. Tratamento para mensagens de texto
        if (update.getMessage().hasText()) {
            String texto = update.getMessage().getText().trim();

            if (texto.startsWith("/start")) {
                enviarBoasVindasELocalizacao(chatId, nomeUsuario);
            } else if (texto.equalsIgnoreCase("/ar") || texto.equalsIgnoreCase("/status")) {
                consultarEEnviarQualidadeAr(chatId);
            } else {
                enviarMensagem(chatId, "Utilize o comando /ar para verificar a qualidade do ar ou envie sua localização novamente.");
            }
        }
    }

    private void salvarLocalizacaoUsuario(Long chatId, String nome, double lat, double lon) throws TelegramApiException {
        Point localizacao = GeoUtils.criarPonto(lat, lon);

        Optional<Usuario> usuarioOpt = usuarioRepository.findByTelegramChatId(chatId);

        Usuario usuario = usuarioOpt.orElseGet(() -> Usuario.builder()
                .telegramChatId(chatId)
                .nome(nome)
                .build());

        usuario.setLocalizacao(localizacao);
        usuarioRepository.save(usuario);

        String textoSucesso = String.format(
                "📍 *Localização registrada com sucesso!*\n\n" +
                        "Coordenadas: `%.4f, %.4f`\n" +
                        "A partir de agora você receberá alertas automáticos sempre que a poluição (PM2.5) estiver alta na sua região.\n\n" +
                        "Para verificar a qualidade do ar agora, digite /ar", lat, lon);

        enviarMensagem(chatId, textoSucesso);
    }

    private void consultarEEnviarQualidadeAr(Long chatId) throws TelegramApiException {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByTelegramChatId(chatId);

        if (usuarioOpt.isEmpty() || usuarioOpt.get().getLocalizacao() == null) {
            enviarBoasVindasELocalizacao(chatId, "Usuário");
            return;
        }

        Usuario usuario = usuarioOpt.get();
        double lat = usuario.getLocalizacao().getY();
        double lon = usuario.getLocalizacao().getX();

        try {
            AirQualityDataDto arData = consome.buscarQualidadeAr(lat, lon);

            String resposta = String.format(
                    "🌫️ *Qualidade do Ar Atual em Manaus*\n\n" +
                            "• *Nível de PM2.5:* `%.2f µg/m³`\n" +
                            "• *Classificação:* *%s*\n\n" +
                            "_Dados consultados em tempo real na Open-Meteo._",
                    arData.pm25(), arData.status());

            enviarMensagem(chatId, resposta);
        } catch (Exception e) {
            enviarMensagem(chatId, "⚠️ Não foi possível consultar a qualidade do ar no momento. Tente novamente em instantes.");
        }
    }

    private void enviarBoasVindasELocalizacao(Long chatId, String nome) throws TelegramApiException {
        String texto = String.format(
                "Olá, *%s*! 👋\n\n" +
                        "Bem-vindo ao *Alerta de Qualidade do Ar de Manaus*.\n" +
                        "Para começar a receber alertas preventivos contra a fumaça de queimadas na sua área, por favor, *compartilhe sua localização* clicando no botão abaixo.",
                nome);

        // Botão para pedir localização GPS nativa do Telegram
        KeyboardButton botaoLocalizacao = new KeyboardButton("📍 Compartilhar minha localização");
        botaoLocalizacao.setRequestLocation(true);

        KeyboardRow row = new KeyboardRow();
        row.add(botaoLocalizacao);

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .keyboardRow(row)
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(texto)
                .parseMode("Markdown")
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new TelegramApiException("Erro ao enviar mensagem de onboarding");
        }
    }

    public void enviarMensagem(Long chatId, String texto) throws TelegramApiException {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(texto)
                .parseMode("Markdown")
                .build();

        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new TelegramApiException("Erro ao enviar mensagem para chatId ");
        }
    }
}

