package br.com.esternevess.tarefas.servico;

public class PrazoInvalidoException extends RuntimeException {

    public PrazoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
