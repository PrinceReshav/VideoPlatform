package com.platform.creatorstudio.model;

/**
 * Enumeration of studio categories with their capabilities.
 * Each category defines what types of content and features are supported.
 */
public enum StudioCategory {
    DANCE_CHOREOGRAPHY(true, true, false, false, false, true, false),
    MUSIC_COMPOSITION(true, true, false, true, false, true, false),
    BICYCLE_ACTION_STUNTS(true, true, false, false, false, true, true),
    PAINTING_VISUAL_ARTS(true, true, true, false, false, true, false),
    VINTAGE_COLLECTIBLES(true, true, true, true, false, true, true),
    COMIC_WRITERS_ILLUSTRATORS(false, true, true, true, false, true, false),
    POETRY_LITERATURE(true, true, true, true, false, true, false),
    RELIGIOUS_SCRIPTURAL_DISCUSSIONS(false, true, false, false, false, true, false),
    SHORT_FILMS_MOVIE_MAKING(true, true, false, false, true, true, false),
    NEWS_ANCHORING_PRESENTATION(true, true, false, true, true, true, false),
    BRAND_PROMOTION_ADVERTISING(true, true, true, false, false, true, true),
    EDUCATION_ONLINE_TEACHING(true, true, true, true, true, true, true),
    ACTING_THEATRE(true, true, false, false, false, true, false),
    PHOTOGRAPHY_CINEMATOGRAPHY(true, true, true, false, false, true, false),
    COMEDY_STANDUP(true, true, false, false, false, true, false),
    FITNESS_YOGA(true, true, false, false, false, true, true),
    COOKING_CULINARY_ARTS(true, true, true, false, false, true, true),
    CRAFTSMANSHIP_DIY(true, true, true, false, false, true, true),
    STORYTELLING_AUDIOBOOKS(false, true, true, true, false, true, false),
    DEBATE_PUBLIC_SPEAKING(false, true, false, false, false, true, false);

    /** Whether this category supports pre-uploaded video */
    private final boolean supportsVideo;
    /** Whether this category supports live sessions */
    private final boolean supportsLive;
    /** Whether this category supports images */
    private final boolean supportsImages;
    /** Whether this category supports audio */
    private final boolean supportsAudio;
    /** Whether this category supports documents (PDF, eBooks, etc.) */
    private final boolean supportsDocuments;
    /** Whether this category supports team studios */
    private final boolean supportsTeams;
    /** Whether this category supports paid participation/content */
    private final boolean supportsPaid;

    StudioCategory(boolean supportsVideo, boolean supportsLive, boolean supportsImages,
                   boolean supportsAudio, boolean supportsDocuments, boolean supportsTeams,
                   boolean supportsPaid) {
        this.supportsVideo = supportsVideo;
        this.supportsLive = supportsLive;
        this.supportsImages = supportsImages;
        this.supportsAudio = supportsAudio;
        this.supportsDocuments = supportsDocuments;
        this.supportsTeams = supportsTeams;
        this.supportsPaid = supportsPaid;
    }

    public boolean supportsVideo() {
        return supportsVideo;
    }

    public boolean supportsLive() {
        return supportsLive;
    }

    public boolean supportsImages() {
        return supportsImages;
    }

    public boolean supportsAudio() {
        return supportsAudio;
    }

    public boolean supportsDocuments() {
        return supportsDocuments;
    }

    public boolean supportsTeams() {
        return supportsTeams;
    }

    public boolean supportsPaid() {
        return supportsPaid;
    }
}