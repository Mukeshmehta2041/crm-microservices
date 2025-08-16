package com.crm.platform.auth.exception;

/**
 * OAuth2 specific exception following RFC 6749 error codes
 */
public class OAuth2Exception extends RuntimeException {

    private final String error;
    private final String errorDescription;
    private final String errorUri;

    public OAuth2Exception(String error, String errorDescription) {
        super(errorDescription);
        this.error = error;
        this.errorDescription = errorDescription;
        this.errorUri = null;
    }

    public OAuth2Exception(String error, String errorDescription, String errorUri) {
        super(errorDescription);
        this.error = error;
        this.errorDescription = errorDescription;
        this.errorUri = errorUri;
    }

    public OAuth2Exception(String error, String errorDescription, Throwable cause) {
        super(errorDescription, cause);
        this.error = error;
        this.errorDescription = errorDescription;
        this.errorUri = null;
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public String getErrorUri() {
        return errorUri;
    }

    // Common OAuth2 error factory methods
    public static OAuth2Exception invalidRequest(String description) {
        return new OAuth2Exception("invalid_request", description);
    }

    public static OAuth2Exception invalidClient(String description) {
        return new OAuth2Exception("invalid_client", description);
    }

    public static OAuth2Exception invalidGrant(String description) {
        return new OAuth2Exception("invalid_grant", description);
    }

    public static OAuth2Exception unauthorizedClient(String description) {
        return new OAuth2Exception("unauthorized_client", description);
    }

    public static OAuth2Exception unsupportedGrantType(String description) {
        return new OAuth2Exception("unsupported_grant_type", description);
    }

    public static OAuth2Exception invalidScope(String description) {
        return new OAuth2Exception("invalid_scope", description);
    }

    public static OAuth2Exception accessDenied(String description) {
        return new OAuth2Exception("access_denied", description);
    }

    public static OAuth2Exception unsupportedResponseType(String description) {
        return new OAuth2Exception("unsupported_response_type", description);
    }

    public static OAuth2Exception serverError(String description) {
        return new OAuth2Exception("server_error", description);
    }

    public static OAuth2Exception temporarilyUnavailable(String description) {
        return new OAuth2Exception("temporarily_unavailable", description);
    }

    public static OAuth2Exception invalidToken(String description) {
        return new OAuth2Exception("invalid_token", description);
    }

    public static OAuth2Exception insufficientScope(String description) {
        return new OAuth2Exception("insufficient_scope", description);
    }
}