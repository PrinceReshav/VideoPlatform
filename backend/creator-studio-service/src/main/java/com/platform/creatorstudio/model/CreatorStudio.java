package com.platform.creatorstudio.model;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity representing a creator's studio.
 * Studios have unique names and serve as the creator's identity/content hub.
 */
@Entity
@Table(name = "creator_studios", schema = "creator_studio_schema",
        indexes = {
                @Index(name = "idx_creator_studios_name", columnList = "name"),
                @Index(name = "idx_creator_studios_owner", columnList = "owner_id"),
                @Index(name = "idx_creator_studios_category", columnList = "primary_category")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_creator_studios_name", columnNames = "name")
        })
@Getter
@Setter
public class CreatorStudio extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Unique studio name (normalized for case-insensitive comparison) */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Display name for the studio (can differ from normalized name) */
    @Column(name = "display_name", length = 150)
    private String displayName;

    /** Description of the studio */
    @Column(name = "description", length = 2000)
    private String description;

    /** URL to studio avatar/logo */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /** URL to studio banner/header image */
    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    /** Primary category of the studio */
    @Column(name = "primary_category", length = 50)
    @Enumerated(EnumType.STRING)
    private StudioCategory primaryCategory;

    /** Secondary categories (can have multiple) */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "creator_studio_categories", schema = "creator_studio_schema",
            joinColumns = @JoinColumn(name = "studio_id"))
    @Column(name = "category", length = 50)
    @Enumerated(EnumType.STRING)
    private Set<StudioCategory> secondaryCategories = new HashSet<>();

    /** Owner type: INDIVIDUAL or TEAM */
    @Column(name = "owner_type", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private OwnerType ownerType = OwnerType.INDIVIDUAL;

    /** ID of the user who owns this studio */
    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    /** Whether the studio is verified (like creator verification) */
    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    /** Whether the studio is active/deactivated */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public enum OwnerType {
        INDIVIDUAL, TEAM
    }

    // Helper methods
    public boolean isOwnedBy(UUID userId) {
        return ownerId != null && ownerId.equals(userId);
    }

    public boolean isActiveAndNotDeleted() {
        return isActive && !isDeleted();
    }

    public void addSecondaryCategory(StudioCategory category) {
        this.secondaryCategories.add(category);
    }

    public void removeSecondaryCategory(StudioCategory category) {
        this.secondaryCategories.remove(category);
    }
}