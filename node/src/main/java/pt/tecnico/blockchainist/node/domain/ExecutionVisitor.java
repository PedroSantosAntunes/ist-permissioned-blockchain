package pt.tecnico.blockchainist.node.domain;

import java.util.Map;

import pt.tecnico.blockchainist.transaction.domain.CreateWalletTransaction;
import pt.tecnico.blockchainist.transaction.domain.DeleteWalletTransaction;
import pt.tecnico.blockchainist.transaction.domain.TransactionVisitor;
import pt.tecnico.blockchainist.transaction.domain.TransferTransaction;

public class ExecutionVisitor implements TransactionVisitor{
    private final Map<String, Wallet> wallets;

    public ExecutionVisitor(Map<String, Wallet> wallets) {
        this.wallets = wallets;
    }



    @Override
    public void execute(CreateWalletTransaction tx) {
        Wallet wallet = new Wallet(tx.getWalletId(), tx.getUserId(), 0L);
        wallets.put(tx.getWalletId(), wallet);
    }

    @Override
    public void execute(DeleteWalletTransaction tx) {
        wallets.remove(tx.getWalletId());
    }

    @Override
    public void execute(TransferTransaction tx) {
        Wallet srcWallet = wallets.get(tx.getScrWalletId());
        Wallet dstWallet = wallets.get(tx.getDstWalletId());

        long srcBalance =  srcWallet.getBalance();
        long dstBalance = dstWallet.getBalance();

        srcWallet.setBalance(srcBalance - tx.getValue());
        dstWallet.setBalance(dstBalance + tx.getValue());
    }


}
