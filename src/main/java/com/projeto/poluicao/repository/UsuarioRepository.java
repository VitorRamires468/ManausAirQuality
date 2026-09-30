package com.projeto.poluicao.repository;

import com.projeto.poluicao.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByTelegramChatId(Long telegramChatId);
}
