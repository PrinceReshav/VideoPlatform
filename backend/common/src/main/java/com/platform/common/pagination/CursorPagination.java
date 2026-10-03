package com.platform.common.pagination;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPagination {
    private String cursor;
    private int limit;
    private String sortField;
    private SortDirection sortDirection;

    public enum SortDirection {
        ASC, DESC
    }

    public static CursorPagination of(String cursor, int limit) {
        return CursorPagination.builder()
                .cursor(cursor)
                .limit(Math.min(limit, 100))
                .sortField("createdAt")
                .sortDirection(SortDirection.DESC)
                .build();
    }

    public static CursorPagination of(String cursor, int limit, String sortField, SortDirection sortDirection) {
        return CursorPagination.builder()
                .cursor(cursor)
                .limit(Math.min(limit, 100))
                .sortField(sortField)
                .sortDirection(sortDirection)
                .build();
    }
}