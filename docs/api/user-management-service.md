# User Management Service API Documentation

The User Management Service provides comprehensive user profile management, role-based access control, team management, and organizational features.

## Base URL

```
http://localhost:8082/api/v1/users
```

## Table of Contents

1. [User Management](#user-management)
2. [Profile Management](#profile-management)
3. [Role Management](#role-management)
4. [Permission Management](#permission-management)
5. [Team Management](#team-management)
6. [User Lifecycle](#user-lifecycle)
7. [Search and Filtering](#search-and-filtering)
8. [Bulk Operations](#bulk-operations)
9. [Analytics and Reporting](#analytics-and-reporting)
10. [Error Codes](#error-codes)

## User Management

### List Users

Retrieve a paginated list of users.

**Endpoint**: `GET /users`

**Headers**:
```http
Authorization: Bearer <access_token>
X-Tenant-ID: <tenant_id>
```

**Query Parameters**:
- `page` (default: 0) - Page number
- `size` (default: 20) - Page size
- `sort` (default: "createdAt,desc") - Sort criteria
- `status` - Filter by user status (ACTIVE, INACTIVE, SUSPENDED)
- `role` - Filter by role name
- `search` - Search in name, email, or username

**Response**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "username": "john.doe",
        "email": "john.doe@example.com",
        "first_name": "John",
        "last_name": "Doe",
        "phone_number": "+1234567890",
        "status": "ACTIVE",
        "email_verified": true,
        "created_at": "2024-01-01T10:00:00Z",
        "last_login_at": "2024-01-01T12:00:00Z",
        "roles": ["USER", "MANAGER"],
        "teams": ["Development", "Leadership"]
      }
    ],
    "page": 0,
    "size": 20,
    "total_elements": 150,
    "total_pages": 8
  }
}
```

### Get User by ID

Retrieve detailed information about a specific user.

**Endpoint**: `GET /users/{id}`

**Headers**:
```http
Authorization: Bearer <access_token>
X-Tenant-ID: <tenant_id>
```

**Response**:
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john.doe",
    "email": "john.doe@example.com",
    "first_name": "John",
    "last_name": "Doe",
    "phone_number": "+1234567890",
    "job_title": "Senior Developer",
    "department": "Engineering",
    "manager_id": "manager-uuid",
    "status": "ACTIVE",
    "email_verified": true,
    "phone_verified": false,
    "created_at": "2024-01-01T10:00:00Z",
    "updated_at": "2024-01-01T11:00:00Z",
    "last_login_at": "2024-01-01T12:00:00Z",
    "timezone": "America/New_York",
    "language": "en",
    "profile_image_url": "https://example.com/avatar.jpg",
    "roles": [
      {
        "id": "role-uuid",
        "name": "USER",
        "description": "Standard user role",
        "assigned_at": "2024-01-01T10:00:00Z"
      }
    ],
    "teams": [
      {
        "id": "team-uuid",
        "name": "Development",
        "role": "MEMBER",
        "joined_at": "2024-01-01T10:00:00Z"
      }
    ],
    "permissions": ["READ_USERS", "WRITE_PROJECTS"],
    "custom_fields": {
      "employee_id": "EMP001",
      "hire_date": "2024-01-01"
    }
  }
}
```

### Create User

Create a new user account.

**Endpoint**: `POST /users`

**Headers**:
```http
Authorization: Bearer <access_token>
X-Tenant-ID: <tenant_id>
Content-Type: application/json
```

**Request Body**:
```json
{
  "username": "jane.smith",
  "email": "jane.smith@example.com",
  "first_name": "Jane",
  "last_name": "Smith",
  "phone_number": "+1234567891",
  "job_title": "Product Manager",
  "department": "Product",
  "manager_id": "manager-uuid",
  "timezone": "America/Los_Angeles",
  "language": "en",
  "roles": ["USER", "PRODUCT_MANAGER"],
  "teams": ["product-team-uuid"],
  "custom_fields": {
    "employee_id": "EMP002",
    "hire_date": "2024-01-15"
  },
  "send_welcome_email": true
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "username": "jane.smith",
    "email": "jane.smith@example.com",
    "status": "ACTIVE",
    "created_at": "2024-01-15T10:00:00Z",
    "temporary_password": "TempPass123!",
    "password_reset_required": true
  }
}
```

### Update User

Update user information.

**Endpoint**: `PUT /users/{id}`

**Headers**:
```http
Authorization: Bearer <access_token>
X-Tenant-ID: <tenant_id>
Content-Type: application/json
```

**Request Body**:
```json
{
  "first_name": "Jane",
  "last_name": "Johnson",
  "phone_number": "+1234567892",
  "job_title": "Senior Product Manager",
  "department": "Product",
  "timezone": "America/New_York",
  "custom_fields": {
    "employee_id": "EMP002",
    "promotion_date": "2024-01-15"
  }
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "message": "User updated successfully",
    "updated_at": "2024-01-15T11:00:00Z"
  }
}
```

### Delete User

Delete a user account (soft delete).

**Endpoint**: `DELETE /users/{id}`

**Headers**:
```http
Authorization: Bearer <access_token>
X-Tenant-ID: <tenant_id>
```

**Query Parameters**:
- `hard_delete` (default: false) - Perform hard delete
- `transfer_data_to` - User ID to transfer data to

**Response**:
```json
{
  "success": true,
  "data": {
    "message": "User deleted successfully",
    "deleted_at": "2024-01-15T12:00:00Z",
    "data_transferred_to": "manager-uuid"
  }
}
```

### Activate User

Activate a user account.

**Endpoint**: `POST /users/{id}/activate`

**Headers**:
```http
Authorization: Bearer <access_token>
X-Tenant-ID: <tenant_id>
```

**Response**:
```json
{
  "success": true,
  "data": {
    "message": "User activated successfully",
    "activated_at": "2024-01-15T13:00:00Z"
  }
}
```

### Deactivate User

Deactivate a user account.

**Endpoint**: `POST /users/{id}/deactivate`

**Headers**:
```http
Authorization: Bearer <access_token>
X-Tenant-ID: <tenant_id>
```

**Request Body**:
```json
{
  "reason": "Employee left company",
  "transfer_data_to": "manager-uuid"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "message": "User deactivated successfully",
    "deactivated_at": "2024-01-15T14:00:00Z"
  }
}
```

## Profile Management

### Get Current User Profile

Get the profile of the currently authenticated user.

**Endpoint**: `GET /users/me`

**Headers**:
```http
Authorization: Bearer <access_token>
```

**Response**:
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john.doe",
    "email": "john.doe@example.com",
    "first_name": "John",
    "last_name": "Doe",
    "profile_image_url": "https://example.com/avatar.jpg",
    "preferences": {
      "theme": "dark",
      "notifications": {
        "email": true,
        "push": false,
        "sms": false
      },
      "language": "en",
      "timezone": "America/New_York"
    }
  }
}
```

### Update Profile

Update the current user's profile.

**Endpoint**: `PUT /users/me`

**Headers**:
```http
Authorization: Bearer <access_token>
Content-Type: application/json
```

**Request Body**:
```json
{
  "first_name": "John",
  "last_name": "Doe",
  "phone_number": "+1234567890",
  "timezone": "America/Los_Angeles",
  "language": "es",
  "preferences": {
    "theme": "light",
    "notifications": {
      "email": true,
      "push": true,
      "sms": false
    }
  }
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "message": "Profile updated successfully",
    "updated_at": "2024-01-15T15:00:00Z"
  }
}
```

### Upload Profile Image

Upload a profile image for the current user.

**Endpoint**: `POST /users/me/avatar`

**Headers**:
```http
Authorization: Bearer <access_token>
Content-Type: multipart/form-data
```

**Request Body** (multipart/form-data):
- `file` - Image file (JPEG, PNG, max 5MB)

**Response**:
```json
{
  "success": true,
  "data": {
    "profile_image_url": "https://example.com/avatars/user-uuid.jpg",
    "uploaded_at": "2024-01-15T16:00:00Z"
  }
}
```

## Role Management

### List Roles

Retrieve all available roles.

**Endpoint**: `GET /roles`

**Headers**:
```http
Authorization: Bearer <access_token>
X-Tenant-ID: <tenant_id>
```

**Query Parameters**:
- `page` (default: 0)
- `size` (default: 20)
- `search` - Search in role name or description

**Response**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "role-uuid",
        "name": "ADMIN",
        "description": "System administrator with full access",
        "permissions": ["ALL"],
        "user_count": 5,
        "created_at": "2024-01-01T10:00:00Z",
        "is_system_role": true
      },
      {
        "id": "role-uuid-2",
        "name": "MANAGER",
        "description": "Team manager with limited admin access",
        "permissions": ["READ_USERS", "WRITE_PROJECTS", "MANAGE_TEAM"],
        "user_count": 15,
        "created_at": "2024-01-01T10:00:00Z",
        "is_system_role": false
      }
    ],
    "page": 0,
    "size": 20,
    "total_elements": 8,
    "total_pages": 1
  }
}
```

### Create Role

Create a new role.

**Endpoint**: `POST /roles`

**Headers**:
```http
Authorization: Bearer <access_token>
X-Tenant-ID: <tenant_id>
Content-Type: application/json
```

**Request Body**:
```json
{
  "name": "SALES_MANAGER",
  "description": "Sales team manager with CRM access",
  "permissions": [
    "READ_USERS",
    "READ_CONTACTS",
    "WRITE_CONTACTS",
    "READ_DEALS",
    "WRITE_DEALS",
    "MANAGE_SALES_TEAM"
  ]
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "id": "new-role-uuid",
    "name": "SALES_MANAGER",
    "description": "Sales team manager with CRM access",
    "permissions": [
      "READ_USERS",
      "READ_CONTACTS",
      "WRITE_CONTACTS",
      "READ_DEALS",
      "WRITE_DEALS",
      "MANAGE_SALES_TEAM"
    ],
    "created_at": "2024-01-15T17:00:00Z"
  }
}
```

### Assign Role to User

Assign a role to a user.

**Endpoint**: `POST /users/{userId}/roles`

**Headers**:
```http
Authorization: Bearer <access_token>
X-Tenant-ID: <tenant_id>
Content-Type: application/json
```

**Request Body**:
```json
{
  "role_id": "role-uuid",
  "expires_at": "2024-12-31T23:59:59Z"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "message": "Role assigned successfully",
    "assigned_at": "2024-01-15T18:00:00Z"
  }
}
```

### Remove Role from User

Remove a role from a user.

**Endpoint**: `DELETE /users/{userId}/roles/{roleId}`

**Headers**:
```http
Authorization: Bearer <access_token>
X-Tenant-ID: <tenant_id>
```

**Response**:
```json
{
  "success": true,
  "data": {
    "message": "Role removed successfully",
    "removed_at": "2024-01-15T19:00:00Z"
  }
}
```

---

*For more information, see the [main API documentation](README.md).*