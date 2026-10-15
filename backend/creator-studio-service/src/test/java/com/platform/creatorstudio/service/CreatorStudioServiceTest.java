
package com.platform.creatorstudio.service;

import com.platform.creatorstudio.model.CreatorStudio;
import com.platform.creatorstudio.model.CreatorStudio.OwnerType;
import com.platform.creatorstudio.model.StudioCategory;
import com.platform.creatorstudio.repository.CreatorStudioRepository;
import com.platform.creatorstudio.repository.StudioMembershipRepository;
import com.platform.common.security.CurrentUserAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatorStudioServiceTest {

    @Mock
    private CurrentUserAccessor currentUserAccessor;

    @Mock
    private CreatorStudioRepository studioRepository;

    @Mock
    private StudioMembershipRepository membershipRepository;

    @Mock
    private StudioNameAvailabilityService nameAvailabilityService;

    @InjectMocks
    private CreatorStudioService creatorStudioService;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
    }

    @Test
    void testCreateStudioSuccess() {
        String name = "teststudio";
        String displayName = "Test Studio";
        String description = "A test studio";
        String avatarUrl = "http://example.com/avatar.jpg";
        String bannerUrl = "http://example.com/banner.jpg";
        StudioCategory category = StudioCategory.DANCE_CHOREOGRAPHY;
        OwnerType ownerType = OwnerType.INDIVIDUAL;

        when(currentUserAccessor.getCurrentUserId()).thenReturn(testUserId);
        when(nameAvailabilityService.isStudioNameAvailable(name))
                .thenReturn(true);
        when(studioRepository.save(any(CreatorStudio.class)))
                .thenAnswer(invocation -> {
                    CreatorStudio studio = invocation.getArgument(0);
                    if (studio.getId() == null) {
                        studio.setId(UUID.randomUUID());
                    }
                    return studio;
                });

        CreatorStudio studio = creatorStudioService.createStudio(
                name, displayName, description, avatarUrl, bannerUrl,
                category, ownerType);

        assertNotNull(studio);
        assertEquals(name, studio.getName());
        assertEquals(displayName, studio.getDisplayName());
        assertEquals(description, studio.getDescription());
        assertEquals(avatarUrl, studio.getAvatarUrl());
        assertEquals(bannerUrl, studio.getBannerUrl());
        assertEquals(category, studio.getPrimaryCategory());
        assertEquals(ownerType, studio.getOwnerType());
        assertEquals(testUserId, studio.getOwnerId());
        assertFalse(studio.isVerified());
        assertTrue(studio.isActive());

        verify(nameAvailabilityService).isStudioNameAvailable(name);
        verify(studioRepository).save(any(CreatorStudio.class));
        verify(membershipRepository).save(any());
        verify(nameAvailabilityService).addStudioName(name);
    }

    @Test
    void testCreateStudioDuplicateName() {
        String name = "teststudio";

        when(nameAvailabilityService.isStudioNameAvailable(name))
                .thenReturn(false);

        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> creatorStudioService.createStudio(
                        name, "Display Name", "Description", null, null,
                        StudioCategory.DANCE_CHOREOGRAPHY,
                        OwnerType.INDIVIDUAL)
        );

        assertTrue(exception.getMessage().contains("Studio name already exists"));
        verify(nameAvailabilityService).isStudioNameAvailable(name);
        verify(studioRepository, never()).save(any(CreatorStudio.class));
    }

    @Test
    void testGetStudioByName() {
        String name = "teststudio";
        UUID studioId = UUID.randomUUID();

        CreatorStudio expectedStudio = new CreatorStudio();
        expectedStudio.setId(studioId);
        expectedStudio.setName(name);
        expectedStudio.setOwnerId(testUserId);

        when(studioRepository.findByNameIgnoreCase(name))
                .thenReturn(Optional.of(expectedStudio));

        CreatorStudio studio = creatorStudioService.getStudioByName(name);

        assertNotNull(studio);
        assertEquals(studioId, studio.getId());
        assertEquals(name, studio.getName());

        verify(studioRepository).findByNameIgnoreCase(name);
    }

    @Test
    void testIsStudioNameAvailable() {
        String name = "teststudio";

        when(nameAvailabilityService.isStudioNameAvailable(name))
                .thenReturn(true);

        boolean available = creatorStudioService.isStudioNameAvailable(name);

        assertTrue(available);
        verify(nameAvailabilityService).isStudioNameAvailable(name);
    }

    @Test
    void testIsStudioNameUnavailable() {
        String name = "teststudio";

        when(nameAvailabilityService.isStudioNameAvailable(name))
                .thenReturn(false);

        boolean available = creatorStudioService.isStudioNameAvailable(name);

        assertFalse(available);
        verify(nameAvailabilityService).isStudioNameAvailable(name);
    }
}
