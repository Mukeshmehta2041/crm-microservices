package com.crm.platform.gateway.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Basic tests for AuthenticationGatewayFilterFactory
 */
class AuthenticationGatewayFilterFactoryTest {

    @Test
    void shouldCreateFilterFactory() {
        AuthenticationGatewayFilterFactory factory = new AuthenticationGatewayFilterFactory();
        assertNotNull(factory);
    }
}