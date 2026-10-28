package br.com.esternevess.tarefas.dominio;

/** Tentativa de mover a tarefa para um status que o fluxo não permite. */
public class TransicaoInvalidaException extends RuntimeException {

    public TransicaoInvalidaException(StatusTarefa de, StatusTarefa para) {
        super("Não é possível mover a tarefa de " + de + " para " + para);
    }
}
