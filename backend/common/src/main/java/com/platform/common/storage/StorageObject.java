package com.platform.common.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageObject {
    private String bucket;
    private String key;
    private String etag;
    private String checksum;
    private long size;
    private String contentType;
    private Instant lastModified;
    private Map<String, String> metadata;
    private String versionId;
}