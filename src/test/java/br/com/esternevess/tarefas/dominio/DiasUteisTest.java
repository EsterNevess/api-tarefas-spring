package br.com.esternevess.tarefas.dominio;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiasUteisTest {

    @ParameterizedTest(name = "{0} é dia útil? {1}")
    @CsvSource({
            "2026-10-26, true",    // segunda
            "2026-10-30, true",    // sexta
            "2026-10-31, false",   // sábado
            "2026-11-01, false",   // domingo
            "2026-11-02, false",   // Finados, que cai numa segunda
            "2026-12-25, false",   // Natal, numa sexta
            "2026-04-03, false",   // Sexta-feira Santa
            "2026-04-06, true"     // segunda depois da Páscoa
    })
    void identificaDiasUteis(LocalDate data, boolean esperado) {
        assertThat(DiasUteis.ehDiaUtil(data)).isEqualTo(esperado);
    }

    @Test
    void somarZeroDiasDevolveOProprioInicio() {
        LocalDate segunda = LocalDate.of(2026, 10, 26);
        assertThat(DiasUteis.adicionar(segunda, 0)).isEqualTo(segunda);
    }

    @Test
    void somarZeroDiasNaoMudaUmFimDeSemana() {
        LocalDate sabado = LocalDate.of(2026, 10, 31);
        assertThat(DiasUteis.adicionar(sabado, 0)).isEqualTo(sabado);
    }

    @Test
    void somaDiasDentroDaMesmaSemana() {
        // segunda 26/10 + 2 dias úteis = quarta 28/10
        assertThat(DiasUteis.adicionar(LocalDate.of(2026, 10, 26), 2)).isEqualTo(LocalDate.of(2026, 10, 28));
    }

    @Test
    void atravessaOFimDeSemana() {
        // sexta 23/10 + 1 dia útil = segunda 26/10, e não o sábado
        assertThat(DiasUteis.adicionar(LocalDate.of(2026, 10, 23), 1)).isEqualTo(LocalDate.of(2026, 10, 26));
    }

    @Test
    void pulaUmFeriadoNoMeioDoCaminho() {
        // quinta 03/09 + 3 dias úteis: sexta 04 (1º), segunda 07 é feriado,
        // terça 08 (2º), quarta 09 (3º)
        assertThat(DiasUteis.adicionar(LocalDate.of(2026, 9, 3), 3)).isEqualTo(LocalDate.of(2026, 9, 9));
    }

    @Test
    void pulaOFeriadoQueCaiLogoNoDiaSeguinte() {
        // sexta 30/10 + 1: sábado, domingo e a segunda 02/11 (Finados) ficam de fora, então terça 03/11
        assertThat(DiasUteis.adicionar(LocalDate.of(2026, 10, 30), 1)).isEqualTo(LocalDate.of(2026, 11, 3));
    }

    @Test
    void pulaASextaFeiraSanta() {
        // quinta 02/04 + 1: a sexta 03/04 é feriado, o fim de semana não conta, então segunda 06/04
        assertThat(DiasUteis.adicionar(LocalDate.of(2026, 4, 2), 1)).isEqualTo(LocalDate.of(2026, 4, 6));
    }

    @Test
    void atravessaAViradaDeAno() {
        // quarta 30/12 + 2: quinta 31/12 (1º); sexta 01/01 é feriado e o fim de semana não conta;
        // segunda 04/01/2027 (2º)
        assertThat(DiasUteis.adicionar(LocalDate.of(2026, 12, 30), 2)).isEqualTo(LocalDate.of(2027, 1, 4));
    }

    @Test
    void recusaQuantidadeNegativa() {
        assertThatThrownBy(() -> DiasUteis.adicionar(LocalDate.of(2026, 10, 26), -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativa");
    }
}
