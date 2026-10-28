package br.com.esternevess.tarefas.web;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import br.com.esternevess.tarefas.dominio.Prioridade;

public record TarefaRequest(
        @NotBlank @Size(max = 120) String titulo,
        @Size(max = 1000) String descricao,
        @NotNull Prioridade prioridade,
        @NotBlank @Size(max = 50) String responsavel,
        @NotNull LocalDate prazo) {
}
