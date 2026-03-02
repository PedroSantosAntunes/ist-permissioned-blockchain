package pt.tecnico.blockchainist.debug;

public class Debug {
    private Debug() {}

    public static final boolean ENABLED = "1".equals(System.getProperty("debug", "0"));

    public static void log (String msg) {
        if (ENABLED) System.err.println(msg);
    }
}
