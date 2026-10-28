package br.com.esternevess.tarefas.dominio;

import java.time.LocalDate;

/**
 * Decide a etiqueta da tarefa a partir do prazo.
 *
 * <ul>
 *   <li>{@code ENCERRADA}: já foi concluída, o prazo não importa mais.</li>
 *   <li>{@code ATRASADA}: o prazo é anterior a hoje.</li>
 *   <li>{@code URGENTE}: vence hoje ou em até {@value #DIAS_UTEIS_DE_URGENCIA} dias úteis.</li>
 *   <li>{@code NO_PRAZO}: tem folga.</li>
 * </ul>
 *
 * Quem pergunta "que dia é hoje" é quem chama, e não esta classe. Assim o teste fixa a
 * data e o resultado deixa de depender do dia em que ele roda, que é a causa clássica
 * de teste que passa hoje e quebra na segunda-feira.
 */
public final class CalculadoraDeSituacao {

    static final int DIAS_UTEIS_DE_URGENCIA = 2;

    private CalculadoraDeSituacao() {
    }

    public static SituacaoTarefa calcular(StatusTarefa status, LocalDate prazo, LocalDate hoje) {
        if (status == StatusTarefa.CONCLUIDA) {
            return SituacaoTarefa.ENCERRADA;
        }
        if (prazo.isBefore(hoje)) {
            return SituacaoTarefa.ATRASADA;
        }
        LocalDate limiteDaUrgencia = DiasUteis.adicionar(hoje, DIAS_UTEIS_DE_URGENCIA);
        if (!prazo.isAfter(limiteDaUrgencia)) {
            return SituacaoTarefa.URGENTE;
        }
        return SituacaoTarefa.NO_PRAZO;
    }
}
