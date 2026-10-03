package com.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CursorPageResponse<T> {
    private List<T> content;
    private String nextCursor;
    private boolean hasNext;
    private int pageSize;

    public static <T> CursorPageResponse<T> of(List<T> content, String nextCursor, int pageSize) {
        boolean hasNext = nextCursor != null && !content.isEmpty();
        return CursorPageResponse.<T>builder()
                .content(content)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .pageSize(pageSize)
                .build();
    }
}