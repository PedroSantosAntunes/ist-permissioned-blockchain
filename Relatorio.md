# DOCUMENTO QUE ACOMPANHA A 3ª ENTREGA DO PROJETO DE SD

1) Quais dos seguintes requisitos foram corretamente resolvidos?
Abaixo de cada requisito, respondam: "Sim" ou "Não" ou "Sim mas com erros: [enumerar sucintamente os erros]"

- Suporta a execução/resposta a pedidos de transferência antes da transação correspondente ser entregue pelo sequenciador [C.1].
Sim
- As transações são assinadas digitalmente pelo utilizador que as invocou e as respetivas assinaturas são verificadas antes de cada transação ser executada nas réplicas [C.2].
Sim
- A blockchain gerada pelo sequenciador é assinada digitalmente e as assinaturas correspondentes são verificadas pelos nós que recebem cada bloco [C.2].
Sim

2) Descrevam sucintamente as principais alterações que o grupo aplicou a cada componente indicada abaixo (máx. 800 palavras)
Idealmente, componham uma lista de itens (cada um iniciado por um hífen, tal como a listagem dos requisitos apresentada acima).
Refiram ainda se houve alterações aos argumentos de linha de comando dos executáveis, quais foram e qual a sua justificação.

Aos .proto:
- ClientSignature e SignedTransaction, por forma a assinar as Transações feitas pelo Cliente;
- SequencerSignature e SignedDeliverBlockResponse, por forma a assinar os blocos provenientes do sequencer;

Ao programa do Cliente: 
- Impossibilidade de se conectar a vários nodes da mesma organização (passando-os por argumento no terminal), ou de conectar a uma organização inexistente, pois agora cada organização é pre-conhecida e tem apenas um node.
- Permite introduzir as organizações válidas em qualquer ordem no terminal, atribuindo um índice a cada nó de forma dinâmica.
- Mapa de userId para privateKey, de modo a assinar as transações de cada user, no ClientNodeService.

Ao programa do Nó:
- Map entre userId e PublicKeys, para conseguir, no NodeServiceImpl, validar a assinatura digital dos users que fizeram os pedidos a partir do cliente.
- Validação da assinatura digital do sequenciador, ao receber um bloco assinado pelo mesmo.
- Verificação se o User pertence à organização à qual está a fazer o pedido.
- Obtenção de locks de carteiras por ordem alfabética, por forma a evitar deadlocks.
- Adição do caso falha rápida de transferências apenas para o caso do user não pertencer à organização, pois apesar de existirem mais otimizações para o “fail fast” (como a inexistência da carteira) e cuja justificação está relacionada com a “assumption” do projeto, decidimos que a melhor abordagem seria enviar os pedidos ao Sequencer e deixar este decidir a ordem de execução dos pedidos, podendo este pedido acabar por executar com sucesso.
- Adição de pending wallets para manter informação das operações pending para cada wallet, mesmo que esta ainda não exista. Esta estrutura existe para prevenir o edge case descrito na Nota 2.
- Adição do caso de execução local da transferência, que usa as pending wallets para saber que transferências são otimizáveis.
- Contador de pending deletes devido a necessidade de saber quantos deletes estão a acontecer (e não apenas de ter uma flag pending deletes). Este edge case é referido na Nota 3.
- Mantemos o pendingDeficitAmount nas pending wallets para permitir o máximo de transferências otimizadas, mesmo que a wallet tenha transferências pendentes que removam saldo.
- Durante a execução de uma transferência otimizada, o broadcast de pedidos para as wallets desta transferência ficam bloqueados, o que garante que a ordem de execução otimizada não conflite com a ordem recebida pelo Sequencer.

Ao programa do Sequenciador:
- Assinatura dos Blocos para enviar ao Node quando a sua thread blockFetcher chama deliverBlock

Notas: 
- 1) O ficheiro AuthInfo é utilizado para saber que organizações existem e que users estão associados a cada organização.
- 2) Todos os pedidos não otimizáveis criam/atualizam as respetivas pending wallets. Isto acontece porque mesmo que uma wallet ainda não exista quando vai atualizar a informação pending, a wallet pode ser criada após esse pedido mas antes de ele retornar, significando que vai existir um período em que a wallet existe mas a informação pending não estaria guardada.
- 3) Se forem enviados vários deletes da mesma wallet para o sequencer, e o primeiro retornar e falhar por ainda ter saldo, este remove a flag. No entanto, ainda há deletes por chegar.
- 4) Para melhor compreensão dos edge cases abordados, adicionámos um documento extra (Edge_Cases.md) que demonstra, com casos práticos, cada edge case.

3) Na vossa solução para o requisito da C.1, em quais condições uma transferência **NÃO** é executada pelo nó (que recebeu o pedido respetivo) antes da transação ser enviada ao sequenciador? (máx. 100 palavras)
Uma transferência não é otimizada se:
 - Uma das carteiras não pertence à organização do Node.
 - Existem pedidos de delete para uma das carteiras que ainda não retornaram do sequencer.
 - A quantidade de dinheiro da src wallet menos o défice é insuficiente para completar a transferência.
Nota: o défice é o total de dinheiro que foi tentado remover da src wallet através de pedidos de transferência não otimizados que ainda não retornaram do Sequencer. Isto é, se todos estes pedidos forem executados, a src wallet ficará com, no mínimo, (balance-deficit).

4) Como foi frisado na primeira aula teórica, não é aceitável o uso de GenAI/Code Copilots para gerar código usado diretamente no projeto.
Tendo isto em conta, respondam brevemente às seguintes 3 perguntas:

i) Os membros do grupo conseguem explicar o código submetido se tal for perguntado na discussão do projeto (feita sem recurso a AI)?
Sim
ii) Os membros do grupo conseguem defender todas as decisões de desenho e implementação se tal for perguntado na discussão do projeto (feita sem recurso a AI)?
Sim
iii) Usaram alguma(s) ferramenta(s) de AI? Se sim, para que uso?
Melhorar nomes de funções e variáveis.
Auxílio à interpretação da documentação das bibliotecas Java utilizadas.