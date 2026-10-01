package com.projeto.poluicao.repository;

import com.projeto.poluicao.model.HistoricoAlerta;
import com.projeto.poluicao.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface HistoricoAlertaRepository extends JpaRepository<HistoricoAlerta, Long> {
    boolean existsByUsuarioAndClassificacaoAndDataEnvioAfter(
            Usuario usuario,
            String classificacao,
            LocalDateTime dataEnvio
    );
}
