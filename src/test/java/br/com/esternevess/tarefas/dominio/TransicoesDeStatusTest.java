package br.com.esternevess.tarefas.dominio;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static br.com.esternevess.tarefas.dominio.StatusTarefa.A_FAZER;
import static br.com.esternevess.tarefas.dominio.StatusTarefa.CONCLUIDA;
import static br.com.esternevess.tarefas.dominio.StatusTarefa.EM_ANDAMENTO;
import static br.com.esternevess.tarefas.dominio.StatusTarefa.PAUSADA;
import static br.com.esternevess.tarefas.dominio.StatusTarefa.PLANEJADA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransicoesDeStatusTest {

    static Stream<Arguments> transicoesPermitidas() {
        return Stream.of(
                Arguments.of(PLANEJADA, A_FAZER),
                Arguments.of(A_FAZER, EM_ANDAMENTO),
                Arguments.of(EM_ANDAMENTO, PAUSADA),
                Arguments.of(EM_ANDAMENTO, CONCLUIDA),
                Arguments.of(PAUSADA, EM_ANDAMENTO));
    }

    @ParameterizedTest(name = "{0} -> {1} é permitida")
    @MethodSource("transicoesPermitidas")
    void aceitaOsMovimentosDoFluxo(StatusTarefa de, StatusTarefa para) {
        assertThat(TransicoesDeStatus.permitida(de, para)).isTrue();
        assertThatCode(() -> TransicoesDeStatus.validar(de, para)).doesNotThrowAnyException();
    }

    /**
     * Em vez de listar à mão os movimentos proibidos, o teste percorre todas as 25 combinações
     * e confere que só as cinco permitidas passam. Se alguém acrescentar um status ou uma
     * transição sem querer, este teste acusa.
     */
    @Test
    void recusaTodasAsCombinacoesForaDoFluxo() {
        long permitidas = 0;
        for (StatusTarefa de : StatusTarefa.values()) {
            for (StatusTarefa para : StatusTarefa.values()) {
                if (TransicoesDeStatus.permitida(de, para)) {
                    permitidas++;
                }
            }
        }
        assertThat(permitidas).isEqualTo(5);
    }

    @Test
    void naoPermiteFicarNoMesmoStatus() {
        for (StatusTarefa status : StatusTarefa.values()) {
            assertThat(TransicoesDeStatus.permitida(status, status)).isFalse();
        }
    }

    @Test
    void tarefaConcluidaNaoVoltaParaNenhumStatus() {
        for (StatusTarefa para : StatusTarefa.values()) {
            assertThat(TransicoesDeStatus.permitida(CONCLUIDA, para)).isFalse();
        }
        assertThat(TransicoesDeStatus.destinosDe(CONCLUIDA)).isEmpty();
    }

    @Test
    void naoPulaEtapas() {
        assertThat(TransicoesDeStatus.permitida(PLANEJADA, EM_ANDAMENTO)).isFalse();
        assertThat(TransicoesDeStatus.permitida(A_FAZER, CONCLUIDA)).isFalse();
    }

    @Test
    void validarLancaExcecaoComMensagemClara() {
        assertThatThrownBy(() -> TransicoesDeStatus.validar(CONCLUIDA, A_FAZER))
                .isInstanceOf(TransicaoInvalidaException.class)
                .hasMessage("Não é possível mover a tarefa de CONCLUIDA para A_FAZER");
    }

    @Test
    void listaOsDestinosPossiveis() {
        assertThat(TransicoesDeStatus.destinosDe(EM_ANDAMENTO)).containsExactlyInAnyOrder(PAUSADA, CONCLUIDA);
    }

    @Test
    void alterarOResultadoNaoMudaAsRegras() {
        Set<StatusTarefa> destinos = TransicoesDeStatus.destinosDe(PLANEJADA);
        destinos.clear();
        assertThat(TransicoesDeStatus.destinosDe(PLANEJADA)).containsExactly(A_FAZER);
    }
}
