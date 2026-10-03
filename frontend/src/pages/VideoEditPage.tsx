import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useState } from 'react';
import { useDropzone } from 'react-dropzone';
import { Upload, X, Loader2, AlertCircle, CheckCircle, FileVideo, Settings, Tag, Hash, Globe, Eye, Lock, Calendar, Save, Trash2, RotateCcw, Video } from 'lucide-react';
import { clsx } from 'clsx';
import { uploadApi } from '../services/api/uploadApi.ts';
import { videoApi } from '../services/api/videoApi.ts';
import { categoryApi } from '../services/api/categoryApi.ts';
import { genreApi } from '../services/api/genreApi.ts';
import { languageApi } from '../services/api/languageApi.ts';
import { Card } from '../components/ui/Card.tsx';
import { Button } from '../components/ui/Button.tsx';
import { Input } from '../components/ui/Input.tsx';
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from '../components/ui/Select.tsx';
import { Progress } from '../components/ui/Progress.tsx';

export function VideoEditPage() {
  const { videoId } = useParams<{ videoId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [formData, setFormData] = React.useState({
    title: '',
    description: '',
    tags: '',
    categoryId: '',
    genreId: '',
    languageId: '',
    visibility: 'PRIVATE' as 'PUBLIC' | 'UNLISTED' | 'PRIVATE',
    allowComments: true,
    allowEmbedding: true,
    ageRestricted: false,
    thumbnailUrl: '',
  });

  const [categories, setCategories] = React.useState<any[]>([]);
  const [genres, setGenres] = React.useState<any[]>([]);
  const [languages, setLanguages] = React.useState<any[]>([]);
  const [thumbnailPreview, setThumbnailPreview] = React.useState<string | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [isSaving, setIsSaving] = React.useState(false);

  const { data: videoResponse, isLoading } = useQuery({
    queryKey: ['video', videoId],
    queryFn: () => videoApi.getDetail(videoId!),
    enabled: !!videoId,
  });

  const video = videoResponse?.data;

  React.useEffect(() => {
    categoryApi.getActiveRootCategories().then(setCategories);
    genreApi.getActiveRootGenres().then(setGenres);
    languageApi.getActiveLanguages().then(setLanguages);
  }, []);

  React.useEffect(() => {
    if (video) {
      setFormData({
        title: video.title || '',
        description: video.description || '',
        tags: video.tags || '',
        categoryId: video.categoryId || '',
        genreId: video.genreId || '',
        languageId: video.languageId || '',
        visibility: video.visibility || 'PRIVATE',
        allowComments: video.allowComments ?? true,
        allowEmbedding: video.allowEmbedding ?? true,
        ageRestricted: video.ageRestricted ?? false,
        thumbnailUrl: video.thumbnailUrl || '',
      });
      setThumbnailPreview(video.thumbnailUrl || null);
    }
  }, [video]);

  const onDrop = (acceptedFiles: File[]) => {
    const file = acceptedFiles[0];
    if (!file.type.startsWith('image/')) {
      setError('Please select an image file');
      return;
    }
    const reader = new FileReader();
    reader.onload = () => setThumbnailPreview(reader.result as string);
    reader.readAsDataURL(file);
    // TODO: Upload thumbnail to storage
  };

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: { 'image/*': ['.jpg', '.jpeg', '.png', '.webp'] },
    maxSize: 5 * 1024 * 1024,
    multiple: false,
  });

  const handleSave = async () => {
    setIsSaving(true);
    setError(null);
    try {
      await videoApi.updateVideo(videoId!, formData);
      queryClient.invalidateQueries({ queryKey: ['myVideos'] });
      queryClient.invalidateQueries({ queryKey: ['video', videoId] });
      navigate('/creator/videos');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save video');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Are you sure you want to delete this video? This cannot be undone.')) return;
    try {
      await videoApi.deleteVideo(videoId!);
      navigate('/creator/videos');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete video');
    }
  };

  if (isLoading) {
    return <div className="max-w-4xl mx-auto p-4"><div className="animate-pulse h-64 bg-[var(--color-bg-tertiary)] rounded-xl" /></div>;
  }

  if (!video) {
    return (
      <div className="max-w-4xl mx-auto p-4 text-center py-16">
        <Video className="w-16 h-16 mx-auto text-[var(--color-text-tertiary)] mb-4" />
        <h2 className="text-xl font-semibold text-[var(--color-text-primary)] mb-2">Video not found</h2>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto p-4 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-[var(--color-text-primary)]">Edit Video</h1>
          <p className="text-[var(--color-text-secondary)]">Update your video details</p>
        </div>
        <div className="flex gap-2">
          <Link to={`/watch/${video.slug || video.id}`} target="_blank">
            <Button variant="outline">
              <Eye className="w-4 h-4" />
              View Video
            </Button>
          </Link>
        </div>
      </div>

      {error && (
        <div className="flex items-center gap-2 p-3 bg-[var(--color-error)]/10 text-[var(--color-error)] rounded-lg">
          <AlertCircle className="w-4 h-4" />
          <span>{error}</span>
        </div>
      )}

      <Card className="p-5 space-y-6">
        <form className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-[var(--color-text-primary)] mb-2">Title *</label>
            <Input
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              placeholder="Enter video title"
              required
              maxLength={200}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[var(--color-text-primary)] mb-2">Description</label>
            <Input
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              placeholder="Tell viewers about your video"
              as="textarea"
              rows={4}
              maxLength={5000}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-[var(--color-text-primary)] mb-2">Tags</label>
            <Input
              value={formData.tags}
              onChange={(e) => setFormData({ ...formData, tags: e.target.value })}
              placeholder="tag1, tag2, tag3"
              maxLength={1000}
            />
            <p className="text-sm text-[var(--color-text-secondary)] mt-1">Comma-separated tags</p>
          </div>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                      <div>
                        <label className="block text-sm font-medium text-[var(--color-text-primary)] mb-2">Category *</label>
                        <Select value={formData.categoryId} onValueChange={(value) => setFormData({ ...formData, categoryId: value })} required>
                          <SelectTrigger>
                            <SelectValue placeholder="Select category" />
                          </SelectTrigger>
                          <SelectContent>
                            {categories.map((c: any) => (
                              <SelectItem key={c.id} value={c.id}>{c.name}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>

                      <div>
                        <label className="block text-sm font-medium text-[var(--color-text-primary)] mb-2">Genre</label>
                        <Select value={formData.genreId} onValueChange={(value) => setFormData({ ...formData, genreId: value })}>
                          <SelectTrigger>
                            <SelectValue placeholder="Select genre" />
                          </SelectTrigger>
                          <SelectContent>
                            {genres.map((g: any) => (
                              <SelectItem key={g.id} value={g.id}>{g.name}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>

                      <div>
                        <label className="block text-sm font-medium text-[var(--color-text-primary)] mb-2">Language</label>
                        <Select value={formData.languageId} onValueChange={(value) => setFormData({ ...formData, languageId: value })}>
                          <SelectTrigger>
                            <SelectValue placeholder="Select language" />
                          </SelectTrigger>
                          <SelectContent>
                            {languages.map((l: any) => (
                              <SelectItem key={l.id} value={l.id}>{l.name}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                    </div>

                    <div className="grid gap-4 sm:grid-cols-3">
                      <div>
                        <label className="block text-sm font-medium text-[var(--color-text-primary)] mb-2">Visibility</label>
                        <Select value={formData.visibility} onValueChange={(value) => setFormData({ ...formData, visibility: value })}>
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="PUBLIC">Public</SelectItem>
                            <SelectItem value="UNLISTED">Unlisted</SelectItem>
                            <SelectItem value="PRIVATE">Private</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>

            <div className="flex items-center gap-2 pt-6">
              <input type="checkbox" id="allowComments" checked={formData.allowComments} onChange={(e) => setFormData({ ...formData, allowComments: e.target.checked })} className="w-4 h-4 rounded border-[var(--color-border)] text-[var(--color-primary)]" />
              <label htmlFor="allowComments" className="text-sm text-[var(--color-text-secondary)]">Allow comments</label>
            </div>

            <div className="flex items-center gap-2 pt-6">
              <input type="checkbox" id="allowEmbedding" checked={formData.allowEmbedding} onChange={(e) => setFormData({ ...formData, allowEmbedding: e.target.checked })} className="w-4 h-4 rounded border-[var(--color-border)] text-[var(--color-primary)]" />
              <label htmlFor="allowEmbedding" className="text-sm text-[var(--color-text-secondary)]">Allow embedding</label>
            </div>
          </div>

          <div className="flex items-center gap-2 pt-6">
            <input type="checkbox" id="ageRestricted" checked={formData.ageRestricted} onChange={(e) => setFormData({ ...formData, ageRestricted: e.target.checked })} className="w-4 h-4 rounded border-[var(--color-border)] text-[var(--color-primary)]" />
            <label htmlFor="ageRestricted" className="text-sm text-[var(--color-text-secondary)]">Age restricted (18+)</label>
          </div>

          {/* Thumbnail */}
          <div className="pt-6 border-t border-[var(--color-border)]">
            <label className="block text-sm font-medium text-[var(--color-text-primary)] mb-2">Thumbnail</label>
            <div {...getRootProps()} className={clsx('relative aspect-video max-w-md overflow-hidden rounded-xl border-2 border-dashed', isDragActive && 'border-[var(--color-primary)] bg-[var(--color-primary)]/5')}>
              <input {...getInputProps()} />
              {thumbnailPreview ? (
                <img src={thumbnailPreview} alt="Preview" className="w-full h-full object-cover" />
              ) : (
                <div className="w-full h-full flex flex-col items-center justify-center gap-2">
                  <FileVideo className="w-12 h-12 text-[var(--color-text-tertiary)]" />
                  <p className="text-[var(--color-text-secondary)]">Drop image or click to select</p>
                  <p className="text-xs text-[var(--color-text-tertiary)]">JPG, PNG, WebP up to 5MB</p>
                </div>
              )}
              <input {...getInputProps()} />
            </div>
            <p className="text-sm text-[var(--color-text-secondary)] mt-2">Recommended: 1280x720 (16:9 aspect ratio)</p>
          </div>

          <div className="flex gap-3 pt-4 border-t border-[var(--color-border)]">
            <Button type="button" variant="outline" onClick={handleDelete}>
              <Trash2 className="w-4 h-4 text-[var(--color-error)]" />
              Delete Video
            </Button>
            <Button type="submit" onClick={handleSave} loading={isSaving}>
              <Save className="w-4 h-4" />
              Save Changes
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}