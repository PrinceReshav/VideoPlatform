package com.platform.search.dto;

import com.platform.common.dto.CursorPageResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    private List<VideoSearchResult> results;
    private String nextCursor;
    private boolean hasNext;
    private long totalHits;
    private long tookMs;
}
