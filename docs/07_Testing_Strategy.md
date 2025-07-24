# Testing Strategy

This document outlines comprehensive testing approaches for the CRM platform, covering unit, integration, contract, end-to-end, performance, and security testing strategies. The testing strategy ensures high code quality, system reliability, and maintainability across all microservices.

---

**Status**: In Progress  
**Last Updated**: January 2025  
**Version**: 1.0

## Table of Contents

1. [Unit Testing Framework and Patterns](#unit-testing-framework-and-patterns)
2. [Integration Testing Approaches](#integration-testing-approaches)
3. [End-to-End Testing Scenarios](#end-to-end-testing-scenarios)
4. [Performance and Security Testing](#performance-and-security-testing)
5. [Testing Infrastructure](#testing-infrastructure)
6. [Quality Metrics and Reporting](#quality-metrics-and-reporting)

## Unit Testing Framework and Patterns

### JUnit 5 Setup and Configuration

#### Maven Dependencies
```xml
<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.1</version>
        <scope>test</scope>
    </dependency>
    
    <!-- Mockito for mocking -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>5.7.0</version>
        <scope>test</scope>
    </dependency>
    
    <!-- Mockito JUnit Jupiter integration -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <version>5.7.0</version>
        <scope>test</scope>
    </dependency>
    
    <!-- AssertJ for fluent assertions -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>3.24.2</version>
        <scope>test</scope>
    </dependency>
    
    <!-- Spring Boot Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

#### JUnit 5 Configuration
```java
// junit-platform.properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
junit.jupiter.execution.parallel.config.strategy=dynamic
junit.jupiter.testinstance.lifecycle.default=per_class
```

### Testing Patterns for Each Microservice

#### Service Layer Testing Pattern
```java
@ExtendWith(MockitoExtension.class)
class ContactServiceTest {
    
    @Mock
    private ContactRepository contactRepository;
    
    @Mock
    private AuditService auditService;
    
    @InjectMocks
    private ContactService contactService;
    
    @Test
    @DisplayName("Should create contact successfully")
    void shouldCreateContactSuccessfully() {
        // Given
        CreateContactRequest request = CreateContactRequest.builder()
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .build();
            
        Contact savedContact = Contact.builder()
            .id(1L)
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .build();
            
        when(contactRepository.save(any(Contact.class))).thenReturn(savedContact);
        
        // When
        ContactResponse response = contactService.createContact(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
        
        verify(contactRepository).save(any(Contact.class));
        verify(auditService).logContactCreation(eq(1L), any());
    }
    
    @Test
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Given
        CreateContactRequest request = CreateContactRequest.builder()
            .email("existing@example.com")
            .build();
            
        when(contactRepository.existsByEmail("existing@example.com"))
            .thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> contactService.createContact(request))
            .isInstanceOf(ContactAlreadyExistsException.class)
            .hasMessage("Contact with email existing@example.com already exists");
            
        verify(contactRepository, never()).save(any());
    }
}
```

#### Repository Layer Testing Pattern
```java
@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb"
})
class ContactRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private ContactRepository contactRepository;
    
    @Test
    @DisplayName("Should find contact by email")
    void shouldFindContactByEmail() {
        // Given
        Contact contact = Contact.builder()
            .firstName("Jane")
            .lastName("Smith")
            .email("jane.smith@example.com")
            .build();
        entityManager.persistAndFlush(contact);
        
        // When
        Optional<Contact> found = contactRepository.findByEmail("jane.smith@example.com");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Jane");
        assertThat(found.get().getLastName()).isEqualTo("Smith");
    }
    
    @Test
    @DisplayName("Should return empty when contact not found by email")
    void shouldReturnEmptyWhenContactNotFoundByEmail() {
        // When
        Optional<Contact> found = contactRepository.findByEmail("nonexistent@example.com");
        
        // Then
        assertThat(found).isEmpty();
    }
}
```

#### Controller Layer Testing Pattern
```java
@WebMvcTest(ContactController.class)
class ContactControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ContactService contactService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("Should create contact and return 201")
    void shouldCreateContactAndReturn201() throws Exception {
        // Given
        CreateContactRequest request = CreateContactRequest.builder()
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .build();
            
        ContactResponse response = ContactResponse.builder()
            .id(1L)
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .build();
            
        when(contactService.createContact(any(CreateContactRequest.class)))
            .thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/v1/contacts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.lastName").value("Doe"))
            .andExpect(jsonPath("$.email").value("john.doe@example.com"));
            
        verify(contactService).createContact(any(CreateContactRequest.class));
    }
    
    @Test
    @DisplayName("Should return 400 for invalid request")
    void shouldReturn400ForInvalidRequest() throws Exception {
        // Given
        CreateContactRequest invalidRequest = CreateContactRequest.builder()
            .firstName("")  // Invalid: empty first name
            .build();
        
        // When & Then
        mockMvc.perform(post("/api/v1/contacts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors[0].field").value("firstName"))
            .andExpect(jsonPath("$.errors[0].message").value("First name is required"));
    }
}
```

### Mocking Strategies Using Mockito

#### External Service Mocking
```java
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
    
    @Mock
    private EmailClient emailClient;
    
    @Mock
    private TemplateService templateService;
    
    @InjectMocks
    private EmailService emailService;
    
    @Test
    @DisplayName("Should send welcome email successfully")
    void shouldSendWelcomeEmailSuccessfully() {
        // Given
        Contact contact = Contact.builder()
            .email("user@example.com")
            .firstName("John")
            .build();
            
        String template = "Welcome {{firstName}}!";
        String renderedContent = "Welcome John!";
        
        when(templateService.getTemplate("welcome")).thenReturn(template);
        when(templateService.render(template, contact)).thenReturn(renderedContent);
        when(emailClient.send(any(EmailMessage.class))).thenReturn(true);
        
        // When
        boolean result = emailService.sendWelcomeEmail(contact);
        
        // Then
        assertThat(result).isTrue();
        
        ArgumentCaptor<EmailMessage> emailCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailClient).send(emailCaptor.capture());
        
        EmailMessage sentEmail = emailCaptor.getValue();
        assertThat(sentEmail.getTo()).isEqualTo("user@example.com");
        assertThat(sentEmail.getSubject()).isEqualTo("Welcome to CRM Platform");
        assertThat(sentEmail.getContent()).isEqualTo("Welcome John!");
    }
}
```

#### Database Mocking with @MockBean
```java
@SpringBootTest
@TestPropertySource(properties = "spring.profiles.active=test")
class IntegrationServiceTest {
    
    @MockBean
    private ExternalApiClient externalApiClient;
    
    @Autowired
    private IntegrationService integrationService;
    
    @Test
    @DisplayName("Should handle external API failure gracefully")
    void shouldHandleExternalApiFailureGracefully() {
        // Given
        when(externalApiClient.fetchData(anyString()))
            .thenThrow(new ExternalApiException("Service unavailable"));
        
        // When
        Result result = integrationService.syncData("contact-123");
        
        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).contains("External service unavailable");
    }
}
```

### Test Data Management and Fixture Creation

#### Test Data Builders
```java
public class ContactTestDataBuilder {
    
    private String firstName = "John";
    private String lastName = "Doe";
    private String email = "john.doe@example.com";
    private String phone = "+1234567890";
    private Address address = AddressTestDataBuilder.aDefaultAddress();
    
    public static ContactTestDataBuilder aContact() {
        return new ContactTestDataBuilder();
    }
    
    public ContactTestDataBuilder withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }
    
    public ContactTestDataBuilder withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }
    
    public ContactTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }
    
    public ContactTestDataBuilder withPhone(String phone) {
        this.phone = phone;
        return this;
    }
    
    public ContactTestDataBuilder withAddress(Address address) {
        this.address = address;
        return this;
    }
    
    public Contact build() {
        return Contact.builder()
            .firstName(firstName)
            .lastName(lastName)
            .email(email)
            .phone(phone)
            .address(address)
            .build();
    }
    
    public CreateContactRequest buildRequest() {
        return CreateContactRequest.builder()
            .firstName(firstName)
            .lastName(lastName)
            .email(email)
            .phone(phone)
            .address(address)
            .build();
    }
}
```

#### Test Fixtures with @TestConfiguration
```java
@TestConfiguration
public class TestDataConfiguration {
    
    @Bean
    @Primary
    public TestDataFactory testDataFactory() {
        return new TestDataFactory();
    }
    
    @Component
    public static class TestDataFactory {
        
        public Contact createTestContact() {
            return ContactTestDataBuilder.aContact().build();
        }
        
        public List<Contact> createTestContacts(int count) {
            return IntStream.range(0, count)
                .mapToObj(i -> ContactTestDataBuilder.aContact()
                    .withEmail("user" + i + "@example.com")
                    .build())
                .collect(Collectors.toList());
        }
        
        public Deal createTestDeal() {
            return DealTestDataBuilder.aDeal()
                .withContact(createTestContact())
                .build();
        }
    }
}
```

#### Database Test Fixtures
```java
@Component
@Profile("test")
public class DatabaseTestFixtures {
    
    @Autowired
    private ContactRepository contactRepository;
    
    @Autowired
    private DealRepository dealRepository;
    
    @EventListener(ApplicationReadyEvent.class)
    public void loadTestData() {
        if (contactRepository.count() == 0) {
            loadContacts();
            loadDeals();
        }
    }
    
    private void loadContacts() {
        List<Contact> contacts = Arrays.asList(
            ContactTestDataBuilder.aContact()
                .withEmail("alice@example.com")
                .withFirstName("Alice")
                .build(),
            ContactTestDataBuilder.aContact()
                .withEmail("bob@example.com")
                .withFirstName("Bob")
                .build()
        );
        contactRepository.saveAll(contacts);
    }
    
    private void loadDeals() {
        Contact alice = contactRepository.findByEmail("alice@example.com").orElseThrow();
        
        Deal deal = DealTestDataBuilder.aDeal()
            .withContact(alice)
            .withAmount(BigDecimal.valueOf(10000))
            .build();
            
        dealRepository.save(deal);
    }
}
```

### Code Coverage Requirements and Measurement

#### Maven Surefire and JaCoCo Configuration
```xml
<build>
    <plugins>
        <!-- Surefire for unit tests -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.2</version>
            <configuration>
                <includes>
                    <include>**/*Test.java</include>
                    <include>**/*Tests.java</include>
                </includes>
                <excludes>
                    <exclude>**/*IntegrationTest.java</exclude>
                </excludes>
                <parallel>methods</parallel>
                <threadCount>4</threadCount>
                <forkCount>1</forkCount>
                <reuseForks>true</reuseForks>
            </configuration>
        </plugin>
        
        <!-- JaCoCo for code coverage -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
                <execution>
                    <id>check</id>
                    <goals>
                        <goal>check</goal>
                    </goals>
                    <configuration>
                        <rules>
                            <rule>
                                <element>BUNDLE</element>
                                <limits>
                                    <limit>
                                        <counter>LINE</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.80</minimum>
                                    </limit>
                                    <limit>
                                        <counter>BRANCH</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.75</minimum>
                                    </limit>
                                </limits>
                            </rule>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

#### Coverage Requirements by Layer

| Layer | Line Coverage | Branch Coverage | Notes |
|-------|---------------|-----------------|-------|
| Service Layer | 90% | 85% | Core business logic requires high coverage |
| Repository Layer | 85% | 80% | Data access patterns |
| Controller Layer | 80% | 75% | API endpoints and validation |
| Utility Classes | 95% | 90% | Shared utilities must be thoroughly tested |
| Configuration | 70% | 65% | Configuration classes |

#### Coverage Exclusions
```xml
<!-- JaCoCo exclusions -->
<configuration>
    <excludes>
        <exclude>**/config/**</exclude>
        <exclude>**/dto/**</exclude>
        <exclude>**/entity/**</exclude>
        <exclude>**/exception/**</exclude>
        <exclude>**/*Application.class</exclude>
        <exclude>**/generated/**</exclude>
    </excludes>
</configuration>
```

#### SonarQube Integration
```xml
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.10.0.2594</version>
</plugin>
```

```properties
# sonar-project.properties
sonar.projectKey=crm-platform
sonar.projectName=CRM Platform
sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
sonar.java.coveragePlugin=jacoco
sonar.coverage.exclusions=**/config/**,**/dto/**,**/entity/**,**/exception/**
sonar.test.exclusions=src/test/**
sonar.java.binaries=target/classes
sonar.java.test.binaries=target/test-classes
```

### Testing Best Practices

#### Test Naming Conventions
- Use descriptive test method names: `shouldCreateContactWhenValidDataProvided()`
- Use `@DisplayName` for human-readable descriptions
- Group related tests in nested classes with `@Nested`

#### Test Organization
```java
@DisplayName("Contact Service Tests")
class ContactServiceTest {
    
    @Nested
    @DisplayName("Create Contact")
    class CreateContactTests {
        
        @Test
        @DisplayName("Should create contact successfully with valid data")
        void shouldCreateContactSuccessfullyWithValidData() {
            // Test implementation
        }
        
        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            // Test implementation
        }
    }
    
    @Nested
    @DisplayName("Update Contact")
    class UpdateContactTests {
        // Update-related tests
    }
}

## Integration Testing Approaches

### API Testing Strategies Using TestContainers

#### TestContainers Setup for Database Integration
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

#### Database Integration Test Base Class
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:tc:postgresql:15:///testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
public abstract class DatabaseIntegrationTestBase {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    protected TestRestTemplate restTemplate;
    
    @LocalServerPort
    protected int port;
    
    protected String getBaseUrl() {
        return "http://localhost:" + port;
    }
}
```

#### API Integration Test Example
```java
class ContactApiIntegrationTest extends DatabaseIntegrationTestBase {
    
    @Autowired
    private ContactRepository contactRepository;
    
    @Test
    @DisplayName("Should create and retrieve contact via API")
    void shouldCreateAndRetrieveContactViaApi() {
        // Given
        CreateContactRequest request = CreateContactRequest.builder()
            .firstName("Integration")
            .lastName("Test")
            .email("integration@test.com")
            .build();
        
        // When - Create contact
        ResponseEntity<ContactResponse> createResponse = restTemplate.postForEntity(
            getBaseUrl() + "/api/v1/contacts",
            request,
            ContactResponse.class
        );
        
        // Then - Verify creation
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        Long contactId = createResponse.getBody().getId();
        
        // When - Retrieve contact
        ResponseEntity<ContactResponse> getResponse = restTemplate.getForEntity(
            getBaseUrl() + "/api/v1/contacts/" + contactId,
            ContactResponse.class
        );
        
        // Then - Verify retrieval
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getFirstName()).isEqualTo("Integration");
        assertThat(getResponse.getBody().getEmail()).isEqualTo("integration@test.com");
        
        // Verify database state
        Optional<Contact> savedContact = contactRepository.findById(contactId);
        assertThat(savedContact).isPresent();
        assertThat(savedContact.get().getEmail()).isEqualTo("integration@test.com");
    }
    
    @Test
    @DisplayName("Should handle concurrent contact creation")
    void shouldHandleConcurrentContactCreation() throws InterruptedException {
        // Given
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<CompletableFuture<ResponseEntity<ContactResponse>>> futures = new ArrayList<>();
        
        // When - Create contacts concurrently
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            CompletableFuture<ResponseEntity<ContactResponse>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    CreateContactRequest request = CreateContactRequest.builder()
                        .firstName("Concurrent")
                        .lastName("Test" + index)
                        .email("concurrent" + index + "@test.com")
                        .build();
                    
                    return restTemplate.postForEntity(
                        getBaseUrl() + "/api/v1/contacts",
                        request,
                        ContactResponse.class
                    );
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }
        
        latch.await(30, TimeUnit.SECONDS);
        
        // Then - Verify all contacts created successfully
        List<ResponseEntity<ContactResponse>> responses = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        assertThat(responses).hasSize(threadCount);
        responses.forEach(response -> {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
        });
        
        // Verify database consistency
        assertThat(contactRepository.count()).isEqualTo(threadCount);
    }
}
```

### Contract Testing Implementation

#### Pact Consumer Test
```java
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "contact-service")
class ContactServiceConsumerTest {
    
    @Mock
    private ContactApiClient contactApiClient;
    
    @Pact(consumer = "deal-service")
    public RequestResponsePact createContactPact(PactDslWithProvider builder) {
        return builder
            .given("contact service is available")
            .uponReceiving("a request to get contact by id")
            .path("/api/v1/contacts/123")
            .method("GET")
            .headers(Map.of("Authorization", "Bearer token123"))
            .willRespondWith()
            .status(200)
            .headers(Map.of("Content-Type", "application/json"))
            .body(LambdaDsl.newJsonBody(body -> body
                .numberType("id", 123)
                .stringType("firstName", "John")
                .stringType("lastName", "Doe")
                .stringType("email", "john.doe@example.com")
            ).build())
            .toPact();
    }
    
    @Test
    @PactTestFor(pactMethod = "createContactPact")
    void shouldGetContactById(MockServer mockServer) {
        // Given
        ContactApiClient client = new ContactApiClient(mockServer.getUrl());
        
        // When
        ContactResponse response = client.getContactById(123L, "Bearer token123");
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(123L);
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
    }
}
```

#### Pact Provider Test
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("contact-service")
@PactFolder("pacts")
class ContactServiceProviderTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private ContactRepository contactRepository;
    
    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }
    
    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }
    
    @State("contact service is available")
    void contactServiceIsAvailable() {
        // Set up test data
        Contact contact = Contact.builder()
            .id(123L)
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .build();
        contactRepository.save(contact);
    }
}
```

#### Spring Cloud Contract Implementation
```groovy
// contracts/contact_service_should_return_contact_by_id.groovy
Contract.make {
    description "should return contact by id"
    request {
        method GET()
        url "/api/v1/contacts/123"
        headers {
            header('Authorization', 'Bearer token123')
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body([
            id: 123,
            firstName: "John",
            lastName: "Doe",
            email: "john.doe@example.com"
        ])
    }
}
```

### Inter-Service Integration Testing Patterns

#### Message-Based Integration Testing
```java
@SpringBootTest
@Testcontainers
class EventDrivenIntegrationTest {
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withReuse(true);
    
    @Autowired
    private ContactService contactService;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @KafkaListener(topics = "contact.created", groupId = "test-group")
    private List<ContactCreatedEvent> receivedEvents = new ArrayList<>();
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    
    @Test
    @DisplayName("Should publish event when contact is created")
    void shouldPublishEventWhenContactIsCreated() throws InterruptedException {
        // Given
        CreateContactRequest request = CreateContactRequest.builder()
            .firstName("Event")
            .lastName("Test")
            .email("event@test.com")
            .build();
        
        // When
        ContactResponse response = contactService.createContact(request);
        
        // Then
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                assertThat(receivedEvents).hasSize(1);
                ContactCreatedEvent event = receivedEvents.get(0);
                assertThat(event.getContactId()).isEqualTo(response.getId());
                assertThat(event.getEmail()).isEqualTo("event@test.com");
            });
    }
}
```

#### Circuit Breaker Integration Testing
```java
@SpringBootTest
class CircuitBreakerIntegrationTest {
    
    @MockBean
    private ExternalApiClient externalApiClient;
    
    @Autowired
    private IntegrationService integrationService;
    
    @Test
    @DisplayName("Should open circuit breaker after consecutive failures")
    void shouldOpenCircuitBreakerAfterConsecutiveFailures() {
        // Given - Configure mock to always fail
        when(externalApiClient.fetchData(anyString()))
            .thenThrow(new RuntimeException("Service unavailable"));
        
        // When - Make multiple calls to trigger circuit breaker
        for (int i = 0; i < 5; i++) {
            Result result = integrationService.syncData("test-id");
            assertThat(result.isSuccess()).isFalse();
        }
        
        // Then - Circuit breaker should be open
        Result result = integrationService.syncData("test-id");
        assertThat(result.getError()).contains("Circuit breaker is open");
        
        // Verify external service was not called due to open circuit
        verify(externalApiClient, times(5)).fetchData(anyString());
    }
}
```

### Database Migration Testing and Rollback Verification

#### Flyway Migration Testing
```java
@SpringBootTest
@Testcontainers
class DatabaseMigrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("migration_test")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }
    
    @Autowired
    private Flyway flyway;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Test
    @DisplayName("Should apply all migrations successfully")
    void shouldApplyAllMigrationsSuccessfully() {
        // When
        MigrateResult result = flyway.migrate();
        
        // Then
        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isGreaterThan(0);
        
        // Verify schema structure
        assertThat(tableExists("contacts")).isTrue();
        assertThat(tableExists("deals")).isTrue();
        assertThat(tableExists("activities")).isTrue();
        
        // Verify indexes
        assertThat(indexExists("idx_contacts_email")).isTrue();
        assertThat(indexExists("idx_deals_contact_id")).isTrue();
    }
    
    @Test
    @DisplayName("Should rollback migrations successfully")
    void shouldRollbackMigrationsSuccessfully() {
        // Given - Apply all migrations
        flyway.migrate();
        
        // When - Rollback to specific version
        flyway.undo();
        
        // Then - Verify rollback
        MigrationInfo[] applied = flyway.info().applied();
        assertThat(applied).hasSizeLessThan(flyway.info().all().length);
    }
    
    private boolean tableExists(String tableName) {
        try {
            jdbcTemplate.queryForObject(
                "SELECT 1 FROM information_schema.tables WHERE table_name = ?",
                Integer.class,
                tableName
            );
            return true;
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }
    
    private boolean indexExists(String indexName) {
        try {
            jdbcTemplate.queryForObject(
                "SELECT 1 FROM pg_indexes WHERE indexname = ?",
                Integer.class,
                indexName
            );
            return true;
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }
}
```

#### Data Migration Verification
```java
@SpringBootTest
@Sql(scripts = "/test-data/migration-test-data.sql")
class DataMigrationTest {
    
    @Autowired
    private ContactRepository contactRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Test
    @DisplayName("Should migrate legacy contact data correctly")
    void shouldMigrateLegacyContactDataCorrectly() {
        // Given - Legacy data exists (loaded via @Sql)
        int legacyCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM legacy_contacts",
            Integer.class
        );
        assertThat(legacyCount).isGreaterThan(0);
        
        // When - Run data migration
        jdbcTemplate.execute("CALL migrate_legacy_contacts()");
        
        // Then - Verify migration results
        List<Contact> migratedContacts = contactRepository.findAll();
        assertThat(migratedContacts).hasSize(legacyCount);
        
        // Verify data transformation
        Contact contact = migratedContacts.get(0);
        assertThat(contact.getEmail()).isNotNull();
        assertThat(contact.getCreatedAt()).isNotNull();
        assertThat(contact.getUpdatedAt()).isNotNull();
        
        // Verify legacy data cleanup
        int remainingLegacyCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM legacy_contacts",
            Integer.class
        );
        assertThat(remainingLegacyCount).isEqualTo(0);
    }
}
```

### Integration Test Configuration

#### Test Profiles and Properties
```yaml
# application-integration-test.yml
spring:
  datasource:
    url: jdbc:tc:postgresql:15:///integration_test
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: integration-test-group
      auto-offset-reset: earliest
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}

logging:
  level:
    org.springframework.web: DEBUG
    com.crm.platform: DEBUG

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

#### Integration Test Suite Configuration
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@TestPropertySource(properties = {
    "spring.test.database.replace=none"
})
@TestMethodOrder(OrderAnnotation.class)
public abstract class IntegrationTestSuite {
    
    @Autowired
    protected TestRestTemplate restTemplate;
    
    @LocalServerPort
    protected int port;
    
    @BeforeEach
    void setUp() {
        restTemplate.getRestTemplate().setInterceptors(
            List.of(new BasicAuthenticationInterceptor("test", "test"))
        );
    }
    
    protected String getApiUrl(String path) {
        return "http://localhost:" + port + "/api/v1" + path;
    }
    
    protected <T> ResponseEntity<T> postForEntity(String path, Object request, Class<T> responseType) {
        return restTemplate.postForEntity(getApiUrl(path), request, responseType);
    }
    
    protected <T> ResponseEntity<T> getForEntity(String path, Class<T> responseType) {
        return restTemplate.getForEntity(getApiUrl(path), responseType);
    }
}

## End-to-End Testing Scenarios

### User Journey Testing Scenarios for CRM Workflows

#### Complete Sales Workflow E2E Test
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(OrderAnnotation.class)
class SalesWorkflowE2ETest extends IntegrationTestSuite {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withReuse(true);
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withReuse(true);
    
    private static Long contactId;
    private static Long leadId;
    private static Long dealId;
    
    @Test
    @Order(1)
    @DisplayName("E2E: Create contact from lead capture")
    void shouldCreateContactFromLeadCapture() {
        // Given - Lead capture form data
        CreateLeadRequest leadRequest = CreateLeadRequest.builder()
            .firstName("Jane")
            .lastName("Prospect")
            .email("jane.prospect@company.com")
            .phone("+1234567890")
            .company("Prospect Corp")
            .source("Website Form")
            .build();
        
        // When - Submit lead
        ResponseEntity<LeadResponse> leadResponse = postForEntity(
            "/leads", leadRequest, LeadResponse.class
        );
        
        // Then - Verify lead creation
        assertThat(leadResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(leadResponse.getBody()).isNotNull();
        leadId = leadResponse.getBody().getId();
        
        // Verify lead appears in lead list
        ResponseEntity<PagedResponse<LeadResponse>> leadsResponse = getForEntity(
            "/leads?status=NEW", 
            new ParameterizedTypeReference<PagedResponse<LeadResponse>>() {}
        );
        
        assertThat(leadsResponse.getBody().getContent())
            .extracting(LeadResponse::getId)
            .contains(leadId);
    }
    
    @Test
    @Order(2)
    @DisplayName("E2E: Qualify lead and convert to contact")
    void shouldQualifyLeadAndConvertToContact() {
        // Given - Lead exists from previous test
        QualifyLeadRequest qualifyRequest = QualifyLeadRequest.builder()
            .score(85)
            .notes("High-value prospect, ready for sales contact")
            .assignedTo("sales@company.com")
            .build();
        
        // When - Qualify lead
        ResponseEntity<LeadResponse> qualifyResponse = restTemplate.exchange(
            getApiUrl("/leads/" + leadId + "/qualify"),
            HttpMethod.PUT,
            new HttpEntity<>(qualifyRequest),
            LeadResponse.class
        );
        
        // Then - Verify qualification
        assertThat(qualifyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(qualifyResponse.getBody().getStatus()).isEqualTo(LeadStatus.QUALIFIED);
        
        // When - Convert lead to contact
        ConvertLeadRequest convertRequest = ConvertLeadRequest.builder()
            .createContact(true)
            .createDeal(true)
            .dealAmount(BigDecimal.valueOf(50000))
            .dealStage("Prospecting")
            .build();
        
        ResponseEntity<ConvertLeadResponse> convertResponse = postForEntity(
            "/leads/" + leadId + "/convert", convertRequest, ConvertLeadResponse.class
        );
        
        // Then - Verify conversion
        assertThat(convertResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        contactId = convertResponse.getBody().getContactId();
        dealId = convertResponse.getBody().getDealId();
        
        assertThat(contactId).isNotNull();
        assertThat(dealId).isNotNull();
    }
    
    @Test
    @Order(3)
    @DisplayName("E2E: Manage deal through sales pipeline")
    void shouldManageDealThroughSalesPipeline() {
        // Given - Deal exists from lead conversion
        
        // When - Move deal to next stage
        UpdateDealStageRequest stageRequest = UpdateDealStageRequest.builder()
            .stage("Qualification")
            .probability(25)
            .notes("Initial qualification call completed")
            .build();
        
        ResponseEntity<DealResponse> stageResponse = restTemplate.exchange(
            getApiUrl("/deals/" + dealId + "/stage"),
            HttpMethod.PUT,
            new HttpEntity<>(stageRequest),
            DealResponse.class
        );
        
        // Then - Verify stage update
        assertThat(stageResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(stageResponse.getBody().getStage()).isEqualTo("Qualification");
        assertThat(stageResponse.getBody().getProbability()).isEqualTo(25);
        
        // When - Add activity to deal
        CreateActivityRequest activityRequest = CreateActivityRequest.builder()
            .type(ActivityType.CALL)
            .subject("Qualification Call")
            .description("Discussed requirements and budget")
            .contactId(contactId)
            .dealId(dealId)
            .scheduledAt(LocalDateTime.now().plusDays(1))
            .build();
        
        ResponseEntity<ActivityResponse> activityResponse = postForEntity(
            "/activities", activityRequest, ActivityResponse.class
        );
        
        // Then - Verify activity creation
        assertThat(activityResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(activityResponse.getBody().getDealId()).isEqualTo(dealId);
    }
    
    @Test
    @Order(4)
    @DisplayName("E2E: Complete deal and generate reports")
    void shouldCompleteDealAndGenerateReports() {
        // Given - Deal in pipeline
        
        // When - Close deal as won
        CloseDealRequest closeRequest = CloseDealRequest.builder()
            .status(DealStatus.WON)
            .actualAmount(BigDecimal.valueOf(45000))
            .closedDate(LocalDate.now())
            .notes("Contract signed, implementation starts next month")
            .build();
        
        ResponseEntity<DealResponse> closeResponse = restTemplate.exchange(
            getApiUrl("/deals/" + dealId + "/close"),
            HttpMethod.PUT,
            new HttpEntity<>(closeRequest),
            DealResponse.class
        );
        
        // Then - Verify deal closure
        assertThat(closeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(closeResponse.getBody().getStatus()).isEqualTo(DealStatus.WON);
        assertThat(closeResponse.getBody().getActualAmount()).isEqualTo(BigDecimal.valueOf(45000));
        
        // When - Generate sales report
        ResponseEntity<SalesReportResponse> reportResponse = getForEntity(
            "/reports/sales?period=CURRENT_MONTH",
            SalesReportResponse.class
        );
        
        // Then - Verify report includes closed deal
        assertThat(reportResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reportResponse.getBody().getTotalRevenue())
            .isGreaterThanOrEqualTo(BigDecimal.valueOf(45000));
        assertThat(reportResponse.getBody().getDealsWon()).isGreaterThanOrEqualTo(1);
    }
}
```

### Automated UI Testing Strategies

#### Selenium WebDriver Setup
```xml
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
    <version>4.15.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.github.bonigarcia</groupId>
    <artifactId>webdrivermanager</artifactId>
    <version>5.6.2</version>
    <scope>test</scope>
</dependency>
```

#### Page Object Model Implementation
```java
@Component
public class ContactsPage {
    
    private final WebDriver driver;
    
    @FindBy(id = "create-contact-btn")
    private WebElement createContactButton;
    
    @FindBy(id = "first-name-input")
    private WebElement firstNameInput;
    
    @FindBy(id = "last-name-input")
    private WebElement lastNameInput;
    
    @FindBy(id = "email-input")
    private WebElement emailInput;
    
    @FindBy(id = "save-contact-btn")
    private WebElement saveContactButton;
    
    @FindBy(css = ".contact-list-item")
    private List<WebElement> contactListItems;
    
    @FindBy(id = "search-input")
    private WebElement searchInput;
    
    public ContactsPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }
    
    public void clickCreateContact() {
        createContactButton.click();
    }
    
    public void fillContactForm(String firstName, String lastName, String email) {
        firstNameInput.sendKeys(firstName);
        lastNameInput.sendKeys(lastName);
        emailInput.sendKeys(email);
    }
    
    public void saveContact() {
        saveContactButton.click();
    }
    
    public void searchContacts(String searchTerm) {
        searchInput.clear();
        searchInput.sendKeys(searchTerm);
        searchInput.sendKeys(Keys.ENTER);
    }
    
    public List<String> getContactNames() {
        return contactListItems.stream()
            .map(element -> element.findElement(By.className("contact-name")).getText())
            .collect(Collectors.toList());
    }
    
    public boolean isContactDisplayed(String contactName) {
        return getContactNames().contains(contactName);
    }
}
```

#### UI End-to-End Test
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = "server.port=8080")
class ContactManagementUITest {
    
    private WebDriver driver;
    private ContactsPage contactsPage;
    
    @BeforeEach
    void setUp() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        
        contactsPage = new ContactsPage(driver);
    }
    
    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
    
    @Test
    @DisplayName("Should create contact through UI")
    void shouldCreateContactThroughUI() {
        // Given
        driver.get("http://localhost:8080/contacts");
        
        // When
        contactsPage.clickCreateContact();
        contactsPage.fillContactForm("UI", "Test", "ui.test@example.com");
        contactsPage.saveContact();
        
        // Then
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.textToBePresentInElement(
            driver.findElement(By.className("success-message")),
            "Contact created successfully"
        ));
        
        assertThat(contactsPage.isContactDisplayed("UI Test")).isTrue();
    }
    
    @Test
    @DisplayName("Should search and filter contacts")
    void shouldSearchAndFilterContacts() {
        // Given - Create test data
        createTestContact("Alice", "Johnson", "alice@example.com");
        createTestContact("Bob", "Smith", "bob@example.com");
        
        driver.get("http://localhost:8080/contacts");
        
        // When
        contactsPage.searchContacts("Alice");
        
        // Then
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.numberOfElementsToBe(
            By.cssSelector(".contact-list-item"), 1
        ));
        
        assertThat(contactsPage.getContactNames()).containsExactly("Alice Johnson");
    }
    
    private void createTestContact(String firstName, String lastName, String email) {
        // Helper method to create test data via API
        RestTemplate restTemplate = new RestTemplate();
        CreateContactRequest request = CreateContactRequest.builder()
            .firstName(firstName)
            .lastName(lastName)
            .email(email)
            .build();
        
        restTemplate.postForEntity(
            "http://localhost:8080/api/v1/contacts",
            request,
            ContactResponse.class
        );
    }
}
```

#### Playwright Alternative Implementation
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ContactManagementPlaywrightTest {
    
    private Playwright playwright;
    private Browser browser;
    private Page page;
    
    @BeforeEach
    void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        page = browser.newPage();
    }
    
    @AfterEach
    void tearDown() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
    
    @Test
    @DisplayName("Should handle contact creation workflow")
    void shouldHandleContactCreationWorkflow() {
        // Given
        page.navigate("http://localhost:8080/contacts");
        
        // When
        page.click("#create-contact-btn");
        page.fill("#first-name-input", "Playwright");
        page.fill("#last-name-input", "Test");
        page.fill("#email-input", "playwright@example.com");
        page.click("#save-contact-btn");
        
        // Then
        page.waitForSelector(".success-message");
        assertThat(page.textContent(".success-message"))
            .contains("Contact created successfully");
        
        assertThat(page.isVisible("text=Playwright Test")).isTrue();
    }
}
```

### API Workflow Testing for Complex Business Processes

#### Multi-Service Workflow Test
```java
@SpringBootTest
@Testcontainers
class ComplexBusinessWorkflowTest extends IntegrationTestSuite {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withReuse(true);
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withReuse(true);
    
    @Test
    @DisplayName("Should handle complete customer onboarding workflow")
    void shouldHandleCompleteCustomerOnboardingWorkflow() {
        // Step 1: Lead Registration
        CreateLeadRequest leadRequest = CreateLeadRequest.builder()
            .firstName("Enterprise")
            .lastName("Customer")
            .email("enterprise@bigcorp.com")
            .company("BigCorp Inc")
            .source("Trade Show")
            .build();
        
        ResponseEntity<LeadResponse> leadResponse = postForEntity(
            "/leads", leadRequest, LeadResponse.class
        );
        
        Long leadId = leadResponse.getBody().getId();
        
        // Step 2: Lead Qualification
        QualifyLeadRequest qualifyRequest = QualifyLeadRequest.builder()
            .score(95)
            .notes("Enterprise customer with high potential")
            .assignedTo("enterprise-sales@company.com")
            .build();
        
        restTemplate.exchange(
            getApiUrl("/leads/" + leadId + "/qualify"),
            HttpMethod.PUT,
            new HttpEntity<>(qualifyRequest),
            LeadResponse.class
        );
        
        // Step 3: Lead Conversion
        ConvertLeadRequest convertRequest = ConvertLeadRequest.builder()
            .createContact(true)
            .createAccount(true)
            .createDeal(true)
            .dealAmount(BigDecimal.valueOf(500000))
            .dealStage("Prospecting")
            .build();
        
        ResponseEntity<ConvertLeadResponse> convertResponse = postForEntity(
            "/leads/" + leadId + "/convert", convertRequest, ConvertLeadResponse.class
        );
        
        Long contactId = convertResponse.getBody().getContactId();
        Long accountId = convertResponse.getBody().getAccountId();
        Long dealId = convertResponse.getBody().getDealId();
        
        // Step 4: Account Setup
        UpdateAccountRequest accountRequest = UpdateAccountRequest.builder()
            .industry("Technology")
            .employees(5000)
            .annualRevenue(BigDecimal.valueOf(100000000))
            .tier("Enterprise")
            .build();
        
        restTemplate.exchange(
            getApiUrl("/accounts/" + accountId),
            HttpMethod.PUT,
            new HttpEntity<>(accountRequest),
            AccountResponse.class
        );
        
        // Step 5: Deal Progression
        progressDealThroughPipeline(dealId, contactId);
        
        // Step 6: Contract Generation
        GenerateContractRequest contractRequest = GenerateContractRequest.builder()
            .dealId(dealId)
            .templateId("enterprise-template")
            .customTerms(Map.of(
                "paymentTerms", "Net 30",
                "supportLevel", "Premium"
            ))
            .build();
        
        ResponseEntity<ContractResponse> contractResponse = postForEntity(
            "/contracts", contractRequest, ContractResponse.class
        );
        
        // Step 7: Verify Complete Workflow
        verifyWorkflowCompletion(leadId, contactId, accountId, dealId, 
                                contractResponse.getBody().getId());
    }
    
    private void progressDealThroughPipeline(Long dealId, Long contactId) {
        String[] stages = {"Qualification", "Proposal", "Negotiation", "Closed Won"};
        int[] probabilities = {25, 50, 75, 100};
        
        for (int i = 0; i < stages.length; i++) {
            UpdateDealStageRequest stageRequest = UpdateDealStageRequest.builder()
                .stage(stages[i])
                .probability(probabilities[i])
                .notes("Progressed to " + stages[i])
                .build();
            
            restTemplate.exchange(
                getApiUrl("/deals/" + dealId + "/stage"),
                HttpMethod.PUT,
                new HttpEntity<>(stageRequest),
                DealResponse.class
            );
            
            // Add activity for each stage
            CreateActivityRequest activityRequest = CreateActivityRequest.builder()
                .type(ActivityType.MEETING)
                .subject("Stage: " + stages[i])
                .contactId(contactId)
                .dealId(dealId)
                .completedAt(LocalDateTime.now())
                .build();
            
            postForEntity("/activities", activityRequest, ActivityResponse.class);
        }
    }
    
    private void verifyWorkflowCompletion(Long leadId, Long contactId, Long accountId, 
                                        Long dealId, Long contractId) {
        // Verify lead is converted
        ResponseEntity<LeadResponse> leadResponse = getForEntity(
            "/leads/" + leadId, LeadResponse.class
        );
        assertThat(leadResponse.getBody().getStatus()).isEqualTo(LeadStatus.CONVERTED);
        
        // Verify contact exists
        ResponseEntity<ContactResponse> contactResponse = getForEntity(
            "/contacts/" + contactId, ContactResponse.class
        );
        assertThat(contactResponse.getBody().getAccountId()).isEqualTo(accountId);
        
        // Verify deal is closed won
        ResponseEntity<DealResponse> dealResponse = getForEntity(
            "/deals/" + dealId, DealResponse.class
        );
        assertThat(dealResponse.getBody().getStatus()).isEqualTo(DealStatus.WON);
        
        // Verify contract exists
        ResponseEntity<ContractResponse> contractResponse = getForEntity(
            "/contracts/" + contractId, ContractResponse.class
        );
        assertThat(contractResponse.getBody().getStatus()).isEqualTo(ContractStatus.ACTIVE);
        
        // Verify activities were created
        ResponseEntity<PagedResponse<ActivityResponse>> activitiesResponse = getForEntity(
            "/activities?dealId=" + dealId,
            new ParameterizedTypeReference<PagedResponse<ActivityResponse>>() {}
        );
        assertThat(activitiesResponse.getBody().getContent()).hasSizeGreaterThan(3);
    }
}
```

### Data Consistency Testing Across Microservices

#### Event-Driven Consistency Test
```java
@SpringBootTest
@Testcontainers
class DataConsistencyTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withReuse(true);
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withReuse(true);
    
    @Autowired
    private ContactService contactService;
    
    @Autowired
    private DealService dealService;
    
    @Autowired
    private ActivityService activityService;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Test
    @DisplayName("Should maintain data consistency across services")
    void shouldMaintainDataConsistencyAcrossServices() throws InterruptedException {
        // Given
        CreateContactRequest contactRequest = CreateContactRequest.builder()
            .firstName("Consistency")
            .lastName("Test")
            .email("consistency@test.com")
            .build();
        
        // When - Create contact
        ContactResponse contact = contactService.createContact(contactRequest);
        
        // Then - Wait for event propagation
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                // Verify contact exists in all relevant services
                assertThat(contactService.getContact(contact.getId())).isNotNull();
                
                // Verify contact appears in search index
                List<ContactResponse> searchResults = contactService.searchContacts("consistency");
                assertThat(searchResults).extracting(ContactResponse::getId)
                    .contains(contact.getId());
                
                // Verify audit trail was created
                List<AuditEvent> auditEvents = auditService.getAuditTrail(
                    "Contact", contact.getId()
                );
                assertThat(auditEvents).hasSize(1);
                assertThat(auditEvents.get(0).getAction()).isEqualTo("CREATE");
            });
        
        // When - Update contact
        UpdateContactRequest updateRequest = UpdateContactRequest.builder()
            .firstName("Updated")
            .lastName("Consistency")
            .build();
        
        contactService.updateContact(contact.getId(), updateRequest);
        
        // Then - Verify consistency after update
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                // Verify update propagated to search
                List<ContactResponse> searchResults = contactService.searchContacts("Updated");
                assertThat(searchResults).extracting(ContactResponse::getFirstName)
                    .contains("Updated");
                
                // Verify audit trail updated
                List<AuditEvent> auditEvents = auditService.getAuditTrail(
                    "Contact", contact.getId()
                );
                assertThat(auditEvents).hasSize(2);
                assertThat(auditEvents.get(1).getAction()).isEqualTo("UPDATE");
            });
    }
    
    @Test
    @DisplayName("Should handle eventual consistency in distributed transactions")
    void shouldHandleEventualConsistencyInDistributedTransactions() {
        // Given
        ContactResponse contact = createTestContact();
        
        CreateDealRequest dealRequest = CreateDealRequest.builder()
            .contactId(contact.getId())
            .title("Distributed Transaction Test")
            .amount(BigDecimal.valueOf(25000))
            .stage("Prospecting")
            .build();
        
        // When - Create deal (triggers multiple service updates)
        DealResponse deal = dealService.createDeal(dealRequest);
        
        // Then - Verify eventual consistency
        await().atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> {
                // Verify deal created
                DealResponse retrievedDeal = dealService.getDeal(deal.getId());
                assertThat(retrievedDeal).isNotNull();
                
                // Verify contact updated with deal reference
                ContactResponse updatedContact = contactService.getContact(contact.getId());
                assertThat(updatedContact.getDealIds()).contains(deal.getId());
                
                // Verify activity created
                List<ActivityResponse> activities = activityService.getActivitiesByDeal(deal.getId());
                assertThat(activities).hasSize(1);
                assertThat(activities.get(0).getType()).isEqualTo(ActivityType.DEAL_CREATED);
                
                // Verify analytics updated
                AnalyticsResponse analytics = analyticsService.getDealAnalytics();
                assertThat(analytics.getTotalDeals()).isGreaterThan(0);
                assertThat(analytics.getTotalValue()).isGreaterThanOrEqualTo(BigDecimal.valueOf(25000));
            });
    }
    
    private ContactResponse createTestContact() {
        CreateContactRequest request = CreateContactRequest.builder()
            .firstName("Test")
            .lastName("Contact")
            .email("test.contact@example.com")
            .build();
        return contactService.createContact(request);
    }
}

## Performance and Security Testing

### Load Testing Scenarios and Performance Benchmarks

#### JMeter Load Testing Configuration
```xml
<!-- pom.xml -->
<plugin>
    <groupId>com.lazerycode.jmeter</groupId>
    <artifactId>jmeter-maven-plugin</artifactId>
    <version>3.7.0</version>
    <configuration>
        <testFilesDirectory>src/test/jmeter</testFilesDirectory>
        <resultsDirectory>target/jmeter/results</resultsDirectory>
        <generateReports>true</generateReports>
    </configuration>
</plugin>
```

#### Contact API Load Test Plan
```xml
<!-- src/test/jmeter/ContactAPI_LoadTest.jmx -->
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="Contact API Load Test">
      <elementProp name="TestPlan.arguments" elementType="Arguments" guiclass="ArgumentsPanel">
        <collectionProp name="Arguments.arguments">
          <elementProp name="baseUrl" elementType="Argument">
            <stringProp name="Argument.name">baseUrl</stringProp>
            <stringProp name="Argument.value">${__P(baseUrl,http://localhost:8080)}</stringProp>
          </elementProp>
          <elementProp name="threads" elementType="Argument">
            <stringProp name="Argument.name">threads</stringProp>
            <stringProp name="Argument.value">${__P(threads,100)}</stringProp>
          </elementProp>
          <elementProp name="rampup" elementType="Argument">
            <stringProp name="Argument.name">rampup</stringProp>
            <stringProp name="Argument.value">${__P(rampup,60)}</stringProp>
          </elementProp>
          <elementProp name="duration" elementType="Argument">
            <stringProp name="Argument.name">duration</stringProp>
            <stringProp name="Argument.value">${__P(duration,300)}</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </TestPlan>
    
    <hashTree>
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="Contact Operations">
        <stringProp name="ThreadGroup.num_threads">${threads}</stringProp>
        <stringProp name="ThreadGroup.ramp_time">${rampup}</stringProp>
        <stringProp name="ThreadGroup.duration">${duration}</stringProp>
        <boolProp name="ThreadGroup.scheduler">true</boolProp>
      </ThreadGroup>
      
      <hashTree>
        <!-- Create Contact Test -->
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="Create Contact">
          <stringProp name="HTTPSampler.domain">${baseUrl}</stringProp>
          <stringProp name="HTTPSampler.path">/api/v1/contacts</stringProp>
          <stringProp name="HTTPSampler.method">POST</stringProp>
          <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
            <collectionProp name="Arguments.arguments">
              <elementProp name="" elementType="HTTPArgument">
                <boolProp name="HTTPArgument.always_encode">false</boolProp>
                <stringProp name="Argument.value">{
                  "firstName": "Load${__threadNum}",
                  "lastName": "Test${__Random(1,10000)}",
                  "email": "load.test.${__threadNum}.${__Random(1,10000)}@example.com",
                  "phone": "+1${__Random(1000000000,9999999999)}"
                }</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
              </elementProp>
            </collectionProp>
          </elementProp>
          <stringProp name="HTTPSampler.postBodyRaw">true</stringProp>
        </HTTPSamplerProxy>
        
        <!-- Get Contact Test -->
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="Get Contact">
          <stringProp name="HTTPSampler.domain">${baseUrl}</stringProp>
          <stringProp name="HTTPSampler.path">/api/v1/contacts/${contactId}</stringProp>
          <stringProp name="HTTPSampler.method">GET</stringProp>
        </HTTPSamplerProxy>
        
        <!-- Search Contacts Test -->
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="Search Contacts">
          <stringProp name="HTTPSampler.domain">${baseUrl}</stringProp>
          <stringProp name="HTTPSampler.path">/api/v1/contacts/search</stringProp>
          <stringProp name="HTTPSampler.method">GET</stringProp>
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
            <collectionProp name="Arguments.arguments">
              <elementProp name="q" elementType="HTTPArgument">
                <stringProp name="Argument.name">q</stringProp>
                <stringProp name="Argument.value">Load</stringProp>
              </elementProp>
              <elementProp name="page" elementType="HTTPArgument">
                <stringProp name="Argument.name">page</stringProp>
                <stringProp name="Argument.value">0</stringProp>
              </elementProp>
              <elementProp name="size" elementType="HTTPArgument">
                <stringProp name="Argument.name">size</stringProp>
                <stringProp name="Argument.value">20</stringProp>
              </elementProp>
            </collectionProp>
          </elementProp>
        </HTTPSamplerProxy>
      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

#### Gatling Performance Tests
```scala
// src/test/scala/ContactApiSimulation.scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class ContactApiSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Load Test")

  val createContactScenario = scenario("Create Contact")
    .exec(
      http("Create Contact")
        .post("/api/v1/contacts")
        .body(StringBody("""
          {
            "firstName": "Gatling${randomInt(1, 10000)}",
            "lastName": "Test${randomInt(1, 10000)}",
            "email": "gatling.${randomInt(1, 10000)}@example.com",
            "phone": "+1${randomLong(1000000000, 9999999999)}"
          }
        """))
        .check(status.is(201))
        .check(jsonPath("$.id").saveAs("contactId"))
    )
    .pause(1)
    .exec(
      http("Get Created Contact")
        .get("/api/v1/contacts/${contactId}")
        .check(status.is(200))
    )

  val searchContactScenario = scenario("Search Contacts")
    .exec(
      http("Search Contacts")
        .get("/api/v1/contacts/search")
        .queryParam("q", "Gatling")
        .queryParam("page", "0")
        .queryParam("size", "20")
        .check(status.is(200))
        .check(jsonPath("$.content").exists)
    )

  setUp(
    createContactScenario.inject(
      rampUsers(50) during (30 seconds),
      constantUsersPerSec(10) during (2 minutes)
    ),
    searchContactScenario.inject(
      rampUsers(100) during (30 seconds),
      constantUsersPerSec(20) during (2 minutes)
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.max.lt(2000),
     global.responseTime.mean.lt(500),
     global.successfulRequests.percent.gt(95)
   )
}
```

#### Spring Boot Performance Tests
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.jpa.show-sql=false",
    "logging.level.org.springframework.web=WARN"
})
class ContactApiPerformanceTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @LocalServerPort
    private int port;
    
    @Test
    @DisplayName("Should handle high concurrent contact creation")
    void shouldHandleHighConcurrentContactCreation() throws InterruptedException {
        // Given
        int threadCount = 50;
        int requestsPerThread = 20;
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<CompletableFuture<List<Long>>> futures = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        // When
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            CompletableFuture<List<Long>> future = CompletableFuture.supplyAsync(() -> {
                List<Long> createdIds = new ArrayList<>();
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        CreateContactRequest request = CreateContactRequest.builder()
                            .firstName("Perf" + threadIndex)
                            .lastName("Test" + j)
                            .email("perf." + threadIndex + "." + j + "@example.com")
                            .build();
                        
                        ResponseEntity<ContactResponse> response = restTemplate.postForEntity(
                            "http://localhost:" + port + "/api/v1/contacts",
                            request,
                            ContactResponse.class
                        );
                        
                        if (response.getStatusCode() == HttpStatus.CREATED) {
                            createdIds.add(response.getBody().getId());
                        }
                    }
                } finally {
                    latch.countDown();
                }
                return createdIds;
            });
            futures.add(future);
        }
        
        // Then
        latch.await(60, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        List<Long> allCreatedIds = futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .collect(Collectors.toList());
        
        int totalRequests = threadCount * requestsPerThread;
        double throughput = (double) allCreatedIds.size() / (duration / 1000.0);
        
        // Performance assertions
        assertThat(allCreatedIds).hasSize(totalRequests);
        assertThat(duration).isLessThan(30000); // Should complete within 30 seconds
        assertThat(throughput).isGreaterThan(10); // Should handle at least 10 requests/second
        
        System.out.println("Performance Results:");
        System.out.println("Total Requests: " + totalRequests);
        System.out.println("Successful Requests: " + allCreatedIds.size());
        System.out.println("Duration: " + duration + "ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " requests/second");
    }
    
    @Test
    @DisplayName("Should maintain response times under load")
    void shouldMaintainResponseTimesUnderLoad() {
        // Given
        List<Long> responseTimes = new ArrayList<>();
        int requestCount = 100;
        
        // When
        for (int i = 0; i < requestCount; i++) {
            long startTime = System.nanoTime();
            
            ResponseEntity<PagedResponse<ContactResponse>> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/contacts?page=0&size=20",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<PagedResponse<ContactResponse>>() {}
            );
            
            long endTime = System.nanoTime();
            long responseTime = (endTime - startTime) / 1_000_000; // Convert to milliseconds
            
            responseTimes.add(responseTime);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
        
        // Then
        double averageResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        long maxResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0L);
        
        long p95ResponseTime = responseTimes.stream()
            .sorted()
            .skip((long) (requestCount * 0.95))
            .findFirst()
            .orElse(0L);
        
        // Performance assertions
        assertThat(averageResponseTime).isLessThan(200); // Average < 200ms
        assertThat(maxResponseTime).isLessThan(1000); // Max < 1000ms
        assertThat(p95ResponseTime).isLessThan(500); // 95th percentile < 500ms
        
        System.out.println("Response Time Results:");
        System.out.println("Average: " + String.format("%.2f", averageResponseTime) + "ms");
        System.out.println("Max: " + maxResponseTime + "ms");
        System.out.println("95th Percentile: " + p95ResponseTime + "ms");
    }
}
```

### Security Testing Approaches

#### OWASP ZAP Integration
```xml
<plugin>
    <groupId>org.zaproxy</groupId>
    <artifactId>zap-maven-plugin</artifactId>
    <version>1.2.0</version>
    <configuration>
        <zapHost>localhost</zapHost>
        <zapPort>8090</zapPort>
        <targetURL>http://localhost:8080</targetURL>
        <spiderURL>http://localhost:8080</spiderURL>
        <activeScanURL>http://localhost:8080</activeScanURL>
        <format>xml</format>
        <reportDir>target/zap-reports</reportDir>
    </configuration>
</plugin>
```

#### Security Test Suite
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "spring.profiles.active=security-test")
class SecurityTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @LocalServerPort
    private int port;
    
    @Test
    @DisplayName("Should prevent SQL injection attacks")
    void shouldPreventSqlInjectionAttacks() {
        // Given - Malicious SQL injection payloads
        String[] sqlInjectionPayloads = {
            "'; DROP TABLE contacts; --",
            "' OR '1'='1",
            "' UNION SELECT * FROM users --",
            "'; INSERT INTO contacts (email) VALUES ('hacked@evil.com'); --"
        };
        
        // When & Then
        for (String payload : sqlInjectionPayloads) {
            ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/contacts/search?q=" + payload,
                String.class
            );
            
            // Should not return 500 error or expose database structure
            assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).doesNotContain("SQLException");
            assertThat(response.getBody()).doesNotContain("DROP TABLE");
        }
    }
    
    @Test
    @DisplayName("Should prevent XSS attacks")
    void shouldPreventXssAttacks() {
        // Given - XSS payloads
        String[] xssPayloads = {
            "<script>alert('XSS')</script>",
            "<img src=x onerror=alert('XSS')>",
            "javascript:alert('XSS')",
            "<svg onload=alert('XSS')>"
        };
        
        // When & Then
        for (String payload : xssPayloads) {
            CreateContactRequest request = CreateContactRequest.builder()
                .firstName(payload)
                .lastName("Test")
                .email("xss.test@example.com")
                .build();
            
            ResponseEntity<ContactResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/contacts",
                request,
                ContactResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.CREATED) {
                // Verify XSS payload is sanitized
                assertThat(response.getBody().getFirstName()).doesNotContain("<script>");
                assertThat(response.getBody().getFirstName()).doesNotContain("javascript:");
                assertThat(response.getBody().getFirstName()).doesNotContain("onerror");
            }
        }
    }
    
    @Test
    @DisplayName("Should enforce authentication")
    void shouldEnforceAuthentication() {
        // Given - Request without authentication
        RestTemplate unauthenticatedTemplate = new RestTemplate();
        
        // When
        ResponseEntity<String> response = unauthenticatedTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/contacts",
            String.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    
    @Test
    @DisplayName("Should enforce authorization")
    void shouldEnforceAuthorization() {
        // Given - User with limited permissions
        TestRestTemplate limitedUserTemplate = new TestRestTemplate("limited-user", "password");
        
        // When - Try to access admin endpoint
        ResponseEntity<String> response = limitedUserTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/admin/users",
            String.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
    
    @Test
    @DisplayName("Should prevent CSRF attacks")
    void shouldPreventCsrfAttacks() {
        // Given - Request without CSRF token
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        CreateContactRequest request = CreateContactRequest.builder()
            .firstName("CSRF")
            .lastName("Test")
            .email("csrf.test@example.com")
            .build();
        
        HttpEntity<CreateContactRequest> entity = new HttpEntity<>(request, headers);
        
        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/contacts",
            entity,
            String.class
        );
        
        // Then - Should require CSRF token for state-changing operations
        // (Implementation depends on CSRF configuration)
        assertThat(response.getStatusCode()).isIn(
            HttpStatus.FORBIDDEN, HttpStatus.CREATED
        );
    }
    
    @Test
    @DisplayName("Should implement rate limiting")
    void shouldImplementRateLimiting() throws InterruptedException {
        // Given - Rapid successive requests
        int requestCount = 100;
        List<ResponseEntity<String>> responses = new ArrayList<>();
        
        // When
        for (int i = 0; i < requestCount; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/contacts",
                String.class
            );
            responses.add(response);
            
            if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                break;
            }
        }
        
        // Then - Should eventually return 429 Too Many Requests
        boolean rateLimitTriggered = responses.stream()
            .anyMatch(response -> response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS);
        
        assertThat(rateLimitTriggered).isTrue();
    }
}
```

### Chaos Engineering Practices for Fault Tolerance Testing

#### Chaos Monkey Integration
```java
@SpringBootTest
@EnableChaosMonkey
@TestPropertySource(properties = {
    "chaos.monkey.enabled=true",
    "chaos.monkey.watcher.service=true",
    "chaos.monkey.assaults.level=5",
    "chaos.monkey.assaults.latency-active=true",
    "chaos.monkey.assaults.exceptions-active=true"
})
class ChaosEngineeringTest {
    
    @Autowired
    private ContactService contactService;
    
    @Autowired
    private ChaosMonkeyService chaosMonkeyService;
    
    @Test
    @DisplayName("Should handle service latency gracefully")
    void shouldHandleServiceLatencyGracefully() {
        // Given - Enable latency assault
        ChaosMonkeyProperties properties = new ChaosMonkeyProperties();
        properties.getAssaults().setLatencyActive(true);
        properties.getAssaults().setLatencyRangeStart(1000);
        properties.getAssaults().setLatencyRangeEnd(3000);
        chaosMonkeyService.updateProperties(properties);
        
        // When
        long startTime = System.currentTimeMillis();
        
        CreateContactRequest request = CreateContactRequest.builder()
            .firstName("Chaos")
            .lastName("Test")
            .email("chaos@test.com")
            .build();
        
        ContactResponse response = contactService.createContact(request);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Then - Should handle latency but still succeed
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(duration).isGreaterThan(1000); // Latency was injected
        assertThat(duration).isLessThan(10000); // But didn't timeout
    }
    
    @Test
    @DisplayName("Should recover from random exceptions")
    void shouldRecoverFromRandomExceptions() {
        // Given - Enable exception assault
        ChaosMonkeyProperties properties = new ChaosMonkeyProperties();
        properties.getAssaults().setExceptionsActive(true);
        properties.getAssaults().setLevel(3); // 30% failure rate
        chaosMonkeyService.updateProperties(properties);
        
        // When - Make multiple requests
        int successCount = 0;
        int totalRequests = 50;
        
        for (int i = 0; i < totalRequests; i++) {
            try {
                CreateContactRequest request = CreateContactRequest.builder()
                    .firstName("Chaos" + i)
                    .lastName("Test")
                    .email("chaos" + i + "@test.com")
                    .build();
                
                ContactResponse response = contactService.createContact(request);
                if (response != null) {
                    successCount++;
                }
            } catch (Exception e) {
                // Expected due to chaos monkey
            }
        }
        
        // Then - Should have some successes despite chaos
        assertThat(successCount).isGreaterThan(totalRequests / 2);
        assertThat(successCount).isLessThan(totalRequests); // Some should fail
    }
}
```

#### Circuit Breaker Testing
```java
@SpringBootTest
class CircuitBreakerChaosTest {
    
    @MockBean
    private ExternalApiClient externalApiClient;
    
    @Autowired
    private IntegrationService integrationService;
    
    @Test
    @DisplayName("Should handle cascading failures gracefully")
    void shouldHandleCascadingFailuresGracefully() {
        // Given - External service is failing
        when(externalApiClient.fetchData(anyString()))
            .thenThrow(new RuntimeException("Service unavailable"));
        
        // When - Make requests that trigger circuit breaker
        List<Result> results = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            Result result = integrationService.syncData("test-" + i);
            results.add(result);
            
            // Small delay between requests
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Then - Circuit breaker should prevent cascading failures
        long failureCount = results.stream()
            .mapToLong(result -> result.isSuccess() ? 0 : 1)
            .sum();
        
        assertThat(failureCount).isEqualTo(10); // All should fail
        
        // Verify circuit breaker opened after threshold
        Result finalResult = integrationService.syncData("final-test");
        assertThat(finalResult.getError()).contains("Circuit breaker");
        
        // Verify external service wasn't called excessively
        verify(externalApiClient, atMost(5)).fetchData(anyString());
    }
}
```

### Automated Security Scanning Integration in CI/CD Pipeline

#### SAST (Static Application Security Testing)
```yaml
# .github/workflows/security-scan.yml
name: Security Scan

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  sast-scan:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Run SpotBugs Security Scan
      run: mvn compile spotbugs:check
    
    - name: Run OWASP Dependency Check
      run: mvn org.owasp:dependency-check-maven:check
    
    - name: Upload SAST Results
      uses: actions/upload-artifact@v3
      with:
        name: sast-results
        path: |
          target/spotbugsXml.xml
          target/dependency-check-report.html

  dast-scan:
    runs-on: ubuntu-latest
    needs: sast-scan
    steps:
    - uses: actions/checkout@v3
    
    - name: Start Application
      run: |
        mvn spring-boot:run &
        sleep 30
    
    - name: Run OWASP ZAP Scan
      run: |
        docker run -v $(pwd):/zap/wrk/:rw \
          -t owasp/zap2docker-stable zap-baseline.py \
          -t http://host.docker.internal:8080 \
          -r zap-report.html
    
    - name: Upload DAST Results
      uses: actions/upload-artifact@v3
      with:
        name: dast-results
        path: zap-report.html
```

#### Security Test Configuration
```java
@TestConfiguration
@Profile("security-test")
public class SecurityTestConfiguration {
    
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        // Use faster encoder for tests
        return new BCryptPasswordEncoder(4);
    }
    
    @Bean
    public SecurityTestDataFactory securityTestDataFactory() {
        return new SecurityTestDataFactory();
    }
    
    @Component
    public static class SecurityTestDataFactory {
        
        public User createTestUser(String username, String... roles) {
            return User.builder()
                .username(username)
                .password("test-password")
                .roles(Set.of(roles))
                .enabled(true)
                .build();
        }
        
        public String generateJwtToken(User user) {
            return jwtTokenProvider.generateToken(user);
        }
    }
}
```

### Performance Benchmarks and SLA Definitions

#### Performance Requirements Matrix

| Operation | Response Time (95th percentile) | Throughput (req/sec) | Availability |
|-----------|--------------------------------|---------------------|--------------|
| Create Contact | < 200ms | > 100 | 99.9% |
| Get Contact | < 100ms | > 500 | 99.9% |
| Search Contacts | < 300ms | > 200 | 99.5% |
| Update Contact | < 250ms | > 150 | 99.9% |
| Delete Contact | < 200ms | > 100 | 99.9% |
| Bulk Operations | < 2000ms | > 50 | 99.5% |

#### Continuous Performance Monitoring
```java
@Component
@Profile("performance-monitoring")
public class PerformanceMonitor {
    
    private final MeterRegistry meterRegistry;
    private final Timer.Sample sample;
    
    public PerformanceMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    @EventListener
    public void handleContactCreated(ContactCreatedEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("contact.creation.time")
            .description("Time taken to create contact")
            .register(meterRegistry));
        
        meterRegistry.counter("contact.created.total").increment();
    }
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void checkPerformanceThresholds() {
        Timer contactCreationTimer = meterRegistry.find("contact.creation.time").timer();
        
        if (contactCreationTimer != null) {
            double meanTime = contactCreationTimer.mean(TimeUnit.MILLISECONDS);
            double maxTime = contactCreationTimer.max(TimeUnit.MILLISECONDS);
            
            if (meanTime > 200 || maxTime > 1000) {
                // Alert or log performance degradation
                log.warn("Performance degradation detected - Mean: {}ms, Max: {}ms", 
                        meanTime, maxTime);
            }
        }
    }
}
```

## Testing Infrastructure

### Test Environment Management

#### Docker Compose for Test Environment
```yaml
# docker-compose.test.yml
version: '3.8'
services:
  postgres-test:
    image: postgres:15
    environment:
      POSTGRES_DB: crm_test
      POSTGRES_USER: test
      POSTGRES_PASSWORD: test
    ports:
      - "5433:5432"
    volumes:
      - postgres_test_data:/var/lib/postgresql/data

  redis-test:
    image: redis:7-alpine
    ports:
      - "6380:6379"

  kafka-test:
    image: confluentinc/cp-kafka:7.4.0
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper-test:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9093:9092"
    depends_on:
      - zookeeper-test

  zookeeper-test:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

volumes:
  postgres_test_data:
```

### Quality Metrics and Reporting

#### Test Reporting Configuration
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <id>merge-results</id>
            <phase>verify</phase>
            <goals>
                <goal>merge</goal>
            </goals>
            <configuration>
                <fileSets>
                    <fileSet>
                        <directory>${project.build.directory}</directory>
                        <includes>
                            <include>*.exec</include>
                        </includes>
                    </fileSet>
                </fileSets>
                <destFile>${project.build.directory}/jacoco-merged.exec</destFile>
            </configuration>
        </execution>
        <execution>
            <id>create-merged-report</id>
            <phase>verify</phase>
            <goals>
                <goal>report</goal>
            </goals>
            <configuration>
                <dataFile>${project.build.directory}/jacoco-merged.exec</dataFile>
                <outputDirectory>${project.reporting.outputDirectory}/jacoco-merged</outputDirectory>
            </configuration>
        </execution>
    </executions>
</plugin>
```

#### Quality Gates and Thresholds

| Metric | Minimum Threshold | Target |
|--------|------------------|--------|
| Line Coverage | 80% | 90% |
| Branch Coverage | 75% | 85% |
| Unit Test Pass Rate | 100% | 100% |
| Integration Test Pass Rate | 95% | 100% |
| Performance Test Pass Rate | 90% | 95% |
| Security Scan Pass Rate | 100% | 100% |

---

**Document Status**: Complete  
**Last Updated**: January 2025  
**Version**: 1.0  
**Next Review**: February 2025