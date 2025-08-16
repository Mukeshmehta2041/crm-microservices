package com.crm.platform.workflow.mapper;

import com.crm.platform.workflow.dto.BusinessRuleDto;
import com.crm.platform.workflow.entity.BusinessRule;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper for BusinessRule entity and DTO
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BusinessRuleMapper {

    /**
     * Convert entity to DTO
     */
    BusinessRuleDto toDto(BusinessRule entity);

    /**
     * Convert DTO to entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "executions", ignore = true)
    BusinessRule toEntity(BusinessRuleDto dto);

    /**
     * Update entity from DTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "executions", ignore = true)
    void updateEntity(BusinessRuleDto dto, @MappingTarget BusinessRule entity);

    /**
     * Convert list of entities to DTOs
     */
    List<BusinessRuleDto> toDtoList(List<BusinessRule> entities);

    /**
     * Convert list of DTOs to entities
     */
    List<BusinessRule> toEntityList(List<BusinessRuleDto> dtos);
}