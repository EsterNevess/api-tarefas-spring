package br.com.esternevess.tarefas.dominio;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Contagem de dias úteis: ignora sábados, domingos e feriados nacionais. */
public final class DiasUteis {

    /** Evita recalcular a Páscoa e os feriados do mesmo ano a cada chamada. */
    private static final Map<Integer, Set<LocalDate>> FERIADOS_POR_ANO = new ConcurrentHashMap<>();

    private DiasUteis() {
    }

    public static boolean ehDiaUtil(LocalDate data) {
        DayOfWeek dia = data.getDayOfWeek();
        if (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) {
            return false;
        }
        return !FERIADOS_POR_ANO.computeIfAbsent(data.getYear(), Feriados::nacionaisDe).contains(data);
    }

    /**
     * Avança {@code dias} dias úteis a partir de {@code inicio}.
     *
     * <p>O dia de início não conta. Com zero dias, devolve o próprio início, mesmo
     * que ele não seja dia útil: "somar nada" não deve mudar a data.
     *
     * @throws IllegalArgumentException se {@code dias} for negativo
     */
    public static LocalDate adicionar(LocalDate inicio, int dias) {
        if (dias < 0) {
            throw new IllegalArgumentException("A quantidade de dias úteis não pode ser negativa");
        }
        LocalDate atual = inicio;
        int restantes = dias;
        while (restantes > 0) {
            atual = atual.plusDays(1);
            if (ehDiaUtil(atual)) {
                restantes--;
            }
        }
        return atual;
    }
}
