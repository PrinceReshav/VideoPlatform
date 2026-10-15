package com.platform.engagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private UUID id;
    private UUID videoId;
    private UUID userId;
    private UUID channelId;
    private String username;
    private String userAvatarUrl;
    private UUID parentId;
    private String content;
    private boolean hidden;
    private int likeCount;
    private int replyCount;
    private boolean pinned;
    /** Timestamp in video (in seconds) when this comment was made */
    private Integer timestampSeconds;
    private Instant createdAt;
    private Instant updatedAt;
    private List<CommentResponse> replies;
    private boolean isLiked;
    private boolean isOwn;
}