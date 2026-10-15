# Video Moderation Service

## What this service actually does

The service consumes `video.uploaded.v1`, downloads the uploaded source object from the configured MinIO bucket, verifies its size/checksum, samples a bounded number of frames with FFmpeg/JavaCV, and analyzes those frames with OpenCV.

### Current inference model

This service intentionally does **not** claim to use a CNN unless a trained model is supplied and integrated. OpenCV, HOG, Haar cascades, skin-area analysis, and perceptual hashes are computer-vision heuristics; they are not a trained NSFW/sexual-content CNN.

No Python runtime is required.

### Pipeline

```text
Kafka video.uploaded.v1
        |
        v
MinIO object stream
        |
        +--> SHA-256 + size verification (single pass)
        |
        v
Temporary local video
        |
        v
JavaCV / FFmpeg
        |
        v
Bounded frame samples
        |
        v
OpenCV analysis
  - optional Haar face signal
  - optional HOG person signal
  - skin-area signal
  - optional dHash known-bad matching
        |
        v
Temporal scoring
        |
        v
Block / allow decision
```

## CNN / AI extension

A CNN requires a trained model. The service is deliberately structured so a future ONNX Runtime/OpenCV-DNN adapter can be added without introducing Python into the main Spring Boot process. The model should produce calibrated probabilities for categories such as `safe`, `nudity`, `sexual_activity`, or `explicit`.

Do not interpret the current heuristic score as a reliable NSFW probability. For a production platform, use a validated trained model and a human-review path for borderline cases.

## Important operational properties

- `moderation.max-sampled-frames` prevents unbounded memory usage on long videos.
- SHA-256 verification is performed while the object is copied to disk; the non-resettable network stream is never reused.
- Per-video temporal state is local to the analysis call, so concurrent Kafka deliveries cannot contaminate one another.
- OpenCV native resources are released after each video.
- Missing optional OpenCV classifiers degrade to zero signal rather than crashing the service.
- `block-on-error=true` provides fail-closed behavior for moderation failures.
