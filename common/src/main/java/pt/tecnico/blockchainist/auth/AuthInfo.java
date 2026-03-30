package pt.tecnico.blockchainist.auth;

import java.util.ArrayList;
import java.util.Map;

public final class AuthInfo {
    private static final Map<String, String> USER_TO_ORG = Map.of(
        "BC", "OrgA",
        "Alice", "OrgA",
        "Bob", "OrgA",
        "Charlie", "OrgA",
        "David", "OrgB",
        "Emma", "OrgB",
        "Fred", "OrgB",
        "Ginger", "OrgC",
        "Henry", "OrgC",
        "Iris", "OrgC"
    );

    private static final Map<Integer, String> INDEX_TO_ORG = Map.of(
        0, "OrgA",
        1, "OrgB",
        2, "OrgC"
    );

    private AuthInfo() {
    }

    public static String getOrganization(String userId) {
        return USER_TO_ORG.get(userId);
    }

    public static boolean userExists(String userId) {
        return USER_TO_ORG.containsKey(userId);
    }

    public static String indexToOrganization(Integer index) {
        return INDEX_TO_ORG.get(index);
    }

    public static ArrayList<String> getAllUsers() {
        return new ArrayList<>(USER_TO_ORG.keySet());
    }
}
