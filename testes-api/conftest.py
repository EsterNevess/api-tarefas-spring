import os
from datetime import date, timedelta

import pytest
import requests

URL_BASE = os.environ.get("API_URL", "http://localhost:8080")
SENHA = os.environ.get("SENHA_DEMO", "senha123")


class Cliente:
    """Pequeno envoltório do requests: já sabe a URL base e quem está logado."""

    def __init__(self, usuario):
        self.sessao = requests.Session()
        self.sessao.auth = (usuario, SENHA)

    def request(self, metodo, caminho, **kwargs):
        return self.sessao.request(metodo, URL_BASE + caminho, timeout=10, **kwargs)

    def get(self, caminho, **kwargs):
        return self.request("GET", caminho, **kwargs)

    def post(self, caminho, **kwargs):
        return self.request("POST", caminho, **kwargs)

    def put(self, caminho, **kwargs):
        return self.request("PUT", caminho, **kwargs)

    def patch(self, caminho, **kwargs):
        return self.request("PATCH", caminho, **kwargs)

    def delete(self, caminho, **kwargs):
        return self.request("DELETE", caminho, **kwargs)


def prazo_em(dias):
    return (date.today() + timedelta(days=dias)).isoformat()


def corpo_tarefa(titulo="Tarefa de teste", responsavel="ana", dias=15, **extra):
    corpo = {
        "titulo": titulo,
        "descricao": "Criada pelos testes de API",
        "prioridade": "MEDIA",
        "responsavel": responsavel,
        "prazo": prazo_em(dias),
    }
    corpo.update(extra)
    return corpo


@pytest.fixture(scope="session")
def admin():
    return Cliente("admin")


@pytest.fixture(scope="session")
def supervisor():
    return Cliente("supervisor")


@pytest.fixture(scope="session")
def ana():
    return Cliente("ana")


@pytest.fixture(scope="session")
def bruno():
    return Cliente("bruno")


@pytest.fixture
def tarefa(admin):
    """Cria uma tarefa da Ana e a remove no fim do teste, para não deixar lixo no banco."""
    resposta = admin.post("/tarefas", json=corpo_tarefa())
    assert resposta.status_code == 201, resposta.text
    criada = resposta.json()
    yield criada
    admin.delete(f"/tarefas/{criada['id']}")
