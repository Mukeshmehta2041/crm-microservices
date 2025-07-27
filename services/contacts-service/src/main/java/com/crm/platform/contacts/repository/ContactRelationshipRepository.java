package com.crm.platform.contacts.repository;

import com.crm.platform.contacts.entity.ContactRelationship;
import com.crm.platform.contacts.entity.RelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContactRelationshipRepository extends JpaRepository<ContactRelationship, UUID> {

    List<ContactRelationship> findByTenantIdAndContactId(UUID tenantId, UUID contactId);
    
    List<ContactRelationship> findByTenantIdAndRelatedContactId(UUID tenantId, UUID relatedContactId);
    
    List<ContactRelationship> findByTenantIdAndContactIdAndRelationshipType(
        UUID tenantId, UUID contactId, RelationshipType relationshipType);
    
    @Query("SELECT cr FROM ContactRelationship cr WHERE cr.tenantId = :tenantId AND " +
           "(cr.contact.id = :contactId OR cr.relatedContact.id = :contactId)")
    List<ContactRelationship> findAllRelationshipsForContact(@Param("tenantId") UUID tenantId, 
                                                             @Param("contactId") UUID contactId);
    
    boolean existsByTenantIdAndContactIdAndRelatedContactId(UUID tenantId, UUID contactId, UUID relatedContactId);
    
    void deleteByTenantIdAndContactId(UUID tenantId, UUID contactId);
    
    void deleteByTenantIdAndRelatedContactId(UUID tenantId, UUID relatedContactId);
}