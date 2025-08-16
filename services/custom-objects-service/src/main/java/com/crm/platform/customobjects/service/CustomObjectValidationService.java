package com.crm.platform.customobjects.service;

import com.crm.platform.customobjects.dto.CustomFieldRequest;
import com.crm.platform.customobjects.dto.CustomObjectRecordRequest;
import com.crm.platform.customobjects.dto.CustomObjectRequest;
import com.crm.platform.customobjects.entity.CustomField;
import com.crm.platform.customobjects.entity.CustomObject;
import com.crm.platform.customobjects.exception.CustomObjectBusinessException;
import com.crm.platform.customobjects.repository.CustomFieldRepository;
import com.crm.platform.customobjects.repository.CustomObjectRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Service for validating custom objects, fields, and records
 */
@Service
public class CustomObjectValidationService {

    private static final Logger logger = LoggerFactory.getLogger(CustomObjectValidationService.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[1-9]?[0-9]{7,15}$"
    );
    
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$"
    );

    private final CustomFieldRepository customFieldRepository;
    private final CustomObjectRecordRepository customObjectRecordRepository;

    @Value("${app.custom-objects.max-fields-per-object:100}")
    private int maxFieldsPerObject;

    @Value("${app.validation.enable-strict-validation:true}")
    private boolean enableStrictValidation;

    @Autowired
    public CustomObjectValidationService(CustomFieldRepository customFieldRepository,
                                       CustomObjectRecordRepository customObjectRecordRepository) {
        this.customFieldRepository = customFieldRepository;
        this.customObjectRecordRepository = customObjectRecordRepository;
    }

    /**
     * Validate custom object request
     */
    public void validateCustomObjectRequest(UUID tenantId, CustomObjectRequest request) {
        logger.debug("Validating custom object request for tenant: {}", tenantId);

        if (request == null) {
            throw new CustomObjectBusinessException.CustomFieldValidationException("Request cannot be null");
        }

        // Validate required fields
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new CustomObjectBusinessException.CustomFieldValidationException("Name is required");
        }

        if (request.getLabel() == null || request.getLabel().trim().isEmpty()) {
            throw new CustomObjectBusinessException.CustomFieldValidationException("Label is required");
        }

        if (request.getPluralLabel() == null || request.getPluralLabel().trim().isEmpty()) {
            throw new CustomObjectBusinessException.CustomFieldValidationException("Plural label is required");
        }

        if (request.getApiName() == null || request.getApiName().trim().isEmpty()) {
            throw new CustomObjectBusinessException.CustomFieldValidationException("API name is required");
        }

        // Validate API name format
        if (!request.getApiName().matches("^[a-z][a-z0-9_]*[a-z0-9]$")) {
            throw new CustomObjectBusinessException.CustomFieldValidationException(
                "API name must be lowercase with underscores and start/end with alphanumeric characters"
            );
        }

        // Validate color format if provided
        if (request.getColor() != null && !request.getColor().matches("^#[0-9A-Fa-f]{6}$")) {
            throw new CustomObjectBusinessException.CustomFieldValidationException(
                "Color must be a valid hex color code (e.g., #FF0000)"
            );
        }

        logger.debug("Custom object request validation passed for tenant: {}", tenantId);
    }

    /**
     * Validate custom field request
     */
    public void validateCustomFieldRequest(UUID tenantId, CustomFieldRequest request, CustomObject customObject) {
        logger.debug("Validating custom field request for tenant: {} and object: {}", tenantId, customObject.getId());

        if (request == null) {
            throw new CustomObjectBusinessException.CustomFieldValidationException("Request cannot be null");
        }

        // Validate required fields
        if (request.getFieldName() == null || request.getFieldName().trim().isEmpty()) {
            throw new CustomObjectBusinessException.CustomFieldValidationException("Field name is required");
        }

        if (request.getFieldLabel() == null || request.getFieldLabel().trim().isEmpty()) {
            throw new CustomObjectBusinessException.CustomFieldValidationException("Field label is required");
        }

        if (request.getFieldType() == null) {
            throw new CustomObjectBusinessException.CustomFieldValidationException("Field type is required");
        }

        if (request.getDataType() == null) {
            throw new CustomObjectBusinessException.CustomFieldValidationException("Data type is required");
        }

        // Validate field name format (alphanumeric and underscores only)
        if (!request.getFieldName().matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
            throw new CustomObjectBusinessException.CustomFieldValidationException(
                "Field name must start with a letter and contain only letters, numbers, and underscores"
            );
        }

        // Check field count limit
        if (enableStrictValidation) {
            long currentFieldCount = customFieldRepository.countByCustomObjectAndIsActiveTrue(customObject);
            if (currentFieldCount >= maxFieldsPerObject) {
                throw new CustomObjectBusinessException.CustomFieldValidationException(
                    "Maximum number of fields (" + maxFieldsPerObject + ") exceeded for custom object"
                );
            }
        }

        // Validate field type and data type compatibility
        validateFieldTypeDataTypeCompatibility(request.getFieldType(), request.getDataType());

        // Validate default value if provided
        if (request.getDefaultValue() != null && !request.getDefaultValue().trim().isEmpty()) {
            validateDefaultValue(request.getDefaultValue(), request.getFieldType(), request.getDataType());
        }

        // Validate validation rules if provided
        if (request.getValidationRules() != null && !request.getValidationRules().isEmpty()) {
            validateValidationRules(request.getValidationRules(), request.getFieldType());
        }

        logger.debug("Custom field request validation passed for tenant: {}", tenantId);
    }

    /**
     * Validate custom object record request
     */
    public void validateCustomObjectRecordRequest(UUID tenantId, CustomObjectRecordRequest request, 
                                                CustomObject customObject, List<CustomField> customFields,
                                                UUID recordId) {
        logger.debug("Validating custom object record request for tenant: {} and object: {}", 
                    tenantId, customObject.getId());

        if (request == null) {
            throw new CustomObjectBusinessException.CustomFieldValidationException("Request cannot be null");
        }

        if (request.getFieldValues() == null) {
            throw new CustomObjectBusinessException.CustomFieldValidationException("Field values cannot be null");
        }

        // Validate required fields
        for (CustomField field : customFields) {
            if (field.getIsRequired() && field.getIsActive()) {
                Object value = request.getFieldValues().get(field.getFieldName());
                if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                    throw new CustomObjectBusinessException.RequiredFieldValidationException(
                        "Required field '" + field.getFieldLabel() + "' is missing or empty",
                        Map.of("fieldName", field.getFieldName(), "fieldLabel", field.getFieldLabel())
                    );
                }
            }
        }

        // Validate unique fields
        for (CustomField field : customFields) {
            if (field.getIsUnique() && field.getIsActive()) {
                Object value = request.getFieldValues().get(field.getFieldName());
                if (value != null) {
                    String stringValue = value.toString();
                    if (!stringValue.trim().isEmpty()) {
                        long count = customObjectRecordRepository.countByTenantIdAndCustomObjectAndFieldValueAndIdNot(
                            tenantId, customObject.getId(), field.getFieldName(), stringValue, recordId
                        );
                        if (count > 0) {
                            throw new CustomObjectBusinessException.UniqueConstraintViolationException(
                                "Value '" + stringValue + "' already exists for unique field '" + field.getFieldLabel() + "'",
                                Map.of("fieldName", field.getFieldName(), "fieldLabel", field.getFieldLabel(), "value", stringValue)
                            );
                        }
                    }
                }
            }
        }

        // Validate field values
        for (CustomField field : customFields) {
            if (field.getIsActive()) {
                Object value = request.getFieldValues().get(field.getFieldName());
                if (value != null) {
                    validateFieldValue(value, field);
                }
            }
        }

        logger.debug("Custom object record request validation passed for tenant: {}", tenantId);
    }

    // Private validation methods

    private void validateFieldTypeDataTypeCompatibility(CustomField.FieldType fieldType, CustomField.DataType dataType) {
        switch (fieldType) {
            case TEXT:
            case EMAIL:
            case PHONE:
            case URL:
                if (dataType != CustomField.DataType.VARCHAR && dataType != CustomField.DataType.TEXT) {
                    throw new CustomObjectBusinessException.CustomFieldValidationException(
                        "Field type " + fieldType + " is not compatible with data type " + dataType
                    );
                }
                break;
            case TEXTAREA:
                if (dataType != CustomField.DataType.TEXT) {
                    throw new CustomObjectBusinessException.CustomFieldValidationException(
                        "Field type TEXTAREA requires TEXT data type"
                    );
                }
                break;
            case NUMBER:
                if (dataType != CustomField.DataType.INTEGER) {
                    throw new CustomObjectBusinessException.CustomFieldValidationException(
                        "Field type NUMBER requires INTEGER data type"
                    );
                }
                break;
            case DECIMAL:
            case CURRENCY:
            case PERCENT:
                if (dataType != CustomField.DataType.DECIMAL) {
                    throw new CustomObjectBusinessException.CustomFieldValidationException(
                        "Field type " + fieldType + " requires DECIMAL data type"
                    );
                }
                break;
            case DATE:
                if (dataType != CustomField.DataType.DATE) {
                    throw new CustomObjectBusinessException.CustomFieldValidationException(
                        "Field type DATE requires DATE data type"
                    );
                }
                break;
            case DATETIME:
                if (dataType != CustomField.DataType.TIMESTAMP) {
                    throw new CustomObjectBusinessException.CustomFieldValidationException(
                        "Field type DATETIME requires TIMESTAMP data type"
                    );
                }
                break;
            case BOOLEAN:
                if (dataType != CustomField.DataType.BOOLEAN) {
                    throw new CustomObjectBusinessException.CustomFieldValidationException(
                        "Field type BOOLEAN requires BOOLEAN data type"
                    );
                }
                break;
            case PICKLIST:
            case MULTIPICKLIST:
                if (dataType != CustomField.DataType.VARCHAR && dataType != CustomField.DataType.JSONB) {
                    throw new CustomObjectBusinessException.CustomFieldValidationException(
                        "Field type " + fieldType + " requires VARCHAR or JSONB data type"
                    );
                }
                break;
            case LOOKUP:
            case MASTER_DETAIL:
                if (dataType != CustomField.DataType.UUID) {
                    throw new CustomObjectBusinessException.CustomFieldValidationException(
                        "Field type " + fieldType + " requires UUID data type"
                    );
                }
                break;
        }
    }

    private void validateDefaultValue(String defaultValue, CustomField.FieldType fieldType, CustomField.DataType dataType) {
        try {
            switch (fieldType) {
                case NUMBER:
                    Integer.parseInt(defaultValue);
                    break;
                case DECIMAL:
                case CURRENCY:
                case PERCENT:
                    new BigDecimal(defaultValue);
                    break;
                case DATE:
                    LocalDate.parse(defaultValue);
                    break;
                case DATETIME:
                    LocalDateTime.parse(defaultValue);
                    break;
                case BOOLEAN:
                    Boolean.parseBoolean(defaultValue);
                    break;
                case EMAIL:
                    if (!EMAIL_PATTERN.matcher(defaultValue).matches()) {
                        throw new CustomObjectBusinessException.CustomFieldValidationException(
                            "Default value is not a valid email address"
                        );
                    }
                    break;
                case PHONE:
                    if (!PHONE_PATTERN.matcher(defaultValue).matches()) {
                        throw new CustomObjectBusinessException.CustomFieldValidationException(
                            "Default value is not a valid phone number"
                        );
                    }
                    break;
                case URL:
                    if (!URL_PATTERN.matcher(defaultValue).matches()) {
                        throw new CustomObjectBusinessException.CustomFieldValidationException(
                            "Default value is not a valid URL"
                        );
                    }
                    break;
                case LOOKUP:
                case MASTER_DETAIL:
                    UUID.fromString(defaultValue);
                    break;
            }
        } catch (Exception e) {
            throw new CustomObjectBusinessException.CustomFieldValidationException(
                "Default value '" + defaultValue + "' is not valid for field type " + fieldType
            );
        }
    }

    private void validateValidationRules(Map<String, Object> validationRules, CustomField.FieldType fieldType) {
        // Validate common rules
        if (validationRules.containsKey("minLength")) {
            Object minLength = validationRules.get("minLength");
            if (!(minLength instanceof Number) || ((Number) minLength).intValue() < 0) {
                throw new CustomObjectBusinessException.CustomFieldValidationException(
                    "minLength must be a non-negative number"
                );
            }
        }

        if (validationRules.containsKey("maxLength")) {
            Object maxLength = validationRules.get("maxLength");
            if (!(maxLength instanceof Number) || ((Number) maxLength).intValue() < 0) {
                throw new CustomObjectBusinessException.CustomFieldValidationException(
                    "maxLength must be a non-negative number"
                );
            }
        }

        // Validate field type specific rules
        switch (fieldType) {
            case NUMBER:
            case DECIMAL:
            case CURRENCY:
            case PERCENT:
                validateNumericValidationRules(validationRules);
                break;
            case TEXT:
            case TEXTAREA:
                validateTextValidationRules(validationRules);
                break;
        }
    }

    private void validateNumericValidationRules(Map<String, Object> validationRules) {
        if (validationRules.containsKey("min")) {
            Object min = validationRules.get("min");
            if (!(min instanceof Number)) {
                throw new CustomObjectBusinessException.CustomFieldValidationException(
                    "min must be a number"
                );
            }
        }

        if (validationRules.containsKey("max")) {
            Object max = validationRules.get("max");
            if (!(max instanceof Number)) {
                throw new CustomObjectBusinessException.CustomFieldValidationException(
                    "max must be a number"
                );
            }
        }
    }

    private void validateTextValidationRules(Map<String, Object> validationRules) {
        if (validationRules.containsKey("pattern")) {
            Object pattern = validationRules.get("pattern");
            if (!(pattern instanceof String)) {
                throw new CustomObjectBusinessException.CustomFieldValidationException(
                    "pattern must be a string"
                );
            }
            try {
                Pattern.compile((String) pattern);
            } catch (Exception e) {
                throw new CustomObjectBusinessException.CustomFieldValidationException(
                    "pattern is not a valid regular expression"
                );
            }
        }
    }

    private void validateFieldValue(Object value, CustomField field) {
        if (value == null) {
            return;
        }

        String stringValue = value.toString();
        if (stringValue.trim().isEmpty()) {
            return;
        }

        try {
            switch (field.getFieldType()) {
                case NUMBER:
                    Integer.parseInt(stringValue);
                    break;
                case DECIMAL:
                case CURRENCY:
                case PERCENT:
                    new BigDecimal(stringValue);
                    break;
                case DATE:
                    LocalDate.parse(stringValue);
                    break;
                case DATETIME:
                    LocalDateTime.parse(stringValue);
                    break;
                case BOOLEAN:
                    Boolean.parseBoolean(stringValue);
                    break;
                case EMAIL:
                    if (!EMAIL_PATTERN.matcher(stringValue).matches()) {
                        throw new CustomObjectBusinessException.CustomFieldValidationException(
                            "Field '" + field.getFieldLabel() + "' must be a valid email address"
                        );
                    }
                    break;
                case PHONE:
                    if (!PHONE_PATTERN.matcher(stringValue).matches()) {
                        throw new CustomObjectBusinessException.CustomFieldValidationException(
                            "Field '" + field.getFieldLabel() + "' must be a valid phone number"
                        );
                    }
                    break;
                case URL:
                    if (!URL_PATTERN.matcher(stringValue).matches()) {
                        throw new CustomObjectBusinessException.CustomFieldValidationException(
                            "Field '" + field.getFieldLabel() + "' must be a valid URL"
                        );
                    }
                    break;
                case LOOKUP:
                case MASTER_DETAIL:
                    UUID.fromString(stringValue);
                    break;
            }

            // Validate against custom validation rules
            if (field.getValidationRules() != null && !field.getValidationRules().isEmpty()) {
                validateFieldValueAgainstRules(stringValue, field);
            }

        } catch (Exception e) {
            if (e instanceof CustomObjectBusinessException) {
                throw e;
            }
            throw new CustomObjectBusinessException.CustomFieldValidationException(
                "Field '" + field.getFieldLabel() + "' has invalid value: " + stringValue
            );
        }
    }

    private void validateFieldValueAgainstRules(String value, CustomField field) {
        Map<String, Object> rules = field.getValidationRules();

        // Check length constraints
        if (rules.containsKey("minLength")) {
            int minLength = ((Number) rules.get("minLength")).intValue();
            if (value.length() < minLength) {
                throw new CustomObjectBusinessException.CustomFieldValidationException(
                    "Field '" + field.getFieldLabel() + "' must be at least " + minLength + " characters long"
                );
            }
        }

        if (rules.containsKey("maxLength")) {
            int maxLength = ((Number) rules.get("maxLength")).intValue();
            if (value.length() > maxLength) {
                throw new CustomObjectBusinessException.CustomFieldValidationException(
                    "Field '" + field.getFieldLabel() + "' must not exceed " + maxLength + " characters"
                );
            }
        }

        // Check pattern constraint
        if (rules.containsKey("pattern")) {
            String pattern = (String) rules.get("pattern");
            if (!value.matches(pattern)) {
                throw new CustomObjectBusinessException.CustomFieldValidationException(
                    "Field '" + field.getFieldLabel() + "' does not match required pattern"
                );
            }
        }

        // Check numeric constraints
        if (field.getFieldType() == CustomField.FieldType.NUMBER ||
            field.getFieldType() == CustomField.FieldType.DECIMAL ||
            field.getFieldType() == CustomField.FieldType.CURRENCY ||
            field.getFieldType() == CustomField.FieldType.PERCENT) {
            
            BigDecimal numericValue = new BigDecimal(value);
            
            if (rules.containsKey("min")) {
                BigDecimal min = new BigDecimal(rules.get("min").toString());
                if (numericValue.compareTo(min) < 0) {
                    throw new CustomObjectBusinessException.CustomFieldValidationException(
                        "Field '" + field.getFieldLabel() + "' must be at least " + min
                    );
                }
            }

            if (rules.containsKey("max")) {
                BigDecimal max = new BigDecimal(rules.get("max").toString());
                if (numericValue.compareTo(max) > 0) {
                    throw new CustomObjectBusinessException.CustomFieldValidationException(
                        "Field '" + field.getFieldLabel() + "' must not exceed " + max
                    );
                }
            }
        }
    }
}