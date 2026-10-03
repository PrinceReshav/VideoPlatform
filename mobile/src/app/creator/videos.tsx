import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image, ActivityIndicator, FlatList, RefreshControl, Modal, TextInput, KeyboardAvoidingView, Platform } from 'react-native';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link, useRouter } from 'expo-router';
import { Upload, Video, Trash2, Edit, Eye, MoreHorizontal, ChevronRight, Filter } from 'lucide-react-native';
import { videoApi } from '../../services/api/videoApi';
import { uploadApi } from '../../services/api/uploadApi';
import * as DocumentPicker from 'expo-document-picker';
import * as FileSystem from 'expo-file-system';

export default function MyVideosScreen() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [filter, setFilter] = React.useState<'all' | 'published' | 'draft' | 'processing' | 'failed'>('all');
  const [page, setPage] = React.useState(0);
  const [showFilterModal, setShowFilterModal] = React.useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = React.useState<string | null>(null);

  const { data: videos, isLoading } = useQuery({
    queryKey: ['myCreatorVideos', filter, page],
    queryFn: () => videoApi.getMyVideos({ page, size: 20, filter }),
  });

  const deleteMutation = useMutation({
    mutationFn: (videoId: string) => videoApi.deleteVideo(videoId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myCreatorVideos'] });
      setShowDeleteConfirm(null);
    },
  });

  const publishMutation = useMutation({
    mutationFn: ({ videoId, action }: { videoId: string; action: 'publish' | 'unpublish' }) =>
      action === 'publish' ? videoApi.publishVideo(videoId) : videoApi.unpublishVideo(videoId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['myCreatorVideos'] }),
  });

  const handleUpload = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: ['video/mp4', 'video/quicktime', 'video/x-matroska', 'video/webm'],
        copyToCacheDirectory: true,
      });
      
      if (!result.canceled && result.assets[0]) {
        const asset = result.assets[0];
        // Navigate to upload screen with the selected file
        router.push({
          pathname: '/creator/upload',
          params: { fileUri: asset.uri, fileName: asset.name, fileSize: asset.size, mimeType: asset.mimeType },
        });
      }
    } catch (error) {
      console.error('File picker error:', error);
    }
  };

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

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <View style={styles.headerContent}>
          <Text style={styles.headerTitle}>My Videos</Text>
          <TouchableOpacity onPress={handleUpload} style={styles.uploadButton}>
            <Upload size={20} color="#ffffff" />
          </TouchableOpacity>
        </View>
      </View>

      {/* Filter */}
      <View style={styles.filterContainer}>
        <TouchableOpacity onPress={() => setShowFilterModal(true)} style={styles.filterButton}>
          <Filter size={20} color="#64748b" style={{ marginRight: 8 }} />
          <Text style={styles.filterButtonText}>Filter: {filter.charAt(0).toUpperCase() + filter.slice(1)}</Text>
          <ChevronRight size={16} color="#64748b" />
        </TouchableOpacity>
      </View>

      {/* Videos List */}
      <FlatList
        data={videos?.content || []}
        renderItem={renderVideo}
        keyExtractor={(item) => item.id}
        numColumns={2}
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.gridList}
        refreshing={isLoading}
        onRefresh={() => queryClient.invalidateQueries({ queryKey: ['myCreatorVideos'] })}
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <Video size={48} color="#94a3b8" style={{ opacity: 0.5, marginBottom: 16 }} />
            <Text style={styles.emptyTitle}>No videos yet</Text>
            <Text style={styles.emptySubtitle}>Upload your first video to get started</Text>
            <TouchableOpacity style={styles.uploadButtonLarge} onPress={handleUpload}>
              <Text style={styles.uploadButtonText}>Upload Video</Text>
            </TouchableOpacity>
          </View>
        }
        ListFooterComponent={
          videos?.totalPages > 1 && (
            <View style={styles.pagination}>
              <TouchableOpacity
                style={[styles.pageButton, page === 0 && styles.pageButtonDisabled]}
                onPress={() => setPage(p => p - 1)}
                disabled={page === 0}
              >
                <Text style={styles.pageButtonText}>Previous</Text>
              </TouchableOpacity>
              <Text style={styles.pageInfo}>Page {page + 1} of {videos.totalPages}</Text>
              <TouchableOpacity
                style={[styles.pageButton, page >= videos.totalPages - 1 && styles.pageButtonDisabled]}
                onPress={() => setPage(p => p + 1)}
                disabled={page >= videos.totalPages - 1}
              >
                <Text style={styles.pageButtonText}>Next</Text>
              </TouchableOpacity>
            </View>
          )
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
  header: {
    backgroundColor: '#ffffff',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
  },
  headerContent: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: '700',
    color: '#0f172a',
  },
  uploadButton: {
    width: 40,
    height: 40,
    borderRadius: 10,
    backgroundColor: '#6366f1',
    justifyContent: 'center',
    alignItems: 'center',
  },
  filterContainer: {
    padding: 16,
    backgroundColor: '#ffffff',
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
  },
  filterButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 10,
    backgroundColor: '#f1f5f9',
    borderRadius: 8,
  },
  filterButtonText: {
    fontSize: 14,
    fontWeight: '500',
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
  emptyState: {
    alignItems: 'center',
    paddingVertical: 48,
    paddingHorizontal: 32,
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
  uploadButtonLarge: {
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
  pagination: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
    paddingVertical: 16,
  },
  pageButton: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    backgroundColor: '#6366f1',
    borderRadius: 8,
  },
  pageButtonDisabled: {
    opacity: 0.5,
  },
  pageButtonText: {
    color: '#ffffff',
    fontWeight: '600',
  },
  pageInfo: {
    color: '#64748b',
    fontSize: 14,
  },
});