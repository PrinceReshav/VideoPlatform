package com.platform.creatorstudio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Records one user's attendance in a live session. */
@Entity
@Table(name = "studio_live_session_attendees", schema = "creator_studio_schema",
        uniqueConstraints = @UniqueConstraint(name = "uk_studio_session_attendee", columnNames = {"session_id", "user_id"}),
        indexes = @Index(name = "idx_studio_session_attendees_user", columnList = "user_id"))
@Getter
@Setter
public class StudioLiveSessionAttendee {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();
}
