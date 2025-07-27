# Microservice Architecture Refactoring

## Overview

This document describes the refactoring performed to properly separate user authentication from user profile management, following microservice best practices as outlined in the API documentation.

## Problem

Previously, the `User` entity was located in the `auth-service`, which violated the single responsibility principle and proper service boundaries. The auth-service was handling both authentication concerns and user profile management.

## Solution

### 1. Service Separation

**Auth Service** (`services/auth-service/`)
- **Responsibility**: Authentication, authorization, session management
- **Entities**: 
  - `UserCredentials` - username, password, login attempts, account locking
  - `UserSession` - JWT tokens, session management
  - `SecurityAuditLog` - security events and audit trails

**User Management Service** (`services/users-service/`)
- **Responsibility**: User profile management, preferences, roles
- **Entities**:
  - `User` - profile information, preferences, roles, team assignments

### 2. New Architecture

```
┌─────────────────┐    ┌──────────────────────┐
│   Auth Service  │    │ User Management      │
│   Port: 8081    │◄──►│ Service              │
│                 │    │ Port: 8082           │
│ - Authentication│    │ - User Profiles      │
│ - JWT Tokens    │    │ - User Preferences   │
│ - Session Mgmt  │    │ - Role Management    │
│ - Security Audit│    │ - Team Management    │
└─────────────────┘    └──────────────────────┘
```

### 3. Key Changes

#### Auth Service Changes:
- **New Entity**: `UserCredentials` - stores authentication-specific data
- **New Repository**: `UserCredentialsRepository` - handles credential operations
- **New Client**: `UserServiceClient` - communicates with User Management Service
- **Updated Service**: `AuthenticationService` - now calls User Service for profile data
- **Database**: New `user_credentials` table with migration from existing `users` table

#### User Management Service Changes:
- **New Entity**: `User` - stores user profile and preference data
- **New Repository**: `UserRepository` - handles user profile operations
- **New Service**: `UserService` - manages user profiles and preferences
- **New Controller**: `UserController` - exposes user management APIs
- **Database**: New `users` table with user profile data

### 4. API Endpoints

#### Auth Service (`/api/v1/auth`)
- `POST /login` - User authentication
- `POST /refresh` - Token refresh
- `POST /logout` - User logout
- `GET /validate` - Token validation
- `POST /password/reset` - Password reset

#### User Management Service (`/api/v1/users`)
- `POST /users` - Create user profile
- `GET /users/{id}` - Get user profile
- `PUT /users/{id}` - Update user profile
- `GET /users/email/{email}` - Get user by email
- `GET /users/tenant/{tenantId}` - Get users by tenant
- `GET /users/tenant/{tenantId}/search` - Search users
- `PATCH /users/{id}/activity` - Update last activity
- `PATCH /users/{id}/status` - Update user status
- `DELETE /users/{id}` - Delete user (soft delete)

### 5. Data Flow

#### Login Process:
1. Client sends credentials to Auth Service
2. Auth Service validates credentials against `UserCredentials`
3. Auth Service calls User Management Service to get user profile
4. Auth Service returns JWT token with user profile information

#### User Profile Updates:
1. Client calls User Management Service directly
2. User Management Service updates user profile
3. Auth Service continues to work with existing credentials

### 6. Database Schema

#### Auth Service Database:
```sql
-- Authentication credentials
CREATE TABLE user_credentials (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,  -- Reference to User in User Management Service
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(60) NOT NULL,
    tenant_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts INTEGER DEFAULT 0,
    account_locked_until TIMESTAMP,
    -- ... other auth-specific fields
);
```

#### User Management Service Database:
```sql
-- User profile information
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    tenant_id UUID NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(20),
    job_title VARCHAR(150),
    department VARCHAR(100),
    -- ... other profile fields
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);
```

### 7. Configuration

#### Auth Service Configuration:
```yaml
services:
  user-service:
    url: http://localhost:8082
```

#### User Management Service Configuration:
```yaml
server:
  port: 8082
spring:
  application:
    name: users-service
  datasource:
    url: jdbc:postgresql://localhost:5432/crm_users
```

### 8. Benefits

1. **Single Responsibility**: Each service has a clear, focused responsibility
2. **Scalability**: Services can be scaled independently based on load
3. **Maintainability**: Easier to maintain and update each service separately
4. **Security**: Authentication logic is isolated from user data
5. **Flexibility**: User profile features can evolve without affecting authentication
6. **Compliance**: Better data governance and audit capabilities

### 9. Migration Steps

1. ✅ Create `UserCredentials` entity in Auth Service
2. ✅ Create `User` entity in User Management Service  
3. ✅ Create repositories and services for both services
4. ✅ Update `AuthenticationService` to use new architecture
5. ✅ Create database migrations
6. ✅ Add service-to-service communication
7. ✅ Remove old `User` entity from Auth Service
8. ⏳ Test the new architecture
9. ⏳ Deploy and monitor

### 10. Testing

To test the new architecture:

1. **Start Services**:
   ```bash
   # Start User Management Service
   cd services/users-service
   mvn spring-boot:run
   
   # Start Auth Service  
   cd services/auth-service
   mvn spring-boot:run
   ```

2. **Create User Profile** (User Management Service):
   ```bash
   curl -X POST http://localhost:8082/api/v1/users \
     -H "Content-Type: application/json" \
     -d '{
       "email": "test@example.com",
       "tenantId": "tenant-123",
       "firstName": "Test",
       "lastName": "User"
     }'
   ```

3. **Create Credentials** (Auth Service):
   ```bash
   # This would typically be done through a user registration endpoint
   # For now, you'll need to insert into user_credentials table manually
   ```

4. **Login** (Auth Service):
   ```bash
   curl -X POST http://localhost:8081/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{
       "usernameOrEmail": "test@example.com",
       "password": "password123"
     }'
   ```

### 11. Next Steps

1. Implement user registration endpoint that creates both profile and credentials
2. Add proper error handling for service communication failures
3. Implement caching for user profile data in Auth Service
4. Add monitoring and logging for service-to-service calls
5. Implement circuit breaker pattern for resilience
6. Add integration tests for the complete flow