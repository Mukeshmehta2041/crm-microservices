package com.crm.platform.contacts.service;

import com.crm.platform.contacts.dto.ContactRequest;
import com.crm.platform.contacts.entity.Contact;
import com.crm.platform.contacts.repository.ContactRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ContactDeduplicationService {

    private final ContactRepository contactRepository;
    
    @Value("${contacts.deduplication.enabled:true}")
    private boolean deduplicationEnabled;
    
    @Value("${contacts.deduplication.match-threshold:0.8}")
    private double matchThreshold;

    public ContactDeduplicationService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    public boolean isDuplicateContact(ContactRequest request, UUID tenantId) {
        return isDuplicateContact(request, tenantId, null);
    }

    public boolean isDuplicateContact(ContactRequest request, UUID tenantId, UUID excludeContactId) {
        if (!deduplicationEnabled) {
            return false;
        }

        // Check exact email match
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            boolean emailExists = contactRepository.existsByTenantIdAndEmail(tenantId, request.getEmail());
            if (emailExists) {
                // If excluding a contact, check if the found contact is different
                if (excludeContactId != null) {
                    return contactRepository.findByTenantIdAndEmail(tenantId, request.getEmail())
                        .map(contact -> !contact.getId().equals(excludeContactId))
                        .orElse(false);
                }
                return true;
            }
        }

        // Check name-based duplicates
        List<Contact> nameMatches = contactRepository
            .findPotentialDuplicatesByName(tenantId, request.getFirstName(), request.getLastName());
        
        if (excludeContactId != null) {
            nameMatches = nameMatches.stream()
                .filter(contact -> !contact.getId().equals(excludeContactId))
                .collect(Collectors.toList());
        }

        if (!nameMatches.isEmpty()) {
            // Apply fuzzy matching logic
            return nameMatches.stream()
                .anyMatch(contact -> calculateSimilarity(request, contact) >= matchThreshold);
        }

        // Check contact information duplicates
        if (request.getPhone() != null || request.getEmail() != null) {
            List<Contact> contactMatches = contactRepository
                .findPotentialDuplicatesByContact(tenantId, request.getEmail(), 
                                                 request.getPhone(), request.getPhone());
            
            if (excludeContactId != null) {
                contactMatches = contactMatches.stream()
                    .filter(contact -> !contact.getId().equals(excludeContactId))
                    .collect(Collectors.toList());
            }

            return !contactMatches.isEmpty();
        }

        return false;
    }

    public List<Contact> findDuplicates(Contact contact, UUID tenantId) {
        List<Contact> duplicates = contactRepository
            .findPotentialDuplicatesByName(tenantId, contact.getFirstName(), contact.getLastName());
        
        // Remove the contact itself from results
        duplicates = duplicates.stream()
            .filter(c -> !c.getId().equals(contact.getId()))
            .collect(Collectors.toList());

        // Add contact-based duplicates
        if (contact.getEmail() != null || contact.getPhone() != null || contact.getMobile() != null) {
            List<Contact> contactDuplicates = contactRepository
                .findPotentialDuplicatesByContact(tenantId, contact.getEmail(), 
                                                 contact.getPhone(), contact.getMobile());
            
            contactDuplicates.stream()
                .filter(c -> !c.getId().equals(contact.getId()))
                .filter(c -> duplicates.stream().noneMatch(d -> d.getId().equals(c.getId())))
                .forEach(duplicates::add);
        }

        // Filter by similarity threshold
        return duplicates.stream()
            .filter(duplicate -> calculateSimilarity(contact, duplicate) >= matchThreshold)
            .collect(Collectors.toList());
    }

    public Contact mergeContacts(Contact primary, List<Contact> duplicates, UUID userId) {
        // Merge logic: combine information from duplicates into primary contact
        
        // Merge tags
        duplicates.forEach(duplicate -> {
            if (duplicate.getTags() != null) {
                duplicate.getTags().forEach(tag -> {
                    if (primary.getTags() == null || !primary.getTags().contains(tag)) {
                        if (primary.getTags() == null) {
                            primary.setTags(List.of(tag));
                        } else {
                            primary.getTags().add(tag);
                        }
                    }
                });
            }
        });

        // Merge custom fields
        duplicates.forEach(duplicate -> {
            if (duplicate.getCustomFields() != null) {
                duplicate.getCustomFields().forEach((key, value) -> {
                    if (primary.getCustomFields() == null || !primary.getCustomFields().containsKey(key)) {
                        if (primary.getCustomFields() == null) {
                            primary.setCustomFields(new HashMap<>(Map.of(key, value)));
                        } else {
                            primary.getCustomFields().put(key, value);
                        }
                    }
                });
            }
        });

        // Fill in missing information from duplicates
        duplicates.forEach(duplicate -> {
            if (primary.getPhone() == null && duplicate.getPhone() != null) {
                primary.setPhone(duplicate.getPhone());
            }
            if (primary.getMobile() == null && duplicate.getMobile() != null) {
                primary.setMobile(duplicate.getMobile());
            }
            if (primary.getTitle() == null && duplicate.getTitle() != null) {
                primary.setTitle(duplicate.getTitle());
            }
            if (primary.getDepartment() == null && duplicate.getDepartment() != null) {
                primary.setDepartment(duplicate.getDepartment());
            }
            if (primary.getLeadSource() == null && duplicate.getLeadSource() != null) {
                primary.setLeadSource(duplicate.getLeadSource());
            }
            if (primary.getSocialProfiles() == null && duplicate.getSocialProfiles() != null) {
                primary.setSocialProfiles(duplicate.getSocialProfiles());
            }
            if (primary.getMailingAddress() == null && duplicate.getMailingAddress() != null) {
                primary.setMailingAddress(duplicate.getMailingAddress());
            }
            
            // Merge notes
            if (duplicate.getNotes() != null && !duplicate.getNotes().isEmpty()) {
                String mergedNotes = primary.getNotes() != null ? 
                    primary.getNotes() + "\n\n--- Merged from duplicate contact ---\n" + duplicate.getNotes() :
                    duplicate.getNotes();
                primary.setNotes(mergedNotes);
            }
            
            // Use highest lead score
            if (duplicate.getLeadScore() != null && 
                (primary.getLeadScore() == null || duplicate.getLeadScore() > primary.getLeadScore())) {
                primary.setLeadScore(duplicate.getLeadScore());
            }
        });

        primary.setUpdatedBy(userId);
        return primary;
    }

    private double calculateSimilarity(ContactRequest request, Contact contact) {
        double score = 0.0;
        int factors = 0;

        // Name similarity
        if (request.getFirstName() != null && contact.getFirstName() != null) {
            score += stringSimilarity(request.getFirstName().toLowerCase(), 
                                    contact.getFirstName().toLowerCase());
            factors++;
        }
        
        if (request.getLastName() != null && contact.getLastName() != null) {
            score += stringSimilarity(request.getLastName().toLowerCase(), 
                                    contact.getLastName().toLowerCase());
            factors++;
        }

        // Email similarity
        if (request.getEmail() != null && contact.getEmail() != null) {
            score += request.getEmail().equalsIgnoreCase(contact.getEmail()) ? 1.0 : 0.0;
            factors++;
        }

        // Phone similarity
        if (request.getPhone() != null && contact.getPhone() != null) {
            score += normalizePhone(request.getPhone()).equals(normalizePhone(contact.getPhone())) ? 1.0 : 0.0;
            factors++;
        }

        return factors > 0 ? score / factors : 0.0;
    }

    private double calculateSimilarity(Contact contact1, Contact contact2) {
        double score = 0.0;
        int factors = 0;

        // Name similarity
        if (contact1.getFirstName() != null && contact2.getFirstName() != null) {
            score += stringSimilarity(contact1.getFirstName().toLowerCase(), 
                                    contact2.getFirstName().toLowerCase());
            factors++;
        }
        
        if (contact1.getLastName() != null && contact2.getLastName() != null) {
            score += stringSimilarity(contact1.getLastName().toLowerCase(), 
                                    contact2.getLastName().toLowerCase());
            factors++;
        }

        // Email similarity
        if (contact1.getEmail() != null && contact2.getEmail() != null) {
            score += contact1.getEmail().equalsIgnoreCase(contact2.getEmail()) ? 1.0 : 0.0;
            factors++;
        }

        // Phone similarity
        if (contact1.getPhone() != null && contact2.getPhone() != null) {
            score += normalizePhone(contact1.getPhone()).equals(normalizePhone(contact2.getPhone())) ? 1.0 : 0.0;
            factors++;
        }

        return factors > 0 ? score / factors : 0.0;
    }

    private double stringSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        
        // Simple Levenshtein distance-based similarity
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        
        return (maxLen - levenshteinDistance(s1, s2)) / (double) maxLen;
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                        dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1),
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1)
                    );
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("[^0-9]", "");
    }
}