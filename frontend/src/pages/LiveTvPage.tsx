import { useEffect, useMemo, useState, useDeferredValue } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  AlertCircle,
  ChevronLeft,
  ChevronRight,
  Heart,
  Loader2,
  Play,
  RefreshCw,
  Search,
  Tv,
  X,
} from 'lucide-react';
import { clsx } from 'clsx';
import { liveTvApi, type CategoryInfo, type CountryInfo, type LiveChannel, type LiveTvResponse } from '../services/api/liveTvApi';
import { VideoPlayer } from '../components/video/VideoPlayer';
import { Button } from '../components/ui/Button';

const PAGE_SIZE = 50;
type SortField = 'name' | 'country' | 'category' | 'status';
type SortDirection = 'asc' | 'desc';

function flag(code?: string | null) {
  const value = code?.trim().toUpperCase();
  if (!value || !/^[A-Z]{2}$/.test(value)) return '🌍';
  return String.fromCodePoint(...value.split('').map(c => 127397 + c.charCodeAt(0)));
}

function logoOf(channel: LiveChannel) {
  return channel.logo?.trim() || channel.tvgLogo?.trim() || undefined;
}

function fallback(name?: string | null) {
  const initial = name?.trim().charAt(0).toUpperCase() || '?';
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="128" height="128"><rect width="128" height="128" rx="16" fill="#1e293b"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" font-family="Arial" font-size="52" font-weight="700" fill="#94a3b8">${initial}</text></svg>`;
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
}

function statusClass(status?: LiveChannel['status']) {
  switch (status) {
    case 'online': return 'bg-emerald-500/15 text-emerald-600';
    case 'offline': return 'bg-red-500/15 text-red-600';
    case 'checking': return 'bg-amber-500/15 text-amber-600';
    default: return 'bg-slate-500/15 text-slate-500';
  }
}

interface ChannelCardProps {
  channel: LiveChannel;
  isPlaying: boolean;
  onPlay: () => void;
  onFavorite: () => void;
  favoritePending: boolean;
}

function ChannelCard({ channel, isPlaying, onPlay, onFavorite, favoritePending }: ChannelCardProps) {
  const image = logoOf(channel);
  const favorite = channel.favorite === true;

  return (
    <div
      className={clsx(
        'group flex items-center gap-3 rounded-xl border bg-[var(--color-surface)] p-3 transition-all',
        isPlaying
          ? 'border-[var(--color-primary)] ring-1 ring-[var(--color-primary)]/30'
          : 'border-[var(--color-border)] hover:border-[var(--color-primary)]/40 hover:bg-[var(--color-bg-hover)]'
      )}
    >
      <button type="button" onClick={onPlay} className="relative flex-shrink-0 rounded-lg focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]" aria-label={`Play ${channel.name}`}>
        <img
          src={image || fallback(channel.name)}
          alt=""
          loading="lazy"
          className="h-14 w-14 rounded-lg bg-[var(--color-bg-tertiary)] object-contain"
          onError={event => {
            const img = event.currentTarget;
            if (!img.dataset.fallback) {
              img.dataset.fallback = 'true';
              img.src = fallback(channel.name);
            }
          }}
        />
        <span className="absolute inset-0 flex items-center justify-center rounded-lg bg-black/0 transition group-hover:bg-black/40">
          <Play className="h-5 w-5 text-white opacity-0 group-hover:opacity-100" fill="currentColor" />
        </span>
      </button>

      <button type="button" onClick={onPlay} className="min-w-0 flex-1 text-left">
        <div className="mb-1 flex items-center gap-2">
          <h3 className="truncate font-medium text-[var(--color-text-primary)]" title={channel.name}>{channel.name}</h3>
          <span className={clsx('flex-shrink-0 rounded-full px-2 py-0.5 text-[11px] font-semibold capitalize', statusClass(channel.status))}>
            {channel.status || 'unknown'}
          </span>
        </div>
        <div className="flex flex-wrap gap-x-3 gap-y-1 text-xs text-[var(--color-text-tertiary)]">
          {channel.country && <span>{flag(channel.countryCode)} {channel.country}</span>}
          {channel.category && <span>{channel.category}</span>}
          {channel.language && channel.language !== 'Unknown' && <span>{channel.language}</span>}
          {channel.accessType && (
            <span className={clsx(
              'rounded-full px-2 py-0.5 text-[11px] font-semibold',
              channel.accessType === 'PUBLIC'
                ? 'bg-sky-500/15 text-sky-600'
                : 'bg-amber-500/15 text-amber-600'
            )}>
              {channel.accessType === 'PUBLIC' ? 'Public stream' : 'Protected stream'}
            </span>
          )}
        </div>
      </button>

      <Button
        variant="ghost"
        size="icon"
        onClick={onFavorite}
        loading={favoritePending}
        aria-label={favorite ? `Remove ${channel.name} from favorites` : `Add ${channel.name} to favorites`}
        title={favorite ? 'Remove favorite' : 'Add favorite'}
      >
        <Heart className={clsx('h-5 w-5', favorite && 'fill-current text-[var(--color-error)]')} />
      </Button>
    </div>
  );
}

function ChannelSkeleton() {
  return (
    <div className="flex items-center gap-3 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-3 animate-pulse">
      <div className="h-14 w-14 rounded-lg bg-[var(--color-bg-tertiary)]" />
      <div className="flex-1 space-y-2">
        <div className="h-4 w-2/3 rounded bg-[var(--color-bg-tertiary)]" />
        <div className="h-3 w-1/3 rounded bg-[var(--color-bg-tertiary)]" />
      </div>
    </div>
  );
}

function LiveTvPage() {
  const queryClient = useQueryClient();
  const [sortBy, setSortBy] = useState<SortField>('name');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCountry, setSelectedCountry] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [page, setPage] = useState(0);
  const [selectedChannel, setSelectedChannel] = useState<LiveChannel | null>(null);
  const [favoritePendingId, setFavoritePendingId] = useState<string | null>(null);
  const deferredSearch = useDeferredValue(searchTerm.trim());

  useEffect(() => {
    setPage(0);
  }, [deferredSearch, selectedCountry, selectedCategory, sortBy, sortDirection]);

  const countriesQuery = useQuery<CountryInfo[]>({
    queryKey: ['liveCountries', selectedCategory, deferredSearch],
    queryFn: () => liveTvApi.getCountries({
      category: selectedCategory || undefined,
      search: deferredSearch || undefined,
    }),
    staleTime: 5 * 60 * 1000,
  });

  const categoriesQuery = useQuery<CategoryInfo[]>({
    queryKey: ['liveCategories', selectedCountry, deferredSearch],
    queryFn: () => liveTvApi.getCategories({
      country: selectedCountry || undefined,
      search: deferredSearch || undefined,
    }),
    staleTime: 5 * 60 * 1000,
  });

  const channelsQuery = useQuery({
    queryKey: ['liveChannels', page, deferredSearch, selectedCountry, selectedCategory, sortBy, sortDirection],
    queryFn: () => liveTvApi.getChannels({
      page,
      size: PAGE_SIZE,
      search: deferredSearch || undefined,
      country: selectedCountry || undefined,
      category: selectedCategory || undefined,
      sort: `${sortBy},${sortDirection}`,
    }),
    placeholderData: previous => previous,
  });

  const channels = channelsQuery.data?.content ?? [];
  const totalPages = channelsQuery.data?.totalPages ?? 0;
  const totalElements = channelsQuery.data?.totalElements ?? 0;

  const selectedStream = useMemo(() => {
    if (!selectedChannel) return '';
    return selectedChannel.hlsUrl?.trim() || selectedChannel.url?.trim() || '';
  }, [selectedChannel]);

  const handlePlay = async (channel: LiveChannel) => {
    setSelectedChannel(channel);
    try {
      const updated = await liveTvApi.recordView(channel.id);
      setSelectedChannel(updated);
      queryClient.setQueryData<LiveTvResponse>(
        ['liveChannels', page, deferredSearch, selectedCountry, selectedCategory, sortBy, sortDirection],
        previous => previous ? {
          ...previous,
          content: previous.content.map(item => item.id === updated.id ? updated : item),
        } : previous
      );
    } catch {
      // Playback should not be blocked by analytics/view recording failure.
    }
  };

  const handleFavorite = async (channel: LiveChannel) => {
    setFavoritePendingId(channel.id);
    try {
      const updated = await liveTvApi.toggleFavorite(channel.id);
      setSelectedChannel(current => current?.id === updated.id ? updated : current);
      queryClient.setQueriesData<LiveTvResponse>({ queryKey: ['liveChannels'] }, previous => previous ? {
        ...previous,
        content: previous.content.map(item => item.id === updated.id ? updated : item),
      } : previous);
    } finally {
      setFavoritePendingId(null);
    }
  };

  const resetFilters = () => {
    setSearchTerm('');
    setSelectedCountry('');
    setSelectedCategory('');
    setSortBy('name');
    setSortDirection('asc');
    setPage(0);
  };

  return (
    <div className="min-h-screen bg-[var(--color-bg-primary)]">
      <div className="mx-auto max-w-7xl px-4 py-6 md:px-6">
        <div className="mb-6 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <div className="mb-2 flex items-center gap-2 text-[var(--color-primary)]">
              <Tv className="h-5 w-5" />
              <span className="text-sm font-semibold uppercase tracking-wide">Live TV</span>
            </div>
            <h1 className="text-2xl font-bold text-[var(--color-text-primary)] md:text-3xl">Live TV Channels</h1>
            <p className="mt-1 text-sm text-[var(--color-text-tertiary)]">Browse, filter and play live channels through the platform.</p>
          </div>
          <Button variant="outline" onClick={() => channelsQuery.refetch()} loading={channelsQuery.isFetching && !channelsQuery.isPlaceholderData}>
            <RefreshCw className="h-4 w-4" /> Refresh
          </Button>
        </div>

        {selectedChannel && selectedStream && (
          <section className="mb-6 overflow-hidden rounded-2xl border border-[var(--color-border)] bg-black shadow-lg">
            <div className="flex items-center justify-between bg-[var(--color-surface)] px-4 py-3">
              <div className="min-w-0">
                <p className="text-xs uppercase tracking-wide text-[var(--color-text-tertiary)]">Now playing</p>
                <h2 className="truncate font-semibold text-[var(--color-text-primary)]">{selectedChannel.name}</h2>
              </div>
              <Button variant="ghost" size="icon" onClick={() => setSelectedChannel(null)} aria-label="Close player">
                <X className="h-5 w-5" />
              </Button>
            </div>
            <div className="aspect-video w-full">
              <VideoPlayer
                key={selectedChannel.id}
                src={selectedStream}
                poster={logoOf(selectedChannel)}
                autoPlay
                initialMuted
                liveMode
                className="h-full w-full"
                onError={() => undefined}
              />
            </div>
          </section>
        )}

        <section className="mb-6 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-4">
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-5">
            <label className="relative xl:col-span-2">
              <span className="sr-only">Search channels</span>
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--color-text-tertiary)]" />
              <input
                type="search"
                value={searchTerm}
                onChange={event => setSearchTerm(event.target.value)}
                placeholder="Search channels..."
                className="w-full rounded-lg border border-[var(--color-border)] bg-[var(--color-bg-primary)] py-2.5 pl-9 pr-3 text-sm text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]"
              />
            </label>

            <select value={selectedCountry} onChange={event => setSelectedCountry(event.target.value)} className="rounded-lg border border-[var(--color-border)] bg-[var(--color-bg-primary)] px-3 py-2.5 text-sm text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]">
              <option value="">All Countries</option>
              {(countriesQuery.data ?? []).map(country => <option key={country.code} value={country.code}>{flag(country.code)} {country.name} ({country.channelCount})</option>)}
            </select>

            <select value={selectedCategory} onChange={event => setSelectedCategory(event.target.value)} className="rounded-lg border border-[var(--color-border)] bg-[var(--color-bg-primary)] px-3 py-2.5 text-sm text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]">
              <option value="">All Categories</option>
              {(categoriesQuery.data ?? []).map(category => <option key={category.name} value={category.name}>{category.name} ({category.channelCount})</option>)}
            </select>

            <div className="flex gap-2">
              <select value={sortBy} onChange={event => setSortBy(event.target.value as SortField)} className="min-w-0 flex-1 rounded-lg border border-[var(--color-border)] bg-[var(--color-bg-primary)] px-3 py-2.5 text-sm text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]">
                <option value="name">Name</option>
                <option value="country">Country</option>
                <option value="category">Category</option>
                <option value="status">Status</option>
              </select>
              <select value={sortDirection} onChange={event => setSortDirection(event.target.value as SortDirection)} className="w-28 rounded-lg border border-[var(--color-border)] bg-[var(--color-bg-primary)] px-2 py-2.5 text-sm text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]">
                <option value="asc">A–Z</option>
                <option value="desc">Z–A</option>
              </select>
            </div>
          </div>
          <div className="mt-3 flex items-center justify-between gap-3 text-xs text-[var(--color-text-tertiary)]">
            <span>{totalElements.toLocaleString()} channel{totalElements === 1 ? '' : 's'}</span>
            <Button variant="ghost" size="sm" onClick={resetFilters}>Reset filters</Button>
          </div>
        </section>

        {channelsQuery.isError ? (
          <div className="rounded-2xl border border-red-500/30 bg-red-500/5 p-8 text-center">
            <AlertCircle className="mx-auto mb-3 h-8 w-8 text-red-500" />
            <h2 className="font-semibold text-[var(--color-text-primary)]">Unable to load Live TV</h2>
            <p className="mt-1 text-sm text-[var(--color-text-tertiary)]">{channelsQuery.error instanceof Error ? channelsQuery.error.message : 'The Live TV service did not return a valid response.'}</p>
            <Button className="mt-4" onClick={() => channelsQuery.refetch()}>Try again</Button>
          </div>
        ) : channelsQuery.isLoading ? (
          <div className="grid gap-3 md:grid-cols-2">
            {Array.from({ length: 8 }, (_, index) => <ChannelSkeleton key={index} />)}
          </div>
        ) : channels.length === 0 ? (
          <div className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-12 text-center">
            <Tv className="mx-auto mb-3 h-10 w-10 text-[var(--color-text-tertiary)]" />
            <h2 className="font-semibold text-[var(--color-text-primary)]">No channels found</h2>
            <p className="mt-1 text-sm text-[var(--color-text-tertiary)]">Try another country, category or search term.</p>
          </div>
        ) : (
          <>
            <div className="grid gap-3 md:grid-cols-2">
              {channels.map(channel => (
                <ChannelCard
                  key={channel.id}
                  channel={channel}
                  isPlaying={selectedChannel?.id === channel.id}
                  onPlay={() => handlePlay(channel)}
                  onFavorite={() => handleFavorite(channel)}
                  favoritePending={favoritePendingId === channel.id}
                />
              ))}
            </div>

            <div className="mt-6 flex items-center justify-between rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-3">
              <span className="text-sm text-[var(--color-text-tertiary)]">Page {totalPages === 0 ? 0 : page + 1} of {totalPages}</span>
              <div className="flex gap-2">
                <Button variant="outline" size="sm" disabled={page === 0 || channelsQuery.isFetching} onClick={() => setPage(current => Math.max(0, current - 1))}>
                  <ChevronLeft className="h-4 w-4" /> Previous
                </Button>
                <Button variant="outline" size="sm" disabled={totalPages === 0 || page >= totalPages - 1 || channelsQuery.isFetching} onClick={() => setPage(current => current + 1)}>
                  Next <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
          </>
        )}

        {channelsQuery.isFetching && !channelsQuery.isLoading && (
          <div className="fixed bottom-5 right-5 flex items-center gap-2 rounded-full bg-[var(--color-text-primary)] px-4 py-2 text-xs text-[var(--color-bg-primary)] shadow-lg">
            <Loader2 className="h-4 w-4 animate-spin" /> Updating channels…
          </div>
        )}
      </div>
    </div>
  );
}

export { LiveTvPage };
export default LiveTvPage;
