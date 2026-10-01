# 🌫️ Alerta de Qualidade do Ar - Manaus (Manaus Air Quality Alert)

[![Java](https://img.shields.io/badge/Java-21%2B-orange?style=flat-square&logo=java)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-green?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![PostGIS](https://img.shields.io/badge/PostGIS-3.3-informational?style=flat-square)](https://postgis.net/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?style=flat-square&logo=docker)](https://www.docker.com/)

Um sistema automatizado de monitoramento, análise e notificação preventiva em tempo real desenvolvido para mitigar os impactos causados pela fumaça das queimadas na saúde da população da Região Metropolitana de Manaus, com foco em indivíduos com doenças respiratórias pré-existentes (asma, bronquite, DPOC, etc.).

---

## 📌 Problema e Solução

### O Problema
Durante os períodos de estiagem e queimadas na Região Amazônica, a cidade de Manaus sofre com picos severos de poluição atmosférica por fumaça. A alta concentração de **Material Particulado Fino ($PM_{2.5}$)** no ar agrava significativamente problemas respiratórios, superlotando unidades de saúde e impactando a qualidade de vida da população.

### A Solução
Uma plataforma orientada a dados e eventos que:
1. **Captura Coordenadas Nativas:** Permite que o cidadão cadastre sua localização exata compartilhando o GPS nativo do celular via Telegram.
2. **Monitora em Tempo Real:** Consulta ciclicamente a **Open-Meteo Air Quality API** buscando os índices de $PM_{2.5}$ específicos da coordenada do morador.
3. **Gera Alertas Preventivos:** Dispara notificações imediatas no Telegram com base na gravidade do ar, trazendo recomendações médicas e sanitárias imediatas (ex.: manter janelas fechadas, usar máscaras N95/PFF2, etc.).
4. **Evita Spam (Controle de Frequência):** Implementa algoritmo anti-spam que impede o envio de notificações idênticas e repetitivas em curtos intervalos de tempo.

---

## 🛠️ Tecnologias e Dependências

- **Linguagem & Runtime:** Java 21+ / Spring Boot 4.x
- **Persistência de Dados & GIS:** PostgreSQL 15 + Extensão Espacial **PostGIS**
- **ORM & Mapeamento:** Spring Data JPA + **Hibernate Spatial** + **JTS (Java Topology Suite)**
- **Gerenciamento de Schema:** **Flyway Migration**
- **Cliente HTTP:** Spring `RestClient` (Integração síncrona/reativa)
- **Mensageria & Bot:** `telegrambots-springboot-longpolling-starter` (v7+)
- **Automação & Agendamento:** Spring Scheduling (`@EnableScheduling`, `@Scheduled`)
- **Containerização & Deploy:** Docker + Docker Compose Multi-Stage Build

---

## 🏗️ Arquitetura do Sistema

```text
                               ┌─────────────────────────────┐
                               │  Open-Meteo Air Quality API │
                               └──────────────┬──────────────┘
                                              │
                                        (HTTP / REST)
                                              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       BACKEND (Java 21 / Spring Boot 4.x)                   │
│                                                                             │
│  • ManausAirBot: Recebe localização GPS do usuário e comandos (/ar, /start) │
│  • AlertaSchedulerService: Executa o Cron Job agendado de verificação        │
│  • OpenMeteoService: Consulta PM2.5 e ajusta hora arredondada (truncated)   │
│  • Anti-Spam Logic: Filtra alertas com janela de retenção temporária         │
└──────────────┬──────────────────────────────┬───────────────────────────────┘
               │                              │
               ▼                              ▼
┌──────────────────────────────┐    ┌─────────────────────────┐
│ PostgreSQL + PostGIS         │    │  Plataforma de Alertas  │
│ - tb_usuario (GEOMETRY Point)│    │  (Telegram Bot)         │
│ - tb_historico_alerta        │    └────────────┬────────────┘
│ - Indices GIST Espaciais     │                 │
└──────────────────────────────┘                 ▼
                                     [ Morador em Manaus ]
```

---

## 🗄️ Modelo de Banco de Dados (Flyway + PostGIS)

A aplicação gerencia nativamente pontos geográficos utilizando a extensão **PostGIS** para o cálculo e armazenamento de posições (SRID `4326` WGS84).

### Tabelas Principais

#### `tb_usuario`
Guarda os dados cadastrais do cidadão e seu ponto geográfico.
- `id` (BIGSERIAL / PRIMARY KEY)
- `nome` (VARCHAR)
- `telegram_chat_id` (BIGINT / UNIQUE)
- `localizacao` (GEOMETRY Point, SRID 4326) - *Com Índice Espacial GIST*
- `data_cadastro` (TIMESTAMP)

#### `tb_historico_alerta`
Registra todos os disparos de notificações para auditoria e controle do filtro anti-spam.
- `id` (BIGSERIAL / PRIMARY KEY)
- `usuario_id` (BIGINT / FK -> tb_usuario.id)
- `nivel_pm25` (DOUBLE PRECISION)
- `classificacao` (VARCHAR)
- `data_envio` (TIMESTAMP)

---

## 📊 Tabela de Classificação do Ar ($PM_{2.5}$)

O algoritmo de recomendação utiliza a escala baseada nos diretrizes da **OMS (Organização Mundial da Saúde)** e **CONAMA**:

| Faixa de $PM_{2.5}$ ($\mu g/m^3$) | Classificação | Nível de Risco / Ação Preventiva |
| :--- | :--- | :--- |
| **0.0 a 12.0** | `BOA` | Condições normais. |
| **12.1 a 35.4** | `MODERADA` | Aceitável; pessoas extremamente sensíveis devem ter atenção. |
| **35.5 a 55.4** | `INADEQUADA` | **Alerta Amarelo:** Reduzir exposição prolongada ao ar livre. |
| **55.5 a 150.4** | `MUITO_RUIM` | **Alerta Laranja:** Evitar atividades ao ar livre e manter janelas fechadas. |
| **> 150.4** | `CRITICA` | **Alerta Vermelho:** Manter-se em local fechado e usar máscara PFF2/N95. |

---

## 🚀 Como Executar o Projeto

### Pré-requisitos
- [Docker](https://www.docker.com/) e **Docker Compose** instalados.
- Um Token de Bot gerado no Telegram via [@BotFather](https://t.me/BotFather).

### 1. Clonar o Repositório
```bash
git clone https://github.com/vitorramires468/manaus-air-quality.git
cd manaus-air-quality
```

### 2. Configurar Variáveis de Ambiente
Crie um arquivo `.env` na raiz do projeto (no mesmo diretório do `docker-compose.yml`):

```env
POSTGRES_DB=manaus_air_quality
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgrespassword
TELEGRAM_BOT_TOKEN=SEU_TOKEN_TELEGRAM_AQUI
```

### 3. Subir a Aplicação com Docker Compose
Execute o comando abaixo para compilar a aplicação e inicializar o PostgreSQL + PostGIS:

```bash
docker compose up -d --build
```

### 4. Acompanhar os Logs da Aplicação
```bash
docker logs -f manaus_air_app
```

---

## 📱 Utilizando o Bot do Telegram

1. Inicie a conversa com o seu bot no Telegram executando o comando `/start`.
2. Clique no botão **📍 Compartilhar minha localização** para enviar suas coordenadas GPS.
3. Sempre que o ar atingir um nível de atenção (`INADEQUADA`, `MUITO_RUIM` ou `CRITICA`), o backend disparará uma mensagem preventiva automática.
4. Para consultar o status em tempo real a qualquer momento, envie o comando `/ar` ou `/status`.

---

## ⚙️ Principais Comandos Maven e Úteis

- **Compilar localmente:**
  ```bash
  mvn clean package -DskipTests
  ```
- **Limpar volumes do Docker (Reset de Banco):**
  ```bash
  docker compose down -v
  ```

---

## 📄 Licença

Este projeto é um software de código aberto e impacto social desenvolvido sob a licença [MIT](LICENSE).
