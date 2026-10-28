package br.com.esternevess.tarefas.dominio;

/**
 * Etiqueta calculada a partir do prazo. Não é guardada no banco: muda com o
 * passar dos dias, e guardar um valor que envelhece é pedir para ele ficar errado.
 */
public enum SituacaoTarefa {
    NO_PRAZO,
    URGENTE,
    ATRASADA,
    ENCERRADA
}
