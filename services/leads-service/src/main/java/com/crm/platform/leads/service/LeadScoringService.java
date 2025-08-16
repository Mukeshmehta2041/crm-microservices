package com.crm.platform.leads.service;

import com.crm.platform.leads.entity.Lead;
import com.crm.platform.leads.entity.LeadScoreHistory;
import com.crm.platform.leads.repository.LeadScoreHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class LeadScoringService {

    private static final Logger logger = LoggerFactory.getLogger(LeadScoringService.class);
    
    private final LeadScoreHistoryRepository scoreHistoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Scoring rule weights and thresholds
    private static final Map<String, Integer> DEMOGRAPHIC_SCORES = Map.of(
        "HAS_EMAIL", 5,
        "HAS_PHONE", 5,
        "HAS_MOBILE", 3,
        "HAS_TITLE", 8,
        "HAS_COMPANY", 10
    );
    
    private static final Map<String, Integer> FIRMOGRAPHIC_SCORES = Map.of(
        "LARGE_COMPANY", 15,      // >1000 employees
        "MEDIUM_COMPANY", 10,     // 100-1000 employees
        "SMALL_COMPANY", 5,       // 10-100 employees
        "HIGH_REVENUE", 20,       // >$10M annual revenue
        "MEDIUM_REVENUE", 15,     // $1M-$10M annual revenue
        "LOW_REVENUE", 5,         // <$1M annual revenue
        "HIGH_BUDGET", 25,        // >$100K budget
        "MEDIUM_BUDGET", 15,      // $10K-$100K budget
        "LOW_BUDGET", 5           // <$10K budget
    );
    
    private static final Map<String, Integer> BEHAVIORAL_SCORES = Map.of(
        "DECISION_MAKER", 20,
        "IMMEDIATE_TIMEFRAME", 25,    // Immediate purchase
        "SHORT_TIMEFRAME", 15,        // 1-3 months
        "MEDIUM_TIMEFRAME", 10,       // 3-6 months
        "LONG_TIMEFRAME", 5,          // 6+ months
        "HAS_PAIN_POINTS", 10,
        "HAS_INTERESTS", 8
    );
    
    private static final Map<String, Integer> NEGATIVE_SCORES = Map.of(
        "DO_NOT_CALL", -5,
        "DO_NOT_EMAIL", -5,
        "EMAIL_OPT_OUT", -10,
        "NO_CONTACT_INFO", -15,
        "INACTIVE_LONG", -20          // No activity for 90+ days
    );

    @Autowired
    public LeadScoringService(LeadScoreHistoryRepository scoreHistoryRepository,
                             RedisTemplate<String, Object> redisTemplate) {
        this.scoreHistoryRepository = scoreHistoryRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Calculate comprehensive lead score based on multiple factors
     */
    public int calculateLeadScore(Lead lead) {
        logger.debug("Calculating lead score for lead: {}", lead.getId());
        
        int totalScore = 0;
        List<String> appliedRules = new ArrayList<>();
        
        // Demographic scoring
        totalScore += calculateDemographicScore(lead, appliedRules);
        
        // Firmographic scoring
        totalScore += calculateFirmographicScore(lead, appliedRules);
        
        // Behavioral scoring
        totalScore += calculateBehavioralScore(lead, appliedRules);
        
        // Negative scoring
        totalScore += calculateNegativeScore(lead, appliedRules);
        
        // Ensure score is within valid range
        totalScore = Math.max(0, Math.min(100, totalScore));
        
        // Cache the score calculation
        cacheLeadScore(lead.getId(), totalScore, appliedRules);
        
        logger.debug("Calculated lead score: {} for lead: {} with rules: {}", 
                    totalScore, lead.getId(), appliedRules);
        
        return totalScore;
    }

    /**
     * Update lead score and record history
     */
    public void updateLeadScore(Lead lead, String reason, String triggeredBy, UUID updatedBy) {
        int previousScore = lead.getLeadScore();
        int newScore = calculateLeadScore(lead);
        
        if (previousScore != newScore) {
            lead.setLeadScore(newScore);
            
            // Record score change history
            LeadScoreHistory history = new LeadScoreHistory(
                lead.getTenantId(),
                lead.getId(),
                previousScore,
                newScore,
                reason,
                updatedBy
            );
            history.setTriggeredBy(triggeredBy);
            
            scoreHistoryRepository.save(history);
            
            logger.info("Updated lead score from {} to {} for lead: {} - Reason: {}", 
                       previousScore, newScore, lead.getId(), reason);
        }
    }

    /**
     * Get cached lead score if available
     */
    @Cacheable(value = "leadScores", key = "#leadId")
    public Integer getCachedLeadScore(UUID leadId) {
        String cacheKey = "lead:score:" + leadId;
        Object cachedScore = redisTemplate.opsForValue().get(cacheKey);
        return cachedScore != null ? (Integer) cachedScore : null;
    }

    /**
     * Get lead scoring breakdown for analysis
     */
    public Map<String, Object> getLeadScoringBreakdown(Lead lead) {
        Map<String, Object> breakdown = new HashMap<>();
        
        breakdown.put("leadId", lead.getId());
        breakdown.put("currentScore", lead.getLeadScore());
        breakdown.put("demographicScore", calculateDemographicScore(lead, new ArrayList<>()));
        breakdown.put("firmographicScore", calculateFirmographicScore(lead, new ArrayList<>()));
        breakdown.put("behavioralScore", calculateBehavioralScore(lead, new ArrayList<>()));
        breakdown.put("negativeScore", calculateNegativeScore(lead, new ArrayList<>()));
        breakdown.put("calculatedAt", LocalDateTime.now());
        
        return breakdown;
    }

    /**
     * Bulk recalculate scores for multiple leads
     */
    public void bulkRecalculateScores(List<Lead> leads, String reason, UUID updatedBy) {
        logger.info("Bulk recalculating scores for {} leads", leads.size());
        
        for (Lead lead : leads) {
            try {
                updateLeadScore(lead, reason, "BULK_RECALCULATION", updatedBy);
            } catch (Exception e) {
                logger.error("Error recalculating score for lead: {}", lead.getId(), e);
            }
        }
    }

    private int calculateDemographicScore(Lead lead, List<String> appliedRules) {
        int score = 0;
        
        if (lead.getEmail() != null && !lead.getEmail().trim().isEmpty()) {
            score += DEMOGRAPHIC_SCORES.get("HAS_EMAIL");
            appliedRules.add("HAS_EMAIL");
        }
        
        if (lead.getPhone() != null && !lead.getPhone().trim().isEmpty()) {
            score += DEMOGRAPHIC_SCORES.get("HAS_PHONE");
            appliedRules.add("HAS_PHONE");
        }
        
        if (lead.getMobile() != null && !lead.getMobile().trim().isEmpty()) {
            score += DEMOGRAPHIC_SCORES.get("HAS_MOBILE");
            appliedRules.add("HAS_MOBILE");
        }
        
        if (lead.getTitle() != null && !lead.getTitle().trim().isEmpty()) {
            score += DEMOGRAPHIC_SCORES.get("HAS_TITLE");
            appliedRules.add("HAS_TITLE");
        }
        
        if (lead.getCompany() != null && !lead.getCompany().trim().isEmpty()) {
            score += DEMOGRAPHIC_SCORES.get("HAS_COMPANY");
            appliedRules.add("HAS_COMPANY");
        }
        
        return score;
    }

    private int calculateFirmographicScore(Lead lead, List<String> appliedRules) {
        int score = 0;
        
        // Employee count scoring
        if (lead.getNumberOfEmployees() != null) {
            if (lead.getNumberOfEmployees() > 1000) {
                score += FIRMOGRAPHIC_SCORES.get("LARGE_COMPANY");
                appliedRules.add("LARGE_COMPANY");
            } else if (lead.getNumberOfEmployees() >= 100) {
                score += FIRMOGRAPHIC_SCORES.get("MEDIUM_COMPANY");
                appliedRules.add("MEDIUM_COMPANY");
            } else if (lead.getNumberOfEmployees() >= 10) {
                score += FIRMOGRAPHIC_SCORES.get("SMALL_COMPANY");
                appliedRules.add("SMALL_COMPANY");
            }
        }
        
        // Annual revenue scoring
        if (lead.getAnnualRevenue() != null) {
            BigDecimal revenue = lead.getAnnualRevenue();
            if (revenue.compareTo(new BigDecimal("10000000")) > 0) { // >$10M
                score += FIRMOGRAPHIC_SCORES.get("HIGH_REVENUE");
                appliedRules.add("HIGH_REVENUE");
            } else if (revenue.compareTo(new BigDecimal("1000000")) > 0) { // >$1M
                score += FIRMOGRAPHIC_SCORES.get("MEDIUM_REVENUE");
                appliedRules.add("MEDIUM_REVENUE");
            } else {
                score += FIRMOGRAPHIC_SCORES.get("LOW_REVENUE");
                appliedRules.add("LOW_REVENUE");
            }
        }
        
        // Budget scoring
        if (lead.getBudget() != null) {
            BigDecimal budget = lead.getBudget();
            if (budget.compareTo(new BigDecimal("100000")) > 0) { // >$100K
                score += FIRMOGRAPHIC_SCORES.get("HIGH_BUDGET");
                appliedRules.add("HIGH_BUDGET");
            } else if (budget.compareTo(new BigDecimal("10000")) > 0) { // >$10K
                score += FIRMOGRAPHIC_SCORES.get("MEDIUM_BUDGET");
                appliedRules.add("MEDIUM_BUDGET");
            } else {
                score += FIRMOGRAPHIC_SCORES.get("LOW_BUDGET");
                appliedRules.add("LOW_BUDGET");
            }
        }
        
        return score;
    }

    private int calculateBehavioralScore(Lead lead, List<String> appliedRules) {
        int score = 0;
        
        if (Boolean.TRUE.equals(lead.getDecisionMaker())) {
            score += BEHAVIORAL_SCORES.get("DECISION_MAKER");
            appliedRules.add("DECISION_MAKER");
        }
        
        // Purchase timeframe scoring
        if (lead.getPurchaseTimeframe() != null) {
            String timeframe = lead.getPurchaseTimeframe().toLowerCase();
            if (timeframe.contains("immediate") || timeframe.contains("now")) {
                score += BEHAVIORAL_SCORES.get("IMMEDIATE_TIMEFRAME");
                appliedRules.add("IMMEDIATE_TIMEFRAME");
            } else if (timeframe.contains("1-3") || timeframe.contains("month")) {
                score += BEHAVIORAL_SCORES.get("SHORT_TIMEFRAME");
                appliedRules.add("SHORT_TIMEFRAME");
            } else if (timeframe.contains("3-6")) {
                score += BEHAVIORAL_SCORES.get("MEDIUM_TIMEFRAME");
                appliedRules.add("MEDIUM_TIMEFRAME");
            } else if (timeframe.contains("6+") || timeframe.contains("year")) {
                score += BEHAVIORAL_SCORES.get("LONG_TIMEFRAME");
                appliedRules.add("LONG_TIMEFRAME");
            }
        }
        
        if (lead.getPainPoints() != null && !lead.getPainPoints().trim().isEmpty()) {
            score += BEHAVIORAL_SCORES.get("HAS_PAIN_POINTS");
            appliedRules.add("HAS_PAIN_POINTS");
        }
        
        if (lead.getInterests() != null && !lead.getInterests().trim().isEmpty()) {
            score += BEHAVIORAL_SCORES.get("HAS_INTERESTS");
            appliedRules.add("HAS_INTERESTS");
        }
        
        return score;
    }

    private int calculateNegativeScore(Lead lead, List<String> appliedRules) {
        int score = 0;
        
        if (Boolean.TRUE.equals(lead.getDoNotCall())) {
            score += NEGATIVE_SCORES.get("DO_NOT_CALL");
            appliedRules.add("DO_NOT_CALL");
        }
        
        if (Boolean.TRUE.equals(lead.getDoNotEmail())) {
            score += NEGATIVE_SCORES.get("DO_NOT_EMAIL");
            appliedRules.add("DO_NOT_EMAIL");
        }
        
        if (Boolean.TRUE.equals(lead.getEmailOptOut())) {
            score += NEGATIVE_SCORES.get("EMAIL_OPT_OUT");
            appliedRules.add("EMAIL_OPT_OUT");
        }
        
        // Check if lead has no contact information
        if ((lead.getEmail() == null || lead.getEmail().trim().isEmpty()) &&
            (lead.getPhone() == null || lead.getPhone().trim().isEmpty()) &&
            (lead.getMobile() == null || lead.getMobile().trim().isEmpty())) {
            score += NEGATIVE_SCORES.get("NO_CONTACT_INFO");
            appliedRules.add("NO_CONTACT_INFO");
        }
        
        // Check for long inactivity
        if (lead.getLastActivityAt() != null) {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
            if (lead.getLastActivityAt().isBefore(cutoff)) {
                score += NEGATIVE_SCORES.get("INACTIVE_LONG");
                appliedRules.add("INACTIVE_LONG");
            }
        }
        
        return score;
    }

    private void cacheLeadScore(UUID leadId, int score, List<String> appliedRules) {
        try {
            String cacheKey = "lead:score:" + leadId;
            String rulesKey = "lead:score:rules:" + leadId;
            
            redisTemplate.opsForValue().set(cacheKey, score, 1, TimeUnit.HOURS);
            redisTemplate.opsForValue().set(rulesKey, appliedRules, 1, TimeUnit.HOURS);
        } catch (Exception e) {
            logger.warn("Failed to cache lead score for lead: {}", leadId, e);
        }
    }
}