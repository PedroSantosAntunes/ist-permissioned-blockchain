# Edge Cases


## Caso 1: Ordem de execução diferente entre Nó e Sequencer após transferência otimizada

**Objetivo**: Justificar a necessidade de bloquear o broadcast durante a execução de transferências otimizadas.  

| Transferência | Wallet Origem | Wallet Destino | Valor | Estado        |
| ------------- | ------------- | -------------- | ----- | ------------- |
| T1            | wA1           | wA2            | 20    | Otimizada     |
| T2            | wA2           | Z              | 20    | Não otimizada |


**Ordem de acontecimentos:**  
T1 é executada localmente  
T2 é enviada em broadcast  
T1 é enviada em broadcast  
T2 volta do Sequencer e é executada  

**Resultado de execução:**  
No Nó: T1, T2  
No Sequencer: T2, T1  

**Explicação**  
Entre T1 ser executada e ser feito o seu broadcast, pode ocorrer o broadcast de T2 resultando na execução inconsistente de duas transações com uma relação causal.

**Solução:**  
Enquanto uma transação otimizada não acabar de ser executada e esperar pela resposta do seu broadcast, não é feito nenhum broadcast de transações que estejam causalmente relacionadas com a otimizada.



## Caso 2: Ordem de execução diferente quando se otimiza uma transação enquanto existem um delete pending

**Objetivo**: Justificar porque é que não podemos otimizar transferências quando existem deletes que foram para o sequencer e ainda não voltaram

| Transação/Operação | Wallet Origem | Wallet Destino | Valor | Estado         |
| ------------------ | ------------- | -------------- | ----- | -------------- |
| T1                 | wA1           | wA2            | 20    | Otimizada      |
| D1                 | wA1           | –              | –     | Pending Delete |

**Ordem de acontecimentos:**  
D1 é enviada em broadcast  
T1 é executada localmente  
T1 é enviada em broadcast  
D1 retorna do Sequencer e é executada  

**Resultado de execução:**  
Nó: T1, D1  
Sequencer: D1, T1  

**Explicação**  
Se D1 for enviado antes e T1 for otimizada antes de receber a confirmação do delete, a ordem final difere entre Nó e Sequencer, causando inconsistência na execução.

**Solução:**  
Não se devem otimizar transações que têm deletes pendentes associados a uma das suas wallets, garantindo isso com um contador de pending deletes.


## Caso 3: Ordem de execução diferente quando se otimiza uma transação enquanto existem vários deletes pending

**Objetivo**: Justificar o uso de um counter me vez de uma flag.  

| Transação/Operação | Wallet Origem | Wallet Destino | Valor | Estado         |
| ------------------ | ------------- | -------------- | ----- | -------------- |
| T1                 | wA1           | wA2            | 20    | Otimizada      |
| D1                 | wA1           | –              | –     | Pending Delete |
| D2                 | wA1           | –              | –     | Pending Delete |

**Ordem de acontecimentos:**  
D1 mete a flag delete em wA1  
D1 é enviada em broadcast  
D2 mete a flag delete em wA1  
D2 é enviada em broadcast  

D1 retorna do Sequencer e falha por existência de saldo em wA1  
D1 tira a flag delete de wA1  

T1 é executada localmente  
T1 é enviada em broadcast  

D2 retorna do Sequencer e é executada  

**Resultado de execução:**  
Nó: D1, T1, D2  
Sequencer: D1, D2, T1  

**Explicação**  
Ao utilizarmos uma flag para os pending deletes, não é possível saber quantos deletes devem ser aguardados antes de otimizar uma transferência.  

**Solução:**  
Utilizar um contador em vez de uma flag permite saber quantos pending deletes existem.  


## Caso 4: Ordem de execução diferente quando se otimiza uma transação enquanto existem outras transações pending para a mesma source wallet

**Objetivo**: Justificar porque é que não podemos otimizar transferências quando existem outras transferências da mesma source wallet que foram para o sequencer e ainda não voltaram  

| Wallet | Saldo |
| ------ | ----- |
| wA1    | 20    |
| wA2    | 0     |
| z      | 20    |

| Transferência | Wallet Origem | Wallet Destino | Valor | Estado         | ID |
| ------------- | ------------- | -------------- | ----- | -------------- | -- |
| T1            | wA1           | Z              | 20    | Não otimizável | T1 |
| T2            | wA1           | wA2            | 20    | Otimizada      | T2 |


**Ordem de acontecimentos:**  
T1 é enviada em broadcast  
T2 é executada localmente  
T2 é enviada em broadcast  
T1 retorna do Sequencer e falha  


**Resultado de execução:**  
Nó: T2, T1  
| Wallet | Saldo |
| ------ | ----- |
| wA1    | 0     |
| wA2    | 20    |
| z      | 0     |

Sequencer: T1, T2  
| Wallet | Saldo |
| ------ | ----- |
| wA1    | 0     |
| wA2    | 0     |
| z      | 20    |



**Explicação**  
Se T1 for enviado antes e T2 for otimizada antes de receber a confirmação do T1, a ordem final difere entre Nó e Sequencer, causando inconsistência na execução.  

**Solução:**  
Utilizar um contador permite saber quantos pending transfers existem.  


## Caso 5: Otimização de transações pendentes  

**Objetivo**: Justificar a utlização do pending Deficit para otimizar mais transferências.  

| Wallet | Saldo |
| ------ | ----- |
| BC     | 1000  |

| Transferência | Wallet Origem | Wallet Destino | Valor | Estado        | ID |
| ------------- | ------------- | -------------- | ----- | ------------- | -- |
| T1            | BC            | Z              | 1     | Não otimizada | T1 |
| T2            | BC            | Y              | 1     | Não otimizada | T2 |
| T3            | BC            | wA1            | 1     | Não otimizada | T3 |


**Ordem de acontecimentos:**
T1 é enviada em broadcast  
T2 é enviada em broadcast  
T3 não otimizada porque T1 está pending  
T3 é enviada em broadcast  
T1 retorna do Sequencer e é executada  
T2 retorna do Sequencer e é executada  
T3 retorna do Sequencer e é executada

**Explicação**  
Se T1 e T2 forem enviadas, quando T3 chega não pode ser otimizado porque existem duas transferências de BC por concluir. No entanto, mesmo se ambas T1 e T2 tiverem sucesso BC ainda teria 998 de saldo, sendo ainda possível a execução de T3.

**Solução:**  
Manter uma contagem do défice de cada carteira, sendo o défice o total montante pedido para remover de cada carteira. Permitindo calcular o valor minimo presente em cada carteira após todas as transferências serem concluidas.  

Nota:  
Usamos a fórmula `Balance Real - pendingDeficit >= Transfer Value` para avaliar se a transferência é otimizável.  



## Caso 6: Ordem de execução diferente entre o Nó e Sequencer quando se guarda informação pending em wallets  

**Objetivo**: Justificar a existência de uma estrutura separada para guardar a informação pending de cada wallet.

**Operações:**  
C1 wA1  
D1 wA1  
T1 User bc wA1 1 0 0  

**Estado inicial:**

| Wallet | Estado     | Saldo |
| ------ | ---------- | ------|
| wA1    | não existe | -     |
| BC     | existe     | 1     |

**Ordem de acontecimentos:**  
<pre>
Cliente                         Nó                               Sequencer
 |                              |                                    |
C1 ---------------------------->|                                    |
D1 ---------------------------->|                                    |
 |                  delete não incrementa counter                    |
 |                     porque a wA1 não existe                       |
 |                              C1---------------------------------->| 
 |                              |                            (C1 fecha bloco)  
 |                              D1---------------------------------->|  
 |                              |<----------------------------------C1  
 |                              |                                    |
 |                           cria wA1                                |
T1----------------------------->|                                    |
 |                  como wA1 tem delete counter a 0                  |
 |                pode otimizar e executar localmente                |
 |                              |                                    |
 |                          executa T1                               |
 |                             T1----------------------------------->|
 |                              |                           (bloco de D1 fecha)
 |                              |<-----------------------------------D1
 |                          elimina wA1                              |
</pre>

**Resultado de execução:**  
Nó: C1, T1, D1  
| Wallet | Estado     | Saldo |
| ------ | ---------- | ------|
| wA1    | existe     | 1     |
| BC     | existe     | 0     |

Sequencer: C1, D1, T1  
| Wallet | Estado     | Saldo |
| ------ | ---------- | ------|
| wA1    | não existe | -     |
| BC     | existe     | 1     |


**Explicação**  
Como a wallet wA1 ainda não existia quando D1 chegou não foi possível guardar o pending delete, resultando na otimização de T1 quando esta chega e deixando o estado do Nó inconsitente com o Sequencer.  

**Solução:**  
Usar uma estrutura separada para guardar a informação pendente de cada carteira, mesmo que a carteira ainda não exista.  