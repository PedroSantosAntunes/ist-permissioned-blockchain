package pt.tecnico.blockchainist.node.domain;

// TODO: ISTO FOI SÓ COPY PASTE DO QUE ESTAVA NA WALLET.
public class PendingWallet {

    private int pendingDeleteCounter;
    private long pendingDeficitAmount;

    public PendingWallet(){

    }

    // Delete counter methods
    public boolean isDeleting() {
        return pendingDeleteCounter > 0;
    }

    public void incrementDeleting() {
        pendingDeleteCounter++;
    }

    public void decrementDeleting() {
        pendingDeleteCounter--;
    }

    // Transfer counter methods
    public boolean hasSpendableBalance(long value) {
        return value <= balance - pendingDeficitAmount;
    }

    public void incrementTransfer(Long amount) {
        pendingDeficitAmount += amount;
    }

    public void decrementTransfer(Long amount) {
        pendingDeficitAmount -= amount;
    }

    @Override
    public String toString() {
        return "Pengin Wallet: " + "\n";
    }
}
