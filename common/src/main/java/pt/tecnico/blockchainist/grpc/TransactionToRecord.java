package pt.tecnico.blockchainist.grpc;

import pt.tecnico.blockchainist.record.*;
import pt.tecnico.blockchainist.contract.*;


public abstract class TransactionToRecord {

    public static TransactionRecord transactionToRecord(Transaction tx) {

        switch (tx.getOperationCase()) {

            case CREATE_WALLET:
                CreateWalletRequest create = tx.getCreateWallet();
                return new CreateWalletRecord(
                        create.getUserId(),
                        create.getWalletId()
                );

            case DELETE_WALLET:
                DeleteWalletRequest delete = tx.getDeleteWallet();
                return new DeleteWalletRecord(
                        delete.getUserId(),
                        delete.getWalletId()
                );

            case TRANSFER:
                TransferRequest transfer = tx.getTransfer();
                return new TransferRecord(
                        transfer.getSrcUserId(),
                        transfer.getSrcWalletId(),
                        transfer.getDstWalletId(),
                        transfer.getValue()
                );

            case OPERATION_NOT_SET:
            default:
                throw new IllegalArgumentException("Transaction operation not set");
        }
    }

}