package br.com.esternevess.tarefas.dominio;

/**
 * Em que ponto do fluxo a tarefa está. As colunas espelham o quadro kanban:
 * planejada, a fazer, em andamento, pausada e concluída.
 */
public enum StatusTarefa {
    PLANEJADA,
    A_FAZER,
    EM_ANDAMENTO,
    PAUSADA,
    CONCLUIDA
}
