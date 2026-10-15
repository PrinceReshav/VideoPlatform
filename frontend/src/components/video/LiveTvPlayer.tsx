import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Hls from 'hls.js';
import { Maximize, Minimize, Pause, PictureInPicture, Play, Volume2, VolumeX } from 'lucide-react';
import { clsx } from 'clsx';
import type { StreamCandidateInfo } from '../../services/api/liveTvApi';

interface LiveTvPlayerProps {
  candidates: StreamCandidateInfo[];
  poster?: string;
  autoPlay?: boolean;
  className?: string;
  onExhausted?: () => void;
  onPlaying?: (source: string) => void;
}

type Attempt = { url: string; label: string; candidateIndex: number };

function isHls(url: string) {
  return /(?:\.m3u8(?:$|[?#])|m3u8)/i.test(url);
}

export function LiveTvPlayer({ candidates, poster, autoPlay = true, className, onExhausted, onPlaying }: LiveTvPlayerProps) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const hlsRef = useRef<Hls | null>(null);
  const recoveryRef = useRef(0);
  const exhaustedRef = useRef(false);
  const [attemptIndex, setAttemptIndex] = useState(0);
  const [playing, setPlaying] = useState(false);
  const [muted, setMuted] = useState(() => typeof window !== 'undefined' && window.localStorage.getItem('videoplatform.liveTv.muted') === 'true');
  const [volume, setVolume] = useState(1);
  const [error, setError] = useState<string | null>(null);
  const [quality, setQuality] = useState('auto');
  const [levels, setLevels] = useState<Array<{ label: string; index: number }>>([]);
  const onPlayingRef = useRef(onPlaying);

  useEffect(() => { onPlayingRef.current = onPlaying; }, [onPlaying]);

  const attempts = useMemo<Attempt[]>(() => {
    const out: Attempt[] = [];
    candidates.forEach((candidate, index) => {
      if (isHls(candidate.url) && candidate.directBrowserCompatible === true) {
        out.push({ url: candidate.url, label: 'IPTV · Direct', candidateIndex: index });
      }
      // The compatibility proxy is always retained. This is the normal path for IPTV
      // providers that do not send Access-Control-Allow-Origin to browser clients.
      out.push({ url: candidate.proxyUrl, label: 'IPTV · Compatibility', candidateIndex: index });
    });
    return out;
  }, [candidates]);

  const current = attempts[attemptIndex];

  const exhaust = useCallback(() => {
    if (exhaustedRef.current) return;
    exhaustedRef.current = true;
    setError('All indexed sources were attempted. Verifying the final stream status…');
    onExhausted?.();
  }, [onExhausted]);

  const advance = useCallback(() => {
    recoveryRef.current = 0;
    setError('Trying the next live source…');
    setAttemptIndex(index => {
      if (index + 1 >= attempts.length) {
        exhaust();
        return index;
      }
      return index + 1;
    });
  }, [attempts.length, exhaust]);

  useEffect(() => {
    setAttemptIndex(0);
    recoveryRef.current = 0;
    exhaustedRef.current = false;
    setError(null);
  }, [candidates]);

  useEffect(() => {
    const video = videoRef.current;
    if (!video || !current) return;

    if (hlsRef.current) {
      hlsRef.current.destroy();
      hlsRef.current = null;
    }
    video.pause();
    video.removeAttribute('src');
    video.load();
    video.crossOrigin = 'anonymous';
    video.muted = muted;
    video.volume = volume;
    setPlaying(false);
    setError(null);
    setLevels([]);
    setQuality('auto');

    const start = () => {
      if (!autoPlay) return;
      video.play().then(() => {
        setPlaying(true);
        onPlayingRef.current?.(current.label);
      }).catch(() => {
        video.muted = true;
        setMuted(true);
        video.play().then(() => {
          setPlaying(true);
          onPlayingRef.current?.(current.label);
        }).catch(() => setError('Tap Play to start this live channel.'));
      });
    };

    if (Hls.isSupported() && isHls(current.url)) {
      const hls = new Hls({
        enableWorker: true,
        lowLatencyMode: true,
        liveDurationInfinity: true,
        maxBufferLength: 30,
        backBufferLength: 30,
        manifestLoadingMaxRetry: 3,
        levelLoadingMaxRetry: 3,
        fragLoadingMaxRetry: 3,
        manifestLoadingRetryDelay: 750,
        levelLoadingRetryDelay: 750,
        fragLoadingRetryDelay: 750,
      });
      hlsRef.current = hls;
      hls.attachMedia(video);
      hls.loadSource(current.url);
      hls.on(Hls.Events.MANIFEST_PARSED, (_event, data) => {
        recoveryRef.current = 0;
        const nextLevels = data.levels.map((level, index) => ({ label: level.height ? `${level.height}p` : `Level ${index + 1}`, index }));
        setLevels(nextLevels);
        start();
      });
      hls.on(Hls.Events.ERROR, (_event, data) => {
        if (!data.fatal) return;
        if (data.type === Hls.ErrorTypes.NETWORK_ERROR || data.type === Hls.ErrorTypes.MEDIA_ERROR) {
          if (recoveryRef.current < 3) {
            recoveryRef.current += 1;
            if (data.type === Hls.ErrorTypes.MEDIA_ERROR) hls.recoverMediaError();
            else hls.startLoad(-1);
            return;
          }
        }
        advance();
      });
      return () => {
        hls.destroy();
        if (hlsRef.current === hls) hlsRef.current = null;
      };
    }

    const native = video.canPlayType('application/vnd.apple.mpegurl');
    if (isHls(current.url) && !native) {
      advance();
      return;
    }
    video.src = current.url;
    const onLoaded = () => start();
    const onError = () => advance();
    video.addEventListener('loadedmetadata', onLoaded);
    video.addEventListener('error', onError);
    return () => {
      video.removeEventListener('loadedmetadata', onLoaded);
      video.removeEventListener('error', onError);
      video.pause();
      video.removeAttribute('src');
      video.load();
    };
  }, [current, autoPlay, muted, volume, advance]);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;
    const onPlay = () => setPlaying(true);
    const onPause = () => setPlaying(false);
    video.addEventListener('play', onPlay);
    video.addEventListener('pause', onPause);
    return () => {
      video.removeEventListener('play', onPlay);
      video.removeEventListener('pause', onPause);
    };
  }, []);

  const togglePlay = () => {
    const video = videoRef.current;
    if (!video) return;
    if (video.paused) video.play().catch(() => setError('Tap Play to start this live channel.'));
    else video.pause();
  };

  const toggleMute = () => {
    const next = !muted;
    setMuted(next);
    if (videoRef.current) videoRef.current.muted = next;
    if (typeof window !== 'undefined') window.localStorage.setItem('videoplatform.liveTv.muted', String(next));
  };

  const setVol = (value: number) => {
    const next = Math.max(0, Math.min(1, value));
    setVolume(next);
    setMuted(next === 0);
    if (videoRef.current) { videoRef.current.volume = next; videoRef.current.muted = next === 0; }
  };

  const toggleFullscreen = () => {
    const container = videoRef.current?.closest('.live-tv-native-player') as HTMLElement | null;
    if (!container) return;
    if (document.fullscreenElement) document.exitFullscreen().catch(() => undefined);
    else container.requestFullscreen().catch(() => undefined);
  };

  const togglePip = () => {
    const video = videoRef.current;
    if (!video || !document.pictureInPictureEnabled) return;
    if (document.pictureInPictureElement) document.exitPictureInPicture().catch(() => undefined);
    else video.requestPictureInPicture?.().catch(() => undefined);
  };

  const chooseQuality = (index: number) => {
    setQuality(index < 0 ? 'auto' : levels.find(l => l.index === index)?.label || 'auto');
    if (hlsRef.current) hlsRef.current.currentLevel = index;
  };

  if (!current) {
    return <div className={clsx('live-tv-native-player', className)}><div className="live-tv-native-player-error">No IPTV stream candidates are indexed for this channel.</div></div>;
  }

  return (
    <div className={clsx('live-tv-native-player', className)}>
      <video ref={videoRef} poster={poster} playsInline />
      <div className="live-tv-native-overlay">
        <div className="live-tv-native-topline"><span className="live-tv-native-live-dot" /> LIVE <span className="live-tv-native-source">{current.label}</span></div>
        {error && <div className="live-tv-native-error">{error}</div>}
        <div className="live-tv-native-controls">
          <button type="button" onClick={togglePlay} aria-label={playing ? 'Pause live stream' : 'Play live stream'}>{playing ? <Pause size={18} /> : <Play size={18} fill="currentColor" />}</button>
          <button type="button" onClick={toggleMute} aria-label={muted ? 'Unmute' : 'Mute'}>{muted ? <VolumeX size={18} /> : <Volume2 size={18} />}</button>
          <input aria-label="Live TV volume" type="range" min="0" max="1" step="0.01" value={muted ? 0 : volume} onChange={e => setVol(Number(e.target.value))} />
          <div className="live-tv-native-spacer" />
          {levels.length > 0 && <select aria-label="Live TV quality" value={quality} onChange={e => chooseQuality(e.target.value === 'auto' ? -1 : (levels.find(l => l.label === e.target.value)?.index ?? -1))}><option value="auto">Auto</option>{levels.map(l => <option key={l.index} value={l.label}>{l.label}</option>)}</select>}
          <button type="button" onClick={togglePip} aria-label="Picture in picture"><PictureInPicture size={18} /></button>
          <button type="button" onClick={toggleFullscreen} aria-label="Fullscreen">{document.fullscreenElement ? <Minimize size={18} /> : <Maximize size={18} />}</button>
        </div>
      </div>
    </div>
  );
}
