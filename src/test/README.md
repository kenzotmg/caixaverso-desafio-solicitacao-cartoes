# Estrutura de testes

Os testes de tipos de cartao e das transicoes de solicitacao estao implementados.

- `entity/SolicitacaoCartaoTest`: transicoes permitidas, rejeicao de estados finais e preservacao dos dados ao reconstruir uma solicitacao.
- `controller/TransicoesSolicitacaoIntegracaoTest`: HTTP com JWT real e H2; cancelamento, emissao unica, titularidade, entrada invalida, rollback e concorrencia. Cada cenario cria e remove seus proprios registros.
- `repository/TipoCartaoRepositoryJdbcTest`: SQL real em H2 isolado para filtro, ordenacao, mapeamento e falha de conexao.
- `repository/TipoCartaoRepositoryJpaTest` e `usecase/TipoCartaoUseCaseTest`: testes unitarios com Mockito.

`usecase/SolicitacaoUseCaseTest` e `controller/SegurancaIntegracaoTest` continuam como estruturas vazias.

No PowerShell, executar os testes das duas rotas:

```powershell
.\mvnw.cmd "-Dtest=SolicitacaoCartaoTest,TransicoesSolicitacaoIntegracaoTest" "-Dquarkus.http.test-port=0" test
```

A porta `0` seleciona uma porta livre, sem interferir em outra API em execucao.
O perfil test usa H2, sem PostgreSQL e sem abrir o servidor TCP H2.

O cenario de rollback prepara deliberadamente um cartao ja vinculado a um pedido
PENDENTE. A tentativa de inserir outro cartao viola a restricao UNIQUE e retorna
409; a alteracao para EMITIDA deve ser desfeita, mantendo o pedido PENDENTE e
somente o cartao previamente preparado.
