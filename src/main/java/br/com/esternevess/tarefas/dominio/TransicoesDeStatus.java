package br.com.esternevess.tarefas.dominio;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static br.com.esternevess.tarefas.dominio.StatusTarefa.A_FAZER;
import static br.com.esternevess.tarefas.dominio.StatusTarefa.CONCLUIDA;
import static br.com.esternevess.tarefas.dominio.StatusTarefa.EM_ANDAMENTO;
import static br.com.esternevess.tarefas.dominio.StatusTarefa.PAUSADA;
import static br.com.esternevess.tarefas.dominio.StatusTarefa.PLANEJADA;

/**
 * O fluxo permitido entre status, em um só lugar.
 *
 * <pre>
 * PLANEJADA -> A_FAZER -> EM_ANDAMENTO -> CONCLUIDA
 *                              ^  |
 *                              |  v
 *                            PAUSADA
 * </pre>
 *
 * Uma tarefa concluída não volta: reabrir seria criar outra. Deixar a regra numa
 * tabela, e não espalhada em vários "if", faz o fluxo caber numa olhada e torna
 * trivial testar todas as combinações.
 */
public final class TransicoesDeStatus {

    private static final Map<StatusTarefa, Set<StatusTarefa>> PERMITIDAS = new EnumMap<>(StatusTarefa.class);

    static {
        PERMITIDAS.put(PLANEJADA, EnumSet.of(A_FAZER));
        PERMITIDAS.put(A_FAZER, EnumSet.of(EM_ANDAMENTO));
        PERMITIDAS.put(EM_ANDAMENTO, EnumSet.of(PAUSADA, CONCLUIDA));
        PERMITIDAS.put(PAUSADA, EnumSet.of(EM_ANDAMENTO));
        PERMITIDAS.put(CONCLUIDA, EnumSet.noneOf(StatusTarefa.class));
    }

    private TransicoesDeStatus() {
    }

    /** Para quais status a tarefa pode ir a partir de {@code de}. A cópia evita alterar a tabela. */
    public static Set<StatusTarefa> destinosDe(StatusTarefa de) {
        return EnumSet.copyOf(PERMITIDAS.get(de));
    }

    public static boolean permitida(StatusTarefa de, StatusTarefa para) {
        return PERMITIDAS.get(de).contains(para);
    }

    /** Lança {@link TransicaoInvalidaException} se o movimento não for permitido. */
    public static void validar(StatusTarefa de, StatusTarefa para) {
        if (!permitida(de, para)) {
            throw new TransicaoInvalidaException(de, para);
        }
    }
}
