package br.com.esternevess.tarefas.dominio;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraDeSituacaoTest {

    // segunda-feira; dois dias úteis depois é quarta 28/10
    private static final LocalDate SEGUNDA = LocalDate.of(2026, 10, 26);

    @Test
    void prazoAnteriorAHojeEstaAtrasado() {
        assertThat(CalculadoraDeSituacao.calcular(StatusTarefa.EM_ANDAMENTO, LocalDate.of(2026, 10, 25), SEGUNDA))
                .isEqualTo(SituacaoTarefa.ATRASADA);
    }

    @Test
    void prazoParaHojeEUrgenteENaoAtrasado() {
        assertThat(CalculadoraDeSituacao.calcular(StatusTarefa.A_FAZER, SEGUNDA, SEGUNDA))
                .isEqualTo(SituacaoTarefa.URGENTE);
    }

    @Test
    void prazoNoSegundoDiaUtilAindaEUrgente() {
        assertThat(CalculadoraDeSituacao.calcular(StatusTarefa.A_FAZER, LocalDate.of(2026, 10, 28), SEGUNDA))
                .isEqualTo(SituacaoTarefa.URGENTE);
    }

    @Test
    void prazoNoTerceiroDiaUtilTemFolga() {
        assertThat(CalculadoraDeSituacao.calcular(StatusTarefa.A_FAZER, LocalDate.of(2026, 10, 29), SEGUNDA))
                .isEqualTo(SituacaoTarefa.NO_PRAZO);
    }

    @Test
    void fimDeSemanaEFeriadoNaoConsomemAJanelaDeUrgencia() {
        // quinta 29/10: o 1º dia útil é sexta 30/10, o 2º é terça 03/11 (segunda 02/11 é Finados)
        LocalDate quinta = LocalDate.of(2026, 10, 29);
        assertThat(CalculadoraDeSituacao.calcular(StatusTarefa.A_FAZER, LocalDate.of(2026, 11, 3), quinta))
                .isEqualTo(SituacaoTarefa.URGENTE);
        assertThat(CalculadoraDeSituacao.calcular(StatusTarefa.A_FAZER, LocalDate.of(2026, 11, 4), quinta))
                .isEqualTo(SituacaoTarefa.NO_PRAZO);
    }

    @Test
    void tarefaConcluidaEstaEncerradaMesmoComPrazoVencido() {
        assertThat(CalculadoraDeSituacao.calcular(StatusTarefa.CONCLUIDA, LocalDate.of(2026, 1, 1), SEGUNDA))
                .isEqualTo(SituacaoTarefa.ENCERRADA);
    }

    @ParameterizedTest
    @EnumSource(value = StatusTarefa.class, names = "CONCLUIDA", mode = EnumSource.Mode.EXCLUDE)
    void somenteAConcluidaViraEncerrada(StatusTarefa status) {
        assertThat(CalculadoraDeSituacao.calcular(status, LocalDate.of(2026, 12, 1), SEGUNDA))
                .isNotEqualTo(SituacaoTarefa.ENCERRADA);
    }
}
