package com.platform.creatorstudio.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for studio live session actions.
 * Used for receiving live session action data from clients.
 */
public class LiveSessionActionDTO {

    private UUID studioId;
    private String title;
    private String description;
    private String sessionCategory;
    private String topic;
    private Instant scheduledStartTime;
    private boolean publicSession;
    private boolean requiresRegistration;
    private Integer maxAttendees;
    private boolean requiresPayment;
    private Integer attendancePricePaise;
    private boolean allowsCommenting;

    // Constructors
    public LiveSessionActionDTO() {}

    // Getters and Setters
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

    public boolean isPublic() { return publicSession; }
    public void setPublic(boolean publicSession) { this.publicSession = publicSession; }

    public boolean isRequiresRegistration() { return requiresRegistration; }
    public void setRequiresRegistration(boolean requiresRegistration) { this.requiresRegistration = requiresRegistration; }

    public Integer getMaxAttendees() { return maxAttendees; }
    public void setMaxAttendees(Integer maxAttendees) { this.maxAttendees = maxAttendees; }

    public boolean isRequiresPayment() { return requiresPayment; }
    public void setRequiresPayment(boolean requiresPayment) { this.requiresPayment = requiresPayment; }

    public Integer getAttendancePricePaise() { return attendancePricePaise; }
    public void setAttendancePricePaise(Integer attendancePricePaise) { this.attendancePricePaise = attendancePricePaise; }

    public boolean isAllowsCommenting() { return allowsCommenting; }
    public void setAllowsCommenting(boolean allowsCommenting) { this.allowsCommenting = allowsCommenting; }
}