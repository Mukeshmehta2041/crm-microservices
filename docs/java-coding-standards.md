# Java Coding Standards for CRM Platform

## Overview

This document outlines the comprehensive Java coding standards for the CRM microservices platform. These standards ensure code consistency, maintainability, and quality across all services.

## Table of Contents

1. [General Principles](#general-principles)
2. [Code Formatting](#code-formatting)
3. [Naming Conventions](#naming-conventions)
4. [Class Design](#class-design)
5. [Method Design](#method-design)
6. [Exception Handling](#exception-handling)
7. [Logging](#logging)
8. [Testing](#testing)
9. [Spring Boot Specific Guidelines](#spring-boot-specific-guidelines)
10. [Security Guidelines](#security-guidelines)
11. [Performance Guidelines](#performance-guidelines)
12. [Documentation](#documentation)

## General Principles

### SOLID Principles
- **Single Responsibility Principle**: Each class should have only one reason to change
- **Open/Closed Principle**: Classes should be open for extension but closed for modification
- **Liskov Substitution Principle**: Objects should be replaceable with instances of their subtypes
- **Interface Segregation Principle**: Clients should not depend on interfaces they don't use
- **Dependency Inversion Principle**: Depend on abstractions, not concretions

### Clean Code Principles
- Write code that is self-documenting
- Use meaningful names for variables, methods, and classes
- Keep methods small and focused
- Avoid deep nesting
- Use composition over inheritance
- Follow the DRY (Don't Repeat Yourself) principle

## Code Formatting

### Line Length
- Maximum line length: **120 characters**
- Break long lines at logical points
- Use proper indentation for continuation lines

### Indentation
- Use **4 spaces** for indentation (no tabs)
- Align continuation lines with the opening parenthesis or use 8-space indentation

### Braces
- Use K&R style braces (opening brace on same line)
- Always use braces for control structures, even single statements

```java
// Good
if (condition) {
    doSomething();
}

// Bad
if (condition)
    doSomething();
```

### Whitespace
- One space after keywords (`if`, `for`, `while`, etc.)
- One space around operators (`=`, `+`, `-`, etc.)
- No trailing whitespace
- One blank line between methods
- Two blank lines between class declarations

## Naming Conventions

### Classes and Interfaces
- Use **PascalCase** for class and interface names
- Use descriptive names that clearly indicate the purpose
- Interfaces should not have prefixes like `I`

```java
// Good
public class UserService { }
public interface PaymentProcessor { }

// Bad
public class userservice { }
public interface IPaymentProcessor { }
```

### Methods
- Use **camelCase** for method names
- Use verbs or verb phrases
- Boolean methods should start with `is`, `has`, `can`, or `should`

```java
// Good
public void processPayment() { }
public boolean isValid() { }
public boolean hasPermission() { }

// Bad
public void ProcessPayment() { }
public boolean valid() { }
```

### Variables
- Use **camelCase** for variable names
- Use descriptive names, avoid abbreviations
- Constants should be **UPPER_SNAKE_CASE**

```java
// Good
private String firstName;
private static final int MAX_RETRY_ATTEMPTS = 3;

// Bad
private String fName;
private static final int maxRetryAttempts = 3;
```

### Packages
- Use **lowercase** with dots as separators
- Follow reverse domain naming convention
- Use singular nouns

```java
// Good
package com.crm.platform.user.service;

// Bad
package com.crm.platform.User.Services;
```

## Class Design

### Class Size
- Keep classes focused and cohesive
- Maximum of **500 lines** per class
- If a class exceeds this limit, consider splitting it

### Constructor Guidelines
- Prefer constructor injection over field injection
- Use builder pattern for classes with many parameters
- Validate constructor parameters

```java
@Service
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.emailService = Objects.requireNonNull(emailService);
    }
}
```

### Field Guidelines
- Make fields `private` and `final` when possible
- Use `@Autowired` on constructors, not fields
- Initialize collections in field declarations when appropriate

```java
// Good
private final List<String> items = new ArrayList<>();

// Bad
@Autowired
private UserRepository userRepository;
```

## Method Design

### Method Size
- Keep methods small and focused
- Maximum of **30 lines** per method
- Extract complex logic into separate methods

### Parameter Guidelines
- Maximum of **5 parameters** per method
- Use parameter objects for methods with many parameters
- Validate method parameters

```java
// Good
public User createUser(CreateUserRequest request) {
    validateCreateUserRequest(request);
    // implementation
}

// Bad
public User createUser(String firstName, String lastName, String email, 
                      String phone, String address, String city, String state) {
    // implementation
}
```

### Return Values
- Prefer returning `Optional<T>` instead of `null`
- Use meaningful return types
- Document what the method returns

```java
// Good
public Optional<User> findUserById(Long id) {
    return userRepository.findById(id);
}

// Bad
public User findUserById(Long id) {
    return userRepository.findById(id).orElse(null);
}
```

## Exception Handling

### Exception Types
- Use checked exceptions for recoverable conditions
- Use unchecked exceptions for programming errors
- Create custom exceptions for business logic errors

```java
// Custom business exception
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
```

### Exception Handling Best Practices
- Never catch and ignore exceptions
- Log exceptions at the appropriate level
- Don't use exceptions for control flow
- Clean up resources in `finally` blocks or use try-with-resources

```java
// Good
try (FileInputStream fis = new FileInputStream(file)) {
    // process file
} catch (IOException e) {
    log.error("Failed to process file: {}", file.getName(), e);
    throw new FileProcessingException("Unable to process file", e);
}
```

## Logging

### Logging Framework
- Use **SLF4J** with **Logback** as the implementation
- Use parameterized logging messages
- Choose appropriate log levels

### Log Levels
- **ERROR**: System errors and exceptions
- **WARN**: Potentially harmful situations
- **INFO**: General information about application flow
- **DEBUG**: Detailed information for debugging
- **TRACE**: Very detailed information

```java
// Good
private static final Logger log = LoggerFactory.getLogger(UserService.class);

public void processUser(User user) {
    log.info("Processing user: {}", user.getId());
    try {
        // process user
        log.debug("User processed successfully: {}", user.getId());
    } catch (Exception e) {
        log.error("Failed to process user: {}", user.getId(), e);
        throw new UserProcessingException("Unable to process user", e);
    }
}

// Bad
System.out.println("Processing user: " + user.getId());
log.info("Processing user: " + user.getId()); // String concatenation
```

## Testing

### Test Structure
- Follow the **Arrange-Act-Assert** pattern
- Use descriptive test method names
- One assertion per test method when possible

```java
@Test
void shouldCreateUserWhenValidDataProvided() {
    // Arrange
    CreateUserRequest request = CreateUserRequest.builder()
        .firstName("John")
        .lastName("Doe")
        .email("john.doe@example.com")
        .build();
    
    // Act
    User result = userService.createUser(request);
    
    // Assert
    assertThat(result.getFirstName()).isEqualTo("John");
    assertThat(result.getLastName()).isEqualTo("Doe");
    assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
}
```

### Test Coverage
- Maintain minimum **80%** code coverage
- Focus on testing business logic
- Use integration tests for complex scenarios

### Test Naming
- Use descriptive names that explain the scenario
- Format: `should[ExpectedBehavior]When[StateUnderTest]`

## Spring Boot Specific Guidelines

### Annotations
- Use `@RestController` for REST endpoints
- Use `@Service` for business logic
- Use `@Repository` for data access
- Use `@Component` for general components

### Configuration
- Use `@ConfigurationProperties` for external configuration
- Validate configuration properties
- Use profiles for environment-specific configuration

```java
@ConfigurationProperties(prefix = "app.security")
@Validated
public class SecurityProperties {
    @NotBlank
    private String jwtSecret;
    
    @Min(1)
    private int jwtExpirationHours = 24;
    
    // getters and setters
}
```

### REST Controllers
- Use proper HTTP methods and status codes
- Validate request parameters
- Use consistent response formats

```java
@RestController
@RequestMapping("/api/v1/users")
@Validated
public class UserController {
    
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request);
        UserResponse response = userMapper.toResponse(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

## Security Guidelines

### Input Validation
- Validate all input parameters
- Use `@Valid` and `@Validated` annotations
- Sanitize user input to prevent injection attacks

### Authentication and Authorization
- Use Spring Security for authentication
- Implement proper authorization checks
- Never store passwords in plain text

```java
@PreAuthorize("hasRole('ADMIN') or @userService.isOwner(#userId, authentication.name)")
public User updateUser(Long userId, UpdateUserRequest request) {
    // implementation
}
```

### Sensitive Data
- Never log sensitive information
- Use encryption for sensitive data at rest
- Use HTTPS for data in transit

## Performance Guidelines

### Database Access
- Use pagination for large result sets
- Avoid N+1 query problems
- Use appropriate fetch strategies

### Caching
- Cache frequently accessed data
- Use appropriate cache eviction strategies
- Monitor cache hit rates

### Resource Management
- Close resources properly
- Use connection pooling
- Monitor memory usage

## Documentation

### JavaDoc
- Document all public classes and methods
- Use proper JavaDoc tags
- Include examples when helpful

```java
/**
 * Creates a new user in the system.
 *
 * @param request the user creation request containing user details
 * @return the created user with generated ID
 * @throws UserAlreadyExistsException if a user with the same email already exists
 * @throws ValidationException if the request contains invalid data
 */
public User createUser(CreateUserRequest request) {
    // implementation
}
```

### README Files
- Include setup instructions
- Document API endpoints
- Provide usage examples

### Code Comments
- Use comments sparingly for complex business logic
- Explain "why" not "what"
- Keep comments up to date

## Code Quality Tools

### Static Analysis
- **Checkstyle**: Code formatting and style checks
- **PMD**: Code quality and potential bug detection
- **SpotBugs**: Bug pattern detection
- **SonarQube**: Comprehensive code quality analysis

### Formatting
- **Spotless**: Automatic code formatting
- **Google Java Format**: Consistent code style

### Security
- **OWASP Dependency Check**: Vulnerability scanning
- **Snyk**: Security vulnerability detection

## Enforcement

### Pre-commit Hooks
- Run code formatting checks
- Execute unit tests
- Perform static analysis

### CI/CD Pipeline
- Automated code quality gates
- Test coverage requirements
- Security vulnerability scanning

### Code Review
- All code must be reviewed before merging
- Focus on design, logic, and maintainability
- Ensure adherence to coding standards

## Conclusion

These coding standards are designed to ensure high-quality, maintainable, and secure code across the CRM platform. All developers should familiarize themselves with these guidelines and apply them consistently in their daily work.

For questions or suggestions regarding these standards, please reach out to the development team leads or create an issue in the project repository.