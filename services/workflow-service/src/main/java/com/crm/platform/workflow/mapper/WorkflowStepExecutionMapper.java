package com.crm.platform.workflow.mapper;

import com.crm.platform.workflow.dto.WorkflowStepExecutionDto;
import com.crm.platform.workflow.entity.WorkflowStepExecution;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper for WorkflowStepExecution entity and DTO
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WorkflowStepExecutionMapper {

    /**
     * Convert entity to DTO
     */
    @Mapping(source = "workflowExecution.id", target = "workflowExecutionId")
    @Mapping(target = "durationMs", expression = "java(calculateDuration(entity))")
    WorkflowStepExecutionDto toDto(WorkflowStepExecution entity);

    /**
     * Convert DTO to entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workflowExecution", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "durationMs", ignore = true)
    WorkflowStepExecution toEntity(WorkflowStepExecutionDto dto);

    /**
     * Update entity from DTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "workflowExecution", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "durationMs", ignore = true)
    void updateEntity(WorkflowStepExecutionDto dto, @MappingTarget WorkflowStepExecution entity);

    /**
     * Convert list of entities to DTOs
     */
    List<WorkflowStepExecutionDto> toDtoList(List<WorkflowStepExecution> entities);

    /**
     * Convert list of DTOs to entities
     */
    List<WorkflowStepExecution> toEntityList(List<WorkflowStepExecutionDto> dtos);

    /**
     * Calculate duration for mapping
     */
    default Long calculateDuration(WorkflowStepExecution entity) {
        if (entity.getStartedAt() != null && entity.getCompletedAt() != null) {
            return java.time.Duration.between(entity.getStartedAt(), entity.getCompletedAt()).toMillis();
        }
        return entity.getDurationMs();
    }
}