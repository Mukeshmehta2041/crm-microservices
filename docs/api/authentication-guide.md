# Authentication Guide

## Overview

The CRM Platform Authentication Service provides multiple authentication methods to suit different use cases and security requirements.

## Authentication Methods

### 1. JWT Bearer Token Authentication

The primary authentication method for most API endpoints.

#### How it works:
1. Obtain a JWT token through login or OAuth2 flow
2. Include the token in the `Authorization` header
3. Token contains user identity, permissions, and tenant context

#### Example:
```bash
curl -X GET https://api.crmplatform.com/api/v1/auth/user/profile \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### Token Structure:
```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user-uuid",
    "tenant_id": "tenant-uuid",
    "roles": ["USER", "ADMIN"],
    "permissions": ["read:profile", "write:profile"],
    "exp": 1640995200,
    "iat": 1640991600
  }
}
```

### 2. OAuth2 Authentication

For third-party applications and integrations.

#### Supported Flows:
- **Authorization Code Flow** (recommended for web applications)
- **Client Credentials Flow** (for service-to-service authentication)

#### Authorization Code Flow:

**Step 1: Authorization Request**
```
GET https://api.crmplatform.com/api/v1/auth/oauth2/authorize?
  client_id=your-client-id&
  response_type=code&
  redirect_uri=https://yourapp.com/callback&
  scope=read write&
  state=random-state-string
```

**Step 2: Token Exchange**
```bash
curl -X POST https://api.crmplatform.com/api/v1/auth/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code" \
  -d "code=authorization-code" \
  -d "client_id=your-client-id" \
  -d "client_secret=your-client-secret" \
  -d "redirect_uri=https://yourapp.com/callback"
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "refresh-token-string",
  "scope": "read write"
}
```

#### Client Credentials Flow:

```bash
curl -X POST https://api.crmplatform.com/api/v1/auth/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=your-client-id" \
  -d "client_secret=your-client-secret" \
  -d "scope=api:read api:write"
```

### 3. API Key Authentication

For simple integrations and webhook endpoints.

```bash
curl -X GET https://api.crmplatform.com/api/v1/auth/webhooks/status \
  -H "X-API-Key: your-api-key"
```

## Multi-Factor Authentication (MFA)

### Setting up MFA

**1. Initiate MFA Setup:**
```bash
curl -X POST https://api.crmplatform.com/api/v1/auth/mfa/setup \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{"method": "TOTP"}'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "secret": "JBSWY3DPEHPK3PXP",
    "qrCode": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
    "backupCodes": ["123456", "789012", "345678", "901234", "567890"]
  }
}
```

**2. Verify MFA Setup:**
```bash
curl -X POST https://api.crmplatform.com/api/v1/auth/mfa/verify-setup \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{"code": "123456"}'
```

### Using MFA during Login

When MFA is enabled, the login flow changes:

**1. Initial Login (returns MFA challenge):**
```bash
curl -X POST https://api.crmplatform.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "user@example.com",
    "password": "password"
  }'
```

**Response (MFA Required):**
```json
{
  "success": false,
  "mfaRequired": true,
  "mfaToken": "mfa-challenge-token",
  "message": "MFA verification required"
}
```

**2. Complete MFA Verification:**
```bash
curl -X POST https://api.crmplatform.com/api/v1/auth/mfa/verify \
  -H "Content-Type: application/json" \
  -d '{
    "mfaToken": "mfa-challenge-token",
    "code": "123456"
  }'
```

**Success Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "refresh-token-string",
    "expiresIn": 3600,
    "user": { ... }
  }
}
```

## Token Refresh

Access tokens have limited lifetime. Use refresh tokens to get new access tokens:

```bash
curl -X POST https://api.crmplatform.com/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your-refresh-token"
  }'
```

## Session Management

### List Active Sessions

```bash
curl -X GET https://api.crmplatform.com/api/v1/auth/sessions \
  -H "Authorization: Bearer your-token"
```

### Terminate Specific Session

```bash
curl -X DELETE https://api.crmplatform.com/api/v1/auth/sessions/session-id \
  -H "Authorization: Bearer your-token"
```

### Terminate All Other Sessions

```bash
curl -X POST https://api.crmplatform.com/api/v1/auth/sessions/terminate-all \
  -H "Authorization: Bearer your-token"
```

## Tenant Context

Most operations require tenant context. Provide it via:

### 1. Header (Recommended)
```bash
curl -X GET https://api.crmplatform.com/api/v1/auth/user/profile \
  -H "Authorization: Bearer your-token" \
  -H "X-Tenant-ID: tenant-uuid"
```

### 2. Subdomain
```bash
curl -X GET https://tenant-subdomain.api.crmplatform.com/api/v1/auth/user/profile \
  -H "Authorization: Bearer your-token"
```

### 3. JWT Token Claims
The tenant ID can be embedded in the JWT token during authentication.

## Error Handling

### Common Authentication Errors

| Error Code | Description | Solution |
|------------|-------------|----------|
| `INVALID_CREDENTIALS` | Username/password incorrect | Verify credentials |
| `ACCOUNT_LOCKED` | Account temporarily locked | Wait or contact admin |
| `MFA_REQUIRED` | MFA verification needed | Complete MFA flow |
| `TOKEN_EXPIRED` | JWT token expired | Use refresh token |
| `INSUFFICIENT_PERMISSIONS` | Missing required permissions | Check user roles |

### Error Response Format

```json
{
  "success": false,
  "errors": [
    {
      "code": "INVALID_CREDENTIALS",
      "message": "Invalid username or password",
      "field": "password",
      "constraint": "AUTHENTICATION_FAILED"
    }
  ],
  "meta": {
    "timestamp": "2024-01-01T12:00:00Z",
    "requestId": "req-123"
  }
}
```

## Security Best Practices

### Token Storage
- Store tokens securely (encrypted storage, secure cookies)
- Never expose tokens in URLs or logs
- Implement token rotation

### Rate Limiting
- Implement exponential backoff for failed requests
- Monitor rate limit headers
- Cache responses when appropriate

### HTTPS Only
- Always use HTTPS in production
- Validate SSL certificates
- Use certificate pinning for mobile apps

### Session Security
- Implement session timeout
- Monitor for suspicious activity
- Use secure session storage

## SDK Examples

### JavaScript/Node.js

```javascript
import { AuthClient } from '@crmplatform/auth-sdk';

const client = new AuthClient({
  baseUrl: 'https://api.crmplatform.com',
  tenantId: 'your-tenant-id'
});

// Login with MFA handling
try {
  const result = await client.login('user@example.com', 'password');
  console.log('Login successful:', result.user);
} catch (error) {
  if (error.code === 'MFA_REQUIRED') {
    const mfaCode = prompt('Enter MFA code:');
    const result = await client.verifyMfa(error.mfaToken, mfaCode);
    console.log('MFA verification successful:', result.user);
  }
}

// Automatic token refresh
client.onTokenRefresh((newToken) => {
  console.log('Token refreshed:', newToken);
});
```

### Python

```python
from crmplatform_auth import AuthClient
from crmplatform_auth.exceptions import MfaRequiredError

client = AuthClient(
    base_url='https://api.crmplatform.com',
    tenant_id='your-tenant-id'
)

# Login with MFA handling
try:
    result = client.login('user@example.com', 'password')
    print(f"Login successful: {result.user}")
except MfaRequiredError as e:
    mfa_code = input("Enter MFA code: ")
    result = client.verify_mfa(e.mfa_token, mfa_code)
    print(f"MFA verification successful: {result.user}")

# Use authenticated client
profile = client.get_profile()
```

## Testing

Use the provided Postman collection for testing:
1. Import the collection from `/api/v1/auth/docs/postman/complete.json`
2. Set up environment variables
3. Run the authentication flow tests
4. Verify all endpoints work correctly

## Support

- **Documentation**: https://docs.crmplatform.com
- **API Status**: https://status.crmplatform.com
- **Support Email**: support@crmplatform.com
- **GitHub Issues**: https://github.com/crmplatform/api/issues