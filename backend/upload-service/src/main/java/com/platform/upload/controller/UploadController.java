package com.platform.upload.controller;

import com.platform.common.dto.PageResponse;
import com.platform.upload.dto.CompleteSessionRequest;
import com.platform.upload.dto.CreateSessionRequest;
import com.platform.upload.dto.SessionStatusResponse;
import com.platform.upload.service.UploadSessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadSessionService uploadSessionService;

    @PostMapping("/sessions")
    public ResponseEntity<SessionStatusResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        SessionStatusResponse response = uploadSessionService.createSession(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/sessions/{sessionId}/chunks/{partNumber}")
    public ResponseEntity<SessionStatusResponse> uploadChunk(
            @PathVariable UUID sessionId,
            @PathVariable @Min(1) int partNumber,
            @RequestPart("file") MultipartFile file) {
        SessionStatusResponse response = uploadSessionService.uploadChunk(sessionId, partNumber, file);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/{sessionId}/complete")
    public ResponseEntity<SessionStatusResponse> completeSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody CompleteSessionRequest request) {
        SessionStatusResponse response = uploadSessionService.completeSession(sessionId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> cancelSession(@PathVariable UUID sessionId) {
        uploadSessionService.cancelSession(sessionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<SessionStatusResponse> getSessionStatus(@PathVariable UUID sessionId) {
        SessionStatusResponse response = uploadSessionService.getSessionStatus(sessionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions/{sessionId}/parts")
    public ResponseEntity<List<Integer>> getUploadedParts(@PathVariable UUID sessionId) {
        List<Integer> parts = uploadSessionService.getUploadedPartNumbers(sessionId);
        return ResponseEntity.ok(parts);
    }

    @GetMapping("/sessions")
    public ResponseEntity<PageResponse<SessionStatusResponse>> getMySessions(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(uploadSessionService.getMySessions(page, size));
    }
}