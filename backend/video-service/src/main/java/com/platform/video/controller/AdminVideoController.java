package com.platform.video.controller;

import com.platform.common.dto.PageResponse;
import com.platform.common.entity.Video;
import com.platform.common.security.CurrentUserAccessor;
import com.platform.video.dto.VideoResponse;
import com.platform.video.repository.VideoRepository;
import com.platform.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/videos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVideoController {
    private final VideoRepository repository;
    private final VideoService videoService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping
    public ResponseEntity<PageResponse<VideoResponse>> getVideos(
            @RequestParam(required=false) String search,
            @RequestParam(required=false) String status,
            @RequestParam(required=false) String visibility,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {
        var result = repository.findAll(PageRequest.of(page, size));
        var content = result.getContent().stream().filter(v -> {
            boolean q = search == null || search.isBlank() || v.getTitle().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
            boolean st = status == null || status.isBlank() || v.getProcessingState().name().equalsIgnoreCase(status) || v.getPublicationState().name().equalsIgnoreCase(status) || (status.equalsIgnoreCase("blocked") && v.isBlocked());
            boolean vis = visibility == null || visibility.isBlank() || v.getVisibility().name().equalsIgnoreCase(visibility);
            return q && st && vis;
        }).map(v -> videoService.mapAdminResponse(v)).toList();
        return ResponseEntity.ok(PageResponse.of(content, page, size, result.getTotalElements()));
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<Void> block(@PathVariable UUID id, @RequestBody Map<String,String> body) {
        Video video = repository.findById(id).orElseThrow();
        video.block(currentUserAccessor.getCurrentUserId(), body.getOrDefault("reason", "Blocked by administrator"));
        repository.save(video);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/unblock")
    public ResponseEntity<Void> unblock(@PathVariable UUID id) {
        Video video = repository.findById(id).orElseThrow();
        video.unblock(); repository.save(video); return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        videoService.deleteVideo(id); return ResponseEntity.noContent().build();
    }
}
