# DOCUMENTO QUE ACOMPANHA A 2ª ENTREGA DO PROJETO DE SD

1) Quais dos seguintes requisitos foram corretamente resolvidos?
Abaixo de cada requisito, respondam: "Sim" ou "Não" ou "Sim mas com erros: [enumerar sucintamente os erros]"

- Estender o sistema para permitir nós replicados  
Sim
- Difusão baseada em blocos (e não transações individuais)  
Sim
- Variantes não-bloqueantes dos comandos  
Sim
- Funcionalidade de atrasar, no nó, a execução de cada pedido  
Sim
- Suporte ao lançamento de novos nós a qualquer ponto no tempo  
Sim
- Tolerância a falhas silenciosas dos nós  
Sim

2) Descrevam sucintamente as principais alterações que o grupo aplicou a cada componente indicada abaixo (máx. 800 palavras)
Idealmente, componham uma lista de itens (cada um iniciado por um hífen, tal como a listagem dos requisitos apresentada acima).
Refiram ainda se houve alterações aos argumentos de linha de comando dos executáveis, quais foram e qual a sua justificação.

Aos .proto:
- ReadBalanceRquest agora tem Block Number do último read que foi feito;
- ReadBalanceResponse agora tem Block Number do último block adicionado pelo nó no momento do read;
- UUID nas transações para identificar as transações e suas respostas devido à assincronicidade;
- Adicionado o Block que contém Transactions;
- Troca a estrutura DeliverTransaction por DeliverBlock;

Ao programa do Cliente:
- Suporta stubs assíncronos e respostas assíncronas (Client Async Response Observer). Respostas chamam as mesmas funções de print (synchronized) para garantir alguma consistência visual no terminal, apesar de não ser garantida máxima consistência;
- Tem uma lista de PendingRequests. Cada PendingRequest têm informação para voltar a pedir a mesma operação a outro node caso o node atual falhe / cliente desconfie que o node falhou (timeout do client);
- Suporta delay nas operações (através de metadados);
- Guarda numero do bloco mais recente recebido durante um ReadBalanceRequest para garantir Linearizability;
- TODO LEBLOCKCHAIN............................................................................................

Ao programa do Nó:
- Também tem PendingTransactions (mapaeia UUID de transação para um CompletableFuture de um InternalResponseStatus)
- A thread BlockFetcher, que pede blocos ao sequencer e executa-os, mete a resposta no CompletableFuture. Assim que isto acontece, já temos resposta ao pedido.
- Como metemos as transações que o blockFetcher executa na estrutura completedTransactions, conseguimos reiniciar o node em qualquer ponto e ainda assim as suas respostas serão as esperadas, pois este, uma vez reiniciado, fará fetch e executará todas as transações de blocos outra vez.
- DelayNodeInterceptor
- É necessário garantir que a partir do momento em que se lê um resultado, não podemos receber um resutado mais antigo (para linearizability). Para isso o temos um pendingBlock. O cliente inicialmente mandou o ultimo block que tinha lido. Se o node ainda nao tiver nesse block, esperamos até que este dê fetch desse bloco para returnar a resposta ao Read. Enviamos também o blockNumber que foi lido para o cliente atualizar o seu valor e nunca ler valores mais antigos que o que acabou de ler. 

Ao programa do Sequenciador:



1) Na vossa solução, as transações recebidas pelo sequenciador levam algum identificador?
Se sim, expliquem brevemente o formato e como é gerado o identificador (máx. 100 palavras)
Sim levam. O identificador é um UUID, gerado no cliente e passado nos pedidos. Este é necessario para 


4) Como foi frisado na primeira aula teórica, não é aceitável o uso de GenAI/Code Copilots para gerar código usado diretamente no projeto.
Tendo isto em conta, respondam brevemente às seguintes 3 perguntas:

i) Os membros do grupo conseguem explicar o código submetido se tal for perguntado na discussão do projeto (feita sem recurso a AI)?
Sim, conseguimos.
ii) Os membros do grupo conseguem defender todas as decisões de desenho e implementação se tal for perguntado na discussão do projeto (feita sem recurso a AI)?  
Sim, conseguimos.
iii) Usaram alguma(s) ferramenta(s) de AI? Se sim, para que uso?
Melhorar nomes de funções e variáveis.
Auxílio à documentação das bibliotecas java utilizadas, exemplo: concurrent.