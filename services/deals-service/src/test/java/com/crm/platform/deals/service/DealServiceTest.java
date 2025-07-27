package com.crm.platform.deals.service;

import com.crm.platform.common.exception.CrmBusinessException;
import com.crm.platform.deals.dto.DealRequest;
import com.crm.platform.deals.dto.DealResponse;
import com.crm.platform.deals.entity.Deal;
import com.crm.platform.deals.entity.Pipeline;
import com.crm.platform.deals.entity.PipelineStage;
import com.crm.platform.deals.repository.DealRepository;
import com.crm.platform.deals.repository.PipelineRepository;
import com.crm.platform.deals.repository.PipelineStageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DealServiceTest {

    @Mock
    private DealRepository dealRepository;

    @Mock
    private PipelineRepository pipelineRepository;

    @Mock
    private PipelineStageRepository pipelineStageRepository;

    @Mock
    private DealEventService dealEventService;

    @Mock
    private DealValidationService dealValidationService;

    @InjectMocks
    private DealService dealService;

    private UUID tenantId;
    private UUID userId;
    private UUID pipelineId;
    private UUID stageId;
    private DealRequest dealRequest;
    private Deal deal;
    private Pipeline pipeline;
    private PipelineStage stage;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        pipelineId = UUID.randomUUID();
        stageId = UUID.randomUUID();

        // Set up test data
        dealRequest = new DealRequest();
        dealRequest.setName("Test Deal");
        dealRequest.setPipelineId(pipelineId);
        dealRequest.setStageId(stageId);
        dealRequest.setAmount(BigDecimal.valueOf(10000));
        dealRequest.setCurrency("USD");
        dealRequest.setProbability(BigDecimal.valueOf(50));
        dealRequest.setExpectedCloseDate(LocalDate.now().plusDays(30));
        dealRequest.setOwnerId(userId);

        deal = new Deal();
        deal.setId(UUID.randomUUID());
        deal.setTenantId(tenantId);
        deal.setName(dealRequest.getName());
        deal.setPipelineId(pipelineId);
        deal.setStageId(stageId);
        deal.setAmount(dealRequest.getAmount());
        deal.setCurrency(dealRequest.getCurrency());
        deal.setProbability(dealRequest.getProbability());
        deal.setExpectedCloseDate(dealRequest.getExpectedCloseDate());
        deal.setOwnerId(userId);
        deal.setCreatedBy(userId);
        deal.setUpdatedBy(userId);

        pipeline = new Pipeline();
        pipeline.setId(pipelineId);
        pipeline.setTenantId(tenantId);
        pipeline.setName("Test Pipeline");
        pipeline.setIsActive(true);

        stage = new PipelineStage();
        stage.setId(stageId);
        stage.setPipelineId(pipelineId);
        stage.setName("Test Stage");
        stage.setIsActive(true);
        stage.setDefaultProbability(BigDecimal.valueOf(50));
    }

    @Test
    void createDeal_Success() {
        // Arrange
        when(dealRepository.save(any(Deal.class))).thenReturn(deal);
        when(pipelineStageRepository.findById(stageId)).thenReturn(Optional.of(stage));
        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(pipeline));

        // Act
        DealResponse response = dealService.createDeal(dealRequest, userId);

        // Assert
        assertNotNull(response);
        assertEquals(deal.getName(), response.getName());
        assertEquals(deal.getAmount(), response.getAmount());
        assertEquals(deal.getProbability(), response.getProbability());

        verify(dealValidationService).validateDealRequest(dealRequest, tenantId);
        verify(dealRepository).save(any(Deal.class));
        verify(dealEventService).publishDealCreated(any(Deal.class));
    }

    @Test
    void createDeal_ValidationFailure() {
        // Arrange
        doThrow(new CrmBusinessException("VALIDATION_ERROR", "Validation failed"))
            .when(dealValidationService).validateDealRequest(dealRequest, tenantId);

        // Act & Assert
        assertThrows(CrmBusinessException.class, () -> {
            dealService.createDeal(dealRequest, userId);
        });

        verify(dealRepository, never()).save(any(Deal.class));
        verify(dealEventService, never()).publishDealCreated(any(Deal.class));
    }

    @Test
    void getDeal_Success() {
        // Arrange
        when(dealRepository.findByIdAndTenantId(deal.getId(), tenantId)).thenReturn(Optional.of(deal));
        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(pipeline));
        when(pipelineStageRepository.findById(stageId)).thenReturn(Optional.of(stage));

        // Act
        DealResponse response = dealService.getDeal(deal.getId());

        // Assert
        assertNotNull(response);
        assertEquals(deal.getId(), response.getId());
        assertEquals(deal.getName(), response.getName());
        assertEquals(pipeline.getName(), response.getPipelineName());
        assertEquals(stage.getName(), response.getStageName());
    }

    @Test
    void getDeal_NotFound() {
        // Arrange
        UUID dealId = UUID.randomUUID();
        when(dealRepository.findByIdAndTenantId(dealId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CrmBusinessException.class, () -> {
            dealService.getDeal(dealId);
        });
    }

    @Test
    void updateDeal_Success() {
        // Arrange
        when(dealRepository.findByIdAndTenantId(deal.getId(), tenantId)).thenReturn(Optional.of(deal));
        when(dealRepository.save(any(Deal.class))).thenReturn(deal);
        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(pipeline));
        when(pipelineStageRepository.findById(stageId)).thenReturn(Optional.of(stage));

        // Modify request
        dealRequest.setName("Updated Deal Name");
        dealRequest.setAmount(BigDecimal.valueOf(15000));

        // Act
        DealResponse response = dealService.updateDeal(deal.getId(), dealRequest, userId);

        // Assert
        assertNotNull(response);
        verify(dealValidationService).validateDealRequest(dealRequest, tenantId);
        verify(dealRepository).save(any(Deal.class));
        verify(dealEventService).publishDealUpdated(any(Deal.class));
    }

    @Test
    void deleteDeal_Success() {
        // Arrange
        when(dealRepository.findByIdAndTenantId(deal.getId(), tenantId)).thenReturn(Optional.of(deal));

        // Act
        dealService.deleteDeal(deal.getId(), userId);

        // Assert
        verify(dealRepository).delete(deal);
        verify(dealEventService).publishDealDeleted(deal);
    }

    @Test
    void calculateWeightedAmount() {
        // Arrange
        deal.setAmount(BigDecimal.valueOf(10000));
        deal.setProbability(BigDecimal.valueOf(50));

        // Act
        BigDecimal weightedAmount = deal.getWeightedAmount();

        // Assert
        assertEquals(BigDecimal.valueOf(5000), weightedAmount);
    }

    @Test
    void calculateWeightedAmount_NullValues() {
        // Arrange
        deal.setAmount(null);
        deal.setProbability(BigDecimal.valueOf(50));

        // Act
        BigDecimal weightedAmount = deal.getWeightedAmount();

        // Assert
        assertEquals(BigDecimal.ZERO, weightedAmount);
    }
}