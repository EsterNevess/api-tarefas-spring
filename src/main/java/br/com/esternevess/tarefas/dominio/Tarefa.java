package br.com.esternevess.tarefas.dominio;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Uma tarefa do quadro. A regra de mudança de status vive aqui dentro, e não no
 * controlador: quem tem o dado é quem sabe se a mudança é válida.
 */
@Entity
@Table(name = "tarefas")
public class Tarefa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(length = 1000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Prioridade prioridade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusTarefa status;

    @Column(nullable = false, length = 50)
    private String responsavel;

    @Column(nullable = false)
    private LocalDate prazo;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;

    /** Exigido pelo JPA; não use diretamente. */
    protected Tarefa() {
    }

    public Tarefa(String titulo, String descricao, Prioridade prioridade, String responsavel,
                  LocalDate prazo, LocalDateTime criadaEm) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.prioridade = prioridade;
        this.responsavel = responsavel;
        this.prazo = prazo;
        this.criadaEm = criadaEm;
        this.status = StatusTarefa.PLANEJADA;
    }

    /** Edita os dados da tarefa. O status não muda por aqui, só por {@link #mudarStatus}. */
    public void atualizar(String titulo, String descricao, Prioridade prioridade,
                          String responsavel, LocalDate prazo) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.prioridade = prioridade;
        this.responsavel = responsavel;
        this.prazo = prazo;
    }

    /**
     * Move a tarefa no fluxo.
     *
     * @throws TransicaoInvalidaException se o fluxo não permitir esse movimento
     */
    public void mudarStatus(StatusTarefa novo, LocalDateTime agora) {
        TransicoesDeStatus.validar(this.status, novo);
        this.status = novo;
        if (novo == StatusTarefa.CONCLUIDA) {
            this.concluidaEm = agora;
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public StatusTarefa getStatus() {
        return status;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public LocalDate getPrazo() {
        return prazo;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public LocalDateTime getConcluidaEm() {
        return concluidaEm;
    }
}
