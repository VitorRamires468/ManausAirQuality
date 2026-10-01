CREATE TABLE tb_historico_alerta (
                                     id BIGSERIAL PRIMARY KEY,
                                     usuario_id BIGINT NOT NULL,
                                     nivel_pm25 DOUBLE PRECISION NOT NULL,
                                     classificacao VARCHAR(50) NOT NULL,
                                     data_envio TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     CONSTRAINT fk_historico_alerta_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario (id) ON DELETE CASCADE
);

CREATE INDEX idx_historico_alerta_usuario_data ON tb_historico_alerta (usuario_id, classificacao, data_envio);