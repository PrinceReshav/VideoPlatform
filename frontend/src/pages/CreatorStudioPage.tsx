import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  ArrowLeft, ArrowRight, BookOpen, Camera, Check, Clapperboard, Compass, Drama,
  FileText, Headphones, Image, Loader2, Mic2, MonitorPlay, Palette, Plus,
  Radio, Search, ShieldCheck, Sparkles, Users, Video, X,
} from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { creatorStudioApi, isCreatorStudioDevNoLogin, type CreatorStudio, type StudioCategory } from '../services/api/creatorStudioApi';

const categories: { id: StudioCategory; label: string; description: string; icon: any }[] = [
  { id: 'DANCE_CHOREOGRAPHY', label: 'Dance & Choreography', description: 'Rehearsals, choreography and performances', icon: Drama },
  { id: 'MUSIC_COMPOSITION', label: 'Music & Composition', description: 'Music sessions, releases and performances', icon: Headphones },
  { id: 'BICYCLE_ACTION_STUNTS', label: 'Bicycle & Action Stunts', description: 'Camera-first action, stunt videos and ride sessions', icon: Camera },
  { id: 'PAINTING_VISUAL_ARTS', label: 'Painting & Visual Arts', description: 'Art process, showcases and collections', icon: Palette },
  { id: 'VINTAGE_COLLECTIBLES', label: 'Vintage & Collectibles', description: 'Collections, provenance and live showcases', icon: Search },
  { id: 'COMIC_WRITERS_ILLUSTRATORS', label: 'Comics & Illustration', description: 'Panels, characters and drawing sessions', icon: Image },
  { id: 'POETRY_LITERATURE', label: 'Poetry & Literature', description: 'Solo poetry, readings and Kavi Sammelan events', icon: BookOpen },
  { id: 'RELIGIOUS_SCRIPTURAL_DISCUSSIONS', label: 'Scripture & Discussion', description: 'Study, discourse and respectful discussion', icon: BookOpen },
  { id: 'SHORT_FILMS_MOVIE_MAKING', label: 'Short Films & Filmmaking', description: 'Plan, produce and present short films up to 20 minutes', icon: Clapperboard },
  { id: 'NEWS_ANCHORING_PRESENTATION', label: 'News & Anchoring', description: 'Newsroom, rundown, teleprompter and broadcast preview', icon: Radio },
  { id: 'BRAND_PROMOTION_ADVERTISING', label: 'Brand & Advertising', description: 'Campaigns, promos and branded content', icon: Sparkles },
  { id: 'EDUCATION_ONLINE_TEACHING', label: 'Education & Teaching', description: 'Lessons, resources and class sessions', icon: MonitorPlay },
  { id: 'ACTING_THEATRE', label: 'Acting & Theatre', description: 'Scenes, rehearsals, cast and stage cues', icon: Drama },
  { id: 'PHOTOGRAPHY_CINEMATOGRAPHY', label: 'Photography & Cinematography', description: 'Shoot planning, galleries and visual projects', icon: Camera },
  { id: 'COMEDY_STANDUP', label: 'Stand-up Comedy', description: 'Set lists, stage timing and performance notes', icon: Mic2 },
  { id: 'FITNESS_YOGA', label: 'Fitness & Yoga', description: 'Routines, programs and guided sessions', icon: Users },
  { id: 'COOKING_CULINARY_ARTS', label: 'Cooking & Culinary Arts', description: 'Recipes, ingredients and cooking demos', icon: Video },
  { id: 'CRAFTSMANSHIP_DIY', label: 'Crafts & DIY', description: 'Build guides, materials and step-by-step projects', icon: Palette },
  { id: 'STORYTELLING_AUDIOBOOKS', label: 'Storytelling & Audiobooks', description: 'Narration, chapters and audio-first releases', icon: Headphones },
  { id: 'DEBATE_PUBLIC_SPEAKING', label: 'Debate & Public Speaking', description: 'Arguments, speaking notes and structured sessions', icon: Mic2 },
];

const categoryById = Object.fromEntries(categories.map((item) => [item.id, item])) as Record<StudioCategory, typeof categories[number]>;

export function CreatorStudioPage() {
  const navigate = useNavigate();
  const { studioId } = useParams();
  const [studios, setStudios] = useState<CreatorStudio[]>([]);
  const [selectedStudio, setSelectedStudio] = useState<CreatorStudio | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [step, setStep] = useState(1);
  const [search, setSearch] = useState('');
  const [name, setName] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState<StudioCategory | ''>('');
  const [ownerType, setOwnerType] = useState<'INDIVIDUAL' | 'TEAM'>('INDIVIDUAL');
  const [nameState, setNameState] = useState<'idle' | 'checking' | 'available' | 'taken' | 'invalid'>('idle');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [created, setCreated] = useState<CreatorStudio | null>(null);
  const [poetryFormat, setPoetryFormat] = useState<'SOLO' | 'KAVI_SAMMELAN'>('SOLO');

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    creatorStudioApi.mine()
      .then((data) => { if (!cancelled) { setStudios(data || []); setLoadError(''); } })
      .catch((e: any) => { if (!cancelled) setLoadError(e?.response?.data?.error || e?.message || 'Could not load your studios.'); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    if (!studioId) { setSelectedStudio(null); return; }
    const existing = studios.find((item) => item.id === studioId);
    if (existing) { setSelectedStudio(existing); return; }
    let cancelled = false;
    creatorStudioApi.get(studioId)
      .then((studio) => { if (!cancelled) setSelectedStudio(studio); })
      .catch((e: any) => { if (!cancelled) setError(e?.response?.data?.error || 'This studio could not be opened.'); });
    return () => { cancelled = true; };
  }, [studioId, studios]);

  useEffect(() => {
    if (!studioId) return;
    const savedFormat = localStorage.getItem(`creator-studio-poetry-format:${studioId}`);
    if (savedFormat === 'KAVI_SAMMELAN' || savedFormat === 'SOLO') setPoetryFormat(savedFormat);
  }, [studioId]);

  useEffect(() => {
    const trimmed = name.trim();
    if (!trimmed) { setNameState('idle'); return; }
    if (trimmed.length < 3 || trimmed.length > 100 || !/^[A-Za-z0-9][A-Za-z0-9._ -]*$/.test(trimmed)) {
      setNameState('invalid');
      return;
    }
    let cancelled = false;
    setNameState('checking');
    const timer = window.setTimeout(() => {
      creatorStudioApi.checkName(trimmed)
        .then((result) => { if (!cancelled) setNameState(result.valid && result.available ? 'available' : result.valid ? 'taken' : 'invalid'); })
        .catch(() => { if (!cancelled) setNameState('idle'); });
    }, 350);
    return () => { cancelled = true; window.clearTimeout(timer); };
  }, [name]);

  const filteredCategories = useMemo(() => categories.filter((item) =>
    `${item.label} ${item.description}`.toLowerCase().includes(search.toLowerCase())), [search]);

  const startCreate = () => {
    setCreated(null); setError(''); setStep(1); setName(''); setDisplayName('');
    setDescription(''); setCategory(''); setOwnerType('INDIVIDUAL');
    setSelectedStudio(null); navigate('/creator-studio', { replace: true });
  };

  const createStudio = async () => {
    if (!category || nameState !== 'available') return;
    setSaving(true); setError('');
    try {
      const studio = await creatorStudioApi.create({
        name: name.trim(),
        displayName: displayName.trim() || name.trim(),
        description: description.trim(),
        primaryCategory: category,
        ownerType,
      });
      setCreated(studio);
      setStudios((current) => [studio, ...current.filter((item) => item.id !== studio.id)]);
      setSelectedStudio(studio);
      navigate(`/creator-studio/${studio.id}`, { replace: true });
    } catch (e: any) {
      setError(e?.response?.data?.error || e?.message || 'Studio creation failed. Check your creator permissions and try again.');
    } finally {
      setSaving(false);
    }
  };

  const activeStudio = selectedStudio || created;
  const activeCategory = activeStudio ? categoryById[activeStudio.primaryCategory] : null;

  if (activeStudio) {
    const Icon = activeCategory?.icon || Sparkles;
    const categoryId = activeStudio.primaryCategory;
    const isNews = categoryId === 'NEWS_ANCHORING_PRESENTATION';
    const isStunts = categoryId === 'BICYCLE_ACTION_STUNTS';
    const isComedy = categoryId === 'COMEDY_STANDUP';
    const isPoetry = categoryId === 'POETRY_LITERATURE';
    const isFilm = categoryId === 'SHORT_FILMS_MOVIE_MAKING';
    const isEducation = categoryId === 'EDUCATION_ONLINE_TEACHING';
    const workspacePanels: Record<StudioCategory, { title: string; detail: string; icon: any }[]> = {
      DANCE_CHOREOGRAPHY: [
        { title: 'Choreography board', detail: 'Break routines into counts, formations and transitions.', icon: Drama },
        { title: 'Rehearsal takes', detail: 'Organize practice clips by routine and revision.', icon: Video },
        { title: 'Stage & music cues', detail: 'Track entrances, formations and soundtrack timing.', icon: Headphones },
      ],
      MUSIC_COMPOSITION: [
        { title: 'Track workspace', detail: 'Group demos, stems, mixes and release versions.', icon: Headphones },
        { title: 'Set list & cues', detail: 'Arrange songs, transitions and performance notes.', icon: FileText },
        { title: 'Release board', detail: 'Track artwork, credits and planned release assets.', icon: Radio },
      ],
      BICYCLE_ACTION_STUNTS: [
        { title: 'Camera & shot plan', detail: 'Plan camera angles, spotters and safety checks before a stunt.', icon: Camera },
        { title: 'Stunt video library', detail: 'Organize ride clips, takes, edits and helmet-cam footage.', icon: Clapperboard },
        { title: 'Live session setup', detail: 'Prepare a session when compatible streaming infrastructure is configured.', icon: Radio },
      ],
      PAINTING_VISUAL_ARTS: [
        { title: 'Canvas & process log', detail: 'Document work-in-progress stages and materials.', icon: Palette },
        { title: 'Artwork gallery', detail: 'Curate finished pieces, studies and collection views.', icon: Image },
        { title: 'Materials shelf', detail: 'Track pigments, tools, dimensions and display notes.', icon: FileText },
      ],
      VINTAGE_COLLECTIBLES: [
        { title: 'Collection catalogue', detail: 'Record item details, condition, provenance and photographs.', icon: Search },
        { title: 'Showcase table', detail: 'Arrange featured pieces for a live or recorded reveal.', icon: Image },
        { title: 'Provenance & notes', detail: 'Keep references and authenticity documentation organized.', icon: FileText },
      ],
      COMIC_WRITERS_ILLUSTRATORS: [
        { title: 'Story & panel board', detail: 'Map scenes, page beats, panels and dialogue.', icon: Image },
        { title: 'Character bible', detail: 'Keep character sheets, references and continuity notes together.', icon: BookOpen },
        { title: 'Ink-to-colour pipeline', detail: 'Track sketches, line art, colour and final pages.', icon: Palette },
      ],
      POETRY_LITERATURE: [
        { title: 'Manuscripts & readings', detail: 'Keep poems, readings, recitation order and performance notes together.', icon: BookOpen },
        { title: 'Podium & delivery notes', detail: 'Prepare pauses, emphasis, language and stage cues.', icon: FileText },
        { title: 'Performer queue', detail: 'Arrange the order of poets for a multi-performer Kavi Sammelan.', icon: Users },
      ],
      RELIGIOUS_SCRIPTURAL_DISCUSSIONS: [
        { title: 'Study outline', detail: 'Organize passages, references and discussion questions.', icon: BookOpen },
        { title: 'Discourse notes', detail: 'Prepare topic flow, context and respectful discussion guidelines.', icon: FileText },
        { title: 'Reading resources', detail: 'Keep supporting documents and recorded sessions together.', icon: Headphones },
      ],
      SHORT_FILMS_MOVIE_MAKING: [
        { title: 'Production board', detail: 'Organize scenes, cast, shot lists and editing notes.', icon: Clapperboard },
        { title: 'Film library', detail: 'Keep cuts, trailers and release versions together.', icon: Video },
        { title: 'Runtime check', detail: 'Target a final short film of 20 minutes or less.', icon: FileText },
      ],
      NEWS_ANCHORING_PRESENTATION: [
        { title: 'Broadcast rundown', detail: 'Arrange headlines, segments and transitions before going on air.', icon: Radio },
        { title: 'Teleprompter', detail: 'Prepare anchor copy, cues and reading pace.', icon: FileText },
        { title: 'News media library', detail: 'Keep approved clips, images and supporting material ready.', icon: Image },
      ],
      BRAND_PROMOTION_ADVERTISING: [
        { title: 'Campaign board', detail: 'Group concepts, deliverables, formats and release dates.', icon: Sparkles },
        { title: 'Creative review', detail: 'Compare promo cuts, copy variants and approved assets.', icon: Video },
        { title: 'Brand asset shelf', detail: 'Keep logos, product shots and campaign guidelines together.', icon: Image },
      ],
      EDUCATION_ONLINE_TEACHING: [
        { title: 'Lesson planner', detail: 'Arrange lesson modules, learning objectives and class flow.', icon: BookOpen },
        { title: 'Class materials', detail: 'Organize slides, documents, demonstrations and recordings.', icon: FileText },
        { title: 'Session room', detail: 'Prepare a lesson or discussion when live tools are enabled.', icon: MonitorPlay },
      ],
      ACTING_THEATRE: [
        { title: 'Rehearsal script', detail: 'Organize scenes, dialogue, blocking and revision notes.', icon: Drama },
        { title: 'Cast & cues', detail: 'Track performers, entrances, exits and stage transitions.', icon: Users },
        { title: 'Scene takes', detail: 'Review rehearsals and performance recordings by scene.', icon: Video },
      ],
      PHOTOGRAPHY_CINEMATOGRAPHY: [
        { title: 'Shot list & moodboard', detail: 'Plan framing, lighting, lenses and visual references.', icon: Camera },
        { title: 'Contact sheets', detail: 'Review selects and organize photo/video takes.', icon: Image },
        { title: 'Project archive', detail: 'Group shoots by client, location or creative project.', icon: FileText },
      ],
      COMEDY_STANDUP: [
        { title: 'Set list & timing', detail: 'Arrange bits, transitions and estimated stage time.', icon: FileText },
        { title: 'Performance clips', detail: 'Review previous sets and upcoming material.', icon: Video },
        { title: 'Stage notes', detail: 'Track callbacks, transitions and delivery notes.', icon: Mic2 },
      ],
      FITNESS_YOGA: [
        { title: 'Routine builder', detail: 'Arrange sequences, repetitions, timing and rest periods.', icon: Users },
        { title: 'Movement library', detail: 'Organize demonstrations and form-reference clips.', icon: Video },
        { title: 'Class schedule', detail: 'Plan guided sessions and program releases.', icon: Radio },
      ],
      COOKING_CULINARY_ARTS: [
        { title: 'Recipe card', detail: 'Structure ingredients, quantities, prep and cooking steps.', icon: FileText },
        { title: 'Kitchen prep board', detail: 'Arrange mise en place, tools and shot sequence.', icon: Video },
        { title: 'Dish gallery', detail: 'Collect plating references and finished dish images.', icon: Image },
      ],
      CRAFTSMANSHIP_DIY: [
        { title: 'Build sequence', detail: 'Break projects into clear, repeatable steps.', icon: FileText },
        { title: 'Tools & materials', detail: 'List supplies, measurements and safety notes.', icon: Palette },
        { title: 'Progress captures', detail: 'Keep before, in-progress and finished project media together.', icon: Camera },
      ],
      STORYTELLING_AUDIOBOOKS: [
        { title: 'Chapter navigator', detail: 'Arrange chapters, narration takes and reading order.', icon: BookOpen },
        { title: 'Voice booth notes', detail: 'Track pronunciation, tone, pacing and pickup lines.', icon: Headphones },
        { title: 'Audio release shelf', detail: 'Organize mastered files, artwork and episode notes.', icon: Radio },
      ],
      DEBATE_PUBLIC_SPEAKING: [
        { title: 'Argument map', detail: 'Arrange claims, evidence, rebuttals and counterpoints.', icon: FileText },
        { title: 'Speech timer', detail: 'Plan opening, evidence, response and closing sections.', icon: Mic2 },
        { title: 'Practice recordings', detail: 'Review delivery, clarity and timing across practice takes.', icon: Video },
      ],
    };
    const panels = workspacePanels[categoryId] || workspacePanels.POETRY_LITERATURE;

    return (
      <div className="mx-auto max-w-7xl space-y-6">
        {isCreatorStudioDevNoLogin && <div role="status" className="flex items-start gap-3 rounded-2xl border border-violet-400/40 bg-violet-500/10 px-4 py-3 text-sm text-[var(--color-text-primary)]"><Sparkles className="mt-0.5 h-4 w-4 shrink-0 text-violet-500" /><div><strong className="block">Development sandbox · login not required</strong><span className="mt-0.5 block text-[var(--color-text-secondary)]">Studios and preferences are stored in this browser only. Payment, KYC and live-provider integrations are not connected in this mode.</span></div></div>}
        <button onClick={() => { setSelectedStudio(null); navigate('/creator-studio'); }} className="inline-flex items-center gap-2 text-sm text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]">
          <ArrowLeft className="h-4 w-4" /> All studios
        </button>
        <section className="overflow-hidden rounded-3xl border border-[var(--color-border)] bg-[var(--color-surface)]">
          <div className="relative bg-gradient-to-br from-indigo-600 via-violet-600 to-fuchsia-600 px-6 py-8 text-white sm:px-9">
            <div className="absolute right-5 top-4 opacity-20"><Icon className="h-28 w-28" /></div>
            <div className="relative max-w-3xl">
              <div className="mb-3 inline-flex items-center gap-2 rounded-full bg-white/15 px-3 py-1 text-xs font-semibold backdrop-blur">
                <Sparkles className="h-3.5 w-3.5" /> CREATOR WORKSPACE
              </div>
              <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">{activeStudio.displayName || activeStudio.name}</h1>
              <p className="mt-2 max-w-2xl text-sm text-white/80">{activeStudio.description || activeCategory?.description || 'Your creative workspace, organized around the way you create.'}</p>
              <div className="mt-5 flex flex-wrap gap-2 text-xs">
                <span className="rounded-full bg-white/15 px-3 py-1.5">{activeCategory?.label || activeStudio.primaryCategory}</span>
                <span className="rounded-full bg-white/15 px-3 py-1.5">{activeStudio.ownerType === 'TEAM' ? 'Team studio' : 'Individual studio'}</span>
                <span className="rounded-full bg-white/15 px-3 py-1.5">Studio ready</span>
              </div>
            </div>
          </div>
          <div className="grid gap-5 p-5 sm:grid-cols-3 sm:p-7">
            {panels.map((panel) => {
              const PanelIcon = panel.icon;
              return <div key={panel.title} className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-bg-primary)] p-5">
                <div className="mb-4 flex h-11 w-11 items-center justify-center rounded-xl bg-[var(--color-primary)]/10 text-[var(--color-primary)]"><PanelIcon className="h-5 w-5" /></div>
                <h2 className="font-semibold text-[var(--color-text-primary)]">{panel.title}</h2>
                <p className="mt-2 text-sm leading-6 text-[var(--color-text-secondary)]">{panel.detail}</p>
              </div>;
            })}
          </div>
          {isNews && <div className="grid gap-4 border-t border-[var(--color-border)] p-5 sm:grid-cols-2 sm:p-7 xl:grid-cols-[0.8fr_1.4fr_0.9fr]">
            <div className="rounded-2xl border border-[var(--color-border)] p-4"><h3 className="font-semibold">Uploaded news media</h3><p className="mt-1 text-xs text-[var(--color-text-secondary)]">Prepared assets · connect your upload library to populate this list</p><div className="mt-4 space-y-2"><div className="flex items-center gap-2 rounded-xl bg-[var(--color-bg-primary)] p-3"><Image className="h-4 w-4 text-[var(--color-primary)]"/><span className="text-sm">B-roll / supporting clip</span></div><div className="flex items-center gap-2 rounded-xl bg-[var(--color-bg-primary)] p-3"><Image className="h-4 w-4 text-[var(--color-primary)]"/><span className="text-sm">Headline graphic</span></div><div className="flex items-center gap-2 rounded-xl bg-[var(--color-bg-primary)] p-3"><FileText className="h-4 w-4 text-[var(--color-primary)]"/><span className="text-sm">Source notes</span></div></div></div>
            <div className="rounded-2xl bg-slate-950 p-5 text-white"><div className="text-xs font-semibold uppercase tracking-[0.2em] text-rose-300">Broadcast preview</div><div className="mt-5 flex aspect-video items-center justify-center rounded-xl border border-white/15 bg-slate-900"><div className="text-center"><Radio className="mx-auto h-9 w-9 text-rose-300" /><p className="mt-2 text-sm">Preview area</p><p className="mt-1 text-xs text-slate-400">Connect a supported video source to preview a live feed</p></div></div><div className="mt-3 rounded-lg bg-slate-900 p-3 text-xs text-slate-400">Preview is a layout placeholder; it is not a live feed.</div></div>
            <div className="rounded-2xl border border-[var(--color-border)] p-5"><h3 className="font-semibold">Headline rundown</h3><p className="mt-2 text-sm text-[var(--color-text-secondary)]">01 · Top story</p><p className="mt-3 text-sm text-[var(--color-text-secondary)]">02 · Follow-up</p><p className="mt-3 text-sm text-[var(--color-text-secondary)]">03 · Closing segment</p><div className="mt-5 rounded-xl bg-[var(--color-bg-primary)] p-3"><h4 className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-tertiary)]">Teleprompter notes</h4><p className="mt-2 text-sm text-[var(--color-text-secondary)]">Add anchor copy and reading cues to prepare your script.</p></div></div>
          </div>}
          {isComedy && <div className="border-t border-[var(--color-border)] p-5 sm:p-7"><div className="grid gap-4 md:grid-cols-[1.4fr_0.8fr]"><div className="flex min-h-56 flex-col items-center justify-center rounded-2xl bg-gradient-to-b from-slate-950 to-indigo-950 p-6 text-center text-white"><Mic2 className="h-9 w-9 text-amber-300"/><div className="mt-3 text-xs font-semibold uppercase tracking-[0.25em] text-amber-200">Stage view</div><h3 className="mt-2 text-2xl font-bold">Your set, your timing</h3><p className="mt-2 max-w-sm text-sm text-white/65">A stage-oriented planning canvas for set order and performance notes. It does not start a live show.</p></div><div className="rounded-2xl border border-[var(--color-border)] p-5"><h3 className="font-semibold">Tonight's set list</h3><ol className="mt-3 space-y-3 text-sm text-[var(--color-text-secondary)]"><li>01 · Opening bit <span className="float-right">3 min</span></li><li>02 · Main story <span className="float-right">5 min</span></li><li>03 · Closing callback <span className="float-right">2 min</span></li></ol></div></div></div>}
          {isStunts && <div className="border-t border-[var(--color-border)] p-5 sm:p-7"><div className="rounded-2xl border border-amber-500/30 bg-amber-500/5 p-4 text-sm text-[var(--color-text-secondary)]"><strong className="text-[var(--color-text-primary)]">Safety-first setup:</strong> plan the route, camera position, protective equipment and spotter before recording. This workspace does not claim to start a camera or live broadcast unless a compatible capture/streaming integration is configured.</div></div>}
          {isPoetry && <div className="border-t border-[var(--color-border)] p-5 sm:p-7"><h2 className="mb-3 font-semibold">Literary event format</h2><div className="grid gap-3 sm:grid-cols-2"><button onClick={() => { setPoetryFormat('SOLO'); if (activeStudio) localStorage.setItem(`creator-studio-poetry-format:${activeStudio.id}`, 'SOLO'); }} className={`rounded-2xl border p-4 text-left ${poetryFormat === 'SOLO' ? 'border-[var(--color-primary)] bg-[var(--color-primary)]/5' : 'border-[var(--color-border)]'}`}><strong>Solo poet / recital</strong><p className="mt-1 text-sm text-[var(--color-text-secondary)]">Manuscript-first workspace for one performer.</p></button><button onClick={() => { setPoetryFormat('KAVI_SAMMELAN'); if (activeStudio) localStorage.setItem(`creator-studio-poetry-format:${activeStudio.id}`, 'KAVI_SAMMELAN'); }} className={`rounded-2xl border p-4 text-left ${poetryFormat === 'KAVI_SAMMELAN' ? 'border-[var(--color-primary)] bg-[var(--color-primary)]/5' : 'border-[var(--color-border)]'}`}><strong>Kavi Sammelan</strong><p className="mt-1 text-sm text-[var(--color-text-secondary)]">Host, multiple poets, performer queue and stage transitions.</p></button></div><div className="mt-4 rounded-xl bg-[var(--color-bg-primary)] p-4 text-sm text-[var(--color-text-secondary)]">{poetryFormat === 'KAVI_SAMMELAN' ? 'Event view: host/emcee · current poet · upcoming performers · stage handoff notes.' : 'Recital view: manuscript · reading order · podium notes · performance recording.'}</div></div>}
          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-[var(--color-border)] p-5 sm:px-7"><p className="text-xs text-[var(--color-text-secondary)]">Studio metadata is saved. Production actions depend on the services configured in this deployment.</p><Button variant="outline" onClick={startCreate}><Plus className="h-4 w-4" /> Create another studio</Button></div>
        </section>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      {isCreatorStudioDevNoLogin && <div role="status" className="flex items-start gap-3 rounded-2xl border border-violet-400/40 bg-violet-500/10 px-4 py-3 text-sm text-[var(--color-text-primary)]"><Sparkles className="mt-0.5 h-4 w-4 shrink-0 text-violet-500" /><div><strong className="block">Development sandbox · login not required</strong><span className="mt-0.5 block text-[var(--color-text-secondary)]">Studios and preferences are stored in this browser only. Payment, KYC and live-provider integrations are not connected in this mode.</span></div></div>}
      <section className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-slate-950 via-indigo-950 to-violet-900 p-6 text-white sm:p-9">
        <div className="absolute -right-8 -top-10 h-56 w-56 rounded-full bg-fuchsia-500/20 blur-3xl" />
        <div className="relative flex flex-col gap-6 md:flex-row md:items-end md:justify-between">
          <div className="max-w-2xl"><div className="mb-3 inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-3 py-1 text-xs font-semibold"><Sparkles className="h-3.5 w-3.5" /> YOUR CREATIVE CONTROL ROOM</div><h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Creator Studio</h1><p className="mt-3 text-sm leading-6 text-white/75 sm:text-base">Build a workspace that fits your craft — from a newsroom and action-camera setup to a comedy stage, poetry recital or Kavi Sammelan.</p></div>
          <Button onClick={startCreate} className="shrink-0 bg-white text-slate-950 hover:bg-white/90"><Plus className="h-4 w-4" /> Create a studio</Button>
        </div>
        <div className="relative mt-7 grid grid-cols-2 gap-3 sm:grid-cols-4"><div className="rounded-2xl border border-white/15 bg-white/10 p-4"><div className="text-2xl font-bold">{studios.length}</div><div className="mt-1 text-xs text-white/70">Your studios</div></div><div className="rounded-2xl border border-white/15 bg-white/10 p-4"><div className="text-2xl font-bold">20</div><div className="mt-1 text-xs text-white/70">Creative categories</div></div><div className="rounded-2xl border border-white/15 bg-white/10 p-4"><div className="text-2xl font-bold">Custom</div><div className="mt-1 text-xs text-white/70">Category workspace</div></div><div className="rounded-2xl border border-white/15 bg-white/10 p-4"><div className="text-2xl font-bold">One place</div><div className="mt-1 text-xs text-white/70">Plan and manage</div></div></div>
      </section>

      {loadError && <div role="status" className="rounded-2xl border border-amber-500/30 bg-amber-500/5 p-4 text-sm text-[var(--color-text-secondary)]"><strong className="text-[var(--color-text-primary)]">Studio access needs attention.</strong> {loadError} Check that you are signed in and the Creator Studio service is reachable through the API gateway. You can still review the setup flow below.</div>}

      {studios.length > 0 && <section className="space-y-3"><div className="flex items-center justify-between"><h2 className="text-xl font-semibold text-[var(--color-text-primary)]">Your studios</h2><span className="text-sm text-[var(--color-text-secondary)]">{studios.length} workspace{studios.length === 1 ? '' : 's'}</span></div><div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">{studios.map((studio) => { const item = categoryById[studio.primaryCategory]; const Icon = item?.icon || Sparkles; return <button key={studio.id} onClick={() => navigate(`/creator-studio/${studio.id}`)} className="group rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5 text-left transition hover:-translate-y-0.5 hover:border-[var(--color-primary)]"><div className="flex items-start justify-between"><div className="flex h-12 w-12 items-center justify-center rounded-xl bg-[var(--color-primary)]/10 text-[var(--color-primary)]"><Icon className="h-6 w-6" /></div><ArrowRight className="h-4 w-4 text-[var(--color-text-tertiary)] transition group-hover:translate-x-1 group-hover:text-[var(--color-primary)]" /></div><h3 className="mt-4 font-semibold text-[var(--color-text-primary)]">{studio.displayName || studio.name}</h3><p className="mt-1 text-sm text-[var(--color-text-secondary)]">{item?.label || studio.primaryCategory}</p><p className="mt-3 line-clamp-2 text-sm text-[var(--color-text-tertiary)]">{studio.description || item?.description}</p></button>; })}</div></section>}

      <section className="rounded-3xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5 sm:p-7">
        <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between"><div><h2 className="text-xl font-semibold text-[var(--color-text-primary)]">Create your workspace</h2><p className="mt-1 text-sm text-[var(--color-text-secondary)]">A short setup, then your category-specific control room.</p></div><div className="flex items-center gap-2 text-xs text-[var(--color-text-tertiary)]"><span className={`rounded-full px-2.5 py-1 ${step === 1 ? 'bg-[var(--color-primary)]/10 text-[var(--color-primary)]' : 'bg-[var(--color-bg-primary)]'}`}>1 · Category</span><span className={`rounded-full px-2.5 py-1 ${step === 2 ? 'bg-[var(--color-primary)]/10 text-[var(--color-primary)]' : 'bg-[var(--color-bg-primary)]'}`}>2 · Identity</span><span className={`rounded-full px-2.5 py-1 ${step === 3 ? 'bg-[var(--color-primary)]/10 text-[var(--color-primary)]' : 'bg-[var(--color-bg-primary)]'}`}>3 · Preview</span></div></div>

        {step === 1 && <><div className="relative mb-4"><Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--color-text-tertiary)]" /><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Find your creative category..." className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-primary)] py-3 pl-10 pr-4 text-sm text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]" /></div><div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">{filteredCategories.map((item) => { const Icon = item.icon; const active = category === item.id; return <button key={item.id} onClick={() => setCategory(item.id)} className={`rounded-2xl border p-4 text-left transition ${active ? 'border-[var(--color-primary)] bg-[var(--color-primary)]/5 ring-1 ring-[var(--color-primary)]/30' : 'border-[var(--color-border)] hover:border-[var(--color-primary)]/50'}`}><div className="flex items-start gap-3"><div className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${active ? 'bg-[var(--color-primary)] text-white' : 'bg-[var(--color-bg-primary)] text-[var(--color-primary)]'}`}><Icon className="h-5 w-5" /></div><div className="min-w-0 flex-1"><div className="font-semibold text-[var(--color-text-primary)]">{item.label}</div><p className="mt-1 text-xs leading-5 text-[var(--color-text-secondary)]">{item.description}</p></div>{active && <Check className="h-4 w-4 text-[var(--color-primary)]" />}</div></button>; })}</div><div className="mt-5 flex justify-end"><Button disabled={!category} onClick={() => setStep(2)}>Continue <ArrowRight className="h-4 w-4" /></Button></div></>}

        {step === 2 && <div className="mx-auto max-w-2xl space-y-5"><button onClick={() => setStep(1)} className="inline-flex items-center gap-2 text-sm text-[var(--color-text-secondary)]"><ArrowLeft className="h-4 w-4" /> Change category</button><div className="rounded-2xl bg-[var(--color-bg-primary)] p-4"><div className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-tertiary)]">Selected category</div><div className="mt-1 font-semibold text-[var(--color-text-primary)]">{category ? categoryById[category].label : ''}</div><p className="mt-1 text-sm text-[var(--color-text-secondary)]">{category ? categoryById[category].description : ''}</p></div><label className="block"><span className="mb-2 block text-sm font-medium text-[var(--color-text-primary)]">Unique studio name</span><input value={name} onChange={(e) => setName(e.target.value)} maxLength={100} placeholder="e.g. The Midnight Newsroom" className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-primary)] px-4 py-3 text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]" /> <span className="mt-2 flex items-center gap-1.5 text-xs">{nameState === 'checking' && <><Loader2 className="h-3.5 w-3.5 animate-spin" /> Checking name…</>}{nameState === 'available' && <span className="text-emerald-600"><Check className="mr-1 inline h-3.5 w-3.5" /> Name available</span>}{nameState === 'taken' && <span className="text-rose-500"><X className="mr-1 inline h-3.5 w-3.5" /> Name already taken</span>}{nameState === 'invalid' && <span className="text-rose-500">Use 3–100 characters: letters, numbers, spaces, dots, underscores or hyphens.</span>}{nameState === 'idle' && <span className="text-[var(--color-text-tertiary)]">Names are checked by the service; the database makes the final decision.</span>}</span></label><label className="block"><span className="mb-2 block text-sm font-medium text-[var(--color-text-primary)]">Display name</span><input value={displayName} onChange={(e) => setDisplayName(e.target.value)} maxLength={150} placeholder="Public-facing studio title (optional)" className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-primary)] px-4 py-3 text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]" /></label><label className="block"><span className="mb-2 block text-sm font-medium text-[var(--color-text-primary)]">About your studio</span><textarea value={description} onChange={(e) => setDescription(e.target.value)} maxLength={2000} rows={3} placeholder="What will you create here?" className="w-full resize-y rounded-xl border border-[var(--color-border)] bg-[var(--color-bg-primary)] px-4 py-3 text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]" /></label><div><span className="mb-2 block text-sm font-medium text-[var(--color-text-primary)]">Workspace ownership</span><div className="grid grid-cols-2 gap-3"><button onClick={() => setOwnerType('INDIVIDUAL')} className={`rounded-xl border p-3 text-left ${ownerType === 'INDIVIDUAL' ? 'border-[var(--color-primary)] bg-[var(--color-primary)]/5' : 'border-[var(--color-border)]'}`}><strong className="text-sm">Individual</strong><p className="mt-1 text-xs text-[var(--color-text-secondary)]">One creator owns this studio.</p></button><button onClick={() => setOwnerType('TEAM')} className={`rounded-xl border p-3 text-left ${ownerType === 'TEAM' ? 'border-[var(--color-primary)] bg-[var(--color-primary)]/5' : 'border-[var(--color-border)]'}`}><strong className="text-sm">Team</strong><p className="mt-1 text-xs text-[var(--color-text-secondary)]">Built for collaborators and performers.</p></button></div></div><div className="flex justify-end gap-3"><Button variant="outline" onClick={() => setStep(1)}>Back</Button><Button disabled={!name.trim() || nameState !== 'available'} onClick={() => setStep(3)}>Preview studio <ArrowRight className="h-4 w-4" /></Button></div></div>}

        {step === 3 && <div className="mx-auto max-w-2xl"><button onClick={() => setStep(2)} className="mb-4 inline-flex items-center gap-2 text-sm text-[var(--color-text-secondary)]"><ArrowLeft className="h-4 w-4" /> Edit details</button><div className="overflow-hidden rounded-2xl border border-[var(--color-border)]"><div className="bg-gradient-to-br from-indigo-600 to-fuchsia-600 p-6 text-white"><div className="inline-flex rounded-full bg-white/15 px-3 py-1 text-xs">{category ? categoryById[category].label : 'Creator workspace'}</div><h3 className="mt-3 text-2xl font-bold">{displayName.trim() || name.trim() || 'Your studio name'}</h3><p className="mt-2 text-sm text-white/80">{description.trim() || (category ? categoryById[category].description : 'A workspace made for your craft.')}</p></div><div className="grid gap-3 p-4 sm:grid-cols-3"><div className="rounded-xl bg-[var(--color-bg-primary)] p-3"><Video className="h-5 w-5 text-[var(--color-primary)]" /><div className="mt-2 text-sm font-semibold">Content workspace</div><div className="mt-1 text-xs text-[var(--color-text-secondary)]">Organize creative work</div></div><div className="rounded-xl bg-[var(--color-bg-primary)] p-3"><Radio className="h-5 w-5 text-[var(--color-primary)]" /><div className="mt-2 text-sm font-semibold">Sessions</div><div className="mt-1 text-xs text-[var(--color-text-secondary)]">Where supported</div></div><div className="rounded-xl bg-[var(--color-bg-primary)] p-3"><ShieldCheck className="h-5 w-5 text-[var(--color-primary)]" /><div className="mt-2 text-sm font-semibold">Private by owner</div><div className="mt-1 text-xs text-[var(--color-text-secondary)]">Access controlled by service</div></div></div></div>{error && <p role="alert" className="mt-4 rounded-xl bg-rose-500/10 p-3 text-sm text-rose-600">{error}</p>}<div className="mt-5 flex justify-end gap-3"><Button variant="outline" onClick={() => setStep(2)}>Back</Button><Button loading={saving} disabled={saving || nameState !== 'available' || !category} onClick={createStudio}><Plus className="h-4 w-4" /> Create Creator Studio</Button></div><p className="mt-3 text-xs text-[var(--color-text-tertiary)]">{isCreatorStudioDevNoLogin ? 'Development mode: studio data is saved to this browser only.' : 'Creation requires an authenticated account. The service validates the name and saves the studio before opening its workspace.'}</p></div>}
      </section>
      {loading && <div className="flex items-center justify-center gap-2 text-sm text-[var(--color-text-secondary)]"><Loader2 className="h-4 w-4 animate-spin" /> Loading your studios…</div>}
    </div>
  );
}
