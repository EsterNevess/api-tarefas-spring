package br.com.esternevess.tarefas.web;

import jakarta.validation.constraints.NotNull;

import br.com.esternevess.tarefas.dominio.StatusTarefa;

public record StatusRequest(@NotNull StatusTarefa status) {
}
