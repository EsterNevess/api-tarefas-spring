package br.com.esternevess.tarefas.dominio;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Feriados nacionais usados na contagem de dias úteis.
 *
 * <p><b>Escopo:</b> só os feriados nacionais. Feriados estaduais, municipais e
 * pontos facultativos (como o Carnaval) ficam de fora. A contagem de prazo
 * oficial, como a processual, tem regras próprias; isto é uma demonstração e não
 * substitui a contagem oficial.
 */
public final class Feriados {

    /** A Consciência Negra virou feriado nacional em 2024 (Lei 14.759/2023). */
    private static final int ANO_CONSCIENCIA_NEGRA_NACIONAL = 2024;

    private Feriados() {
    }

    public static Set<LocalDate> nacionaisDe(int ano) {
        Set<LocalDate> feriados = new HashSet<>();
        feriados.add(LocalDate.of(ano, 1, 1));   // Confraternização Universal
        feriados.add(LocalDate.of(ano, 4, 21));  // Tiradentes
        feriados.add(LocalDate.of(ano, 5, 1));   // Dia do Trabalho
        feriados.add(LocalDate.of(ano, 9, 7));   // Independência
        feriados.add(LocalDate.of(ano, 10, 12)); // Nossa Senhora Aparecida
        feriados.add(LocalDate.of(ano, 11, 2));  // Finados
        feriados.add(LocalDate.of(ano, 11, 15)); // Proclamação da República
        feriados.add(LocalDate.of(ano, 12, 25)); // Natal
        feriados.add(sextaFeiraSanta(ano));      // móvel: depende da Páscoa
        if (ano >= ANO_CONSCIENCIA_NEGRA_NACIONAL) {
            feriados.add(LocalDate.of(ano, 11, 20));
        }
        return feriados;
    }

    public static LocalDate sextaFeiraSanta(int ano) {
        return pascoa(ano).minusDays(2);
    }

    /**
     * Domingo de Páscoa, pelo algoritmo de Meeus/Jones/Butcher (calendário
     * gregoriano). É a única data móvel que precisamos: a Sexta-feira Santa
     * é sempre dois dias antes dela.
     */
    public static LocalDate pascoa(int ano) {
        int a = ano % 19;
        int b = ano / 100;
        int c = ano % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(ano, mes, dia);
    }
}
