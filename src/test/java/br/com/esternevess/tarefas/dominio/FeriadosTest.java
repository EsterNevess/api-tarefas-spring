package br.com.esternevess.tarefas.dominio;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class FeriadosTest {

    /** Datas de Páscoa conferidas em calendário: o algoritmo precisa acertar anos diferentes. */
    @ParameterizedTest(name = "Páscoa de {0} é {1}")
    @CsvSource({
            "2024, 2024-03-31",
            "2025, 2025-04-20",
            "2026, 2026-04-05",
            "2027, 2027-03-28",
            "2038, 2038-04-25"
    })
    void calculaAPascoa(int ano, LocalDate esperada) {
        assertThat(Feriados.pascoa(ano)).isEqualTo(esperada);
    }

    @Test
    void sextaFeiraSantaEDoisDiasAntesDaPascoa() {
        assertThat(Feriados.sextaFeiraSanta(2026)).isEqualTo(LocalDate.of(2026, 4, 3));
    }

    @Test
    void incluiOsFeriadosFixosNacionais() {
        assertThat(Feriados.nacionaisDe(2026)).contains(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 4, 21),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 15),
                LocalDate.of(2026, 12, 25));
    }

    @Test
    void conscienciaNegraSoEFeriadoNacionalAPartirDe2024() {
        assertThat(Feriados.nacionaisDe(2023)).doesNotContain(LocalDate.of(2023, 11, 20));
        assertThat(Feriados.nacionaisDe(2024)).contains(LocalDate.of(2024, 11, 20));
    }

    @Test
    void naoTratarCarnavalComoFeriadoNacional() {
        // ponto facultativo, fora do escopo
        assertThat(Feriados.nacionaisDe(2026)).doesNotContain(LocalDate.of(2026, 2, 17));
    }
}
