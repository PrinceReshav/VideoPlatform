package com.platform.common.controller;

import com.platform.common.dto.ApiResponse;
import com.platform.common.service.CopyrightDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for copyright detection configuration and status (Feature 4: Copyright Detection & Claims System).
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/copyright-detection")
public class CopyrightDetectionController {

    private final CopyrightDetectionService copyrightDetectionService;

    /**
     * Get copyright detection status and settings.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCopyrightDetectionStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", copyrightDetectionService.isDetectionEnabled());
        status.put("sensitivity", copyrightDetectionService.getDetectionSensitivity());
        status.put("blockOnDetection", copyrightDetectionService.isBlockOnDetection());

        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * Enable or disable copyright detection.
     */
    @PostMapping("/enabled/{enabled}")
    public ResponseEntity<ApiResponse<Void>> setDetectionEnabled(
            @PathVariable boolean enabled) {
        copyrightDetectionService.setDetectionEnabled(enabled);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Set copyright detection sensitivity.
     */
    @PostMapping("/sensitivity/{sensitivity}")
    public ResponseEntity<ApiResponse<Void>> setDetectionSensitivity(
            @PathVariable double sensitivity) {
        copyrightDetectionService.setDetectionSensitivity(sensitivity);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Set whether to block videos when copyright is detected.
     */
    @PostMapping("/block-on-detection/{blockOnDetection}")
    public ResponseEntity<ApiResponse<Void>> setBlockOnDetection(
            @PathVariable boolean blockOnDetection) {
        copyrightDetectionService.setBlockOnDetection(blockOnDetection);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}