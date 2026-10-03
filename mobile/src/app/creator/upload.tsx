import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image, ActivityIndicator, ScrollView, RefreshControl, Modal, TextInput, KeyboardAvoidingView, Platform, Alert } from 'react-native';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link, useRouter, useLocalSearchParams } from 'expo-router';
import { Upload, X, Loader2, AlertCircle, CheckCircle, FileVideo, Settings, Tag, Hash, Globe, Eye, Lock, Calendar, Save, ArrowLeft, Camera, Image as ImageIcon, Check } from 'lucide-react-native';
import { uploadApi } from '../../services/api/uploadApi';
import { videoApi } from '../../services/api/videoApi';
import { categoryApi } from '../../services/api/categoryApi';
import { genreApi } from '../../services/api/genreApi';
import { languageApi } from '../../services/api/languageApi';
import { channelApi } from '../../services/api/channelApi';

const Select = ({ value, onValueChange, items, placeholder }: { value: string; onValueChange: (value: string) => void; items: { label: string; value: string }[]; placeholder?: string }) => (
  <View style={{ borderWidth: 1, borderColor: '#e2e8f0', borderRadius: 10, padding: 12 }}>
    <Text style={{ color: '#64748b', marginBottom: 6 }}>{placeholder}</Text>
    {items.map(item => (
      <TouchableOpacity key={item.value} onPress={() => onValueChange(item.value)} style={{ paddingVertical: 6 }}>
        <Text style={{ color: item.value === value ? '#6366f1' : '#0f172a', fontWeight: item.value === value ? '700' : '400' }}>{item.label}</Text>
      </TouchableOpacity>
    ))}
  </View>
);

export default function UploadScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{ fileUri?: string; fileName?: string; fileSize?: string; mimeType?: string }>();
  const { fileUri, fileName, fileSize, mimeType } = params;
  const [step, setStep] = React.useState<'details' | 'complete'>('details');
  const [formData, setFormData] = React.useState({
    title: fileName ? fileName.replace(/\.[^/.]+$/, '') : '',
    description: '',
    tags: '',
    categoryId: '',
    genreId: '',
    languageId: '',
    visibility: 'PRIVATE' as const,
    allowComments: true,
    allowEmbedding: true,
    ageRestricted: false,
    thumbnailUri: null,
  });

  const [categories, setCategories] = React.useState<any[]>([]);
  const [genres, setGenres] = React.useState<any[]>([]);
  const [languages, setLanguages] = React.useState<any[]>([]);
  const [error, setError] = React.useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = React.useState(false);

  React.useEffect(() => {
    categoryApi.getActiveRootCategories().then(setCategories);
    genreApi.getActiveRootGenres().then(setGenres);
    languageApi.getActiveLanguages().then(setLanguages);
  }, []);

  const handleImagePick = async () => {
    // Use expo-image-picker to select thumbnail
  };

  const handleSubmit = async () => {
    if (!formData.title.trim()) {
      setError('Title is required');
      return;
    }
    if (!formData.categoryId) {
      setError('Category is required');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      // Create video metadata
      const video = await videoApi.createVideo({
        ...formData,
      });

      // Navigate to video detail
      router.push(`/creator/videos/${video.id}/edit`);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create video');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
          <ArrowLeft size={24} color="#0f172a" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Upload Video</Text>
      </View>

      <ScrollView
        contentContainerStyle={styles.contentContainer}
        keyboardShouldPersistTaps="handled"
      >
        {error && (
          <View style={styles.errorBanner}>
            <AlertCircle size={20} color="#ef4444" style={{ marginRight: 8 }} />
            <Text style={styles.errorText}>{error}</Text>
          </View>
        )}

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Video Details</Text>
          
          <View style={styles.formGroup}>
            <Text style={styles.label}>Title *</Text>
            <TextInput
              style={styles.input}
              value={formData.title}
              onChangeText={(text) => setFormData({ ...formData, title: text })}
              placeholder="Enter video title"
              maxLength={200}
              autoCapitalize="words"
            />
          </View>

          <View style={styles.formGroup}>
            <Text style={styles.label}>Description</Text>
            <TextInput
              style={[styles.input, styles.textArea]}
              value={formData.description}
              onChangeText={(text) => setFormData({ ...formData, description: text })}
              placeholder="Tell viewers about your video"
              multiline
              numberOfLines={4}
              maxLength={5000}
            />
          </View>

          <View style={styles.formGroup}>
            <Text style={styles.label}>Tags</Text>
            <TextInput
              style={styles.input}
              value={formData.tags}
              onChangeText={(text) => setFormData({ ...formData, tags: text })}
              placeholder="tag1, tag2, tag3"
              maxLength={1000}
            />
            <Text style={styles.hint}>Comma-separated tags</Text>
          </View>

          <View style={styles.formRow}>
            <View style={styles.formGroupFlex}>
              <Text style={styles.label}>Category *</Text>
              <Select
                value={formData.categoryId}
                onValueChange={(value) => setFormData({ ...formData, categoryId: value })}
                placeholder="Select category"
                items={categories.map(c => ({ label: c.name, value: c.id }))}
              />
            </View>
            <View style={styles.formGroupFlex}>
              <Text style={styles.label}>Genre</Text>
              <Select
                value={formData.genreId}
                onValueChange={(value) => setFormData({ ...formData, genreId: value })}
                placeholder="Select genre"
                items={genres.map(g => ({ label: g.name, value: g.id }))}
              />
            </View>
          </View>

          <View style={styles.formRow}>
            <View style={styles.formGroupFlex}>
              <Text style={styles.label}>Language</Text>
              <Select
                value={formData.languageId}
                onValueChange={(value) => setFormData({ ...formData, languageId: value })}
                placeholder="Select language"
                items={languages.map(l => ({ label: l.name, value: l.id }))}
              />
            </View>
            <View style={styles.formGroupFlex}>
              <Text style={styles.label}>Visibility</Text>
              <Select
                value={formData.visibility}
                onValueChange={(value) => setFormData({ ...formData, visibility: value })}
                placeholder="Select visibility"
                items={[
                  { label: 'Public', value: 'PUBLIC' },
                  { label: 'Unlisted', value: 'UNLISTED' },
                  { label: 'Private', value: 'PRIVATE' },
                ]}
              />
            </View>
          </View>

          <View style={styles.formGroup}>
            <TouchableOpacity style={styles.checkboxRow} onPress={() => setFormData({ ...formData, allowComments: !formData.allowComments })}>
              <View style={[
                styles.checkbox,
                formData.allowComments && styles.checkboxChecked
              ]}>
                {formData.allowComments && <Check size={16} color="#ffffff" />}
              </View>
              <Text style={styles.checkboxLabel}>Allow comments</Text>
            </TouchableOpacity>

            <TouchableOpacity style={styles.checkboxRow} onPress={() => setFormData({ ...formData, allowEmbedding: !formData.allowEmbedding })}>
              <View style={[
                styles.checkbox,
                formData.allowEmbedding && styles.checkboxChecked
              ]}>
                {formData.allowEmbedding && <Check size={16} color="#ffffff" />}
              </View>
              <Text style={styles.checkboxLabel}>Allow embedding</Text>
            </TouchableOpacity>
          </View>

          <View style={styles.formGroup}>
            <TouchableOpacity style={styles.checkboxRow} onPress={() => setFormData({ ...formData, ageRestricted: !formData.ageRestricted })}>
              <View style={[
                styles.checkbox,
                formData.ageRestricted && styles.checkboxChecked
              ]}>
                {formData.ageRestricted && <Check size={16} color="#ffffff" />}
              </View>
              <Text style={styles.checkboxLabel}>Age restricted (18+)</Text>
            </TouchableOpacity>
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Thumbnail</Text>
          <TouchableOpacity style={styles.thumbnailPicker} onPress={() => {}}>
            {formData.thumbnailUri ? (
              <Image source={{ uri: formData.thumbnailUri }} style={styles.thumbnailPreview} />
            ) : (
              <View style={styles.thumbnailPlaceholder}>
                <ImageIcon size={32} color="#94a3b8" style={{ marginBottom: 8 }} />
                <Text style={styles.thumbnailPlaceholderText}>Tap to select thumbnail</Text>
                <Text style={styles.thumbnailPlaceholderHint}>Recommended: 1280x720 (16:9)</Text>
              </View>
            )}
          </TouchableOpacity>
        </View>

        {error && (
          <View style={styles.errorBanner}>
            <AlertCircle size={20} color="#ef4444" style={{ marginRight: 8 }} />
            <Text style={styles.errorText}>{error}</Text>
          </View>
        )}

        <View style={styles.actions}>
          <TouchableOpacity style={styles.cancelButton} onPress={() => router.back()}>
            <Text style={styles.cancelButtonText}>Cancel</Text>
          </TouchableOpacity>
          <TouchableOpacity 
            style={[styles.submitButton, isSubmitting && styles.submitButtonDisabled]} 
            onPress={() => {}}
            disabled={isSubmitting}
          >
            {isSubmitting ? (
              <ActivityIndicator size="small" color="#ffffff" />
            ) : (
              <Text style={styles.submitButtonText}>
                <Save size={18} color="#ffffff" style={{ marginRight: 8 }} />
                Publish Video
              </Text>
            )}
          </TouchableOpacity>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8fafc',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#ffffff',
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
  },
  backButton: {
    padding: 8,
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#0f172a',
  },
  contentContainer: {
    padding: 16,
    paddingBottom: 100,
  },
  section: {
    marginBottom: 24,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#0f172a',
    marginBottom: 16,
  },
  formGroup: {
    marginBottom: 20,
  },
  formGroupFlex: {
    flex: 1,
    marginRight: 8,
  },
  formRow: {
    flexDirection: 'row',
    gap: 12,
  },
  label: {
    fontSize: 14,
    fontWeight: '600',
    color: '#0f172a',
    marginBottom: 8,
  },
  input: {
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: '#e2e8f0',
    borderRadius: 10,
    paddingHorizontal: 16,
    paddingVertical: 14,
    fontSize: 16,
    color: '#0f172a',
  },
  textArea: {
    minHeight: 100,
    textAlignVertical: 'top',
  },
  hint: {
    fontSize: 12,
    color: '#94a3b8',
    marginTop: 4,
  },
  checkboxRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: 12,
  },
  checkbox: {
    width: 22,
    height: 22,
    borderWidth: 2,
    borderColor: '#e2e8f0',
    borderRadius: 6,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 10,
    backgroundColor: '#f8fafc',
  },
  checkboxChecked: {
    backgroundColor: '#6366f1',
    borderColor: '#6366f1',
  },
  checkboxLabel: {
    fontSize: 14,
    color: '#0f172a',
  },
  errorBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: '#fef2f2',
    borderWidth: 1,
    borderColor: '#fecaca',
    borderRadius: 8,
    padding: 12,
    marginBottom: 16,
  },
  errorText: {
    color: '#ef4444',
    fontSize: 14,
  },
  thumbnailPicker: {
    borderWidth: 2,
    borderStyle: 'dashed',
    borderColor: '#cbd5e1',
    borderRadius: 12,
    aspectRatio: 16 / 9,
    overflow: 'hidden',
  },
  thumbnailPreview: {
    width: '100%',
    height: '100%',
  },
  thumbnailPlaceholder: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    gap: 8,
    backgroundColor: '#f1f5f9',
  },
  thumbnailPlaceholderText: {
    fontSize: 14,
    fontWeight: '500',
    color: '#64748b',
  },
  thumbnailPlaceholderHint: {
    fontSize: 12,
    color: '#94a3b8',
  },
  actions: {
    flexDirection: 'row',
    gap: 12,
    paddingTop: 8,
    marginTop: 8,
  },
  cancelButton: {
    flex: 1,
    paddingVertical: 14,
    paddingHorizontal: 24,
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: '#e2e8f0',
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
  },
  cancelButtonText: {
    color: '#64748b',
    fontSize: 16,
    fontWeight: '600',
  },
  submitButton: {
    flex: 1,
    paddingVertical: 14,
    paddingHorizontal: 24,
    backgroundColor: '#6366f1',
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
    flexDirection: 'row',
    gap: 8,
  },
  submitButtonDisabled: {
    opacity: 0.7,
  },
  submitButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
});
