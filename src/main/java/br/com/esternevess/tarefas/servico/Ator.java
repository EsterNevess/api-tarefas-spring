package br.com.esternevess.tarefas.servico;

import br.com.esternevess.tarefas.dominio.Perfil;

/** Quem está fazendo a requisição: nome de usuário e perfil. */
public record Ator(String nome, Perfil perfil) {

    public boolean ehColaborador() {
        return perfil == Perfil.COLABORADOR;
    }
}
