import { http } from '../http';

/**
 * Live-TV channel status.
 */
export type LiveChannelStatus =
  | 'playable'
  | 'degraded'
  | 'no_source'
  | 'checking'
  | 'unknown';

/**
 * Live-TV channel returned by the backend.
 */
export interface LiveChannel {
  id: string;
  name: string;
  country: string;
  countryCode: string;
  language: string;
  category: string;

  logo?: string | null;

  /**
   * Primary stream URL.
   */
  url: string;

  /**
   * HLS stream URL when available.
   */
  hlsUrl?: string | null;

  isNsfw: boolean;

  tvgId: string;
  tvgName?: string | null;
  tvgLogo?: string | null;
  groupTitle?: string | null;

  status: LiveChannelStatus;

  lastChecked?: string | null;
  favorite?: boolean;
  viewCount?: number;
  /** Access classification derived from the stream URL/source metadata. */
  accessType?: 'PUBLIC' | 'PROTECTED';
}

/**
 * Spring Boot / Spring Data paginated response.
 *
 * IMPORTANT:
 * `number` is zero-based.
 *
 * Page 1 -> number = 0
 * Page 2 -> number = 1
 * Page 14 -> number = 13
 */
export interface LiveTvResponse {
  content: LiveChannel[];

  pageable?: unknown;

  totalElements: number;
  totalPages: number;

  size: number;
  number: number;

  first: boolean;
  last: boolean;

  numberOfElements: number;
  empty: boolean;
}

export interface LiveTvReadiness {
  ready: boolean;
  syncInProgress: boolean;
  channelCount: number;
}

export interface CountryInfo {
  code: string;
  name: string;
  channelCount: number;
}

export interface CategoryInfo {
  name: string;
  channelCount: number;
}

export interface EpgProgram {
  channelId: string;
  title: string;
  startTime: string;
  endTime: string;
  description?: string;
  category?: string;
  icon?: string;
  season?: number;
  episode?: number;
  episodeTitle?: string;
}

export interface PlaybackAttempt {
  url: string;
  profile: string;
  status: number;
  responseTimeMs: number;
  reason?: string | null;
}

export interface PlaybackDiagnostic {
  channelId: string;
  tvgId: string;
  channelName: string;
  classification: 'PLAYING' | 'UNAVAILABLE' | 'ERROR';
  userMessage: string;
  mainReason: string;
  successfulUrl: string;
  status: number;
  attempts: PlaybackAttempt[];
  checkedAt: string;
}

export interface StreamCandidateInfo {
  url: string;
  proxyUrl: string;
  feedId?: string | null;
  quality?: string | null;
  status?: LiveChannelStatus | string;
  /** True only when the provider explicitly permits browser HLS from the configured web origin. */
  directBrowserCompatible?: boolean;
}

export interface YouTubeFallbackResponse {
  enabled: boolean;
  found: boolean;
  videoId: string;
  title: string;
  channelTitle: string;
  matchScore: number;
  reason: string;
}

export interface EpgResponse {
  programs: EpgProgram[];
  channelId: string;
  date: string;
}

/**
 * Parameters sent to the backend.
 *
 * All filtering is SERVER-SIDE.
 */
export interface LiveTvChannelParams {
  /**
   * ISO country code.
   *
   * Example:
   * IN
   * US
   * GB
   */
  country?: string;

  /**
   * Channel category.
   */
  category?: string;

  /**
   * Search text.
   */
  search?: string;

  /**
   * Zero-based Spring page number.
   */
  page?: number;

  /**
   * Number of records.
   *
   * Live-TV uses a maximum of 50 records per page.
   */
  size?: number;

  /**
   * Spring Data sort syntax.
   *
   * Examples:
   * name,asc
   * name,desc
   * country,asc
   * category,asc
   */
  sort?: string;
}

/**
 * IPTV.org public API endpoints.
 *
 * These constants are only useful for synchronization/import jobs.
 * The Live-TV UI must NOT download the entire IPTV catalogue.
 */
export const IPTV_ORG_BASE =
  'https://iptv-org.github.io/api';

export const IPTV_CHANNELS_URL =
  `${IPTV_ORG_BASE}/channels.json`;

export const IPTV_STREAMS_URL =
  `${IPTV_ORG_BASE}/streams.json`;

export const IPTV_CATEGORIES_URL =
  `${IPTV_ORG_BASE}/categories.json`;

export const IPTV_COUNTRIES_URL =
  `${IPTV_ORG_BASE}/countries.json`;

/**
 * Live-TV page size.
 *
 * The UI will never request more than 50 records.
 */
export const LIVE_TV_PAGE_SIZE = 50;

/**
 * Normalize country.
 *
 * Backend expects ISO country codes such as:
 *
 * IN
 * US
 * GB
 * AU
 */
function normalizeCountry(
  country?: string
): string | undefined {
  if (!country) {
    return undefined;
  }

  const value = country.trim();

  if (!value) {
    return undefined;
  }

  return value.toUpperCase();
}

/**
 * Normalize category.
 */
function normalizeCategory(
  category?: string
): string | undefined {
  if (!category) {
    return undefined;
  }

  const value = category.trim();

  return value || undefined;
}

/**
 * Normalize search.
 */
function normalizeSearch(
  search?: string
): string | undefined {
  if (!search) {
    return undefined;
  }

  const value = search.trim();

  return value || undefined;
}

/**
 * Normalize the ZERO-BASED Spring page number.
 *
 * IMPORTANT:
 * This function is intentionally named `normalizePageNumber`
 * so it cannot collide with the backend-response `normalizePage`.
 */
function normalizePageNumber(
  page?: number
): number {
  if (
    page === undefined ||
    !Number.isFinite(page) ||
    page < 0
  ) {
    return 0;
  }

  return Math.floor(page);
}

/**
 * Normalize page size.
 *
 * Live-TV deliberately uses a maximum of 50.
 */
function normalizePageSize(
  size?: number
): number {
  if (
    size === undefined ||
    !Number.isFinite(size) ||
    size <= 0
  ) {
    return LIVE_TV_PAGE_SIZE;
  }

  return Math.min(
    Math.floor(size),
    LIVE_TV_PAGE_SIZE
  );
}

/**
 * Convert an unknown value to a finite number safely.
 */
function toNumber(
  value: unknown,
  fallback = 0
): number {
  if (
    typeof value === 'number' &&
    Number.isFinite(value)
  ) {
    return value;
  }

  if (typeof value === 'string') {
    const parsed = Number(value);

    if (Number.isFinite(parsed)) {
      return parsed;
    }
  }

  return fallback;
}

/**
 * Empty paginated response.
 */
function emptyPage(): LiveTvResponse {
  return {
    content: [],
    totalElements: 0,
    totalPages: 0,
    size: LIVE_TV_PAGE_SIZE,
    number: 0,
    first: true,
    last: true,
    numberOfElements: 0,
    empty: true,
  };
}

/**
 * Normalize the backend response.
 *
 * Supported formats:
 *
 * 1. Standard Spring Page:
 *
 * {
 *   content: [...],
 *   totalElements: 690,
 *   totalPages: 14,
 *   size: 50,
 *   number: 0,
 *   first: true,
 *   last: false,
 *   numberOfElements: 50,
 *   empty: false
 * }
 *
 * 2. Wrapped response:
 *
 * {
 *   data: {
 *     content: [...]
 *   }
 * }
 *
 * 3. Raw array:
 *
 * [
 *   ...
 * ]
 *
 * The backend should preferably return the Spring Page directly.
 */
function normalizePage(
  response: unknown
): LiveTvResponse {
  if (response == null) {
    return emptyPage();
  }

  /**
   * Raw array response.
   *
   * Check this BEFORE object processing.
   */
  if (Array.isArray(response)) {
    const content =
      response as LiveChannel[];

    return {
      content,
      totalElements: content.length,
      totalPages:
        content.length > 0 ? 1 : 0,
      size: content.length,
      number: 0,
      first: true,
      last: true,
      numberOfElements: content.length,
      empty: content.length === 0,
    };
  }

  if (typeof response !== 'object') {
    return emptyPage();
  }

  const value =
    response as Record<string, unknown>;

  /**
   * Some HTTP abstractions may return:
   *
   * {
   *   data: {...}
   * }
   *
   * Only unwrap `data` when it is an object.
   */
  if (
    value.data &&
    typeof value.data === 'object'
  ) {
    return normalizePage(value.data);
  }

  /**
   * Standard Spring Data Page.
   */
  if (Array.isArray(value.content)) {
    const content =
      value.content as LiveChannel[];

    const totalElements =
      toNumber(
        value.totalElements,
        content.length
      );

    const size =
      toNumber(
        value.size,
        LIVE_TV_PAGE_SIZE
      );

    const safeSize =
      size > 0
        ? size
        : LIVE_TV_PAGE_SIZE;

    const number =
      Math.max(
        0,
        Math.floor(
          toNumber(value.number, 0)
        )
      );

    const calculatedTotalPages =
      totalElements > 0
        ? Math.ceil(
            totalElements / safeSize
          )
        : 0;

    const totalPages =
      Math.max(
        0,
        Math.floor(
          toNumber(
            value.totalPages,
            calculatedTotalPages
          )
        )
      );

    return {
      content,

      pageable: value.pageable,

      totalElements,

      totalPages,

      size: safeSize,

      number,

      first:
        typeof value.first === 'boolean'
          ? value.first
          : number === 0,

      last:
        typeof value.last === 'boolean'
          ? value.last
          : totalPages === 0 ||
            number >= totalPages - 1,

      numberOfElements:
        toNumber(
          value.numberOfElements,
          content.length
        ),

      empty:
        typeof value.empty === 'boolean'
          ? value.empty
          : content.length === 0,
    };
  }

  /**
   * Some legacy responses may return:
   *
   * {
   *   content: {
   *     content: [...]
   *   }
   * }
   */
  if (
    value.content &&
    typeof value.content === 'object'
  ) {
    return normalizePage(
      value.content
    );
  }

  return emptyPage();
}

/**
 * Normalize a list response.
 *
 * Supported:
 *
 * []
 *
 * {
 *   data: []
 * }
 *
 * {
 *   content: []
 * }
 *
 * {
 *   data: {
 *     content: []
 *   }
 * }
 */
function normalizeList<T>(
  response: unknown
): T[] {
  if (Array.isArray(response)) {
    return response as T[];
  }

  if (
    !response ||
    typeof response !== 'object'
  ) {
    return [];
  }

  const value =
    response as Record<string, unknown>;

  if (Array.isArray(value.data)) {
    return value.data as T[];
  }

  if (Array.isArray(value.content)) {
    return value.content as T[];
  }

  if (
    value.data &&
    typeof value.data === 'object'
  ) {
    const data =
      value.data as Record<
        string,
        unknown
      >;

    if (Array.isArray(data.content)) {
      return data.content as T[];
    }
  }

  return [];
}

/**
 * Validate channel ID before making a request.
 */
function requireChannelId(
  channelId: string
): string {
  const value =
    channelId?.trim();

  if (!value) {
    throw new Error(
      'channelId is required'
    );
  }

  return value;
}

/**
 * Live-TV API.
 */
export const liveTvApi = {
  async getReadiness(): Promise<LiveTvReadiness> {
    return http.get<LiveTvReadiness>('/live/readiness');
  },

  /**
   * ============================================================
   * CHANNELS
   * ============================================================
   *
   * Fetch exactly ONE page.
   *
   * This is NOT an infinite-query API.
   *
   * All filtering is server-side.
   */
  async getChannels(
    params: LiveTvChannelParams = {}
  ): Promise<LiveTvResponse> {
    const requestParams = {
      /**
       * undefined means all countries.
       */
      country:
        normalizeCountry(
          params.country
        ),

      /**
       * undefined means all categories.
       */
      category:
        normalizeCategory(
          params.category
        ),

      /**
       * undefined means no search filter.
       */
      search:
        normalizeSearch(
          params.search
        ),

      /**
       * Spring Boot uses zero-based pages.
       */
      page:
        normalizePageNumber(
          params.page
        ),

      /**
       * Never request more than 50.
       */
      size:
        normalizePageSize(
          params.size
        ),

      /**
       * Default server-side sorting.
       */
      sort:
        params.sort?.trim() ||
        'name,asc',
    };

    const response =
      await http.get<unknown>(
        '/live/channels',
        {
          params: requestParams,
        }
      );

    return normalizePage(response);
  },

  /**
   * ============================================================
   * COUNTRIES
   * ============================================================
   */
  async getCountries(params: { category?: string; search?: string } = {}): Promise<
    CountryInfo[]
  > {
    const response =
      await http.get<unknown>(
        '/live/countries',
        {
          params: {
            category: normalizeCategory(params.category),
            search: normalizeSearch(params.search),
          },
        }
      );

    return normalizeList<CountryInfo>(
      response
    );
  },

  /**
   * ============================================================
   * CATEGORIES
   * ============================================================
   */
  async getCategories(params: { country?: string; search?: string } = {}): Promise<
    CategoryInfo[]
  > {
    const response =
      await http.get<unknown>(
        '/live/categories',
        {
          params: {
            country: normalizeCountry(params.country),
            search: normalizeSearch(params.search),
          },
        }
      );

    return normalizeList<CategoryInfo>(
      response
    );
  },

  /**
   * ============================================================
   * SINGLE CHANNEL
   * ============================================================
   */
  async getChannel(
    channelId: string
  ): Promise<LiveChannel> {
    const id =
      requireChannelId(channelId);

    return http.get<LiveChannel>(
      `/live/channels/${encodeURIComponent(id)}`
    );
  },

  /**
   * Capture/diagnose the complete upstream playback path after a playback error.
   */
  async getPlaybackDiagnostic(channelId: string): Promise<PlaybackDiagnostic> {
    const id = requireChannelId(channelId);
    return http.get<PlaybackDiagnostic>(
      `/live/channels/${encodeURIComponent(id)}/playback-diagnostic`
    );
  },

  async getStreamCandidates(channelId: string): Promise<StreamCandidateInfo[]> {
    const id = requireChannelId(channelId);
    return http.get<StreamCandidateInfo[]>(`/live/channels/${encodeURIComponent(id)}/stream-candidates`);
  },

  async getYouTubeFallback(channelId: string): Promise<YouTubeFallbackResponse> {
    const id = requireChannelId(channelId);
    return http.get<YouTubeFallbackResponse>(`/live/channels/${encodeURIComponent(id)}/youtube-fallback`);
  },

  /**
   * ============================================================
   * EPG
   * ============================================================
   */
  async getEpg(
    channelId: string,
    date?: string
  ): Promise<EpgResponse> {
    const id =
      requireChannelId(channelId);

    return http.get<EpgResponse>(
      `/live/channels/${encodeURIComponent(id)}/epg`,
      {
        params: date
          ? { date }
          : undefined,
      }
    );
  },

  /**
   * ============================================================
   * CHANNEL STATUS
   * ============================================================
   */
  async checkChannelStatus(
    channelId: string
  ): Promise<LiveChannel> {
    const id =
      requireChannelId(channelId);

    return http.post<LiveChannel>(
      `/live/channels/${encodeURIComponent(id)}/check`
    );
  },

  /**
   * Toggle the channel favorite flag.
   */
  async toggleFavorite(
    channelId: string
  ): Promise<LiveChannel> {
    const id = requireChannelId(channelId);

    return http.post<LiveChannel>(
      `/live/channels/${encodeURIComponent(id)}/favorite`
    );
  },

  /**
   * Record a channel view after playback is started.
   */
  async recordView(
    channelId: string
  ): Promise<LiveChannel> {
    const id = requireChannelId(channelId);

    return http.post<LiveChannel>(
      `/live/channels/${encodeURIComponent(id)}/view`
    );
  },

  /**
   * ============================================================
   * FEATURED CHANNELS
   * ============================================================
   */
  async getFeatured(): Promise<
    LiveChannel[]
  > {
    const response =
      await http.get<unknown>(
        '/live/featured'
      );

    return normalizeList<LiveChannel>(
      response
    );
  },
};

export default liveTvApi;