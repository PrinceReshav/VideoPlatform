package com.platform.creatorstudio.service;

import com.platform.creatorstudio.model.StudioLiveSession;
import com.platform.creatorstudio.model.StudioLiveSession.LiveSessionStatus;
import com.platform.creatorstudio.repository.StudioLiveSessionRepository;
import com.platform.creatorstudio.repository.CreatorStudioRepository;
import com.platform.creatorstudio.repository.StudioMembershipRepository;
import com.platform.common.security.CurrentUserAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing studio live sessions.
 * Handles session creation, scheduling, starting, ending, and participant management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StudioLiveSessionService {

    private final StudioLiveSessionRepository sessionRepository;
    private final CreatorStudioRepository studioRepository;
    private final StudioMembershipRepository membershipRepository;
    private final CurrentUserAccessor currentUserAccessor;
    private final com.platform.creatorstudio.repository.StudioLiveSessionAttendeeRepository attendeeRepository;

    @Value("${creator-studio.registration.mock-enabled:false}")
    private boolean registrationMockEnabled;

    @Value("${creator-studio.payments.mock-enabled:false}")
    private boolean paymentsMockEnabled;

    /**
     * Creates a new live session.
     *
     * @param studioId The studio ID
     * @param title The session title
     * @param description The session description
     * @param sessionCategory The session category
     * @param topic The session topic
     * @param scheduledStartTime When the session is scheduled to start
     * @param isPublic Whether the session is open for public attendance
     * @param requiresRegistration Whether registration is required
     * @param maxAttendees Maximum number of attendees (0 = unlimited)
     * @param requiresPayment Whether payment is required for attendance
     * @param attendancePricePaise Price for attendance (in paise)
     * @param allowsCommenting Whether commenting is allowed during the session
     * @return The created live session
     */
    @PreAuthorize("@studioMembershipService.canManageContent(#studioId)")
    public StudioLiveSession createLiveSession(UUID studioId, String title, String description,
                                               String sessionCategory, String topic,
                                               Instant scheduledStartTime, boolean isPublic,
                                               boolean requiresRegistration, Integer maxAttendees,
                                               boolean requiresPayment, Integer attendancePricePaise,
                                               boolean allowsCommenting) {
        // Verify studio exists
        var studio = studioRepository.findById(studioId);
        if (studio.isEmpty() || !studio.get().isActiveAndNotDeleted()) {
            throw new IllegalArgumentException("Studio not found or inactive: " + studioId);
        }

        // Validate required fields
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Session title cannot be empty");
        }
        if (scheduledStartTime == null) {
            throw new IllegalArgumentException("Scheduled start time cannot be null");
        }
        if (scheduledStartTime.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Scheduled start time must be in the future");
        }

        StudioLiveSession session = new StudioLiveSession();
        session.setStudioId(studioId);
        session.setTitle(title.trim());
        session.setDescription(description != null ? description : "");
        session.setSessionCategory(sessionCategory != null ? sessionCategory : "");
        session.setTopic(topic != null ? topic : "");
        session.setScheduledStartTime(scheduledStartTime);
        session.setPublic(isPublic);
        session.setRequiresRegistration(requiresRegistration);
        session.setMaxAttendees(maxAttendees != null ? maxAttendees : 0);
        session.setRequiresPayment(requiresPayment);
        session.setAttendancePricePaise(attendancePricePaise != null ? attendancePricePaise : 0);
        session.setAllowsCommenting(allowsCommenting);
        session.setStatus(LiveSessionStatus.DRAFT);
        session.setCreatedAt(Instant.now());
        session.setUpdatedAt(Instant.now());

        StudioLiveSession savedSession = sessionRepository.save(session);
        log.info("Live session created: {} for studio {}", savedSession.getId(), studioId);
        return savedSession;
    }

    /**
     * Gets a live session by its ID.
     *
     * @param sessionId The session ID
     * @return The session if found, null otherwise
     */
    public StudioLiveSession getLiveSessionById(UUID sessionId) {
        return sessionRepository.findById(sessionId).orElse(null);
    }

    /**
     * Gets a live session by ID and verifies it belongs to a studio.
     *
     * @param sessionId The session ID
     * @param studioId The expected studio ID
     * @return The session if found and belongs to the studio, null otherwise
     */
    public StudioLiveSession getLiveSessionByIdAndStudio(UUID sessionId, UUID studioId) {
        return sessionRepository.findByIdAndStudioId(sessionId, studioId).orElse(null);
    }

    /** Public sessions can be viewed anonymously; private sessions require studio membership or admin access. */
    public boolean canViewSession(StudioLiveSession session) {
        if (session == null) return false;
        if (session.isPublic()) return true;
        var principal = currentUserAccessor.getCurrentUser();
        if (principal.isEmpty()) return false;
        UUID userId = principal.get().getUserId();
        UUID ownerId = studioRepository.findById(session.getStudioId())
                .map(com.platform.creatorstudio.model.CreatorStudio::getOwnerId).orElse(null);
        return currentUserAccessor.isAdmin()
                || (userId != null && (userId.equals(ownerId)
                    || membershipRepository.existsByStudioIdAndUserIdAndIsActiveTrue(session.getStudioId(), userId)));
    }

    /**
     * Gets all live sessions for a studio.
     *
     * @param studioId The studio ID
     * @return List of all live sessions for the studio
     */
    @PreAuthorize("@studioMembershipService.canViewContent(#studioId)")
    public List<StudioLiveSession> getStudioLiveSessions(UUID studioId) {
        return sessionRepository.findByStudioId(studioId);
    }

    /**
     * Gets upcoming live sessions for a studio.
     *
     * @param studioId The studio ID
     * @return List of upcoming live sessions for the studio
     */
    public List<StudioLiveSession> getUpcomingStudioLiveSessions(UUID studioId) {
        return sessionRepository.findByStudioIdAndStatus(studioId, LiveSessionStatus.SCHEDULED)
                .stream().filter(this::canViewSession).toList();
    }

    /**
     * Gets currently live sessions.
     *
     * @return List of all currently live sessions
     */
    public List<StudioLiveSession> getCurrentlyLiveSessions() {
        return sessionRepository.findByStatus(LiveSessionStatus.LIVE)
                .stream().filter(this::canViewSession).toList();
    }

    /**
     * Gets recently ended sessions (for replay availability).
     *
     * @param cutoffHours How many hours back to look for ended sessions
     * @return List of recently ended sessions
     */
    public List<StudioLiveSession> getRecentlyEndedSessions(int cutoffHours) {
        Instant cutoff = Instant.now().minusSeconds(cutoffHours * 3600L);
        return sessionRepository.findRecentlyEndedSessions(
                StudioLiveSession.LiveSessionStatus.ENDED,
                StudioLiveSession.LiveSessionStatus.COMPLETED,
                cutoff).stream().filter(this::canViewSession).toList();
    }

    /**
     * Schedules a live session (changes status from DRAFT to SCHEDULED).
     *
     * @param sessionId The session ID
     * @return The updated session
     */
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageContent(@studioLiveSessionService.getStudioIdBySessionId(#sessionId))")
    public StudioLiveSession scheduleLiveSession(UUID sessionId) {
        StudioLiveSession session = getLiveSessionById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Live session not found: " + sessionId);
        }

        if (session.getStatus() != LiveSessionStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft sessions can be scheduled");
        }

        if (session.getScheduledStartTime() == null) {
            throw new IllegalArgumentException("Session must have a scheduled start time");
        }

        session.setStatus(LiveSessionStatus.SCHEDULED);
        session.setUpdatedAt(Instant.now());

        StudioLiveSession updatedSession = sessionRepository.save(session);
        log.info("Live session scheduled: {} for studio {}", sessionId, session.getStudioId());
        return updatedSession;
    }

    /**
     * Starts a live session (changes status from SCHEDULED to LIVE).
     *
     * @param sessionId The session ID
     * @return The updated session
     */
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageContent(@studioLiveSessionService.getStudioIdBySessionId(#sessionId))")
    public StudioLiveSession startLiveSession(UUID sessionId) {
        StudioLiveSession session = getLiveSessionById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Live session not found: " + sessionId);
        }

        if (!session.canStart()) {
            throw new IllegalArgumentException("Session cannot be started at this time");
        }

        session.startLive();

        StudioLiveSession updatedSession = sessionRepository.save(session);
        log.info("Live session started: {} for studio {}", sessionId, session.getStudioId());
        return updatedSession;
    }

    /**
     * Ends a live session (changes status from LIVE to ENDED).
     *
     * @param sessionId The session ID
     * @return The updated session
     */
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageContent(@studioLiveSessionService.getStudioIdBySessionId(#sessionId))")
    public StudioLiveSession endLiveSession(UUID sessionId) {
        StudioLiveSession session = getLiveSessionById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Live session not found: " + sessionId);
        }

        if (!session.canEnd()) {
            throw new IllegalArgumentException("Session cannot be ended");
        }

        session.endLive();

        StudioLiveSession updatedSession = sessionRepository.save(session);
        log.info("Live session ended: {} for studio {}", sessionId, session.getStudioId());
        return updatedSession;
    }

    /**
     * Cancels a live session (changes status to CANCELLED).
     *
     * @param sessionId The session ID
     * @return The updated session
     */
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageContent(@studioLiveSessionService.getStudioIdBySessionId(#sessionId))")
    public StudioLiveSession cancelLiveSession(UUID sessionId) {
        StudioLiveSession session = getLiveSessionById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Live session not found: " + sessionId);
        }

        if (session.getStatus() == LiveSessionStatus.LIVE) {
            throw new IllegalArgumentException("Cannot cancel a live session; use endLiveSession instead");
        }

        session.cancel();
        session.setUpdatedAt(Instant.now());

        StudioLiveSession updatedSession = sessionRepository.save(session);
        log.info("Live session cancelled: {} for studio {}", sessionId, session.getStudioId());
        return updatedSession;
    }

    /**
     * Marks a live session as completed (changes status from ENDED to COMPLETED).
     *
     * @param sessionId The session ID
     * @return The updated session
     */
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageContent(@studioLiveSessionService.getStudioIdBySessionId(#sessionId))")
    public StudioLiveSession completeLiveSession(UUID sessionId) {
        StudioLiveSession session = getLiveSessionById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Live session not found: " + sessionId);
        }

        if (session.getStatus() != LiveSessionStatus.ENDED) {
            throw new IllegalArgumentException("Only ended sessions can be marked as completed");
        }

        session.markAsCompleted();
        session.setUpdatedAt(Instant.now());

        StudioLiveSession updatedSession = sessionRepository.save(session);
        log.info("Live session completed: {} for studio {}", sessionId, session.getStudioId());
        return updatedSession;
    }

    /**
     * Updates live session information.
     *
     * @param sessionId The session ID
     * @param title The new title
     * @param description The new description
     * @param sessionCategory The new session category
     * @param topic The new topic
     * @param scheduledStartTime The new scheduled start time
     * @param isPublic The new public status
     * @param requiresRegistration The new registration requirement
     * @param maxAttendees The new maximum attendees
     * @param requiresPayment The new payment requirement
     * @param attendancePricePaise The new attendance price
     * @param allowsCommenting The new commenting allowance
     * @return The updated session
     */
    @PreAuthorize("hasRole('ADMIN') or @studioMembershipService.canManageContent(@studioLiveSessionService.getStudioIdBySessionId(#sessionId))")
    public StudioLiveSession updateLiveSession(UUID sessionId, String title, String description,
                                               String sessionCategory, String topic,
                                               Instant scheduledStartTime, boolean isPublic,
                                               boolean requiresRegistration, Integer maxAttendees,
                                               boolean requiresPayment, Integer attendancePricePaise,
                                               boolean allowsCommenting) {
        StudioLiveSession session = getLiveSessionById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Live session not found: " + sessionId);
        }

        if (session.getStatus() == LiveSessionStatus.LIVE) {
            throw new IllegalArgumentException("Cannot modify a live session");
        }

        if (title != null && !title.trim().isEmpty()) {
            session.setTitle(title.trim());
        }
        if (description != null) {
            session.setDescription(description);
        }
        if (sessionCategory != null) {
            session.setSessionCategory(sessionCategory);
        }
        if (topic != null) {
            session.setTopic(topic);
        }
        if (scheduledStartTime != null) {
            if (scheduledStartTime.isBefore(Instant.now())) {
                throw new IllegalArgumentException("Scheduled start time must be in the future");
            }
            session.setScheduledStartTime(scheduledStartTime);
        }
        session.setPublic(isPublic);
        session.setRequiresRegistration(requiresRegistration);
        if (maxAttendees != null) {
            session.setMaxAttendees(maxAttendees);
        }
        session.setRequiresPayment(requiresPayment);
        if (attendancePricePaise != null) {
            session.setAttendancePricePaise(attendancePricePaise);
        }
        session.setAllowsCommenting(allowsCommenting);
        session.setUpdatedAt(Instant.now());

        StudioLiveSession updatedSession = sessionRepository.save(session);
        log.info("Live session updated: {} for studio {}", sessionId, session.getStudioId());
        return updatedSession;
    }

    /**
     * Joins a user to a live session as an attendee.
     *
     * @param sessionId The session ID
     * @param userId The user ID joining as attendee
     * @return true if successfully joined, false otherwise
     */
    public boolean joinLiveSession(UUID sessionId, UUID userId) {
        if (userId == null) return false;
        StudioLiveSession session = sessionRepository.findByIdForUpdate(sessionId).orElse(null);
        if (session == null) {
            return false;
        }

        // A user may join only a public session or a private session they can access.
        if (!canViewSession(session)) return false;

        // Check if session is live or scheduled to start soon
        if (!session.isUpcoming() && !session.isOngoing()) {
            return false;
        }

        // Check if user has permission to attend
        if (!canAttendSession(session, userId)) {
            return false;
        }

        // The unique database constraint makes repeat joins idempotently rejectable.
        if (attendeeRepository.existsBySessionIdAndUserId(sessionId, userId)) return false;

        // Enforce capacity before recording attendance.
        if (session.getMaxAttendees() != null && session.getMaxAttendees() > 0 &&
            session.getCurrentAttendeeCount() >= session.getMaxAttendees()) {
            return false;
        }

        var attendee = new com.platform.creatorstudio.model.StudioLiveSessionAttendee();
        attendee.setSessionId(sessionId);
        attendee.setUserId(userId);
        attendee.setJoinedAt(Instant.now());
        attendeeRepository.save(attendee);
        session.setCurrentAttendeeCount(session.getCurrentAttendeeCount() + 1);
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);

        log.info("User {} joined live session {}", userId, sessionId);
        return true;
    }

    /**
     * Leaves a live session as an attendee.
     *
     * @param sessionId The session ID
     * @param userId The user ID leaving as attendee
     * @return true if successfully left, false otherwise
     */
    public boolean leaveLiveSession(UUID sessionId, UUID userId) {
        if (userId == null) return false;
        StudioLiveSession session = sessionRepository.findByIdForUpdate(sessionId).orElse(null);
        if (session == null) return false;
        var attendee = attendeeRepository.findBySessionIdAndUserId(sessionId, userId);
        if (attendee.isEmpty()) return false;
        attendeeRepository.delete(attendee.get());

        // Decrement only after confirming this user had an active attendance record.
        int newCount = Math.max(0, session.getCurrentAttendeeCount() - 1);
        session.setCurrentAttendeeCount(newCount);
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);

        log.info("User {} left live session {}", userId, sessionId);
        return true;
    }

    /**
     * Checks if a user can attend a live session.
     *
     * @param session The live session
     * @param userId The user ID
     * @return true if the user can attend, false otherwise
     */
    public boolean canAttendSession(StudioLiveSession session, UUID userId) {
        // Check if session is public
        if (!session.isPublic()) {
            // For private sessions, user must be invited or a member
            // In a full implementation, we'd check invitations and memberships
            // For dev, we'll allow studio members to attend private sessions
            boolean isMember = membershipRepository.existsByStudioIdAndUserIdAndIsActiveTrue(
                    session.getStudioId(), userId);
            if (!isMember) {
                return false;
            }
        }

        // Check if session requires registration
        // Registration/payment providers are not integrated yet. Only explicit dev mocks may bypass these gates.
        if (session.requiresRegistration() && !registrationMockEnabled) return false;
        if (session.requiresPayment() && !paymentsMockEnabled) return false;

        return true;
    }

    /**
     * Gets the current user's ID from the authentication context.
     *
     * @return The current user's ID
     */
    protected UUID getCurrentUserId() {
        return currentUserAccessor.getCurrentUserId();
    }

    /** Resolves the parent studio for authorization of session-ID operations. */
    public UUID getStudioIdBySessionId(UUID sessionId) {
        if (sessionId == null) return null;
        StudioLiveSession session = getLiveSessionById(sessionId);
        return session == null ? null : session.getStudioId();
    }
}