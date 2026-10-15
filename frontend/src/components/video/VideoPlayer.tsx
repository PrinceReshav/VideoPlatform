import {
  forwardRef,
  useCallback,
  useEffect,
  useRef,
  useState,
  type MouseEvent,
} from 'react';
import Hls from 'hls.js';
import {
  Maximize,
  Minimize,
  Pause,
  PictureInPicture,
  Play,
  SkipBack,
  SkipForward,
  Volume2,
  VolumeX,
} from 'lucide-react';
import { clsx } from 'clsx';
import { Button } from '../ui/Button';

interface VideoPlayerProps {
  src: string;
  poster?: string;
  autoPlay?: boolean;
  /** Start muted so browser autoplay policies do not prevent live playback. */
  initialMuted?: boolean;
  /** Live-TV mode: removes VOD timeline, seek/jump controls and playback speed. */
  liveMode?: boolean;
  onEnded?: () => void;
  onTimeUpdate?: (currentTime: number, duration: number) => void;
  onError?: (error: Error) => void;
  className?: string;
}

export const VideoPlayer = forwardRef<
  HTMLVideoElement,
  VideoPlayerProps
>(
  (
    {
      src,
      poster,
      autoPlay = false,
      initialMuted = false,
      liveMode = false,
      onEnded,
      onTimeUpdate,
      onError,
      className,
      ...props
    },
    ref
  ) => {
    const videoRef = useRef<HTMLVideoElement | null>(null);
    const hlsRef = useRef<Hls | null>(null);
    const networkRecoveryAttemptsRef = useRef(0);
    const mediaRecoveryAttemptsRef = useRef(0);
    const retryTimerRef = useRef<number | null>(null);

    /*
     * Keep the latest callbacks without forcing the HLS instance
     * to be recreated every time the parent renders.
     */
    const onEndedRef = useRef(onEnded);
    const onTimeUpdateRef = useRef(onTimeUpdate);
    const onErrorRef = useRef(onError);

    useEffect(() => {
      onEndedRef.current = onEnded;
    }, [onEnded]);

    useEffect(() => {
      onTimeUpdateRef.current = onTimeUpdate;
    }, [onTimeUpdate]);

    useEffect(() => {
      onErrorRef.current = onError;
    }, [onError]);

    const [isPlaying, setIsPlaying] = useState(false);
    const [showControls, setShowControls] = useState(false);
    const [currentTime, setCurrentTime] = useState(0);
    const [duration, setDuration] = useState(0);
    const [buffered, setBuffered] = useState(0);
    const [volume, setVolume] = useState(1);
    const [isMuted, setIsMuted] = useState(() => {
      if (initialMuted) {
        return true;
      }

      if (liveMode && typeof window !== 'undefined') {
        return window.localStorage.getItem('videoplatform.liveTv.muted') === 'true';
      }

      return false;
    });
    const [autoplayNeedsUnmute, setAutoplayNeedsUnmute] = useState(false);
    const [playbackRate, setPlaybackRate] = useState(1);
    const [isFullscreen, setIsFullscreen] = useState(false);
    const [isPictureInPicture, setIsPictureInPicture] =
      useState(false);
    const [quality, setQuality] = useState('auto');
    const [availableQualities, setAvailableQualities] =
      useState<string[]>([]);
    const [error, setError] = useState<string | null>(null);

    /*
     * Changing this value forces a complete media/HLS
     * reinitialization when the user presses Retry.
     */
    const [retryNonce, setRetryNonce] = useState(0);

    /*
     * Forward the video element to the parent while keeping
     * our own internal ref.
     */
    const setVideoElement = useCallback(
      (element: HTMLVideoElement | null) => {
        videoRef.current = element;

        if (typeof ref === 'function') {
          ref(element);
        } else if (ref) {
          ref.current = element;
        }
      },
      [ref]
    );

    /*
     * Play / pause.
     */
    const togglePlay = useCallback(() => {
      const video = videoRef.current;

      if (!video) {
        return;
      }

      if (video.paused) {
        video.play().catch((playError: unknown) => {
          const message =
            playError instanceof Error
              ? playError.message
              : 'Unable to start playback';

          setError(message);

          onErrorRef.current?.(
            playError instanceof Error
              ? playError
              : new Error(message)
          );
        });
      } else {
        video.pause();
      }
    }, []);

    /*
     * Mute / unmute.
     */
    const toggleMute = useCallback(() => {
      const video = videoRef.current;

      if (!video) {
        return;
      }

      const nextMuted = !video.muted;

      video.muted = nextMuted;
      setIsMuted(nextMuted);
      setAutoplayNeedsUnmute(false);

      if (liveMode && typeof window !== 'undefined') {
        window.localStorage.setItem('videoplatform.liveTv.muted', String(nextMuted));
      }

      if (!nextMuted && video.paused) {
        video.play().catch(() => {
          // The browser may still require an explicit play gesture.
        });
      }
    }, [liveMode]);

    /*
     * Volume.
     */
    const handleVolumeChange = useCallback(
      (value: number) => {
        const video = videoRef.current;

        if (!video) {
          return;
        }

        const nextVolume = Math.min(
          1,
          Math.max(0, value)
        );

        video.volume = nextVolume;

        if (nextVolume > 0 && video.muted) {
          video.muted = false;
        }

        if (nextVolume === 0) {
          video.muted = true;
        }

        setVolume(nextVolume);
        setIsMuted(video.muted);
        setAutoplayNeedsUnmute(false);

        if (liveMode && typeof window !== 'undefined') {
          window.localStorage.setItem('videoplatform.liveTv.muted', String(video.muted));
        }
      },
      [liveMode]
    );

    /*
     * Playback speed.
     */
    const handlePlaybackRateChange = useCallback(
      (rate: number) => {
        if (liveMode) return;

        const video = videoRef.current;

        if (!video) {
          return;
        }

        const nextRate = Math.min(
          2,
          Math.max(0.25, rate)
        );

        video.playbackRate = nextRate;
        setPlaybackRate(nextRate);
      },
      [liveMode]
    );

    /*
     * Fullscreen.
     */
    const toggleFullscreen = useCallback(() => {
      const video = videoRef.current;

      if (!video) {
        return;
      }

      const playerContainer =
        video.closest(
          '.video-player-container'
        ) as HTMLElement | null;

      if (!playerContainer) {
        return;
      }

      if (document.fullscreenElement) {
        document.exitFullscreen().catch(() => {
          // Browser rejected fullscreen exit.
        });

        return;
      }

      if (!document.fullscreenEnabled) {
        return;
      }

      playerContainer
        .requestFullscreen()
        .catch(() => {
          // Browser rejected fullscreen request.
        });
    }, []);

    /*
     * Picture-in-Picture.
     */
    const togglePictureInPicture = useCallback(() => {
      const video = videoRef.current;

      if (!video) {
        return;
      }

      if (document.pictureInPictureElement) {
        document
          .exitPictureInPicture()
          .catch(() => {
            // Browser rejected PiP exit.
          });

        return;
      }

      if (
        !document.pictureInPictureEnabled ||
        typeof video.requestPictureInPicture !==
          'function'
      ) {
        return;
      }

      video
        .requestPictureInPicture()
        .catch(() => {
          // Browser rejected PiP request.
        });
    }, []);

    /*
     * Keyboard controls.
     */
    useEffect(() => {
      const handleKeyDown = (event: KeyboardEvent) => {
        const video = videoRef.current;

        if (!video) {
          return;
        }

        /*
         * Don't steal keyboard input from text fields,
         * selects or textareas.
         */
        const target = event.target as HTMLElement | null;

        if (
          target instanceof HTMLInputElement ||
          target instanceof HTMLTextAreaElement ||
          target instanceof HTMLSelectElement
        ) {
          return;
        }

        switch (event.key) {
          case ' ':
          case 'k':
            event.preventDefault();
            togglePlay();
            break;

          case 'ArrowLeft':
            if (liveMode) break;
            event.preventDefault();

            if (Number.isFinite(video.duration)) {
              video.currentTime = Math.max(
                0,
                video.currentTime - 10
              );
            }

            break;

          case 'ArrowRight':
            if (liveMode) break;
            event.preventDefault();

            if (Number.isFinite(video.duration)) {
              video.currentTime = Math.min(
                video.duration,
                video.currentTime + 10
              );
            }

            break;

          case 'ArrowUp':
            event.preventDefault();
            handleVolumeChange(volume + 0.1);
            break;

          case 'ArrowDown':
            event.preventDefault();
            handleVolumeChange(volume - 0.1);
            break;

          case 'm':
            event.preventDefault();
            toggleMute();
            break;

          case 'f':
            event.preventDefault();
            toggleFullscreen();
            break;

          case '>':
            if (liveMode) break;
            event.preventDefault();
            handlePlaybackRateChange(
              playbackRate + 0.25
            );
            break;

          case '<':
            if (liveMode) break;
            event.preventDefault();
            handlePlaybackRateChange(
              playbackRate - 0.25
            );
            break;

          default:
            break;
        }
      };

      window.addEventListener(
        'keydown',
        handleKeyDown
      );

      return () => {
        window.removeEventListener(
          'keydown',
          handleKeyDown
        );
      };
    }, [
      togglePlay,
      toggleMute,
      toggleFullscreen,
      handleVolumeChange,
      handlePlaybackRateChange,
      volume,
      playbackRate,
      liveMode,
    ]);

    /*
     * Fullscreen browser state.
     */
    useEffect(() => {
      const handleFullscreenChange = () => {
        setIsFullscreen(
          document.fullscreenElement !== null
        );
      };

      document.addEventListener(
        'fullscreenchange',
        handleFullscreenChange
      );

      return () => {
        document.removeEventListener(
          'fullscreenchange',
          handleFullscreenChange
        );
      };
    }, []);

    /*
     * Picture-in-Picture browser state.
     */
    useEffect(() => {
      const handleEnterPictureInPicture = () => {
        setIsPictureInPicture(true);
      };

      const handleLeavePictureInPicture = () => {
        setIsPictureInPicture(false);
      };

      const video = videoRef.current;

      if (!video) {
        return;
      }

      video.addEventListener(
        'enterpictureinpicture',
        handleEnterPictureInPicture
      );

      video.addEventListener(
        'leavepictureinpicture',
        handleLeavePictureInPicture
      );

      return () => {
        video.removeEventListener(
          'enterpictureinpicture',
          handleEnterPictureInPicture
        );

        video.removeEventListener(
          'leavepictureinpicture',
          handleLeavePictureInPicture
        );
      };
    }, []);

    const isLikelyHlsSource = /(?:\.m3u8(?:$|[?#])|m3u8|\/api\/v1\/live\/channels\/[^/]+\/stream)/i.test(src);

    /*
     * Initialize / reinitialize HLS or a native media source.
     *
     * IMPORTANT:
     * Volume, mute and playback rate are deliberately NOT
     * dependencies here. Changing them must not recreate HLS.
     */
    useEffect(() => {
      const video = videoRef.current;

      if (!video || !src) {
        return;
      }

      /*
       * Destroy any previous HLS instance before
       * creating another one.
       */
      if (hlsRef.current) {
        hlsRef.current.destroy();
        hlsRef.current = null;
      }

      /*
       * Reset player state for the new channel/source.
       */
      setError(null);
      networkRecoveryAttemptsRef.current = 0;
      mediaRecoveryAttemptsRef.current = 0;
      if (retryTimerRef.current !== null) {
        window.clearTimeout(retryTimerRef.current);
        retryTimerRef.current = null;
      }
      setCurrentTime(0);
      setDuration(0);
      setBuffered(0);
      setAvailableQualities([]);
      setQuality('auto');
      setIsPlaying(false);
      setAutoplayNeedsUnmute(false);

      /*
       * CORS must be configured before attaching/loading
       * the HLS media.
       */
      video.crossOrigin = 'anonymous';

      /*
       * Preserve current user media settings.
       */
      video.volume = volume;
      video.muted = isMuted;
      video.playbackRate = playbackRate;

      /*
       * HLS.js path.
       */
      if (Hls.isSupported() && isLikelyHlsSource) {
        const hls = new Hls({
          enableWorker: true,
          lowLatencyMode: true,
          backBufferLength: 30,
          manifestLoadingMaxRetry: 8,
          levelLoadingMaxRetry: 8,
          fragLoadingMaxRetry: 8,
          manifestLoadingRetryDelay: 1000,
          levelLoadingRetryDelay: 1000,
          fragLoadingRetryDelay: 750,
          maxBufferLength: 30,
          liveDurationInfinity: true,
        });

        hlsRef.current = hls;

        /*
         * Attach the media element first.
         */
        hls.attachMedia(video);

        /*
         * Then load the HLS manifest.
         */
        hls.loadSource(src);

        hls.on(
          Hls.Events.MANIFEST_PARSED,
          (_event, data) => {
            networkRecoveryAttemptsRef.current = 0;
            mediaRecoveryAttemptsRef.current = 0;
            const heights = data.levels
              .map((level) => level.height)
              .filter(
                (height): height is number =>
                  Number.isFinite(height) &&
                  height > 0
              );

            const qualities = [
              ...new Set(heights),
            ]
              .sort((a, b) => b - a)
              .map(
                (height) => `${height}p`
              );

            setAvailableQualities(
              qualities
            );

            /*
             * Start playback only after the manifest
             * has been parsed.
             */
            if (autoPlay) {
              /*
               * Prefer audible autoplay. If Chrome/Edge/Safari rejects it,
               * fall back to muted playback rather than failing the channel.
               * The user can then explicitly unmute; that gesture is accepted
               * by browser autoplay policy and the preference is remembered
               * for subsequent Live-TV channels.
               */
              video.muted = isMuted;
              video.volume = volume;

              video.play().then(() => {
                setAutoplayNeedsUnmute(false);
              }).catch(() => {
                video.muted = true;
                setIsMuted(true);
                setAutoplayNeedsUnmute(true);

                // Do not persist this fallback as a user preference. The browser
                // rejected autoplay with sound for this session; the next channel
                // should still get a fresh attempt with audio.

                video.play().catch((playError: unknown) => {
                  const message = playError instanceof Error
                    ? playError.message
                    : 'Unable to start live playback';
                  setError(message);
                  onErrorRef.current?.(
                    playError instanceof Error ? playError : new Error(message)
                  );
                });
              });
            }
          }
        );

        hls.on(
          Hls.Events.ERROR,
          (_event, data) => {
            if (!data.fatal) {
              return;
            }

            switch (data.type) {
              case Hls.ErrorTypes.NETWORK_ERROR:
                /*
                 * Recover a small number of times with backoff. An unlimited
                 * startLoad() loop can hammer an unavailable provider and makes
                 * the player look frozen.
                 */
                if (networkRecoveryAttemptsRef.current < 30) {
                  networkRecoveryAttemptsRef.current += 1;
                  const delay = Math.min(10000, 750 * networkRecoveryAttemptsRef.current);
                  if (retryTimerRef.current !== null) {
                    window.clearTimeout(retryTimerRef.current);
                  }
                  retryTimerRef.current = window.setTimeout(() => {
                    retryTimerRef.current = null;
                    if (hlsRef.current === hls) {
                      hls.startLoad(-1);
                    }
                  }, delay);
                } else {
                  // Live TV should remain in a recovery state rather than
                  // permanently stopping because an upstream provider had a
                  // transient outage. Keep retrying at a low frequency.
                  networkRecoveryAttemptsRef.current = 0;
                  if (retryTimerRef.current !== null) {
                    window.clearTimeout(retryTimerRef.current);
                  }
                  retryTimerRef.current = window.setTimeout(() => {
                    retryTimerRef.current = null;
                    if (hlsRef.current === hls) {
                      hls.stopLoad();
                      hls.startLoad(-1);
                    }
                  }, 10000);
                }
                break;

              case Hls.ErrorTypes.MEDIA_ERROR:
                /* Recover media corruption repeatedly; Live TV has no VOD end state. */
                if (mediaRecoveryAttemptsRef.current < 8) {
                  mediaRecoveryAttemptsRef.current += 1;
                  hls.recoverMediaError();
                } else {
                  mediaRecoveryAttemptsRef.current = 0;
                  hls.stopLoad();
                  hls.loadSource(src);
                }
                break;

              default: {
                const playbackError = new Error(
                  data.details ||
                    'Playback error occurred'
                );

                setError(
                  playbackError.message
                );

                onErrorRef.current?.(
                  playbackError
                );

                hls.destroy();

                if (hlsRef.current === hls) {
                  hlsRef.current = null;
                }

                break;
              }
            }
          }
        );

        return () => {
          video.pause();
          if (retryTimerRef.current !== null) {
            window.clearTimeout(retryTimerRef.current);
            retryTimerRef.current = null;
          }

          hls.destroy();

          if (hlsRef.current === hls) {
            hlsRef.current = null;
          }

          video.removeAttribute('src');
          video.load();

          setIsPlaying(false);
        };
      }

      /*
       * Native HLS (Safari/WebKit) and ordinary browser-supported
       * media sources (for example MP4/WebM live endpoints).
       */
      const canUseNativeHls = video.canPlayType(
        'application/vnd.apple.mpegurl'
      );

      if (!isLikelyHlsSource || canUseNativeHls) {
        video.src = src;

        const startPlayback = () => {
          if (!autoPlay) return;

          video.play().catch(() => {
            video.muted = true;
            setIsMuted(true);
            video.play().catch((playError: unknown) => {
              const message = playError instanceof Error
                ? playError.message
                : 'Unable to start live playback';
              setError(message);
              onErrorRef.current?.(
                playError instanceof Error ? playError : new Error(message)
              );
            });
          });
        };

        video.addEventListener('loadedmetadata', startPlayback);
        video.addEventListener('canplay', startPlayback);

        return () => {
          video.removeEventListener('loadedmetadata', startPlayback);
          video.removeEventListener('canplay', startPlayback);
          video.pause();
          video.removeAttribute('src');
          video.load();
          setIsPlaying(false);
        };
      }

      /*
       * Browser does not support HLS.
       */
      const unsupportedError = new Error(
        'This browser does not support HLS playback.'
      );

      setError(
        unsupportedError.message
      );

      onErrorRef.current?.(
        unsupportedError
      );

      return () => {
        video.pause();
        video.removeAttribute('src');
        video.load();

        setIsPlaying(false);
      };
    // Intentionally excludes volume/mute/rate: changing a media control must
    // not tear down and recreate the live HLS pipeline.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
      src,
      autoPlay,
      retryNonce,
    ]);

    /*
     * Apply media settings independently of HLS.
     *
     * This prevents volume/mute/speed changes from
     * destroying and recreating the HLS pipeline.
     */
    useEffect(() => {
      const video = videoRef.current;

      if (!video) {
        return;
      }

      video.volume = volume;
      video.muted = isMuted;
      video.playbackRate = playbackRate;
    }, [
      volume,
      isMuted,
      playbackRate,
    ]);

    /*
     * Media event listeners.
     */
    useEffect(() => {
      const video = videoRef.current;

      if (!video) {
        return;
      }

      const handleTimeUpdate = () => {
        const nextCurrentTime =
          video.currentTime;

        const nextDuration =
          video.duration;

        setCurrentTime(
          nextCurrentTime
        );

        setDuration(
          nextDuration
        );

        onTimeUpdateRef.current?.(
          nextCurrentTime,
          nextDuration
        );
      };

      const handleDurationChange = () => {
        setDuration(video.duration);
      };

      const handleProgress = () => {
        if (video.buffered.length === 0) {
          setBuffered(0);
          return;
        }

        try {
          setBuffered(
            video.buffered.end(
              video.buffered.length - 1
            )
          );
        } catch {
          /*
           * TimeRanges can change between checking
           * and reading them.
           */
        }
      };

      const handlePlay = () => {
        setIsPlaying(true);
      };

      const handlePause = () => {
        setIsPlaying(false);
      };

      const handleEnded = () => {
        setIsPlaying(false);
        onEndedRef.current?.();
      };

      const handleLoadedMetadata = () => {
        setDuration(video.duration);
      };

      const handleError = () => {
        /*
         * HLS.js errors are handled separately.
         * This handles native HTMLMediaElement errors.
         */
        const mediaError = video.error;

        if (!mediaError) {
          return;
        }

        const message =
          mediaError.message ||
          'Unable to play this stream.';

        const playbackError =
          new Error(message);

        setError(message);

        onErrorRef.current?.(
          playbackError
        );
      };

      video.addEventListener(
        'timeupdate',
        handleTimeUpdate
      );

      video.addEventListener(
        'durationchange',
        handleDurationChange
      );

      video.addEventListener(
        'progress',
        handleProgress
      );

      video.addEventListener(
        'loadedmetadata',
        handleLoadedMetadata
      );

      video.addEventListener(
        'play',
        handlePlay
      );

      video.addEventListener(
        'pause',
        handlePause
      );

      video.addEventListener(
        'ended',
        handleEnded
      );

      video.addEventListener(
        'error',
        handleError
      );

      return () => {
        video.removeEventListener(
          'timeupdate',
          handleTimeUpdate
        );

        video.removeEventListener(
          'durationchange',
          handleDurationChange
        );

        video.removeEventListener(
          'progress',
          handleProgress
        );

        video.removeEventListener(
          'loadedmetadata',
          handleLoadedMetadata
        );

        video.removeEventListener(
          'play',
          handlePlay
        );

        video.removeEventListener(
          'pause',
          handlePause
        );

        video.removeEventListener(
          'ended',
          handleEnded
        );

        video.removeEventListener(
          'error',
          handleError
        );
      };
    }, []);

    /*
     * Retry the current stream by completely recreating
     * the media/HLS pipeline.
     */
    const retryPlayback = useCallback(() => {
      setError(null);

      setRetryNonce(
        (current) => current + 1
      );
    }, []);

    /*
     * Quality selection.
     */
    const handleQualityChange = useCallback(
      (selectedQuality: string) => {
        setQuality(selectedQuality);

        const hls = hlsRef.current;

        if (!hls) {
          return;
        }

        if (selectedQuality === 'auto') {
          hls.currentLevel = -1;
          return;
        }

        const targetHeight =
          Number.parseInt(
            selectedQuality,
            10
          );

        if (
          !Number.isFinite(
            targetHeight
          )
        ) {
          hls.currentLevel = -1;
          return;
        }

        const levelIndex =
          hls.levels.findIndex(
            (level) =>
              level.height ===
              targetHeight
          );

        if (levelIndex >= 0) {
          hls.currentLevel =
            levelIndex;
        }
      },
      []
    );

    /*
     * Seek.
     *
     * Live streams commonly expose duration === Infinity,
     * so seeking is only enabled when the stream has a
     * finite duration/DVR window.
     */
    const handleSeek = useCallback(
      (event: MouseEvent<HTMLDivElement>) => {
        const video = videoRef.current;

        if (
          !video ||
          !Number.isFinite(video.duration) ||
          video.duration <= 0
        ) {
          return;
        }

        const rect =
          event.currentTarget.getBoundingClientRect();

        if (rect.width <= 0) {
          return;
        }

        const percent = Math.min(
          1,
          Math.max(
            0,
            (event.clientX - rect.left) /
              rect.width
          )
        );

        video.currentTime =
          percent * video.duration;
      },
      []
    );

    /*
     * Format media time.
     */
    const formatTime = useCallback(
      (seconds: number) => {
        if (!Number.isFinite(seconds)) {
          return 'LIVE';
        }

        if (seconds < 0) {
          return '0:00';
        }

        const hrs = Math.floor(
          seconds / 3600
        );

        const mins = Math.floor(
          (seconds % 3600) / 60
        );

        const secs = Math.floor(
          seconds % 60
        );

        if (hrs > 0) {
          return `${hrs}:${mins
            .toString()
            .padStart(2, '0')}:${secs
            .toString()
            .padStart(2, '0')}`;
        }

        return `${mins}:${secs
          .toString()
          .padStart(2, '0')}`;
      },
      []
    );

    /*
     * Calculate progress percentages safely.
     */
    const progressPercent =
      Number.isFinite(duration) &&
      duration > 0
        ? Math.min(
            100,
            Math.max(
              0,
              (currentTime / duration) *
                100
            )
          )
        : 0;

    const bufferedPercent =
      Number.isFinite(duration) &&
      duration > 0
        ? Math.min(
            100,
            Math.max(
              0,
              (buffered / duration) *
                100
            )
          )
        : 0;

    const pictureInPictureSupported =
      typeof document !== 'undefined' &&
      document.pictureInPictureEnabled;

    return (
      <div
        className={clsx(
          'relative bg-black video-player-container',
          className
        )}
        onMouseEnter={() =>
          setShowControls(true)
        }
        onMouseLeave={() =>
          setShowControls(false)
        }
      >
        <video
          ref={setVideoElement}
          poster={poster}
          crossOrigin="anonymous"
          playsInline
          preload="metadata"
          className="w-full h-full object-contain"
          {...props}
        />

        {autoplayNeedsUnmute && !error && (
          <button
            type="button"
            className="absolute left-1/2 top-1/2 z-10 -translate-x-1/2 -translate-y-1/2 rounded-full bg-black/75 px-4 py-3 text-sm font-semibold text-white shadow-xl backdrop-blur-sm hover:bg-black/90"
            onClick={() => {
              const video = videoRef.current;
              if (!video) return;
              video.muted = false;
              video.volume = Math.max(volume, 0.5);
              setVolume(video.volume);
              setIsMuted(false);
              setAutoplayNeedsUnmute(false);
              if (liveMode && typeof window !== 'undefined') {
                window.localStorage.setItem('videoplatform.liveTv.muted', 'false');
              }
              video.play().catch(() => undefined);
            }}
            aria-label="Turn on live TV sound"
          >
            🔊 Click to unmute
          </button>
        )}

        {error && (
          <div className="absolute inset-0 flex items-center justify-center bg-black/80 z-10">
            <div className="text-center p-4 max-w-md">
              <p className="text-red-400 mb-2">
                Playback Error
              </p>

              <p className="text-white/70 text-sm mb-4">
                {error}
              </p>

              <Button
                variant="outline"
                onClick={retryPlayback}
              >
                Retry
              </Button>
            </div>
          </div>
        )}

        <div
          className={clsx(
            'absolute bottom-0 left-0 right-0',
            'bg-gradient-to-t from-black/90 via-black/50 to-transparent',
            'transition-opacity duration-300',
            showControls || isPlaying
              ? 'opacity-100'
              : 'opacity-0'
          )}
        >
          <div className="p-3 md:p-4">
            {liveMode ? (
              <div className="mb-3 flex items-center gap-2" aria-label="Live stream">
                <span className="inline-flex items-center gap-1.5 rounded-full bg-red-600/90 px-2.5 py-1 text-[11px] font-bold uppercase tracking-wide text-white shadow-sm">
                  <span className="h-1.5 w-1.5 rounded-full bg-white animate-pulse" />
                  LIVE
                </span>
                <span className="text-xs text-white/60">Live stream</span>
              </div>
            ) : (
              <div className="relative mb-3">
                <div className="relative h-1 md:h-2 rounded-full bg-white/30 cursor-pointer" onClick={handleSeek} role="slider" aria-label="Video progress">
                  <div className="absolute left-0 top-0 bottom-0 bg-white/50 rounded-full" style={{ width: `${bufferedPercent}%` }} />
                  <div className="relative h-full bg-[var(--color-primary)] rounded-full transition-all duration-100" style={{ width: `${progressPercent}%` }} />
                </div>
                <div className="flex justify-between text-xs text-white/70 mt-1 px-1">
                  <span>{formatTime(currentTime)}</span>
                  <span>{formatTime(duration)}</span>
                </div>
              </div>
            )}

            {/*
             * Controls.
             */}
            <div className="flex items-center justify-between gap-4">
              {/*
               * Playback controls.
               */}
              <div className="flex items-center gap-2">
                {!liveMode && (
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => {
                      const video = videoRef.current;
                      if (!video || !Number.isFinite(video.duration)) return;
                      video.currentTime = Math.max(0, video.currentTime - 10);
                    }}
                    aria-label="Rewind 10 seconds"
                  >
                    <SkipBack className="w-5 h-5" />
                  </Button>
                )}

                <Button
                  variant="ghost"
                  size="icon"
                  onClick={togglePlay}
                  aria-label={isPlaying ? 'Pause' : 'Play'}
                >
                  {isPlaying ? (
                    <Pause className="w-6 h-6" />
                  ) : (
                    <Play className="w-6 h-6" />
                  )}
                </Button>

                {!liveMode && (
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => {
                      const video = videoRef.current;
                      if (!video || !Number.isFinite(video.duration)) return;
                      video.currentTime = Math.min(video.duration, video.currentTime + 10);
                    }}
                    aria-label="Forward 10 seconds"
                  >
                    <SkipForward className="w-5 h-5" />
                  </Button>
                )}
              </div>

              {/*
               * Volume.
               */}
              <div className="flex items-center gap-2 flex-1 max-w-xs mx-4">
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={toggleMute}
                  aria-label={
                    isMuted
                      ? 'Unmute'
                      : 'Mute'
                  }
                >
                  {isMuted ||
                  volume === 0 ? (
                    <VolumeX className="w-5 h-5" />
                  ) : (
                    <Volume2 className="w-5 h-5" />
                  )}
                </Button>

                <input
                  type="range"
                  min="0"
                  max="1"
                  step="0.05"
                  value={
                    isMuted
                      ? 0
                      : volume
                  }
                  onChange={(event) =>
                    handleVolumeChange(
                      Number.parseFloat(
                        event.target.value
                      )
                    )
                  }
                  className="w-full h-1 bg-white/30 rounded-full appearance-none accent-[var(--color-primary)]"
                  aria-label="Volume"
                />
              </div>

              {/*
               * Speed / quality / fullscreen / PiP.
               */}
              <div className="flex items-center gap-2">
                {!liveMode && (
                  <select
                    value={playbackRate}
                    onChange={(event) => handlePlaybackRateChange(Number.parseFloat(event.target.value))}
                    className="bg-white/10 border border-white/20 rounded text-white text-xs px-2 py-1"
                    aria-label="Playback speed"
                  >
                    <option value="0.25">0.25x</option>
                    <option value="0.5">0.5x</option>
                    <option value="0.75">0.75x</option>
                    <option value="1">1x</option>
                    <option value="1.25">1.25x</option>
                    <option value="1.5">1.5x</option>
                    <option value="1.75">1.75x</option>
                    <option value="2">2x</option>
                  </select>
                )}

                <select
                  value={quality}
                  onChange={(event) =>
                    handleQualityChange(
                      event.target.value
                    )
                  }
                  className="bg-white/10 border border-white/20 rounded text-white text-xs px-2 py-1"
                  aria-label="Quality"
                >
                  <option value="auto">
                    Auto
                  </option>

                  {availableQualities.map(
                    (qualityOption) => (
                      <option
                        key={qualityOption}
                        value={qualityOption}
                      >
                        {qualityOption}
                      </option>
                    )
                  )}
                </select>

                <Button
                  variant="ghost"
                  size="icon"
                  onClick={
                    toggleFullscreen
                  }
                  aria-label={
                    isFullscreen
                      ? 'Exit fullscreen'
                      : 'Enter fullscreen'
                  }
                >
                  {isFullscreen ? (
                    <Minimize className="w-5 h-5" />
                  ) : (
                    <Maximize className="w-5 h-5" />
                  )}
                </Button>

                <Button
                  variant="ghost"
                  size="icon"
                  onClick={
                    togglePictureInPicture
                  }
                  disabled={
                    !pictureInPictureSupported
                  }
                  aria-label={
                    isPictureInPicture
                      ? 'Exit Picture in Picture'
                      : 'Enter Picture in Picture'
                  }
                >
                  <PictureInPicture className="w-5 h-5" />
                </Button>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }
);

VideoPlayer.displayName = 'VideoPlayer';