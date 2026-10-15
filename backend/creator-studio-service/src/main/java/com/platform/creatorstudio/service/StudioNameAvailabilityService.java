package com.platform.creatorstudio.service;

import com.platform.creatorstudio.repository.CreatorStudioRepository;
import com.platform.creatorstudio.util.BloomFilter;
import com.platform.common.security.CurrentUserAccessor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for efficient studio name availability checking using a Bloom Filter.
 * The Bloom Filter serves as a first-line defense for quick negative checks,
 * while the database remains the final authority for positive checks.
 * <p>
 * This approach significantly reduces database load for common cases where
 * studio names are not available (Bloom Filter says "definitely not in set").
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudioNameAvailabilityService {

    private final CreatorStudioRepository studioRepository;
    private final CurrentUserAccessor currentUserAccessor;

    // Bloom Filter configuration
    // Expected: 100,000 studios with 1% false positive probability
    private static final long EXPECTED_STUDIOS = 100_000L;
    private static final double FALSE_POSITIVE_PROBABILITY = 0.01; // 1%

    private BloomFilter bloomFilter;
    private boolean bloomFilterInitialized = false;

    /**
     * Initializes the Bloom Filter with existing studio names from the database.
     * This method is called after the application context is refreshed.
     */
    @PostConstruct
    public void initializeBloomFilter() {
        try {
            log.info("Initializing studio name Bloom Filter...");
            bloomFilter = new BloomFilter(EXPECTED_STUDIOS, FALSE_POSITIVE_PROBABILITY);

            // Load all existing studio names from database
            List<String> existingStudioNames = studioRepository.findAllStudioNames();

            // Add each name to the Bloom Filter
            for (String name : existingStudioNames) {
                if (name != null && !name.isEmpty()) {
                    bloomFilter.add(name.toLowerCase().trim()); // Normalize for case-insensitive comparison
                }
            }

            bloomFilterInitialized = true;
            log.info("Studio name Bloom Filter initialized with {} names. " +
                            "Expected false positive rate: {}%, Actual: {}%",
                    existingStudioNames.size(),
                    FALSE_POSITIVE_PROBABILITY * 100,
                    bloomFilter.falsePositiveProbability() * 100);
        } catch (Exception e) {
            log.error("Failed to initialize studio name Bloom Filter", e);
            // Continue without Bloom Filter - database checks will still work
            bloomFilterInitialized = false;
        }
    }

    /**
     * Checks if a studio name is available for use.
     * Uses Bloom Filter for quick negative checks, falls back to database for final verification.
     *
     * @param name The studio name to check
     * @return true if the name is available, false otherwise
     */
    public boolean isStudioNameAvailable(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        String normalizedName = normalizeStudioName(name);

        // Step 1: Quick negative check with Bloom Filter
        // If Bloom Filter says "definitely not in set", we can return true immediately
        if (!bloomFilterInitialized || !bloomFilter.mightContain(normalizedName)) {
            // Name is definitely not in the Bloom Filter, so it's available
            // (unless there's a bug in our initialization)
            log.debug("Studio name '{}' is available (Bloom Filter negative check)", name);
            return true;
        }

        // Step 2: Bloom Filter says "might be in set" - need to check database
        // This could be a true positive or a false positive
        boolean actuallyExists = studioRepository.existsByNameIgnoreCase(normalizedName);

        if (actuallyExists) {
            // Name definitely exists in database
            log.debug("Studio name '{}' is not available (confirmed by database)", name);
            return false;
        } else {
            // Bloom Filter false positive - name doesn't actually exist
            // We should add it to the Bloom Filter to avoid future false positives for this name
            log.debug("Studio name '{}' is available (Bloom Filter false positive corrected)", name);
            bloomFilter.add(normalizedName);
            return true;
        }
    }

    /**
     * Adds a newly created studio name to the Bloom Filter.
     * Should be called after a studio is successfully created in the database.
     *
     * @param studioName The name of the newly created studio
     */
    public void addStudioName(String studioName) {
        if (studioName != null && !studioName.isEmpty() && bloomFilterInitialized) {
            String normalizedName = normalizeStudioName(studioName);
            bloomFilter.add(normalizedName);
            log.debug("Added studio name '{}' to Bloom Filter", studioName);
        }
    }

    /**
     * Rebuilds the Bloom Filter from scratch using all studio names from the database.
     * Useful for recovery if the Bloom Filter state becomes inconsistent.
     */
    public void rebuildBloomFilter() {
        log.info("Rebuilding studio name Bloom Filter from database...");
        initializeBloomFilter(); // This will reload everything from DB
    }

    /**
     * Normalizes a studio name for consistent comparison.
     * Converts to lowercase and trims whitespace.
     *
     * @param name The studio name to normalize
     * @return The normalized studio name
     */
    private String normalizeStudioName(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase().trim();
    }

    /**
     * Returns the current false positive probability of the Bloom Filter.
     *
     * @return Current false positive probability (0.0 to 1.0)
     */
    public double getFalsePositiveProbability() {
        return bloomFilterInitialized ? bloomFilter.falsePositiveProbability() : 0.0;
    }

    /**
     * Returns the number of studio names currently in the Bloom Filter.
     *
     * @return Number of elements in the Bloom Filter
     */
    public int getBloomFilterSize() {
        return bloomFilterInitialized ? bloomFilter.count() : 0;
    }

    /**
     * Checks if the Bloom Filter is initialized and ready for use.
     *
     * @return true if Bloom Filter is ready, false otherwise
     */
    public boolean isBloomFilterInitialized() {
        return bloomFilterInitialized;
    }
}