package com.platform.creatorstudio.model;

import com.platform.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a live session in a studio.
 * Manages the lifecycle and metadata of studio live sessions.
 */
@Entity
@Table(name = "studio_live_sessions", schema = "creator_studio_schema",
        indexes = {
                @Index(name = "idx_studio_live_sessions_studio", columnList = "studio_id"),
                @Index(name = "idx_studio_live_sessions_status", columnList = "status"),
                @Index(name = "idx_studio_live_sessions_scheduled", columnList = "scheduled_start_time"),
                @Index(name = "idx_studio_live_sessions_actual", columnList = "actual_start_time")
        })
@Getter
@Setter
public class StudioLiveSession extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The studio hosting the live session */
    @Column(name = "studio_id", nullable = false)
    private UUID studioId;

    /** Title of the live session */
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    /** Description of the live session */
    @Column(name = "description", length = 2000)
    private String description;

    /** Category of the live session */
    @Column(name = "session_category", length = 50)
    private String sessionCategory;

    /** Topic or subject of the live session */
    @Column(name = "topic", length = 500)
    private String topic;

    /** Scheduled start time for the session */
    @Column(name = "scheduled_start_time")
    private Instant scheduledStartTime;

    /** Actual start time of the session */
    @Column(name = "actual_start_time")
    private Instant actualStartTime;

    /** Actual end time of the session */
    @Column(name = "actual_end_time")
    private Instant actualEndTime;

    /** Current status of the live session */
    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private LiveSessionStatus status = LiveSessionStatus.DRAFT;

    /** Whether the session is open for public attendance */
    @Column(name = "is_public", nullable = false)
    private boolean isPublic = true;

    /** Whether registration is required to attend */
    @Column(name = "requires_registration", nullable = false)
    private boolean requiresRegistration = false;

    /** Maximum number of attendees allowed (0 = unlimited) */
    @Column(name = "max_attendees")
    private Integer maxAttendees;

    /** Current number of attendees in the session */
    @Column(name = "current_attendee_count", nullable = false)
    private int currentAttendeeCount = 0;

    /** Whether the session requires payment for attendance */
    @Column(name = "requires_payment", nullable = false)
    private boolean requiresPayment = false;

    /** Price for attending the session (in paise for precise currency handling) */
    @Column(name = "attendance_price_paise")
    private Integer attendancePricePaise; // e.g., 10000 = ₹100

    /** Whether the session allows commenting */
    @Column(name = "allows_commenting", nullable = false)
    private boolean allowsCommenting = true;

    /** Whether the session is being recorded */
    @Column(name = "is_recording", nullable = false)
    private boolean isRecording = false;

    /** Whether a recording is available for replay */
    @Column(name = "has_replay", nullable = false)
    private boolean hasReplay = false;

    /** Reference to recording ID (if available) */
    @Column(name = "recording_reference_id", length = 255)
    private String recordingReferenceId;
public enum LiveSessionStatus {
        DRAFT,          // Session is being planned, not yet scheduled
        SCHEDULED,      // Session is scheduled for future time
        LIVE,           // Session is currently live and streaming
        ENDED,          // Session has ended
        CANCELLED,      // Session was cancelled before starting
        COMPLETED       // Session has ended and processing is complete
    }

    // Helper methods
    public boolean isScheduled() {
        return status == LiveSessionStatus.SCHEDULED;
    }

    public boolean isLive() {
        return status == LiveSessionStatus.LIVE;
    }

    public boolean isEndedOrCompleted() {
        return status == LiveSessionStatus.ENDED || status == LiveSessionStatus.COMPLETED;
    }

    public boolean canStart() {
        return status == LiveSessionStatus.SCHEDULED &&
               scheduledStartTime != null &&
               Instant.now().isAfter(scheduledStartTime.minusSeconds(300)); // Can start 5 min early
    }

    public boolean canEnd() {
        return status == LiveSessionStatus.LIVE;
    }

    public boolean isUpcoming() {
        return status == LiveSessionStatus.SCHEDULED &&
               scheduledStartTime != null &&
               Instant.now().isBefore(scheduledStartTime);
    }

    public boolean requiresRegistration() {
        return requiresRegistration;
    }

    public boolean requiresPayment() {
        return requiresPayment;
    }

    public boolean allowsCommenting() {
        return allowsCommenting;
    }

    public boolean hasReplay() {
        return hasReplay;
    }

    public boolean isOngoing() {
        return status == LiveSessionStatus.LIVE;
    }

    public void startLive() {
        this.status = LiveSessionStatus.LIVE;
        this.actualStartTime = Instant.now();
    }

    public void endLive() {
        this.status = LiveSessionStatus.ENDED;
        this.actualEndTime = Instant.now();
    }

    public void cancel() {
        this.status = LiveSessionStatus.CANCELLED;
    }

    public void markAsCompleted() {
        this.status = LiveSessionStatus.COMPLETED;
    }

    public void setRecording(boolean recording) {
        this.isRecording = recording;
    }

    public void setHasReplay(boolean hasReplay) {
        this.hasReplay = hasReplay;
    }
}