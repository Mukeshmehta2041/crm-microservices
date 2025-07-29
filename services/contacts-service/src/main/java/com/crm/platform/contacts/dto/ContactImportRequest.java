package com.crm.platform.contacts.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for contact import operations
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactImportRequest {
    
    @JsonProperty("fileUrl")
    @NotBlank(message = "File URL is required")
    private String fileUrl;
    
    @JsonProperty("fileName")
    private String fileName;
    
    @JsonProperty("format")
    @NotBlank(message = "Format is required")
    private String format;
    
    @JsonProperty("fieldMapping")
    private Map<String, String> fieldMapping;
    
    @JsonProperty("skipDuplicates")
    private Boolean skipDuplicates = false;
    
    @JsonProperty("updateExisting")
    private Boolean updateExisting = false;
    
    @JsonProperty("duplicateMatchFields")
    private List<String> duplicateMatchFields;
    
    @JsonProperty("validateOnly")
    private Boolean validateOnly = false;
    
    @JsonProperty("batchSize")
    private Integer batchSize = 100;
    
    @JsonProperty("skipHeaderRow")
    private Boolean skipHeaderRow = true;
    
    @JsonProperty("delimiter")
    private String delimiter = ",";
    
    @JsonProperty("encoding")
    private String encoding = "UTF-8";
    
    @JsonProperty("defaultValues")
    private Map<String, Object> defaultValues;
    
    // Constructors
    public ContactImportRequest() {}
    
    public ContactImportRequest(String fileUrl, String format) {
        this.fileUrl = fileUrl;
        this.format = format;
    }
    
    // Getters and setters
    public String getFileUrl() {
        return fileUrl;
    }
    
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFormat() {
        return format;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
    
    public Map<String, String> getFieldMapping() {
        return fieldMapping;
    }
    
    public void setFieldMapping(Map<String, String> fieldMapping) {
        this.fieldMapping = fieldMapping;
    }
    
    public Boolean getSkipDuplicates() {
        return skipDuplicates;
    }
    
    public void setSkipDuplicates(Boolean skipDuplicates) {
        this.skipDuplicates = skipDuplicates;
    }
    
    public Boolean getUpdateExisting() {
        return updateExisting;
    }
    
    public void setUpdateExisting(Boolean updateExisting) {
        this.updateExisting = updateExisting;
    }
    
    public List<String> getDuplicateMatchFields() {
        return duplicateMatchFields;
    }
    
    public void setDuplicateMatchFields(List<String> duplicateMatchFields) {
        this.duplicateMatchFields = duplicateMatchFields;
    }
    
    public Boolean getValidateOnly() {
        return validateOnly;
    }
    
    public void setValidateOnly(Boolean validateOnly) {
        this.validateOnly = validateOnly;
    }
    
    public Integer getBatchSize() {
        return batchSize;
    }
    
    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }
    
    public Boolean getSkipHeaderRow() {
        return skipHeaderRow;
    }
    
    public void setSkipHeaderRow(Boolean skipHeaderRow) {
        this.skipHeaderRow = skipHeaderRow;
    }
    
    public String getDelimiter() {
        return delimiter;
    }
    
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }
    
    public String getEncoding() {
        return encoding;
    }
    
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
    
    public Map<String, Object> getDefaultValues() {
        return defaultValues;
    }
    
    public void setDefaultValues(Map<String, Object> defaultValues) {
        this.defaultValues = defaultValues;
    }
}