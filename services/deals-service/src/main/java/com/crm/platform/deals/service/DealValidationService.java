package com.crm.platform.deals.service;

import com.crm.platform.common.exception.CrmBusinessException;
import com.crm.platform.deals.dto.DealRequest;
import com.crm.platform.deals.entity.Pipeline;
import com.crm.platform.deals.entity.PipelineStage;
import com.crm.platform.deals.repository.PipelineRepository;
import com.crm.platform.deals.repository.PipelineStageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DealValidationService {

    private final PipelineRepository pipelineRepository;
    private final PipelineStageRepository pipelineStageRepository;

    @Autowired
    public DealValidationService(PipelineRepository pipelineRepository,
                               PipelineStageRepository pipelineStageRepository) {
        this.pipelineRepository = pipelineRepository;
        this.pipelineStageRepository = pipelineStageRepository;
    }

    public void validateDealRequest(DealRequest request, UUID tenantId) {
        // Validate pipeline exists and is active
        Pipeline pipeline = pipelineRepository.findByIdAndTenantId(request.getPipelineId(), tenantId)
            .orElseThrow(() -> new CrmBusinessException("PIPELINE_NOT_FOUND", "Pipeline not found"));

        if (!pipeline.getIsActive()) {
            throw new CrmBusinessException("PIPELINE_INACTIVE", "Pipeline is not active");
        }

        // Validate stage exists, is active, and belongs to the pipeline
        PipelineStage stage = pipelineStageRepository.findByIdAndTenantId(request.getStageId(), tenantId)
            .orElseThrow(() -> new CrmBusinessException("STAGE_NOT_FOUND", "Pipeline stage not found"));

        if (!stage.getIsActive()) {
            throw new CrmBusinessException("STAGE_INACTIVE", "Pipeline stage is not active");
        }

        if (!stage.getPipelineId().equals(request.getPipelineId())) {
            throw new CrmBusinessException("INVALID_STAGE", "Stage does not belong to the specified pipeline");
        }

        // Validate business rules
        validateBusinessRules(request);
    }

    private void validateBusinessRules(DealRequest request) {
        // Validate amount is positive if provided
        if (request.getAmount() != null && request.getAmount().signum() < 0) {
            throw new CrmBusinessException("INVALID_AMOUNT", "Deal amount cannot be negative");
        }

        // Validate probability is within valid range if provided
        if (request.getProbability() != null) {
            if (request.getProbability().signum() < 0 || request.getProbability().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
                throw new CrmBusinessException("INVALID_PROBABILITY", "Probability must be between 0 and 100");
            }
        }

        // Validate currency format if provided
        if (request.getCurrency() != null && !request.getCurrency().matches("^[A-Z]{3}$")) {
            throw new CrmBusinessException("INVALID_CURRENCY", "Currency must be a valid 3-letter code");
        }

        // Validate expected close date is not in the past
        if (request.getExpectedCloseDate() != null && request.getExpectedCloseDate().isBefore(java.time.LocalDate.now())) {
            throw new CrmBusinessException("INVALID_CLOSE_DATE", "Expected close date cannot be in the past");
        }

        // Validate deal type if provided
        if (request.getDealType() != null && !isValidDealType(request.getDealType())) {
            throw new CrmBusinessException("INVALID_DEAL_TYPE", "Invalid deal type");
        }
    }

    private boolean isValidDealType(String dealType) {
        return dealType.equals("new_business") || 
               dealType.equals("existing_business") || 
               dealType.equals("renewal");
    }
}