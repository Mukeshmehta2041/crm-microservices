package com.crm.platform.users.controller;

import com.crm.platform.users.dto.CreateUserRequest;
import com.crm.platform.users.dto.UpdateUserRequest;
import com.crm.platform.users.dto.UserResponse;
import com.crm.platform.users.entity.User;
import com.crm.platform.users.service.UserService;
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
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<UserResponse>> getUsersByTenant(@PathVariable UUID tenantId) {
        List<UserResponse> users = userService.getUsersByTenant(tenantId);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/tenant/{tenantId}/paginated")
    public ResponseEntity<Page<UserResponse>> getUsersByTenantPaginated(
            @PathVariable UUID tenantId,
            Pageable pageable) {
        Page<UserResponse> users = userService.getUsersByTenant(tenantId, pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/tenant/{tenantId}/search")
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @PathVariable UUID tenantId,
            @RequestParam String query,
            Pageable pageable) {
        Page<UserResponse> users = userService.searchUsers(tenantId, query, pageable);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        try {
            UserResponse updatedUser = userService.updateUser(id, request);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/activity")
    public ResponseEntity<Void> updateLastActivity(@PathVariable UUID id) {
        try {
            userService.updateLastActivity(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateUserStatus(
            @PathVariable UUID id,
            @RequestParam User.UserStatus status) {
        try {
            userService.updateUserStatus(id, status);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/tenant/{tenantId}/count")
    public ResponseEntity<Long> countActiveUsers(@PathVariable UUID tenantId) {
        long count = userService.countActiveUsersByTenant(tenantId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/tenant/{tenantId}/onboarding-incomplete")
    public ResponseEntity<List<UserResponse>> getUsersWithIncompleteOnboarding(@PathVariable UUID tenantId) {
        List<UserResponse> users = userService.getUsersWithIncompleteOnboarding(tenantId);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Users Service is healthy");
    }
}