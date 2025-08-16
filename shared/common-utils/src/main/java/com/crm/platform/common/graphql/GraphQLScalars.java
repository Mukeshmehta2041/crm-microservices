package com.crm.platform.common.graphql;

import graphql.language.StringValue;
import graphql.schema.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

/**
 * Custom GraphQL scalar types
 */
public class GraphQLScalars {
    
    public static final GraphQLScalarType DateTime = GraphQLScalarType.newScalar()
            .name("DateTime")
            .description("DateTime scalar type")
            .coercing(new Coercing<Instant, String>() {
                @Override
                public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult instanceof Instant) {
                        return DateTimeFormatter.ISO_INSTANT.format((Instant) dataFetcherResult);
                    }
                    throw new CoercingSerializeException("Expected an Instant object.");
                }
                
                @Override
                public Instant parseValue(Object input) throws CoercingParseValueException {
                    if (input instanceof String) {
                        try {
                            return Instant.parse((String) input);
                        } catch (DateTimeParseException e) {
                            throw new CoercingParseValueException("Invalid DateTime format", e);
                        }
                    }
                    throw new CoercingParseValueException("Expected a String");
                }
                
                @Override
                public Instant parseLiteral(Object input) throws CoercingParseLiteralException {
                    if (input instanceof StringValue) {
                        try {
                            return Instant.parse(((StringValue) input).getValue());
                        } catch (DateTimeParseException e) {
                            throw new CoercingParseLiteralException("Invalid DateTime format", e);
                        }
                    }
                    throw new CoercingParseLiteralException("Expected a StringValue");
                }
            })
            .build();
    
    public static final GraphQLScalarType UUID = GraphQLScalarType.newScalar()
            .name("UUID")
            .description("UUID scalar type")
            .coercing(new Coercing<java.util.UUID, String>() {
                @Override
                public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult instanceof java.util.UUID) {
                        return dataFetcherResult.toString();
                    }
                    throw new CoercingSerializeException("Expected a UUID object.");
                }
                
                @Override
                public java.util.UUID parseValue(Object input) throws CoercingParseValueException {
                    if (input instanceof String) {
                        try {
                            return java.util.UUID.fromString((String) input);
                        } catch (IllegalArgumentException e) {
                            throw new CoercingParseValueException("Invalid UUID format", e);
                        }
                    }
                    throw new CoercingParseValueException("Expected a String");
                }
                
                @Override
                public java.util.UUID parseLiteral(Object input) throws CoercingParseLiteralException {
                    if (input instanceof StringValue) {
                        try {
                            return java.util.UUID.fromString(((StringValue) input).getValue());
                        } catch (IllegalArgumentException e) {
                            throw new CoercingParseLiteralException("Invalid UUID format", e);
                        }
                    }
                    throw new CoercingParseLiteralException("Expected a StringValue");
                }
            })
            .build();
    
    public static final GraphQLScalarType JSON = GraphQLScalarType.newScalar()
            .name("JSON")
            .description("JSON scalar type")
            .coercing(new Coercing<Object, Object>() {
                @Override
                public Object serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    return dataFetcherResult;
                }
                
                @Override
                public Object parseValue(Object input) throws CoercingParseValueException {
                    return input;
                }
                
                @Override
                public Object parseLiteral(Object input) throws CoercingParseLiteralException {
                    return input;
                }
            })
            .build();
    
    public static final GraphQLScalarType BigDecimal = GraphQLScalarType.newScalar()
            .name("BigDecimal")
            .description("BigDecimal scalar type")
            .coercing(new Coercing<java.math.BigDecimal, String>() {
                @Override
                public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult instanceof java.math.BigDecimal) {
                        return dataFetcherResult.toString();
                    }
                    if (dataFetcherResult instanceof Number) {
                        return new java.math.BigDecimal(dataFetcherResult.toString()).toString();
                    }
                    throw new CoercingSerializeException("Expected a BigDecimal or Number object.");
                }
                
                @Override
                public java.math.BigDecimal parseValue(Object input) throws CoercingParseValueException {
                    if (input instanceof String) {
                        try {
                            return new java.math.BigDecimal((String) input);
                        } catch (NumberFormatException e) {
                            throw new CoercingParseValueException("Invalid BigDecimal format", e);
                        }
                    }
                    if (input instanceof Number) {
                        return new java.math.BigDecimal(input.toString());
                    }
                    throw new CoercingParseValueException("Expected a String or Number");
                }
                
                @Override
                public java.math.BigDecimal parseLiteral(Object input) throws CoercingParseLiteralException {
                    if (input instanceof StringValue) {
                        try {
                            return new java.math.BigDecimal(((StringValue) input).getValue());
                        } catch (NumberFormatException e) {
                            throw new CoercingParseLiteralException("Invalid BigDecimal format", e);
                        }
                    }
                    throw new CoercingParseLiteralException("Expected a StringValue");
                }
            })
            .build();
}