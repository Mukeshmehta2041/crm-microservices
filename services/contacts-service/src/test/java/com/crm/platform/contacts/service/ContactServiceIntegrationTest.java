package com.crm.platform.contacts.service;

import com.crm.platform.contacts.dto.ContactRequest;
import com.crm.platform.contacts.dto.ContactResponse;
import com.crm.platform.contacts.entity.Contact;
import com.crm.platform.contacts.entity.ContactStatus;
import com.crm.platform.contacts.repository.ContactRepository;
import com.crm.platform.common.util.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "eureka.client.enabled=false"
})
@Transactional
class ContactServiceIntegrationTest {

    @Autowired
    private ContactService contactService;

    @Autowired
    private ContactRepository contactRepository;

    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        
        // Set tenant context for the test
        TenantContext.setCurrentTenantId(tenantId);
    }

    @Test
    void shouldCreateContactSuccessfully() {
        // Given
        ContactRequest request = new ContactRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john.doe@example.com");
        request.setPhone("+1-555-123-4567");
        request.setTitle("Software Engineer");
        request.setOwnerId(userId);

        // When
        ContactResponse response = contactService.createContact(request, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(response.getPhone()).isEqualTo("+1-555-123-4567");
        assertThat(response.getTitle()).isEqualTo("Software Engineer");
        assertThat(response.getTenantId()).isEqualTo(tenantId);
        assertThat(response.getOwnerId()).isEqualTo(userId);
        assertThat(response.getContactStatus()).isEqualTo(ContactStatus.ACTIVE);
        assertThat(response.getLeadScore()).isEqualTo(0);
        assertThat(response.getFullName()).isEqualTo("John Doe");

        // Verify in database
        Contact savedContact = contactRepository.findById(response.getId()).orElse(null);
        assertThat(savedContact).isNotNull();
        assertThat(savedContact.getFirstName()).isEqualTo("John");
        assertThat(savedContact.getLastName()).isEqualTo("Doe");
    }

    @Test
    void shouldUpdateContactSuccessfully() {
        // Given - create a contact first
        ContactRequest createRequest = new ContactRequest();
        createRequest.setFirstName("Jane");
        createRequest.setLastName("Smith");
        createRequest.setEmail("jane.smith@example.com");
        createRequest.setOwnerId(userId);

        ContactResponse createdContact = contactService.createContact(createRequest, userId);

        // When - update the contact
        ContactRequest updateRequest = new ContactRequest();
        updateRequest.setFirstName("Jane");
        updateRequest.setLastName("Smith-Johnson");
        updateRequest.setEmail("jane.smith-johnson@example.com");
        updateRequest.setTitle("Senior Developer");
        updateRequest.setLeadScore(85);
        updateRequest.setOwnerId(userId);

        ContactResponse updatedContact = contactService.updateContact(createdContact.getId(), updateRequest, userId);

        // Then
        assertThat(updatedContact.getId()).isEqualTo(createdContact.getId());
        assertThat(updatedContact.getLastName()).isEqualTo("Smith-Johnson");
        assertThat(updatedContact.getEmail()).isEqualTo("jane.smith-johnson@example.com");
        assertThat(updatedContact.getTitle()).isEqualTo("Senior Developer");
        assertThat(updatedContact.getLeadScore()).isEqualTo(85);
        assertThat(updatedContact.getUpdatedAt()).isAfter(createdContact.getUpdatedAt());
    }

    @Test
    void shouldRetrieveContactById() {
        // Given
        ContactRequest request = new ContactRequest();
        request.setFirstName("Bob");
        request.setLastName("Wilson");
        request.setEmail("bob.wilson@example.com");
        request.setOwnerId(userId);

        ContactResponse createdContact = contactService.createContact(request, userId);

        // When
        ContactResponse retrievedContact = contactService.getContact(createdContact.getId());

        // Then
        assertThat(retrievedContact).isNotNull();
        assertThat(retrievedContact.getId()).isEqualTo(createdContact.getId());
        assertThat(retrievedContact.getFirstName()).isEqualTo("Bob");
        assertThat(retrievedContact.getLastName()).isEqualTo("Wilson");
        assertThat(retrievedContact.getEmail()).isEqualTo("bob.wilson@example.com");
    }

    @Test
    void shouldDeleteContactSuccessfully() {
        // Given
        ContactRequest request = new ContactRequest();
        request.setFirstName("Alice");
        request.setLastName("Brown");
        request.setEmail("alice.brown@example.com");
        request.setOwnerId(userId);

        ContactResponse createdContact = contactService.createContact(request, userId);

        // When
        contactService.deleteContact(createdContact.getId(), userId);

        // Then
        boolean exists = contactRepository.existsById(createdContact.getId());
        assertThat(exists).isFalse();
    }
}