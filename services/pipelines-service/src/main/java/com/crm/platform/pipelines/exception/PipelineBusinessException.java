package com.crm.platform.pipelines.exception;

public class PipelineBusinessException extends RuntimeException {

    private final String errorCode;

    public PipelineBusinessException(String message) {
        super(message);
        this.errorCode = "PIPELINE_BUSINESS_ERROR";
    }

    public PipelineBusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public PipelineBusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "PIPELINE_BUSINESS_ERROR";
    }

    public PipelineBusinessException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}