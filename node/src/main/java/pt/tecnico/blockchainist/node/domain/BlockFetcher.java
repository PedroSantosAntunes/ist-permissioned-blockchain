package pt.tecnico.blockchainist.node.domain;

public class BlockFetcher extends Thread {
    private final NodeState state;

    public BlockFetcher (NodeState state) {
        this.state = state;
    }

    @Override
    public void run() {
        while(!isInterrupted()) {
            state.getBlock();
        }
    }
}