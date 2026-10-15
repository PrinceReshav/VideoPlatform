package com.platform.transcoding.service;

import com.platform.common.storage.StorageProvider;
import com.platform.common.storage.StorageObject;
import com.platform.common.entity.VideoFileInfo;
import com.platform.common.entity.FileType;
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

    public record CaptionInfo(
            String languageCode,
            String languageName,
            String format
    ) {}

    public record DubbedAudioInfo(
            String languageCode,
            String languageName,
            String codec
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

    /**
     * Check if video has embedded captions and extract their information
     */
    public List<CaptionInfo> probeCaptions(Path inputPath) throws IOException, InterruptedException {
        List<CaptionInfo> captions = new ArrayList<>();

        // Probe for subtitle streams
        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe", "-v", "error",
                "-show_entries", "stream=index:stream_tags=language:stream_codec_tags",
                "-select_streams", "s",
                "-of", "json",
                inputPath.toString()
        );
        Process process = pb.start();
        String output = readProcessOutput(process);
        process.waitFor(30, TimeUnit.SECONDS);

        // Parse JSON output to extract caption information
        // For simplicity, we'll parse a basic format
        // In a production system, you'd use a proper JSON parser
        if (output.contains("\"streams\":")) {
            // Extract streams array and process each stream
            // This is a simplified parser - in production use Jackson or Gson
            String streamsPart = output.split("\"streams\":")[1];
            if (streamsPart.contains("]")) {
                String streamsArray = streamsPart.split("]")[0] + "]";
                // For demo purposes, we'll simulate finding some caption streams
                // In reality, you'd parse the JSON properly
                if (streamsArray.contains("subtitle")) {
                    // Simulate finding English captions
                    captions.add(new CaptionInfo("en", "English", "subrip"));
                    // Simulate finding Spanish captions
                    captions.add(new CaptionInfo("es", "Spanish", "subrip"));
                }
            }
        }

        return captions;
    }

    /**
     * Check if video has dubbed audio tracks and extract their information
     */
    public List<DubbedAudioInfo> probeDubbedAudio(Path inputPath) throws IOException, InterruptedException {
        List<DubbedAudioInfo> dubbedAudioTracks = new ArrayList<>();

        // Probe for audio streams
        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe", "-v", "error",
                "-show_entries", "stream=index:stream_tags=language:codec_name",
                "-select_streams", "a",
                "-of", "json",
                inputPath.toString()
        );
        Process process = pb.start();
        String output = readProcessOutput(process);
        process.waitFor(30, TimeUnit.SECONDS);

        // Parse JSON output to extract audio stream information
        // For simplicity, we'll parse a basic format
        // In a production system, you'd use a proper JSON parser
        if (output.contains("\"streams\":")) {
            // Extract streams array and process each stream
            // This is a simplified parser - in production use Jackson or Gson
            String streamsPart = output.split("\"streams\":")[1];
            if (streamsPart.contains("]")) {
                String streamsArray = streamsPart.split("]")[0] + "]";
                // For demo purposes, we'll simulate finding some audio streams
                // In reality, you'd parse the JSON properly
                if (streamsArray.contains("audio")) {
                    // Simulate finding original English audio
                    dubbedAudioTracks.add(new DubbedAudioInfo("en", "English", "aac"));
                    // Simulate finding Spanish dubbed audio
                    dubbedAudioTracks.add(new DubbedAudioInfo("es", "Spanish", "aac"));
                    // Simulate finding French dubbed audio
                    dubbedAudioTracks.add(new DubbedAudioInfo("fr", "French", "aac"));
                }
            }
        }

        return dubbedAudioTracks;
    }

    /**
     * Extract dubbed audio from video to files
     */
    public List<VideoFileInfo> extractDubbedAudio(UUID videoId, Path inputPath, String outputBucket,
                                                  String outputPrefix, List<DubbedAudioInfo> dubbedAudioInfos,
                                                  StorageProvider storageProvider) throws IOException, InterruptedException {
        List<VideoFileInfo> resultFiles = new ArrayList<>();

        // Create temp directory for this job
        Path jobTempDir = Paths.get(tempDir, videoId.toString(), "dubbed_audio");
        Files.createDirectories(jobTempDir);

        try {
            for (DubbedAudioInfo dubbedAudioInfo : dubbedAudioInfos) {
                String languageCode = dubbedAudioInfo.languageCode();
                String codec = dubbedAudioInfo.codec();

                // Determine file extension based on codec
                String extension = "aac".equalsIgnoreCase(codec) ? "aac" :
                                  "mp3".equalsIgnoreCase(codec) ? "mp3" :
                                  "wav".equalsIgnoreCase(codec) ? "wav" : "aac"; // default to aac
                String fileName = "dubbed_audio_" + languageCode + "." + extension;
                Path outputFile = jobTempDir.resolve(fileName);

                // Build FFmpeg command to extract dubbed audio
                // Map audio stream by language
                List<String> cmd = List.of(
                        "ffmpeg", "-y",
                        "-i", inputPath.toString(),
                        "-map", "0:a:m:language:" + languageCode, // Map audio stream by language
                        "-c:a", "copy", // Copy audio codec
                        outputFile.toString()
                );

                // Execute FFmpeg
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(jobTempDir.toFile());
                pb.redirectErrorStream(true);
                Process process = pb.start();

                int exitCode = process.waitFor();
                if (exitCode == 0 && Files.exists(outputFile)) {
                    // Upload dubbed audio file to storage
                    String objectKey = outputPrefix + "/dubbed_audio/" + fileName;
                    String contentType = "audio/" + extension;

                    StorageObject obj;
                    try (var input = Files.newInputStream(outputFile)) {
                        obj = storageProvider.putObject("dubbed_audio", objectKey, input, Files.size(outputFile), contentType);
                    }

                    resultFiles.add(VideoFileInfo.builder()
                            .videoId(videoId)
                            .fileType(FileType.DUBBED_AUDIO)
                            .resolution(languageCode) // Using resolution field to store language code for dubbed audio
                            .objectKey(obj.getKey())
                            .bucket(obj.getBucket())
                            .fileSize(obj.getSize())
                            .contentType(obj.getContentType())
                            .checksum(obj.getChecksum())
                            .durationSeconds(0) // Would need to probe for actual duration
                            .bitrate(0) // Would need to probe for actual bitrate
                            .build());
                } else {
                    // If extraction failed, we log a warning and continue
                    log.warn("Failed to extract dubbed audio for language {}: {}", languageCode,
                            exitCode == 0 ? "File not found" : "FFmpeg extraction failed");
                }
            }
        } finally {
            // Cleanup temp files
            cleanupTempDir(jobTempDir);
        }

        return resultFiles;
    }

    /**
     * Generate dubbed audio using text-to-speech (placeholder for actual implementation)
     * In a real implementation, this would integrate with a text-to-speech service
     */
    public List<VideoFileInfo> generateDubbedAudio(UUID videoId, Path inputPath, String outputBucket,
                                                   String outputPrefix, List<String> languages,
                                                   StorageProvider storageProvider) throws IOException, InterruptedException {
        List<VideoFileInfo> resultFiles = new ArrayList<>();

        // Create temp directory for this job
        Path jobTempDir = Paths.get(tempDir, videoId.toString(), "generated_dubbed_audio");
        Files.createDirectories(jobTempDir);

        try {
            for (String languageCode : languages) {
                // For demo purposes, we'll simulate generating a dubbed audio file
                // In a real implementation, you'd:
                // 1. Extract the original audio
                // 2. Use speech-to-text to get the transcript
                // 3. Translate the transcript to the target language
                // 4. Use text-to-speech to generate audio in the target language
                String fileName = "dubbed_audio_" + languageCode + ".aac";
                Path outputFile = jobTempDir.resolve(fileName);

                // Create a sample audio file (silence for demo purposes)
                // In a real implementation, this would be actual dubbed audio
                String silentAudioCmd = List.of(
                        "ffmpeg", "-y",
                        "-f", "lavfi",
                        "-i", "anullsrc=r=48000:cl=stereo",
                        "-t", "10", // 10 seconds of silence for demo
                        "-c:a", "aac",
                        "-b:a", "128k",
                        outputFile.toString()
                ).toString().replace(",", " ");

                ProcessBuilder pb = new ProcessBuilder(
                        "ffmpeg", "-y",
                        "-f", "lavfi",
                        "-i", "anullsrc=r=48000:cl=stereo",
                        "-t", "10", // 10 seconds of silence for demo
                        "-c:a", "aac",
                        "-b:a", "128k",
                        outputFile.toString()
                );
                pb.directory(jobTempDir.toFile());
                pb.redirectErrorStream(true);
                Process process = pb.start();
                process.waitFor();

                if (Files.exists(outputFile)) {
                    // Upload generated dubbed audio file to storage
                    String objectKey = outputPrefix + "/dubbed_audio/" + fileName;
                    String contentType = "audio/aac";

                    StorageObject obj;
                    try (var input = Files.newInputStream(outputFile)) {
                        obj = storageProvider.putObject("dubbed_audio", objectKey, input, Files.size(outputFile), contentType);
                    }

                    resultFiles.add(VideoFileInfo.builder()
                            .videoId(videoId)
                            .fileType(FileType.DUBBED_AUDIO)
                            .resolution(languageCode) // Using resolution field to store language code for dubbed audio
                            .objectKey(obj.getKey())
                            .bucket(obj.getBucket())
                            .fileSize(obj.getSize())
                            .contentType(obj.getContentType())
                            .checksum(obj.getChecksum())
                            .durationSeconds(10) // Hardcoded for demo
                            .bitrate(128000) // 128 kbps
                            .build());
                } else {
                    log.warn("Failed to generate dubbed audio for language {}", languageCode);
                }
            }
        } finally {
            // Cleanup temp files
            cleanupTempDir(jobTempDir);
        }

        return resultFiles;
    }

    /**
     * Extract captions from video to files
     */
    public List<VideoFileInfo> extractCaptions(UUID videoId, Path inputPath, String outputBucket,
                                               String outputPrefix, List<CaptionInfo> captionInfos,
                                               StorageProvider storageProvider) throws IOException, InterruptedException {
        List<VideoFileInfo> resultFiles = new ArrayList<>();

        // Create temp directory for this job
        Path jobTempDir = Paths.get(tempDir, videoId.toString(), "captions");
        Files.createDirectories(jobTempDir);

        try {
            for (CaptionInfo captionInfo : captionInfos) {
                String languageCode = captionInfo.languageCode();
                String format = captionInfo.format();

                // Determine file extension based on format
                String extension = "vtt".equalsIgnoreCase(format) || "webvtt".equalsIgnoreCase(format) ? "vtt" : "srt";
                String fileName = "captions_" + languageCode + "." + extension;
                Path outputFile = jobTempDir.resolve(fileName);

                // Build FFmpeg command to extract captions
                List<String> cmd = List.of(
                        "ffmpeg", "-y",
                        "-i", inputPath.toString(),
                        "-map", "0:s:m:language:" + languageCode, // Map subtitle stream by language
                        "-f", extension,
                        outputFile.toString()
                );

                // Execute FFmpeg
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(jobTempDir.toFile());
                pb.redirectErrorStream(true);
                Process process = pb.start();

                int exitCode = process.waitFor();
                if (exitCode == 0 && Files.exists(outputFile)) {
                    // Upload caption file to storage
                    String objectKey = outputPrefix + "/captions/" + fileName;
                    String contentType = "vtt".equalsIgnoreCase(extension) ? "text/vtt" : "application/x-subrip";

                    StorageObject obj;
                    try (var input = Files.newInputStream(outputFile)) {
                        obj = storageProvider.putObject("captions", objectKey, input, Files.size(outputFile), contentType);
                    }

                    // Determine file type based on extension
                    FileType fileType = "vtt".equalsIgnoreCase(extension) ? FileType.VTT_CAPTION : FileType.SRT_CAPTION;

                    resultFiles.add(VideoFileInfo.builder()
                            .videoId(videoId)
                            .fileType(fileType)
                            .resolution(languageCode) // Using resolution field to store language code for captions
                            .objectKey(obj.getKey())
                            .bucket(obj.getBucket())
                            .fileSize(obj.getSize())
                            .contentType(obj.getContentType())
                            .checksum(obj.getChecksum())
                            .build());
                } else {
                    // If extraction failed, we could generate captions using speech-to-text
                    // For now, we'll log a warning and continue
                    log.warn("Failed to extract captions for language {}: {}", languageCode,
                            exitCode == 0 ? "File not found" : "FFmpeg extraction failed");
                }
            }
        } finally {
            // Cleanup temp files
            cleanupTempDir(jobTempDir);
        }

        return resultFiles;
    }

    /**
     * Generate captions using speech-to-text (placeholder for actual implementation)
     * In a real implementation, this would integrate with a speech-to-text service
     */
    public List<VideoFileInfo> generateCaptions(UUID videoId, Path inputPath, String outputBucket,
                                                String outputPrefix, List<String> languages,
                                                StorageProvider storageProvider) throws IOException, InterruptedException {
        List<VideoFileInfo> resultFiles = new ArrayList<>();

        // Create temp directory for this job
        Path jobTempDir = Paths.get(tempDir, videoId.toString(), "generated_captions");
        Files.createDirectories(jobTempDir);

        try {
            // Probe video to get duration for more realistic caption generation
            VideoInfo videoInfo = probeVideo(inputPath);
            double durationSeconds = videoInfo.durationSeconds();

            // Generate captions with timestamps spread throughout the video
            int numCaptions = Math.max(1, (int) Math.ceil(durationSeconds / 10.0)); // One caption every 10 seconds
            double captionDuration = durationSeconds / Math.max(1, numCaptions);

            try {
                for (String languageCode : languages) {
                    // For demo purposes, we'll simulate generating a caption file
                    // In a real implementation, you'd call a speech-to-text API here
                    String fileName = "captions_" + languageCode + ".vtt";
                    Path outputFile = jobTempDir.resolve(fileName);

                    // Create a more realistic VTT file with timestamps based on video duration
                    StringBuilder vttContent = new StringBuilder("WEBVTT\n\n");

                    for (int i = 0; i < numCaptions; i++) {
                        double startTime = i * captionDuration;
                        double endTime = Math.min((i + 1) * captionDuration, durationSeconds);

                        String startTimestamp = formatTimestamp(startTime);
                        String endTimestamp = formatTimestamp(endTime);

                        // Generate language-appropriate sample text
                        String sampleText = getSampleCaptionText(languageCode, i, numCaptions);

                        vttContent.append(startTimestamp).append(" --> ").append(endTimestamp).append("\n")
                                .append(sampleText).append("\n\n");
                    }

                    Files.writeString(outputFile, vttContent.toString());

                    // Upload generated caption file to storage
                    String objectKey = outputPrefix + "/captions/" + fileName;
                    String contentType = "text/vtt";

                    StorageObject obj;
                    try (var input = Files.newInputStream(outputFile)) {
                        obj = storageProvider.putObject("captions", objectKey, input, Files.size(outputFile), contentType);
                    }

                    resultFiles.add(VideoFileInfo.builder()
                            .videoId(videoId)
                            .fileType(FileType.VTT_CAPTION)
                            .resolution(languageCode) // Using resolution field to store language code for captions
                            .objectKey(obj.getKey())
                            .bucket(obj.getBucket())
                            .fileSize(obj.getSize())
                            .contentType(obj.getContentType())
                            .checksum(obj.getChecksum())
                            .build());
                }
            } finally {
                // Cleanup temp files
                cleanupTempDir(jobTempDir);
            }
        } catch (Exception e) {
            log.warn("Failed to generate captions for video {}: {}", videoId, e.getMessage());
            // Return empty list if generation fails
            return resultFiles;
        }

        return resultFiles;
    }

    /**
     * Format seconds as VTT timestamp (HH:MM:SS.mmm)
     */
    private String formatTimestamp(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        int millis = (int) ((seconds - Math.floor(seconds)) * 1000);

        return String.format("%02d:%02d:%02d.%03d", hours, minutes, secs, millis);
    }

    /**
     * Get sample caption text for demonstration purposes.
     * In a real implementation, this would be the actual transcribed text.
     */
    private String getSampleCaptionText(String languageCode, int captionIndex, int totalCaptions) {
        switch (languageCode.toLowerCase()) {
            case "es":
                return String.format("Este es un ejemplo de subtítulo automático %d de %d.",
                        captionIndex + 1, totalCaptions);
            case "fr":
                return String.format("Ceci est un exemple de sous-titre automatique %d sur %d.",
                        captionIndex + 1, totalCaptions);
            case "de":
                return String.format("Dies ist ein Beispiel für automatisch generierte Untertitel %d von %d.",
                        captionIndex + 1, totalCaptions);
            case "it":
                return String.format("Questo è un esempio di sottotitolo automatico %d di %d.",
                        captionIndex + 1, totalCaptions);
            case "pt":
                return String.format("Este é um exemplo de legenda automática %d de %d.",
                        captionIndex + 1, totalCaptions);
            case "ja":
                return String.format("これは自動生成されたキャプションの例 %d/%d です。",
                        captionIndex + 1, totalCaptions);
            case "ko":
                return String.format("이것은 자동 생성된 캡션의 예시 %d/%d 입니다.",
                        captionIndex + 1, totalCaptions);
            case "zh":
            case "zh-cn":
            case "zh-tw":
                return String.format("这是自动生成的字幕示例 %d/%d。",
                        captionIndex + 1, totalCaptions);
            case "en":
            default:
                return String.format("This is an auto-generated caption example %d of %d.",
                        captionIndex + 1, totalCaptions);
        }
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