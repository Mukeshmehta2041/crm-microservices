package com.crm.platform.customobjects.service;

import com.crm.platform.customobjects.dto.*;
import com.crm.platform.customobjects.entity.CustomObject;
import com.crm.platform.customobjects.event.CustomObjectCreatedEvent;
import com.crm.platform.customobjects.event.CustomObjectUpdatedEvent;
import com.crm.platform.customobjects.exception.CustomObjectBusinessException;
import com.crm.platform.customobjects.repository.CustomObjectRepository;
import com.crm.platform.customobjects.repository.CustomFieldRepository;
import com.crm.platform.customobjects.repository.CustomObjectRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for managing custom objects
 */
@Service
@Transactional
public class CustomObjectService {

    private static final Logger logger = LoggerFactory.getLogger(CustomObjectService.class);

    private final CustomObjectRepository customObjectRepository;
    private final CustomFieldRepository customFieldRepository;
    private final CustomObjectRecordRepository customObjectRecordRepository;
    private final CustomObjectMapper customObjectMapper;
    private final CustomObjectValidationService validationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public CustomObjectService(CustomObjectRepository customObjectRepository,
                              CustomFieldRepository customFieldRepository,
                              CustomObjectRecordRepository customObjectRecordRepository,
                              CustomObjectMapper customObjectMapper,
                              CustomObjectValidationService validationService,
                              KafkaTemplate<String, Object> kafkaTemplate) {
        this.customObjectRepository = customObjectRepository;
        this.customFieldRepository = customFieldRepository;
        this.customObjectRecordRepository = customObjectRecordRepository;
        this.customObjectMapper = customObjectMapper;
        this.validationService = validationService;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Create a new custom object
     */
    public CustomObjectResponse createCustomObject(UUID tenantId, CustomObjectRequest request, UUID userId) {
        logger.info("Creating custom object for tenant: {}, name: {}", tenantId, request.getName());

        // Validate request
        validationService.validateCustomObjectRequest(tenantId, request);

        // Check if custom object with same name or API name already exists
        if (customObjectRepository.existsByTenantIdAndName(tenantId, request.getName())) {
            throw new CustomObjectBusinessException.CustomObjectAlreadyExistsException(
                "Custom object with name '" + request.getName() + "' already exists",
                Map.of("name", request.getName(), "tenantId", tenantId)
            );
        }

        if (customObjectRepository.existsByTenantIdAndApiName(tenantId, request.getApiName())) {
            throw new CustomObjectBusinessException.CustomObjectAlreadyExistsException(
                "Custom object with API name '" + request.getApiName() + "' already exists",
                Map.of("apiName", request.getApiName(), "tenantId", tenantId)
            );
        }

        // Create custom object entity
        CustomObject customObject = customObjectMapper.toEntity(tenantId, request, userId);
        customObject = customObjectRepository.save(customObject);

        // Publish event
        publishCustomObjectCreatedEvent(customObject);

        logger.info("Created custom object with ID: {} for tenant: {}", customObject.getId(), tenantId);
        return customObjectMapper.toResponse(customObject);
    }

    /**
     * Update an existing custom object
     */
    @CacheEvict(value = "customObjects", key = "#tenantId + ':' + #customObjectId")
    public CustomObjectResponse updateCustomObject(UUID tenantId, UUID customObjectId, 
                                                  CustomObjectRequest request, UUID userId) {
        logger.info("Updating custom object: {} for tenant: {}", customObjectId, tenantId);

        // Find existing custom object
        CustomObject customObject = customObjectRepository.findByTenantIdAndId(tenantId, customObjectId)
            .orElseThrow(() -> new CustomObjectBusinessException.CustomObjectNotFoundException(
                "Custom object not found with ID: " + customObjectId,
                Map.of("customObjectId", customObjectId, "tenantId", tenantId)
            ));

        // Validate request
        validationService.validateCustomObjectRequest(tenantId, request);

        // Check for name conflicts (excluding current object)
        if (!customObject.getName().equals(request.getName()) && 
            customObjectRepository.existsByTenantIdAndNameAndIdNot(tenantId, request.getName(), customObjectId)) {
            throw new CustomObjectBusinessException.CustomObjectAlreadyExistsException(
                "Custom object with name '" + request.getName() + "' already exists",
                Map.of("name", request.getName(), "tenantId", tenantId)
            );
        }

        if (!customObject.getApiName().equals(request.getApiName()) && 
            customObjectRepository.existsByTenantIdAndApiNameAndIdNot(tenantId, request.getApiName(), customObjectId)) {
            throw new CustomObjectBusinessException.CustomObjectAlreadyExistsException(
                "Custom object with API name '" + request.getApiName() + "' already exists",
                Map.of("apiName", request.getApiName(), "tenantId", tenantId)
            );
        }

        // Update custom object
        customObjectMapper.updateEntity(customObject, request, userId);
        customObject = customObjectRepository.save(customObject);

        // Publish event
        publishCustomObjectUpdatedEvent(customObject);

        logger.info("Updated custom object: {} for tenant: {}", customObjectId, tenantId);
        return customObjectMapper.toResponse(customObject);
    }

    /**
     * Get custom object by ID
     */
    @Cacheable(value = "customObjects", key = "#tenantId + ':' + #customObjectId")
    @Transactional(readOnly = true)
    public CustomObjectResponse getCustomObject(UUID tenantId, UUID customObjectId, boolean includeFields) {
        logger.debug("Getting custom object: {} for tenant: {}", customObjectId, tenantId);

        CustomObject customObject = customObjectRepository.findByTenantIdAndId(tenantId, customObjectId)
            .orElseThrow(() -> new CustomObjectBusinessException.CustomObjectNotFoundException(
                "Custom object not found with ID: " + customObjectId,
                Map.of("customObjectId", customObjectId, "tenantId", tenantId)
            ));

        CustomObjectResponse response = customObjectMapper.toResponse(customObject);

        if (includeFields) {
            // Load custom fields
            response.setCustomFields(
                customFieldRepository.findByCustomObjectAndIsActiveTrueOrderByFieldOrderAsc(customObject)
                    .stream()
                    .map(customObjectMapper::toFieldResponse)
                    .collect(Collectors.toList())
            );
        }

        // Add record count
        response.setRecordCount(customObjectRecordRepository.countByTenantIdAndCustomObjectAndIsActiveTrue(tenantId, customObject));
        response.setFieldCount(customFieldRepository.countByCustomObjectAndIsActiveTrue(customObject));

        return response;
    }

    /**
     * Get custom object by name
     */
    @Transactional(readOnly = true)
    public CustomObjectResponse getCustomObjectByName(UUID tenantId, String name, boolean includeFields) {
        logger.debug("Getting custom object by name: {} for tenant: {}", name, tenantId);

        CustomObject customObject = customObjectRepository.findByTenantIdAndName(tenantId, name)
            .orElseThrow(() -> new CustomObjectBusinessException.CustomObjectNotFoundException(
                "Custom object not found with name: " + name,
                Map.of("name", name, "tenantId", tenantId)
            ));

        return getCustomObject(tenantId, customObject.getId(), includeFields);
    }

    /**
     * Get custom object by API name
     */
    @Transactional(readOnly = true)
    public CustomObjectResponse getCustomObjectByApiName(UUID tenantId, String apiName, boolean includeFields) {
        logger.debug("Getting custom object by API name: {} for tenant: {}", apiName, tenantId);

        CustomObject customObject = customObjectRepository.findByTenantIdAndApiName(tenantId, apiName)
            .orElseThrow(() -> new CustomObjectBusinessException.CustomObjectNotFoundException(
                "Custom object not found with API name: " + apiName,
                Map.of("apiName", apiName, "tenantId", tenantId)
            ));

        return getCustomObject(tenantId, customObject.getId(), includeFields);
    }

    /**
     * Get all custom objects for tenant
     */
    @Transactional(readOnly = true)
    public List<CustomObjectResponse> getAllCustomObjects(UUID tenantId, boolean activeOnly) {
        logger.debug("Getting all custom objects for tenant: {}, activeOnly: {}", tenantId, activeOnly);

        List<CustomObject> customObjects = activeOnly 
            ? customObjectRepository.findByTenantIdAndIsActiveTrueOrderByCreatedAtDesc(tenantId)
            : customObjectRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);

        return customObjects.stream()
            .map(customObjectMapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Search custom objects
     */
    @Transactional(readOnly = true)
    public Page<CustomObjectResponse> searchCustomObjects(UUID tenantId, CustomObjectSearchRequest searchRequest) {
        logger.debug("Searching custom objects for tenant: {} with request: {}", tenantId, searchRequest);

        // Create pageable
        Sort sort = Sort.by(
            "desc".equalsIgnoreCase(searchRequest.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
            searchRequest.getSortBy()
        );
        Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);

        Page<CustomObject> customObjects;

        if (searchRequest.getSearchTerm() != null && !searchRequest.getSearchTerm().trim().isEmpty()) {
            customObjects = customObjectRepository.findByTenantIdAndSearchTerm(
                tenantId, searchRequest.getSearchTerm().trim(), pageable);
        } else {
            if (searchRequest.getIsActive() != null && searchRequest.getIsActive()) {
                customObjects = customObjectRepository.findByTenantIdAndIsActiveTrue(tenantId, pageable);
            } else {
                customObjects = customObjectRepository.findByTenantId(tenantId, pageable);
            }
        }

        return customObjects.map(customObject -> {
            CustomObjectResponse response = customObjectMapper.toResponse(customObject);
            
            if (searchRequest.getIncludeRecordCount()) {
                response.setRecordCount(customObjectRecordRepository.countByTenantIdAndCustomObjectAndIsActiveTrue(tenantId, customObject));
            }
            
            if (searchRequest.getIncludeFields()) {
                response.setCustomFields(
                    customFieldRepository.findByCustomObjectAndIsActiveTrueOrderByFieldOrderAsc(customObject)
                        .stream()
                        .map(customObjectMapper::toFieldResponse)
                        .collect(Collectors.toList())
                );
            }
            
            return response;
        });
    }

    /**
     * Delete custom object (soft delete)
     */
    @CacheEvict(value = "customObjects", key = "#tenantId + ':' + #customObjectId")
    public void deleteCustomObject(UUID tenantId, UUID customObjectId, UUID userId) {
        logger.info("Deleting custom object: {} for tenant: {}", customObjectId, tenantId);

        CustomObject customObject = customObjectRepository.findByTenantIdAndId(tenantId, customObjectId)
            .orElseThrow(() -> new CustomObjectBusinessException.CustomObjectNotFoundException(
                "Custom object not found with ID: " + customObjectId,
                Map.of("customObjectId", customObjectId, "tenantId", tenantId)
            ));

        // Soft delete by setting isActive to false
        customObject.setIsActive(false);
        customObject.setUpdatedBy(userId);
        customObjectRepository.save(customObject);

        // Soft delete all related records
        customObjectRecordRepository.softDeleteByTenantIdAndCustomObject(tenantId, customObject, userId);

        logger.info("Deleted custom object: {} for tenant: {}", customObjectId, tenantId);
    }

    /**
     * Get custom objects that allow reports
     */
    @Transactional(readOnly = true)
    public List<CustomObjectResponse> getCustomObjectsForReports(UUID tenantId) {
        logger.debug("Getting custom objects for reports for tenant: {}", tenantId);

        return customObjectRepository.findByTenantIdAndAllowReportsTrueAndIsActiveTrueOrderByName(tenantId)
            .stream()
            .map(customObjectMapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get custom objects that allow activities
     */
    @Transactional(readOnly = true)
    public List<CustomObjectResponse> getCustomObjectsForActivities(UUID tenantId) {
        logger.debug("Getting custom objects for activities for tenant: {}", tenantId);

        return customObjectRepository.findByTenantIdAndAllowActivitiesTrueAndIsActiveTrueOrderByName(tenantId)
            .stream()
            .map(customObjectMapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get custom object statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCustomObjectStatistics(UUID tenantId) {
        logger.debug("Getting custom object statistics for tenant: {}", tenantId);

        long totalObjects = customObjectRepository.countByTenantId(tenantId);
        long activeObjects = customObjectRepository.countByTenantIdAndIsActiveTrue(tenantId);

        return Map.of(
            "totalObjects", totalObjects,
            "activeObjects", activeObjects,
            "inactiveObjects", totalObjects - activeObjects
        );
    }

    // Private helper methods

    private void publishCustomObjectCreatedEvent(CustomObject customObject) {
        try {
            CustomObjectCreatedEvent event = new CustomObjectCreatedEvent(
                customObject.getId(),
                customObject.getTenantId(),
                customObject.getName(),
                customObject.getLabel(),
                customObject.getApiName(),
                customObject.getCreatedBy(),
                customObject.getCreatedAt()
            );
            
            kafkaTemplate.send("crm.custom-objects.created", customObject.getId().toString(), event);
            logger.debug("Published CustomObjectCreatedEvent for object: {}", customObject.getId());
        } catch (Exception e) {
            logger.error("Failed to publish CustomObjectCreatedEvent for object: {}", customObject.getId(), e);
        }
    }

    private void publishCustomObjectUpdatedEvent(CustomObject customObject) {
        try {
            CustomObjectUpdatedEvent event = new CustomObjectUpdatedEvent(
                customObject.getId(),
                customObject.getTenantId(),
                customObject.getName(),
                customObject.getLabel(),
                customObject.getApiName(),
                customObject.getIsActive(),
                customObject.getUpdatedBy(),
                customObject.getUpdatedAt()
            );
            
            kafkaTemplate.send("crm.custom-objects.updated", customObject.getId().toString(), event);
            logger.debug("Published CustomObjectUpdatedEvent for object: {}", customObject.getId());
        } catch (Exception e) {
            logger.error("Failed to publish CustomObjectUpdatedEvent for object: {}", customObject.getId(), e);
        }
    }
}