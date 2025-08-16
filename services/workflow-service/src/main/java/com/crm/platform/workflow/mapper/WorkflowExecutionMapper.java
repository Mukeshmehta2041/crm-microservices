package com.crm.platform.workflow.mapper;

import com.crm.platform.workflow.dto.WorkflowExecutionDto;
import com.crm.platform.workflow.entity.WorkflowExecution;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper for WorkflowExecution entity and DTO
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {WorkflowStepExecutionMapper.class})
public interface WorkflowExecutionMapper {

    /**
     * Convert entity to DTO
     */
    @Mapping(source = "workflowDefinition.id", target = "workflowDefinitionId")
    @Mapping(source = "workflowDefinition.name", target = "workflowName")
    @Mapping(target = "durationMs", expression = "java(calculateDuration(entity))")
    WorkflowExecutionDto toDto(WorkflowExecution entity);

    /**
     * Convert DTO to entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workflowDefinition", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "stepExecutions", ignore = true)
    WorkflowExecution toEntity(WorkflowExecutionDto dto);

    /**
     * Update entity from DTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "workflowDefinition", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "stepExecutions", ignore = true)
    void updateEntity(WorkflowExecutionDto dto, @MappingTarget WorkflowExecution entity);

    /**
     * Convert list of entities to DTOs
     */
    List<WorkflowExecutionDto> toDtoList(List<WorkflowExecution> entities);

    /**
     * Convert list of DTOs to entities
     */
    List<WorkflowExecution> toEntityList(List<WorkflowExecutionDto> dtos);

    /**
     * Calculate duration for mapping
     */
    default Long calculateDuration(WorkflowExecution entity) {
        if (entity.getStartedAt() != null && entity.getCompletedAt() != null) {
            return java.time.Duration.between(entity.getStartedAt(), entity.getCompletedAt()).toMillis();
        } else if (entity.getStartedAt() != null) {
            return java.time.Duration.between(entity.getStartedAt(), java.time.LocalDateTime.now()).toMillis();
        }
        return null;
    }
}