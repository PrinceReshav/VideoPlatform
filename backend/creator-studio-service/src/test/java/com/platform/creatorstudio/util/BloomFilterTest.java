package com.platform.creatorstudio.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for BloomFilter implementation.
 */
class BloomFilterTest {

    private BloomFilter bloomFilter;

    @BeforeEach
    void setUp() {
        // Create a Bloom Filter expecting 1000 elements with 1% false positive rate
        bloomFilter = new BloomFilter(1000, 0.01);
    }

    @Test
    void testAddAndCheck() {
        // Add an element
        bloomFilter.add("test");

        // Check that it exists (should return true)
        assertTrue(bloomFilter.mightContain("test"), "Element should be found in Bloom Filter");

        // Check that a different element doesn't exist (should return false with high probability)
        assertFalse(bloomFilter.mightContain("completely different string"),
                "Different element should not be found in Bloom Filter");
    }

    @Test
    void testFalsePositiveRate() {
        // Add many elements
        for (int i = 0; i < 500; i++) {
            bloomFilter.add("element_" + i);
        }

        // Check false positive rate is reasonable
        double fpp = bloomFilter.falsePositiveProbability();
        assertTrue(fpp <= 0.02, "False positive rate should be close to configured rate");
        assertTrue(fpp >= 0.005, "False positive rate should be close to configured rate");
    }

    @Test
    void testNullHandling() {
        // Adding null should not cause issues
        bloomFilter.add(null);

        // Checking null should return false
        assertFalse(bloomFilter.mightContain(null), "Null should not be found in Bloom Filter");
        assertFalse(bloomFilter.mightContain(""), "Empty string should not be found in Bloom Filter");
    }

    @Test
    void testCount() {
        assertEquals(0, bloomFilter.count(), "Initial count should be zero");

        bloomFilter.add("test1");
        assertEquals(1, bloomFilter.count(), "Count should be 1 after adding one element");

        bloomFilter.add("test2");
        bloomFilter.add("test3");
        assertEquals(3, bloomFilter.count(), "Count should be 3 after adding three elements");
    }

    @Test
    void testClear() {
        bloomFilter.add("test1");
        bloomFilter.add("test2");
        assertEquals(2, bloomFilter.count(), "Count should be 2 before clear");

        bloomFilter.clear();
        assertEquals(0, bloomFilter.count(), "Count should be 0 after clear");
        assertFalse(bloomFilter.mightContain("test1"), "Element should not be found after clear");
    }
}