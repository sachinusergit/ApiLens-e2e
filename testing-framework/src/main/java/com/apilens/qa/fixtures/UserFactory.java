package com.apilens.qa.fixtures;

import java.util.UUID;

public final class UserFactory {

    private UserFactory() {}

    public static String uniqueEmail() {
        return "test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@example.com";
    }

    public static String uniqueName() {
        return "Test User " + UUID.randomUUID().toString().substring(0, 6);
    }

    public static String defaultPassword() {
        return "Test@1234";
    }
}
