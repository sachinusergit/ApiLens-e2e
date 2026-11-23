package com.apilens.qa.utils;

/**
 * Thread-local auth state so parallel test classes never share a token.
 * Every test class registers its own user in BaseParallelApiTest.
 */
public final class ThreadContext {

    private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> ORG_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> EMAIL = new ThreadLocal<>();

    private ThreadContext() {}

    public static void setAuth(String token, String userId, String orgId, String email) {
        TOKEN.set(token);
        USER_ID.set(userId);
        ORG_ID.set(orgId);
        EMAIL.set(email);
    }

    public static String getToken() { return TOKEN.get(); }
    public static String getUserId() { return USER_ID.get(); }
    public static String getOrgId() { return ORG_ID.get(); }
    public static String getEmail() { return EMAIL.get(); }

    public static void clear() {
        TOKEN.remove();
        USER_ID.remove();
        ORG_ID.remove();
        EMAIL.remove();
    }
}
