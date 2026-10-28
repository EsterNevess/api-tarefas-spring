package br.com.esternevess.tarefas.web;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa a API de ponta a ponta (HTTP, segurança, validação, banco H2 com as mesmas
 * migrações do PostgreSQL). O relógio é fixo na segunda-feira 26/10/2026, às 12h.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class TarefaApiTest {

    private static final String SENHA = "senha123";

    @TestConfiguration
    static class RelogioFixo {
        @Bean
        @Primary
        Clock relogioFixo() {
            return Clock.fixed(Instant.parse("2026-10-26T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        }
    }

    @Autowired
    MockMvc mvc;

    // ---------- autenticação e permissões ----------

    @Test
    void semCredenciaisRetorna401() throws Exception {
        mvc.perform(get("/tarefas")).andExpect(status().isUnauthorized());
    }

    @Test
    void senhaErradaRetorna401() throws Exception {
        mvc.perform(get("/tarefas").with(httpBasic("admin", "errada"))).andExpect(status().isUnauthorized());
    }

    @Test
    void healthEPublico() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void colaboradorNaoCriaTarefa() throws Exception {
        enviarCriacao("ana", corpo("Relatório", "ana", "2026-10-30")).andExpect(status().isForbidden());
    }

    @Test
    void supervisorNaoExcluiTarefa() throws Exception {
        long id = criar("Relatório", "ana", "2026-10-30");
        mvc.perform(delete("/tarefas/" + id).with(httpBasic("supervisor", SENHA)))
                .andExpect(status().isForbidden());
    }

    // ---------- criação e leitura ----------

    @Test
    void adminCriaTarefaPlanejadaComLocalizacao() throws Exception {
        enviarCriacao("admin", corpo("Preparar apresentação", "ana", "2026-10-30"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/tarefas/\\d+")))
                .andExpect(jsonPath("$.status").value("PLANEJADA"))
                .andExpect(jsonPath("$.situacao").value("NO_PRAZO"))
                .andExpect(jsonPath("$.proximosStatus").value(containsInAnyOrder("A_FAZER")))
                .andExpect(jsonPath("$.criadaEm").value("2026-10-26T12:00:00"))
                .andExpect(jsonPath("$.concluidaEm").value(nullValue()));
    }

    @Test
    void prazoProximoViraUrgente() throws Exception {
        enviarCriacao("admin", corpo("Corrigir bug", "ana", "2026-10-28"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.situacao").value("URGENTE"));
    }

    @Test
    void rejeitaCamposInvalidosComDetalhe() throws Exception {
        enviarCriacao("admin", """
                {"titulo": "", "prioridade": "ALTA", "responsavel": "ana", "prazo": "2026-10-30"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.titulo").value(notNullValue()));
    }

    @Test
    void rejeitaPrazoNoPassado() throws Exception {
        enviarCriacao("admin", corpo("Tarefa antiga", "ana", "2026-10-25"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O prazo não pode estar no passado"));
    }

    @Test
    void rejeitaPrioridadeDesconhecida() throws Exception {
        enviarCriacao("admin", """
                {"titulo": "X", "prioridade": "ENORME", "responsavel": "ana", "prazo": "2026-10-30"}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    void buscarInexistenteRetorna404() throws Exception {
        mvc.perform(get("/tarefas/999").with(httpBasic("admin", SENHA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Tarefa 999 não encontrada"));
    }

    @Test
    void atualizaOsDadosDeUmaTarefa() throws Exception {
        long id = criar("Rascunho", "ana", "2026-10-30");
        mvc.perform(put("/tarefas/" + id)
                        .with(httpBasic("supervisor", SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Versão final", "bruno", "2026-11-05")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Versão final"))
                .andExpect(jsonPath("$.responsavel").value("bruno"))
                .andExpect(jsonPath("$.prazo").value("2026-11-05"));
    }

    // ---------- fluxo de status ----------

    @Test
    void percorreOFluxoCompletoAteConcluir() throws Exception {
        long id = criar("Entregar módulo", "ana", "2026-10-30");

        mudarStatus("ana", id, "A_FAZER").andExpect(status().isOk());
        mudarStatus("ana", id, "EM_ANDAMENTO").andExpect(status().isOk());
        mudarStatus("ana", id, "CONCLUIDA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.situacao").value("ENCERRADA"))
                .andExpect(jsonPath("$.concluidaEm").value("2026-10-26T12:00:00"))
                .andExpect(jsonPath("$.proximosStatus", hasSize(0)));
    }

    @Test
    void transicaoForaDoFluxoRetorna409() throws Exception {
        long id = criar("Entregar módulo", "ana", "2026-10-30");

        mudarStatus("ana", id, "CONCLUIDA")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Não é possível mover a tarefa de PLANEJADA para CONCLUIDA"));
    }

    @Test
    void statusDesconhecidoRetorna400() throws Exception {
        long id = criar("Entregar módulo", "ana", "2026-10-30");
        mvc.perform(patch("/tarefas/" + id + "/status")
                        .with(httpBasic("ana", SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"VOANDO\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- visibilidade por perfil ----------

    @Test
    void colaboradorSoVeAsPropriasTarefas() throws Exception {
        criar("Da Ana", "ana", "2026-10-30");
        criar("Do Bruno", "bruno", "2026-10-30");

        mvc.perform(get("/tarefas").with(httpBasic("ana", SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].titulo").value("Da Ana"));
    }

    @Test
    void colaboradorNaoContornaOFiltroPedindoOutroResponsavel() throws Exception {
        criar("Da Ana", "ana", "2026-10-30");
        criar("Do Bruno", "bruno", "2026-10-30");

        mvc.perform(get("/tarefas?responsavel=bruno").with(httpBasic("ana", SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].responsavel").value("ana"));
    }

    @Test
    void colaboradorNaoMexeNaTarefaDeOutro() throws Exception {
        long idDoBruno = criar("Do Bruno", "bruno", "2026-10-30");

        mudarStatus("ana", idDoBruno, "A_FAZER").andExpect(status().isForbidden());
        mvc.perform(get("/tarefas/" + idDoBruno).with(httpBasic("ana", SENHA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void supervisorVeTudoEFiltraPorStatusEResponsavel() throws Exception {
        long idAna = criar("Da Ana", "ana", "2026-10-30");
        criar("Do Bruno", "bruno", "2026-10-30");
        mudarStatus("ana", idAna, "A_FAZER").andExpect(status().isOk());

        mvc.perform(get("/tarefas").with(httpBasic("supervisor", SENHA)))
                .andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/tarefas?status=A_FAZER").with(httpBasic("supervisor", SENHA)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].titulo").value("Da Ana"));
        mvc.perform(get("/tarefas?responsavel=bruno").with(httpBasic("supervisor", SENHA)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].titulo").value("Do Bruno"));
    }

    @Test
    void listaVemOrdenadaPeloPrazo() throws Exception {
        criar("Depois", "ana", "2026-11-10");
        criar("Primeiro", "ana", "2026-10-28");

        mvc.perform(get("/tarefas").with(httpBasic("admin", SENHA)))
                .andExpect(jsonPath("$[0].titulo").value("Primeiro"))
                .andExpect(jsonPath("$[1].titulo").value("Depois"));
    }

    @Test
    void filtroDeStatusInvalidoRetorna400() throws Exception {
        mvc.perform(get("/tarefas?status=VOANDO").with(httpBasic("admin", SENHA)))
                .andExpect(status().isBadRequest());
    }

    // ---------- exclusão ----------

    @Test
    void adminExcluiETarefaSomeDaListagem() throws Exception {
        long id = criar("Descartável", "ana", "2026-10-30");

        mvc.perform(delete("/tarefas/" + id).with(httpBasic("admin", SENHA)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/tarefas/" + id).with(httpBasic("admin", SENHA)))
                .andExpect(status().isNotFound());
    }

    // ---------- apoio ----------

    private long criar(String titulo, String responsavel, String prazo) throws Exception {
        String resposta = enviarCriacao("admin", corpo(titulo, responsavel, prazo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }

    private ResultActions enviarCriacao(String usuario, String json) throws Exception {
        return mvc.perform(post("/tarefas")
                .with(httpBasic(usuario, SENHA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions mudarStatus(String usuario, long id, String novo) throws Exception {
        return mvc.perform(patch("/tarefas/" + id + "/status")
                .with(httpBasic(usuario, SENHA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"" + novo + "\"}"));
    }

    private static String corpo(String titulo, String responsavel, String prazo) {
        return """
                {"titulo": "%s", "descricao": "Descrição de teste", "prioridade": "MEDIA",
                 "responsavel": "%s", "prazo": "%s"}""".formatted(titulo, responsavel, prazo);
    }
}
