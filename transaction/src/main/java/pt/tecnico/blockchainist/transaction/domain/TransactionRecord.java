package pt.tecnico.blockchainist.transaction;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import pt.tecnico.blockchainist.contract.*;

public abstract class TransactionRecord {
    int sequence_number;

    public TransactionRecord(int sequence_number){
        this.sequence_number = sequence_number;
    }

    public int getSequenceNumber(){
        return this.sequence_number;
    }

    public static TransactionRecord transactionToRecord(Transaction tx, int global_transaction_counter){
        TransactionRecord transaction = null;


        switch (tx.getOperationCase()) {
            case CREATE_WALLET:
                CreateWalletRequest create = tx.getCreateWallet();
                transaction = new CreateWalletTransaction(global_transaction_counter, create.getUserId(), create.getWalletId());
                break;

            case DELETE_WALLET:
                DeleteWalletRequest delete = tx.getDeleteWallet();
                transaction = new DeleteWalletTransaction(global_transaction_counter, delete.getUserId(), delete.getWalletId());
                break;

            case TRANSFER:
                TransferRequest transfer = tx.getTransfer();
                transaction = new TransferTransaction(global_transaction_counter, transfer.getSrcUserId(), 
                                                        transfer.getSrcWalletId(), transfer.getDstWalletId(), transfer.getValue());
                break;

            // TODO when does this happen
            case OPERATION_NOT_SET:
                break;
        }
        return transaction;
    }

    public abstract Transaction recordToTransaction();
    public abstract void execute(Map<String, String> wallets, Map<String, Long> balances);

}
