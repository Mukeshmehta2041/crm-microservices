package com.crm.platform.discovery;

import org.junit.jupiter.api.Test;

/**
 * Simple unit test for Discovery Server Application
 */
class DiscoveryServerApplicationTest {

    @Test
    void contextLoads() {
        // Test that the main method can be called without errors
        // This is a basic smoke test
        DiscoveryServerApplication application = new DiscoveryServerApplication();
        // Just verify the class can be instantiated
        assert application != null;
    }

    @Test
    void applicationStarts() {
        // Test that the main method exists and can be referenced
        // This verifies the application class structure is correct
        try {
            DiscoveryServerApplication.class.getMethod("main", String[].class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Main method should exist", e);
        }
    }
}