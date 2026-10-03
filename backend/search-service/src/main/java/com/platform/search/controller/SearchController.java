package com.platform.search.controller;

import com.platform.common.dto.CursorPageResponse;
import com.platform.search.dto.SearchRequest;
import com.platform.search.dto.SearchResponse;
import com.platform.search.dto.SuggestResponse;
import com.platform.search.service.VideoSearchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final VideoSearchService videoSearchService;

    @PostMapping("/videos")
    public ResponseEntity<SearchResponse> searchVideos(@Valid @RequestBody SearchRequest request) {
        SearchResponse response = videoSearchService.search(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/videos")
    public ResponseEntity<SearchResponse> searchVideosGet(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID genreId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID creatorId,
            @RequestParam(required = false) UUID channelId,
            @RequestParam(required = false) UUID languageId,
            @RequestParam(required = false) Integer durationMin,
            @RequestParam(required = false) Integer durationMax,
            @RequestParam(defaultValue = "relevance") String sort,
            @RequestParam(required = false) String publishedAfter,
            @RequestParam(required = false) String publishedBefore,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String cursor) {

        SearchRequest request = SearchRequest.builder()
                .q(q)
                .genreId(genreId)
                .categoryId(categoryId)
                .creatorId(creatorId)
                .channelId(channelId)
                .languageId(languageId)
                .isPublished(true)
                .visibility(java.util.List.of("PUBLIC", "UNLISTED"))
                .processingState("READY")
                .durationMin(durationMin)
                .durationMax(durationMax)
                .sort(sort)
                .publishedAfter(publishedAfter)
                .publishedBefore(publishedBefore)
                .size(size)
                .cursor(cursor)
                .build();

        SearchResponse response = videoSearchService.search(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/suggest")
    public ResponseEntity<SuggestResponse> suggest(@RequestParam @NotBlank @jakarta.validation.constraints.Size(min = 2, max = 50) String q) {
        SuggestResponse response = videoSearchService.suggest(q);
        return ResponseEntity.ok(response);
    }
}