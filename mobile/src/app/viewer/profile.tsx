import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image, TextInput, ActivityIndicator, ScrollView, RefreshControl, SafeAreaView } from 'react-native';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'expo-router';
import { User, Settings, Bell, Palette, Moon, Sun, Shield, LogOut, Key, Mail, Save, Camera, Monitor } from 'lucide-react-native';
import { authApi } from '../../services/api/authApi';
import { useAuth } from '../../app/providers/AuthProvider';
import { useTheme } from '../../app/providers/ThemeProvider';

export default function ProfileScreen() {
  const { user, isAuthenticated } = useAuth();
  const { theme, resolvedTheme, setTheme, palette, setPalette } = useTheme();
  const [activeTab, setActiveTab] = React.useState<'profile' | 'security' | 'notifications' | 'appearance' | 'privacy'>('profile');
  const [avatarPreview, setAvatarPreview] = React.useState<string | null>(null);

  const { data: profile } = useQuery({
    queryKey: ['userProfile', user?.id],
    queryFn: () => authApi.getCurrentUser(),
    enabled: isAuthenticated,
  });

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        <View style={styles.header}>
          <Text style={styles.headerTitle}>Profile</Text>
        </View>

        <View style={styles.tabs}>
          {[
            { id: 'profile', label: 'Profile', icon: User },
            { id: 'security', label: 'Security', icon: Shield },
            { id: 'notifications', label: 'Notifications', icon: Bell },
            { id: 'appearance', label: 'Appearance', icon: Palette },
            { id: 'privacy', label: 'Privacy', icon: Shield },
          ].map((tab) => (
            <TouchableOpacity
              key={tab.id}
              onPress={() => setActiveTab(tab.id as any)}
              style={[
                styles.tab,
                activeTab === tab.id && styles.tabActive,
              ]}
            >
              <tab.icon size={20} color={activeTab === tab.id ? '#6366f1' : '#94a3b8'} />
              <Text style={[
                styles.tabLabel,
                activeTab === tab.id && styles.tabLabelActive,
              ]}>
                {tab.label}
              </Text>
            </TouchableOpacity>
          ))}
        </View>

        {activeTab === 'profile' && <ProfileTab user={profile || user} />}
        {activeTab === 'security' && <SecurityTab />}
        {activeTab === 'notifications' && <NotificationsTab />}
        {activeTab === 'appearance' && <AppearanceTab />}
        {activeTab === 'privacy' && <PrivacyTab />}
      </ScrollView>
    </SafeAreaView>
  );
}

function ProfileTab({ user }: { user: any }) {
  const [isEditing, setIsEditing] = React.useState(false);
  const [formData, setFormData] = React.useState({
    displayName: user?.displayName || '',
    email: user?.email || '',
    bio: user?.bio || '',
    websiteUrl: user?.websiteUrl || '',
    location: user?.location || '',
  });
  const [avatarPreview, setAvatarPreview] = React.useState<string | null>(null);

  const handleSubmit = async (e: any) => {
    e.preventDefault();
    setIsEditing(false);
  };

  if (isEditing) {
    return (
      <View style={styles.card}>
        <View style={styles.cardHeader}>
          <Text style={styles.cardTitle}>Edit Profile</Text>
        </View>
        <ScrollView style={styles.formScroll} keyboardShouldPersistTaps="handled">
          <View style={styles.formGroup}>
            <View style={styles.avatarRow}>
              <View style={styles.avatarPreview}>
                {avatarPreview ? (
                  <Image source={{ uri: avatarPreview }} style={styles.avatarLarge} />
                ) : (
                  <Image source={{ uri: user?.avatarUrl }} style={styles.avatarLarge} />
                )}
              </View>
              <TouchableOpacity style={styles.avatarUploadBtn} onPress={() => {}}>
                <Camera size={20} color="#6366f1" />
              </TouchableOpacity>
            </View>
          </View>
          <View style={styles.formGroup}>
            <Text style={styles.label}>Display Name</Text>
            <TextInput style={styles.input} value={formData.displayName} onChangeText={(text) => setFormData({...formData, displayName: text})} />
          </View>
          <View style={styles.formGroup}>
            <Text style={styles.label}>Email</Text>
            <TextInput style={styles.input} value={formData.email} onChangeText={(text) => setFormData({...formData, email: text})} keyboardType="email-address" />
          </View>
          <View style={styles.formGroup}>
            <Text style={styles.label}>Bio</Text>
            <TextInput style={[styles.input, styles.textArea]} value={formData.bio} onChangeText={(text) => setFormData({...formData, bio: text})} multiline numberOfLines={3} />
          </View>
          <View style={styles.formGroup}>
            <Text style={styles.label}>Website</Text>
            <TextInput style={styles.input} value={formData.websiteUrl} onChangeText={(text) => setFormData({...formData, websiteUrl: text})} placeholder="https://example.com" />
          </View>
          <View style={styles.formGroup}>
            <Text style={styles.label}>Location</Text>
            <TextInput style={styles.input} value={formData.location} onChangeText={(text) => setFormData({...formData, location: text})} placeholder="City, Country" />
          </View>
          <View style={styles.formActions}>
            <TouchableOpacity style={styles.cancelButton} onPress={() => setIsEditing(false)}>
              <Text style={styles.cancelButtonText}>Cancel</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.saveButton} onPress={handleSubmit}>
              <Text style={styles.saveButtonText}>Save Changes</Text>
            </TouchableOpacity>
          </View>
        </ScrollView>
      </View>
    );
  }

  return (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <View style={styles.profileHeader}>
          <Image source={{ uri: user?.avatarUrl }} style={styles.avatarLarge} />
          <View style={styles.profileInfo}>
            <Text style={styles.displayName}>{user?.displayName}</Text>
            <Text style={styles.username}>@{user?.username}</Text>
          </View>
        </View>
        <TouchableOpacity style={styles.editButton} onPress={() => setIsEditing(true)}>
          <Text style={styles.editButtonText}>Edit Profile</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.profileFields}>
        <View style={styles.field}>
          <Text style={styles.fieldLabel}>Email</Text>
          <Text style={styles.fieldValue}>{user?.email}</Text>
        </View>
        <View style={styles.field}>
          <Text style={styles.fieldLabel}>Joined</Text>
          <Text style={styles.fieldValue}>January 2024</Text>
        </View>
      </View>

      {user?.bio && (
        <View style={styles.bioSection}>
          <Text style={styles.fieldLabel}>Bio</Text>
          <Text style={styles.bioText}>{user?.bio}</Text>
        </View>
      )}

      {user?.websiteUrl && (
        <View style={styles.bioSection}>
          <Text style={styles.fieldLabel}>Website</Text>
          <Text style={styles.linkText}>{user?.websiteUrl}</Text>
        </View>
      )}

      {user?.location && (
        <View style={styles.bioSection}>
          <Text style={styles.fieldLabel}>Location</Text>
          <Text style={styles.fieldValue}>{user?.location}</Text>
        </View>
      )}
    </View>
  );
}

function SecurityTab() {
  const [currentPassword, setCurrentPassword] = React.useState('');
  const [newPassword, setNewPassword] = React.useState('');
  const [confirmPassword, setConfirmPassword] = React.useState('');
  const [showPassword, setShowPassword] = React.useState(false);

  return (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <Text style={styles.cardTitle}>Security</Text>
      </View>

      <View style={styles.cardContent}>
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Change Password</Text>
          <View style={styles.formGroup}>
            <Text style={styles.label}>Current Password</Text>
            <TextInput
              style={styles.input}
              secureTextEntry={!showPassword}
              value={currentPassword}
              onChangeText={setCurrentPassword}
              placeholder="Current password"
            />
          </View>
          <View style={styles.formGroup}>
            <Text style={styles.label}>New Password</Text>
            <TextInput
              style={styles.input}
              secureTextEntry={!showPassword}
              value={newPassword}
              onChangeText={setNewPassword}
              placeholder="New password"
            />
          </View>
          <View style={styles.formGroup}>
            <Text style={styles.label}>Confirm New Password</Text>
            <TextInput
              style={styles.input}
              secureTextEntry={!showPassword}
              value={confirmPassword}
              onChangeText={setConfirmPassword}
              placeholder="Confirm new password"
            />
          </View>
          <TouchableOpacity style={styles.saveButton}>
            <Text style={styles.saveButtonText}>Update Password</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Two-Factor Authentication</Text>
          <Text style={styles.sectionDesc}>Add an extra layer of security to your account.</Text>
          <TouchableOpacity style={styles.outlineButton}>
            <Text style={styles.outlineButtonText}>Enable 2FA</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Active Sessions</Text>
          <Text style={styles.sectionDesc}>Manage devices logged into your account.</Text>
          <TouchableOpacity style={styles.dangerButton}>
            <Text style={styles.dangerButtonText}>Log out of all sessions</Text>
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
}

function NotificationsTab() {
  return (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <Text style={styles.cardTitle}>Notifications</Text>
      </View>
      <View style={styles.cardContent}>
        <Text style={styles.sectionTitle}>Notification Preferences</Text>
        <Text style={styles.sectionDesc}>Choose what notifications you want to receive.</Text>

        <View style={styles.prefList}>
          {[
            { id: 'newVideo', label: 'New videos from subscriptions', default: true },
            { id: 'comments', label: 'New comments on your videos', default: true },
            { id: 'replies', label: 'Replies to your comments', default: true },
            { id: 'likes', label: 'Likes on your videos', default: true },
            { id: 'subscribers', label: 'New subscribers', default: true },
            { id: 'milestones', label: 'Channel milestones', default: true },
            { id: 'announcements', label: 'Platform announcements', default: true },
            { id: 'email', label: 'Email notifications', default: false },
            { id: 'push', label: 'Push notifications', default: true },
          ].map((pref) => (
            <View key={pref.id} style={styles.prefItem}>
              <View style={styles.prefInfo}>
                <Text style={styles.prefLabel}>{pref.label}</Text>
              </View>
              <Switch value={pref.default} onValueChange={() => {}} />
            </View>
          ))}
        </View>
      </View>
    </View>
  );
}

function AppearanceTab() {
  const { theme, resolvedTheme, setTheme, palette, setPalette } = useTheme();

  return (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <Text style={styles.cardTitle}>Appearance</Text>
      </View>
      <View style={styles.cardContent}>
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Theme</Text>
          <View style={styles.themeOptions}>
            {['light', 'dark', 'system'].map((t) => (
              <TouchableOpacity
                key={t}
                onPress={() => setTheme(t as any)}
                style={[
                  styles.themeOption,
                  theme === t && styles.themeOptionActive,
                ]}
              >
                <View style={styles.themeIcon}>
                  {t === 'light' && <Sun size={24} color={theme === t ? '#ffffff' : '#6366f1'} />}
                  {t === 'dark' && <Moon size={24} color={theme === t ? '#ffffff' : '#6366f1'} />}
                  {t === 'system' && <Monitor size={24} color={theme === t ? '#ffffff' : '#6366f1'} />}
                </View>
                <Text style={[
                  styles.themeLabel,
                  theme === t && styles.themeLabelActive,
                ]}>
                  {t.charAt(0).toUpperCase() + t.slice(1)}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Color Palette</Text>
          <View style={styles.paletteOptions}>
            {['default', 'ocean', 'forest', 'sunset', 'monochrome'].map((p) => (
              <TouchableOpacity
                key={p}
                onPress={() => setPalette(p as any)}
                style={[
                  styles.paletteOption,
                  palette === p && styles.paletteOptionActive,
                ]}
              >
                <Text style={[
                  styles.paletteLabel,
                  palette === p && styles.paletteLabelActive,
                ]}>
                  {p.charAt(0).toUpperCase() + p.slice(1)}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>
      </View>
    </View>
  );
}

function PrivacyTab() {
  return (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <Text style={styles.cardTitle}>Privacy</Text>
      </View>
      <View style={styles.cardContent}>
        <Text style={styles.sectionTitle}>Privacy Settings</Text>
        <Text style={styles.sectionDesc}>Control your data and privacy preferences.</Text>

        <View style={styles.prefList}>
          {[
            { id: 'profile', label: 'Profile Visibility', desc: 'Allow others to find your profile', default: true },
            { id: 'history', label: 'Watch History', desc: 'Allow others to see your watch history', default: false },
            { id: 'likes', label: 'Liked Videos', desc: 'Allow others to see your liked videos', default: false },
            { id: 'subscriptions', label: 'Subscriptions', desc: 'Allow others to see your subscriptions', default: false },
            { id: 'analytics', label: 'Analytics', desc: 'Allow analytics and personalized recommendations', default: true },
          ].map((pref) => (
            <View key={pref.id} style={styles.prefItem}>
              <View style={styles.prefInfo}>
                <Text style={styles.prefLabel}>{pref.label}</Text>
                <Text style={styles.prefDesc}>{pref.desc}</Text>
              </View>
              <Switch value={pref.default} onValueChange={() => {}} />
            </View>
          ))}
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Data Management</Text>
          <View style={styles.dataButtons}>
            <TouchableOpacity style={styles.outlineButton}>
              <Text style={styles.outlineButtonText}>Download Your Data</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.dangerButton}>
              <Text style={styles.dangerButtonText}>Delete Account</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </View>
  );
}

function Switch({ value, onValueChange }: { value: boolean; onValueChange: (v: boolean) => void }) {
  return (
    <TouchableOpacity onPress={() => onValueChange(!value)}>
      <View style={[
        styles.switchTrack,
        value && styles.switchTrackActive,
      ]}>
        <View style={[
          styles.switchThumb,
          value && styles.switchThumbActive,
        ]} />
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8fafc',
  },
  scrollContent: {
    padding: 16,
    paddingBottom: 100,
  },
  header: {
    marginBottom: 24,
  },
  headerTitle: {
    fontSize: 28,
    fontWeight: '700',
    color: '#0f172a',
  },
  tabs: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 24,
  },
  tab: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingHorizontal: 16,
    paddingVertical: 10,
    backgroundColor: '#f1f5f9',
    borderRadius: 10,
  },
  tabActive: {
    backgroundColor: '#6366f1',
  },
  tabLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#64748b',
  },
  tabLabelActive: {
    color: '#ffffff',
  },
  card: {
    backgroundColor: '#ffffff',
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#e2e8f0',
    overflow: 'hidden',
    marginBottom: 16,
  },
  cardHeader: {
    padding: 20,
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
  },
  cardTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#0f172a',
  },
  cardContent: {
    padding: 20,
  },
  profileHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
  },
  avatarLarge: {
    width: 80,
    height: 80,
    borderRadius: 40,
  },
  profileInfo: {
    flex: 1,
  },
  displayName: {
    fontSize: 22,
    fontWeight: '700',
    color: '#0f172a',
  },
  username: {
    fontSize: 14,
    color: '#64748b',
    marginTop: 4,
  },
  editButton: {
    paddingHorizontal: 20,
    paddingVertical: 10,
    backgroundColor: '#6366f1',
    borderRadius: 10,
  },
  editButtonText: {
    color: '#ffffff',
    fontWeight: '600',
  },
  profileFields: {
    paddingTop: 8,
  },
  field: {
    marginBottom: 16,
  },
  fieldLabel: {
    fontSize: 12,
    fontWeight: '600',
    color: '#64748b',
    marginBottom: 4,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  fieldValue: {
    fontSize: 16,
    color: '#0f172a',
  },
  bioSection: {
    marginTop: 8,
    paddingTop: 16,
    borderTopWidth: 1,
    borderTopColor: '#e2e8f0',
  },
  bioText: {
    fontSize: 16,
    color: '#0f172a',
    lineHeight: 24,
  },
  linkText: {
    fontSize: 16,
    color: '#6366f1',
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
  sectionDesc: {
    color: '#64748b',
    marginBottom: 16,
  },
  formGroup: {
    marginBottom: 20,
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
  saveButton: {
    paddingVertical: 14,
    backgroundColor: '#6366f1',
    borderRadius: 10,
    alignItems: 'center',
  },
  saveButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  outlineButton: {
    paddingVertical: 14,
    borderWidth: 1,
    borderColor: '#6366f1',
    borderRadius: 10,
    alignItems: 'center',
  },
  outlineButtonText: {
    color: '#6366f1',
    fontWeight: '600',
  },
  dangerButton: {
    paddingVertical: 14,
    backgroundColor: '#fef2f2',
    borderWidth: 1,
    borderColor: '#fecaca',
    borderRadius: 10,
    alignItems: 'center',
  },
  dangerButtonText: {
    color: '#ef4444',
    fontWeight: '600',
  },
  prefList: {
    gap: 8,
  },
  prefItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
  },
  prefInfo: {
    flex: 1,
  },
  prefLabel: {
    fontSize: 16,
    fontWeight: '500',
    color: '#0f172a',
  },
  prefDesc: {
    fontSize: 13,
    color: '#64748b',
    marginTop: 2,
  },
  dataButtons: {
    flexDirection: 'row',
    gap: 12,
    flexWrap: 'wrap',
  },
  themeOptions: {
    flexDirection: 'row',
    gap: 12,
  },
  themeOption: {
    flex: 1,
    paddingVertical: 20,
    paddingHorizontal: 16,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: '#e2e8f0',
    alignItems: 'center',
  },
  themeOptionActive: {
    borderColor: '#6366f1',
    backgroundColor: '#6366f10a',
  },
  themeIcon: {
    marginBottom: 8,
  },
  themeLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#64748b',
  },
  themeLabelActive: {
    color: '#6366f1',
  },
  paletteOptions: {
    flexDirection: 'row',
    gap: 12,
  },
  paletteOption: {
    flex: 1,
    paddingVertical: 16,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: '#e2e8f0',
    alignItems: 'center',
  },
  paletteOptionActive: {
    borderColor: '#6366f1',
  },
  paletteLabel: {
    fontSize: 13,
    fontWeight: '600',
    color: '#64748b',
    marginTop: 8,
  },
  paletteLabelActive: {
    color: '#6366f1',
  },
});
