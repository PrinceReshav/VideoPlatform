import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image, ActivityIndicator, ScrollView, FlatList } from 'react-native';
import { Link, useRouter } from 'expo-router';
import { Clock, Heart, List, Search, ChevronRight } from 'lucide-react-native';
import { engagementApi } from '../../services/api/engagementApi';
import { playlistApi } from '../../services/api/playlistApi';
import { useQuery } from '@tanstack/react-query';

export default function LibraryScreen() {
  const router = useRouter();
  const [activeTab, setActiveTab] = React.useState<'history' | 'liked' | 'playlists' | 'watch-later'>('history');
  const [page, setPage] = React.useState(0);

  const { data: history, isLoading: historyLoading } = useQuery({
    queryKey: ['watchHistory', page],
    queryFn: () => engagementApi.getWatchHistory({ page, size: 20 }),
  });

  const { data: likedVideos, isLoading: likedLoading } = useQuery({
    queryKey: ['likedVideos', page],
    queryFn: () => engagementApi.getLikedVideos({ page, size: 20 }),
  });

  const { data: playlists, isLoading: playlistsLoading } = useQuery({
    queryKey: ['myPlaylists', page],
    queryFn: () => playlistApi.getMyPlaylists({ page, size: 20 }),
  });

  const { data: continueWatching } = useQuery({
    queryKey: ['continueWatching'],
    queryFn: () => engagementApi.getContinueWatching(),
  });

  const renderPlaylist = ({ item }: { item: any }) => (
    <TouchableOpacity style={styles.playlistCard} onPress={() => router.push(`/playlist/${item.id}`)}>
      <View style={styles.playlistThumbnail}><List size={28} color="#64748b" /></View>
      <View style={{ flex: 1, marginLeft: 12 }}>
        <Text style={styles.videoTitle} numberOfLines={2}>{item.name || item.title || 'Playlist'}</Text>
        <Text style={styles.metaText}>{item.videoCount ?? item.itemCount ?? 0} videos</Text>
      </View>
    </TouchableOpacity>
  );

  const formatViews = (count: number) => {
    if (count >= 1000000) return `${(count / 1000000).toFixed(1)}M`;
    if (count >= 1000) return `${(count / 1000).toFixed(1)}K`;
    return count.toString();
  };

  const formatDuration = (seconds?: number) => {
    if (!seconds) return '';
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
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
        {item.progressPercent && (
          <View style={styles.progressBarContainer}>
            <View
              style={[
                styles.progressBar,
                { width: `${item.progressPercent}%` },
              ]}
            />
          </View>
        )}
      </View>
      <View style={styles.videoInfo}>
        <Text style={styles.videoTitle} numberOfLines={2}>{item.title}</Text>
        <View style={styles.metaRow}>
          <Text style={styles.metaText}>{item.channelName}</Text>
          <Text style={styles.separator}>•</Text>
          <Text style={styles.metaText}>{formatViews(item.viewCount)} views</Text>
        </View>
      </View>
    </TouchableOpacity>
  );

  const tabs = [
    { id: 'history', label: 'History', icon: Clock },
    { id: 'liked', label: 'Liked', icon: Heart },
    { id: 'playlists', label: 'Playlists', icon: List },
    { id: 'watch-later', label: 'Watch Later', icon: Clock },
  ];

  return (
    <View style={styles.container}>
      {/* Tabs */}
      <View style={styles.tabsContainer}>
        {tabs.map((tab) => (
          <TouchableOpacity
            key={tab.id}
            style={[
              styles.tab,
              activeTab === tab.id && styles.tabActive,
            ]}
            onPress={() => setActiveTab(tab.id)}
          >
            <tab.icon size={20} color={activeTab === tab.id ? '#6366f1' : '#94a3b8'} />
            <Text style={[
              styles.tabLabel,
              activeTab === tab.id && styles.tabLabelActive,
            ]}>
              {tab.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* Continue Watching */}
      {continueWatching?.length > 0 && activeTab === 'history' && (
        <View style={styles.continueSection}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>Continue Watching</Text>
          </View>
          <FlatList
            data={continueWatching}
            renderItem={({ item }) => (
              <TouchableOpacity style={styles.continueCard} onPress={() => router.push(`/watch/${item.videoSlug}`)}>
                <View style={styles.continueThumbnail}>
                  {item.videoThumbnailUrl ? (
                    <Image source={{ uri: item.videoThumbnailUrl }} style={styles.continueThumbnailImage} />
                  ) : (
                    <View style={[styles.continueThumbnailImage, { backgroundColor: '#e2e8f0' }]} />
                  )}
                  <View style={styles.progressOverlay}>
                    <View
                      style={[
                        styles.progressBar,
                        { width: `${item.progressPercent}%` },
                      ]}
                    />
                  </View>
                </View>
                <View style={styles.continueInfo}>
                  <Text style={styles.continueTitle} numberOfLines={1}>{item.videoTitle}</Text>
                  <Text style={styles.continueMeta}>{item.channelName}</Text>
                  <View style={styles.progressInfo}>
                    <Clock size={12} color="#64748b" style={{ marginRight: 4 }} />
                    <Text style={styles.progressText}>{item.progressPercent}% watched</Text>
                  </View>
                </View>
              </TouchableOpacity>
            )}
            keyExtractor={(item) => item.id}
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.horizontalList}
          />
        </View>
      )}

      {/* Main Content */}
      <FlatList
        data={(activeTab === 'history' ? history?.content :
          activeTab === 'liked' ? likedVideos?.content :
          playlists?.content) || []}
        renderItem={activeTab === 'playlists' ? renderPlaylist : renderVideo}
        keyExtractor={(item) => item.id}
        numColumns={activeTab === 'playlists' ? 1 : 2}
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.contentContainer}
        refreshing={activeTab === 'history' ? historyLoading : activeTab === 'liked' ? likedLoading : playlistsLoading}
        onRefresh={() => {}}
        ListEmptyComponent={
          <View style={styles.emptyState}>
            {activeTab === 'history' && <Clock size={48} color="#94a3b8" style={{ opacity: 0.5, marginBottom: 16 }} />}
            {activeTab === 'liked' && <Heart size={48} color="#94a3b8" style={{ opacity: 0.5, marginBottom: 16 }} />}
            {activeTab === 'playlists' && <List size={48} color="#94a3b8" style={{ opacity: 0.5, marginBottom: 16 }} />}
            <Text style={styles.emptyTitle}>
              {activeTab === 'history' ? 'No watch history' :
               activeTab === 'liked' ? 'No liked videos' :
               'No playlists yet'}
            </Text>
            <Text style={styles.emptySubtitle}>
              {activeTab === 'history' ? 'Videos you watch will appear here' :
               activeTab === 'liked' ? 'Like videos to save them here' :
               'Create your first playlist'}
            </Text>
          </View>
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8fafc',
  },
  tabsContainer: {
    flexDirection: 'row',
    backgroundColor: '#ffffff',
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
    paddingHorizontal: 16,
  },
  tab: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingVertical: 16,
    paddingHorizontal: 8,
    borderBottomWidth: 2,
    borderBottomColor: 'transparent',
  },
  tabActive: {
    borderBottomColor: '#6366f1',
  },
  tabLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#94a3b8',
  },
  tabLabelActive: {
    color: '#6366f1',
  },
  continueSection: {
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  sectionHeader: {
    marginBottom: 12,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#0f172a',
  },
  horizontalList: {
    paddingHorizontal: 16,
    gap: 12,
  },
  continueCard: {
    width: 180,
  },
  continueThumbnail: {
    position: 'relative',
    borderRadius: 12,
    overflow: 'hidden',
    marginBottom: 8,
    backgroundColor: '#e2e8f0',
  },
  continueThumbnailImage: {
    width: '100%',
    aspectRatio: 16 / 9,
  },
  progressOverlay: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    height: 4,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
  },
  progressBarContainer: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    height: 3,
    backgroundColor: 'rgba(0, 0, 0, 0.3)',
  },
  progressBar: {
    height: '100%',
    backgroundColor: '#6366f1',
    borderRadius: 0,
  },
  continueInfo: {
    gap: 4,
  },
  continueTitle: {
    fontSize: 13,
    fontWeight: '600',
    color: '#0f172a',
  },
  continueMeta: {
    fontSize: 12,
    color: '#64748b',
  },
  progressInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  progressText: {
    fontSize: 11,
    color: '#64748b',
  },
  contentContainer: {
    padding: 16,
    paddingBottom: 100,
  },
  emptyState: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 32,
    paddingVertical: 48,
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
    fontSize: 12,
    fontWeight: '600',
  },
  videoInfo: {
    gap: 4,
  },
  videoTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#0f172a',
  },
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  metaText: {
    fontSize: 13,
    color: '#64748b',
  },
  separator: {
    fontSize: 12,
    color: '#94a3b8',
  },
  playlistCard: {
    flexDirection: 'row',
    padding: 16,
    backgroundColor: '#ffffff',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#e2e8f0',
  },
  playlistThumbnail: {
    width: 120,
    height: 68,
    borderRadius: 8,
    overflow: 'hidden',
    marginRight: 16,
  },
  playlistInfo: {
    flex: 1,
    justifyContent: 'center',
  },
  playlistTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#0f172a',
    marginBottom: 4,
  },
  playlistMeta: {
    fontSize: 13,
    color: '#64748b',
  },
});
