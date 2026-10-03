package com.platform.common.storage;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioStorageProvider implements StorageProvider {

    private final MinioClient minioClient;

    @Value("${minio.bucket.videos:videos}")
    private String videosBucket;

    @Value("${minio.bucket.thumbnails:thumbnails}")
    private String thumbnailsBucket;

    @Value("${minio.bucket.hls:hls}")
    private String hlsBucket;

    @Override
    public String createMultipartUpload(String bucket, String key, String contentType) {
        // MinIO's compose API gives us a portable multipart implementation without
        // relying on SDK-internal S3 multipart methods. Each part is stored as a
        // temporary object and composed into the final object on completion.
        return java.util.UUID.randomUUID().toString();
    }

    @Override
    public String uploadPart(String bucket, String key, String uploadId, int partNumber,
                             InputStream inputStream, long partSize) {
        try {
            String partKey = partKey(key, uploadId, partNumber);
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(partKey)
                    .stream(inputStream, partSize, -1)
                    .contentType("application/octet-stream")
                    .build();
            ObjectWriteResponse response = minioClient.putObject(args);
            return response.etag();
        } catch (Exception e) {
            log.error("Failed to upload part {} for {}/{}", partNumber, bucket, key, e);
            throw new StorageException("Failed to upload multipart part", e);
        }
    }

    @Override
    public StorageObject completeMultipartUpload(String bucket, String key, String uploadId,
                                                  List<PartETag> parts) {
        try {
            List<ComposeSource> sources = parts.stream()
                    .sorted(java.util.Comparator.comparingInt(PartETag::partNumber))
                    .map(part -> ComposeSource.builder()
                            .bucket(bucket)
                            .object(partKey(key, uploadId, part.partNumber()))
                            .build())
                    .toList();

            ObjectWriteResponse response = minioClient.composeObject(ComposeObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .sources(sources)
                    .build());

            StorageObject result = getObject(bucket, key)
                    .orElseThrow(() -> new StorageException("Composed object was not found after upload"));
            result.setEtag(response.etag());
            result.setChecksum(response.etag());

            // Cleanup temporary part objects after successful composition.
            for (PartETag part : parts) {
                deleteObject(bucket, partKey(key, uploadId, part.partNumber()));
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to complete multipart upload for {}/{}", bucket, key, e);
            throw new StorageException("Failed to complete multipart upload", e);
        }
    }

    @Override
    public void abortMultipartUpload(String bucket, String key, String uploadId) {
        try {
            String prefix = key + ".__parts/" + uploadId + "/";
            for (StorageObject object : listObjects(bucket, prefix, 1000)) {
                deleteObject(bucket, object.getKey());
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup multipart upload {}: {}", uploadId, e.getMessage());
        }
    }

    private String partKey(String key, String uploadId, int partNumber) {
        return key + ".__parts/" + uploadId + "/" + partNumber;
    }

    @Override
    public StorageObject putObject(String bucket, String key, InputStream inputStream,
                                   long size, String contentType) {
        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build();
            ObjectWriteResponse response = minioClient.putObject(args);

            return StorageObject.builder()
                    .bucket(bucket)
                    .key(key)
                    .etag(response.etag())
                    .checksum(response.etag())
                    .size(size)
                    .contentType(contentType)
                    .lastModified(Instant.now())
                    .build();
        } catch (Exception e) {
            log.error("Failed to put object {}/{}", bucket, key, e);
            throw new StorageException("Failed to put object", e);
        }
    }

    @Override
    public Optional<StorageObject> getObject(String bucket, String key) {
        try {
            StatObjectArgs args = StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build();
            StatObjectResponse stat = minioClient.statObject(args);

            return Optional.of(StorageObject.builder()
                    .bucket(bucket)
                    .key(key)
                    .etag(stat.etag())
                    .checksum(stat.etag())
                    .size(stat.size())
                    .contentType(stat.contentType())
                    .lastModified(stat.lastModified().toInstant())
                    .build());
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return Optional.empty();
            }
            log.error("Failed to get object {}/{}", bucket, key, e);
            throw new StorageException("Failed to get object", e);
        } catch (Exception e) {
            log.error("Failed to get object {}/{}", bucket, key, e);
            throw new StorageException("Failed to get object", e);
        }
    }

    @Override
    public Optional<InputStream> getObjectStream(String bucket, String key) {
        try {
            GetObjectArgs args = GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build();
            return Optional.of(minioClient.getObject(args));
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return Optional.empty();
            }
            log.error("Failed to get object stream {}/{}", bucket, key, e);
            throw new StorageException("Failed to get object stream", e);
        } catch (Exception e) {
            log.error("Failed to get object stream {}/{}", bucket, key, e);
            throw new StorageException("Failed to get object stream", e);
        }
    }

    @Override
    public void deleteObject(String bucket, String key) {
        try {
            RemoveObjectArgs args = RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build();
            minioClient.removeObject(args);
        } catch (Exception e) {
            log.error("Failed to delete object {}/{}", bucket, key, e);
            throw new StorageException("Failed to delete object", e);
        }
    }

    @Override
    public void deleteObjects(String bucket, List<String> keys) {
        try {
            List<DeleteObject> objects = keys.stream()
                    .map(DeleteObject::new)
                    .collect(Collectors.toList());

            RemoveObjectsArgs args = RemoveObjectsArgs.builder()
                    .bucket(bucket)
                    .objects(objects)
                    .build();

            for (Result<DeleteError> result : minioClient.removeObjects(args)) {
                DeleteError error = result.get();
                log.warn("Failed to delete object {}: {}", error.objectName(), error.message());
            }
        } catch (Exception e) {
            log.error("Failed to delete objects from {}", bucket, e);
            throw new StorageException("Failed to delete objects", e);
        }
    }

    @Override
    public boolean objectExists(String bucket, String key) {
        try {
            StatObjectArgs args = StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build();
            minioClient.statObject(args);
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            throw new StorageException("Failed to check object existence", e);
        } catch (Exception e) {
            throw new StorageException("Failed to check object existence", e);
        }
    }

    @Override
    public String generatePresignedGetUrl(String bucket, String key, int expirySeconds) {
        try {
            GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .method(Method.GET)
                    .expiry(expirySeconds, TimeUnit.SECONDS)
                    .build();
            return minioClient.getPresignedObjectUrl(args);
        } catch (Exception e) {
            log.error("Failed to generate presigned GET URL for {}/{}", bucket, key, e);
            throw new StorageException("Failed to generate presigned URL", e);
        }
    }

    @Override
    public String generatePresignedPutUrl(String bucket, String key, int expirySeconds) {
        try {
            GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .method(Method.PUT)
                    .expiry(expirySeconds, TimeUnit.SECONDS)
                    .build();
            return minioClient.getPresignedObjectUrl(args);
        } catch (Exception e) {
            log.error("Failed to generate presigned PUT URL for {}/{}", bucket, key, e);
            throw new StorageException("Failed to generate presigned URL", e);
        }
    }

    @Override
    public StorageObject copyObject(String sourceBucket, String sourceKey,
                                    String destBucket, String destKey) {
        try {
            CopySource source = CopySource.builder()
                    .bucket(sourceBucket)
                    .object(sourceKey)
                    .build();

            CopyObjectArgs args = CopyObjectArgs.builder()
                    .bucket(destBucket)
                    .object(destKey)
                    .source(source)
                    .build();

            ObjectWriteResponse response = minioClient.copyObject(args);

            return StorageObject.builder()
                    .bucket(destBucket)
                    .key(destKey)
                    .etag(response.etag())
                    .checksum(response.etag())
                    .lastModified(Instant.now())
                    .build();
        } catch (Exception e) {
            log.error("Failed to copy object {}/{} to {}/{}", sourceBucket, sourceKey, destBucket, destKey, e);
            throw new StorageException("Failed to copy object", e);
        }
    }

    @Override
    public List<StorageObject> listObjects(String bucket, String prefix, int maxKeys) {
        try {
            ListObjectsArgs args = ListObjectsArgs.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .maxKeys(maxKeys)
                    .recursive(true)
                    .build();

            List<StorageObject> result = new ArrayList<>();
            Iterable<Result<Item>> objects = minioClient.listObjects(args);
            for (Result<Item> itemResult : objects) {
                Item item = itemResult.get();
                result.add(StorageObject.builder()
                        .bucket(bucket)
                        .key(item.objectName())
                        .etag(item.etag())
                        .size(item.size())
                        .lastModified(item.lastModified().toInstant())
                        .build());
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to list objects in {}/{}", bucket, prefix, e);
            throw new StorageException("Failed to list objects", e);
        }
    }

    @Override
    public Optional<StorageObject> getObjectMetadata(String bucket, String key) {
        return getObject(bucket, key);
    }

    public static class StorageException extends RuntimeException {

        public StorageException(String message) {
            super(message);
        }

        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}