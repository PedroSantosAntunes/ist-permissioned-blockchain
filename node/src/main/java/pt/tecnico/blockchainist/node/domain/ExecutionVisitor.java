package pt.tecnico.blockchainist.node.domain;

import java.util.Map;

import pt.tecnico.blockchainist.transaction.domain.CreateWalletTransaction;
import pt.tecnico.blockchainist.transaction.domain.DeleteWalletTransaction;
import pt.tecnico.blockchainist.transaction.domain.TransactionVisitor;
import pt.tecnico.blockchainist.transaction.domain.TransferTransaction;

public class ExecutionVisitor implements TransactionVisitor{
    private final Map<String, String> wallets;
    private final Map<String, Long> balances;

    public ExecutionVisitor(Map<String, String> wallets,
                            Map<String, Long> balances) {
        this.wallets = wallets;
        this.balances = balances;
    }



    @Override
    public void execute(CreateWalletTransaction tx) {
        wallets.put(tx.getWalletId(), tx.getUserId());
        balances.put(tx.getWalletId(), 0L);
    }

    @Override
    public void execute(DeleteWalletTransaction tx) {
        wallets.remove(tx.getWalletId());
        balances.remove(tx.getWalletId());
    }

    @Override
    public void execute(TransferTransaction tx) {
        long srcBalance = balances.get(tx.getScrWalletId());
        long dstBalance = balances.get(tx.getDstWalletId());
        balances.replace(tx.getScrWalletId(), srcBalance - tx.getValue());
        balances.replace(tx.getDstWalletId(), dstBalance + tx.getValue());
    }


}
