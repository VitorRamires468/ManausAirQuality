package com.projeto.poluicao.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.*;

import java.awt.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "telegram_chat_id", nullable = false, unique = true)
    private Long telegramChatId;

    private String bairro;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point localizacao;

    @Column(name = "data_cadastro")
    private LocalDateTime dataCadastro;

    @PrePersist
    public void prePersist() {
        this.dataCadastro = LocalDateTime.now();
    }

}
