# API de Tarefas — Spring Boot

[![Testes](https://github.com/EsterNevess/api-tarefas-spring/actions/workflows/ci.yml/badge.svg)](https://github.com/EsterNevess/api-tarefas-spring/actions/workflows/ci.yml)
[![Relatório Allure](https://img.shields.io/badge/Relat%C3%B3rio-Allure-FF5722)](https://esternevess.github.io/api-tarefas-spring/)
[![Cobertura](https://img.shields.io/badge/Cobertura-JaCoCo-2E7D32)](https://esternevess.github.io/api-tarefas-spring/cobertura/)

API REST para gestão de tarefas em quadro, com fluxo de status, perfis de acesso e cálculo de
situação do prazo em dias úteis. Back-end em **Java 21 + Spring Boot 4**, banco **PostgreSQL**,
execução via **Docker Compose** e testes em três camadas (unitários, integração e API externa).

📊 **[Relatório Allure da última execução](https://esternevess.github.io/api-tarefas-spring/)** ·
**[Cobertura de código](https://esternevess.github.io/api-tarefas-spring/cobertura/)**

O domínio é inspirado no projeto Tarefiz (gestão de tarefas por perfil e status), reescrito aqui do
zero como projeto público e independente.

## Regras de negócio

**Fluxo de status** — só os movimentos abaixo são aceitos; qualquer outro retorna `409`.

```
PLANEJADA → A_FAZER → EM_ANDAMENTO ⇄ PAUSADA
                           ↓
                       CONCLUIDA   (estado final)
```

**Situação do prazo** — calculada na leitura, não gravada:

| Situação | Regra |
|---|---|
| `ENCERRADA` | tarefa concluída |
| `ATRASADA` | prazo anterior a hoje |
| `URGENTE` | vence hoje ou em até 2 dias úteis |
| `NO_PRAZO` | demais casos |

Dias úteis ignoram sábados, domingos e feriados nacionais (incluindo a Sexta-feira Santa, obtida
pelo algoritmo da Páscoa, e o Dia da Consciência Negra a partir de 2024).
A contagem é uma demonstração: não considera Carnaval nem feriados estaduais e municipais, e não
substitui contagem de prazo oficial.

**Perfis**

| Ação | ADMIN | SUPERVISOR | COLABORADOR |
|---|:-:|:-:|:-:|
| Listar / consultar | todas | todas | só as próprias |
| Criar e editar | ✔ | ✔ | ✖ |
| Mudar status | ✔ | ✔ | só as próprias |
| Excluir | ✔ | ✖ | ✖ |

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| GET | `/tarefas?status=&responsavel=` | lista, ordenada por prazo |
| GET | `/tarefas/{id}` | consulta uma tarefa |
| POST | `/tarefas` | cria (nasce `PLANEJADA`) |
| PUT | `/tarefas/{id}` | edita os dados |
| PATCH | `/tarefas/{id}/status` | move no fluxo: `{"status": "A_FAZER"}` |
| DELETE | `/tarefas/{id}` | exclui |
| GET | `/actuator/health` | saúde da aplicação (público) |

Erros seguem o formato `ProblemDetail` (RFC 9457): `400` validação, `401` sem login, `403` sem
permissão, `404` inexistente, `409` transição de status inválida.

## Como rodar

**Com Docker (PostgreSQL):**

```bash
docker compose up --build
```

**Sem instalar banco (H2 em memória, mesmas migrações):**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

Usuários de demonstração (HTTP Basic), todos com a senha `senha123`:
`admin` · `supervisor` · `ana` (colaboradora) · `bruno` (colaborador).
São fixos e existem **apenas para demonstração**; em um sistema real viriam do banco, com login
por token.

```bash
curl -u admin:senha123 -X POST localhost:8080/tarefas -H "Content-Type: application/json" \
  -d '{"titulo":"Preparar apresentação","prioridade":"ALTA","responsavel":"ana","prazo":"2026-12-10"}'
```

## Testes

| Camada | O que cobre | Como rodar |
|---|---|---|
| Unitários (JUnit 5 + AssertJ) | Páscoa e feriados, dias úteis, fluxo de status, situação do prazo | `./mvnw test` |
| Integração (MockMvc + H2) | HTTP, segurança, validação, perfis, banco com Flyway, relógio fixo | `./mvnw test` |
| API externa (pytest + requests) | a API real subindo em Docker com PostgreSQL | `pip install -r testes-api/requirements.txt` e `pytest testes-api` |
| Desempenho (k6) | smoke de 5 usuários simultâneos, limites de erro e latência | `k6 run k6/smoke.js` |

`./mvnw verify` roda ainda a verificação de **cobertura mínima de 85% das linhas** (JaCoCo); abaixo
disso o build falha. Os testes de data usam um `Clock` injetado e fixo, para não dependerem do dia
em que rodam.

A cada push, o GitHub Actions executa as três camadas e publica o relatório Allure e a cobertura
no GitHub Pages.

## Estrutura

```
src/main/java/.../tarefas/
  dominio/       regras puras: fluxo de status, feriados, dias úteis, situação, entidade
  servico/       casos de uso e permissões por perfil
  web/           controller, DTOs e tratamento de erros
  seguranca/     HTTP Basic, perfis e usuários de demonstração
  repositorio/   Spring Data JPA
src/main/resources/db/migration/   migrações Flyway (esquema do PostgreSQL)
src/test/        testes unitários e de integração
testes-api/      pytest contra a API em execução
k6/              teste de desempenho
```

## Limitações conhecidas

- Usuários e senhas fixos (demonstração).
- Sem paginação na listagem.
- Calendário de feriados nacionais apenas.
- Sem login por token (JWT/OAuth2).
