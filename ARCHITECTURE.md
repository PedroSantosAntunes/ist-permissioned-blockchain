# This file is intended to be an explanation of the architecture of the project.

## gRPC and Domain split
### Client > Node
> CommandProcessor ------ (individual args) ---------> ClientNodeService  
> ClientNodeService ----- (gRPC Request)   ----------> NodeSServiceImpl  
> NodeServiceImpl   ----- (individual args) ---------> NodeState  


### Node > Sequencer
> NodeState ------------- (individual args) ---------> NodeSequencerService  
> NodeSequencerService -- (gRPC Request) ------------> SequencerServiceImpl  
> SequencerServiceImpl -- (seq_num or Record) -------> SequencerState  

### Sequencer > Node
> SequencerState -------- (seq_num or Record) -------> SequencerServiceImpl  
> SequencerServiceImpl -- (gRPC Response) -----------> NodeSequencerService  
> NodeSequencerService -- (seq_num or Record) -------> NodeState  


### Node > Client
> NodeState ------------- (InternalResponseStatus) --> NodeServiceImpl  
> NodeServiceImpl ------- (gRPC Response)------------> ClientNodeService  
> ClientNodeService ----- (response fields) ---------> CommandProcessor  

---

## Multihreaded
gRPC made our node receive multiple client request across multiple threads.  
Since there are shared objects across node threads, its necessary to protect them.  

We didn't make all our node methods synchronized because that defeats the purpose of concurrency on our blockchain.  
Aditionally, locking gRPC method calls can cause issues due to the possibility of long response times or other comunication errors.

All concurrency issues are explained and handled across these NodeState functions:
```java
public long readBalance(String walletId)

public ArrayList<TransactionRecord> getBlockchainState()

public InternalResponseStatus pullMissingTransactions (int target)
```