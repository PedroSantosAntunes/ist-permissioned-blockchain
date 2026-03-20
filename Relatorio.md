# DOCUMENTO QUE ACOMPANHA A 2ª ENTREGA DO PROJETO DE SD

1) Quais dos seguintes requisitos foram corretamente resolvidos?
Abaixo de cada requisito, respondam: "Sim" ou "Não" ou "Sim mas com erros: [enumerar sucintamente os erros]"

- Estender o sistema para permitir nós replicados  
**Sim**
- Difusão baseada em blocos (e não transações individuais)  
**Sim**
- Variantes não-bloqueantes dos comandos  
**Sim**
- Funcionalidade de atrasar, no nó, a execução de cada pedido  
**Sim**
- Suporte ao lançamento de novos nós a qualquer ponto no tempo  
**Sim**
- Tolerância a falhas silenciosas dos nós  
**Sim**

2) Descrevam sucintamente as principais alterações que o grupo aplicou a cada componente indicada abaixo (máx. 800 palavras)
Idealmente, componham uma lista de itens (cada um iniciado por um hífen, tal como a listagem dos requisitos apresentada acima).
Refiram ainda se houve alterações aos argumentos de linha de comando dos executáveis, quais foram e qual a sua justificação.

Aos .proto:
- **ReadBalanceRequest** agora tem **Block Number** do último read que foi feito;
- **ReadBalanceResponse** agora tem **Block Number** do último block adicionado pelo nó no momento do read;
- **UUID nas transações** para identificar as transações e suas respostas devido à **assincronicidade** e evitar duplicados;
- Adicionado o **Block** que contém Transactions;
- Troca a estrutura DeliverTransaction por **DeliverBlock**;

Ao programa do Cliente:
- Suporta **stubs e respostas assíncronas** (`ClientNodeService e Client Async Response Observer`). Respostas chamam as mesmas funções de print (`synchronized`) para garantir alguma consistência visual no terminal, apesar de não ser garantida máxima consistência;
- Tem uma lista de **PendingRequests**. Cada PendingRequest têm informação para **voltar a pedir a mesma operação a outro nó caso o nó atual falhe / cliente desconfie que o nó falhou** (timeout do client);
- Suporta **delay** nas operações (através de **metadados**);
- Guarda numero do bloco mais recente recebido durante um ReadBalanceRequest para ajudar a garantir **Linearizability**;
- Cria um UUID para cada pedido de transação para conseguir identificar a transação, suportar o recebimento de respostas assincronas e para evitar que a transação seja executada várias vezes;

Ao programa do Nó:
- Tem um **PendingTransactions** que mapeia UUID de transação para um **CompletableFuture de um InternalResponseStatus**;
- A thread **BlockFetcher**, que pede blocos ao sequencer e executa-os, mete a resposta no CompletableFuture. Assim que isto acontece, já temos resposta ao pedido;
- Como metemos as transações que o blockFetcher executa na estrutura **completedTransactions**, **conseguimos reiniciar o nó em qualquer ponto** e ainda assim as suas respostas serão as esperadas, pois este, uma vez reiniciado, fará fetch e executará todas as transações dos blocos outra vez;
- Tem um **DelayNodeInterceptor** para possibilitar pedidos com delay do cliente;
- É necessário garantir que a partir do momento em que se lê um resultado, não podemos receber um resultado mais antigo (**Linearizability**). Para isso, temos um **PendingBlocks**. O cliente inicialmente mandou o último block que tinha lido. Se o nó ainda não estiver nesse block, esperamos até que este dê fetch desse bloco para retornar a resposta ao Read. Enviamos também o blockNumber que foi lido para o cliente atualizar o seu valor e **nunca ler valores mais antigos que o que acabou de ler**;


- **Nota**: o leBlockchain é uma operação de debug com objetivo de retornar o estado atual do nó, logo não se aplica a verificação se o nó encontra-se mais atrasado que a última transação conhecida pelo cliente (Linearizability).

Ao programa do Sequenciador:
- Tem um timer que a cada X segundos cria um bloco, a não ser que o bloco ainda não contenha nenhuma transação;
- Recebe os Parâmetros **N (BlockSize) e T (Timeout)** no terminal ou utiliza os valores default estabelecidos no pom;
- **Deliver block admite paralelização** permitindo que vários nós obtenham blocos existentes na blockchain ao mesmo tempo;
- Impede a adição de transações repetidas para garantir que uma transação pedida por um cliente só seja adicionada uma vez;


1) Na vossa solução, as transações recebidas pelo sequenciador levam algum identificador?
Se sim, expliquem brevemente o formato e como é gerado o identificador (máx. 100 palavras)  
Sim, levam. O identificador é um UUID, gerado no cliente e passado nos pedidos. Este é necessário para prevenir a adição de transações repetidas no sequencer e para conseguir identificar a transação por forma a obter posteriormente a sua resposta.


1) Como foi frisado na primeira aula teórica, não é aceitável o uso de GenAI/Code Copilots para gerar código usado diretamente no projeto.
Tendo isto em conta, respondam brevemente às seguintes 3 perguntas:

i) Os membros do grupo conseguem explicar o código submetido se tal for perguntado na discussão do projeto (feita sem recurso a AI)?  
Sim, conseguimos.
ii) Os membros do grupo conseguem defender todas as decisões de desenho e implementação se tal for perguntado na discussão do projeto (feita sem recurso a AI)?
Sim, conseguimos.
iii) Usaram alguma(s) ferramenta(s) de AI? Se sim, para que uso?
Melhorar nomes de funções e variáveis.
Auxílio à interpretação da documentação das bibliotecas java utilizadas, exemplo: concurrent.