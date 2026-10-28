package br.com.esternevess.tarefas.web;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.esternevess.tarefas.dominio.Perfil;
import br.com.esternevess.tarefas.dominio.StatusTarefa;
import br.com.esternevess.tarefas.dominio.Tarefa;
import br.com.esternevess.tarefas.servico.Ator;
import br.com.esternevess.tarefas.servico.TarefaService;

@RestController
@RequestMapping("/tarefas")
public class TarefaController {

    private final TarefaService servico;

    public TarefaController(TarefaService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<TarefaResponse> listar(Authentication autenticacao,
                                       @RequestParam(required = false) StatusTarefa status,
                                       @RequestParam(required = false) String responsavel) {
        return servico.listar(ator(autenticacao), status, responsavel).stream()
                .map(this::resposta)
                .toList();
    }

    @GetMapping("/{id}")
    public TarefaResponse buscar(Authentication autenticacao, @PathVariable Long id) {
        return resposta(servico.buscar(ator(autenticacao), id));
    }

    @PostMapping
    public ResponseEntity<TarefaResponse> criar(@Valid @RequestBody TarefaRequest pedido) {
        Tarefa tarefa = servico.criar(pedido.titulo(), pedido.descricao(), pedido.prioridade(),
                pedido.responsavel(), pedido.prazo());
        return ResponseEntity.created(URI.create("/tarefas/" + tarefa.getId())).body(resposta(tarefa));
    }

    @PutMapping("/{id}")
    public TarefaResponse atualizar(@PathVariable Long id, @Valid @RequestBody TarefaRequest pedido) {
        return resposta(servico.atualizar(id, pedido.titulo(), pedido.descricao(), pedido.prioridade(),
                pedido.responsavel(), pedido.prazo()));
    }

    @PatchMapping("/{id}/status")
    public TarefaResponse mudarStatus(Authentication autenticacao, @PathVariable Long id,
                                      @Valid @RequestBody StatusRequest pedido) {
        return resposta(servico.mudarStatus(ator(autenticacao), id, pedido.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        servico.excluir(id);
        return ResponseEntity.noContent().build();
    }

    private TarefaResponse resposta(Tarefa tarefa) {
        return TarefaResponse.de(tarefa, servico.hoje());
    }

    private static Ator ator(Authentication autenticacao) {
        Perfil perfil = autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(papel -> papel.startsWith("ROLE_"))
                .map(papel -> Perfil.valueOf(papel.substring("ROLE_".length())))
                .findFirst()
                .orElseThrow();
        return new Ator(autenticacao.getName(), perfil);
    }
}
