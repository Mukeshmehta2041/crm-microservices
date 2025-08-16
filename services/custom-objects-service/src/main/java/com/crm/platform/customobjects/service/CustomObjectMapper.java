package com.crm.platform.customobjects.service;

import com.crm.platform.customobjects.dto.*;
import com.crm.platform.customobjects.entity.*;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper component for converting between entities and DTOs
 */
@Component
public class CustomObjectMapper {

    /**
     * Convert CustomObjectRequest to CustomObject entity
     */
    public CustomObject toEntity(UUID tenantId, CustomObjectRequest request, UUID userId) {
        CustomObject customObject = new CustomObject();
        customObject.setTenantId(tenantId);
        customObject.setName(request.getName());
        customObject.setLabel(request.getLabel());
        customObject.setPluralLabel(request.getPluralLabel());
        customObject.setDescription(request.getDescription());
        customObject.setApiName(request.getApiName());
        customObject.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        customObject.setAllowReports(request.getAllowReports() != null ? request.getAllowReports() : true);
        customObject.setAllowActivities(request.getAllowActivities() != null ? request.getAllowActivities() : true);
        customObject.setRecordNameField(request.getRecordNameField());
        customObject.setIcon(request.getIcon());
        customObject.setColor(request.getColor());
        customObject.setCreatedBy(userId);
        customObject.setUpdatedBy(userId);
        return customObject;
    }

    /**
     * Update CustomObject entity from CustomObjectRequest
     */
    public void updateEntity(CustomObject customObject, CustomObjectRequest request, UUID userId) {
        customObject.setName(request.getName());
        customObject.setLabel(request.getLabel());
        customObject.setPluralLabel(request.getPluralLabel());
        customObject.setDescription(request.getDescription());
        customObject.setApiName(request.getApiName());
        customObject.setIsActive(request.getIsActive() != null ? request.getIsActive() : customObject.getIsActive());
        customObject.setAllowReports(request.getAllowReports() != null ? request.getAllowReports() : customObject.getAllowReports());
        customObject.setAllowActivities(request.getAllowActivities() != null ? request.getAllowActivities() : customObject.getAllowActivities());
        customObject.setRecordNameField(request.getRecordNameField());
        customObject.setIcon(request.getIcon());
        customObject.setColor(request.getColor());
        customObject.setUpdatedBy(userId);
    }

    /**
     * Convert CustomObject entity to CustomObjectResponse
     */
    public CustomObjectResponse toResponse(CustomObject customObject) {
        CustomObjectResponse response = new CustomObjectResponse();
        response.setId(customObject.getId());
        response.setTenantId(customObject.getTenantId());
        response.setName(customObject.getName());
        response.setLabel(customObject.getLabel());
        response.setPluralLabel(customObject.getPluralLabel());
        response.setDescription(customObject.getDescription());
        response.setApiName(customObject.getApiName());
        response.setIsActive(customObject.getIsActive());
        response.setAllowReports(customObject.getAllowReports());
        response.setAllowActivities(customObject.getAllowActivities());
        response.setRecordNameField(customObject.getRecordNameField());
        response.setIcon(customObject.getIcon());
        response.setColor(customObject.getColor());
        response.setCreatedBy(customObject.getCreatedBy());
        response.setUpdatedBy(customObject.getUpdatedBy());
        response.setCreatedAt(customObject.getCreatedAt());
        response.setUpdatedAt(customObject.getUpdatedAt());
        return response;
    }

    /**
     * Convert CustomFieldRequest to CustomField entity
     */
    public CustomField toFieldEntity(UUID tenantId, CustomFieldRequest request, CustomObject customObject, UUID userId) {
        CustomField customField = new CustomField();
        customField.setTenantId(tenantId);
        customField.setObjectType(request.getObjectType());
        customField.setCustomObject(customObject);
        customField.setFieldName(request.getFieldName());
        customField.setFieldLabel(request.getFieldLabel());
        customField.setFieldType(request.getFieldType());
        customField.setDataType(request.getDataType());
        customField.setIsRequired(request.getIsRequired() != null ? request.getIsRequired() : false);
        customField.setIsUnique(request.getIsUnique() != null ? request.getIsUnique() : false);
        customField.setIsIndexed(request.getIsIndexed() != null ? request.getIsIndexed() : false);
        customField.setDefaultValue(request.getDefaultValue());
        customField.setHelpText(request.getHelpText());
        customField.setFieldOrder(request.getFieldOrder() != null ? request.getFieldOrder() : 0);
        customField.setValidationRules(request.getValidationRules());
        customField.setDisplayOptions(request.getDisplayOptions());
        customField.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        customField.setCreatedBy(userId);
        customField.setUpdatedBy(userId);
        return customField;
    }

    /**
     * Update CustomField entity from CustomFieldRequest
     */
    public void updateFieldEntity(CustomField customField, CustomFieldRequest request, UUID userId) {
        customField.setFieldLabel(request.getFieldLabel());
        customField.setFieldType(request.getFieldType());
        customField.setDataType(request.getDataType());
        customField.setIsRequired(request.getIsRequired() != null ? request.getIsRequired() : customField.getIsRequired());
        customField.setIsUnique(request.getIsUnique() != null ? request.getIsUnique() : customField.getIsUnique());
        customField.setIsIndexed(request.getIsIndexed() != null ? request.getIsIndexed() : customField.getIsIndexed());
        customField.setDefaultValue(request.getDefaultValue());
        customField.setHelpText(request.getHelpText());
        customField.setFieldOrder(request.getFieldOrder() != null ? request.getFieldOrder() : customField.getFieldOrder());
        customField.setValidationRules(request.getValidationRules());
        customField.setDisplayOptions(request.getDisplayOptions());
        customField.setIsActive(request.getIsActive() != null ? request.getIsActive() : customField.getIsActive());
        customField.setUpdatedBy(userId);
    }

    /**
     * Convert CustomField entity to CustomFieldResponse
     */
    public CustomFieldResponse toFieldResponse(CustomField customField) {
        CustomFieldResponse response = new CustomFieldResponse();
        response.setId(customField.getId());
        response.setTenantId(customField.getTenantId());
        response.setObjectType(customField.getObjectType());
        response.setCustomObjectId(customField.getCustomObject() != null ? customField.getCustomObject().getId() : null);
        response.setFieldName(customField.getFieldName());
        response.setFieldLabel(customField.getFieldLabel());
        response.setFieldType(customField.getFieldType());
        response.setDataType(customField.getDataType());
        response.setIsRequired(customField.getIsRequired());
        response.setIsUnique(customField.getIsUnique());
        response.setIsIndexed(customField.getIsIndexed());
        response.setDefaultValue(customField.getDefaultValue());
        response.setHelpText(customField.getHelpText());
        response.setFieldOrder(customField.getFieldOrder());
        response.setValidationRules(customField.getValidationRules());
        response.setDisplayOptions(customField.getDisplayOptions());
        response.setIsActive(customField.getIsActive());
        response.setCreatedBy(customField.getCreatedBy());
        response.setUpdatedBy(customField.getUpdatedBy());
        response.setCreatedAt(customField.getCreatedAt());
        response.setUpdatedAt(customField.getUpdatedAt());

        // Map field options if present
        if (customField.getFieldOptions() != null && !customField.getFieldOptions().isEmpty()) {
            response.setFieldOptions(
                customField.getFieldOptions().stream()
                    .map(this::toFieldOptionResponse)
                    .collect(Collectors.toList())
            );
        }

        return response;
    }

    /**
     * Convert CustomFieldOptionRequest to CustomFieldOption entity
     */
    public CustomFieldOption toFieldOptionEntity(CustomFieldOptionRequest request, CustomField customField, UUID userId) {
        CustomFieldOption option = new CustomFieldOption();
        option.setCustomField(customField);
        option.setOptionLabel(request.getOptionLabel());
        option.setOptionValue(request.getOptionValue());
        option.setOptionOrder(request.getOptionOrder() != null ? request.getOptionOrder() : 0);
        option.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : false);
        option.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        option.setColor(request.getColor());
        option.setDescription(request.getDescription());
        option.setCreatedBy(userId);
        option.setUpdatedBy(userId);
        return option;
    }

    /**
     * Update CustomFieldOption entity from CustomFieldOptionRequest
     */
    public void updateFieldOptionEntity(CustomFieldOption option, CustomFieldOptionRequest request, UUID userId) {
        option.setOptionLabel(request.getOptionLabel());
        option.setOptionValue(request.getOptionValue());
        option.setOptionOrder(request.getOptionOrder() != null ? request.getOptionOrder() : option.getOptionOrder());
        option.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : option.getIsDefault());
        option.setIsActive(request.getIsActive() != null ? request.getIsActive() : option.getIsActive());
        option.setColor(request.getColor());
        option.setDescription(request.getDescription());
        option.setUpdatedBy(userId);
    }

    /**
     * Convert CustomFieldOption entity to CustomFieldOptionResponse
     */
    public CustomFieldOptionResponse toFieldOptionResponse(CustomFieldOption option) {
        CustomFieldOptionResponse response = new CustomFieldOptionResponse();
        response.setId(option.getId());
        response.setCustomFieldId(option.getCustomField().getId());
        response.setOptionLabel(option.getOptionLabel());
        response.setOptionValue(option.getOptionValue());
        response.setOptionOrder(option.getOptionOrder());
        response.setIsDefault(option.getIsDefault());
        response.setIsActive(option.getIsActive());
        response.setColor(option.getColor());
        response.setDescription(option.getDescription());
        response.setCreatedBy(option.getCreatedBy());
        response.setUpdatedBy(option.getUpdatedBy());
        response.setCreatedAt(option.getCreatedAt());
        response.setUpdatedAt(option.getUpdatedAt());
        return response;
    }

    /**
     * Convert CustomObjectRecordRequest to CustomObjectRecord entity
     */
    public CustomObjectRecord toRecordEntity(UUID tenantId, CustomObjectRecordRequest request, 
                                           CustomObject customObject, UUID userId) {
        CustomObjectRecord record = new CustomObjectRecord();
        record.setTenantId(tenantId);
        record.setCustomObject(customObject);
        record.setRecordName(request.getRecordName());
        record.setFieldValues(request.getFieldValues());
        record.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        record.setOwnerId(request.getOwnerId() != null ? request.getOwnerId() : userId);
        record.setCreatedBy(userId);
        record.setUpdatedBy(userId);
        return record;
    }

    /**
     * Update CustomObjectRecord entity from CustomObjectRecordRequest
     */
    public void updateRecordEntity(CustomObjectRecord record, CustomObjectRecordRequest request, UUID userId) {
        record.setRecordName(request.getRecordName());
        record.setFieldValues(request.getFieldValues());
        record.setIsActive(request.getIsActive() != null ? request.getIsActive() : record.getIsActive());
        if (request.getOwnerId() != null) {
            record.setOwnerId(request.getOwnerId());
        }
        record.setUpdatedBy(userId);
    }

    /**
     * Convert CustomObjectRecord entity to CustomObjectRecordResponse
     */
    public CustomObjectRecordResponse toRecordResponse(CustomObjectRecord record) {
        CustomObjectRecordResponse response = new CustomObjectRecordResponse();
        response.setId(record.getId());
        response.setTenantId(record.getTenantId());
        response.setCustomObjectId(record.getCustomObject().getId());
        response.setCustomObjectName(record.getCustomObject().getName());
        response.setCustomObjectLabel(record.getCustomObject().getLabel());
        response.setRecordName(record.getRecordName());
        response.setFieldValues(record.getFieldValues());
        response.setIsActive(record.getIsActive());
        response.setOwnerId(record.getOwnerId());
        response.setCreatedBy(record.getCreatedBy());
        response.setUpdatedBy(record.getUpdatedBy());
        response.setCreatedAt(record.getCreatedAt());
        response.setUpdatedAt(record.getUpdatedAt());
        return response;
    }
}