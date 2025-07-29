package com.crm.platform.workflow.service;

import com.crm.platform.workflow.dto.WorkflowDefinitionDto;
import com.crm.platform.workflow.entity.WorkflowDefinition;
import com.crm.platform.workflow.exception.WorkflowNotFoundException;
import com.crm.platform.workflow.exception.WorkflowValidationException;
import com.crm.platform.workflow.mapper.WorkflowDefinitionMapper;
import com.crm.platform.workflow.repository.WorkflowDefinitionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowDefinitionServiceTest {

    @Mock
    private WorkflowDefinitionRepository workflowDefinitionRepository;

    @Mock
    private WorkflowDefinitionMapper workflowDefinitionMapper;

    @Mock
    private WorkflowValidationService workflowValidationService;

    @InjectMocks
    private WorkflowDefinitionService workflowDefinitionService;

    private ObjectMapper objectMapper;
    private UUID tenantId;
    private UUID userId;
    private WorkflowDefinition workflowDefinition;
    private WorkflowDefinitionDto workflowDefinitionDto;
    private JsonNode workflowJson;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();

        // Create sample workflow JSON
        workflowJson = objectMapper.readTree("""
            {
                "name": "Test Workflow",
                "version": "1.0",
                "steps": [
                    {
                        "id": "start",
                        "name": "Start",
                        "type": "start_event",
                        "isStart": true
                    },
                    {
                        "id": "task1",
                        "name": "Task 1",
                        "type": "service_task",
                        "serviceClass": "com.example.TestService"
                    },
                    {
                        "id": "end",
                        "name": "End",
                        "type": "end_event",
                        "isEnd": true
                    }
                ],
                "connections": [
                    {"from": "start", "to": "task1"},
                    {"from": "task1", "to": "end"}
                ]
            }
            """);

        // Create test entities
        workflowDefinition = new WorkflowDefinition();
        workflowDefinition.setId(UUID.randomUUID());
        workflowDefinition.setTenantId(tenantId);
        workflowDefinition.setName("Test Workflow");
        workflowDefinition.setDescription("Test Description");
        workflowDefinition.setVersion(1);
        workflowDefinition.setIsActive(true);
        workflowDefinition.setIsPublished(false);
        workflowDefinition.setWorkflowJson(workflowJson);
        workflowDefinition.setCreatedBy(userId);
        workflowDefinition.setUpdatedBy(userId);

        workflowDefinitionDto = new WorkflowDefinitionDto();
        workflowDefinitionDto.setId(workflowDefinition.getId());
        workflowDefinitionDto.setTenantId(tenantId);
        workflowDefinitionDto.setName("Test Workflow");
        workflowDefinitionDto.setDescription("Test Description");
        workflowDefinitionDto.setVersion(1);
        workflowDefinitionDto.setIsActive(true);
        workflowDefinitionDto.setIsPublished(false);
        workflowDefinitionDto.setWorkflowJson(workflowJson);
        workflowDefinitionDto.setCreatedBy(userId);
        workflowDefinitionDto.setUpdatedBy(userId);
    }

    @Test
    void createWorkflow_Success() {
        // Arrange
        when(workflowDefinitionRepository.existsByTenantIdAndName(tenantId, "Test Workflow"))
                .thenReturn(false);
        when(workflowDefinitionMapper.toEntity(workflowDefinitionDto))
                .thenReturn(workflowDefinition);
        when(workflowDefinitionRepository.save(workflowDefinition))
                .thenReturn(workflowDefinition);
        when(workflowDefinitionMapper.toDto(workflowDefinition))
                .thenReturn(workflowDefinitionDto);

        // Act
        WorkflowDefinitionDto result = workflowDefinitionService.createWorkflow(workflowDefinitionDto);

        // Assert
        assertNotNull(result);
        assertEquals("Test Workflow", result.getName());
        verify(workflowValidationService).validateWorkflowDefinition(workflowJson);
        verify(workflowDefinitionRepository).save(workflowDefinition);
    }

    @Test
    void createWorkflow_ValidationFailure() {
        // Arrange
        doThrow(new WorkflowValidationException("Invalid workflow"))
                .when(workflowValidationService).validateWorkflowDefinition(workflowJson);

        // Act & Assert
        assertThrows(WorkflowValidationException.class, () -> {
            workflowDefinitionService.createWorkflow(workflowDefinitionDto);
        });

        verify(workflowDefinitionRepository, never()).save(any());
    }

    @Test
    void createWorkflow_DuplicateName_CreatesNewVersion() {
        // Arrange
        WorkflowDefinition existingWorkflow = new WorkflowDefinition();
        existingWorkflow.setVersion(2);
        
        when(workflowDefinitionRepository.existsByTenantIdAndName(tenantId, "Test Workflow"))
                .thenReturn(true);
        when(workflowDefinitionRepository.findByTenantIdAndNameOrderByVersionDesc(tenantId, "Test Workflow"))
                .thenReturn(Arrays.asList(existingWorkflow));
        when(workflowDefinitionMapper.toEntity(workflowDefinitionDto))
                .thenReturn(workflowDefinition);
        when(workflowDefinitionRepository.save(workflowDefinition))
                .thenReturn(workflowDefinition);
        when(workflowDefinitionMapper.toDto(workflowDefinition))
                .thenReturn(workflowDefinitionDto);

        // Act
        WorkflowDefinitionDto result = workflowDefinitionService.createWorkflow(workflowDefinitionDto);

        // Assert
        assertNotNull(result);
        assertEquals(3, workflowDefinitionDto.getVersion()); // Should be incremented
        verify(workflowDefinitionRepository).save(workflowDefinition);
    }

    @Test
    void getWorkflow_Success() {
        // Arrange
        when(workflowDefinitionRepository.findById(workflowDefinition.getId()))
                .thenReturn(Optional.of(workflowDefinition));
        when(workflowDefinitionMapper.toDto(workflowDefinition))
                .thenReturn(workflowDefinitionDto);

        // Act
        WorkflowDefinitionDto result = workflowDefinitionService.getWorkflow(tenantId, workflowDefinition.getId());

        // Assert
        assertNotNull(result);
        assertEquals("Test Workflow", result.getName());
        assertEquals(tenantId, result.getTenantId());
    }

    @Test
    void getWorkflow_NotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(workflowDefinitionRepository.findById(nonExistentId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(WorkflowNotFoundException.class, () -> {
            workflowDefinitionService.getWorkflow(tenantId, nonExistentId);
        });
    }

    @Test
    void getWorkflow_WrongTenant() {
        // Arrange
        UUID wrongTenantId = UUID.randomUUID();
        when(workflowDefinitionRepository.findById(workflowDefinition.getId()))
                .thenReturn(Optional.of(workflowDefinition));

        // Act & Assert
        assertThrows(WorkflowNotFoundException.class, () -> {
            workflowDefinitionService.getWorkflow(wrongTenantId, workflowDefinition.getId());
        });
    }

    @Test
    void getWorkflows_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<WorkflowDefinition> workflows = Arrays.asList(workflowDefinition);
        Page<WorkflowDefinition> workflowPage = new PageImpl<>(workflows, pageable, 1);
        
        when(workflowDefinitionRepository.findByTenantId(tenantId, pageable))
                .thenReturn(workflowPage);
        when(workflowDefinitionMapper.toDto(workflowDefinition))
                .thenReturn(workflowDefinitionDto);

        // Act
        Page<WorkflowDefinitionDto> result = workflowDefinitionService.getWorkflows(tenantId, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Workflow", result.getContent().get(0).getName());
    }

    @Test
    void updateWorkflow_Success() {
        // Arrange
        WorkflowDefinitionDto updateDto = new WorkflowDefinitionDto();
        updateDto.setTenantId(tenantId);
        updateDto.setName("Updated Workflow");
        updateDto.setDescription("Updated Description");
        updateDto.setWorkflowJson(workflowJson);
        updateDto.setUpdatedBy(userId);

        when(workflowDefinitionRepository.findById(workflowDefinition.getId()))
                .thenReturn(Optional.of(workflowDefinition));
        when(workflowDefinitionRepository.save(workflowDefinition))
                .thenReturn(workflowDefinition);
        when(workflowDefinitionMapper.toDto(workflowDefinition))
                .thenReturn(updateDto);

        // Act
        WorkflowDefinitionDto result = workflowDefinitionService.updateWorkflow(workflowDefinition.getId(), updateDto);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Workflow", result.getName());
        verify(workflowValidationService).validateWorkflowDefinition(workflowJson);
        verify(workflowDefinitionMapper).updateEntity(updateDto, workflowDefinition);
        verify(workflowDefinitionRepository).save(workflowDefinition);
    }

    @Test
    void publishWorkflow_Success() {
        // Arrange
        when(workflowDefinitionRepository.findById(workflowDefinition.getId()))
                .thenReturn(Optional.of(workflowDefinition));
        when(workflowDefinitionRepository.save(workflowDefinition))
                .thenReturn(workflowDefinition);
        when(workflowDefinitionMapper.toDto(workflowDefinition))
                .thenReturn(workflowDefinitionDto);

        // Act
        WorkflowDefinitionDto result = workflowDefinitionService.publishWorkflow(tenantId, workflowDefinition.getId());

        // Assert
        assertNotNull(result);
        assertTrue(workflowDefinition.getIsPublished());
        assertTrue(workflowDefinition.getIsActive());
        verify(workflowValidationService).validateWorkflowDefinition(workflowDefinition.getWorkflowJson());
        verify(workflowDefinitionRepository).save(workflowDefinition);
    }

    @Test
    void deleteWorkflow_Success() {
        // Arrange
        when(workflowDefinitionRepository.findById(workflowDefinition.getId()))
                .thenReturn(Optional.of(workflowDefinition));

        // Act
        workflowDefinitionService.deleteWorkflow(tenantId, workflowDefinition.getId());

        // Assert
        verify(workflowDefinitionRepository).delete(workflowDefinition);
    }

    @Test
    void cloneWorkflow_Success() {
        // Arrange
        String newName = "Cloned Workflow";
        when(workflowDefinitionRepository.findById(workflowDefinition.getId()))
                .thenReturn(Optional.of(workflowDefinition));
        when(workflowDefinitionRepository.save(any(WorkflowDefinition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(workflowDefinitionMapper.toDto(any(WorkflowDefinition.class)))
                .thenReturn(workflowDefinitionDto);

        // Act
        WorkflowDefinitionDto result = workflowDefinitionService.cloneWorkflow(tenantId, workflowDefinition.getId(), newName);

        // Assert
        assertNotNull(result);
        verify(workflowDefinitionRepository).save(any(WorkflowDefinition.class));
    }

    @Test
    void getWorkflowStatistics_Success() {
        // Arrange
        when(workflowDefinitionRepository.countByTenantId(tenantId)).thenReturn(10L);
        when(workflowDefinitionRepository.countByTenantIdAndIsActiveTrue(tenantId)).thenReturn(8L);
        when(workflowDefinitionRepository.countByTenantIdAndIsPublishedTrue(tenantId)).thenReturn(5L);

        // Act
        WorkflowDefinitionService.WorkflowStatistics result = workflowDefinitionService.getWorkflowStatistics(tenantId);

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.getTotalWorkflows());
        assertEquals(8L, result.getActiveWorkflows());
        assertEquals(5L, result.getPublishedWorkflows());
    }

    @Test
    void searchWorkflows_Success() {
        // Arrange
        String searchTerm = "test";
        Pageable pageable = PageRequest.of(0, 10);
        List<WorkflowDefinition> workflows = Arrays.asList(workflowDefinition);
        Page<WorkflowDefinition> workflowPage = new PageImpl<>(workflows, pageable, 1);
        
        when(workflowDefinitionRepository.findByTenantIdAndNameContainingIgnoreCase(tenantId, searchTerm, pageable))
                .thenReturn(workflowPage);
        when(workflowDefinitionMapper.toDto(workflowDefinition))
                .thenReturn(workflowDefinitionDto);

        // Act
        Page<WorkflowDefinitionDto> result = workflowDefinitionService.searchWorkflows(tenantId, searchTerm, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Workflow", result.getContent().get(0).getName());
    }

    @Test
    void getLatestWorkflowVersion_Success() {
        // Arrange
        String workflowName = "Test Workflow";
        when(workflowDefinitionRepository.findLatestVersionByTenantIdAndName(eq(tenantId), eq(workflowName), any(Pageable.class)))
                .thenReturn(Arrays.asList(workflowDefinition));
        when(workflowDefinitionMapper.toDto(workflowDefinition))
                .thenReturn(workflowDefinitionDto);

        // Act
        Optional<WorkflowDefinitionDto> result = workflowDefinitionService.getLatestWorkflowVersion(tenantId, workflowName);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Test Workflow", result.get().getName());
    }

    @Test
    void getLatestWorkflowVersion_NotFound() {
        // Arrange
        String workflowName = "Non-existent Workflow";
        when(workflowDefinitionRepository.findLatestVersionByTenantIdAndName(eq(tenantId), eq(workflowName), any(Pageable.class)))
                .thenReturn(Arrays.asList());

        // Act
        Optional<WorkflowDefinitionDto> result = workflowDefinitionService.getLatestWorkflowVersion(tenantId, workflowName);

        // Assert
        assertFalse(result.isPresent());
    }
}