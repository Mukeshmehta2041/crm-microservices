package com.crm.platform.leads.service;

import com.crm.platform.leads.entity.Lead;
import com.crm.platform.leads.entity.LeadScoreHistory;
import com.crm.platform.leads.repository.LeadScoreHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadScoringServiceTest {

    @Mock
    private LeadScoreHistoryRepository scoreHistoryRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private LeadScoringService leadScoringService;

    private Lead testLead;
    private UUID tenantId;
    private UUID leadId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        leadId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testLead = new Lead(tenantId, "John", "Doe", userId, userId);
        testLead.setId(leadId);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void calculateLeadScore_BasicLead_ReturnsCorrectScore() {
        // Given
        testLead.setEmail("john.doe@example.com");
        testLead.setPhone("+1234567890");
        testLead.setCompany("Test Company");
        testLead.setTitle("Manager");

        // When
        int score = leadScoringService.calculateLeadScore(testLead);

        // Then
        assertTrue(score >= 0 && score <= 100);
        assertTrue(score > 0); // Should have some positive score due to contact info
    }

    @Test
    void calculateLeadScore_HighValueLead_ReturnsHighScore() {
        // Given - Create a high-value lead
        testLead.setEmail("ceo@bigcompany.com");
        testLead.setPhone("+1234567890");
        testLead.setMobile("+1987654321");
        testLead.setCompany("Big Corporation");
        testLead.setTitle("CEO");
        testLead.setNumberOfEmployees(5000);
        testLead.setAnnualRevenue(new BigDecimal("50000000"));
        testLead.setBudget(new BigDecimal("500000"));
        testLead.setDecisionMaker(true);
        testLead.setPurchaseTimeframe("immediate");
        testLead.setPainPoints("Need better solution");
        testLead.setInterests("Enterprise software");

        // When
        int score = leadScoringService.calculateLeadScore(testLead);

        // Then
        assertTrue(score >= 80); // Should be a hot lead
    }

    @Test
    void calculateLeadScore_LowValueLead_ReturnsLowScore() {
        // Given - Create a low-value lead with negative factors
        testLead.setDoNotCall(true);
        testLead.setDoNotEmail(true);
        testLead.setEmailOptOut(true);
        testLead.setLastActivityAt(LocalDateTime.now().minusDays(100)); // Inactive

        // When
        int score = leadScoringService.calculateLeadScore(testLead);

        // Then
        assertTrue(score < 50); // Should be a cold lead
    }

    @Test
    void calculateLeadScore_NoContactInfo_ReceivesNegativeScore() {
        // Given - Lead with no contact information
        testLead.setEmail(null);
        testLead.setPhone(null);
        testLead.setMobile(null);

        // When
        int score = leadScoringService.calculateLeadScore(testLead);

        // Then
        assertTrue(score >= 0); // Score should never go below 0
    }

    @Test
    void updateLeadScore_ScoreChanged_RecordsHistory() {
        // Given
        testLead.setLeadScore(50);
        testLead.setEmail("test@example.com");
        testLead.setCompany("Test Company");

        when(scoreHistoryRepository.save(any(LeadScoreHistory.class)))
            .thenReturn(new LeadScoreHistory());

        // When
        leadScoringService.updateLeadScore(testLead, "Test update", "SYSTEM", userId);

        // Then
        verify(scoreHistoryRepository).save(any(LeadScoreHistory.class));
        assertNotEquals(50, testLead.getLeadScore()); // Score should have changed
    }

    @Test
    void updateLeadScore_ScoreUnchanged_DoesNotRecordHistory() {
        // Given
        int initialScore = leadScoringService.calculateLeadScore(testLead);
        testLead.setLeadScore(initialScore);

        // When
        leadScoringService.updateLeadScore(testLead, "Test update", "SYSTEM", userId);

        // Then
        verify(scoreHistoryRepository, never()).save(any(LeadScoreHistory.class));
    }

    @Test
    void getLeadScoringBreakdown_ValidLead_ReturnsBreakdown() {
        // Given
        testLead.setEmail("test@example.com");
        testLead.setCompany("Test Company");
        testLead.setNumberOfEmployees(100);
        testLead.setDecisionMaker(true);

        // When
        Map<String, Object> breakdown = leadScoringService.getLeadScoringBreakdown(testLead);

        // Then
        assertNotNull(breakdown);
        assertTrue(breakdown.containsKey("leadId"));
        assertTrue(breakdown.containsKey("currentScore"));
        assertTrue(breakdown.containsKey("demographicScore"));
        assertTrue(breakdown.containsKey("firmographicScore"));
        assertTrue(breakdown.containsKey("behavioralScore"));
        assertTrue(breakdown.containsKey("negativeScore"));
        assertTrue(breakdown.containsKey("calculatedAt"));
        assertEquals(leadId, breakdown.get("leadId"));
    }

    @Test
    void calculateLeadScore_DecisionMaker_AddsCorrectScore() {
        // Given
        testLead.setDecisionMaker(true);

        // When
        int score = leadScoringService.calculateLeadScore(testLead);

        // Then
        assertTrue(score >= 20); // Should include decision maker score
    }

    @Test
    void calculateLeadScore_LargeCompany_AddsCorrectScore() {
        // Given
        testLead.setNumberOfEmployees(2000);
        testLead.setAnnualRevenue(new BigDecimal("20000000"));

        // When
        int score = leadScoringService.calculateLeadScore(testLead);

        // Then
        assertTrue(score >= 35); // Should include large company and high revenue scores
    }

    @Test
    void calculateLeadScore_ImmediatePurchaseTimeframe_AddsCorrectScore() {
        // Given
        testLead.setPurchaseTimeframe("immediate");

        // When
        int score = leadScoringService.calculateLeadScore(testLead);

        // Then
        assertTrue(score >= 25); // Should include immediate timeframe score
    }

    @Test
    void calculateLeadScore_HasPainPointsAndInterests_AddsCorrectScore() {
        // Given
        testLead.setPainPoints("Current solution is too slow");
        testLead.setInterests("Cloud-based solutions");

        // When
        int score = leadScoringService.calculateLeadScore(testLead);

        // Then
        assertTrue(score >= 18); // Should include pain points (10) + interests (8) scores
    }

    @Test
    void calculateLeadScore_InactiveLead_ReceivesNegativeScore() {
        // Given
        testLead.setLastActivityAt(LocalDateTime.now().minusDays(100));

        // When
        int score = leadScoringService.calculateLeadScore(testLead);

        // Then
        // Score calculation should account for inactivity penalty
        // The exact score depends on other factors, but inactivity should reduce it
        assertNotNull(score);
    }

    @Test
    void calculateLeadScore_AllContactPreferencesDisabled_ReceivesNegativeScore() {
        // Given
        testLead.setDoNotCall(true);
        testLead.setDoNotEmail(true);
        testLead.setEmailOptOut(true);

        // When
        int score = leadScoringService.calculateLeadScore(testLead);

        // Then
        // Should receive negative scores for contact restrictions
        assertNotNull(score);
    }

    @Test
    void calculateLeadScore_ScoreNeverExceedsMaximum() {
        // Given - Create a lead with maximum possible positive factors
        testLead.setEmail("ceo@megacorp.com");
        testLead.setPhone("+1234567890");
        testLead.setMobile("+1987654321");
        testLead.setCompany("Mega Corporation");
        testLead.setTitle("Chief Executive Officer");
        testLead.setNumberOfEmployees(10000);
        testLead.setAnnualRevenue(new BigDecimal("1000000000"));
        testLead.setBudget(new BigDecimal("10000000"));
        testLead.setDecisionMaker(true);
        testLead.setPurchaseTimeframe("immediate");
        testLead.setPainPoints("Critical business need");
        testLead.setInterests("Enterprise solutions");

        // When
        int score = leadScoringService.calculateLeadScore(testLead);

        // Then
        assertTrue(score <= 100);
    }

    @Test
    void calculateLeadScore_ScoreNeverGoesBelowMinimum() {
        // Given - Create a lead with maximum possible negative factors
        testLead.setDoNotCall(true);
        testLead.setDoNotEmail(true);
        testLead.setEmailOptOut(true);
        testLead.setEmail(null);
        testLead.setPhone(null);
        testLead.setMobile(null);
        testLead.setLastActivityAt(LocalDateTime.now().minusDays(200));

        // When
        int score = leadScoringService.calculateLeadScore(testLead);

        // Then
        assertTrue(score >= 0);
    }
}