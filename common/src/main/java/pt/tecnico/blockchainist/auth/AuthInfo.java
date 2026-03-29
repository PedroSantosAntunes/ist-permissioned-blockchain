package pt.tecnico.blockchainist.auth;

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

    private AuthInfo() {
    }

    public static String getOrganization(String userId) {
        return USER_TO_ORG.get(userId);
    }

    public static boolean userExists(String userId) {
        return USER_TO_ORG.containsKey(userId);
    }
}
