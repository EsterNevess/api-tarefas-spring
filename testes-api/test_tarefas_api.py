import allure
import pytest
import requests

from conftest import URL_BASE, corpo_tarefa


@allure.feature("Autenticação")
class TestAutenticacao:
    def test_sem_credenciais_retorna_401(self):
        assert requests.get(URL_BASE + "/tarefas", timeout=10).status_code == 401

    def test_senha_errada_retorna_401(self):
        resposta = requests.get(URL_BASE + "/tarefas", auth=("admin", "errada"), timeout=10)
        assert resposta.status_code == 401

    def test_health_e_publico(self):
        resposta = requests.get(URL_BASE + "/actuator/health", timeout=10)
        assert resposta.status_code == 200
        assert resposta.json()["status"] == "UP"


@allure.feature("Permissões por perfil")
class TestPermissoes:
    def test_colaborador_nao_cria_tarefa(self, ana):
        assert ana.post("/tarefas", json=corpo_tarefa()).status_code == 403

    def test_supervisor_cria_mas_nao_exclui(self, supervisor, admin):
        criada = supervisor.post("/tarefas", json=corpo_tarefa("Criada pela supervisão"))
        assert criada.status_code == 201
        id_ = criada.json()["id"]
        assert supervisor.delete(f"/tarefas/{id_}").status_code == 403
        assert admin.delete(f"/tarefas/{id_}").status_code == 204

    def test_colaborador_nao_mexe_na_tarefa_de_outro(self, tarefa, bruno):
        assert bruno.get(f"/tarefas/{tarefa['id']}").status_code == 403
        resposta = bruno.patch(f"/tarefas/{tarefa['id']}/status", json={"status": "A_FAZER"})
        assert resposta.status_code == 403

    def test_colaborador_so_lista_as_proprias(self, tarefa, bruno):
        ids = [t["id"] for t in bruno.get("/tarefas").json()]
        assert tarefa["id"] not in ids


@allure.feature("Criação e validação")
class TestCriacao:
    def test_cria_tarefa_planejada(self, tarefa):
        assert tarefa["status"] == "PLANEJADA"
        assert tarefa["situacao"] == "NO_PRAZO"
        assert tarefa["proximosStatus"] == ["A_FAZER"]
        assert tarefa["concluidaEm"] is None

    def test_titulo_vazio_retorna_400_com_detalhe_do_campo(self, admin):
        resposta = admin.post("/tarefas", json=corpo_tarefa(titulo=""))
        assert resposta.status_code == 400
        assert "titulo" in resposta.json()["erros"]

    def test_prazo_no_passado_retorna_400(self, admin):
        resposta = admin.post("/tarefas", json=corpo_tarefa(dias=-1))
        assert resposta.status_code == 400
        assert "passado" in resposta.json()["detail"]

    def test_prioridade_desconhecida_retorna_400(self, admin):
        assert admin.post("/tarefas", json=corpo_tarefa(prioridade="ENORME")).status_code == 400

    def test_prazo_de_hoje_e_urgente(self, admin):
        resposta = admin.post("/tarefas", json=corpo_tarefa(dias=0))
        assert resposta.status_code == 201
        try:
            assert resposta.json()["situacao"] == "URGENTE"
        finally:
            admin.delete(f"/tarefas/{resposta.json()['id']}")

    def test_tarefa_inexistente_retorna_404(self, admin):
        assert admin.get("/tarefas/999999999").status_code == 404


@allure.feature("Fluxo de status")
class TestFluxoDeStatus:
    def test_percorre_o_fluxo_ate_concluir(self, tarefa, ana):
        caminho = f"/tarefas/{tarefa['id']}/status"
        for novo in ["A_FAZER", "EM_ANDAMENTO", "CONCLUIDA"]:
            resposta = ana.patch(caminho, json={"status": novo})
            assert resposta.status_code == 200, resposta.text
        final = resposta.json()
        assert final["situacao"] == "ENCERRADA"
        assert final["concluidaEm"] is not None
        assert final["proximosStatus"] == []

    def test_pausar_e_retomar(self, tarefa, ana):
        caminho = f"/tarefas/{tarefa['id']}/status"
        for novo in ["A_FAZER", "EM_ANDAMENTO", "PAUSADA", "EM_ANDAMENTO"]:
            assert ana.patch(caminho, json={"status": novo}).status_code == 200

    @pytest.mark.parametrize("destino", ["EM_ANDAMENTO", "CONCLUIDA", "PAUSADA", "PLANEJADA"])
    def test_tarefa_planejada_so_vai_para_a_fazer(self, tarefa, ana, destino):
        resposta = ana.patch(f"/tarefas/{tarefa['id']}/status", json={"status": destino})
        assert resposta.status_code == 409

    def test_tarefa_concluida_nao_volta(self, tarefa, ana):
        caminho = f"/tarefas/{tarefa['id']}/status"
        for novo in ["A_FAZER", "EM_ANDAMENTO", "CONCLUIDA"]:
            ana.patch(caminho, json={"status": novo})
        assert ana.patch(caminho, json={"status": "EM_ANDAMENTO"}).status_code == 409


@allure.feature("Listagem")
class TestListagem:
    def test_filtra_por_status(self, tarefa, admin, ana):
        ana.patch(f"/tarefas/{tarefa['id']}/status", json={"status": "A_FAZER"})
        ids = [t["id"] for t in admin.get("/tarefas", params={"status": "A_FAZER"}).json()]
        assert tarefa["id"] in ids
        outros = admin.get("/tarefas", params={"status": "PLANEJADA"}).json()
        assert tarefa["id"] not in [t["id"] for t in outros]

    def test_status_invalido_no_filtro_retorna_400(self, admin):
        assert admin.get("/tarefas", params={"status": "VOANDO"}).status_code == 400

    def test_atualiza_dados_da_tarefa(self, tarefa, supervisor):
        resposta = supervisor.put(
            f"/tarefas/{tarefa['id']}",
            json=corpo_tarefa("Título novo", responsavel="bruno", dias=20),
        )
        assert resposta.status_code == 200
        assert resposta.json()["titulo"] == "Título novo"
        assert resposta.json()["responsavel"] == "bruno"
