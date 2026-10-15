package com.platform.analytics.service;

import com.platform.analytics.dto.ChannelAnalyticsResponse;
import com.platform.analytics.dto.ChannelAnalyticsSummary;
import com.platform.analytics.dto.DailyChannelAnalytics;
import com.platform.analytics.dto.VideoAnalyticsResponse;
import com.platform.analytics.dto.VideoAnalyticsSummary;
import com.platform.analytics.dto.DailyVideoAnalytics;
import com.platform.analytics.entity.ChannelAnalyticsDaily;
import com.platform.analytics.entity.VideoAnalyticsDaily;
import com.platform.analytics.repository.ChannelAnalyticsDailyRepository;
import com.platform.analytics.repository.VideoAnalyticsDailyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final VideoAnalyticsDailyRepository videoAnalyticsRepository;
    private final ChannelAnalyticsDailyRepository channelAnalyticsRepository;

    @Transactional
    public void recordVideoView(UUID videoId, UUID channelId, LocalDate date, Integer positionSeconds) {
        if (videoId == null || date == null) return;
        incrementVideoAnalytics(videoId, date, v -> {
            v.setViews(v.getViews() + 1);
            if (positionSeconds != null && positionSeconds > 0) {
                v.setWatchTimeSeconds(v.getWatchTimeSeconds() + positionSeconds);
            }
        });
        if (channelId != null) incrementChannelAnalytics(channelId, date, c -> {
            c.setViews(c.getViews() + 1);
            if (positionSeconds != null && positionSeconds > 0) c.setWatchTimeSeconds(c.getWatchTimeSeconds() + positionSeconds);
        });
    }

    @Transactional
    public void recordVideoLike(UUID videoId, UUID channelId, LocalDate date, boolean liked) {
        if (videoId == null || date == null) return;
        incrementVideoAnalytics(videoId, date, v -> {
            if (liked) v.setLikes(v.getLikes() + 1);
            else v.setLikes(Math.max(0, v.getLikes() - 1));
        });
        if (channelId != null) incrementChannelAnalytics(channelId, date, c -> {
            if (liked) c.setLikes(c.getLikes() + 1);
            else c.setLikes(Math.max(0, c.getLikes() - 1));
        });
    }

    @Transactional
    public void recordComment(UUID videoId, UUID channelId, LocalDate date) {
        if (videoId == null || date == null) return;
        incrementVideoAnalytics(videoId, date, v -> v.setComments(v.getComments() + 1));
        if (channelId != null) incrementChannelAnalytics(channelId, date, c -> c.setComments(c.getComments() + 1));
    }

    private void incrementVideoAnalytics(UUID videoId, LocalDate date, java.util.function.Consumer<VideoAnalyticsDaily> updater) {
        VideoAnalyticsDaily analytics = videoAnalyticsRepository.findByVideoIdAndDate(videoId, date)
                .orElseGet(() -> {
                    VideoAnalyticsDaily created = new VideoAnalyticsDaily();
                    created.setVideoId(videoId);
                    created.setDate(date);
                    return created;
                });
        updater.accept(analytics);
        videoAnalyticsRepository.save(analytics);
    }

    private void incrementChannelAnalytics(UUID channelId, LocalDate date, java.util.function.Consumer<ChannelAnalyticsDaily> updater) {
        ChannelAnalyticsDaily analytics = channelAnalyticsRepository.findByChannelIdAndDate(channelId, date)
                .orElseGet(() -> {
                    ChannelAnalyticsDaily newAnalytics = new ChannelAnalyticsDaily();
                    newAnalytics.setChannelId(channelId);
                    newAnalytics.setDate(date);
                    return newAnalytics;
                });

        updater.accept(analytics);
        channelAnalyticsRepository.save(analytics);
    }

    @Transactional(readOnly = true)
    public VideoAnalyticsResponse getVideoAnalytics(UUID videoId, LocalDate startDate, LocalDate endDate) {
        List<VideoAnalyticsDaily> dailyData = videoAnalyticsRepository.findByVideoIdAndDateBetween(videoId, startDate, endDate);
        VideoAnalyticsSummary summary = videoAnalyticsRepository.getSummaryByVideoIdAndDateRange(videoId, startDate, endDate);

        return VideoAnalyticsResponse.builder()
                .videoId(videoId)
                .videoTitle("Video " + videoId)
                .summary(summary)
                .dailyData(dailyData.stream().map(this::mapToDailyVideo).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public ChannelAnalyticsResponse getChannelAnalytics(UUID channelId, LocalDate startDate, LocalDate endDate) {
        List<ChannelAnalyticsDaily> dailyData = channelAnalyticsRepository.findByChannelIdAndDateBetween(channelId, startDate, endDate);
        ChannelAnalyticsSummary summary = channelAnalyticsRepository.getSummaryByChannelIdAndDateRange(channelId, startDate, endDate);

        return ChannelAnalyticsResponse.builder()
                .channelId(channelId)
                .channelName("Channel " + channelId)
                .summary(summary)
                .dailyData(dailyData.stream().map(this::mapToDailyChannel).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public List<VideoAnalyticsSummary> getTopVideosByViews(UUID channelId, LocalDate startDate, LocalDate endDate, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return videoAnalyticsRepository.findByChannelIdAndDateBetween(channelId, startDate, endDate).stream()
                .collect(java.util.stream.Collectors.groupingBy(VideoAnalyticsDaily::getVideoId))
                .values().stream()
                .map(rows -> {
                    long views = rows.stream().mapToLong(VideoAnalyticsDaily::getViews).sum();
                    long watch = rows.stream().mapToLong(VideoAnalyticsDaily::getWatchTimeSeconds).sum();
                    long likes = rows.stream().mapToLong(VideoAnalyticsDaily::getLikes).sum();
                    long comments = rows.stream().mapToLong(VideoAnalyticsDaily::getComments).sum();
                    long shares = rows.stream().mapToLong(VideoAnalyticsDaily::getShares).sum();
                    long gained = rows.stream().mapToLong(VideoAnalyticsDaily::getSubscribersGained).sum();
                    long lost = rows.stream().mapToLong(VideoAnalyticsDaily::getSubscribersLost).sum();
                    double avgDuration = rows.stream().map(VideoAnalyticsDaily::getAvgViewDurationSeconds).filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(0d);
                    double avgPercentage = rows.stream().map(VideoAnalyticsDaily::getAvgViewPercentage).filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(0d);
                    return VideoAnalyticsSummary.builder()
                            .totalViews(views)
                            .totalWatchTimeSeconds(watch)
                            .totalLikes(likes)
                            .totalComments(comments)
                            .totalShares(shares)
                            .totalSubscribersGained(gained)
                            .totalSubscribersLost(lost)
                            .avgViewDurationSeconds(avgDuration)
                            .avgViewPercentage(avgPercentage)
                            .build();
                })
                .sorted(java.util.Comparator.comparing(VideoAnalyticsSummary::getTotalViews, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .limit(safeLimit)
                .toList();
    }

    private DailyVideoAnalytics mapToDailyVideo(VideoAnalyticsDaily a) {
        return DailyVideoAnalytics.builder()
                .date(a.getDate())
                .views(a.getViews())
                .watchTimeSeconds(a.getWatchTimeSeconds())
                .likes(a.getLikes())
                .comments(a.getComments())
                .shares(a.getShares())
                .subscribersGained(a.getSubscribersGained())
                .subscribersLost(a.getSubscribersLost())
                .avgViewDurationSeconds(a.getAvgViewDurationSeconds())
                .avgViewPercentage(a.getAvgViewPercentage())
                // Enhanced metrics for Feature 6
                .uniqueViewers(a.getUniqueViewers())
                .viewersAtPeakConcurrent(a.getViewersAtPeakConcurrent())
                .averageCompletionRatePercentage(a.getAverageCompletionRatePercentage())
                // Note: JSON fields would need to be parsed - for now leaving as null/empty
                // These would be populated when the analytics pipeline is enhanced to collect this data
                .trafficSources(java.util.Collections.emptyMap())
                .viewsByCountry(java.util.Collections.emptyMap())
                .viewsByCity(java.util.Collections.emptyMap())
                .viewsByDeviceType(java.util.Collections.emptyMap())
                .viewsByOperatingSystem(java.util.Collections.emptyMap())
                .viewsByBrowser(java.util.Collections.emptyMap())
                .viewsByQuality(java.util.Collections.emptyMap())
                .averageBitrateKbps(a.getAverageBitrateKbps())
                .rebufferCount(a.getRebufferCount())
                .rebufferRatio(a.getRebufferRatio())
                .estimatedRevenue(a.getEstimatedRevenue())
                .adRevenue(a.getAdRevenue())
                .premiumRevenue(a.getPremiumRevenue())
                .adImpressions(a.getAdImpressions())
                .adClicks(a.getAdClicks())
                .cpm(a.getCpm())
                .likesPerView(a.getLikesPerView())
                .commentsPerView(a.getCommentsPerView())
                .sharesPerView(a.getSharesPerView())
                .subscribersGainedPerView(a.getSubscribersGainedPerView())
                .retentionAtIntervals(java.util.Collections.emptyMap())
                .audienceByAgeGroup(java.util.Collections.emptyMap())
                .audienceByGender(java.util.Collections.emptyMap())
                .build();
    }

    private DailyChannelAnalytics mapToDailyChannel(ChannelAnalyticsDaily a) {
        return DailyChannelAnalytics.builder()
                .date(a.getDate())
                .views(a.getViews())
                .watchTimeSeconds(a.getWatchTimeSeconds())
                .likes(a.getLikes())
                .comments(a.getComments())
                .shares(a.getShares())
                .subscribersGained(a.getSubscribersGained())
                .subscribersLost(a.getSubscribersLost())
                .videosPublished(a.getVideosPublished())
                .totalVideos(a.getTotalVideos())
                .estimatedRevenue(a.getEstimatedRevenue())
                // Enhanced metrics for Feature 6
                .uniqueViewers(a.getUniqueViewers())
                .viewersAtPeakConcurrent(a.getViewersAtPeakConcurrent())
                .averageCompletionRatePercentage(a.getAverageCompletionRatePercentage())
                .shortsPublished(a.getShortsPublished())
                .liveStreamMinutes(a.getLiveStreamMinutes())
                .peakConcurrentLiveViewers(a.getPeakConcurrentLiveViewers())
                // Note: JSON fields would need to be parsed - for now leaving as null/empty
                .trafficSources(java.util.Collections.emptyMap())
                .viewsByCountry(java.util.Collections.emptyMap())
                .viewsByCity(java.util.Collections.emptyMap())
                .viewsByDeviceType(java.util.Collections.emptyMap())
                .viewsByOperatingSystem(java.util.Collections.emptyMap())
                .viewsByBrowser(java.util.Collections.emptyMap())
                .viewsByQuality(java.util.Collections.emptyMap())
                .averageBitrateKbps(a.getAverageBitrateKbps())
                .rebufferCount(a.getRebufferCount())
                .rebufferRatio(a.getRebufferRatio())
                .adRevenue(a.getAdRevenue())
                .premiumRevenue(a.getPremiumRevenue())
                .membershipRevenue(a.getMembershipRevenue())
                .adImpressions(a.getAdImpressions())
                .adClicks(a.getAdClicks())
                .membershipsGained(a.getMembershipsGained())
                .membershipsLost(a.getMembershipsLost())
                .cpm(a.getCpm())
                .averageViewsPerVideo(a.getAverageViewsPerVideo())
                .averageWatchTimePerVideo(a.getAverageWatchTimePerVideo())
                .engagementRatePercentage(a.getEngagementRatePercentage())
                .subscriberGrowthRatePercentage(a.getSubscriberGrowthRatePercentage())
                .likesPerView(a.getLikesPerView())
                .commentsPerView(a.getCommentsPerView())
                .sharesPerView(a.getSharesPerView())
                .subscribersGainedPerView(a.getSubscribersGainedPerView())
                .retentionAtIntervals(java.util.Collections.emptyMap())
                .audienceByAgeGroup(java.util.Collections.emptyMap())
                .audienceByGender(java.util.Collections.emptyMap())
                .build();
    }
}