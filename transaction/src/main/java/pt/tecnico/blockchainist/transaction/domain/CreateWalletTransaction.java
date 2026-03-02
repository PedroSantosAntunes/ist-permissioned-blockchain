package pt.tecnico.blockchainist.transaction.domain;

import pt.tecnico.blockchainist.contract.*;


public class CreateWalletTransaction extends TransactionRecord {

    private String userId;
    private String walletId;


    public CreateWalletTransaction(int sequence_number, String userId, String walletId){
        super(sequence_number);
        this.userId = userId;
        this.walletId = walletId;
    }

    public String getUserId(){
        return this.userId;
    }

    public String getWalletId(){
        return this.walletId;
    }

    public String toString() {
        return "user: " + this.userId + " wallet: " + this.walletId;
    }


    @Override
    public Transaction recordToTransaction(){
        Transaction transaction = Transaction.newBuilder()
                .setCreateWallet(
                    CreateWalletRequest.newBuilder()
                        .setUserId(this.userId)
                        .setWalletId(this.walletId)
                        .build()
                ).build();
        return transaction;
    }

    @Override
    public void accept(TransactionVisitor visitor) {
        visitor.execute(this);
    }
}

