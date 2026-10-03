import React from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, Image, ActivityIndicator, TextInput, KeyboardAvoidingView, Platform, ScrollView } from 'react-native';
import { useQuery } from '@tanstack/react-query';
import { Link, useRouter } from 'expo-router';
import { Search, Filter, Loader2, Video, Users, Clock, Heart, List, Settings, ChevronRight, X } from 'lucide-react-native';
import { videoApi } from '../../services/api/videoApi';
import { subscriptionApi } from '../../services/api/subscriptionApi';
import { engagementApi } from '../../services/api/engagementApi';

export default function SearchScreen() {
  const router = useRouter();
  const [query, setQuery] = React.useState('');
  const [suggestions, setSuggestions] = React.useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = React.useState(false);

  const { data: suggestionsResponse } = useQuery({
    queryKey: ['search-suggestions', query],
    queryFn: () => videoApi.getSuggestions(query),
    enabled: query.length >= 2,
    retry: false,
  });
  React.useEffect(() => setSuggestions(suggestionsResponse?.suggestions ?? []), [suggestionsResponse]);

  const { data: results, isLoading } = useQuery({
    queryKey: ['search', query],
    queryFn: () => videoApi.search({ q: query, size: 20 }),
    enabled: query.length >= 2,
  });

  const formatViews = (count: number = 0) => count >= 1000000 ? `${(count/1000000).toFixed(1)}M` : count >= 1000 ? `${(count/1000).toFixed(1)}K` : String(count);
  const formatDuration = (seconds?: number) => seconds ? `${Math.floor(seconds/60)}:${String(seconds%60).padStart(2,'0')}` : '';

  const handleSearch = () => {
    if (query.trim()) {
      setShowSuggestions(false);
    }
  };

  const handleSuggestionPress = (suggestion: string) => {
    setQuery(suggestion);
    setShowSuggestions(false);
    handleSearch();
  };

  return (
    <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
      <View style={styles.searchContainer}>
        <View style={styles.searchBar}>
          <Search size={24} color="#94a3b8" style={styles.searchIcon} />
          <TextInput
            style={styles.searchInput}
            placeholder="Search videos, channels, topics..."
            value={query}
            onChangeText={(text) => {
              setQuery(text);
              setShowSuggestions(text.length >= 2);
            }}
            onSubmitEditing={handleSearch}
            autoCapitalize="none"
            autoCorrect={false}
          />
          {query && (
            <TouchableOpacity onPress={() => setQuery('')} style={styles.clearButton}>
              <X size={24} color="#94a3b8" />
            </TouchableOpacity>
          )}
        </View>

        {showSuggestions && suggestions.length > 0 && (
          <View style={styles.suggestionsList}>
            {suggestions.map((s) => (
              <TouchableOpacity key={s} onPress={() => handleSuggestionPress(s)} style={styles.suggestionItem}>
                <Search size={20} color="#64748b" style={{ marginRight: 12 }} />
                <Text style={styles.suggestionText}>{s}</Text>
              </TouchableOpacity>
            ))}
          </View>
        )}
      </View>

      {query && (
        <View style={styles.resultsContainer}>
          <View style={styles.resultsHeader}>
            <Text style={styles.resultsTitle}>Results for "{query}"</Text>
            <TouchableOpacity>
              <Filter size={24} color="#6366f1" />
            </TouchableOpacity>
          </View>

          {isLoading ? (
            <View style={styles.loadingContainer}>
              <ActivityIndicator size="large" color="#6366f1" />
            </View>
          ) : results?.content?.length > 0 ? (
            <FlatList
              data={results.content}
              renderItem={({ item }) => (
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
              )}
              keyExtractor={(item) => item.id}
              contentContainerStyle={styles.listContent}
              showsVerticalScrollIndicator={false}
            />
          ) : (
            <View style={styles.emptyState}>
              <Video size={48} color="#94a3b8" style={{ opacity: 0.5, marginBottom: 16 }} />
              <Text style={styles.emptyTitle}>No results found</Text>
              <Text style={styles.emptySubtitle}>Try different keywords or check your spelling</Text>
            </View>
          )}
        </View>
      )}
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8fafc',
  },
  searchContainer: {
    padding: 16,
    backgroundColor: '#ffffff',
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
  },
  searchBar: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#f1f5f9',
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  searchIcon: {
    marginRight: 12,
  },
  searchInput: {
    flex: 1,
    fontSize: 16,
    color: '#0f172a',
  },
  clearButton: {
    padding: 4,
  },
  suggestionsList: {
    marginTop: 12,
    backgroundColor: '#ffffff',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#e2e8f0',
    overflow: 'hidden',
  },
  suggestionItem: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
  },
  suggestionText: {
    fontSize: 16,
    color: '#0f172a',
  },
  resultsContainer: {
    flex: 1,
  },
  resultsHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: 16,
  },
  resultsTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#0f172a',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 48,
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
  listContent: {
    paddingHorizontal: 16,
    paddingBottom: 100,
    gap: 12,
  },
  videoCard: {
    width: '100%',
  },
  thumbnailContainer: {
    position: 'relative',
    borderRadius: 12,
    overflow: 'hidden',
    marginBottom: 12,
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
    gap: 8,
  },
  videoTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#0f172a',
  },
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  metaText: {
    fontSize: 13,
    color: '#64748b',
  },
  separator: {
    fontSize: 13,
    color: '#94a3b8',
  },
});
