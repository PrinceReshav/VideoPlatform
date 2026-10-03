package com.platform.video.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVideoRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200, message = "Title must be 1-200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @Size(max = 1000, message = "Tags must not exceed 1000 characters")
    private String tags;

    @NotNull(message = "Category is required")
    private UUID categoryId;

    private UUID genreId;

    private UUID languageId;

    @NotNull(message = "Visibility is required")
    private String visibility = "PRIVATE";

    private boolean allowComments = true;

    private boolean allowEmbedding = true;

    private boolean ageRestricted = false;

    private UUID uploadSessionId;
}