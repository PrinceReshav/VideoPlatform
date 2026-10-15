package com.platform.common.service;

import com.platform.common.entity.VideoShareLink;
import com.platform.common.repository.VideoShareLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Service for handling video share links
 * Feature 13: Privacy & Sharing Controls
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoShareLinkService {

    private final VideoShareLinkRepository videoShareLinkRepository;

    /**
     * Create a new video share link.
     *
     * If no token is supplied, a cryptographically strong URL-safe
     * random token is generated.
     */
    @Transactional
    public VideoShareLink createVideoShareLink(VideoShareLink shareLink) {

        if (shareLink == null) {
            throw new IllegalArgumentException("Share link must not be null");
        }

        if (shareLink.getShareToken() == null
                || shareLink.getShareToken().isBlank()) {

            shareLink.setShareToken(generateUniqueShareToken());
        }

        if (shareLink.getCurrentUses() == null) {
            shareLink.setCurrentUses(0);
        }

        shareLink.setCreatedAt(Instant.now());

        VideoShareLink savedShareLink =
                videoShareLinkRepository.save(shareLink);

        log.info(
                "Created video share link {} for video {} by user {}",
                savedShareLink.getId(),
                savedShareLink.getVideoId(),
                savedShareLink.getCreatedBy()
        );

        return savedShareLink;
    }

    /**
     * Get a video share link by ID.
     */
    public VideoShareLink getVideoShareLinkById(UUID id) {

        if (id == null) {
            throw new IllegalArgumentException("Share link ID must not be null");
        }

        return videoShareLinkRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Video share link not found with ID: " + id
                        )
                );
    }

    /**
     * Get a video share link by share token.
     *
     * The repository currently returns VideoShareLink directly,
     * therefore null must be checked instead of using orElseThrow().
     */
    public VideoShareLink getVideoShareLinkByShareToken(String shareToken) {

        if (shareToken == null || shareToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Share token must not be null or blank"
            );
        }

        VideoShareLink shareLink =
                videoShareLinkRepository.findByShareToken(shareToken);

        if (shareLink == null) {
            throw new IllegalArgumentException(
                    "Video share link not found with token: " + shareToken
            );
        }

        return shareLink;
    }

    /**
     * Get all share links for a video.
     */
    public List<VideoShareLink> getVideoShareLinksByVideoId(UUID videoId) {

        if (videoId == null) {
            throw new IllegalArgumentException(
                    "Video ID must not be null"
            );
        }

        return videoShareLinkRepository.findByVideoId(videoId);
    }

    /**
     * Get all share links created by a user.
     */
    public List<VideoShareLink> getVideoShareLinksByCreatedBy(UUID createdBy) {

        if (createdBy == null) {
            throw new IllegalArgumentException(
                    "Creator ID must not be null"
            );
        }

        return videoShareLinkRepository.findByCreatedBy(createdBy);
    }

    /**
     * Increment the usage count for a share link.
     */
    @Transactional
    public void incrementUsage(UUID id) {

        VideoShareLink shareLink = getVideoShareLinkById(id);

        int currentUses =
                shareLink.getCurrentUses() == null
                        ? 0
                        : shareLink.getCurrentUses();

        shareLink.setCurrentUses(currentUses + 1);
        shareLink.setLastUsedAt(Instant.now());

        videoShareLinkRepository.save(shareLink);

        log.info(
                "Incremented usage for share link {} to {}",
                id,
                shareLink.getCurrentUses()
        );
    }

    /**
     * Update an existing video share link.
     *
     * Video ID, creator ID and creation timestamp are intentionally
     * treated as immutable once the share link has been created.
     */
    @Transactional
    public VideoShareLink updateVideoShareLink(
            UUID id,
            VideoShareLink shareLinkDetails) {

        if (shareLinkDetails == null) {
            throw new IllegalArgumentException(
                    "Share link details must not be null"
            );
        }

        VideoShareLink shareLink = getVideoShareLinkById(id);

        shareLink.setDescription(
                shareLinkDetails.getDescription()
        );

        shareLink.setMaxUses(
                shareLinkDetails.getMaxUses()
        );

        shareLink.setPasswordProtected(
                shareLinkDetails.isPasswordProtected()
        );

        shareLink.setPasswordHash(
                shareLinkDetails.getPasswordHash()
        );

        shareLink.setExpiresAt(
                shareLinkDetails.getExpiresAt()
        );

        /*
         * Do NOT call setUpdatedAt().
         *
         * VideoShareLink does not currently contain an updatedAt field.
         */

        VideoShareLink updatedShareLink =
                videoShareLinkRepository.save(shareLink);

        log.info(
                "Updated video share link {}",
                updatedShareLink.getId()
        );

        return updatedShareLink;
    }

    /**
     * Delete a video share link.
     *
     * VideoShareLink uses @SQLDelete, so repository.delete()
     * performs a soft delete by setting deleted = true.
     */
    @Transactional
    public void deleteVideoShareLink(UUID id) {

        VideoShareLink shareLink = getVideoShareLinkById(id);

        videoShareLinkRepository.delete(shareLink);

        log.info(
                "Deleted video share link {}",
                id
        );
    }

    /**
     * Get video share links with filtering.
     *
     * The repository returns a Page, while this service method
     * intentionally exposes the existing List-based service contract.
     */
    public List<VideoShareLink> getVideoShareLinksByCriteria(
            UUID videoId,
            UUID createdBy,
            String shareToken,
            Instant startTime,
            Instant endTime) {

        return videoShareLinkRepository.findByCriteria(
                        videoId,
                        createdBy,
                        shareToken,
                        startTime,
                        endTime,
                        org.springframework.data.domain.PageRequest.of(
                                0,
                                Integer.MAX_VALUE
                        )
                )
                .getContent();
    }

    /**
     * Generate a unique share token.
     *
     * 22 URL-safe characters provide a very large token space.
     * A repository lookup is still performed to protect against
     * the extremely unlikely event of a collision.
     */
    private String generateUniqueShareToken() {

        RandomGenerator random = RandomGenerator.getDefault();

        final String chars =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                        + "abcdefghijklmnopqrstuvwxyz"
                        + "0123456789-_";

        for (int attempt = 0; attempt < 10; attempt++) {

            StringBuilder token = new StringBuilder(22);

            for (int i = 0; i < 22; i++) {
                int index = random.nextInt(chars.length());
                token.append(chars.charAt(index));
            }

            String generatedToken = token.toString();

            if (videoShareLinkRepository.findByShareToken(generatedToken)
                    == null) {

                return generatedToken;
            }
        }

        throw new IllegalStateException(
                "Unable to generate a unique video share token"
        );
    }

    /**
     * Generate HTML embed code for a video share link.
     *
     * NOTE:
     * The application URL should eventually come from configuration
     * rather than being hard-coded.
     */
    public String generateEmbedCode(
            VideoShareLink shareLink,
            int width,
            int height) {

        if (shareLink == null) {
            throw new IllegalArgumentException(
                    "Share link must not be null"
            );
        }

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Width and height must be greater than zero"
            );
        }

        String shareUrl =
                String.format(
                        "https://example.com/watch/%s",
                        shareLink.getShareToken()
                );

        return String.format(
                "<iframe width=\"%d\" height=\"%d\" src=\"%s\" "
                        + "frameborder=\"0\" allowfullscreen></iframe>",
                width,
                height,
                shareUrl
        );
    }

    /**
     * Generate QR code data for a video share link.
     *
     * IMPORTANT:
     * This remains a placeholder until a QR library such as ZXing
     * is intentionally added to the project.
     */
    public String generateQrCode(VideoShareLink shareLink) {

        if (shareLink == null) {
            throw new IllegalArgumentException(
                    "Share link must not be null"
            );
        }

        return String.format(
                "data:image/png;base64,QR_CODE_PLACEHOLDER_FOR_%s",
                shareLink.getShareToken()
        );
    }
}
