import React from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, Image, ActivityIndicator, RefreshControl, ScrollView } from 'react-native';
import { useQuery } from '@tanstack/react-query';
import { Link, useRouter } from 'expo-router';
import { Video, Users, Clock, Bell, ChevronRight } from 'lucide-react-native';
import { videoApi } from '../../services/api/videoApi';
import { subscriptionApi } from '../../services/api/subscriptionApi';

export default function SubscriptionsScreen() {
  const router = useRouter();
  const { data: feed, isLoading } = useQuery({
    queryKey: ['subscriptionFeed'],
    queryFn: () => subscriptionApi.getFeed({ page: 0, size: 20 }),
  });

  const { data: subscriptions, isLoading: subsLoading } = useQuery({
    queryKey: ['mySubscriptions'],
    queryFn: () => subscriptionApi.getMySubscriptions({ page: 0, size: 50 }),
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

  return (
    <View style={styles.container}>
      <ScrollView
        refreshControl={
          <RefreshControl
            refreshing={isLoading}
            onRefresh={() => {}}
            colors={['#6366f1']}
          />
        }
        contentContainerStyle={styles.contentContainer}
      >
        {/* Subscribed Channels */}
        {!subsLoading && subscriptions?.content?.length > 0 && (
          <View style={styles.section}>
            <View style={styles.sectionHeader}>
              <View style={styles.sectionIcon}>
                <Users size={20} color="#6366f1" />
              </View>
              <View>
                <Text style={styles.sectionTitle}>Subscribed Channels</Text>
                <Text style={styles.sectionSubtitle}>{subscriptions.content.length} channels</Text>
              </View>
            </View>
            <FlatList
              data={subscriptions.content}
              renderItem={({ item }) => (
                <TouchableOpacity style={styles.channelCard} onPress={() => router.push(`/channel/${item.slug}`)}>
                  <View style={styles.channelAvatar}>
                    {item.avatarUrl ? (
                      <Image source={{ uri: item.avatarUrl }} style={styles.channelAvatarImage} />
                    ) : (
                      <View style={[styles.channelAvatarImage, { backgroundColor: '#e2e8f0' }]} />
                    )}
                  </View>
                  <Text style={styles.channelName} numberOfLines={1}>{item.name}</Text>
                </TouchableOpacity>
              )}
              keyExtractor={(item) => item.id}
              horizontal
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={styles.horizontalList}
            />
          </View>
        )}

        {/* Feed */}
        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <View style={styles.sectionIcon}>
              <Video size={20} color="#3b82f6" />
            </View>
            <View>
              <Text style={styles.sectionTitle}>Subscription Feed</Text>
              <Text style={styles.sectionSubtitle}>Latest from your subscriptions</Text>
            </View>
          </View>

          {isLoading ? (
            <View style={styles.loadingContainer}>
              <ActivityIndicator size="large" color="#6366f1" />
            </View>
          ) : feed?.content?.length > 0 ? (
            <FlatList
              data={feed.content}
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
              <Text style={styles.emptySubtitle}>Subscribe to channels to see their content here</Text>
            </View>
          )}
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
    paddingVertical: 48,
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
    backgroundColor: '#6366f11a',
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
  channelCard: {
    width: 80,
    alignItems: 'center',
  },
  channelAvatar: {
    width: 64,
    height: 64,
    borderRadius: 32,
    overflow: 'hidden',
    marginBottom: 8,
    borderWidth: 2,
    borderColor: '#e2e8f0',
  },
  channelAvatarImage: {
    width: '100%',
    height: '100%',
  },
  channelName: {
    fontSize: 12,
    fontWeight: '500',
    color: '#0f172a',
    textAlign: 'center',
    width: 80,
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
});
