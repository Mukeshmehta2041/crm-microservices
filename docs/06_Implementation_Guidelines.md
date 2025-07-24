# Implementation Guidelines

This document provides comprehensive guidelines for implementing the CRM platform, covering Java coding standards, microservice development patterns, message broker integration, and version control strategies.

---

**Status**: In Progress  
**Last Updated**: January 2025  
**Version**: 1.0

## Table of Contents

1. [Java Coding Standards](#java-coding-standards)
2. [Microservice Development Patterns](#microservice-development-patterns)
3. [Message Broker Integration Patterns](#message-broker-integration-patterns)
4. [Version Control and Branching Strategy](#version-control-and-branching-strategy)

---

## Java Coding Standards

### Code Formatting and Style Guidelines

#### General Formatting Rules

**Indentation and Spacing:**
- Use 4 spaces for indentation (no tabs)
- Maximum line length: 120 characters
- Use blank lines to separate logical blocks of code
- No trailing whitespace

**Braces and Brackets:**
```java
// Correct - K&R style braces
public class CustomerService {
    public void processCustomer(Customer customer) {
        if (customer.isValid()) {
            // process customer
        } else {
            throw new InvalidCustomerException("Invalid customer data");
        }
    }
}

// Incorrect - Allman style
public class CustomerService 
{
    public void processCustomer(Customer customer) 
    {
        // Don't use this style
    }
}
```

**Import Organization:**
```java
// Static imports first
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Java standard library
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Third-party libraries (alphabetical by group)
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// Internal packages (alphabetical)
import com.crmplatform.common.exceptions.BusinessException;
import com.crmplatform.customer.model.Customer;
```

### Naming Conventions

#### Package Naming
```java
// Base package structure
com.crmplatform.{module}.{layer}

// Examples
com.crmplatform.customer.service
com.crmplatform.customer.repository
com.crmplatform.customer.controller
com.crmplatform.customer.model
com.crmplatform.customer.config
```

#### Class and Interface Naming
```java
// Classes - PascalCase, descriptive nouns
public class CustomerService { }
public class PaymentProcessor { }
public class OrderValidationException extends BusinessException { }

// Interfaces - PascalCase, often with 'able' suffix or descriptive
public interface Processable { }
public interface CustomerRepository { }
public interface PaymentGateway { }

// Abstract classes - prefix with 'Abstract' or 'Base'
public abstract class AbstractEntityService<T> { }
public abstract class BaseController { }
```

#### Method and Variable Naming
```java
public class CustomerService {
    // Constants - UPPER_SNAKE_CASE
    private static final String DEFAULT_CUSTOMER_STATUS = "ACTIVE";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    // Fields - camelCase
    private final CustomerRepository customerRepository;
    private final NotificationService notificationService;
    
    // Methods - camelCase, verb-based
    public Customer createCustomer(CustomerRequest request) { }
    public Optional<Customer> findCustomerById(Long customerId) { }
    public boolean isCustomerActive(Customer customer) { }
    public void validateCustomerData(Customer customer) { }
    
    // Boolean methods - use 'is', 'has', 'can', 'should'
    public boolean isValidEmail(String email) { }
    public boolean hasActiveSubscription(Customer customer) { }
    public boolean canProcessPayment(Payment payment) { }
}
```

### Package Organization and Module Structure

#### Standard Module Structure
```
src/main/java/com/crmplatform/{module}/
├── config/                 # Configuration classes
│   ├── {Module}Config.java
│   └── {Module}Properties.java
├── controller/             # REST controllers
│   ├── {Entity}Controller.java
│   └── dto/               # Data Transfer Objects
│       ├── {Entity}Request.java
│       └── {Entity}Response.java
├── service/               # Business logic
│   ├── {Entity}Service.java
│   └── impl/
│       └── {Entity}ServiceImpl.java
├── repository/            # Data access layer
│   ├── {Entity}Repository.java
│   └── impl/
│       └── {Entity}RepositoryImpl.java
├── model/                 # Domain entities
│   ├── {Entity}.java
│   └── enums/
│       └── {Entity}Status.java
├── exception/             # Custom exceptions
│   ├── {Entity}Exception.java
│   └── {Entity}ValidationException.java
└── util/                  # Utility classes
    └── {Entity}Utils.java
```

#### Layer Responsibilities

**Controller Layer:**
```java
@RestController
@RequestMapping("/api/v1/customers")
@Validated
public class CustomerController {
    
    private final CustomerService customerService;
    
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }
    
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CustomerRequest request) {
        Customer customer = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomerMapper.toResponse(customer));
    }
}
```

**Service Layer:**
```java
@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {
    
    private final CustomerRepository customerRepository;
    private final EventPublisher eventPublisher;
    
    public CustomerServiceImpl(CustomerRepository customerRepository,
                              EventPublisher eventPublisher) {
        this.customerRepository = customerRepository;
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    public Customer createCustomer(CustomerRequest request) {
        validateCustomerRequest(request);
        
        Customer customer = CustomerMapper.fromRequest(request);
        customer = customerRepository.save(customer);
        
        eventPublisher.publishEvent(new CustomerCreatedEvent(customer));
        
        return customer;
    }
}
```

### Dependency Injection and Configuration Management

#### Constructor Injection (Preferred)
```java
@Service
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    private final NotificationService notificationService;
    private final CustomerProperties properties;
    
    // Constructor injection - preferred approach
    public CustomerService(CustomerRepository customerRepository,
                          NotificationService notificationService,
                          CustomerProperties properties) {
        this.customerRepository = customerRepository;
        this.notificationService = notificationService;
        this.properties = properties;
    }
}
```

#### Configuration Properties
```java
@ConfigurationProperties(prefix = "crm.customer")
@Data
@Validated
public class CustomerProperties {
    
    @NotNull
    @Min(1)
    private Integer maxRetryAttempts = 3;
    
    @NotNull
    @Pattern(regexp = "^[A-Z_]+$")
    private String defaultStatus = "ACTIVE";
    
    @Valid
    private ValidationConfig validation = new ValidationConfig();
    
    @Data
    public static class ValidationConfig {
        private boolean strictEmailValidation = true;
        private int minNameLength = 2;
        private int maxNameLength = 100;
    }
}
```

#### Configuration Classes
```java
@Configuration
@EnableConfigurationProperties(CustomerProperties.class)
public class CustomerConfig {
    
    @Bean
    @ConditionalOnMissingBean
    public CustomerValidator customerValidator(CustomerProperties properties) {
        return new CustomerValidator(properties.getValidation());
    }
    
    @Bean
    @ConditionalOnProperty(name = "crm.customer.async.enabled", havingValue = "true")
    public AsyncCustomerProcessor asyncCustomerProcessor() {
        return new AsyncCustomerProcessor();
    }
}
```

### Code Review Checklists and Quality Gates

#### Pre-Commit Checklist

**Code Quality:**
- [ ] Code follows established naming conventions
- [ ] Methods are focused and have single responsibility
- [ ] Classes are cohesive and loosely coupled
- [ ] No code duplication (DRY principle)
- [ ] Proper error handling and logging
- [ ] No hardcoded values (use configuration)

**Documentation:**
- [ ] Public methods have Javadoc comments
- [ ] Complex business logic is commented
- [ ] README updated if necessary
- [ ] API documentation updated

**Testing:**
- [ ] Unit tests cover new/modified code
- [ ] Integration tests for service interactions
- [ ] Test names are descriptive
- [ ] Edge cases are tested

#### Code Review Quality Gates

**Automated Quality Gates:**
```yaml
# SonarQube quality gate configuration
quality_gate:
  coverage:
    minimum: 80%
    new_code: 85%
  
  duplicated_lines:
    maximum: 3%
  
  maintainability_rating: A
  reliability_rating: A
  security_rating: A
  
  code_smells:
    maximum: 10
  
  bugs:
    maximum: 0
  
  vulnerabilities:
    maximum: 0
```

**Manual Review Checklist:**

**Architecture and Design:**
- [ ] Follows established architectural patterns
- [ ] Proper separation of concerns
- [ ] Appropriate use of design patterns
- [ ] Database transactions handled correctly
- [ ] Security considerations addressed

**Performance:**
- [ ] No obvious performance bottlenecks
- [ ] Proper use of caching where appropriate
- [ ] Database queries are optimized
- [ ] Async processing used where beneficial

**Maintainability:**
- [ ] Code is readable and self-documenting
- [ ] Consistent with existing codebase style
- [ ] Proper exception handling
- [ ] Logging is appropriate and informative

#### Quality Standards

**Complexity Metrics:**
- Cyclomatic complexity: Maximum 10 per method
- Method length: Maximum 30 lines
- Class length: Maximum 300 lines
- Parameter count: Maximum 5 per method

**Test Coverage Requirements:**
- Unit test coverage: Minimum 80%
- Integration test coverage: Minimum 70%
- Critical path coverage: 100%
- New code coverage: Minimum 85%

**Documentation Standards:**
```java
/**
 * Processes customer registration and sends welcome notification.
 * 
 * @param request the customer registration request containing personal details
 * @return the created customer with generated ID and timestamps
 * @throws CustomerValidationException if customer data is invalid
 * @throws DuplicateCustomerException if customer already exists
 * @since 1.0
 */
@Override
public Customer registerCustomer(CustomerRegistrationRequest request) {
    // Implementation
}
```

---

## Microservice Development Patterns

### Service Implementation Patterns and Best Practices

#### Domain-Driven Design (DDD) Patterns

**Aggregate Pattern:**
```java
@Entity
@Table(name = "customers")
public class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Embedded
    private CustomerProfile profile;
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CustomerContact> contacts = new ArrayList<>();
    
    // Business methods that maintain invariants
    public void updateProfile(CustomerProfile newProfile) {
        validateProfileUpdate(newProfile);
        this.profile = newProfile;
        this.lastModified = LocalDateTime.now();
    }
    
    public void addContact(ContactInfo contactInfo) {
        if (contacts.size() >= MAX_CONTACTS_PER_CUSTOMER) {
            throw new BusinessException("Maximum contacts limit exceeded");
        }
        contacts.add(new CustomerContact(this, contactInfo));
    }
    
    private void validateProfileUpdate(CustomerProfile profile) {
        // Domain validation logic
    }
}
```

**Repository Pattern:**
```java
public interface CustomerRepository {
    Optional<Customer> findById(Long id);
    Optional<Customer> findByEmail(String email);
    List<Customer> findByStatus(CustomerStatus status);
    Customer save(Customer customer);
    void deleteById(Long id);
    
    // Custom query methods
    List<Customer> findActiveCustomersWithRecentActivity(LocalDateTime since);
    Page<Customer> findBySearchCriteria(CustomerSearchCriteria criteria, Pageable pageable);
}

@Repository
public class JpaCustomerRepository implements CustomerRepository {
    
    private final CustomerJpaRepository jpaRepository;
    private final EntityManager entityManager;
    
    public JpaCustomerRepository(CustomerJpaRepository jpaRepository,
                                EntityManager entityManager) {
        this.jpaRepository = jpaRepository;
        this.entityManager = entityManager;
    }
    
    @Override
    public List<Customer> findActiveCustomersWithRecentActivity(LocalDateTime since) {
        return entityManager.createQuery(
            "SELECT c FROM Customer c " +
            "WHERE c.status = :status " +
            "AND c.lastActivity >= :since", Customer.class)
            .setParameter("status", CustomerStatus.ACTIVE)
            .setParameter("since", since)
            .getResultList();
    }
}
```

#### Service Layer Patterns

**Application Service Pattern:**
```java
@Service
@Transactional
public class CustomerApplicationService {
    
    private final CustomerRepository customerRepository;
    private final CustomerDomainService customerDomainService;
    private final EventPublisher eventPublisher;
    
    public CustomerApplicationService(CustomerRepository customerRepository,
                                    CustomerDomainService customerDomainService,
                                    EventPublisher eventPublisher) {
        this.customerRepository = customerRepository;
        this.customerDomainService = customerDomainService;
        this.eventPublisher = eventPublisher;
    }
    
    public CustomerResponse createCustomer(CreateCustomerCommand command) {
        // 1. Validate command
        validateCreateCustomerCommand(command);
        
        // 2. Check business rules
        customerDomainService.validateCustomerCreation(command);
        
        // 3. Create domain object
        Customer customer = Customer.create(command.getProfile(), command.getContacts());
        
        // 4. Persist
        customer = customerRepository.save(customer);
        
        // 5. Publish domain event
        eventPublisher.publishEvent(new CustomerCreatedEvent(customer.getId()));
        
        // 6. Return response
        return CustomerMapper.toResponse(customer);
    }
}
```

**Domain Service Pattern:**
```java
@Component
public class CustomerDomainService {
    
    private final CustomerRepository customerRepository;
    private final EmailValidationService emailValidationService;
    
    public CustomerDomainService(CustomerRepository customerRepository,
                               EmailValidationService emailValidationService) {
        this.customerRepository = customerRepository;
        this.emailValidationService = emailValidationService;
    }
    
    public void validateCustomerCreation(CreateCustomerCommand command) {
        // Business rule: Email must be unique
        if (customerRepository.findByEmail(command.getEmail()).isPresent()) {
            throw new DuplicateCustomerException("Customer with email already exists");
        }
        
        // Business rule: Email must be valid
        if (!emailValidationService.isValidBusinessEmail(command.getEmail())) {
            throw new InvalidEmailException("Invalid business email format");
        }
    }
    
    public boolean canUpgradeCustomer(Customer customer, SubscriptionTier newTier) {
        // Complex business logic for upgrade eligibility
        return customer.getStatus() == CustomerStatus.ACTIVE &&
               customer.getPaymentHistory().hasSuccessfulPayments() &&
               newTier.isUpgradeFrom(customer.getCurrentTier());
    }
}
```

### Inter-Service Communication Patterns and Error Handling

#### Synchronous Communication (REST)

**Service Client Pattern:**
```java
@Component
public class PaymentServiceClient {
    
    private final WebClient webClient;
    private final PaymentServiceProperties properties;
    private final CircuitBreaker circuitBreaker;
    
    public PaymentServiceClient(WebClient.Builder webClientBuilder,
                               PaymentServiceProperties properties,
                               CircuitBreaker circuitBreaker) {
        this.webClient = webClientBuilder
                .baseUrl(properties.getBaseUrl())
                .build();
        this.properties = properties;
        this.circuitBreaker = circuitBreaker;
    }
    
    public Mono<PaymentResponse> processPayment(PaymentRequest request) {
        return circuitBreaker.executeSupplier(() ->
            webClient.post()
                .uri("/payments")
                .body(BodyInserters.fromValue(request))
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, this::handleClientError)
                .onStatus(HttpStatus::is5xxServerError, this::handleServerError)
                .bodyToMono(PaymentResponse.class)
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .retry(properties.getMaxRetries())
        );
    }
    
    private Mono<? extends Throwable> handleClientError(ClientResponse response) {
        return response.bodyToMono(ErrorResponse.class)
                .map(error -> new PaymentValidationException(error.getMessage()));
    }
    
    private Mono<? extends Throwable> handleServerError(ClientResponse response) {
        return Mono.error(new PaymentServiceException("Payment service unavailable"));
    }
}
```

**Circuit Breaker Configuration:**
```java
@Configuration
public class ResilienceConfig {
    
    @Bean
    public CircuitBreaker paymentServiceCircuitBreaker() {
        return CircuitBreaker.ofDefaults("paymentService")
                .toBuilder()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .build();
    }
    
    @Bean
    public RetryConfig defaultRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(ConnectException.class, SocketTimeoutException.class)
                .ignoreExceptions(PaymentValidationException.class)
                .build();
    }
}
```

#### Asynchronous Communication (Events)

**Event Publishing Pattern:**
```java
@Component
public class DomainEventPublisher {
    
    private final ApplicationEventPublisher applicationEventPublisher;
    private final MessageBrokerTemplate messageBrokerTemplate;
    
    public DomainEventPublisher(ApplicationEventPublisher applicationEventPublisher,
                               MessageBrokerTemplate messageBrokerTemplate) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.messageBrokerTemplate = messageBrokerTemplate;
    }
    
    public void publishEvent(DomainEvent event) {
        // Local event for same service
        applicationEventPublisher.publishEvent(event);
        
        // External event for other services
        messageBrokerTemplate.send(event.getEventType(), event);
    }
}

@EventListener
@Async
public class CustomerEventHandler {
    
    private final NotificationService notificationService;
    private final AuditService auditService;
    
    @EventListener
    public void handleCustomerCreated(CustomerCreatedEvent event) {
        try {
            notificationService.sendWelcomeEmail(event.getCustomerId());
            auditService.logCustomerCreation(event);
        } catch (Exception e) {
            log.error("Failed to handle customer created event", e);
            // Consider dead letter queue or retry mechanism
        }
    }
}
```

### Data Access Layer Patterns and Database Interaction Guidelines

#### Repository Implementation Patterns

**Base Repository Pattern:**
```java
public abstract class BaseRepository<T, ID> {
    
    protected final EntityManager entityManager;
    protected final Class<T> entityClass;
    
    protected BaseRepository(EntityManager entityManager, Class<T> entityClass) {
        this.entityManager = entityManager;
        this.entityClass = entityClass;
    }
    
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(entityManager.find(entityClass, id));
    }
    
    public T save(T entity) {
        if (isNew(entity)) {
            entityManager.persist(entity);
            return entity;
        } else {
            return entityManager.merge(entity);
        }
    }
    
    public void delete(T entity) {
        if (entityManager.contains(entity)) {
            entityManager.remove(entity);
        } else {
            entityManager.remove(entityManager.merge(entity));
        }
    }
    
    protected abstract boolean isNew(T entity);
    
    protected TypedQuery<T> createQuery(String jpql) {
        return entityManager.createQuery(jpql, entityClass);
    }
}
```

**Specification Pattern for Dynamic Queries:**
```java
public class CustomerSpecifications {
    
    public static Specification<Customer> hasStatus(CustomerStatus status) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);
    }
    
    public static Specification<Customer> hasEmailContaining(String email) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")),
                        "%" + email.toLowerCase() + "%"
                );
    }
    
    public static Specification<Customer> createdAfter(LocalDateTime date) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThan(root.get("createdAt"), date);
    }
    
    public static Specification<Customer> withActiveSubscription() {
        return (root, query, criteriaBuilder) -> {
            Join<Customer, Subscription> subscriptionJoin = root.join("subscriptions");
            return criteriaBuilder.equal(
                    subscriptionJoin.get("status"),
                    SubscriptionStatus.ACTIVE
            );
        };
    }
}

// Usage in service
public Page<Customer> searchCustomers(CustomerSearchCriteria criteria, Pageable pageable) {
    Specification<Customer> spec = Specification.where(null);
    
    if (criteria.getStatus() != null) {
        spec = spec.and(CustomerSpecifications.hasStatus(criteria.getStatus()));
    }
    
    if (StringUtils.hasText(criteria.getEmail())) {
        spec = spec.and(CustomerSpecifications.hasEmailContaining(criteria.getEmail()));
    }
    
    if (criteria.getCreatedAfter() != null) {
        spec = spec.and(CustomerSpecifications.createdAfter(criteria.getCreatedAfter()));
    }
    
    return customerRepository.findAll(spec, pageable);
}
```

#### Transaction Management Patterns

**Declarative Transaction Management:**
```java
@Service
@Transactional
public class OrderProcessingService {
    
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse processOrder(OrderRequest request) {
        // All operations in single transaction
        Order order = createOrder(request);
        reserveInventory(order);
        processPayment(order);
        
        return OrderMapper.toResponse(order);
    }
    
    @Transactional(readOnly = true)
    public List<OrderSummary> getOrderHistory(Long customerId) {
        return orderRepository.findOrderSummariesByCustomerId(customerId);
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logOrderEvent(OrderEvent event) {
        // This runs in separate transaction
        orderEventRepository.save(event);
    }
}
```

### Service Testing Patterns and Mock Strategies

#### Unit Testing Patterns

**Service Layer Testing:**
```java
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private EventPublisher eventPublisher;
    
    @InjectMocks
    private CustomerService customerService;
    
    @Test
    void shouldCreateCustomerSuccessfully() {
        // Given
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .email("test@example.com")
                .name("John Doe")
                .build();
        
        Customer savedCustomer = Customer.builder()
                .id(1L)
                .email(request.getEmail())
                .name(request.getName())
                .status(CustomerStatus.ACTIVE)
                .build();
        
        when(customerRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class)))
                .thenReturn(savedCustomer);
        
        // When
        CustomerResponse response = customerService.createCustomer(request);
        
        // Then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        assertThat(response.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        
        verify(customerRepository).findByEmail(request.getEmail());
        verify(customerRepository).save(any(Customer.class));
        verify(eventPublisher).publishEvent(any(CustomerCreatedEvent.class));
    }
    
    @Test
    void shouldThrowExceptionWhenCustomerAlreadyExists() {
        // Given
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .email("existing@example.com")
                .build();
        
        when(customerRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(mock(Customer.class)));
        
        // When & Then
        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(DuplicateCustomerException.class)
                .hasMessage("Customer with email already exists");
        
        verify(customerRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
```

#### Integration Testing Patterns

**Repository Integration Testing:**
```java
@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb"
})
class CustomerRepositoryIntegrationTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Test
    void shouldFindActiveCustomersWithRecentActivity() {
        // Given
        LocalDateTime recentDate = LocalDateTime.now().minusDays(7);
        
        Customer activeCustomer = Customer.builder()
                .email("active@example.com")
                .status(CustomerStatus.ACTIVE)
                .lastActivity(LocalDateTime.now().minusDays(3))
                .build();
        
        Customer inactiveCustomer = Customer.builder()
                .email("inactive@example.com")
                .status(CustomerStatus.INACTIVE)
                .lastActivity(LocalDateTime.now().minusDays(3))
                .build();
        
        entityManager.persistAndFlush(activeCustomer);
        entityManager.persistAndFlush(inactiveCustomer);
        
        // When
        List<Customer> result = customerRepository
                .findActiveCustomersWithRecentActivity(recentDate);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("active@example.com");
    }
}
```

#### Test Containers for External Dependencies

**Database Integration Testing:**
```java
@SpringBootTest
@Testcontainers
class CustomerServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private CustomerService customerService;
    
    @Test
    void shouldPersistCustomerToDatabase() {
        // Given
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .email("integration@example.com")
                .name("Integration Test")
                .build();
        
        // When
        CustomerResponse response = customerService.createCustomer(request);
        
        // Then
        assertThat(response.getId()).isNotNull();
        
        // Verify persistence
        Optional<Customer> saved = customerService.findById(response.getId());
        assertThat(saved).isPresent();
        assertThat(saved.get().getEmail()).isEqualTo(request.getEmail());
    }
}
```

#### Mock Strategies for External Services

**WireMock for HTTP Services:**
```java
@SpringBootTest
class PaymentServiceClientTest {
    
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(8089))
            .build();
    
    @Autowired
    private PaymentServiceClient paymentServiceClient;
    
    @Test
    void shouldProcessPaymentSuccessfully() {
        // Given
        PaymentRequest request = PaymentRequest.builder()
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .build();
        
        PaymentResponse expectedResponse = PaymentResponse.builder()
                .transactionId("txn-123")
                .status(PaymentStatus.SUCCESS)
                .build();
        
        wireMock.stubFor(post(urlEqualTo("/payments"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(expectedResponse))));
        
        // When
        PaymentResponse response = paymentServiceClient.processPayment(request).block();
        
        // Then
        assertThat(response.getTransactionId()).isEqualTo("txn-123");
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }
}
```