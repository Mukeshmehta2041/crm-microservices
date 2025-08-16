package com.crm.platform.common.graphql;

import graphql.GraphQL;
import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.AsyncSerialExecutionStrategy;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
// import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation;
import graphql.execution.instrumentation.tracing.TracingInstrumentation;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
// import org.dataloader.DataLoaderRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * GraphQL configuration for federated schema support
 */
@Configuration
public class GraphQLConfig {

  @Bean
  public GraphQL graphQL(GraphQLSchema schema) {
    return GraphQL.newGraphQL(schema)
        .queryExecutionStrategy(new AsyncExecutionStrategy())
        .mutationExecutionStrategy(new AsyncSerialExecutionStrategy())
        .instrumentation(createInstrumentation())
        .build();
  }

  @Bean
  public GraphQLSchema graphQLSchema(RuntimeWiring runtimeWiring) throws IOException {
    TypeDefinitionRegistry typeRegistry = new TypeDefinitionRegistry();

    // Load schema files
    List<String> schemaFiles = Arrays.asList(
        "graphql/common.graphqls",
        "graphql/contacts.graphqls",
        "graphql/deals.graphqls",
        "graphql/leads.graphqls",
        "graphql/accounts.graphqls",
        "graphql/activities.graphqls",
        "graphql/analytics.graphqls",
        "graphql/workflow.graphqls");

    SchemaParser schemaParser = new SchemaParser();

    for (String schemaFile : schemaFiles) {
      try {
        Resource resource = new ClassPathResource(schemaFile);
        if (resource.exists()) {
          String schemaContent = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
          TypeDefinitionRegistry registry = schemaParser.parse(schemaContent);
          typeRegistry.merge(registry);
        }
      } catch (IOException e) {
        // Log warning but continue - some services might not have all schema files
        System.out.println("Warning: Could not load schema file: " + schemaFile);
      }
    }

    SchemaGenerator schemaGenerator = new SchemaGenerator();
    return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
  }

  @Bean
  public RuntimeWiring runtimeWiring(List<GraphQLResolver> resolvers) {
    RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring();

    // Register all resolvers
    for (GraphQLResolver resolver : resolvers) {
      resolver.configureRuntimeWiring(builder);
    }

    // Add scalar types
    builder.scalar(GraphQLScalars.DateTime)
        .scalar(GraphQLScalars.UUID)
        .scalar(GraphQLScalars.JSON)
        .scalar(GraphQLScalars.BigDecimal);

    return builder.build();
  }

  // DataLoaderRegistry is commented out due to missing dependency
  // @Bean
  // public DataLoaderRegistry dataLoaderRegistry() {
  // return new DataLoaderRegistry();
  // }

  private Instrumentation createInstrumentation() {
    return new ChainedInstrumentation(Arrays.asList(
        new TracingInstrumentation(),
        // new DataLoaderDispatcherInstrumentation(),
        new GraphQLSecurityInstrumentation()));
  }
}