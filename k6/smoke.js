import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.API_URL || 'http://localhost:8080';
const SENHA = __ENV.SENHA_DEMO || 'senha123';

export const options = {
  vus: 5,
  duration: '20s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'],
    checks: ['rate>0.99'],
  },
};

function prazo(dias) {
  const d = new Date(Date.now() + dias * 86400000);
  return d.toISOString().slice(0, 10);
}

export default function () {
  const admin = { auth: 'basic', headers: { 'Content-Type': 'application/json' } };
  const cred = `admin:${SENHA}`;
  const url = (p) => `http://${cred}@${BASE.replace(/^https?:\/\//, '')}${p}`;

  const criada = http.post(
    url('/tarefas'),
    JSON.stringify({ titulo: 'Tarefa k6', prioridade: 'ALTA', responsavel: 'ana', prazo: prazo(10) }),
    admin,
  );
  check(criada, { 'criou (201)': (r) => r.status === 201 });
  const id = criada.json('id');

  const lista = http.get(url('/tarefas'), admin);
  check(lista, { 'listou (200)': (r) => r.status === 200 });

  const muda = http.patch(url(`/tarefas/${id}/status`), JSON.stringify({ status: 'A_FAZER' }), admin);
  check(muda, { 'mudou status (200)': (r) => r.status === 200 });

  // O 409 é o resultado esperado aqui, então não deve contar como requisição que falhou.
  const esperado409 = { ...admin, responseCallback: http.expectedStatuses(409) };
  const invalida = http.patch(url(`/tarefas/${id}/status`), JSON.stringify({ status: 'CONCLUIDA' }), esperado409);
  check(invalida, { 'transição inválida (409)': (r) => r.status === 409 });

  const apaga = http.del(url(`/tarefas/${id}`), null, admin);
  check(apaga, { 'excluiu (204)': (r) => r.status === 204 });

  sleep(0.5);
}
