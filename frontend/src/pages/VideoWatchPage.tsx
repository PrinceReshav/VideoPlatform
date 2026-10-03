import { useQuery } from '@tanstack/react-query';
import { useParams, Link } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { VideoPlayer } from '../components/video/VideoPlayer.tsx';
import { Card } from '../components/ui/Card.tsx';
import { Button } from '../components/ui/Button.tsx';
import { Avatar } from '../components/ui/Avatar.tsx';
import { videoApi } from '../services/api/videoApi.ts';
import { subscriptionApi } from '../services/api/subscriptionApi.ts';
import { engagementApi } from '../services/api/engagementApi.ts';
import { useAuth } from '../app/providers/AuthProvider.tsx';
import { formatDistanceToNow } from 'date-fns';
import { ThumbsUp, ThumbsDown, MessageCircle, Share2, MoreHorizontal, Bell, Download, Video } from 'lucide-react';

export function VideoWatchPage() {
  const { videoId } = useParams<{ videoId: string }>();
  const { isAuthenticated } = useAuth();
  const [isLiked, setIsLiked] = useState(false);
  const [isSubscribed, setIsSubscribed] = useState(false);

  const { data: videoResponse, isLoading } = useQuery({
    queryKey: ['video', videoId],
    queryFn: () => videoApi.getDetail(videoId!),
    enabled: !!videoId,
  });

  const video = videoResponse?.data ?? videoResponse;

  const { data: likeStatus } = useQuery({
    queryKey: ['like', videoId],
    queryFn: () => engagementApi.checkLike(videoId!),
    enabled: !!videoId && isAuthenticated,
  });

  const { data: subscriptionStatus } = useQuery({
    queryKey: ['subscription', video?.channelId],
    queryFn: () => subscriptionApi.isSubscribed(video!.channelId),
    enabled: !!video?.channelId && isAuthenticated,
  });

  useEffect(() => {
    if (likeStatus?.data !== undefined) setIsLiked(likeStatus.data);
    else if (typeof likeStatus === 'boolean') setIsLiked(likeStatus);
    if (subscriptionStatus?.data !== undefined) setIsSubscribed(subscriptionStatus.data);
    else if (typeof subscriptionStatus === 'boolean') setIsSubscribed(subscriptionStatus);
  }, [likeStatus, subscriptionStatus]);

  const handleLike = async () => {
    if (!isAuthenticated) return;
    try {
      await engagementApi.toggleLike(videoId!);
      setIsLiked(!isLiked);
    } catch (error) {
      console.error('Like failed:', error);
    }
  };

  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto p-4">
        <div className="aspect-video w-full rounded-xl bg-[var(--color-bg-tertiary)] animate-pulse" />
        <div className="mt-4 space-y-4">
          <div className="h-6 w-3/4 bg-[var(--color-bg-tertiary)] rounded animate-pulse" />
          <div className="h-4 w-1/2 bg-[var(--color-bg-tertiary)] rounded animate-pulse" />
        </div>
      </div>
    );
  }

  if (!video) {
    return (
      <div className="max-w-4xl mx-auto p-4 text-center py-16">
        <Video className="w-16 h-16 mx-auto text-[var(--color-text-tertiary)] mb-4" />
        <h2 className="text-xl font-semibold text-[var(--color-text-primary)] mb-2">Video not found</h2>
        <p className="text-[var(--color-text-secondary)]">This video doesn't exist or has been removed.</p>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto p-4 space-y-6">
      {/* Video Player */}
      <div className="aspect-video w-full rounded-xl overflow-hidden bg-black">
        <VideoPlayer
          src={video.hlsManifestUrl || ''}
          poster={video.thumbnailUrl}
        />
      </div>

      {/* Video Info */}
      <div className="space-y-4">
        <h1 className="text-2xl font-bold text-[var(--color-text-primary)]">{video.title}</h1>

        <div className="flex flex-wrap items-center gap-4 text-[var(--color-text-secondary)] text-sm">
          <span>{video.viewCount?.toLocaleString()} views</span>
          <span>•</span>
          <span>{formatDistanceToNow(new Date(video.publishedAt), { addSuffix: true })}</span>
        </div>

        {/* Channel Info */}
        <div className="flex items-center gap-4 p-4 bg-[var(--color-surface)] rounded-xl border border-[var(--color-border)]">
          <Link to={`/channel/${video.channelSlug}`}>
            <Avatar src={video.channelAvatarUrl} alt={video.channelName} size="md" />
          </Link>
          <div className="flex-1">
            <Link to={`/channel/${video.channelSlug}`} className="font-semibold text-[var(--color-text-primary)] hover:text-[var(--color-primary)]">
              {video.channelName}
            </Link>
            <p className="text-sm text-[var(--color-text-secondary)]">{video.subscriberCount?.toLocaleString()} subscribers</p>
          </div>
          <Button variant={isSubscribed ? 'outline' : 'primary'} size="sm">
            {isSubscribed ? 'Subscribed' : 'Subscribe'}
          </Button>
          <Button variant="ghost" size="icon" aria-label="Notifications">
            <Bell className="w-5 h-5" />
          </Button>
        </div>

        {/* Actions */}
        <div className="flex flex-wrap gap-2">
          <Button variant={isLiked ? 'secondary' : 'outline'} onClick={handleLike} className="gap-2">
            <ThumbsUp className="w-5 h-5" />
            <span>{video.likeCount?.toLocaleString()}</span>
          </Button>
          <Button variant="outline" className="gap-2">
            <ThumbsDown className="w-5 h-5" />
          </Button>
          <Button variant="outline" className="gap-2">
            <MessageCircle className="w-5 h-5" />
            <span>{video.commentCount?.toLocaleString()}</span>
          </Button>
          <Button variant="outline" className="gap-2">
            <Share2 className="w-5 h-5" />
            <span>Share</span>
          </Button>
          <Button variant="outline" className="gap-2">
            <Download className="w-5 h-5" />
            <span>Save</span>
          </Button>
          <Button variant="ghost" size="icon" className="ml-auto">
            <MoreHorizontal className="w-5 h-5" />
          </Button>
        </div>

        {/* Description */}
        {video.description && (
          <div className="prose prose-invert max-w-none text-[var(--color-text-secondary)]">
            <p>{video.description}</p>
          </div>
        )}

        {/* Tags */}
        {video.tags && (
          <div className="flex flex-wrap gap-2">
            {video.tags.split(',').map((tag: string) => (
              <Link
                key={tag.trim()}
                to={`/search?q=${encodeURIComponent(tag.trim())}`}
                className="px-3 py-1 bg-[var(--color-bg-tertiary)] text-sm text-[var(--color-text-secondary)] rounded-full hover:bg-[var(--color-bg-hover)] transition-colors"
              >
                #{tag.trim()}
              </Link>
            ))}
          </div>
        )}

        {/* Comments Section */}
        <CommentsSection />
      </div>
    </div>
  );
}

function CommentsSection() {
  // Comments implementation would go here
  return (
    <Card className="mt-8">
      <div className="p-5 border-b border-[var(--color-border)]">
        <h2 className="text-xl font-semibold text-[var(--color-text-primary)]">Comments</h2>
      </div>
      <div className="p-5">
        <p className="text-[var(--color-text-secondary)]">Comments loading...</p>
      </div>
    </Card>
  );
}