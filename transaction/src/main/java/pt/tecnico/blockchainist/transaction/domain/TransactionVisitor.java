package pt.tecnico.blockchainist.transaction.domain;

public interface TransactionVisitor {
    void execute(CreateWalletTransaction tx);

    void execute(DeleteWalletTransaction tx);

    void execute(TransferTransaction tx);
}
