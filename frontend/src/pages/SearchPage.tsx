import { useEffect, useRef, useState, type FormEvent } from 'react';

import { useQuery } from '@tanstack/react-query';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import {
  ArrowRight,
  Search,
  SlidersHorizontal,
  Sparkles,
  X,
  ChevronDown,
  Flame,
  Gamepad2,
  Cpu,
  GraduationCap,
  Radio,
  Mic2,
  Clapperboard,
  Trophy,
  Compass,
} from 'lucide-react';

import { VideoCard } from '../components/video/VideoCard';
import { videoApi } from '../services/api/videoApi';

const topics = [
  { label: 'Music', icon: Mic2, query: 'Music' },
  { label: 'Gaming', icon: Gamepad2, query: 'Gaming' },
  { label: 'Technology', icon: Cpu, query: 'Technology' },
  { label: 'Education', icon: GraduationCap, query: 'Education' },
  { label: 'Live', icon: Radio, query: 'Live', route: '/live' },
  { label: 'Podcasts', icon: Mic2, query: 'Podcasts' },
  { label: 'Entertainment', icon: Clapperboard, query: 'Entertainment' },
  { label: 'Sports', icon: Trophy, query: 'Sports' },
];

const discoveryGroups = [
  {
    title: 'Trending right now',
    icon: Flame,
    items: ['AI', 'Football', 'Music', 'Gaming', 'Movies', 'Podcasts'],
  },
  {
    title: 'Learn something new',
    icon: GraduationCap,
    items: [
      'Programming',
      'Science',
      'Business',
      'Design',
      'History',
      'Finance',
    ],
  },
  {
    title: 'Watch & relax',
    icon: Clapperboard,
    items: [
      'Comedy',
      'Documentaries',
      'Travel',
      'Food',
      'Technology',
      'Live',
    ],
  },
];

export function SearchPage() {
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();

  const query = (params.get('q') || '').trim();
  const sort = params.get('sort') || 'relevance';
  const cursor = params.get('cursor') || '';

  const [draft, setDraft] = useState(query);
  const [discoverOpen, setDiscoverOpen] = useState(false);

  const searchInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    setDraft(query);
  }, [query]);

  const update = (values: Record<string, string>) => {
    const next = new URLSearchParams(params);

    for (const [key, value] of Object.entries(values)) {
      if (value) {
        next.set(key, value);
      } else {
        next.delete(key);
      }
    }

    if (!('cursor' in values)) {
      next.delete('cursor');
    }

    setParams(next);
  };

  const search = (value: string) => {
    const q = value.trim();

    setDraft(q);
    update({
      q,
      cursor: '',
    });
  };

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    search(draft);
  };

  const runDiscovery = (value: string) => {
    setDiscoverOpen(false);
    search(value);
  };

  const handleTopicClick = (topic: {
    label: string;
    query: string;
    route?: string;
  }) => {
    setDiscoverOpen(false);

    if (topic.route) {
      navigate(topic.route);
      return;
    }

    runDiscovery(topic.query);
  };

  const handleDiscoveryItemClick = (item: string) => {
    setDiscoverOpen(false);

    if (item === 'Live') {
      navigate('/live');
      return;
    }

    runDiscovery(item);
  };

  const clearSearch = () => {
    setDraft('');
    search('');
    setDiscoverOpen(false);
  };

  const startWithSearch = () => {
    setDiscoverOpen(false);

    requestAnimationFrame(() => {
      searchInputRef.current?.focus();
    });
  };

  const { data, isPending, isError, refetch } = useQuery({
    queryKey: ['video-search', query, sort, cursor],
    queryFn: () =>
      videoApi.search({
        q: query || undefined,
        sort,
        cursor: cursor || undefined,
        size: 20,
      }),
    retry: 1,
  });

  const { data: suggestions } = useQuery({
    queryKey: ['search-suggest', draft.trim()],
    queryFn: () => videoApi.getSuggestions(draft.trim()),
    enabled: draft.trim().length >= 2 && draft.trim() !== query,
    retry: false,
  });

  const videos = data?.content || [];

  return (
    <div className="search-page">
      <section className="search-hero">
        <div className="search-hero-copy">
          <button
            type="button"
            className={`discover-trigger ${
              discoverOpen ? 'active' : ''
            }`}
            onClick={() => setDiscoverOpen((value) => !value)}
            aria-expanded={discoverOpen}
            aria-controls="discover-panel"
          >
            <span className="discover-trigger-icon">
              <Sparkles className="w-4 h-4" />
            </span>

            <span>DISCOVER</span>

            <ChevronDown
              className={`w-4 h-4 discover-trigger-chevron ${
                discoverOpen ? 'open' : ''
              }`}
            />
          </button>

          <h1>Find something worth watching.</h1>

          <p>
            Explore videos, creators, and new ideas. Start with a search or
            let us help you discover something new.
          </p>

          <div className="search-hero-actions">
            <button
              type="button"
              className="search-hero-start"
              onClick={startWithSearch}
            >
              <Search className="w-4 h-4" />
              Start with search
            </button>

            <button
              type="button"
              className="search-hero-browse"
              onClick={() => setDiscoverOpen(true)}
            >
              <Compass className="w-4 h-4" />
              Browse ideas
            </button>
          </div>
        </div>

        {discoverOpen && (
          <div id="discover-panel" className="discover-panel">
            <div className="discover-panel-head">
              <div>
                <strong>Explore the platform</strong>
                <span>
                  Pick a direction and we'll turn it into a search.
                </span>
              </div>

              <button
                type="button"
                onClick={() => setDiscoverOpen(false)}
                aria-label="Close discover"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="discover-groups">
              {discoveryGroups.map((group) => {
                const Icon = group.icon;

                return (
                  <div className="discover-group" key={group.title}>
                    <div className="discover-group-title">
                      <Icon className="w-4 h-4" />
                      <span>{group.title}</span>
                    </div>

                    <div className="discover-group-items">
                      {group.items.map((item) => (
                        <button
                          type="button"
                          key={item}
                          onClick={() =>
                            handleDiscoveryItemClick(item)
                          }
                        >
                          {item}
                          <ArrowRight className="w-3.5 h-3.5" />
                        </button>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        <form
          className="search-page-form"
          onSubmit={submit}
          role="search"
        >
          <Search className="w-5 h-5" />

          <input
            aria-label="Discover videos"
            ref={searchInputRef}
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            placeholder="Search videos, channels, creators..."
            autoComplete="off"
          />

          {draft && (
            <button
              type="button"
              className="search-input-clear"
              onClick={clearSearch}
              aria-label="Clear search"
            >
              <X className="w-4 h-4" />
            </button>
          )}

          <button type="submit">
            Search
            <ArrowRight className="w-4 h-4" />
          </button>
        </form>

        {suggestions?.suggestions?.length > 0 && (
          <div className="search-suggestions">
            <span>Suggestions</span>

            {suggestions.suggestions.slice(0, 6).map((suggestion: string) => (
              <button
                type="button"
                key={suggestion}
                onClick={() => runDiscovery(suggestion)}
              >
                <Search className="w-4 h-4" />
                {suggestion}
              </button>
            ))}
          </div>
        )}

        <div
          className="discovery-topics"
          aria-label="Explore topics"
        >
          {topics.map((topic) => {
            const Icon = topic.icon;

            return (
              <button
                type="button"
                key={topic.label}
                className={
                  !topic.route &&
                  query.toLowerCase() === topic.query.toLowerCase()
                    ? 'active'
                    : ''
                }
                onClick={() => handleTopicClick(topic)}
              >
                <Icon className="w-4 h-4" />
                {topic.label}
              </button>
            );
          })}
        </div>
      </section>

      <div className="search-results-toolbar">
        <div>
          <p className="search-result-count">
            {query
              ? `Results for “${query}”`
              : 'Discover videos'}
          </p>

          <p className="search-result-query">
            {isPending
              ? 'Finding videos…'
              : `${data?.totalElements ?? 0} videos`}
          </p>
        </div>

        <label className="sort-control">
          <SlidersHorizontal className="w-4 h-4" />

          <span>Sort</span>

          <select
            aria-label="Sort results"
            value={sort}
            onChange={(event) =>
              update({
                sort: event.target.value,
                cursor: '',
              })
            }
          >
            <option value="relevance">Relevance</option>
            <option value="publishedAt">Newest</option>
            <option value="viewCount">Most viewed</option>
            <option value="likeCount">Most liked</option>
          </select>
        </label>
      </div>

      {isPending ? (
        <div
          className="video-grid"
          aria-label="Loading videos"
        >
          {Array.from({ length: 8 }, (_, index) => (
            <div
              key={index}
              className="video-skeleton"
            >
              <div />
              <span />
              <small />
            </div>
          ))}
        </div>
      ) : isError ? (
        <section
          className="discovery-card search-feedback"
          role="alert"
        >
          <Search />

          <h2>Search is unavailable</h2>

          <p>
            We couldn't reach the search service. Check that the API
            gateway and search service are running, then try again.
          </p>

          <button
            type="button"
            onClick={() => refetch()}
          >
            Retry search
          </button>
        </section>
      ) : videos.length ? (
        <div className="video-grid">
          {videos.map((video) => (
            <VideoCard
              key={video.id}
              video={video}
            />
          ))}
        </div>
      ) : (
        <section className="discovery-card search-feedback">
          <Search />

          <h2>
            {query
              ? 'No matching videos yet'
              : 'No videos available yet'}
          </h2>

          <p>
            {query
              ? 'Try another topic or a shorter search phrase.'
              : 'Published videos will appear here once the catalog is populated.'}
          </p>

          {query && (
            <button
              type="button"
              onClick={() => search('')}
            >
              Explore all videos
            </button>
          )}

          <Link to="/">Go to home</Link>
        </section>
      )}

      {data?.hasNext && data.nextCursor && (
        <div className="pagination">
          <button
            type="button"
            onClick={() =>
              update({
                cursor: data.nextCursor!,
              })
            }
          >
            Load more videos
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      )}
    </div>
  );
}