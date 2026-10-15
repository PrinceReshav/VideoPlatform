package com.platform.creatorstudio.dto;

import com.platform.creatorstudio.model.StudioLiveSession;
import com.platform.creatorstudio.model.StudioLiveSession.LiveSessionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for StudioLiveSession entity.
 * Used for transferring studio live session data between processes.
 */
public class LiveSessionDTO {

    private UUID id;
    private UUID studioId;
    private String title;
    private String description;
    private String sessionCategory;
    private String topic;
    private Instant scheduledStartTime;
    private Instant actualStartTime;
    private Instant actualEndTime;
    private LiveSessionStatus status;
    private boolean isPublic;
    private boolean requiresRegistration;
    private Integer maxAttendees;
    private int currentAttendeeCount;
    private boolean requiresPayment;
    private Integer attendancePricePaise;
    private boolean allowsCommenting;
    private boolean isRecording;
    private boolean hasReplay;
    private String recordingReferenceId;
    private Instant createdAt;
    private Instant updatedAt;

    // Constructors
    public LiveSessionDTO() {}

    public LiveSessionDTO(StudioLiveSession session) {
        this.id = session.getId();
        this.studioId = session.getStudioId();
        this.title = session.getTitle();
        this.description = session.getDescription();
        this.sessionCategory = session.getSessionCategory();
        this.topic = session.getTopic();
        this.scheduledStartTime = session.getScheduledStartTime();
        this.actualStartTime = session.getActualStartTime();
        this.actualEndTime = session.getActualEndTime();
        this.status = session.getStatus();
        this.isPublic = session.isPublic();
        this.requiresRegistration = session.requiresRegistration();
        this.maxAttendees = session.getMaxAttendees();
        this.currentAttendeeCount = session.getCurrentAttendeeCount();
        this.requiresPayment = session.requiresPayment();
        this.attendancePricePaise = session.getAttendancePricePaise();
        this.allowsCommenting = session.allowsCommenting();
        this.isRecording = session.isRecording();
        this.hasReplay = session.hasReplay();
        this.recordingReferenceId = session.getRecordingReferenceId();
        this.createdAt = session.getCreatedAt();
        this.updatedAt = session.getUpdatedAt();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getStudioId() { return studioId; }
    public void setStudioId(UUID studioId) { this.studioId = studioId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSessionCategory() { return sessionCategory; }
    public void setSessionCategory(String sessionCategory) { this.sessionCategory = sessionCategory; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public Instant getScheduledStartTime() { return scheduledStartTime; }
    public void setScheduledStartTime(Instant scheduledStartTime) { this.scheduledStartTime = scheduledStartTime; }

    public Instant getActualStartTime() { return actualStartTime; }
    public void setActualStartTime(Instant actualStartTime) { this.actualStartTime = actualStartTime; }

    public Instant getActualEndTime() { return actualEndTime; }
    public void setActualEndTime(Instant actualEndTime) { this.actualEndTime = actualEndTime; }

    public LiveSessionStatus getStatus() { return status; }
    public void setStatus(LiveSessionStatus status) { this.status = status; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    public boolean requiresRegistration() { return requiresRegistration; }
    public void setRequiresRegistration(boolean requiresRegistration) { this.requiresRegistration = requiresRegistration; }

    public Integer getMaxAttendees() { return maxAttendees; }
    public void setMaxAttendees(Integer maxAttendees) { this.maxAttendees = maxAttendees; }

    public int getCurrentAttendeeCount() { return currentAttendeeCount; }
    public void setCurrentAttendeeCount(int currentAttendeeCount) { this.currentAttendeeCount = currentAttendeeCount; }

    public boolean requiresPayment() { return requiresPayment; }
    public void setRequiresPayment(boolean requiresPayment) { this.requiresPayment = requiresPayment; }

    public Integer getAttendancePricePaise() { return attendancePricePaise; }
    public void setAttendancePricePaise(Integer attendancePricePaise) { this.attendancePricePaise = attendancePricePaise; }

    public boolean allowsCommenting() { return allowsCommenting; }
    public void setAllowsCommenting(boolean allowsCommenting) { this.allowsCommenting = allowsCommenting; }

    public boolean isRecording() { return isRecording; }
    public void setIsRecording(boolean isRecording) { this.isRecording = isRecording; }

    public boolean hasReplay() { return hasReplay; }
    public void setHasReplay(boolean hasReplay) { this.hasReplay = hasReplay; }

    public String getRecordingReferenceId() { return recordingReferenceId; }
    public void setRecordingReferenceId(String recordingReferenceId) { this.recordingReferenceId = recordingReferenceId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}