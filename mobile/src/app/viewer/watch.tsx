import React from 'react';
import { View, Text, StyleSheet, Image, TouchableOpacity, ActivityIndicator } from 'react-native';
import { Video } from 'expo-av';
import { useRoute, useNavigation } from 'expo-router';
import { useQuery } from '@tanstack/react-query';
import { videoApi } from '../../services/api/videoApi';
import { ChevronLeft, MoreHorizontal, SkipBack, SkipForward, Settings, Download, Share2, Maximize, Minimize, Volume2, VolumeX, Play, Pause } from 'lucide-react-native';

export default function WatchScreen() {
  const route = useRoute<any>();
  const navigation = useNavigation();
  const videoId = route.params?.videoId;
  const hlsUrl = route.params?.hlsUrl;

  const { data: video, isLoading } = useQuery({
    queryKey: ['video', videoId],
    queryFn: () => videoApi.getDetail(videoId!),
    enabled: !!videoId,
  });

  const [isPlaying, setIsPlaying] = React.useState(true);
  const [showControls, setShowControls] = React.useState(true);
  const [currentTime, setCurrentTime] = React.useState(0);
  const [duration, setDuration] = React.useState(0);
  const [volume, setVolume] = React.useState(1);
  const [isMuted, setIsMuted] = React.useState(false);
  const [isFullscreen, setIsFullscreen] = React.useState(false);

  const videoRef = React.useRef<Video>(null);

  const formatTime = (seconds: number) => {
    if (isNaN(seconds)) return '0:00';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const togglePlay = () => {
    if (!videoRef.current) return;
    if (isPlaying) {
      videoRef.current.pauseAsync();
    } else {
      videoRef.current.playAsync();
    }
    setIsPlaying(!isPlaying);
  };

  const handleSeek = (value: number) => {
    if (!videoRef.current) return;
    videoRef.current.seekToAsync(value);
    setCurrentTime(value);
  };

  const handleVolumeChange = (value: number) => {
    if (!videoRef.current) return;
    videoRef.current.setVolumeAsync(value);
    setVolume(value);
    setIsMuted(value === 0);
  };

  const toggleMute = () => {
    if (!videoRef.current) return;
    if (isMuted) {
      videoRef.current.setVolumeAsync(volume);
    } else {
      videoRef.current.setVolumeAsync(0);
    }
    setIsMuted(!isMuted);
  };

  const toggleFullscreen = () => {
    setIsFullscreen(!isFullscreen);
  };

  if (isLoading || !hlsUrl) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#6366f1" />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Video
        ref={videoRef}
        source={{ uri: hlsUrl }}
        style={styles.video}
        rate={1.0}
        volume={isMuted ? 0 : volume}
        isMuted={isMuted}
        resizeMode="contain"
        useNativeControls={false}
        onLoad={({ duration }) => setDuration(duration)}
        onProgress={({ currentTime }) => setCurrentTime(currentTime)}
        onPlaybackStatusUpdate={({ didJustFinish, isPlaying: playing }) => {
          setIsPlaying(playing);
          if (didJustFinish) {
            setIsPlaying(false);
          }
        }}
        onError={(error) => console.error('Video error:', error)}
      />

      <TouchableOpacity
        style={styles.video}
        onPress={() => setShowControls(!showControls)}
        activeOpacity={1}
      />

      {showControls && (
        <View style={styles.controlsContainer}>
          {/* Top Bar */}
          <View style={styles.topBar}>
            <TouchableOpacity onPress={() => navigation.goBack()}>
              <ChevronLeft size={24} color="#ffffff" />
            </TouchableOpacity>
            <View style={styles.titleContainer}>
              <Text style={styles.videoTitle}>{video?.title}</Text>
              <Text style={styles.channelName}>{video?.channelName}</Text>
            </View>
            <View style={styles.topRight}>
              <TouchableOpacity>
                <MoreHorizontal size={24} color="#ffffff" />
              </TouchableOpacity>
            </View>
          </View>

          {/* Progress Bar */}
          <View style={styles.progressContainer}>
            <Text style={styles.timeText}>{formatTime(currentTime)}</Text>
            <View style={styles.progressBar}>
              <View
                style={[
                  styles.progressFill,
                  { width: `${duration > 0 ? (currentTime / duration) * 100 : 0}%` },
                ]}
              />
            </View>
            <Text style={styles.timeText}>{formatTime(duration)}</Text>
          </View>

          {/* Bottom Controls */}
          <View style={styles.bottomControls}>
            <View style={styles.controlGroup}>
              <TouchableOpacity onPress={() => handleSeek(Math.max(0, currentTime - 10))}>
                <SkipBack size={28} color="#ffffff" />
              </TouchableOpacity>
              <TouchableOpacity onPress={togglePlay} style={styles.playButton}>
                {isPlaying ? <Pause size={28} color="#ffffff" /> : <Play size={28} color="#ffffff" />}
              </TouchableOpacity>
              <TouchableOpacity onPress={() => handleSeek(Math.min(duration, currentTime + 10))}>
                <SkipForward size={28} color="#ffffff" />
              </TouchableOpacity>
            </View>

            <View style={styles.controlGroup}>
              <TouchableOpacity onPress={toggleMute}>
                {isMuted || volume === 0 ? <VolumeX size={24} color="#ffffff" /> : <Volume2 size={24} color="#ffffff" />}
              </TouchableOpacity>
              <TouchableOpacity onPress={() => handleVolumeChange(isMuted ? volume || 1 : 0)} style={styles.volumeButton}>
                <Text style={styles.volumeButtonText}>{isMuted ? 'Unmute' : 'Mute'}</Text>
              </TouchableOpacity>
            </View>

            <View style={styles.controlGroup}>
              <TouchableOpacity>
                <Settings size={24} color="#ffffff" />
              </TouchableOpacity>
              <TouchableOpacity>
                <Download size={24} color="#ffffff" />
              </TouchableOpacity>
              <TouchableOpacity>
                <Share2 size={24} color="#ffffff" />
              </TouchableOpacity>
              <TouchableOpacity onPress={toggleFullscreen}>
                {isFullscreen ? <Minimize size={24} color="#ffffff" /> : <Maximize size={24} color="#ffffff" />}
              </TouchableOpacity>
            </View>
          </View>
        </View>
      )}

    </View>
  );
}

const formatTime = (seconds: number) => {
  if (isNaN(seconds)) return '0:00';
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs.toString().padStart(2, '0')}`;
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000000',
  },
  video: {
    flex: 1,
    width: '100%',
  },
  controlsContainer: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
  },
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingTop: 50,
    paddingBottom: 16,
  },
  titleContainer: {
    flex: 1,
    marginHorizontal: 16,
  },
  videoTitle: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  channelName: {
    color: '#ffffff99',
    fontSize: 12,
  },
  topRight: {
    paddingRight: 16,
  },
  progressContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingBottom: 8,
  },
  timeText: {
    color: '#ffffff',
    fontSize: 12,
    fontVariant: ['tabular-nums'],
    width: 45,
  },
  progressBar: {
    flex: 1,
    height: 4,
    backgroundColor: '#ffffff33',
    borderRadius: 2,
    marginHorizontal: 8,
  },
  progressFill: {
    height: '100%',
    backgroundColor: '#6366f1',
    borderRadius: 2,
  },
  bottomControls: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingBottom: 30,
    paddingTop: 8,
  },
  controlGroup: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
  },
  playButton: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: '#ffffff',
    justifyContent: 'center',
    alignItems: 'center',
  },
  volumeButton: {
    paddingHorizontal: 10,
    paddingVertical: 7,
    borderRadius: 8,
    backgroundColor: 'rgba(255,255,255,0.12)',
  },
  volumeButtonText: {
    color: '#ffffff',
    fontSize: 12,
    fontWeight: '600',
  },
});
