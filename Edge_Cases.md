# Edge Cases


### Caso 1: Ordem de execução diferente entre Nó e Sequencer após transferência otimizada

**Objetivo**: Justificar a necessidade de bloquear o broadcast durante a execução de transferências otimizadas.

| Transferência | Wallet Origem | Wallet Destino | Valor | Estado        |
| ------------- | ------------- | -------------- | ----- | ------------- |
| T1            | wA1           | wA2            | 20    | Otimizada     |
| T2            | wA2           | Z              | 20    | Não otimizada |


**Descrição do caso**:
Ordem de acontecimentos:
1) T1 é executada localmente
2) T2 é broadcasted
3) T1 é broadcasted
4) T2 volta do Sequencer e é executada

Resultado de execução:
No Nó: T1, T2
No Sequencer: T2, T1