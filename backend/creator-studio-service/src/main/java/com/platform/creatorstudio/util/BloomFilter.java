package com.platform.creatorstudio.util;

import java.util.BitSet;

/**
 * Simple Bloom Filter implementation for efficient set membership testing.
 * Provides fast probabilistic checks with configurable false positive rate.
 * <p>
 * Note: Bloom Filter can have false positives (says an element is in the set when it's not),
 * but never false negatives (says an element is not in the set when it is).
 * Therefore, the database must remain the final authority for studio name uniqueness.
 */
public class BloomFilter {
    private final BitSet bitset;
    private final int numHashes;
    private final int size;
    private int numElements = 0;

    /**
     * Creates a Bloom Filter with the expected number of elements and desired false positive probability.
     *
     * @param expectedInsertions Expected number of elements to be inserted
     * @param fpp Desired false positive probability (between 0 and 1)
     */
    public BloomFilter(long expectedInsertions, double fpp) {
        if (fpp <= 0 || fpp >= 1) {
            throw new IllegalArgumentException("False positive probability must be between 0 and 1 exclusive");
        }
        if (expectedInsertions <= 0) {
            throw new IllegalArgumentException("Expected insertions must be positive");
        }

        // Calculate optimal size and number of hash functions
        // m = -(n * ln(p)) / (ln(2)^2)
        // k = (m/n) * ln(2)
        this.size = (int) Math.ceil(-(expectedInsertions * Math.log(fpp)) / (Math.log(2) * Math.log(2)));
        this.numHashes = Math.max(1, (int) Math.round((double) size / expectedInsertions * Math.log(2)));
        this.bitset = new BitSet(size);
    }

    /**
     * Adds an element to the Bloom Filter.
     *
     * @param element The element to add (will be converted to string)
     */
    public void add(Object element) {
        if (element == null) {
            return;
        }
        String str = element.toString();
        int[] hashes = getHashes(str);
        for (int hash : hashes) {
            bitset.set(hash);
        }
        numElements++;
    }

    /**
     * Checks if an element might be in the set.
     *
     * @param element The element to check
     * @return true if the element is definitely in the set or might be in the set (possible false positive),
     *         false if the element is definitely not in the set
     */
    public boolean mightContain(Object element) {
        if (element == null) {
            return false;
        }
        String str = element.toString();
        int[] hashes = getHashes(str);
        for (int hash : hashes) {
            if (!bitset.get(hash)) {
                return false; // Definitely not in set
            }
        }
        return true; // Might be in set (could be false positive)
    }

    /**
     * Returns the number of elements that have been added to the filter.
     *
     * @return Number of elements added
     */
    public int count() {
        return numElements;
    }

    /**
     * Returns the theoretical false positive probability based on current state.
     *
     * @return Theoretical false positive probability
     */
    public double falsePositiveProbability() {
        if (numElements == 0) {
            return 0.0;
        }
        double ratio = (double) numElements / size;
        return Math.pow(1.0 - Math.exp(-numHashes * ratio), numHashes);
    }

    /**
     * Generates multiple hash values for the given string.
     * Uses a simple approach with different seeds for each hash.
     *
     * @param value The string to hash
     * @return Array of hash values
     */
    private int[] getHashes(String value) {
        int[] hashes = new int[numHashes];
        int hash1 = value.hashCode();
        int hash2 = hash1 >>> 16;

        for (int i = 0; i < numHashes; i++) {
            // Use double hashing to generate multiple hash values
            int combinedHash = hash1 + (i * hash2);
            // Ensure positive value within bitset bounds
            hashes[i] = Math.abs(combinedHash % size);
        }
        return hashes;
    }

    /**
     * Clears all bits in the filter, resetting it to empty state.
     */
    public void clear() {
        bitset.clear();
        numElements = 0;
    }
}