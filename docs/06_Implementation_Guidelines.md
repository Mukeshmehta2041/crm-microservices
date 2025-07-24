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
```---


## Message Broker Integration Patterns

### Event Publishing and Consumption Patterns

#### Domain Event Publishing

**Event Definition:**
```java
public abstract class DomainEvent {
    private final String eventId;
    private final LocalDateTime occurredAt;
    private final String eventType;
    private final String aggregateId;
    private final Long version;
    
    protected DomainEvent(String aggregateId, Long version) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
        this.eventType = this.getClass().getSimpleName();
        this.aggregateId = aggregateId;
        this.version = version;
    }
    
    // Getters and abstract methods
    public abstract String getEventType();
}

@JsonTypeName("CustomerCreated")
public class CustomerCreatedEvent extends DomainEvent {
    private final String customerId;
    private final String email;
    private final String name;
    private final CustomerStatus status;
    
    public CustomerCreatedEvent(String customerId, String email, String name, CustomerStatus status, Long version) {
        super(customerId, version);
        this.customerId = customerId;
        this.email = email;
        this.name = name;
        this.status = status;
    }
    
    @Override
    public String getEventType() {
        return "customer.created";
    }
}
```

**Event Publisher Implementation:**
```java
@Component
public class EventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    private final EventStore eventStore;
    private final ObjectMapper objectMapper;
    
    public EventPublisher(RabbitTemplate rabbitTemplate, 
                         EventStore eventStore,
                         ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.eventStore = eventStore;
        this.objectMapper = objectMapper;
    }
    
    @Transactional
    public void publishEvent(DomainEvent event) {
        try {
            // 1. Store event for audit and replay
            eventStore.save(event);
            
            // 2. Publish to message broker
            String routingKey = event.getEventType();
            String eventPayload = objectMapper.writeValueAsString(event);
            
            rabbitTemplate.convertAndSend(
                "crm.events.exchange",
                routingKey,
                eventPayload,
                message -> {
                    message.getMessageProperties().setMessageId(event.getEventId());
                    message.getMessageProperties().setTimestamp(
                        Date.from(event.getOccurredAt().atZone(ZoneId.systemDefault()).toInstant())
                    );
                    message.getMessageProperties().setHeader("eventType", event.getEventType());
                    message.getMessageProperties().setHeader("aggregateId", event.getAggregateId());
                    return message;
                }
            );
            
            log.info("Published event: {} for aggregate: {}", event.getEventType(), event.getAggregateId());
            
        } catch (Exception e) {
            log.error("Failed to publish event: {}", event.getEventType(), e);
            throw new EventPublishingException("Failed to publish event", e);
        }
    }
}
```

#### Event Consumer Patterns

**Message Listener Configuration:**
```java
@Configuration
@EnableRabbit
public class RabbitConfig {
    
    @Bean
    public TopicExchange crmEventsExchange() {
        return ExchangeBuilder
                .topicExchange("crm.events.exchange")
                .durable(true)
                .build();
    }
    
    @Bean
    public Queue customerEventsQueue() {
        return QueueBuilder
                .durable("customer.events.queue")
                .withArgument("x-dead-letter-exchange", "crm.dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "customer.events.dlq")
                .build();
    }
    
    @Bean
    public Binding customerEventsBinding() {
        return BindingBuilder
                .bind(customerEventsQueue())
                .to(crmEventsExchange())
                .with("customer.*");
    }
    
    @Bean
    public RabbitListenerContainerFactory<SimpleMessageListenerContainer> rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setDefaultRequeueRejected(false);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        return factory;
    }
}
```

**Event Consumer Implementation:**
```java
@Component
@RabbitListener(queues = "customer.events.queue")
public class CustomerEventConsumer {
    
    private final NotificationService notificationService;
    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper;
    
    public CustomerEventConsumer(NotificationService notificationService,
                                AnalyticsService analyticsService,
                                ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.analyticsService = analyticsService;
        this.objectMapper = objectMapper;
    }
    
    @RabbitHandler
    public void handleCustomerCreated(
            @Payload String eventPayload,
            @Header Map<String, Object> headers,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        
        try {
            CustomerCreatedEvent event = objectMapper.readValue(eventPayload, CustomerCreatedEvent.class);
            
            log.info("Processing customer created event: {}", event.getCustomerId());
            
            // Process the event
            processCustomerCreatedEvent(event);
            
            // Manual acknowledgment if needed
            // channel.basicAck(deliveryTag, false);
            
        } catch (BusinessException e) {
            log.warn("Business error processing customer created event: {}", e.getMessage());
            // Don't retry for business exceptions
            // channel.basicReject(deliveryTag, false);
            
        } catch (Exception e) {
            log.error("Technical error processing customer created event", e);
            // Let the message be requeued for retry
            throw new AmqpRejectAndDontRequeueException("Processing failed", e);
        }
    }
    
    private void processCustomerCreatedEvent(CustomerCreatedEvent event) {
        // Send welcome email
        notificationService.sendWelcomeEmail(event.getCustomerId(), event.getEmail());
        
        // Update analytics
        analyticsService.trackCustomerRegistration(event.getCustomerId());
        
        // Additional processing...
    }
}
```

### Message Serialization and Versioning Strategies

#### Event Schema Evolution

**Versioned Event Schema:**
```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventVersion")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CustomerCreatedEventV1.class, name = "v1"),
    @JsonSubTypes.Type(value = CustomerCreatedEventV2.class, name = "v2")
})
public abstract class CustomerCreatedEvent extends DomainEvent {
    public abstract String getEventVersion();
}

// Version 1
public class CustomerCreatedEventV1 extends CustomerCreatedEvent {
    private final String customerId;
    private final String email;
    private final String name;
    
    @Override
    public String getEventVersion() {
        return "v1";
    }
}

// Version 2 - Added additional fields
public class CustomerCreatedEventV2 extends CustomerCreatedEvent {
    private final String customerId;
    private final String email;
    private final String name;
    private final String phoneNumber;  // New field
    private final Address address;     // New field
    
    @Override
    public String getEventVersion() {
        return "v2";
    }
}
```

**Event Upcasting Strategy:**
```java
@Component
public class EventUpcaster {
    
    private final ObjectMapper objectMapper;
    
    public EventUpcaster(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    public DomainEvent upcastEvent(String eventPayload, String eventType, String version) {
        try {
            switch (eventType) {
                case "customer.created":
                    return upcastCustomerCreatedEvent(eventPayload, version);
                default:
                    throw new UnsupportedEventTypeException("Unknown event type: " + eventType);
            }
        } catch (Exception e) {
            throw new EventUpcastingException("Failed to upcast event", e);
        }
    }
    
    private CustomerCreatedEvent upcastCustomerCreatedEvent(String payload, String version) throws JsonProcessingException {
        switch (version) {
            case "v1":
                CustomerCreatedEventV1 v1Event = objectMapper.readValue(payload, CustomerCreatedEventV1.class);
                // Convert v1 to v2 with default values
                return new CustomerCreatedEventV2(
                    v1Event.getCustomerId(),
                    v1Event.getEmail(),
                    v1Event.getName(),
                    null, // phoneNumber - default to null
                    null  // address - default to null
                );
            case "v2":
                return objectMapper.readValue(payload, CustomerCreatedEventV2.class);
            default:
                throw new UnsupportedEventVersionException("Unsupported version: " + version);
        }
    }
}
```

#### Avro Schema Registry Integration

**Avro Event Schema:**
```json
{
  "type": "record",
  "name": "CustomerCreatedEvent",
  "namespace": "com.crmplatform.events",
  "fields": [
    {"name": "eventId", "type": "string"},
    {"name": "customerId", "type": "string"},
    {"name": "email", "type": "string"},
    {"name": "name", "type": "string"},
    {"name": "phoneNumber", "type": ["null", "string"], "default": null},
    {"name": "address", "type": ["null", {
      "type": "record",
      "name": "Address",
      "fields": [
        {"name": "street", "type": "string"},
        {"name": "city", "type": "string"},
        {"name": "country", "type": "string"}
      ]
    }], "default": null},
    {"name": "occurredAt", "type": "long", "logicalType": "timestamp-millis"}
  ]
}
```

**Avro Serialization Configuration:**
```java
@Configuration
public class AvroSerializationConfig {
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
    
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put("schema.registry.url", "http://localhost:8081");
        props.put("auto.register.schemas", false);
        props.put("use.latest.version", true);
        
        return new DefaultKafkaProducerFactory<>(props);
    }
}
```

### Dead Letter Queue Handling and Retry Mechanisms

#### Dead Letter Queue Configuration

**DLQ Setup:**
```java
@Configuration
public class DeadLetterQueueConfig {
    
    @Bean
    public TopicExchange deadLetterExchange() {
        return ExchangeBuilder
                .topicExchange("crm.dlx.exchange")
                .durable(true)
                .build();
    }
    
    @Bean
    public Queue customerEventsDLQ() {
        return QueueBuilder
                .durable("customer.events.dlq")
                .withArgument("x-message-ttl", 86400000) // 24 hours
                .build();
    }
    
    @Bean
    public Binding customerEventsDLQBinding() {
        return BindingBuilder
                .bind(customerEventsDLQ())
                .to(deadLetterExchange())
                .with("customer.events.dlq");
    }
}
```

**Retry Mechanism with Exponential Backoff:**
```java
@Component
public class RetryableEventProcessor {
    
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_DELAY_MS = 1000;
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public RetryableEventProcessor(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @RabbitListener(queues = "customer.events.queue")
    public void processEvent(
            @Payload String eventPayload,
            @Header Map<String, Object> headers,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        
        String eventId = (String) headers.get("eventId");
        String retryKey = "retry:" + eventId;
        
        try {
            // Get current retry count
            Integer retryCount = (Integer) redisTemplate.opsForValue().get(retryKey);
            if (retryCount == null) {
                retryCount = 0;
            }
            
            // Process the event
            processEventInternal(eventPayload);
            
            // Success - remove retry counter
            redisTemplate.delete(retryKey);
            
        } catch (RetryableException e) {
            handleRetryableException(eventPayload, eventId, retryKey, retryCount, channel, deliveryTag, e);
            
        } catch (NonRetryableException e) {
            log.error("Non-retryable error processing event {}: {}", eventId, e.getMessage());
            sendToDeadLetterQueue(eventPayload, headers, e);
            
        } catch (Exception e) {
            log.error("Unexpected error processing event {}", eventId, e);
            handleRetryableException(eventPayload, eventId, retryKey, retryCount, channel, deliveryTag, e);
        }
    }
    
    private void handleRetryableException(String eventPayload, String eventId, String retryKey, 
                                        Integer retryCount, Channel channel, long deliveryTag, Exception e) {
        if (retryCount >= MAX_RETRY_ATTEMPTS) {
            log.error("Max retry attempts exceeded for event {}", eventId);
            sendToDeadLetterQueue(eventPayload, Map.of("eventId", eventId), e);
            return;
        }
        
        // Increment retry count
        retryCount++;
        redisTemplate.opsForValue().set(retryKey, retryCount, Duration.ofHours(1));
        
        // Calculate delay with exponential backoff
        long delay = INITIAL_DELAY_MS * (long) Math.pow(2, retryCount - 1);
        
        log.warn("Retrying event {} (attempt {}/{}) after {}ms delay", 
                eventId, retryCount, MAX_RETRY_ATTEMPTS, delay);
        
        // Schedule retry
        scheduleRetry(eventPayload, eventId, delay);
    }
    
    private void scheduleRetry(String eventPayload, String eventId, long delayMs) {
        // Use RabbitMQ delayed message plugin or Redis for scheduling
        rabbitTemplate.convertAndSend(
            "crm.retry.exchange",
            "customer.events.retry",
            eventPayload,
            message -> {
                message.getMessageProperties().setDelay((int) delayMs);
                message.getMessageProperties().setHeader("eventId", eventId);
                return message;
            }
        );
    }
    
    private void sendToDeadLetterQueue(String eventPayload, Map<String, Object> headers, Exception error) {
        rabbitTemplate.convertAndSend(
            "crm.dlx.exchange",
            "customer.events.dlq",
            eventPayload,
            message -> {
                message.getMessageProperties().setHeader("originalError", error.getMessage());
                message.getMessageProperties().setHeader("failedAt", System.currentTimeMillis());
                headers.forEach((key, value) -> 
                    message.getMessageProperties().setHeader(key, value));
                return message;
            }
        );
    }
}
```

### Event Sourcing Patterns for Audit and Replay Capabilities

#### Event Store Implementation

**Event Store Interface:**
```java
public interface EventStore {
    void save(DomainEvent event);
    void save(List<DomainEvent> events);
    List<DomainEvent> getEvents(String aggregateId);
    List<DomainEvent> getEvents(String aggregateId, Long fromVersion);
    List<DomainEvent> getAllEvents(LocalDateTime from, LocalDateTime to);
    Optional<DomainEvent> getEvent(String eventId);
}

@Repository
public class JpaEventStore implements EventStore {
    
    private final EventStoreRepository repository;
    private final ObjectMapper objectMapper;
    
    public JpaEventStore(EventStoreRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }
    
    @Override
    @Transactional
    public void save(DomainEvent event) {
        try {
            EventStoreEntry entry = EventStoreEntry.builder()
                    .eventId(event.getEventId())
                    .aggregateId(event.getAggregateId())
                    .eventType(event.getEventType())
                    .eventData(objectMapper.writeValueAsString(event))
                    .version(event.getVersion())
                    .occurredAt(event.getOccurredAt())
                    .build();
            
            repository.save(entry);
            
        } catch (JsonProcessingException e) {
            throw new EventSerializationException("Failed to serialize event", e);
        }
    }
    
    @Override
    public List<DomainEvent> getEvents(String aggregateId) {
        return repository.findByAggregateIdOrderByVersionAsc(aggregateId)
                .stream()
                .map(this::deserializeEvent)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<DomainEvent> getEvents(String aggregateId, Long fromVersion) {
        return repository.findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(aggregateId, fromVersion)
                .stream()
                .map(this::deserializeEvent)
                .collect(Collectors.toList());
    }
    
    private DomainEvent deserializeEvent(EventStoreEntry entry) {
        try {
            Class<? extends DomainEvent> eventClass = getEventClass(entry.getEventType());
            return objectMapper.readValue(entry.getEventData(), eventClass);
        } catch (Exception e) {
            throw new EventDeserializationException("Failed to deserialize event", e);
        }
    }
}
```

#### Aggregate Reconstruction from Events

**Event Sourced Aggregate:**
```java
public abstract class EventSourcedAggregate {
    
    protected String id;
    protected Long version = 0L;
    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();
    
    protected void applyEvent(DomainEvent event) {
        applyEventInternal(event);
        uncommittedEvents.add(event);
    }
    
    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }
    
    public List<DomainEvent> getUncommittedEvents() {
        return new ArrayList<>(uncommittedEvents);
    }
    
    public void loadFromHistory(List<DomainEvent> events) {
        events.forEach(this::applyEventInternal);
    }
    
    private void applyEventInternal(DomainEvent event) {
        try {
            Method method = this.getClass().getDeclaredMethod("on", event.getClass());
            method.setAccessible(true);
            method.invoke(this, event);
            this.version = event.getVersion();
        } catch (Exception e) {
            throw new EventApplicationException("Failed to apply event", e);
        }
    }
}

public class Customer extends EventSourcedAggregate {
    
    private String email;
    private String name;
    private CustomerStatus status;
    private LocalDateTime createdAt;
    
    // Factory method
    public static Customer create(String email, String name) {
        Customer customer = new Customer();
        customer.applyEvent(new CustomerCreatedEvent(
            UUID.randomUUID().toString(),
            email,
            name,
            CustomerStatus.ACTIVE,
            1L
        ));
        return customer;
    }
    
    // Event handlers
    private void on(CustomerCreatedEvent event) {
        this.id = event.getCustomerId();
        this.email = event.getEmail();
        this.name = event.getName();
        this.status = event.getStatus();
        this.createdAt = event.getOccurredAt();
    }
    
    private void on(CustomerEmailUpdatedEvent event) {
        this.email = event.getNewEmail();
    }
    
    private void on(CustomerStatusChangedEvent event) {
        this.status = event.getNewStatus();
    }
    
    // Business methods
    public void updateEmail(String newEmail) {
        if (!Objects.equals(this.email, newEmail)) {
            applyEvent(new CustomerEmailUpdatedEvent(
                this.id,
                this.email,
                newEmail,
                this.version + 1
            ));
        }
    }
}
```

#### Event Replay and Projection Rebuilding

**Projection Rebuilder:**
```java
@Component
public class ProjectionRebuilder {
    
    private final EventStore eventStore;
    private final List<EventProjection> projections;
    
    public ProjectionRebuilder(EventStore eventStore, List<EventProjection> projections) {
        this.eventStore = eventStore;
        this.projections = projections;
    }
    
    @Async
    public CompletableFuture<Void> rebuildProjections(LocalDateTime from, LocalDateTime to) {
        log.info("Starting projection rebuild from {} to {}", from, to);
        
        try {
            // Clear existing projections
            projections.forEach(EventProjection::clear);
            
            // Replay events
            List<DomainEvent> events = eventStore.getAllEvents(from, to);
            
            for (DomainEvent event : events) {
                projections.forEach(projection -> {
                    try {
                        projection.handle(event);
                    } catch (Exception e) {
                        log.error("Failed to apply event {} to projection {}", 
                                event.getEventId(), projection.getClass().getSimpleName(), e);
                    }
                });
            }
            
            log.info("Completed projection rebuild. Processed {} events", events.size());
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("Failed to rebuild projections", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}

public interface EventProjection {
    void handle(DomainEvent event);
    void clear();
}

@Component
public class CustomerSummaryProjection implements EventProjection {
    
    private final CustomerSummaryRepository repository;
    
    public CustomerSummaryProjection(CustomerSummaryRepository repository) {
        this.repository = repository;
    }
    
    @Override
    public void handle(DomainEvent event) {
        if (event instanceof CustomerCreatedEvent) {
            handleCustomerCreated((CustomerCreatedEvent) event);
        } else if (event instanceof CustomerEmailUpdatedEvent) {
            handleCustomerEmailUpdated((CustomerEmailUpdatedEvent) event);
        }
    }
    
    private void handleCustomerCreated(CustomerCreatedEvent event) {
        CustomerSummary summary = CustomerSummary.builder()
                .customerId(event.getCustomerId())
                .email(event.getEmail())
                .name(event.getName())
                .status(event.getStatus())
                .createdAt(event.getOccurredAt())
                .build();
        
        repository.save(summary);
    }
    
    private void handleCustomerEmailUpdated(CustomerEmailUpdatedEvent event) {
        repository.findByCustomerId(event.getCustomerId())
                .ifPresent(summary -> {
                    summary.setEmail(event.getNewEmail());
                    repository.save(summary);
                });
    }
    
    @Override
    public void clear() {
        repository.deleteAll();
    }
}
```---

#
# Version Control and Branching Strategy

### Git Workflow and Branching Model for Team Collaboration

#### GitFlow-Based Branching Strategy

**Branch Structure:**
```
main (production-ready code)
├── develop (integration branch)
│   ├── feature/CRM-123-customer-management
│   ├── feature/CRM-124-payment-integration
│   └── feature/CRM-125-notification-service
├── release/v1.2.0 (release preparation)
├── hotfix/v1.1.1-critical-security-fix
└── support/v1.0.x (long-term support)
```

**Branch Types and Purposes:**

**Main Branch:**
- Contains production-ready code
- Protected branch with strict access controls
- All commits must be signed and verified
- Automatic deployment to production environment
- Tagged with semantic version numbers

**Develop Branch:**
- Integration branch for ongoing development
- Base branch for all feature branches
- Continuous integration and testing
- Automatic deployment to development environment

**Feature Branches:**
```bash
# Naming convention: feature/TICKET-ID-short-description
feature/CRM-123-customer-management
feature/CRM-124-payment-integration
feature/CRM-125-notification-service

# Creation and workflow
git checkout develop
git pull origin develop
git checkout -b feature/CRM-123-customer-management
# ... development work ...
git push origin feature/CRM-123-customer-management
# Create pull request to develop
```

**Release Branches:**
```bash
# Naming convention: release/vX.Y.Z
release/v1.2.0

# Creation workflow
git checkout develop
git pull origin develop
git checkout -b release/v1.2.0
# Update version numbers, final testing
git push origin release/v1.2.0
# Merge to main and develop when ready
```

**Hotfix Branches:**
```bash
# Naming convention: hotfix/vX.Y.Z-description
hotfix/v1.1.1-critical-security-fix

# Creation workflow
git checkout main
git pull origin main
git checkout -b hotfix/v1.1.1-critical-security-fix
# Fix the issue
git push origin hotfix/v1.1.1-critical-security-fix
# Merge to main and develop
```

#### Commit Message Standards

**Conventional Commits Format:**
```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

**Commit Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks
- `perf`: Performance improvements
- `ci`: CI/CD changes

**Examples:**
```bash
feat(customer): add customer profile management API

Implement CRUD operations for customer profiles including:
- Create customer profile with validation
- Update profile information
- Retrieve profile by ID or email
- Soft delete customer profiles

Closes #CRM-123

fix(payment): resolve payment processing timeout issue

The payment service was timing out due to insufficient connection pool size.
Increased the pool size from 10 to 50 connections and added retry logic.

Fixes #CRM-456

docs(api): update customer API documentation

- Add examples for all customer endpoints
- Document error response formats
- Update authentication requirements

chore(deps): upgrade Spring Boot to 2.7.5

- Update Spring Boot from 2.7.3 to 2.7.5
- Update related dependencies
- Fix compatibility issues
```

### Code Review Process and Approval Requirements

#### Pull Request Requirements

**Pre-PR Checklist:**
- [ ] Feature branch is up to date with target branch
- [ ] All tests pass locally
- [ ] Code follows established style guidelines
- [ ] Documentation is updated
- [ ] No merge conflicts exist
- [ ] Commit messages follow conventional format

**PR Template:**
```markdown
## Description
Brief description of changes and motivation.

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing completed
- [ ] Performance impact assessed

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No new warnings introduced
- [ ] Database migrations included (if applicable)

## Related Issues
Closes #CRM-123
Related to #CRM-124

## Screenshots (if applicable)
[Add screenshots for UI changes]

## Additional Notes
[Any additional information for reviewers]
```

#### Review Process Workflow

**Automated Checks:**
```yaml
# GitHub Actions workflow for PR validation
name: Pull Request Validation
on:
  pull_request:
    branches: [develop, main]

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          
      - name: Run Tests
        run: ./gradlew test
        
      - name: Code Quality Check
        run: ./gradlew sonarqube
        
      - name: Security Scan
        run: ./gradlew dependencyCheckAnalyze
        
      - name: Build Application
        run: ./gradlew build
```

**Review Approval Matrix:**

| Target Branch | Required Reviewers | Approval Requirements |
|---------------|-------------------|----------------------|
| `develop` | 1 team member | 1 approval + CI passing |
| `main` | 2 senior developers | 2 approvals + CI passing + security scan |
| `release/*` | Tech lead + 1 senior | 2 approvals + full test suite |
| `hotfix/*` | Tech lead | 1 approval + expedited review |

**Review Guidelines:**

**Code Quality Review:**
- [ ] Code is readable and well-structured
- [ ] Follows established patterns and conventions
- [ ] No code duplication
- [ ] Appropriate error handling
- [ ] Performance considerations addressed
- [ ] Security best practices followed

**Architecture Review:**
- [ ] Changes align with system architecture
- [ ] Proper separation of concerns
- [ ] Database changes are backward compatible
- [ ] API changes maintain backward compatibility
- [ ] Integration points are well-defined

**Testing Review:**
- [ ] Adequate test coverage
- [ ] Tests are meaningful and maintainable
- [ ] Edge cases are covered
- [ ] Integration tests for service interactions
- [ ] Performance tests for critical paths

### Release Management and Tagging Strategies

#### Semantic Versioning

**Version Format:** `MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]`

**Version Increment Rules:**
- **MAJOR**: Breaking changes or significant architectural changes
- **MINOR**: New features, backward-compatible changes
- **PATCH**: Bug fixes, security patches

**Examples:**
```
1.0.0     - Initial release
1.1.0     - New customer management features
1.1.1     - Bug fixes for customer module
1.2.0     - Payment integration feature
2.0.0     - Major API redesign (breaking changes)
2.0.0-rc.1 - Release candidate
2.0.0+20231201 - Build metadata
```

#### Release Process

**Release Preparation:**
```bash
# 1. Create release branch
git checkout develop
git pull origin develop
git checkout -b release/v1.2.0

# 2. Update version numbers
./scripts/update-version.sh 1.2.0

# 3. Update changelog
./scripts/generate-changelog.sh v1.1.0..HEAD

# 4. Commit version changes
git add .
git commit -m "chore(release): prepare v1.2.0"
git push origin release/v1.2.0
```

**Release Finalization:**
```bash
# 1. Merge to main
git checkout main
git pull origin main
git merge --no-ff release/v1.2.0
git tag -a v1.2.0 -m "Release version 1.2.0"
git push origin main --tags

# 2. Merge back to develop
git checkout develop
git pull origin develop
git merge --no-ff release/v1.2.0
git push origin develop

# 3. Delete release branch
git branch -d release/v1.2.0
git push origin --delete release/v1.2.0
```

**Automated Release Pipeline:**
```yaml
name: Release Pipeline
on:
  push:
    tags:
      - 'v*'

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Build Release Artifacts
        run: ./gradlew build -Prelease
        
      - name: Run Full Test Suite
        run: ./gradlew test integrationTest
        
      - name: Security Scan
        run: ./gradlew dependencyCheckAnalyze
        
      - name: Build Docker Images
        run: |
          docker build -t crm-platform:${{ github.ref_name }} .
          docker tag crm-platform:${{ github.ref_name }} crm-platform:latest
          
      - name: Push to Registry
        run: |
          docker push crm-platform:${{ github.ref_name }}
          docker push crm-platform:latest
          
      - name: Deploy to Production
        run: ./scripts/deploy-production.sh ${{ github.ref_name }}
        
      - name: Create GitHub Release
        uses: actions/create-release@v1
        with:
          tag_name: ${{ github.ref }}
          release_name: Release ${{ github.ref }}
          body_path: CHANGELOG.md
```

### Contribution Guidelines for Open Source Components

#### Contributor Onboarding

**Getting Started Guide:**
```markdown
# Contributing to CRM Platform

## Development Setup

1. Fork the repository
2. Clone your fork: `git clone https://github.com/yourusername/crm-platform.git`
3. Set up upstream: `git remote add upstream https://github.com/crmplatform/crm-platform.git`
4. Install dependencies: `./gradlew build`
5. Run tests: `./gradlew test`

## Development Workflow

1. Create feature branch: `git checkout -b feature/your-feature-name`
2. Make changes following our coding standards
3. Add tests for new functionality
4. Run full test suite: `./gradlew test integrationTest`
5. Commit with conventional commit format
6. Push and create pull request

## Code Standards

- Follow Java coding standards outlined in docs/06_Implementation_Guidelines.md
- Maintain test coverage above 80%
- Update documentation for new features
- Follow security best practices
```

**Contributor License Agreement (CLA):**
```markdown
# Contributor License Agreement

By contributing to this project, you agree that:

1. You have the right to submit the contribution
2. Your contribution is licensed under the project's license
3. You grant the project maintainers perpetual rights to use your contribution
4. Your contribution does not violate any third-party rights

Please sign the CLA by commenting on your first PR with:
"I have read and agree to the Contributor License Agreement"
```

#### Community Guidelines

**Code of Conduct:**
- Be respectful and inclusive
- Provide constructive feedback
- Help newcomers get started
- Focus on technical merit
- Resolve conflicts professionally

**Issue Management:**
```markdown
# Issue Templates

## Bug Report
- Description of the bug
- Steps to reproduce
- Expected vs actual behavior
- Environment details
- Relevant logs or screenshots

## Feature Request
- Problem description
- Proposed solution
- Alternative solutions considered
- Additional context

## Documentation Improvement
- Current documentation issue
- Proposed improvement
- Affected sections
```

**Maintainer Responsibilities:**
- Review PRs within 48 hours
- Provide constructive feedback
- Maintain project roadmap
- Ensure code quality standards
- Manage releases and versioning
- Foster inclusive community

**Recognition System:**
```markdown
# Contributor Recognition

## Levels
- **Contributor**: Made accepted contributions
- **Regular Contributor**: 5+ merged PRs
- **Core Contributor**: 20+ merged PRs + significant features
- **Maintainer**: Trusted with repository access

## Benefits
- Recognition in README and releases
- Priority review for contributions
- Input on project direction
- Access to maintainer channels
```

---

## Summary

This Implementation Guidelines document provides comprehensive standards and patterns for developing the CRM platform, covering:

1. **Java Coding Standards**: Formatting, naming conventions, package organization, dependency injection, and quality gates
2. **Microservice Development Patterns**: DDD patterns, service layer design, inter-service communication, and testing strategies
3. **Message Broker Integration**: Event publishing/consumption, serialization, versioning, DLQ handling, and event sourcing
4. **Version Control Strategy**: GitFlow branching, commit standards, code review processes, release management, and contribution guidelines

These guidelines ensure consistent, maintainable, and scalable code across all development teams while supporting both internal development and open source contributions.

**Next Steps**: Proceed to Task 8 - Testing Strategy documentation to define comprehensive testing approaches for all implementation patterns outlined in this document.