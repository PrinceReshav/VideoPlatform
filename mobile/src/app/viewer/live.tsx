import React from 'react';
import { ActivityIndicator, FlatList, Image, Modal, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Heart, Play, Search, Tv, X } from 'lucide-react-native';
import Video from 'react-native-video';
import { liveTvApi, LiveChannel } from '../../services/api/liveTvApi';

const statusLabel = (status: LiveChannel['status']) => {
  switch (status) {
    case 'playable': return 'PLAYABLE';
    case 'degraded': return 'DEGRADED';
    case 'checking': return 'CHECKING';
    case 'no_source': return 'NO SOURCE';
    default: return 'UNKNOWN';
  }
};

const streamUrl = (channel: LiveChannel) => channel.hlsUrl || channel.url;

export default function LiveTvScreen() {
  const queryClient = useQueryClient();
  const [search, setSearch] = React.useState('');
  const [country, setCountry] = React.useState<string | undefined>();
  const [category, setCategory] = React.useState<string | undefined>();
  const [selected, setSelected] = React.useState<LiveChannel | null>(null);

  const channels = useQuery({
    queryKey: ['mobile-live-tv', country, category, search],
    queryFn: () => liveTvApi.getChannels({ country, category, search: search.trim() || undefined, page: 0, size: 50, sort: 'name,asc' }),
  });

  const countries = useQuery({
    queryKey: ['mobile-live-tv-countries', category, search],
    queryFn: () => liveTvApi.getCountries({ category, search: search.trim() || undefined }),
  });

  const categories = useQuery({
    queryKey: ['mobile-live-tv-categories', country, search],
    queryFn: () => liveTvApi.getCategories({ country, search: search.trim() || undefined }),
  });

  const favorite = useMutation({
    mutationFn: (id: string) => liveTvApi.toggleFavorite(id),
    onSuccess: (updated) => {
      queryClient.setQueryData(['mobile-live-tv', country, category, search], (old: any) => {
        if (!old) return old;
        return { ...old, content: old.content.map((c: LiveChannel) => c.id === updated.id ? updated : c) };
      });
      setSelected(current => current?.id === updated.id ? updated : current);
    },
  });

  const openChannel = async (channel: LiveChannel) => {
    setSelected(channel);
    try { await liveTvApi.recordView(channel.id); } catch { /* playback remains available */ }
  };

  const renderChannel = ({ item }: { item: LiveChannel }) => {
    const playable = item.status === 'playable' || item.status === 'degraded';
    return (
      <Pressable style={styles.card} onPress={() => openChannel(item)}>
        <View style={styles.logoWrap}>
          {item.logo || item.tvgLogo ? <Image source={{ uri: item.logo || item.tvgLogo }} style={styles.logo} resizeMode="contain" /> : <Tv size={28} color="#64748b" />}
          <View style={[styles.statusDot, { backgroundColor: item.status === 'playable' ? '#22c55e' : item.status === 'degraded' ? '#f59e0b' : '#94a3b8' }]} />
        </View>
        <View style={styles.cardBody}>
          <Text style={styles.cardTitle} numberOfLines={2}>{item.name}</Text>
          <Text style={styles.cardMeta} numberOfLines={1}>{item.country} • {item.category}</Text>
          <Text style={styles.status}>{statusLabel(item.status)}{item.accessType === 'PROTECTED' ? ' • PROTECTED' : ''}</Text>
        </View>
        <Pressable hitSlop={10} onPress={() => favorite.mutate(item.id)} style={styles.heart}>
          <Heart size={20} color={item.favorite ? '#e11d48' : '#94a3b8'} fill={item.favorite ? '#e11d48' : 'transparent'} />
        </Pressable>
        <Play size={18} color={playable ? '#6366f1' : '#cbd5e1'} />
      </Pressable>
    );
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <View style={styles.titleRow}><Tv size={24} color="#6366f1" /><View><Text style={styles.title}>Live TV</Text><Text style={styles.subtitle}>{channels.data?.totalElements ?? 0} channels</Text></View></View>
        <View style={styles.searchBar}><Search size={18} color="#94a3b8" /><TextInput value={search} onChangeText={setSearch} placeholder="Search channels" placeholderTextColor="#94a3b8" style={styles.searchInput} /></View>
      </View>

      <View style={styles.filterSection}>
        <FlatList horizontal showsHorizontalScrollIndicator={false} data={[{ code: undefined, name: 'All countries' }, ...(countries.data || [])]} keyExtractor={(x, i) => `${x.code || 'all'}-${i}`} renderItem={({ item }) => (
          <Pressable onPress={() => setCountry(item.code)} style={[styles.chip, country === item.code && styles.chipActive]}><Text style={[styles.chipText, country === item.code && styles.chipTextActive]}>{item.name}</Text></Pressable>
        )} />
        <FlatList horizontal showsHorizontalScrollIndicator={false} data={[{ name: 'All categories' }, ...(categories.data || [])]} keyExtractor={(x, i) => `${x.name}-${i}`} renderItem={({ item }) => (
          <Pressable onPress={() => setCategory(item.name === 'All categories' ? undefined : item.name)} style={[styles.chip, (category === item.name || (!category && item.name === 'All categories')) && styles.chipActive]}><Text style={[styles.chipText, (!category && item.name === 'All categories') && styles.chipTextActive]}>{item.name}</Text></Pressable>
        )} />
      </View>

      {channels.isLoading ? <View style={styles.center}><ActivityIndicator size="large" color="#6366f1" /></View> : channels.isError ? <View style={styles.center}><Text style={styles.error}>Unable to load Live TV.</Text><Text style={styles.muted}>Check the API gateway and try again.</Text></View> : (
        <FlatList data={channels.data?.content || []} keyExtractor={item => item.id} renderItem={renderChannel} contentContainerStyle={styles.list} refreshing={channels.isRefetching} onRefresh={() => channels.refetch()} ListEmptyComponent={<View style={styles.center}><Text style={styles.error}>No channels found.</Text></View>} />
      )}

      <Modal visible={!!selected} animationType="slide" onRequestClose={() => setSelected(null)}>
        {selected && <View style={styles.playerScreen}>
          <View style={styles.playerHeader}><View style={{ flex: 1 }}><Text style={styles.playerTitle} numberOfLines={1}>{selected.name}</Text><Text style={styles.playerMeta}>{selected.country} • {selected.category}</Text></View><Pressable onPress={() => setSelected(null)}><X size={26} color="#fff" /></Pressable></View>
          <View style={styles.player}>
            <Video source={{ uri: streamUrl(selected) }} style={StyleSheet.absoluteFill} controls resizeMode="contain" paused={false} playInBackground={false} onError={() => {}} />
          </View>
          <View style={styles.playerFooter}><Text style={styles.muted}>{statusLabel(selected.status)}</Text><Pressable onPress={() => favorite.mutate(selected.id)} style={styles.saveButton}><Heart size={18} color="#fff" fill={selected.favorite ? '#fff' : 'transparent'} /><Text style={styles.saveText}>{selected.favorite ? 'Saved' : 'Save'}</Text></Pressable></View>
        </View>}
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8fafc' },
  header: { padding: 16, backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#e2e8f0' },
  titleRow: { flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 14 },
  title: { fontSize: 22, fontWeight: '800', color: '#0f172a' },
  subtitle: { color: '#64748b', fontSize: 12, marginTop: 2 },
  searchBar: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#f1f5f9', borderRadius: 12, paddingHorizontal: 12 },
  searchInput: { flex: 1, paddingVertical: 11, paddingHorizontal: 8, color: '#0f172a' },
  filterSection: { backgroundColor: '#fff', paddingBottom: 8 },
  chip: { marginLeft: 12, marginVertical: 5, paddingHorizontal: 12, paddingVertical: 8, borderRadius: 20, backgroundColor: '#f1f5f9' },
  chipActive: { backgroundColor: '#6366f1' },
  chipText: { color: '#475569', fontSize: 12, fontWeight: '600' },
  chipTextActive: { color: '#fff' },
  list: { padding: 12, paddingBottom: 100 },
  card: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff', borderRadius: 14, padding: 10, marginBottom: 10, borderWidth: 1, borderColor: '#e2e8f0' },
  logoWrap: { width: 68, height: 52, borderRadius: 10, backgroundColor: '#f1f5f9', justifyContent: 'center', alignItems: 'center', position: 'relative' },
  logo: { width: 58, height: 44 },
  statusDot: { position: 'absolute', right: 4, top: 4, width: 8, height: 8, borderRadius: 4 },
  cardBody: { flex: 1, marginLeft: 10, marginRight: 8 },
  cardTitle: { fontSize: 14, fontWeight: '700', color: '#0f172a' },
  cardMeta: { fontSize: 11, color: '#64748b', marginTop: 3 },
  status: { fontSize: 9, color: '#6366f1', fontWeight: '800', marginTop: 4 },
  heart: { padding: 7, marginRight: 4 },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 30 },
  error: { fontSize: 16, fontWeight: '700', color: '#334155' },
  muted: { color: '#94a3b8', fontSize: 12, marginTop: 5 },
  playerScreen: { flex: 1, backgroundColor: '#05070a' },
  playerHeader: { flexDirection: 'row', alignItems: 'center', padding: 16, paddingTop: 22, backgroundColor: '#0f172a' },
  playerTitle: { color: '#fff', fontSize: 17, fontWeight: '800' },
  playerMeta: { color: '#94a3b8', fontSize: 11, marginTop: 3 },
  player: { width: '100%', aspectRatio: 16 / 9, backgroundColor: '#000' },
  playerFooter: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: 16 },
  saveButton: { flexDirection: 'row', alignItems: 'center', gap: 7, backgroundColor: '#6366f1', borderRadius: 10, paddingHorizontal: 14, paddingVertical: 9 },
  saveText: { color: '#fff', fontWeight: '700' },
});
