package com.crm.platform.users.controller;

import com.crm.platform.users.dto.CreateUserRequest;
import com.crm.platform.users.dto.UpdateUserRequest;
import com.crm.platform.users.dto.UserResponse;
import com.crm.platform.users.entity.User;
import com.crm.platform.users.service.UserService;
import com.crm.platform.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByTenant(@PathVariable UUID tenantId) {
        List<UserResponse> users = userService.getUsersByTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/tenant/{tenantId}/paginated")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsersByTenantPaginated(
            @PathVariable UUID tenantId,
            Pageable pageable) {
        Page<UserResponse> users = userService.getUsersByTenant(tenantId, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/tenant/{tenantId}/search")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(
            @PathVariable UUID tenantId,
            @RequestParam String query,
            Pageable pageable) {
        Page<UserResponse> users = userService.searchUsers(tenantId, query, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        try {
            UserResponse updatedUser = userService.updateUser(id, request);
            return ResponseEntity.ok(ApiResponse.success(updatedUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/activity")
    public ResponseEntity<ApiResponse<Void>> updateLastActivity(@PathVariable UUID id) {
        try {
            userService.updateLastActivity(id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable UUID id,
            @RequestParam User.UserStatus status) {
        try {
            userService.updateUserStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/tenant/{tenantId}/count")
    public ResponseEntity<ApiResponse<Long>> countActiveUsers(@PathVariable UUID tenantId) {
        long count = userService.countActiveUsersByTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/tenant/{tenantId}/onboarding-incomplete")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersWithIncompleteOnboarding(@PathVariable UUID tenantId) {
        List<UserResponse> users = userService.getUsersWithIncompleteOnboarding(tenantId);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Users Service is healthy"));
    }
}