package com.crm.platform.workflow.mapper;

import com.crm.platform.workflow.dto.WorkflowDefinitionDto;
import com.crm.platform.workflow.entity.WorkflowDefinition;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper for WorkflowDefinition entity and DTO
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WorkflowDefinitionMapper {

    /**
     * Convert entity to DTO
     */
    WorkflowDefinitionDto toDto(WorkflowDefinition entity);

    /**
     * Convert DTO to entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "executions", ignore = true)
    @Mapping(target = "triggers", ignore = true)
    WorkflowDefinition toEntity(WorkflowDefinitionDto dto);

    /**
     * Update entity from DTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "executions", ignore = true)
    @Mapping(target = "triggers", ignore = true)
    void updateEntity(WorkflowDefinitionDto dto, @MappingTarget WorkflowDefinition entity);

    /**
     * Convert list of entities to DTOs
     */
    List<WorkflowDefinitionDto> toDtoList(List<WorkflowDefinition> entities);

    /**
     * Convert list of DTOs to entities
     */
    List<WorkflowDefinition> toEntityList(List<WorkflowDefinitionDto> dtos);
}