package com.platform.common.storage;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface StorageProvider {

    /**
     * Create a multipart upload and return upload ID
     */
    String createMultipartUpload(String bucket, String key, String contentType);

    /**
     * Upload a part for multipart upload
     */
    String uploadPart(String bucket, String key, String uploadId, int partNumber, InputStream inputStream, long partSize);

    /**
     * Complete multipart upload
     */
    StorageObject completeMultipartUpload(String bucket, String key, String uploadId, List<PartETag> parts);

    /**
     * Abort multipart upload
     */
    void abortMultipartUpload(String bucket, String key, String uploadId);

    /**
     * Upload a single object (for small files)
     */
    StorageObject putObject(String bucket, String key, InputStream inputStream, long size, String contentType);

    /**
     * Get object
     */
    Optional<StorageObject> getObject(String bucket, String key);

    /**
     * Get object as InputStream
     */
    Optional<InputStream> getObjectStream(String bucket, String key);

    /**
     * Delete object
     */
    void deleteObject(String bucket, String key);

    /**
     * Delete multiple objects
     */
    void deleteObjects(String bucket, List<String> keys);

    /**
     * Check if object exists
     */
    boolean objectExists(String bucket, String key);

    /**
     * Generate presigned URL for GET
     */
    String generatePresignedGetUrl(String bucket, String key, int expirySeconds);

    /**
     * Generate presigned URL for PUT (direct upload)
     */
    String generatePresignedPutUrl(String bucket, String key, int expirySeconds);

    /**
     * Copy object within or across buckets
     */
    StorageObject copyObject(String sourceBucket, String sourceKey, String destBucket, String destKey);

    /**
     * List objects with prefix
     */
    List<StorageObject> listObjects(String bucket, String prefix, int maxKeys);

    /**
     * Get object metadata without downloading
     */
    Optional<StorageObject> getObjectMetadata(String bucket, String key);

    record PartETag(int partNumber, String eTag) {}
}