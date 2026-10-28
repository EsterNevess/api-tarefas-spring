package br.com.esternevess.tarefas.servico;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.esternevess.tarefas.dominio.Prioridade;
import br.com.esternevess.tarefas.dominio.StatusTarefa;
import br.com.esternevess.tarefas.dominio.Tarefa;
import br.com.esternevess.tarefas.repositorio.TarefaRepository;

@Service
@Transactional
public class TarefaService {

    private static final Sort ORDEM_PADRAO = Sort.by("prazo", "id");

    private final TarefaRepository repositorio;
    private final Clock relogio;

    public TarefaService(TarefaRepository repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    public LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    @Transactional(readOnly = true)
    public List<Tarefa> listar(Ator ator, StatusTarefa status, String responsavel) {
        // Colaborador só enxerga o que é dele, não importa o filtro pedido.
        String dono = ator.ehColaborador() ? ator.nome() : responsavel;

        Specification<Tarefa> filtro = Specification.unrestricted();
        if (status != null) {
            filtro = filtro.and((tarefa, consulta, cb) -> cb.equal(tarefa.get("status"), status));
        }
        if (dono != null && !dono.isBlank()) {
            filtro = filtro.and((tarefa, consulta, cb) -> cb.equal(tarefa.get("responsavel"), dono));
        }
        return repositorio.findAll(filtro, ORDEM_PADRAO);
    }

    @Transactional(readOnly = true)
    public Tarefa buscar(Ator ator, Long id) {
        Tarefa tarefa = obter(id);
        exigirAcesso(ator, tarefa);
        return tarefa;
    }

    public Tarefa criar(String titulo, String descricao, Prioridade prioridade,
                        String responsavel, LocalDate prazo) {
        validarPrazo(prazo);
        Tarefa tarefa = new Tarefa(titulo, descricao, prioridade, responsavel, prazo,
                LocalDateTime.now(relogio));
        return repositorio.save(tarefa);
    }

    public Tarefa atualizar(Long id, String titulo, String descricao, Prioridade prioridade,
                            String responsavel, LocalDate prazo) {
        Tarefa tarefa = obter(id);
        if (!prazo.equals(tarefa.getPrazo())) {
            validarPrazo(prazo);
        }
        tarefa.atualizar(titulo, descricao, prioridade, responsavel, prazo);
        return tarefa;
    }

    public Tarefa mudarStatus(Ator ator, Long id, StatusTarefa novo) {
        Tarefa tarefa = obter(id);
        exigirAcesso(ator, tarefa);
        tarefa.mudarStatus(novo, LocalDateTime.now(relogio));
        return tarefa;
    }

    public void excluir(Long id) {
        repositorio.delete(obter(id));
    }

    private Tarefa obter(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new TarefaNaoEncontradaException(id));
    }

    private void exigirAcesso(Ator ator, Tarefa tarefa) {
        if (ator.ehColaborador() && !ator.nome().equals(tarefa.getResponsavel())) {
            throw new AccessDeniedException("Colaborador só acessa as próprias tarefas");
        }
    }

    private void validarPrazo(LocalDate prazo) {
        if (prazo.isBefore(hoje())) {
            throw new PrazoInvalidoException("O prazo não pode estar no passado");
        }
    }
}
