package com.crm.platform.common.graphql;

import graphql.schema.idl.RuntimeWiring;

/**
 * Interface for GraphQL resolvers to configure runtime wiring
 */
public interface GraphQLResolver {
    
    /**
     * Configure runtime wiring for this resolver
     * @param builder RuntimeWiring builder
     */
    void configureRuntimeWiring(RuntimeWiring.Builder builder);
}