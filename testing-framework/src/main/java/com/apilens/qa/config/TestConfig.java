package com.apilens.qa.config;

public final class TestConfig {

    private TestConfig() {}

    public static final String BASE_URL =
            System.getenv().getOrDefault("BASE_URL", "http://localhost:8080");
}
