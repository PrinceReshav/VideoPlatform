package com.platform.transcoding.service;

import com.platform.common.storage.StorageProvider;
import com.platform.common.storage.StorageObject;
import com.platform.common.entity.VideoFileInfo;
import com.platform.transcoding.model.ResolutionLadder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FfmpegService {

    private final StorageProvider storageProvider;

    @Value("${ffmpeg.threads:2}")
    private int ffmpegThreads;

    @Value("${transcoding.temp-dir:/tmp/transcoding}")
    private String tempDir;

    @Value("${transcoding.hls-segment-duration:6}")
    private int hlsSegmentDuration;

    public record VideoInfo(
            int width,
            int height,
            int durationSeconds,
            String videoCodec,
            String audioCodec,
            long bitrate
    ) {}

    public VideoInfo probeVideo(Path inputPath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe", "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height,codec_name,bit_rate",
                "-of", "csv=p=0",
                inputPath.toString()
        );
        Process process = pb.start();
        String output = readProcessOutput(process);
        process.waitFor(30, TimeUnit.SECONDS);

        String[] parts = output.trim().split(",");
        int width = Integer.parseInt(parts[0]);
        int height = Integer.parseInt(parts[1]);
        String codec = parts[2];
        long bitrate = parts.length > 3 && !parts[3].equals("N/A") ? Long.parseLong(parts[3]) : 0;

        // Get duration
        ProcessBuilder pbDur = new ProcessBuilder(
                "ffprobe", "-v", "error",
                "-show_entries", "format=duration",
                "-of", "csv=p=0",
                inputPath.toString()
        );
        Process processDur = pbDur.start();
        String durOutput = readProcessOutput(processDur);
        processDur.waitFor(30, TimeUnit.SECONDS);
        int duration = (int) Math.round(Double.parseDouble(durOutput.trim()));

        return new VideoInfo(width, height, duration, codec, "aac", bitrate);
    }

    public List<VideoFileInfo> transcodeToHls(UUID videoId, Path inputPath, String outputBucket,
                                               String outputPrefix, ResolutionLadder ladder,
                                               StorageProvider storageProvider) throws IOException, InterruptedException {
        List<VideoFileInfo> resultFiles = new ArrayList<>();

        // Create temp directory for this job
        Path jobTempDir = Paths.get(tempDir, videoId.toString());
        Files.createDirectories(jobTempDir);

        try {
            // Generate master playlist
            Path masterPlaylist = jobTempDir.resolve("master.m3u8");
            StringBuilder masterContent = new StringBuilder("#EXTM3U\n#EXT-X-VERSION:3\n");

            for (ResolutionLadder.Resolution resolution : ladder.getResolutions()) {
                log.info("Transcoding to {} for video {}", resolution.getName(), videoId);

                Path resolutionDir = jobTempDir.resolve(resolution.getName());
                Files.createDirectories(resolutionDir);

                Path outputPlaylist = resolutionDir.resolve("playlist.m3u8");
                Path outputPattern = resolutionDir.resolve("segment%05d.ts");

                // Build FFmpeg command
                List<String> cmd = buildTranscodeCommand(
                        inputPath,
                        outputPattern.toString(),
                        outputPlaylist.toString(),
                        resolution,
                        ladder.getSourceWidth(),
                        ladder.getSourceHeight()
                );

                // Execute FFmpeg
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(jobTempDir.toFile());
                pb.redirectErrorStream(true);
                Process process = pb.start();

                // Monitor progress
                monitorProgress(process, videoId, resolution.getName());

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    String error = readProcessOutput(process);
                    throw new IOException("FFmpeg failed for " + resolution.getName() + ": " + error);
                }

                // Upload segments and playlist to MinIO
                String resolutionPrefix = outputPrefix + "/" + resolution.getName() + "/";
                uploadResolutionFiles(resolutionDir, resolutionPrefix, storageProvider, resultFiles, videoId, resolution);

                // Add to master playlist
                masterContent.append("#EXT-X-STREAM-INF:BANDWIDTH=")
                        .append(resolution.getBitrateKbps() * 1000)
                        .append(",RESOLUTION=")
                        .append(resolution.getWidth())
                        .append("x")
                        .append(resolution.getHeight())
                        .append(",CODECS=\"avc1.4d401f,mp4a.40.2\"\n")
                        .append(resolution.getName())
                        .append("/playlist.m3u8\n");
            }

            // Upload master playlist
            String masterKey = outputPrefix + "/master.m3u8";
            String masterContentStr = masterContent.toString();
            Path masterFile = masterPlaylist;
            Files.writeString(masterFile, masterContentStr);

            StorageObject masterObj = storageProvider.putObject(
                    outputBucket, masterKey,
                    java.io.ByteArrayInputStream.class.cast(new java.io.ByteArrayInputStream(masterContentStr.getBytes())),
                    masterContentStr.length(),
                    "application/vnd.apple.mpegurl"
            );

            resultFiles.add(VideoFileInfo.builder()
                    .videoId(videoId)
                    .fileType(com.platform.common.entity.FileType.HLS_MANIFEST)
                    .resolution("master")
                    .objectKey(masterObj.getKey())
                    .bucket(masterObj.getBucket())
                    .fileSize(masterObj.getSize())
                    .contentType(masterObj.getContentType())
                    .checksum(masterObj.getChecksum())
                    .master(true)
                    .build());

            // Generate and upload thumbnails
            generateThumbnails(inputPath, jobTempDir, outputPrefix, storageProvider, resultFiles, videoId);

            // Generate sprite and VTT for scrubbing preview
            generateSprite(inputPath, jobTempDir, outputPrefix, storageProvider, resultFiles, videoId, ladder);

        } finally {
            // Cleanup temp files
            cleanupTempDir(jobTempDir);
        }

        return resultFiles;
    }

    private List<String> buildTranscodeCommand(Path inputPath, String segmentPattern,
                                               String playlistPath,
                                               ResolutionLadder.Resolution resolution,
                                               int sourceWidth, int sourceHeight) {
        // Calculate scale filter - maintain aspect ratio, don't upscale
        String scaleFilter = String.format("scale=w=%d:h=%d:force_original_aspect_ratio=decrease",
                resolution.getWidth(), resolution.getHeight());

        return List.of(
                "ffmpeg", "-y",
                "-i", inputPath.toString(),
                "-vf", scaleFilter,
                "-c:v", "libx264",
                "-profile:v", resolution.getProfile(),
                "-level", "4.0",
                "-b:v", resolution.getBitrateKbps() + "k",
                "-maxrate", resolution.getBitrateKbps() + "k",
                "-bufsize", (resolution.getBitrateKbps() * 2) + "k",
                "-c:a", "aac",
                "-b:a", "128k",
                "-ac", "2",
                "-ar", "48000",
                "-f", "hls",
                "-hls_time", String.valueOf(hlsSegmentDuration),
                "-hls_playlist_type", "vod",
                "-hls_segment_filename", segmentPattern,
                "-hls_flags", "independent_segments",
                "-threads", String.valueOf(ffmpegThreads),
                playlistPath
        );
    }

    private void monitorProgress(Process process, UUID videoId, String resolution) throws IOException, InterruptedException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Parse FFmpeg progress output
                if (line.contains("time=")) {
                    // Could emit progress events here
                }
            }
        }
    }

    private void uploadResolutionFiles(Path resolutionDir, String prefix,
                                       StorageProvider storageProvider,
                                       List<VideoFileInfo> resultFiles,
                                       UUID videoId,
                                       ResolutionLadder.Resolution resolution) throws IOException {
        try (var files = Files.list(resolutionDir)) {
            files.forEach(file -> {
            try {
                String fileName = file.getFileName().toString();
                String objectKey = prefix + fileName;
                String contentType = fileName.endsWith(".m3u8") ? "application/vnd.apple.mpegurl" : "video/mp2t";

                StorageObject obj;
                try (var input = Files.newInputStream(file)) {
                    obj = storageProvider.putObject("hls", objectKey, input, Files.size(file), contentType);
                }

                resultFiles.add(VideoFileInfo.builder()
                        .videoId(videoId)
                        .fileType(fileName.endsWith(".m3u8") ? com.platform.common.entity.FileType.HLS_MANIFEST : com.platform.common.entity.FileType.HLS_SEGMENT)
                        .resolution(resolution.getName())
                        .objectKey(obj.getKey())
                        .bucket(obj.getBucket())
                        .fileSize(obj.getSize())
                        .contentType(obj.getContentType())
                        .checksum(obj.getChecksum())
                        .durationSeconds(hlsSegmentDuration)
                        .bitrate(resolution.getBitrateKbps() * 1000)
                        .width(resolution.getWidth())
                        .height(resolution.getHeight())
                        .codec("h264")
                        .segmentIndex(fileName.endsWith(".ts") ? extractSegmentIndex(fileName) : null)
                        .master(fileName.equals("playlist.m3u8"))
                        .build());

            } catch (Exception e) {
                log.error("Failed to upload file {}: {}", file, e.getMessage());
            }
            });
        }
    }

    private int extractSegmentIndex(String fileName) {
        try {
            String numPart = fileName.replace("segment", "").replace(".ts", "");
            return Integer.parseInt(numPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void generateThumbnails(Path inputPath, Path tempDir, String outputPrefix,
                                    StorageProvider storageProvider,
                                    List<VideoFileInfo> resultFiles,
                                    UUID videoId) throws IOException, InterruptedException {
        Path thumbDir = tempDir.resolve("thumbnails");
        Files.createDirectories(thumbDir);

        // Generate thumbnails every 10 seconds
        String thumbPattern = thumbDir.resolve("thumb_%05d.jpg").toString();
        List<String> cmd = List.of(
                "ffmpeg", "-y",
                "-i", inputPath.toString(),
                "-vf", "fps=1/10,scale=320:-1",
                "-q:v", "2",
                thumbPattern
        );

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor(60, TimeUnit.SECONDS);

        // Upload thumbnails
        String thumbPrefix = outputPrefix + "/thumbnails/";
        try (var files = Files.list(thumbDir)) {
            files.forEach(file -> {
                try {
                    String fileName = file.getFileName().toString();
                    String objectKey = thumbPrefix + fileName;

                    StorageObject obj;
                    try (var input = Files.newInputStream(file)) {
                        obj = storageProvider.putObject("thumbnails", objectKey, input, Files.size(file), "image/jpeg");
                    }

                    resultFiles.add(VideoFileInfo.builder()
                            .videoId(videoId)
                            .fileType(com.platform.common.entity.FileType.THUMBNAIL)
                            .objectKey(obj.getKey())
                            .bucket(obj.getBucket())
                            .fileSize(obj.getSize())
                            .contentType(obj.getContentType())
                            .checksum(obj.getChecksum())
                            .build());
                } catch (Exception e) {
                    log.error("Failed to upload thumbnail {}: {}", file, e.getMessage());
                }
            });
        } catch (IOException e) {
            log.error("Failed to list thumbnails: {}", e.getMessage());
        }
    }

    private void generateSprite(Path inputPath, Path tempDir, String outputPrefix,
                                StorageProvider storageProvider,
                                List<VideoFileInfo> resultFiles,
                                UUID videoId, ResolutionLadder ladder) throws IOException, InterruptedException {
        // Generate sprite sheet (one frame every 10 seconds)
        Path spriteDir = tempDir.resolve("sprite");
        Files.createDirectories(spriteDir);

        String spriteFile = spriteDir.resolve("sprite.jpg").toString();
        String vttFile = spriteDir.resolve("sprite.vtt").toString();

        // Use 160x90 thumbnails in a 10x10 grid
        List<String> cmd = List.of(
                "ffmpeg", "-y",
                "-i", inputPath.toString(),
                "-vf", "fps=1/10,scale=160:90,tile=10x10",
                "-q:v", "2",
                spriteFile
        );

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor(60, TimeUnit.SECONDS);

        // Generate VTT file for sprite
        generateSpriteVtt(vttFile, 10, 10, 160, 90, 10.0);

        // Upload sprite
        if (Files.exists(Paths.get(spriteFile))) {
            String spriteKey = outputPrefix + "/thumbnails/sprite.jpg";
            StorageObject spriteObj = storageProvider.putObject(
                    "thumbnails", spriteKey,
                    Files.newInputStream(Paths.get(spriteFile)),
                    Files.size(Paths.get(spriteFile)),
                    "image/jpeg"
            );

            resultFiles.add(VideoFileInfo.builder()
                    .videoId(videoId)
                    .fileType(com.platform.common.entity.FileType.SPRITE)
                    .objectKey(spriteObj.getKey())
                    .bucket(spriteObj.getBucket())
                    .fileSize(spriteObj.getSize())
                    .contentType(spriteObj.getContentType())
                    .checksum(spriteObj.getChecksum())
                    .width(1600) // 10 * 160
                    .height(900) // 10 * 90
                    .build());
        }

        // Upload VTT
        if (Files.exists(Paths.get(vttFile))) {
            String vttKey = outputPrefix + "/thumbnails/sprite.vtt";
            String vttContent = Files.readString(Paths.get(vttFile));
            StorageObject vttObj = storageProvider.putObject(
                    "thumbnails", vttKey,
                    new java.io.ByteArrayInputStream(vttContent.getBytes()),
                    vttContent.length(),
                    "text/vtt"
            );

            resultFiles.add(VideoFileInfo.builder()
                    .videoId(videoId)
                    .fileType(com.platform.common.entity.FileType.SPRITE_VTT)
                    .objectKey(vttObj.getKey())
                    .bucket(vttObj.getBucket())
                    .fileSize(vttObj.getSize())
                    .contentType(vttObj.getContentType())
                    .checksum(vttObj.getChecksum())
                    .build());
        }
    }

    private void generateSpriteVtt(String vttPath, int cols, int rows, int thumbWidth, int thumbHeight, double interval) throws IOException {
        StringBuilder vtt = new StringBuilder("WEBVTT\n\n");
        int count = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                double startTime = count * interval;
                double endTime = (count + 1) * interval;
                int x = col * thumbWidth;
                int y = row * thumbHeight;

                vtt.append(formatTime(startTime)).append(" --> ").append(formatTime(endTime)).append("\n");
                vtt.append("sprite.jpg#xywh=").append(x).append(",").append(y).append(",").append(thumbWidth).append(",").append(thumbHeight).append("\n\n");
                count++;
            }
        }
        Files.writeString(Paths.get(vttPath), vtt.toString());
    }

    private String formatTime(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        int millis = (int) ((seconds - (int) seconds) * 1000);
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, secs, millis);
    }

    private void cleanupTempDir(Path tempDir) {
        try {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("Failed to delete temp file {}: {}", path, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to cleanup temp dir {}: {}", tempDir, e.getMessage());
        }
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }
}