import React from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, Image, ActivityIndicator, RefreshControl, ScrollView } from 'react-native';
import { useQuery } from '@tanstack/react-query';
import { Link, useRouter } from 'expo-router';
import { Play, Clock, Eye, Users, Search, Flame } from 'lucide-react-native';
import { videoApi } from '../../services/api/videoApi';

export default function HomeScreen() {
  const router = useRouter();
  const { data: videos, isLoading, refetch } = useQuery({
    queryKey: ['videos', 'home'],
    queryFn: () => videoApi.getPublished({ page: 0, size: 20 }),
  });

  const { data: trending } = useQuery({
    queryKey: ['videos', 'trending'],
    queryFn: () => videoApi.getPublished({ page: 0, size: 10, sort: 'viewCount' }),
  });

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

  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#6366f1" />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <ScrollView
        refreshControl={
          <RefreshControl
            refreshing={isLoading}
            onRefresh={() => refetch()}
            colors={['#6366f1']}
          />
        }
        contentContainerStyle={styles.contentContainer}
      >
        {/* Trending Section */}
        {trending?.content?.length > 0 && (
          <View style={styles.section}>
            <View style={styles.sectionHeader}>
              <View style={styles.sectionIcon}>
                <Flame size={20} color="#f59e0b" />
              </View>
              <View>
                <Text style={styles.sectionTitle}>Trending Now</Text>
                <Text style={styles.sectionSubtitle}>Most viewed this week</Text>
              </View>
            </View>
            <FlatList
              data={trending.content}
              renderItem={renderVideo}
              keyExtractor={(item) => item.id}
              horizontal
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={styles.horizontalList}
            />
          </View>
        )}

        {/* Latest Videos Section */}
        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <View style={styles.sectionIcon}>
              <Clock size={20} color="#3b82f6" />
            </View>
            <View>
              <Text style={styles.sectionTitle}>Latest Videos</Text>
              <Text style={styles.sectionSubtitle}>Just uploaded</Text>
            </View>
          </View>
          <FlatList
            data={videos?.content || []}
            renderItem={renderVideo}
            keyExtractor={(item) => item.id}
            numColumns={2}
            showsVerticalScrollIndicator={false}
            contentContainerStyle={styles.gridList}
          />
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8fafc',
  },
  contentContainer: {
    padding: 16,
    paddingBottom: 100,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  section: {
    marginBottom: 24,
  },
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 12,
  },
  sectionIcon: {
    width: 32,
    height: 32,
    borderRadius: 8,
    backgroundColor: '#f59e0b1a',
    justifyContent: 'center',
    alignItems: 'center',
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#0f172a',
  },
  sectionSubtitle: {
    fontSize: 12,
    color: '#64748b',
  },
  horizontalList: {
    paddingHorizontal: 16,
    gap: 12,
  },
  gridList: {
    paddingHorizontal: 16,
    gap: 12,
  },
  videoCard: {
    width: 160,
  },
  thumbnailContainer: {
    position: 'relative',
    borderRadius: 12,
    overflow: 'hidden',
    marginBottom: 8,
  },
  thumbnail: {
    width: '100%',
    aspectRatio: 16/9,
  },
  durationBadge: {
    position: 'absolute',
    bottom: 4,
    right: 4,
    backgroundColor: 'rgba(0, 0, 0, 0.9)',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  durationText: {
    color: '#ffffff',
    fontSize: 11,
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
    fontSize: 12,
    color: '#64748b',
  },
  separator: {
    fontSize: 12,
    color: '#94a3b8',
  },
});