package com.platform.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String icon;
    private String color;
    private UUID parentId;
    private String parentName;
    private int sortOrder;
    private boolean active;
    private boolean system;
    private List<CategoryResponse> children;
}