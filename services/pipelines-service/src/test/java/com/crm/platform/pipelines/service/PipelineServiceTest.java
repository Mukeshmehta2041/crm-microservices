package com.crm.platform.pipelines.service;

import com.crm.platform.pipelines.dto.PipelineRequest;
import com.crm.platform.pipelines.dto.PipelineResponse;
import com.crm.platform.pipelines.dto.PipelineStageRequest;
import com.crm.platform.pipelines.entity.Pipeline;
import com.crm.platform.pipelines.exception.PipelineBusinessException;
import com.crm.platform.pipelines.repository.PipelineRepository;
import com.crm.platform.pipelines.repository.PipelineStageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineServiceTest {

    @Mock
    private PipelineRepository pipelineRepository;

    @Mock
    private PipelineStageRepository stageRepository;

    @Mock
    private PipelineValidationService validationService;

    @Mock
    private PipelineEventService eventService;

    @Mock
    private PipelineMapper mapper;

    private PipelineService pipelineService;

    private UUID tenantId;
    private UUID userId;
    private UUID pipelineId;

    @BeforeEach
    void setUp() {
        pipelineService = new PipelineService(
                pipelineRepository,
                stageRepository,
                validationService,
                eventService,
                mapper
        );

        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        pipelineId = UUID.randomUUID();
    }

    @Test
    void createPipeline_Success() {
        // Arrange
        PipelineRequest request = createPipelineRequest();
        Pipeline pipeline = createPipeline();
        PipelineResponse expectedResponse = createPipelineResponse();

        when(mapper.toEntity(request, tenantId, userId)).thenReturn(pipeline);
        when(pipelineRepository.save(pipeline)).thenReturn(pipeline);
        when(mapper.toResponse(pipeline)).thenReturn(expectedResponse);

        // Act
        PipelineResponse result = pipelineService.createPipeline(tenantId, request, userId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse.getName(), result.getName());
        
        verify(validationService).validatePipelineRequest(tenantId, request);
        verify(pipelineRepository).save(pipeline);
        verify(eventService).publishPipelineCreatedEvent(pipeline);
    }

    @Test
    void createPipeline_WithStages_Success() {
        // Arrange
        PipelineRequest request = createPipelineRequestWithStages();
        Pipeline pipeline = createPipeline();
        PipelineResponse expectedResponse = createPipelineResponse();

        when(mapper.toEntity(request, tenantId, userId)).thenReturn(pipeline);
        when(pipelineRepository.save(pipeline)).thenReturn(pipeline);
        when(mapper.toResponse(pipeline)).thenReturn(expectedResponse);

        // Act
        PipelineResponse result = pipelineService.createPipeline(tenantId, request, userId);

        // Assert
        assertNotNull(result);
        verify(stageRepository, times(request.getStages().size())).save(any());
    }

    @Test
    void updatePipeline_Success() {
        // Arrange
        PipelineRequest request = createPipelineRequest();
        Pipeline existingPipeline = createPipeline();
        PipelineResponse expectedResponse = createPipelineResponse();

        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(existingPipeline));
        when(pipelineRepository.save(existingPipeline)).thenReturn(existingPipeline);
        when(mapper.toResponse(existingPipeline)).thenReturn(expectedResponse);

        // Act
        PipelineResponse result = pipelineService.updatePipeline(tenantId, pipelineId, request, userId);

        // Assert
        assertNotNull(result);
        verify(validationService).validatePipelineUpdateRequest(tenantId, pipelineId, request);
        verify(mapper).updateEntity(existingPipeline, request, userId);
        verify(eventService).publishPipelineUpdatedEvent(existingPipeline);
    }

    @Test
    void updatePipeline_NotFound_ThrowsException() {
        // Arrange
        PipelineRequest request = createPipelineRequest();
        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PipelineBusinessException.class, () -> 
                pipelineService.updatePipeline(tenantId, pipelineId, request, userId));
    }

    @Test
    void getPipeline_Success() {
        // Arrange
        Pipeline pipeline = createPipeline();
        PipelineResponse expectedResponse = createPipelineResponse();

        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(pipeline));
        when(mapper.toResponse(pipeline)).thenReturn(expectedResponse);

        // Act
        PipelineResponse result = pipelineService.getPipeline(tenantId, pipelineId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse.getName(), result.getName());
    }

    @Test
    void getAllPipelines_Success() {
        // Arrange
        List<Pipeline> pipelines = Arrays.asList(createPipeline(), createPipeline());
        List<PipelineResponse> expectedResponses = Arrays.asList(createPipelineResponse(), createPipelineResponse());

        when(pipelineRepository.findByTenantIdOrderByDisplayOrderAsc(tenantId)).thenReturn(pipelines);
        when(mapper.toResponse(any(Pipeline.class))).thenReturn(expectedResponses.get(0), expectedResponses.get(1));

        // Act
        List<PipelineResponse> result = pipelineService.getAllPipelines(tenantId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getActivePipelines_Success() {
        // Arrange
        List<Pipeline> activePipelines = Arrays.asList(createPipeline());
        List<PipelineResponse> expectedResponses = Arrays.asList(createPipelineResponse());

        when(pipelineRepository.findByTenantIdAndIsActiveTrueOrderByDisplayOrderAsc(tenantId)).thenReturn(activePipelines);
        when(mapper.toResponse(any(Pipeline.class))).thenReturn(expectedResponses.get(0));

        // Act
        List<PipelineResponse> result = pipelineService.getActivePipelines(tenantId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getDefaultPipeline_Success() {
        // Arrange
        Pipeline defaultPipeline = createPipeline();
        defaultPipeline.setIsDefault(true);
        PipelineResponse expectedResponse = createPipelineResponse();

        when(pipelineRepository.findByTenantIdAndIsDefaultTrue(tenantId)).thenReturn(Optional.of(defaultPipeline));
        when(mapper.toResponse(defaultPipeline)).thenReturn(expectedResponse);

        // Act
        Optional<PipelineResponse> result = pipelineService.getDefaultPipeline(tenantId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(expectedResponse.getName(), result.get().getName());
    }

    @Test
    void deletePipeline_Success() {
        // Arrange
        Pipeline pipeline = createPipeline();
        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(pipeline));

        // Act
        pipelineService.deletePipeline(tenantId, pipelineId, userId);

        // Assert
        verify(validationService).validatePipelineDeletion(pipeline);
        verify(eventService).publishPipelineDeletedEvent(pipeline);
        verify(pipelineRepository).delete(pipeline);
    }

    @Test
    void clonePipeline_Success() {
        // Arrange
        String newName = "Cloned Pipeline";
        Pipeline template = createPipeline();
        Pipeline clonedPipeline = createPipeline();
        clonedPipeline.setName(newName);
        PipelineResponse expectedResponse = createPipelineResponse();

        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(template));
        when(mapper.cloneFromTemplate(template, newName, userId)).thenReturn(clonedPipeline);
        when(pipelineRepository.save(clonedPipeline)).thenReturn(clonedPipeline);
        when(stageRepository.findByPipelineIdOrderByDisplayOrderAsc(pipelineId)).thenReturn(Arrays.asList());
        when(mapper.toResponse(clonedPipeline)).thenReturn(expectedResponse);

        // Act
        PipelineResponse result = pipelineService.clonePipeline(tenantId, pipelineId, newName, userId);

        // Assert
        assertNotNull(result);
        verify(validationService).validatePipelineClone(tenantId, template, newName);
        verify(eventService).publishPipelineClonedEvent(clonedPipeline, template);
    }

    // Helper methods

    private PipelineRequest createPipelineRequest() {
        PipelineRequest request = new PipelineRequest();
        request.setName("Test Pipeline");
        request.setDescription("Test Description");
        request.setIsActive(true);
        request.setIsDefault(false);
        request.setDisplayOrder(0);
        return request;
    }

    private PipelineRequest createPipelineRequestWithStages() {
        PipelineRequest request = createPipelineRequest();
        
        PipelineStageRequest stage1 = new PipelineStageRequest();
        stage1.setName("Stage 1");
        stage1.setDisplayOrder(0);
        stage1.setDefaultProbability(BigDecimal.valueOf(25));
        
        PipelineStageRequest stage2 = new PipelineStageRequest();
        stage2.setName("Stage 2");
        stage2.setDisplayOrder(1);
        stage2.setDefaultProbability(BigDecimal.valueOf(50));
        
        request.setStages(Arrays.asList(stage1, stage2));
        return request;
    }

    private Pipeline createPipeline() {
        Pipeline pipeline = new Pipeline();
        pipeline.setId(pipelineId);
        pipeline.setTenantId(tenantId);
        pipeline.setName("Test Pipeline");
        pipeline.setDescription("Test Description");
        pipeline.setIsActive(true);
        pipeline.setIsDefault(false);
        pipeline.setDisplayOrder(0);
        pipeline.setCreatedBy(userId);
        pipeline.setUpdatedBy(userId);
        return pipeline;
    }

    private PipelineResponse createPipelineResponse() {
        PipelineResponse response = new PipelineResponse();
        response.setId(pipelineId);
        response.setTenantId(tenantId);
        response.setName("Test Pipeline");
        response.setDescription("Test Description");
        response.setIsActive(true);
        response.setIsDefault(false);
        response.setDisplayOrder(0);
        return response;
    }
}