package com.satyam.quotation.util;

import com.satyam.quotation.repository.CompanyRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Generates unique License IDs for companies.
 * Format: QF-{YEAR}-{3 LETTERS FROM NAME}-{4 DIGIT SEQ}
 * Example: QF-2025-ABC-0001
 *
 * Once generated, the License ID never changes — it identifies
 * the client subscription permanently.
 */
@Component
public class LicenseIdGenerator {

    private final CompanyRepository companyRepository;

    public LicenseIdGenerator(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public String generate(String companyName) {
        String year = String.valueOf(LocalDateTime.now().getYear());
        String prefix = extractPrefix(companyName);

        // Find next available sequence number
        int seq = 1;
        String candidate;
        do {
            candidate = String.format("QF-%s-%s-%04d", year, prefix, seq);
            seq++;
        } while (companyRepository.existsByLicenseId(candidate));

        return candidate;
    }

    private String extractPrefix(String name) {
        if (name == null || name.isBlank()) return "QFL";
        // Keep only letters, take first 3, uppercase
        String letters = name.replaceAll("[^a-zA-Z]", "");
        if (letters.length() >= 3) return letters.substring(0, 3).toUpperCase();
        // Pad with X if less than 3 letters
        return (letters + "XXX").substring(0, 3).toUpperCase();
    }
}
