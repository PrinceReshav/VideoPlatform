package com.platform.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestResponse {
    private List<String> suggestions;
    private List<ChannelSuggestion> channels;
    private List<TagSuggestion> tags;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ChannelSuggestion {
        private String channelId;
        private String channelName;
        private String channelSlug;
        private String avatarUrl;
        private long subscriberCount;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TagSuggestion {
        private String tag;
        private long count;
    }
}
