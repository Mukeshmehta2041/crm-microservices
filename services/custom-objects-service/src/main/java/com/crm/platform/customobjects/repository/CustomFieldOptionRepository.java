package com.crm.platform.customobjects.repository;

import com.crm.platform.customobjects.entity.CustomField;
import com.crm.platform.customobjects.entity.CustomFieldOption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for CustomFieldOption entity
 */
@Repository
public interface CustomFieldOptionRepository extends JpaRepository<CustomFieldOption, UUID>, 
                                                    JpaSpecificationExecutor<CustomFieldOption> {

    /**
     * Find custom field option by ID
     */
    Optional<CustomFieldOption> findById(UUID id);

    /**
     * Find custom field option by custom field and option value
     */
    Optional<CustomFieldOption> findByCustomFieldAndOptionValue(CustomField customField, String optionValue);

    /**
     * Find all options by custom field
     */
    List<CustomFieldOption> findByCustomFieldOrderByOptionOrderAsc(CustomField customField);

    /**
     * Find all active options by custom field
     */
    List<CustomFieldOption> findByCustomFieldAndIsActiveTrueOrderByOptionOrderAsc(CustomField customField);

    /**
     * Find options by custom field with pagination
     */
    Page<CustomFieldOption> findByCustomField(CustomField customField, Pageable pageable);

    /**
     * Find active options by custom field with pagination
     */
    Page<CustomFieldOption> findByCustomFieldAndIsActiveTrue(CustomField customField, Pageable pageable);

    /**
     * Find default option by custom field
     */
    Optional<CustomFieldOption> findByCustomFieldAndIsDefaultTrue(CustomField customField);

    /**
     * Find default options by custom field (for multipicklist)
     */
    List<CustomFieldOption> findByCustomFieldAndIsDefaultTrueOrderByOptionOrderAsc(CustomField customField);

    /**
     * Check if option value exists for custom field
     */
    boolean existsByCustomFieldAndOptionValue(CustomField customField, String optionValue);

    /**
     * Check if option value exists for custom field excluding specific ID
     */
    boolean existsByCustomFieldAndOptionValueAndIdNot(CustomField customField, String optionValue, UUID id);

    /**
     * Check if default option exists for custom field
     */
    boolean existsByCustomFieldAndIsDefaultTrue(CustomField customField);

    /**
     * Count options by custom field
     */
    long countByCustomField(CustomField customField);

    /**
     * Count active options by custom field
     */
    long countByCustomFieldAndIsActiveTrue(CustomField customField);

    /**
     * Delete options by custom field
     */
    void deleteByCustomField(CustomField customField);

    /**
     * Find options by custom field and search term
     */
    List<CustomFieldOption> findByCustomFieldAndOptionLabelContainingIgnoreCaseOrderByOptionOrderAsc(
            CustomField customField, String searchTerm);

    /**
     * Find active options by custom field and search term
     */
    List<CustomFieldOption> findByCustomFieldAndIsActiveTrueAndOptionLabelContainingIgnoreCaseOrderByOptionOrderAsc(
            CustomField customField, String searchTerm);
}