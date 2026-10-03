import React from 'react';
import { Link } from 'react-router-dom';
import { useState } from 'react';
import { useDropzone } from 'react-dropzone';
import { Upload, X, Loader2, AlertCircle, CheckCircle, FileVideo, Settings, Tag, Hash, Globe, Eye, Lock, Calendar } from 'lucide-react';
import { clsx } from 'clsx';
import { uploadApi } from '../services/api/uploadApi.ts';
import { videoApi } from '../services/api/videoApi.ts';
import { categoryApi } from '../services/api/categoryApi.ts';
import { genreApi } from '../services/api/genreApi.ts';
import { languageApi } from '../services/api/languageApi.ts';
import { Card } from '../components/ui/Card.tsx';
import { Button } from '../components/ui/Button.tsx';
import { Input } from '../components/ui/Input.tsx';
import { Select } from '../components/ui/Select.tsx';
import { Progress } from '../components/ui/Progress.tsx';

export function UploadPage() {
  const [step, setStep] = React.useState<'upload' | 'details' | 'complete'>('upload');
  const [uploadSession, setUploadSession] = React.useState<any>(null);
  const [selectedFile, setSelectedFile] = React.useState<File | null>(null);
  const [uploadProgress, setUploadProgress] = React.useState(0);
  const [uploadedChunks, setUploadedChunks] = React.useState<number[]>([]);
  const [isUploading, setIsUploading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  // Form data
  const [formData, setFormData] = React.useState({
    title: '',
    description: '',
    tags: '',
    categoryId: '',
    genreId: '',
    languageId: '',
    visibility: 'PRIVATE' as const,
    allowComments: true,
    allowEmbedding: true,
    ageRestricted: false,
  });

  const [categories, setCategories] = React.useState<any[]>([]);
  const [genres, setGenres] = React.useState<any[]>([]);
  const [languages, setLanguages] = React.useState<any[]>([]);

  // Load reference data
  React.useEffect(() => {
    categoryApi.getActiveRootCategories().then(setCategories);
    genreApi.getActiveRootGenres().then(setGenres);
    languageApi.getActiveLanguages().then(setLanguages);
  }, []);

  const onDrop = (acceptedFiles: File[]) => {
    const file = acceptedFiles[0];
    if (!file.type.startsWith('video/')) {
      setError('Please select a video file');
      return;
    }
    if (file.size > 50 * 1024 * 1024 * 1024) { // 50GB
      setError('File size exceeds 50GB limit');
      return;
    }
    setSelectedFile(file);
    setError(null);
  };

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'video/mp4': ['.mp4'],
      'video/quicktime': ['.mov'],
      'video/x-matroska': ['.mkv'],
      'video/webm': ['.webm'],
    },
    maxSize: 50 * 1024 * 1024 * 1024,
    multiple: false,
  });

  const handleCreateSession = async () => {
    if (!selectedFile) return;
    setIsUploading(true);
    setError(null);

    try {
      const session = await uploadApi.createSession({
        fileName: selectedFile.name,
        contentType: selectedFile.type,
        totalSize: selectedFile.size,
      });
      setUploadSession(session);
      setUploadProgress(0);
      setUploadedChunks([]);
      startChunkedUpload(session, selectedFile);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create upload session');
      setIsUploading(false);
    }
  };

  const startChunkedUpload = async (session: any, file: File) => {
    const chunkSize = session.chunkSize;
    const totalChunks = session.totalChunks;

    for (let partNumber = 1; partNumber <= totalChunks; partNumber++) {
      if (uploadedChunks.includes(partNumber)) continue;

      const start = (partNumber - 1) * chunkSize;
      const end = Math.min(start + chunkSize, file.size);
      const chunk = file.slice(start, end);

      try {
        await uploadApi.uploadChunk(session.id, partNumber, chunk);
        setUploadedChunks(prev => [...prev, partNumber]);
        setUploadProgress((partNumber / totalChunks) * 100);
      } catch (err) {
        setError(`Failed to upload chunk ${partNumber}`);
        setIsUploading(false);
        return;
      }
    }

    // Complete upload
    try {
      await uploadApi.completeSession(session.id, { parts: [] });
      setUploadProgress(100);
      setStep('details');
      setIsUploading(false);
    } catch (err) {
      setError('Failed to complete upload');
      setIsUploading(false);
    }
  };

  const handleComplete = async () => {
    try {
      const video = await videoApi.createVideo({
        ...formData,
        uploadSessionId: uploadSession.id,
      });
      // Navigate to video edit page
      window.location.href = `/creator/videos/${video.id}/edit`;
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create video');
    }
  };

  if (step === 'upload') {
    return (
      <div className="max-w-4xl mx-auto p-4 space-y-6">
        <div>
          <h1 className="text-3xl font-bold text-[var(--color-text-primary)]">Upload Video</h1>
          <p className="text-[var(--color-text-secondary)]">Drag and drop or select a video file to upload</p>
        </div>

        <Card className={clsx('p-8 text-center', isDragActive && 'border-[var(--color-primary)] bg-[var(--color-primary)]/5')}>
          <div {...getRootProps()} className="relative">
            <input {...getInputProps()} />
            <div className="space-y-4">
              <Upload className="w-16 h-16 mx-auto text-[var(--color-text-tertiary)]" />
              <p className="text-[var(--color-text-primary)]">
                {isDragActive ? 'Drop the video file here' : 'Drag & drop a video file, or click to select'}
              </p>
              <p className="text-sm text-[var(--color-text-secondary)]">
                Supports MP4, MOV, MKV, WebM up to 50GB
              </p>
              {selectedFile && (
                <div className="flex items-center justify-center gap-4 p-4 bg-[var(--color-bg-tertiary)] rounded-lg">
                  <FileVideo className="w-8 h-8 text-[var(--color-primary)]" />
                  <div className="text-left">
                    <p className="font-medium text-[var(--color-text-primary)]">{selectedFile.name}</p>
                    <p className="text-sm text-[var(--color-text-secondary)]">
                      {(selectedFile.size / (1024 * 1024)).toFixed(1)} MB
                    </p>
                  </div>
                  <Button variant="ghost" size="icon" onClick={() => setSelectedFile(null)}>
                    <X className="w-4 h-4" />
                  </Button>
                </div>
              )}
              {error && (
                <div className="flex items-center gap-2 text-[var(--color-error)]">
                  <AlertCircle className="w-4 h-4" />
                  <span>{error}</span>
                </div>
              )}
              {!isUploading && selectedFile && !uploadSession && (
                <Button onClick={handleCreateSession} size="lg" className="mt-4">
                  <Upload className="w-4 h-4" />
                  Start Upload
                </Button>
              )}
            </div>
          </div>
        </Card>
      </div>
    );
  }

  if (step === 'details') {
    return (
      <div className="max-w-4xl mx-auto p-4 space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-[var(--color-text-primary)]">Video Details</h1>
            <p className="text-[var(--color-text-secondary)]">Add metadata for your video</p>
          </div>
          <div className="flex gap-2">
            <span className="px-3 py-1 bg-[var(--color-success)]/10 text-[var(--color-success)] text-sm font-medium rounded-full">Upload Complete</span>
          </div>
        </div>

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
              <Select label="Category *" value={formData.categoryId} onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })} required>
                <Select.Trigger><Select.Value placeholder="Select category" /></Select.Trigger>
                <Select.Content>
                  {categories.map((c: any) => (
                    <Select.Item key={c.id} value={c.id}>{c.name}</Select.Item>
                  ))}
                </Select.Content>
              </Select>

              <Select label="Genre" value={formData.genreId} onChange={(e) => setFormData({ ...formData, genreId: e.target.value })}>
                <Select.Trigger><Select.Value placeholder="Select genre" /></Select.Trigger>
                <Select.Content>
                  {genres.map((g: any) => (
                    <Select.Item key={g.id} value={g.id}>{g.name}</Select.Item>
                  ))}
                </Select.Content>
              </Select>

              <Select label="Language" value={formData.languageId} onChange={(e) => setFormData({ ...formData, languageId: e.target.value })}>
                <Select.Trigger><Select.Value placeholder="Select language" /></Select.Trigger>
                <Select.Content>
                  {languages.map((l: any) => (
                    <Select.Item key={l.id} value={l.id}>{l.name}</Select.Item>
                  ))}
                </Select.Content>
              </Select>
            </div>

            <div className="grid gap-4 sm:grid-cols-3">
              <Select label="Visibility" value={formData.visibility} onChange={(e) => setFormData({ ...formData, visibility: e.target.value as any })}>
                <Select.Trigger><Select.Value /></Select.Trigger>
                <Select.Content>
                  <Select.Item value="PUBLIC">Public</Select.Item>
                  <Select.Item value="UNLISTED">Unlisted</Select.Item>
                  <Select.Item value="PRIVATE">Private</Select.Item>
                </Select.Content>
              </Select>

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

            <div className="flex gap-3 pt-4">
              <Button type="submit" onClick={handleComplete} size="lg">
                <CheckCircle className="w-4 h-4" />
                Publish Video
              </Button>
              <Button variant="outline" type="button">
                Save as Draft
              </Button>
            </div>
          </form>
        </Card>
      </div>
    );
  }

  if (step === 'complete') {
    return (
      <div className="max-w-4xl mx-auto p-4 space-y-6">
        <Card className="p-8 text-center">
          <div className="w-20 h-20 mx-auto mb-4 bg-[var(--color-success)]/10 rounded-full flex items-center justify-center">
            <CheckCircle className="w-10 h-10 text-[var(--color-success)]" />
          </div>
          <h1 className="text-3xl font-bold text-[var(--color-text-primary)] mb-2">Upload Complete!</h1>
          <p className="text-[var(--color-text-secondary)] mb-6">Your video has been published and is now live.</p>
          <div className="flex gap-3 justify-center">
            <Link to={`/watch/${uploadSession?.videoId}`}>
              <Button size="lg">Watch Video</Button>
            </Link>
            <Link to="/creator/videos">
              <Button variant="outline" size="lg">Manage Videos</Button>
            </Link>
          </div>
        </Card>
      </div>
    );
  }
}