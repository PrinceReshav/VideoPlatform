package com.platform.common.dto;

public final class ApiErrorCodes {

    // Authentication & Authorization
    public static final String INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS";
    public static final String ACCOUNT_LOCKED = "AUTH_ACCOUNT_LOCKED";
    public static final String ACCOUNT_DISABLED = "AUTH_ACCOUNT_DISABLED";
    public static final String EMAIL_NOT_VERIFIED = "AUTH_EMAIL_NOT_VERIFIED";
    public static final String TOKEN_EXPIRED = "AUTH_TOKEN_EXPIRED";
    public static final String TOKEN_INVALID = "AUTH_TOKEN_INVALID";
    public static final String TOKEN_MALFORMED = "AUTH_TOKEN_MALFORMED";
    public static final String REFRESH_TOKEN_EXPIRED = "AUTH_REFRESH_TOKEN_EXPIRED";
    public static final String REFRESH_TOKEN_REVOKED = "AUTH_REFRESH_TOKEN_REVOKED";
    public static final String REFRESH_TOKEN_ROTATION_FAILED = "AUTH_REFRESH_TOKEN_ROTATION_FAILED";
    public static final String ACCESS_DENIED = "AUTH_ACCESS_DENIED";
    public static final String INSUFFICIENT_PERMISSIONS = "AUTH_INSUFFICIENT_PERMISSIONS";
    public static final String ROLE_REQUIRED = "AUTH_ROLE_REQUIRED";

    // Validation
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String INVALID_EMAIL = "VALIDATION_INVALID_EMAIL";
    public static final String INVALID_PASSWORD = "VALIDATION_INVALID_PASSWORD";
    public static final String INVALID_USERNAME = "VALIDATION_INVALID_USERNAME";
    public static final String FIELD_REQUIRED = "VALIDATION_FIELD_REQUIRED";
    public static final String FIELD_TOO_LONG = "VALIDATION_FIELD_TOO_LONG";
    public static final String FIELD_TOO_SHORT = "VALIDATION_FIELD_TOO_SHORT";
    public static final String INVALID_FORMAT = "VALIDATION_INVALID_FORMAT";

    // Resource Errors
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String RESOURCE_ALREADY_EXISTS = "RESOURCE_ALREADY_EXISTS";
    public static final String RESOURCE_CONFLICT = "RESOURCE_CONFLICT";
    public static final String RESOURCE_LOCKED = "RESOURCE_LOCKED";

    // User/Creator
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";
    public static final String EMAIL_ALREADY_REGISTERED = "USER_EMAIL_ALREADY_REGISTERED";
    public static final String USERNAME_ALREADY_TAKEN = "USER_USERNAME_ALREADY_TAKEN";
    public static final String CREATOR_PROFILE_NOT_FOUND = "CREATOR_PROFILE_NOT_FOUND";
    public static final String CHANNEL_NOT_FOUND = "CHANNEL_NOT_FOUND";
    public static final String CHANNEL_NAME_TAKEN = "CHANNEL_NAME_TAKEN";
    public static final String CANNOT_BECOME_CREATOR = "CREATOR_CANNOT_BECOME";

    // Video
    public static final String VIDEO_NOT_FOUND = "VIDEO_NOT_FOUND";
    public static final String VIDEO_PROCESSING = "VIDEO_PROCESSING";
    public static final String VIDEO_PROCESSING_FAILED = "VIDEO_PROCESSING_FAILED";
    public static final String VIDEO_NOT_PUBLISHED = "VIDEO_NOT_PUBLISHED";
    public static final String VIDEO_PRIVATE = "VIDEO_PRIVATE";
    public static final String VIDEO_BLOCKED = "VIDEO_BLOCKED";
    public static final String VIDEO_DELETED = "VIDEO_DELETED";
    public static final String VIDEO_UPLOAD_FAILED = "VIDEO_UPLOAD_FAILED";

    // Upload
    public static final String UPLOAD_SESSION_NOT_FOUND = "UPLOAD_SESSION_NOT_FOUND";
    public static final String UPLOAD_SESSION_EXPIRED = "UPLOAD_SESSION_EXPIRED";
    public static final String UPLOAD_CHUNK_MISMATCH = "UPLOAD_CHUNK_MISMATCH";
    public static final String UPLOAD_CHECKSUM_MISMATCH = "UPLOAD_CHECKSUM_MISMATCH";
    public static final String UPLOAD_FILE_TOO_LARGE = "UPLOAD_FILE_TOO_LARGE";
    public static final String UPLOAD_INVALID_MIME_TYPE = "UPLOAD_INVALID_MIME_TYPE";
    public static final String UPLOAD_INCOMPLETE = "UPLOAD_INCOMPLETE";

    // Engagement
    public static final String ALREADY_LIKED = "ENGAGEMENT_ALREADY_LIKED";
    public static final String NOT_LIKED = "ENGAGEMENT_NOT_LIKED";
    public static final String COMMENT_NOT_FOUND = "COMMENT_NOT_FOUND";
    public static final String COMMENT_EMPTY = "COMMENT_EMPTY";
    public static final String COMMENT_TOO_LONG = "COMMENT_TOO_LONG";
    public static final String CANNOT_MODERATE_OWN_COMMENT = "ENGAGEMENT_CANNOT_MODERATE_OWN";

    // Subscription
    public static final String ALREADY_SUBSCRIBED = "SUBSCRIPTION_ALREADY_EXISTS";
    public static final String NOT_SUBSCRIBED = "SUBSCRIPTION_NOT_EXISTS";
    public static final String CANNOT_SUBSCRIBE_SELF = "SUBSCRIPTION_CANNOT_SELF";

    // Playlist
    public static final String PLAYLIST_NOT_FOUND = "PLAYLIST_NOT_FOUND";
    public static final String PLAYLIST_VIDEO_NOT_FOUND = "PLAYLIST_VIDEO_NOT_FOUND";
    public static final String VIDEO_ALREADY_IN_PLAYLIST = "PLAYLIST_VIDEO_ALREADY_EXISTS";
    public static final String CANNOT_MODIFY_PLAYLIST = "PLAYLIST_CANNOT_MODIFY";

    // Search
    public static final String SEARCH_QUERY_INVALID = "SEARCH_QUERY_INVALID";
    public static final String SEARCH_INDEX_UNAVAILABLE = "SEARCH_INDEX_UNAVAILABLE";

    // Transcoding
    public static final String TRANSCODING_FAILED = "TRANSCODING_FAILED";
    public static final String TRANSCODING_TIMEOUT = "TRANSCODING_TIMEOUT";
    public static final String FFMPEG_NOT_AVAILABLE = "TRANSCODING_FFMPEG_UNAVAILABLE";

    // Storage
    public static final String STORAGE_UNAVAILABLE = "STORAGE_UNAVAILABLE";
    public static final String STORAGE_UPLOAD_FAILED = "STORAGE_UPLOAD_FAILED";
    public static final String STORAGE_DOWNLOAD_FAILED = "STORAGE_DOWNLOAD_FAILED";
    public static final String PRESIGNED_URL_EXPIRED = "STORAGE_PRESIGNED_URL_EXPIRED";

    // Rate Limiting
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String TOO_MANY_REQUESTS = "TOO_MANY_REQUESTS";

    // Generic
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";

    private ApiErrorCodes() {}
}