# CRM Platform Postman Collections

This directory contains comprehensive Postman collections for testing and interacting with the CRM Platform APIs.

## Collections Overview

### 1. Authentication Service Collection
**File**: `CRM-Platform-Authentication-Service.postman_collection.json`

Complete collection for the Authentication Service including:
- **Authentication**: Login, logout, registration, token refresh
- **Password Management**: Reset, change, policy retrieval
- **Multi-Factor Authentication**: Setup, verification, status management
- **Session Management**: List, terminate, current session info
- **Security Monitoring**: Metrics, alerts, monitoring
- **Rate Limiting & CAPTCHA**: Status checks, CAPTCHA generation/verification
- **Tenant Management**: Context, configuration, usage statistics

### 2. User Management Service Collection
**File**: `CRM-Platform-User-Management-Service.postman_collection.json`

Complete collection for the User Management Service including:
- **User Management**: CRUD operations, search, lifecycle management
- **Profile Management**: Current user profile, updates, avatar upload
- **Role Management**: CRUD operations, assignments, permissions
- **Permission Management**: CRUD operations, categorization
- **Team Management**: CRUD operations, member management
- **Analytics & Reporting**: User analytics, bulk operations

### 3. Environment Configuration
**File**: `CRM-Platform-Environment.postman_environment.json`

Pre-configured environment with:
- Service base URLs (development environment)
- Test user credentials
- Auto-populated variables for tokens and IDs
- Configurable parameters for testing

## Quick Start Guide

### 1. Import Collections and Environment

1. Open Postman
2. Click **Import** button
3. Import all three files:
   - `CRM-Platform-Authentication-Service.postman_collection.json`
   - `CRM-Platform-User-Management-Service.postman_collection.json`
   - `CRM-Platform-Environment.postman_environment.json`

### 2. Set Up Environment

1. Select the **CRM Platform - Development Environment** from the environment dropdown
2. Update the following variables if needed:
   - `auth_base_url`: Authentication service URL (default: `http://localhost:8081/api/v1/auth`)
   - `users_base_url`: User management service URL (default: `http://localhost:8082/api/v1/users`)
   - `tenant_id`: Your tenant ID (default: `default-tenant-uuid`)
   - `test_email`: Test user email (default: `test.user@example.com`)
   - `test_password`: Test user password (default: `TestPassword123!`)

### 3. Authentication Flow

1. **Start with Login**: Run the \"Login - Basic\" request in the Authentication Service collection
2. **Verify Token**: The access token will be automatically stored in the `access_token` environment variable
3. **Use Protected Endpoints**: All subsequent requests will automatically use the stored token

### 4. Testing Workflow

#### Basic Authentication Testing
```
1. Authentication Service > Authentication > Login - Basic
2. Authentication Service > Session Management > List Sessions
3. User Management Service > Profile Management > Get Current User Profile
4. Authentication Service > Authentication > Logout
```

#### User Management Testing
```
1. Authentication Service > Authentication > Login - Basic
2. User Management Service > User Management > List Users
3. User Management Service > User Management > Create User
4. User Management Service > User Management > Update User
5. User Management Service > User Management > Delete User
```

#### Role and Permission Testing
```
1. Authentication Service > Authentication > Login - Basic
2. User Management Service > Role Management > List Roles
3. User Management Service > Role Management > Create Role
4. User Management Service > Role Management > Assign Role to User
5. User Management Service > Permission Management > List Permissions
```

## Environment Variables

### Auto-Populated Variables
These variables are automatically set by the collection scripts:

| Variable | Description | Set By |
|----------|-------------|---------|
| `access_token` | JWT access token | Login requests |
| `refresh_token` | JWT refresh token | Login requests |
| `user_id` | Current user ID | Login requests |
| `mfa_token` | MFA challenge token | MFA login flow |
| `session_id` | Session ID | Session list requests |
| `test_user_id` | Test user ID | User list requests |
| `created_user_id` | Created user ID | User creation |
| `test_role_id` | Test role ID | Role list requests |
| `created_role_id` | Created role ID | Role creation |
| `test_team_id` | Test team ID | Team list requests |
| `created_team_id` | Created team ID | Team creation |

### Manual Configuration Variables
Update these variables as needed:

| Variable | Description | Default Value |
|----------|-------------|---------------|
| `tenant_id` | Tenant identifier | `default-tenant-uuid` |
| `test_email` | Test user email | `test.user@example.com` |
| `test_password` | Test user password | `TestPassword123!` |
| `mfa_code` | MFA verification code | `123456` |
| `captcha_response` | CAPTCHA solution | `ABC123` |
| `current_password` | Current password for changes | `CurrentPassword123!` |
| `new_password` | New password for changes | `NewPassword123!` |

## Collection Features

### Automated Testing
Each request includes comprehensive test scripts that:
- Verify HTTP status codes
- Validate response structure
- Check for required fields
- Store relevant data in environment variables
- Provide meaningful error messages

### Pre-request Scripts
Collections include pre-request scripts that:
- Set up required headers (Authorization, X-Tenant-ID)
- Generate unique values for testing (emails, usernames)
- Validate environment setup
- Set dynamic timestamps

### Error Handling
Requests are designed to handle common scenarios:
- Invalid credentials
- Missing permissions
- Rate limiting
- Network errors
- Validation failures

## Advanced Usage

### Running Collections with Newman

Install Newman (Postman CLI):
```bash
npm install -g newman
```

Run entire collection:
```bash
newman run CRM-Platform-Authentication-Service.postman_collection.json \
  -e CRM-Platform-Environment.postman_environment.json \
  --reporters cli,json \
  --reporter-json-export results.json
```

Run specific folder:
```bash
newman run CRM-Platform-User-Management-Service.postman_collection.json \
  -e CRM-Platform-Environment.postman_environment.json \
  --folder "User Management" \
  --reporters cli
```

### Continuous Integration

Example GitHub Actions workflow:
```yaml
name: API Tests
on: [push, pull_request]
jobs:
  api-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Install Newman
        run: npm install -g newman
      - name: Run Authentication Tests
        run: |
          newman run docs/postman/CRM-Platform-Authentication-Service.postman_collection.json \
            -e docs/postman/CRM-Platform-Environment.postman_environment.json \
            --reporters cli,junit \
            --reporter-junit-export auth-results.xml
      - name: Run User Management Tests
        run: |
          newman run docs/postman/CRM-Platform-User-Management-Service.postman_collection.json \
            -e docs/postman/CRM-Platform-Environment.postman_environment.json \
            --reporters cli,junit \
            --reporter-junit-export users-results.xml
```

### Custom Test Scenarios

#### Load Testing
Use Newman with multiple iterations:
```bash
newman run CRM-Platform-Authentication-Service.postman_collection.json \
  -e CRM-Platform-Environment.postman_environment.json \
  -n 100 \
  --delay-request 100
```

#### Data-Driven Testing
Create a CSV file with test data:
```csv
email,password,expected_status
valid@example.com,ValidPass123!,200
invalid@example.com,wrongpass,401
```

Run with data file:
```bash
newman run collection.json -e environment.json -d testdata.csv
```

## Troubleshooting

### Common Issues

#### 1. Authentication Failures
- Verify service URLs are correct
- Check if services are running
- Ensure test credentials are valid
- Verify tenant ID is correct

#### 2. Token Expiration
- Re-run the login request to get fresh tokens
- Check token expiration times
- Use refresh token endpoint if available

#### 3. Permission Errors
- Ensure user has required roles/permissions
- Check tenant context is set correctly
- Verify admin credentials for privileged operations

#### 4. Network Issues
- Verify service endpoints are accessible
- Check firewall/proxy settings
- Ensure correct ports are used

### Debug Mode

Enable verbose logging in Newman:
```bash
newman run collection.json -e environment.json --verbose
```

### Request Debugging

Add debug information to pre-request scripts:
```javascript
console.log('Current environment:', pm.environment.name);
console.log('Access token:', pm.environment.get('access_token'));
console.log('Tenant ID:', pm.environment.get('tenant_id'));
```

## Best Practices

### 1. Environment Management
- Use separate environments for dev/staging/prod
- Never commit production credentials
- Use environment-specific URLs and settings

### 2. Test Organization
- Group related requests in folders
- Use descriptive request names
- Add comprehensive descriptions

### 3. Data Management
- Clean up test data after tests
- Use unique identifiers for test resources
- Implement proper teardown procedures

### 4. Security
- Rotate test credentials regularly
- Use least-privilege test accounts
- Avoid hardcoding sensitive data

### 5. Maintenance
- Keep collections updated with API changes
- Review and update test assertions
- Monitor test execution results

## Support

For issues with the Postman collections:
1. Check the troubleshooting section above
2. Verify API documentation is up to date
3. Test individual requests before running full collections
4. Check service logs for detailed error information

## Contributing

When updating collections:
1. Test all requests thoroughly
2. Update environment variables as needed
3. Add appropriate test assertions
4. Update documentation
5. Validate with Newman CLI

---

*Last updated: 2024-01-01*
*Version: 1.0*