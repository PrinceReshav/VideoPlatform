package com.platform.common.pagination;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CursorPaginationUtils {

    public static <T> Specification<T> withCursor(String cursor, String sortField,
                                                   CursorPagination.SortDirection direction) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        // Parse cursor - format: "value:tiebreaker" (e.g., "2024-01-01T00:00:00Z:uuid")
        String[] parts = cursor.split(":", 2);
        String cursorValue = parts[0];
        String tiebreaker = parts.length > 1 ? parts[1] : null;

        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            // Main sort field predicate
            if (direction == CursorPagination.SortDirection.DESC) {
                predicates.add(criteriaBuilder.lessThan(root.get(sortField), cursorValue));
            } else {
                predicates.add(criteriaBuilder.greaterThan(root.get(sortField), cursorValue));
            }

            // Tiebreaker for stable ordering
            if (tiebreaker != null) {
                String tiebreakerField = "id"; // Default to ID as tiebreaker
                if (direction == CursorPagination.SortDirection.DESC) {
                    predicates.add(criteriaBuilder.or(
                            criteriaBuilder.lessThan(root.get(sortField), cursorValue),
                            criteriaBuilder.and(
                                    criteriaBuilder.equal(root.get(sortField), cursorValue),
                                    criteriaBuilder.lessThan(root.get(tiebreakerField), tiebreaker)
                            )
                    ));
                } else {
                    predicates.add(criteriaBuilder.or(
                            criteriaBuilder.greaterThan(root.get(sortField), cursorValue),
                            criteriaBuilder.and(
                                    criteriaBuilder.equal(root.get(sortField), cursorValue),
                                    criteriaBuilder.greaterThan(root.get(tiebreakerField), tiebreaker)
                            )
                    ));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    public static <T> Order buildOrder(CriteriaBuilder cb, Root<T> root,
                                        String sortField, CursorPagination.SortDirection direction) {
        return direction == CursorPagination.SortDirection.DESC
                ? cb.desc(root.get(sortField))
                : cb.asc(root.get(sortField));
    }

    public static String encodeCursor(Object sortValue, Object tiebreaker) {
        return sortValue + ":" + tiebreaker;
    }

    public static String[] decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return new String[0];
        }
        return cursor.split(":", 2);
    }
}