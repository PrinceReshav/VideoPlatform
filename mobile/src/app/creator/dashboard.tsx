import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image, ActivityIndicator, TextInput, KeyboardAvoidingView, Platform, ScrollView, RefreshControl, FlatList } from 'react-native';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link, useRouter } from 'expo-router';
import { Upload, Video, BarChart2, Settings, Plus, Search, ChevronRight, Check } from 'lucide-react-native';
import { videoApi } from '../../services/api/videoApi';
import { uploadApi } from '../../services/api/uploadApi';
import { channelApi } from '../../services/api/channelApi';

export default function CreatorDashboardScreen() {
  const router = useRouter();
  const { data: channel, isLoading: channelLoading } = useQuery({
    queryKey: ['myChannel'],
    queryFn: () => channelApi.getMyChannel(),
  });

  const { data: videos, isLoading: videosLoading } = useQuery({
    queryKey: ['myCreatorVideos'],
    queryFn: () => videoApi.getMyVideos({ page: 0, size: 10 }),
  });

  const formatDuration = (seconds?: number) => {
    if (!seconds) return '';
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const formatViews = (count: number) => {
    if (count >= 1000000) return `${(count / 1000000).toFixed(1)}M`;
    if (count >= 1000) return `${(count / 1000).toFixed(1)}K`;
    return count.toString();
  };

  const renderVideo = ({ item }: { item: any }) => (
    <TouchableOpacity style={styles.videoCard} onPress={() => router.push(`/watch/${item.slug || item.id}`)}>
      <View style={styles.thumbnailContainer}>
        {item.thumbnailUrl ? (
          <Image source={{ uri: item.thumbnailUrl }} style={styles.thumbnail} resizeMode="cover" />
        ) : (
          <View style={[styles.thumbnail, { backgroundColor: '#e2e8f0' }]} />
        )}
        {item.durationSeconds && (
          <View style={styles.durationBadge}>
            <Text style={styles.durationText}>{formatDuration(item.durationSeconds)}</Text>
          </View>
        )}
        <View style={styles.statusBadge}>
          <Text style={[
            styles.statusText,
            item.publicationState === 'PUBLISHED' && styles.statusPublished,
            item.publicationState === 'DRAFT' && styles.statusDraft,
            item.processingState === 'PROCESSING' && styles.statusProcessing,
            item.processingState === 'PROCESSING_FAILED' && styles.statusFailed,
          ]}>
            {item.publicationState === 'PUBLISHED' ? 'Published' :
             item.publicationState === 'DRAFT' ? 'Draft' :
             item.processingState === 'PROCESSING' ? 'Processing' : 'Failed'}
          </Text>
        </View>
      </View>
      <View style={styles.videoInfo}>
        <Text style={styles.videoTitle} numberOfLines={2}>{item.title}</Text>
        <View style={styles.metaRow}>
          <Text style={styles.metaText}>{formatViews(item.viewCount)} views</Text>
          <Text style={styles.separator}>•</Text>
          <Text style={styles.metaText}>{formatDuration(item.durationSeconds)}</Text>
        </View>
      </View>
    </TouchableOpacity>
  );

  if (channelLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#6366f1" />
      </View>
    );
  }

  if (!channel) {
    return (
      <View style={styles.emptyContainer}>
        <View style={styles.emptyCard}>
          <Video size={48} color="#94a3b8" style={{ opacity: 0.5, marginBottom: 16 }} />
          <Text style={styles.emptyTitle}>Become a Creator</Text>
          <Text style={styles.emptySubtitle}>Start your channel and share your content with the world</Text>
          <TouchableOpacity style={styles.becomeCreatorButton} onPress={() => router.push('/creator/setup')}>
            <Text style={styles.becomeCreatorButtonText}>Create Channel</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  return (
    <ScrollView
      refreshControl={
        <RefreshControl
          refreshing={videosLoading}
          onRefresh={() => {}}
          colors={['#6366f1']}
        />
      }
      contentContainerStyle={styles.contentContainer}
    >
      {/* Header */}
      <View style={styles.header}>
        <View style={styles.headerContent}>
          <View style={styles.channelAvatarContainer}>
            {channel.avatarUrl ? (
              <Image source={{ uri: channel.avatarUrl }} style={styles.channelAvatar} />
            ) : (
              <View style={[styles.channelAvatar, { backgroundColor: '#e2e8f0' }]} />
            )}
            {channel.verified && (
              <View style={styles.verifiedBadge}>
                <Check size={12} color="#ffffff" />
              </View>
            )}
          </View>
          <View style={styles.channelInfo}>
            <Text style={styles.channelName}>{channel.name}</Text>
            <Text style={styles.channelHandle}>@{channel.slug}</Text>
            <View style={styles.statsRow}>
              <View style={styles.statItem}>
                <Text style={styles.statValue}>{channel.subscriberCount?.toLocaleString()}</Text>
                <Text style={styles.statLabel}>Subscribers</Text>
              </View>
              <View style={styles.statItem}>
                <Text style={styles.statValue}>{channel.videoCount}</Text>
                <Text style={styles.statLabel}>Videos</Text>
              </View>
              <View style={styles.statItem}>
                <Text style={styles.statValue}>{channel.totalViews?.toLocaleString()}</Text>
                <Text style={styles.statLabel}>Views</Text>
              </View>
            </View>
          </View>
        </View>
      </View>

      {/* Quick Actions */}
      <View style={styles.section}>
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Quick Actions</Text>
        </View>
        <View style={styles.actionsGrid}>
          <Link to="/creator/upload" style={styles.actionCard}>
            <View style={styles.actionIcon}>
              <Upload size={24} color="#6366f1" />
            </View>
            <Text style={styles.actionTitle}>Upload Video</Text>
            <Text style={styles.actionDescription}>Add new content</Text>
          </Link>
          <Link to="/creator/videos" style={styles.actionCard}>
            <View style={styles.actionIcon}>
              <Video size={24} color="#10b981" />
            </View>
            <Text style={styles.actionTitle}>My Videos</Text>
            <Text style={styles.actionDescription}>Manage content</Text>
          </Link>
          <Link to="/creator/analytics" style={styles.actionCard}>
            <View style={styles.actionIcon}>
              <BarChart2 size={24} color="#f59e0b" />
            </View>
            <Text style={styles.actionTitle}>Analytics</Text>
            <Text style={styles.actionDescription}>Track performance</Text>
          </Link>
          <Link to="/creator/settings" style={styles.actionCard}>
            <View style={styles.actionIcon}>
              <Settings size={24} color="#64748b" />
            </View>
            <Text style={styles.actionTitle}>Settings</Text>
            <Text style={styles.actionDescription}>Channel options</Text>
          </Link>
        </View>
      </View>

      {/* Latest Videos */}
      <View style={styles.section}>
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Your Videos</Text>
          <TouchableOpacity onPress={() => router.push('/creator/videos')}>
            <Text style={styles.viewAll}>View All</Text>
            <ChevronRight size={20} color="#64748b" />
          </TouchableOpacity>
        </View>

        {videosLoading ? (
          <View style={styles.loadingContainer}>
            <ActivityIndicator size="large" color="#6366f1" />
          </View>
        ) : videos?.content?.length > 0 ? (
          <FlatList
            data={videos.content}
            renderItem={renderVideo}
            keyExtractor={(item) => item.id}
            numColumns={2}
            showsVerticalScrollIndicator={false}
            contentContainerStyle={styles.gridList}
          />
        ) : (
          <View style={styles.emptyState}>
            <Video size={48} color="#94a3b8" style={{ opacity: 0.5, marginBottom: 16 }} />
            <Text style={styles.emptyTitle}>No videos yet</Text>
            <Text style={styles.emptySubtitle}>Upload your first video to get started</Text>
            <TouchableOpacity style={styles.uploadButton} onPress={() => router.push('/creator/upload')}>
              <Text style={styles.uploadButtonText}>Upload Video</Text>
            </TouchableOpacity>
          </View>
        )}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8fafc',
  },
  header: {
    backgroundColor: '#ffffff',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
  },
  headerContent: {
    flexDirection: 'row',
    gap: 16,
  },
  channelAvatarContainer: {
    position: 'relative',
  },
  channelAvatar: {
    width: 80,
    height: 80,
    borderRadius: 40,
  },
  verifiedBadge: {
    position: 'absolute',
    bottom: 0,
    right: 0,
    width: 20,
    height: 20,
    borderRadius: 10,
    backgroundColor: '#6366f1',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 3,
    borderColor: '#ffffff',
  },
  channelInfo: {
    flex: 1,
    justifyContent: 'center',
  },
  channelName: {
    fontSize: 20,
    fontWeight: '700',
    color: '#0f172a',
  },
  channelHandle: {
    fontSize: 14,
    color: '#64748b',
    marginTop: 4,
  },
  statsRow: {
    flexDirection: 'row',
    gap: 16,
    marginTop: 12,
  },
  statItem: {
    alignItems: 'center',
  },
  statValue: {
    fontSize: 16,
    fontWeight: '700',
    color: '#0f172a',
  },
  statLabel: {
    fontSize: 11,
    color: '#64748b',
    marginTop: 2,
  },
  section: {
    padding: 16,
    paddingBottom: 8,
  },
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 16,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#0f172a',
  },
  viewAll: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    color: '#6366f1',
    fontWeight: '600',
  },
  actionsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
  },
  actionCard: {
    width: '48%',
    padding: 16,
    backgroundColor: '#ffffff',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#e2e8f0',
  },
  actionIcon: {
    width: 40,
    height: 40,
    borderRadius: 10,
    backgroundColor: '#6366f11a',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 12,
  },
  actionTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#0f172a',
    marginBottom: 4,
  },
  actionDescription: {
    fontSize: 12,
    color: '#64748b',
  },
  gridList: {
    paddingHorizontal: 16,
    gap: 12,
  },
  videoCard: {
    width: '48%',
  },
  thumbnailContainer: {
    position: 'relative',
    borderRadius: 12,
    overflow: 'hidden',
    marginBottom: 8,
    backgroundColor: '#e2e8f0',
  },
  thumbnail: {
    width: '100%',
    aspectRatio: 16 / 9,
  },
  durationBadge: {
    position: 'absolute',
    bottom: 8,
    right: 8,
    backgroundColor: 'rgba(0, 0, 0, 0.9)',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4,
  },
  durationText: {
    color: '#ffffff',
    fontSize: 11,
    fontWeight: '600',
  },
  statusBadge: {
    position: 'absolute',
    top: 8,
    left: 8,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4,
  },
  statusText: {
    fontSize: 10,
    fontWeight: '600',
    color: '#ffffff',
  },
  statusPublished: {
    backgroundColor: '#10b981',
  },
  statusDraft: {
    backgroundColor: '#64748b',
  },
  statusProcessing: {
    backgroundColor: '#f59e0b',
  },
  statusFailed: {
    backgroundColor: '#ef4444',
  },
  videoInfo: {
    gap: 4,
  },
  videoTitle: {
    fontSize: 13,
    fontWeight: '600',
    color: '#0f172a',
  },
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  metaText: {
    fontSize: 12,
    color: '#64748b',
  },
  separator: {
    fontSize: 12,
    color: '#94a3b8',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 32,
  },
  emptyCard: {
    alignItems: 'center',
    padding: 32,
    backgroundColor: '#ffffff',
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#e2e8f0',
    width: '100%',
    maxWidth: 320,
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#0f172a',
    marginTop: 16,
    textAlign: 'center',
  },
  emptySubtitle: {
    fontSize: 14,
    color: '#64748b',
    marginTop: 8,
    textAlign: 'center',
    paddingHorizontal: 24,
  },
  becomeCreatorButton: {
    marginTop: 24,
    paddingVertical: 14,
    paddingHorizontal: 32,
    backgroundColor: '#6366f1',
    borderRadius: 12,
  },
  becomeCreatorButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 48,
  },
  emptyState: {
    alignItems: 'center',
    paddingVertical: 48,
  },
  uploadButton: {
    marginTop: 24,
    paddingVertical: 14,
    paddingHorizontal: 32,
    backgroundColor: '#6366f1',
    borderRadius: 12,
  },
  uploadButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  contentContainer: {
    padding: 16,
    paddingBottom: 100,
  },
});
