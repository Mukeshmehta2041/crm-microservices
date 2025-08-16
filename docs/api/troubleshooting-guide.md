# API Troubleshooting Guide

## Common Issues and Solutions

### Authentication Issues

#### 1. Invalid Credentials Error

**Error Response:**
```json
{
  "success": false,
  "errors": [
    {
      "code": "INVALID_CREDENTIALS",
      "message": "Invalid username or password"
    }
  ]
}
```

**Possible Causes & Solutions:**
- **Incorrect username/password**: Verify credentials are correct
- **Account locked**: Check if account is temporarily locked due to failed attempts
- **Email not verified**: Ensure email address is verified
- **Tenant context missing**: Include `X-Tenant-ID` header or use tenant subdomain
- **Account disabled**: Contact administrator to check account status

**Debugging Steps:**
1. Verify credentials with a known working account
2. Check account status via admin panel
3. Ensure tenant ID is correct and active
4. Review recent security audit logs

#### 2. Token Expired Error

**Error Response:**
```json
{
  "success": false,
  "errors": [
    {
      "code": "TOKEN_EXPIRED",
      "message": "JWT token has expired"
    }
  ]
}
```

**Solutions:**
- Use refresh token to obtain new access token
- Re-authenticate if refresh token is also expired
- Implement automatic token refresh in your client

**Example Token Refresh:**
```bash
curl -X POST https://api.crmplatform.com/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "your-refresh-token"}'
```

#### 3. MFA Required Error

**Error Response:**
```json
{
  "success": false,
  "mfaRequired": true,
  "mfaToken": "mfa-challenge-token",
  "message": "MFA verification required"
}
```

**Solutions:**
- Complete MFA verification using the provided MFA token
- Use backup codes if TOTP device is unavailable
- Contact administrator if MFA device is lost

**MFA Verification:**
```bash
curl -X POST https://api.crmplatform.com/api/v1/auth/mfa/verify \
  -H "Content-Type: application/json" \
  -d '{
    "mfaToken": "mfa-challenge-token",
    "code": "123456"
  }'
```

### Authorization Issues

#### 1. Insufficient Permissions

**Error Response:**
```json
{
  "success": false,
  "errors": [
    {
      "code": "INSUFFICIENT_PERMISSIONS",
      "message": "User lacks required permissions for this operation"
    }
  ]
}
```

**Solutions:**
- Check user roles and permissions
- Contact administrator to grant required permissions
- Verify tenant context is correct
- Ensure user is active and not suspended

#### 2. Tenant Access Denied

**Error Response:**
```json
{
  "success": false,
  "errors": [
    {
      "code": "TENANT_ACCESS_DENIED",
      "message": "Access to tenant is denied"
    }
  ]
}
```

**Solutions:**
- Verify tenant ID is correct
- Ensure tenant is active and not suspended
- Check if user belongs to the specified tenant
- Verify tenant context in JWT token

### Rate Limiting Issues

#### 1. Rate Limit Exceeded

**Error Response:**
```json
{
  "success": false,
  "errors": [
    {
      "code": "RATE_LIMIT_EXCEEDED",
      "message": "Rate limit exceeded for this endpoint"
    }
  ]
}
```

**Rate Limit Headers:**
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1640995200
```

**Solutions:**
- Wait for rate limit window to reset
- Implement exponential backoff in your client
- Reduce request frequency
- Contact support for rate limit increases

**Exponential Backoff Example (JavaScript):**
```javascript
async function makeRequestWithBackoff(url, options, maxRetries = 3) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      const response = await fetch(url, options);
      if (response.status === 429) {
        const retryAfter = response.headers.get('Retry-After') || Math.pow(2, i);
        await new Promise(resolve => setTimeout(resolve, retryAfter * 1000));
        continue;
      }
      return response;
    } catch (error) {
      if (i === maxRetries - 1) throw error;
    }
  }
}
```

#### 2. IP Blocked

**Error Response:**
```json
{
  "success": false,
  "errors": [
    {
      "code": "IP_BLOCKED",
      "message": "IP address is temporarily blocked"
    }
  ]
}
```

**Solutions:**
- Wait for automatic unblock (typically 1 hour)
- Contact support to manually unblock IP
- Review and fix any automated scripts causing excessive requests
- Implement proper rate limiting in your application

### Validation Issues

#### 1. Required Field Missing

**Error Response:**
```json
{
  "success": false,
  "errors": [
    {
      "code": "REQUIRED_FIELD",
      "message": "Required field is missing",
      "field": "email"
    }
  ]
}
```

**Solutions:**
- Check API documentation for required fields
- Ensure all required fields are included in request
- Verify field names match exactly (case-sensitive)

#### 2. Invalid Format

**Error Response:**
```json
{
  "success": false,
  "errors": [
    {
      "code": "INVALID_FORMAT",
      "message": "Field format is invalid",
      "field": "email",
      "constraint": "EMAIL_FORMAT"
    }
  ]
}
```

**Solutions:**
- Validate input format before sending request
- Check API documentation for format requirements
- Use proper data types (string, number, boolean)

### OAuth2 Issues

#### 1. Invalid Client

**Error Response:**
```json
{
  "error": "invalid_client",
  "error_description": "Client authentication failed"
}
```

**Solutions:**
- Verify client ID and secret are correct
- Ensure client is registered and active
- Check client authentication method
- Verify redirect URI matches registered URI

#### 2. Invalid Grant

**Error Response:**
```json
{
  "error": "invalid_grant",
  "error_description": "Authorization code is invalid or expired"
}
```

**Solutions:**
- Ensure authorization code is used only once
- Check code hasn't expired (typically 10 minutes)
- Verify redirect URI matches authorization request
- Ensure code exchange happens quickly after authorization

### Session Issues

#### 1. Session Not Found

**Error Response:**
```json
{
  "success": false,
  "errors": [
    {
      "code": "SESSION_NOT_FOUND",
      "message": "Session not found or expired"
    }
  ]
}
```

**Solutions:**
- Re-authenticate to create new session
- Check if session was terminated by another request
- Verify session ID is correct
- Ensure session hasn't expired

#### 2. Concurrent Session Limit

**Error Response:**
```json
{
  "success": false,
  "errors": [
    {
      "code": "SESSION_LIMIT_EXCEEDED",
      "message": "Maximum concurrent sessions exceeded"
    }
  ]
}
```

**Solutions:**
- Terminate unused sessions
- Increase session limit (contact administrator)
- Implement proper session cleanup in your application

## Debugging Tools

### 1. Request/Response Logging

Enable detailed logging to debug issues:

```javascript
// JavaScript example
const client = new AuthClient({
  baseUrl: 'https://api.crmplatform.com',
  debug: true, // Enable debug logging
  onRequest: (config) => console.log('Request:', config),
  onResponse: (response) => console.log('Response:', response),
  onError: (error) => console.error('Error:', error)
});
```

### 2. JWT Token Inspection

Decode JWT tokens to inspect claims:

```bash
# Using jwt-cli (install with: npm install -g jwt-cli)
jwt decode eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

Online tool: https://jwt.io

### 3. API Testing with curl

Test endpoints directly with curl:

```bash
# Test with verbose output
curl -v -X POST https://api.crmplatform.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: your-tenant-id" \
  -d '{"usernameOrEmail": "user@example.com", "password": "password"}'
```

### 4. Postman Collection

Use the provided Postman collection for testing:
1. Import collection from `/api/v1/auth/docs/postman/complete.json`
2. Set up environment variables
3. Enable request/response logging
4. Use collection runner for automated testing

## Performance Issues

### 1. Slow Response Times

**Symptoms:**
- API requests taking longer than expected
- Timeouts occurring

**Solutions:**
- Check API status page for known issues
- Implement request timeout handling
- Use connection pooling for multiple requests
- Consider caching frequently accessed data

### 2. High Memory Usage

**Symptoms:**
- Application consuming excessive memory
- Out of memory errors

**Solutions:**
- Implement proper token cleanup
- Avoid storing large responses in memory
- Use streaming for large data transfers
- Monitor and limit concurrent requests

## Error Response Reference

### HTTP Status Codes

| Status Code | Meaning | Common Causes |
|-------------|---------|---------------|
| 400 | Bad Request | Invalid request format, missing required fields |
| 401 | Unauthorized | Invalid or missing authentication |
| 403 | Forbidden | Insufficient permissions, tenant access denied |
| 404 | Not Found | Resource doesn't exist, invalid endpoint |
| 409 | Conflict | Duplicate resource, constraint violation |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server-side error |
| 503 | Service Unavailable | Service temporarily down |

### Error Code Categories

| Category | Prefix | Examples |
|----------|--------|----------|
| Authentication | AUTH_ | AUTH_INVALID_CREDENTIALS, AUTH_TOKEN_EXPIRED |
| Authorization | AUTHZ_ | AUTHZ_INSUFFICIENT_PERMISSIONS, AUTHZ_ACCESS_DENIED |
| Validation | VALID_ | VALID_REQUIRED_FIELD, VALID_INVALID_FORMAT |
| Rate Limiting | RATE_ | RATE_LIMIT_EXCEEDED, RATE_IP_BLOCKED |
| System | SYS_ | SYS_INTERNAL_ERROR, SYS_SERVICE_UNAVAILABLE |

## Getting Help

### 1. Check Status Page
Visit https://status.crmplatform.com for service status and known issues.

### 2. Review Documentation
- API Documentation: https://docs.crmplatform.com
- Authentication Guide: https://docs.crmplatform.com/auth
- SDK Documentation: https://docs.crmplatform.com/sdks

### 3. Contact Support
- Email: support@crmplatform.com
- Include request ID from error response
- Provide relevant logs and error messages
- Describe steps to reproduce the issue

### 4. Community Resources
- GitHub Issues: https://github.com/crmplatform/api/issues
- Stack Overflow: Tag questions with `crmplatform-api`
- Developer Forum: https://forum.crmplatform.com

## Best Practices for Troubleshooting

1. **Always check HTTP status codes first**
2. **Read error messages carefully**
3. **Use request IDs for support requests**
4. **Enable debug logging during development**
5. **Test with minimal examples first**
6. **Check API documentation for recent changes**
7. **Verify environment configuration**
8. **Use proper error handling in your code**
9. **Monitor rate limits and implement backoff**
10. **Keep SDKs and libraries up to date**