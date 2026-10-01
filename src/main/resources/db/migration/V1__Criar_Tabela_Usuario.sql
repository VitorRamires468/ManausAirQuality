CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE tb_usuario (
                            id BIGSERIAL PRIMARY KEY,
                            nome VARCHAR(255) NOT NULL,
                            telegram_chat_id BIGINT NOT NULL UNIQUE,
                            bairro VARCHAR(255),
                            localizacao GEOMETRY(Point, 4326),
                            data_cadastro TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tb_usuario_localizacao ON tb_usuario USING GIST (localizacao);