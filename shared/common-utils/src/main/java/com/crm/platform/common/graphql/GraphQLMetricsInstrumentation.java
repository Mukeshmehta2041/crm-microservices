package com.crm.platform.common.graphql;

import graphql.ExecutionResult;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * GraphQL instrumentation for metrics collection
 */
@Component
public class GraphQLMetricsInstrumentation extends SimpleInstrumentation {

  private final MeterRegistry meterRegistry;
  private final Timer queryTimer;
  private final Counter queryCounter;
  private final Counter errorCounter;

  @Autowired
  public GraphQLMetricsInstrumentation(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.queryTimer = Timer.builder("graphql.query.duration")
        .description("GraphQL query execution time")
        .register(meterRegistry);
    this.queryCounter = Counter.builder("graphql.query.count")
        .description("GraphQL query count")
        .register(meterRegistry);
    this.errorCounter = Counter.builder("graphql.query.errors")
        .description("GraphQL query errors")
        .register(meterRegistry);
  }

  @Override
  public InstrumentationContext<ExecutionResult> beginExecution(InstrumentationExecutionParameters parameters) {
    Timer.Sample sample = Timer.start(meterRegistry);
    queryCounter.increment();

    return new InstrumentationContext<ExecutionResult>() {
      @Override
      public void onCompleted(ExecutionResult result, Throwable t) {
        sample.stop(queryTimer);

        if (result.getErrors() != null && !result.getErrors().isEmpty()) {
          errorCounter.increment();
        }
      }

      @Override
      public void onDispatched(CompletableFuture<ExecutionResult> result) {
        // No-op
      }
    };
  }

  @Override
  public DataFetcher<?> instrumentDataFetcher(DataFetcher<?> dataFetcher,
      InstrumentationFieldFetchParameters parameters) {
    String fieldName = parameters.getField().getName();

    Timer fieldTimer = Timer.builder("graphql.field.duration")
        .description("GraphQL field fetch time")
        .tag("field", fieldName)
        .register(meterRegistry);

    return environment -> {
      Timer.Sample sample = Timer.start(meterRegistry);
      try {
        Object result = dataFetcher.get(environment);
        sample.stop(fieldTimer);
        return result;
      } catch (Exception e) {
        sample.stop(fieldTimer);
        Counter.builder("graphql.field.errors")
            .description("GraphQL field fetch errors")
            .tag("field", fieldName)
            .register(meterRegistry)
            .increment();
        throw e;
      }
    };
  }
}