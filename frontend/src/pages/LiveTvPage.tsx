import { useEffect, useState, useDeferredValue } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { createPortal } from 'react-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Baby,
  CarFront,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Clapperboard,
  Globe2,
  Heart,
  Loader2,
  Menu,
  Maximize2,
  Minimize2,
  Monitor,
  Music2,
  Newspaper,
  Play,
  RefreshCw,
  Search,
  Sparkles,
  Trophy,
  Tv,
  Utensils,
  X,
} from 'lucide-react';
import { clsx } from 'clsx';
import { liveTvApi, type CategoryInfo, type CountryInfo, type LiveChannel, type LiveTvResponse, type PlaybackDiagnostic, type StreamCandidateInfo, type YouTubeFallbackResponse } from '../services/api/liveTvApi';
import { LiveTvPlayer } from '../components/video/LiveTvPlayer';
import { EarthGlobe } from '../components/live-tv/EarthGlobe';
import { Button } from '../components/ui/Button';
import './LiveTvPage.css';

const PAGE_SIZE = 50;
type SortField = 'name' | 'country' | 'category' | 'status';
type SortDirection = 'asc' | 'desc';
type LivePlayerMode = 'normal' | 'mini' | 'fullscreen';

const categoryIconMap: Record<string, typeof Tv> = {
  news: Newspaper,
  sports: Trophy,
  music: Music2,
  animation: Sparkles,
  kids: Baby,
  cooking: Utensils,
  auto: CarFront,
  movies: Clapperboard,
  entertainment: Monitor,
};

function flag(code?: string | null) {
  const value = code?.trim().toUpperCase();
  if (!value || !/^[A-Z]{2}$/.test(value)) return '🌍';
  return String.fromCodePoint(...value.split('').map(c => 127397 + c.charCodeAt(0)));
}

function fallback(name?: string | null) {
  const initial = name?.trim().charAt(0).toUpperCase() || '?';
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="128" height="128"><rect width="128" height="128" rx="24" fill="#17202d"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" font-family="Arial" font-size="52" font-weight="700" fill="#7dd3fc">${initial}</text></svg>`;
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
}

function slugify(value: string) {
  return value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

function logoOf(channel: LiveChannel) {
  // Always prefer our same-origin IPTV-org logo endpoint. The backend first
  // serves the local cloned IPTV repository and falls back to IPTV-org's
  // catalogue URL, so browser CORS/ad-blocker/network differences cannot
  // hide otherwise valid channel artwork.
  if (channel.id) {
    return `/api/v1/live/channels/${encodeURIComponent(channel.id)}/logo`;
  }

  return channel.logo?.trim() || channel.tvgLogo?.trim() || undefined;
}

function categoryIcon(name: string) {
  const Icon = categoryIconMap[name.trim().toLowerCase()] ?? Tv;
  return <Icon className="live-tv-nav-icon" aria-hidden="true" />;
}

function statusDot(status?: LiveChannel['status']) {
  if (status === 'playable') return 'bg-emerald-400';
  if (status === 'checking') return 'bg-amber-400';
  if (status === 'degraded') return 'bg-amber-400';
  if (status === 'no_source') return 'bg-red-400';
  return 'bg-slate-500';
}

interface ChannelRowProps {
  channel: LiveChannel;
  isPlaying: boolean;
  onPlay: () => void;
  onFavorite: () => void;
  favoritePending: boolean;
}

function ChannelRow({ channel, isPlaying, onPlay, onFavorite, favoritePending }: ChannelRowProps) {
  const image = logoOf(channel);
  const favorite = channel.favorite === true;

  return (
    <article className={clsx('live-tv-channel-row', isPlaying && 'is-playing')}>
      <button type="button" className="live-tv-channel-play" onClick={onPlay} aria-label={`Play ${channel.name}`}>
        <img
          src={image || fallback(channel.name)}
          alt=""
          loading="lazy"
          onError={event => {
            const img = event.currentTarget;
            if (!img.dataset.fallback) {
              img.dataset.fallback = 'true';
              img.src = fallback(channel.name);
            }
          }}
        />
        <span className="live-tv-channel-play-overlay"><Play size={16} fill="currentColor" /></span>
      </button>

      <button type="button" className="live-tv-channel-copy" onClick={onPlay}>
        <div className="live-tv-channel-title-line">
          <span className="live-tv-channel-name" title={channel.name}>{channel.name}</span>
          <span className={clsx('live-tv-status-dot', statusDot(channel.status))} title={channel.status || 'unknown'} />
        </div>
        <div className="live-tv-channel-meta">
          <span>{flag(channel.countryCode)} {channel.countryCode || '—'}</span>
          {channel.language && channel.language !== 'Unknown' && <span>{channel.language}</span>}
          {channel.category && <span>{channel.category}</span>}
        </div>
      </button>

      <Button
        variant="ghost"
        size="icon"
        onClick={onFavorite}
        loading={favoritePending}
        aria-label={favorite ? `Remove ${channel.name} from favorites` : `Add ${channel.name} to favorites`}
        className="live-tv-heart"
      >
        <Heart className={clsx('h-4 w-4', favorite && 'fill-current text-rose-400')} />
      </Button>
    </article>
  );
}

function GlobePanel({
  selectedCountry,
  onCountryChange,
  countries,
  totalElements,
}: {
  selectedCountry: string;
  onCountryChange: (code: string) => void;
  countries: CountryInfo[];
  totalElements: number;
}) {
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    const timer = window.setInterval(() => setNow(new Date()), 30_000);
    return () => window.clearInterval(timer);
  }, []);

  const selected = countries.find(country => country.code === selectedCountry);

  return (
    <section className="live-tv-globe-panel">
      <div className="live-tv-globe-stars" aria-hidden="true" />
      <div className="live-tv-globe-heading">
        <div>
          <span className="live-tv-eyebrow"><Globe2 size={14} /> Global live television</span>
          <h1>{selectedCountry ? `${flag(selectedCountry)} ${selected?.name || selectedCountry}` : 'All Channels'}</h1>
          <p>{totalElements.toLocaleString()} channels in the current view · country selection drives the channel guide</p>
        </div>
        <div className="live-tv-local-time">
          <span>Local Time</span>
          <strong>{new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit' }).format(now)}</strong>
        </div>
      </div>

      <div className="live-tv-globe-stage">
        <EarthGlobe selectedCountry={selectedCountry} countries={countries} onCountryChange={onCountryChange} />
      </div>

      <div className="live-tv-country-strip">
        <button type="button" className={clsx('live-tv-country-chip', !selectedCountry && 'active')} onClick={() => onCountryChange('')}>
          <span>🌍</span><strong>All</strong>
        </button>
        {countries.slice(0, 20).map(country => (
          <button key={country.code} type="button" className={clsx('live-tv-country-chip', selectedCountry === country.code && 'active')} onClick={() => onCountryChange(country.code)}>
            <span>{flag(country.code)}</span>
            <strong>{country.code}</strong>
            <small>{country.channelCount.toLocaleString()}</small>
          </button>
        ))}
      </div>
    </section>
  );
}
function LiveTvPage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { category: routeCategory, channelId: routeChannelId } = useParams<{ category?: string; channelId?: string }>();
  const [sortBy, setSortBy] = useState<SortField>('name');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCountry, setSelectedCountry] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [page, setPage] = useState(0);
  const [selectedChannel, setSelectedChannel] = useState<LiveChannel | null>(null);
  const [playerMode, setPlayerMode] = useState<LivePlayerMode>('normal');
  const [favoritePendingId, setFavoritePendingId] = useState<string | null>(null);
  const [playbackDiagnostic, setPlaybackDiagnostic] = useState<PlaybackDiagnostic | null>(null);
  const [playbackDiagnosticLoading, setPlaybackDiagnosticLoading] = useState(false);
  const [streamCandidates, setStreamCandidates] = useState<StreamCandidateInfo[]>([]);
  const [streamCandidatesLoading, setStreamCandidatesLoading] = useState(false);
  const [streamCandidatesLoaded, setStreamCandidatesLoaded] = useState(false);
  const [playingSource, setPlayingSource] = useState('IPTV');
  const [categoryPanelOpen, setCategoryPanelOpen] = useState(false);
  const [youtubeFallback, setYoutubeFallback] = useState<YouTubeFallbackResponse | null>(null);
  const [youtubeLoading, setYoutubeLoading] = useState(false);
  const deferredSearch = useDeferredValue(searchTerm.trim());

  // The Live-TV catalogue is imported asynchronously at service startup. Do not
  // let the first empty response become a 5-minute React Query cache entry.
  const readinessQuery = useQuery({
    queryKey: ['liveReadiness'],
    queryFn: () => liveTvApi.getReadiness(),
    staleTime: 0,
    refetchInterval: query => query.state.data?.ready ? false : 1000,
    refetchOnWindowFocus: false,
    retry: 2,
  });
  const liveTvReady = readinessQuery.data?.ready === true;

  useEffect(() => {
    setPage(0);
  }, [deferredSearch, selectedCountry, selectedCategory, sortBy, sortDirection]);

  const countriesQuery = useQuery<CountryInfo[]>({
    queryKey: ['liveCountries', selectedCategory, deferredSearch],
    enabled: liveTvReady,
    queryFn: () => liveTvApi.getCountries({ category: selectedCategory || undefined, search: deferredSearch || undefined }),
    staleTime: 5 * 60 * 1000,
    placeholderData: previous => previous,
    refetchOnWindowFocus: false,
  });

  const categoriesQuery = useQuery<CategoryInfo[]>({
    queryKey: ['liveCategories', selectedCountry, deferredSearch],
    enabled: liveTvReady,
    queryFn: () => liveTvApi.getCategories({ country: selectedCountry || undefined, search: deferredSearch || undefined }),
    staleTime: 5 * 60 * 1000,
    placeholderData: previous => previous,
    refetchOnWindowFocus: false,
  });

  const categoryRouteReady = liveTvReady && (!routeCategory || !categoriesQuery.isLoading);

  const channelsQuery = useQuery({
    queryKey: ['liveChannels', page, deferredSearch, selectedCountry, selectedCategory, sortBy, sortDirection],
    enabled: categoryRouteReady,
    queryFn: () => liveTvApi.getChannels({
      page,
      size: PAGE_SIZE,
      search: deferredSearch || undefined,
      country: selectedCountry || undefined,
      category: selectedCategory || undefined,
      sort: `${sortBy},${sortDirection}`,
    }),
    placeholderData: previous => previous,
    staleTime: 30 * 1000,
    refetchOnWindowFocus: false,
  });

  // The URL is the source of truth for the current Live TV section.
  // /live -> all channels, /live/kids -> Kids, /live/kids/channel/:id -> player.
  useEffect(() => {
    if (!routeCategory) {
      setSelectedCategory('');
      return;
    }
    const match = (categoriesQuery.data ?? []).find(category => slugify(category.name) === routeCategory);
    setSelectedCategory(match?.name ?? '');
  }, [routeCategory, categoriesQuery.data]);

  const channelRouteQuery = useQuery<LiveChannel>({
    queryKey: ['liveChannel', routeChannelId],
    queryFn: () => liveTvApi.getChannel(routeChannelId!),
    enabled: Boolean(routeChannelId),
    staleTime: 60 * 1000,
  });

  const channels = channelsQuery.data?.content ?? [];
  const totalPages = channelsQuery.data?.totalPages ?? 0;
  const totalElements = channelsQuery.data?.totalElements ?? 0;
  const selectedChannelId = selectedChannel?.id;

  useEffect(() => {
    if (routeChannelId && channelRouteQuery.data) {
      setSelectedChannel(channelRouteQuery.data);
      setPlayerMode('normal');
      return;
    }
    if (!routeChannelId) {
      setSelectedChannel(null);
    }
  }, [routeChannelId, channelRouteQuery.data]);

  useEffect(() => {
    let cancelled = false;
    if (!selectedChannelId) {
      setStreamCandidates([]);
      setStreamCandidatesLoaded(false);
      return;
    }
    setStreamCandidatesLoaded(false);
    setStreamCandidatesLoading(true);
    liveTvApi.getStreamCandidates(selectedChannelId)
      .then(result => { if (!cancelled) setStreamCandidates(result); })
      .catch(() => { if (!cancelled) setStreamCandidates([]); })
      .finally(() => { if (!cancelled) { setStreamCandidatesLoading(false); setStreamCandidatesLoaded(true); } });
    return () => { cancelled = true; };
  }, [selectedChannelId]);

  useEffect(() => {
    if (!selectedChannelId || !streamCandidatesLoaded || streamCandidatesLoading || streamCandidates.length > 0) return;
    let cancelled = false;
    setYoutubeLoading(true);
    liveTvApi.getYouTubeFallback(selectedChannelId).then(result => {
      if (!cancelled) {
        setYoutubeFallback(result);
        if (result.found) setPlayingSource('YouTube');
      }
    }).catch(() => undefined).finally(() => { if (!cancelled) setYoutubeLoading(false); });
    return () => { cancelled = true; };
  }, [selectedChannelId, streamCandidatesLoaded, streamCandidatesLoading, streamCandidates.length]);

  useEffect(() => {
    if (!selectedChannel || playerMode === 'mini') return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => { document.body.style.overflow = previousOverflow; };
  }, [selectedChannel, playerMode]);

  const handlePlay = async (channel: LiveChannel) => {
    setPlaybackDiagnostic(null);
    setPlaybackDiagnosticLoading(false);
    setStreamCandidates([]);
    setStreamCandidatesLoaded(false);
    setYoutubeFallback(null);
    setYoutubeLoading(false);
    setPlayingSource('IPTV');
    setSelectedChannel(channel);
    setPlayerMode('normal');
    const categoryPath = selectedCategory ? `${slugify(selectedCategory)}/` : '';
    navigate(`/live/${categoryPath}channel/${encodeURIComponent(channel.id)}`);
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
      // Playback must remain independent of analytics/view recording.
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
    navigate('/live');
    setSortBy('name');
    setSortDirection('asc');
    setPage(0);
    setCategoryPanelOpen(false);
  };

  const categories = categoriesQuery.data ?? [];
  const countries = countriesQuery.data ?? [];

  return (
    <div className="live-tv-page">
      <header className="live-tv-topbar">
        <div className="live-tv-brand">
          <Link
            to={routeCategory || routeChannelId ? `/live${routeCategory ? `/${routeCategory}` : ''}` : '/'}
            className="live-tv-menu-button"
            aria-label={routeCategory || routeChannelId ? 'Back to Live TV' : 'Back to platform'}
            title={routeCategory || routeChannelId ? 'Back to Live TV' : 'Back to platform'}
          >
            <Menu size={21} />
          </Link>
          <div className="live-tv-brand-mark"><Tv size={20} /></div>
          <div>
            <strong>VideoPlatform</strong>
            <span>LIVE TV</span>
          </div>
        </div>

        <div className="live-tv-search-wrap">
          <Search size={18} />
          <input value={searchTerm} onChange={event => setSearchTerm(event.target.value)} placeholder="Search channels, countries, categories..." aria-label="Search live TV" />
          {searchTerm && <button type="button" onClick={() => setSearchTerm('')} aria-label="Clear search"><X size={17} /></button>}
        </div>

        <div className="live-tv-top-actions">
          <span className="live-tv-online-pill"><span /> LIVE</span>
          <button type="button" onClick={() => channelsQuery.refetch()} className="live-tv-icon-button" aria-label="Refresh channels" title="Refresh channels">
            <RefreshCw size={18} className={channelsQuery.isFetching ? 'animate-spin' : ''} />
          </button>
        </div>
      </header>

      <div className={clsx('live-tv-workspace', categoryPanelOpen && 'category-panel-open')}>
        <aside className="live-tv-channel-panel">
          <div className="live-tv-channel-panel-header">
            <div>
              <span className="live-tv-eyebrow"><Monitor size={13} /> Channel guide</span>
              <h2>{selectedCountry ? `${flag(selectedCountry)} ${countries.find(country => country.code === selectedCountry)?.name || selectedCountry}${selectedCategory ? ` · ${selectedCategory}` : ''}` : (selectedCategory || 'All Channels')}</h2>
            </div>
            <span className="live-tv-panel-count">{totalElements.toLocaleString()}</span>
          </div>

          <div className="live-tv-channel-list">
            {!liveTvReady || channelsQuery.isLoading ? (
              Array.from({ length: 9 }, (_, index) => <div key={index} className="live-tv-channel-skeleton"><span /><div><b /><i /><i /></div></div>)
            ) : channelsQuery.isError ? (
              <div className="live-tv-panel-empty"><Tv size={30} /><strong>Live TV unavailable</strong><p>Unable to load the channel guide.</p><Button size="sm" onClick={() => channelsQuery.refetch()}>Try again</Button></div>
            ) : channels.length === 0 ? (
              <div className="live-tv-panel-empty"><Search size={30} /><strong>No channels found</strong><p>Try another filter or search.</p></div>
            ) : (
              channels.map(channel => (
                <ChannelRow
                  key={channel.id}
                  channel={channel}
                  isPlaying={selectedChannel?.id === channel.id}
                  onPlay={() => handlePlay(channel)}
                  onFavorite={() => handleFavorite(channel)}
                  favoritePending={favoritePendingId === channel.id}
                />
              ))
            )}
          </div>

          <div className="live-tv-pagination">
            <span>{totalPages === 0 ? 0 : page + 1} / {totalPages || 0}</span>
            <div>
              <button type="button" disabled={page === 0 || channelsQuery.isFetching} onClick={() => setPage(current => Math.max(0, current - 1))}><ChevronLeft size={17} /></button>
              <button type="button" disabled={totalPages === 0 || page >= totalPages - 1 || channelsQuery.isFetching} onClick={() => setPage(current => current + 1)}><ChevronRight size={17} /></button>
            </div>
          </div>
        </aside>
        <main className="live-tv-main">
          <GlobePanel selectedCountry={selectedCountry} onCountryChange={setSelectedCountry} countries={countries} totalElements={totalElements} />
          <div className="live-tv-bottom-toolbar">
            <div className="live-tv-filter-summary">
              <span>{selectedCountry ? flag(selectedCountry) : '🌍'} {selectedCategory || 'All categories'}</span>
              {selectedCountry && <span>{countries.find(c => c.code === selectedCountry)?.name || selectedCountry}</span>}
              {deferredSearch && <span>“{deferredSearch}”</span>}
            </div>
            <div className="live-tv-sort-controls">
              <span>Sort</span>
              <select value={sortBy} onChange={event => setSortBy(event.target.value as SortField)} aria-label="Sort channels">
                <option value="name">Name</option>
                <option value="country">Country</option>
                <option value="category">Category</option>
                <option value="status">Status</option>
              </select>
              <button type="button" onClick={() => setSortDirection(current => current === 'asc' ? 'desc' : 'asc')} aria-label="Toggle sort direction">
                {sortDirection === 'asc' ? 'A–Z' : 'Z–A'} <ChevronDown size={14} className={sortDirection === 'desc' ? 'rotate-180' : ''} />
              </button>
            </div>
          </div>
        </main>

        <button
          type="button"
          className={clsx('live-tv-category-toggle', categoryPanelOpen && 'open')}
          onClick={() => setCategoryPanelOpen(current => !current)}
          aria-expanded={categoryPanelOpen}
          aria-controls="live-tv-category-sidebar"
          title={categoryPanelOpen ? 'Hide categories' : 'Show categories'}
          aria-label={categoryPanelOpen ? 'Hide categories' : 'Show categories'}
        >
          {categoryPanelOpen ? <ChevronRight size={17} /> : <ChevronLeft size={17} />}
        </button>

        <aside id="live-tv-category-sidebar" className={clsx('live-tv-category-sidebar', categoryPanelOpen && 'open')}>

          <div className="live-tv-sidebar-title">Browse</div>
          <button type="button" className={clsx('live-tv-nav-item', !selectedCategory && 'active')} onClick={() => { setSelectedCategory(''); setCategoryPanelOpen(false); navigate('/live'); }}>
            <Tv size={18} />
            <span>All Channels</span>
            <small>{totalElements.toLocaleString()}</small>
          </button>
          <div className="live-tv-sidebar-divider" />
          <div className="live-tv-sidebar-title">Categories</div>
          <div className="live-tv-sidebar-scroll">
            {!liveTvReady || categoriesQuery.isLoading ? (
              Array.from({ length: 8 }, (_, index) => <div key={index} className="live-tv-nav-item live-tv-nav-skeleton"><span /><i /><small /></div>)
            ) : categoriesQuery.isError ? (
              <div className="live-tv-sidebar-empty">Unable to load categories.</div>
            ) : categories.length === 0 ? (
              <div className="live-tv-sidebar-empty">No categories for this filter.</div>
            ) : categories.map(category => (
              <button key={category.name} type="button" className={clsx('live-tv-nav-item', selectedCategory.toLowerCase() === category.name.toLowerCase() && 'active')} onClick={() => { setSelectedCategory(category.name); setCategoryPanelOpen(false); navigate(`/live/${slugify(category.name)}`); }}>
                {categoryIcon(category.name)}
                <span>{category.name}</span>
                <small>{category.channelCount.toLocaleString()}</small>
              </button>
            ))}
          </div>
          <button type="button" className="live-tv-reset-button" onClick={resetFilters}>Reset filters</button>
        </aside>

      </div>

      {selectedChannel && typeof document !== 'undefined' && createPortal(
        <div
          className={clsx(
            'live-tv-player-modal',
            `live-tv-player-mode-${playerMode}`
          )}
          role="dialog"
          aria-modal={playerMode !== 'mini'}
          aria-label={`Playing ${selectedChannel.name}`}
        >
          {playerMode !== 'mini' && (
            <button
              type="button"
              className="live-tv-player-backdrop"
              onClick={() => {
                setSelectedChannel(null);
                setPlayerMode('normal');
                navigate(`/live${selectedCategory ? `/${slugify(selectedCategory)}` : ''}`);
              }}
              aria-label="Close player"
            />
          )}

          <div className="live-tv-player-card">
            <div className="live-tv-player-header">
              <div className="live-tv-player-channel">
                <img src={logoOf(selectedChannel) || fallback(selectedChannel.name)} alt="" />
                <div>
                  <span><i /> LIVE · {selectedChannel.countryCode || 'GLOBAL'}</span>
                  <h2>{selectedChannel.name}</h2>
                </div>
              </div>

              <div className="live-tv-player-window-actions">
                <button
                  type="button"
                  onClick={() => setPlayerMode(current => current === 'mini' ? 'normal' : 'mini')}
                  aria-label={playerMode === 'mini' ? 'Restore player' : 'Minimize player'}
                  title={playerMode === 'mini' ? 'Restore player' : 'Minimize player'}
                >
                  {playerMode === 'mini' ? <Maximize2 size={18} /> : <Minimize2 size={18} />}
                </button>
                <button
                  type="button"
                  onClick={() => setPlayerMode(current => current === 'fullscreen' ? 'normal' : 'fullscreen')}
                  aria-label={playerMode === 'fullscreen' ? 'Exit full player' : 'Full player'}
                  title={playerMode === 'fullscreen' ? 'Exit full player' : 'Full player'}
                >
                  {playerMode === 'fullscreen' ? <Minimize2 size={18} /> : <Maximize2 size={18} />}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setSelectedChannel(null);
                    setPlayerMode('normal');
                    navigate(`/live${selectedCategory ? `/${slugify(selectedCategory)}` : ''}`);
                  }}
                  aria-label="Close player"
                  title="Close player"
                >
                  <X size={20} />
                </button>
              </div>
            </div>

            <div className="live-tv-player-frame">
              {streamCandidatesLoading ? (
                <div className="live-tv-player-loading"><Loader2 size={26} className="animate-spin" /><span>Finding live sources…</span></div>
              ) : youtubeFallback?.found ? (
                <div className="live-tv-youtube-player">
                  <div className="live-tv-youtube-label"><span className="live-tv-native-live-dot" /> LIVE · YouTube</div>
                  <iframe
                    title={`${selectedChannel.name} YouTube Live`}
                    src={`https://www.youtube-nocookie.com/embed/${encodeURIComponent(youtubeFallback.videoId)}?autoplay=1&playsinline=1&rel=0`}
                    allow="autoplay; encrypted-media; picture-in-picture; fullscreen"
                    allowFullScreen
                  />
                </div>
              ) : streamCandidates.length > 0 ? (
                <LiveTvPlayer
                  key={selectedChannel.id}
                  candidates={streamCandidates}
                  poster={logoOf(selectedChannel)}
                  autoPlay
                  className="h-full w-full"
                  onPlaying={source => { setPlayingSource(source.includes('YouTube') ? 'YouTube' : 'IPTV'); setPlaybackDiagnostic(null); }}
                  onExhausted={async () => {
                    if (!selectedChannel) return;
                    setYoutubeLoading(true);
                    setPlaybackDiagnosticLoading(true);
                    const [diagnosticResult, youtubeResult] = await Promise.allSettled([
                      liveTvApi.getPlaybackDiagnostic(selectedChannel.id),
                      liveTvApi.getYouTubeFallback(selectedChannel.id),
                    ]);
                    if (diagnosticResult.status === 'fulfilled') setPlaybackDiagnostic(diagnosticResult.value);
                    else setPlaybackDiagnostic({
                      channelId: selectedChannel.id, tvgId: selectedChannel.tvgId, channelName: selectedChannel.name,
                      classification: 'ERROR', userMessage: 'Live stream could not be started. Please try again.',
                      mainReason: 'Playback diagnostic request failed.', successfulUrl: '', status: 0, attempts: [], checkedAt: new Date().toISOString(),
                    });
                    if (youtubeResult.status === 'fulfilled' && youtubeResult.value.found) {
                      setYoutubeFallback(youtubeResult.value);
                      setPlayingSource('YouTube');
                    } else if (youtubeResult.status === 'fulfilled') {
                      setYoutubeFallback(youtubeResult.value);
                    }
                    setYoutubeLoading(false);
                    setPlaybackDiagnosticLoading(false);
                  }}
                />
              ) : (
                <div className="live-tv-player-loading"><Tv size={28} /><strong>No indexed IPTV source</strong><span>{youtubeLoading ? 'Checking YouTube Live fallback…' : 'Checking available alternatives…'}</span></div>
              )}
            </div>
            {(playbackDiagnosticLoading || playbackDiagnostic) && (
              <div className="live-tv-playback-diagnostic" role="status">
                {playbackDiagnosticLoading && <span>Checking IPTV sources…</span>}
                {youtubeLoading && <span>Checking YouTube Live fallback…</span>}
                {!playbackDiagnosticLoading && playbackDiagnostic && (
                  <>
                    <strong>{playbackDiagnostic.userMessage}</strong>
                    {playbackDiagnostic.mainReason && <span>{playbackDiagnostic.mainReason}</span>}
                  </>
                )}
              </div>
            )}

            <div className="live-tv-player-footer">
              <div>
                <span>{flag(selectedChannel.countryCode)} {selectedChannel.country || 'Global'}</span>
                <span>{selectedChannel.category || 'General'}</span>
                <span>{selectedChannel.language || 'Unknown'}</span>
                <span className="live-tv-source-badge">SOURCE · {playingSource}</span>
              </div>
              <button
                type="button"
                onClick={() => handleFavorite(selectedChannel)}
                className={selectedChannel.favorite ? 'favorite' : ''}
              >
                <Heart size={17} className={selectedChannel.favorite ? 'fill-current' : ''} />
                {selectedChannel.favorite ? 'Saved' : 'Save'}
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}


      {channelsQuery.isFetching && !channelsQuery.isLoading && (
        <div className="live-tv-fetching"><Loader2 size={15} className="animate-spin" /> Updating guide…</div>
      )}
    </div>
  );
}

export { LiveTvPage };
export default LiveTvPage;
