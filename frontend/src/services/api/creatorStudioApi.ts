import { http } from '../http';

export type StudioCategory =
  | 'DANCE_CHOREOGRAPHY' | 'MUSIC_COMPOSITION' | 'BICYCLE_ACTION_STUNTS'
  | 'PAINTING_VISUAL_ARTS' | 'VINTAGE_COLLECTIBLES' | 'COMIC_WRITERS_ILLUSTRATORS'
  | 'POETRY_LITERATURE' | 'RELIGIOUS_SCRIPTURAL_DISCUSSIONS' | 'SHORT_FILMS_MOVIE_MAKING'
  | 'NEWS_ANCHORING_PRESENTATION' | 'BRAND_PROMOTION_ADVERTISING' | 'EDUCATION_ONLINE_TEACHING'
  | 'ACTING_THEATRE' | 'PHOTOGRAPHY_CINEMATOGRAPHY' | 'COMEDY_STANDUP'
  | 'FITNESS_YOGA' | 'COOKING_CULINARY_ARTS' | 'CRAFTSMANSHIP_DIY'
  | 'STORYTELLING_AUDIOBOOKS' | 'DEBATE_PUBLIC_SPEAKING';

export interface CreatorStudio {
  id: string;
  name: string;
  displayName: string;
  description?: string;
  avatarUrl?: string;
  bannerUrl?: string;
  primaryCategory: StudioCategory;
  ownerType: 'INDIVIDUAL' | 'TEAM';
  ownerId: string;
  isVerified: boolean;
  isActive: boolean;
  createdAt: string;
  updatedAt?: string;
}

export interface CreateStudioPayload {
  name: string;
  displayName: string;
  description?: string;
  primaryCategory: StudioCategory;
  ownerType: 'INDIVIDUAL' | 'TEAM';
}

function unwrap<T>(response: any): T {
  if (response && response.success === false) {
    throw new Error(response.error || 'Creator Studio request failed.');
  }
  return (response?.data ?? response) as T;
}

/**
 * Deliberately limited to Vite's local development server. A production build
 * cannot enable this path because import.meta.env.DEV is compiled to false.
 * In this mode Creator Studio uses browser-local demo data and never disables
 * authentication or authorization in the backend.
 */
export const isCreatorStudioDevNoLogin =
  import.meta.env.DEV && import.meta.env.VITE_CREATOR_STUDIO_DEV_NO_LOGIN === 'true';

const DEV_STUDIO_STORAGE_KEY = 'creator-studio-dev-no-login:studios:v1';
const devOwnerId = 'local-dev-owner';

function readDevStudios(): CreatorStudio[] {
  try {
    const raw = localStorage.getItem(DEV_STUDIO_STORAGE_KEY);
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed as CreatorStudio[] : [];
  } catch {
    return [];
  }
}

function writeDevStudios(studios: CreatorStudio[]): void {
  localStorage.setItem(DEV_STUDIO_STORAGE_KEY, JSON.stringify(studios));
}

function devCheckName(name: string): { name: string; valid: boolean; available: boolean } {
  const trimmed = name.trim();
  const valid = trimmed.length >= 3 && trimmed.length <= 100 && /^[A-Za-z0-9][A-Za-z0-9._ -]*$/.test(trimmed);
  const available = valid && !readDevStudios().some((studio) => studio.name.toLowerCase() === trimmed.toLowerCase() && studio.isActive);
  return { name: trimmed, valid, available };
}

function createDevStudio(payload: CreateStudioPayload): CreatorStudio {
  const nameCheck = devCheckName(payload.name);
  if (!nameCheck.valid) throw new Error('Studio name must be 3–100 characters and use letters, numbers, spaces, dots, underscores or hyphens.');
  if (!nameCheck.available) throw new Error('A studio with this name already exists in this browser.');
  const now = new Date().toISOString();
  const studio: CreatorStudio = {
    id: typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function' ? crypto.randomUUID() : `dev-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`,
    name: payload.name.trim(),
    displayName: payload.displayName.trim() || payload.name.trim(),
    description: payload.description?.trim() || '',
    primaryCategory: payload.primaryCategory,
    ownerType: payload.ownerType,
    ownerId: devOwnerId,
    isVerified: false,
    isActive: true,
    createdAt: now,
    updatedAt: now,
  };
  writeDevStudios([studio, ...readDevStudios()]);
  return studio;
}

export const creatorStudioApi = {
  async mine(): Promise<CreatorStudio[]> {
    if (isCreatorStudioDevNoLogin) return readDevStudios().filter((studio) => studio.isActive);
    return unwrap<CreatorStudio[]>(await http.get('/creator-studios/mine'));
  },
  async checkName(name: string): Promise<{ name: string; valid: boolean; available: boolean }> {
    if (isCreatorStudioDevNoLogin) return devCheckName(name);
    return unwrap(await http.get(`/creator-studios/name-availability?name=${encodeURIComponent(name)}`));
  },
  async create(payload: CreateStudioPayload): Promise<CreatorStudio> {
    if (isCreatorStudioDevNoLogin) return createDevStudio(payload);
    return unwrap<CreatorStudio>(await http.post('/creator-studios', payload));
  },
  async get(studioId: string): Promise<CreatorStudio> {
    if (isCreatorStudioDevNoLogin) {
      const studio = readDevStudios().find((item) => item.id === studioId && item.isActive);
      if (!studio) throw new Error('This local demo studio was not found in this browser.');
      return studio;
    }
    return unwrap<CreatorStudio>(await http.get(`/creator-studios/${studioId}`));
  },
  async update(studioId: string, payload: Partial<CreateStudioPayload>): Promise<CreatorStudio> {
    if (isCreatorStudioDevNoLogin) {
      const studios = readDevStudios();
      const index = studios.findIndex((item) => item.id === studioId && item.isActive);
      if (index < 0) throw new Error('This local demo studio was not found in this browser.');
      const updated = { ...studios[index], ...payload, updatedAt: new Date().toISOString() };
      studios[index] = updated;
      writeDevStudios(studios);
      return updated;
    }
    return unwrap<CreatorStudio>(await http.patch(`/creator-studios/${studioId}`, payload));
  },
  async deactivate(studioId: string): Promise<void> {
    if (isCreatorStudioDevNoLogin) {
      writeDevStudios(readDevStudios().map((studio) => studio.id === studioId ? { ...studio, isActive: false, updatedAt: new Date().toISOString() } : studio));
      return;
    }
    unwrap(await http.delete(`/creator-studios/${studioId}`));
  },
};
