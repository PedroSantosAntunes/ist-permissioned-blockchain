package pt.tecnico.blockchainist.grpc;

import pt.tecnico.blockchainist.record.*;
import pt.tecnico.blockchainist.contract.*;

public abstract class RecordToTransaction {

    public static Transaction recordToTransaction(TransactionRecord record){
        switch (record.getType()) {
            case CREATE_WALLET:
                CreateWalletRecord createRecord = (CreateWalletRecord) record;
                return recordToTransaction(createRecord);
                
            case DELETE_WALLET:
                DeleteWalletRecord deleteRecord = (DeleteWalletRecord) record;
                return recordToTransaction(deleteRecord);
                
            case TRANSFER:
                TransferRecord transferRecord = (TransferRecord) record;
                return recordToTransaction(transferRecord);
            default:
                return null;
        }
    }

    private static Transaction recordToTransaction(CreateWalletRecord record){
        return Transaction.newBuilder()
                .setCreateWallet(
                    CreateWalletRequest.newBuilder()
                        .setUuid(record.getId())
                        .setUserId(record.getUserId())
                        .setWalletId(record.getWalletId())
                        .build()
                ).build();
    }

    private static Transaction recordToTransaction(DeleteWalletRecord record){
        return Transaction.newBuilder()
                .setDeleteWallet(
                    DeleteWalletRequest.newBuilder()
                        .setUuid(record.getId())
                        .setUserId(record.getUserId())
                        .setWalletId(record.getWalletId())
                        .build()
                ).build();
    }

    private static Transaction recordToTransaction(TransferRecord record){
        return Transaction.newBuilder()
                .setTransfer(
                    TransferRequest.newBuilder()
                        .setUuid(record.getId())
                        .setSrcUserId(record.getSrcUserId())
                        .setSrcWalletId(record.getSrcWalletId())
                        .setDstWalletId(record.getDstWalletId())
                        .setValue(record.getAmount())
                        .build()
                ).build();
    }

}