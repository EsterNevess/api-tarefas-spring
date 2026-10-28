package br.com.esternevess.tarefas.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import br.com.esternevess.tarefas.dominio.CalculadoraDeSituacao;
import br.com.esternevess.tarefas.dominio.Prioridade;
import br.com.esternevess.tarefas.dominio.SituacaoTarefa;
import br.com.esternevess.tarefas.dominio.StatusTarefa;
import br.com.esternevess.tarefas.dominio.Tarefa;
import br.com.esternevess.tarefas.dominio.TransicoesDeStatus;

public record TarefaResponse(
        Long id,
        String titulo,
        String descricao,
        Prioridade prioridade,
        StatusTarefa status,
        String responsavel,
        LocalDate prazo,
        SituacaoTarefa situacao,
        List<StatusTarefa> proximosStatus,
        LocalDateTime criadaEm,
        LocalDateTime concluidaEm) {

    public static TarefaResponse de(Tarefa tarefa, LocalDate hoje) {
        return new TarefaResponse(
                tarefa.getId(),
                tarefa.getTitulo(),
                tarefa.getDescricao(),
                tarefa.getPrioridade(),
                tarefa.getStatus(),
                tarefa.getResponsavel(),
                tarefa.getPrazo(),
                CalculadoraDeSituacao.calcular(tarefa.getStatus(), tarefa.getPrazo(), hoje),
                TransicoesDeStatus.destinosDe(tarefa.getStatus()).stream().sorted().toList(),
                tarefa.getCriadaEm(),
                tarefa.getConcluidaEm());
    }
}
