
package com.platform.creatorstudio.service;

import com.platform.common.security.CurrentUserAccessor;
import com.platform.creatorstudio.repository.CreatorStudioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudioNameAvailabilityServiceTest {

    @Mock
    private CreatorStudioRepository studioRepository;

    @Mock
    private CurrentUserAccessor currentUserAccessor;

    @InjectMocks
    private StudioNameAvailabilityService nameAvailabilityService;

    @BeforeEach
    void setUp() {
        nameAvailabilityService =
                new StudioNameAvailabilityService(
                        studioRepository,
                        currentUserAccessor
                );
    }

    @Test
    void testInitializeBloomFilter() {
        when(studioRepository.findAllStudioNames())
                .thenReturn(Arrays.asList(
                        "studio1",
                        "studio2",
                        "STUDIO3"
                ));

        nameAvailabilityService.initializeBloomFilter();

        assertTrue(nameAvailabilityService.isBloomFilterInitialized());
        assertEquals(3, nameAvailabilityService.getBloomFilterSize());

        // Existing names are unavailable.
        assertFalse(nameAvailabilityService.isStudioNameAvailable("studio1"));
        assertFalse(nameAvailabilityService.isStudioNameAvailable("STUDIO1"));

        // A name not present in the filter is available.
        assertTrue(nameAvailabilityService.isStudioNameAvailable("nonexistent"));

        verify(studioRepository, times(1)).findAllStudioNames();

        // A definite Bloom-filter miss should not query the database.
        verify(studioRepository, never())
                .existsByNameIgnoreCase(anyString());
    }

    @Test
    void testIsStudioNameAvailable_NotInBloomFilter() {
        when(studioRepository.findAllStudioNames())
                .thenReturn(Arrays.asList("existingStudio"));

        nameAvailabilityService.initializeBloomFilter();

        boolean available =
                nameAvailabilityService.isStudioNameAvailable("nonexistent");

        assertTrue(available);

        verify(studioRepository, times(1)).findAllStudioNames();
        verify(studioRepository, never())
                .existsByNameIgnoreCase(anyString());
    }

    @Test
    void testIsStudioNameAvailable_InBloomFilterButNotInDB_FalsePositive() {
        /*
         * Arrange for a Bloom-filter possible match by adding the queried
         * name to the filter. The mocked database still says it does not
         * exist, simulating a stale filter or false positive.
         */
        when(studioRepository.findAllStudioNames())
                .thenReturn(Arrays.asList("existingStudio"));

        when(studioRepository.existsByNameIgnoreCase("nonexistent"))
                .thenReturn(false);

        nameAvailabilityService.initializeBloomFilter();
        nameAvailabilityService.addStudioName("nonexistent");

        boolean available =
                nameAvailabilityService.isStudioNameAvailable("nonexistent");

        assertTrue(available);

        verify(studioRepository, times(1)).findAllStudioNames();
        verify(studioRepository, atLeastOnce())
                .existsByNameIgnoreCase("nonexistent");
    }

    @Test
    void testIsStudioNameAvailable_InBloomFilterAndInDB() {
        when(studioRepository.findAllStudioNames())
                .thenReturn(Arrays.asList("existingStudio"));

        when(studioRepository.existsByNameIgnoreCase("existingstudio"))
                .thenReturn(true);

        nameAvailabilityService.initializeBloomFilter();

        boolean available =
                nameAvailabilityService.isStudioNameAvailable("existingStudio");

        assertFalse(available);

        verify(studioRepository, times(1)).findAllStudioNames();
        verify(studioRepository, atLeastOnce())
                .existsByNameIgnoreCase("existingstudio");
    }

    @Test
    void testAddStudioName() {
        when(studioRepository.findAllStudioNames())
                .thenReturn(Arrays.asList("initialStudio"));

        nameAvailabilityService.initializeBloomFilter();

        int initialSize = nameAvailabilityService.getBloomFilterSize();

        nameAvailabilityService.addStudioName("newStudio");

        int finalSize = nameAvailabilityService.getBloomFilterSize();

        assertEquals(initialSize + 1, finalSize);
        assertTrue(nameAvailabilityService.isBloomFilterInitialized());

        // The newly added name should be considered occupied.
        assertFalse(nameAvailabilityService.isStudioNameAvailable("newStudio"));
    }

    @Test
    void testRebuildBloomFilter() {
        when(studioRepository.findAllStudioNames())
                .thenReturn(Arrays.asList("studio1"))
                .thenReturn(Arrays.asList("studio2", "studio3"));

        nameAvailabilityService.initializeBloomFilter();

        nameAvailabilityService.addStudioName("tempStudio");

        assertEquals(2, nameAvailabilityService.getBloomFilterSize());

        nameAvailabilityService.rebuildBloomFilter();

        assertTrue(nameAvailabilityService.isBloomFilterInitialized());
        assertEquals(2, nameAvailabilityService.getBloomFilterSize());

        // The rebuilt filter reflects the database snapshot.
        assertFalse(nameAvailabilityService.isStudioNameAvailable("studio2"));
        assertFalse(nameAvailabilityService.isStudioNameAvailable("studio3"));

        // A definite miss is available.
        assertTrue(nameAvailabilityService.isStudioNameAvailable("nonexistent"));

        verify(studioRepository, times(2)).findAllStudioNames();
    }

    @Test
    void testCaseInsensitiveHandling() {
        when(studioRepository.findAllStudioNames())
                .thenReturn(Arrays.asList("MyStudio"));

        /*
         * Normalize the stubbing to match the lowercase value passed by
         * the production service.
         */
        when(studioRepository.existsByNameIgnoreCase("mystudio"))
                .thenReturn(true);

        nameAvailabilityService.initializeBloomFilter();

        assertFalse(nameAvailabilityService.isStudioNameAvailable("MYSTUDIO"));
        assertFalse(nameAvailabilityService.isStudioNameAvailable("mystudio"));
        assertFalse(nameAvailabilityService.isStudioNameAvailable("MyStUdIo"));

        assertTrue(nameAvailabilityService.isStudioNameAvailable("differentname"));

        verify(studioRepository, times(1)).findAllStudioNames();

        /*
         * The three case variants should produce the same normalized
         * repository argument. The exact call count depends on whether
         * the service caches availability results.
         */
        verify(studioRepository, atLeastOnce())
                .existsByNameIgnoreCase("mystudio");
    }
}
