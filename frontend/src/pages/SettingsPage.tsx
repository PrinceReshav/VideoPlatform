import { Download, Eye, EyeOff, Monitor, Trash2, Lock } from 'lucide-react';
import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { User, Settings, Bell, Palette, Moon, Sun, Shield, LogOut, Key, Mail, Save, Camera } from 'lucide-react';
import { Card } from '../components/ui/Card.tsx';
import { Button } from '../components/ui/Button.tsx';
import { Input } from '../components/ui/Input.tsx';
import { Avatar } from '../components/ui/Avatar.tsx';
import { Select } from '../components/ui/Select.tsx';
import { authApi } from '../services/api/authApi.ts';
import { useAuth } from '../app/providers/AuthProvider.tsx';
import { useTheme } from '../app/providers/ThemeProvider.tsx';
import { useThemeStore } from '../store/themeStore';
import { themes, palettes } from '../components/layout/AppearanceMenu';

export function SettingsPage() {
  const { user, isAuthenticated, logout } = useAuth();
  const { theme, resolvedTheme, setTheme, palette, setPalette } = useTheme();
  const [activeTab, setActiveTab] = React.useState<'account' | 'security' | 'notifications' | 'appearance' | 'privacy'>('account');
  const [avatarPreview, setAvatarPreview] = React.useState<string | null>(null);

  const { data: profile } = useQuery({
    queryKey: ['userProfile', user?.id],
    queryFn: () => authApi.getCurrentUser(),
    enabled: isAuthenticated,
  });

  return (
    <div className="max-w-4xl mx-auto p-4 space-y-6">
      <h1 className="text-3xl font-bold text-[var(--color-text-primary)]">Settings</h1>

      <div className="grid lg:grid-cols-4 gap-6">
        {/* Sidebar */}
        <div className="lg:col-span-1">
          <Card className="sticky top-24">
            <div className="p-5 text-center border-b border-[var(--color-border)]">
              <Avatar src={profile?.avatarUrl || user?.avatarUrl} alt={profile?.displayName || user?.displayName} size="xl" />
              <h2 className="mt-3 font-semibold text-[var(--color-text-primary)]">{profile?.displayName || user?.displayName}</h2>
              <p className="text-sm text-[var(--color-text-secondary)]">@{profile?.username || user?.username}</p>
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-[var(--color-primary)]/10 text-[var(--color-primary)] mt-2">
                {profile?.roles?.includes('CREATOR') ? 'Creator' : 'Viewer'}
              </span>
            </div>
            <nav className="p-2 space-y-1">
              {[
                { id: 'account', label: 'Account', icon: User },
                { id: 'security', label: 'Security', icon: Shield },
                { id: 'notifications', label: 'Notifications', icon: Bell },
                { id: 'appearance', label: 'Appearance', icon: Palette },
                { id: 'privacy', label: 'Privacy', icon: Shield },
              ].map((tab) => (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id as any)}
                  className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                    activeTab === tab.id
                      ? 'bg-[var(--color-primary)]/10 text-[var(--color-primary)]'
                      : 'text-[var(--color-text-secondary)] hover:bg-[var(--color-bg-hover)] hover:text-[var(--color-text-primary)]'
                  }`}
                >
                  <tab.icon className="w-4 h-4" />
                  {tab.label}
                </button>
              ))}
            </nav>
          </Card>
        </div>

        {/* Content */}
        <div className="lg:col-span-3 space-y-6">
          {activeTab === 'account' && <AccountTab user={profile || user} />}
          {activeTab === 'security' && <SecurityTab />}
          {activeTab === 'notifications' && <NotificationsTab />}
          {activeTab === 'appearance' && <AppearanceTab />}
          {activeTab === 'privacy' && <PrivacyTab />}
        </div>
      </div>
    </div>
  );
}

function AccountTab({ user }: { user: any }) {
  const [isEditing, setIsEditing] = React.useState(false);
  const [formData, setFormData] = React.useState({
    displayName: user?.displayName || '',
    email: user?.email || '',
    bio: user?.bio || '',
    websiteUrl: user?.websiteUrl || '',
    location: user?.location || '',
  });
  const [avatarPreview, setAvatarPreview] = React.useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsEditing(false);
  };

  if (isEditing) {
    return (
      <Card>
        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          <div className="flex items-center gap-4">
            <Avatar src={avatarPreview || user?.avatarUrl} alt={user?.displayName} size="xl" />
            <div>
              <label className="block text-sm font-medium text-[var(--color-text-primary)] mb-1">Avatar</label>
              <input type="file" accept="image/*" onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) {
                  const reader = new FileReader();
                  reader.onload = () => setAvatarPreview(reader.result as string);
                  reader.readAsDataURL(file);
                }
              }} className="text-sm" />
            </div>
          </div>
          <Input label="Display Name" value={formData.displayName} onChange={(e) => setFormData({...formData, displayName: e.target.value})} />
          <Input label="Email" value={formData.email} onChange={(e) => setFormData({...formData, email: e.target.value})} type="email" />
          <Input label="Bio" value={formData.bio} onChange={(e) => setFormData({...formData, bio: e.target.value})} as="textarea" rows={3} placeholder="Tell us about yourself..." />
          <Input label="Website" value={formData.websiteUrl} onChange={(e) => setFormData({...formData, websiteUrl: e.target.value})} placeholder="https://example.com" />
          <Input label="Location" value={formData.location} onChange={(e) => setFormData({...formData, location: e.target.value})} placeholder="City, Country" />
          <div className="flex gap-3 pt-4">
            <Button type="submit">Save Changes</Button>
            <Button type="button" variant="outline" onClick={() => setIsEditing(false)}>Cancel</Button>
          </div>
        </form>
      </Card>
    );
  }

  return (
    <Card>
      <div className="p-5 space-y-6">
        <div className="flex items-center gap-4">
          <Avatar src={user?.avatarUrl} alt={user?.displayName} size="xl" />
          <div>
            <h2 className="text-2xl font-bold text-[var(--color-text-primary)]">{user?.displayName}</h2>
            <p className="text-[var(--color-text-secondary)]">@{user?.username}</p>
          </div>
          <Button variant="outline" onClick={() => setIsEditing(true)}>
            Edit Profile
          </Button>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="block text-sm font-medium text-[var(--color-text-secondary)] mb-1">Email</label>
            <p className="text-[var(--color-text-primary)]">{user?.email}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-[var(--color-text-secondary)] mb-1">Joined</label>
            <p className="text-[var(--color-text-primary)]">January 2024</p>
          </div>
        </div>

        {user?.bio && (
          <div className="pt-4 border-t border-[var(--color-border)]">
            <label className="block text-sm font-medium text-[var(--color-text-secondary)] mb-1">Bio</label>
            <p className="text-[var(--color-text-primary)]">{user?.bio}</p>
          </div>
        )}
        {user?.websiteUrl && (
          <div className="pt-4 border-t border-[var(--color-border)]">
            <label className="block text-sm font-medium text-[var(--color-text-secondary)] mb-1">Website</label>
            <a href={user?.websiteUrl} target="_blank" rel="noopener noreferrer" className="text-[var(--color-primary)] hover:underline">
              {user?.websiteUrl}
            </a>
          </div>
        )}
        {user?.location && (
          <div className="pt-4 border-t border-[var(--color-border)]">
            <label className="block text-sm font-medium text-[var(--color-text-secondary)] mb-1">Location</label>
            <p className="text-[var(--color-text-primary)]">{user?.location}</p>
          </div>
        )}
      </div>
    </Card>
  );
}

function SecurityTab() {
  const [currentPassword, setCurrentPassword] = React.useState('');
  const [newPassword, setNewPassword] = React.useState('');
  const [confirmPassword, setConfirmPassword] = React.useState('');
  const [showPassword, setShowPassword] = React.useState(false);

  return (
    <Card className="p-5 space-y-6">
      <div>
        <h2 className="text-lg font-semibold text-[var(--color-text-primary)] mb-4">Change Password</h2>
        <form className="space-y-4 max-w-md">
          <div className="relative">
            <Input
              label="Current Password"
              type={showPassword ? 'text' : 'password'}
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              required
              leftIcon={<Lock className="w-5 h-5" />}
              rightIcon={
                <button type="button" onClick={() => setShowPassword(!showPassword)}>
                  {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              }
            />
          </div>
          <Input
            label="New Password"
            type={showPassword ? 'text' : 'password'}
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            required
            leftIcon={<Lock className="w-5 h-5" />}
            hint="At least 8 characters with uppercase, lowercase, number, and special character"
          />
          <Input
            label="Confirm New Password"
            type={showPassword ? 'text' : 'password'}
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
            leftIcon={<Lock className="w-5 h-5" />}
          />
          <Button type="submit">Update Password</Button>
        </form>
      </div>

      <div className="pt-6 border-t border-[var(--color-border)]">
        <h3 className="text-lg font-semibold text-[var(--color-text-primary)] mb-4">Two-Factor Authentication</h3>
        <p className="text-[var(--color-text-secondary)] mb-4">Add an extra layer of security to your account.</p>
        <Button variant="outline">
          <Shield className="w-4 h-4" />
          Enable 2FA
        </Button>
      </div>

      <div className="pt-6 border-t border-[var(--color-border)]">
        <h3 className="text-lg font-semibold text-[var(--color-text-primary)] mb-4">Active Sessions</h3>
        <p className="text-[var(--color-text-secondary)] mb-4">Manage devices logged into your account.</p>
        <Button variant="outline">
          <LogOut className="w-4 h-4" />
          Log out of all sessions
        </Button>
      </div>
    </Card>
  );
}

function NotificationsTab() {
  return (
    <Card className="p-5 space-y-6">
      <h2 className="text-lg font-semibold text-[var(--color-text-primary)]">Notification Preferences</h2>
      <p className="text-[var(--color-text-secondary)]">Choose what notifications you want to receive.</p>

      <div className="space-y-4">
        {[
          { id: 'newVideo', label: 'New videos from subscriptions', default: true },
          { id: 'comments', label: 'New comments on your videos', default: true },
          { id: 'replies', label: 'Replies to your comments', default: true },
          { id: 'likes', label: 'Likes on your videos', default: true },
          { id: 'subscribers', label: 'New subscribers', default: true },
          { id: 'milestones', label: 'Channel milestones', default: true },
          { id: 'announcements', label: 'Platform announcements', default: true },
          { id: 'email', label: 'Email notifications', default: false },
        ].map((pref) => (
          <div key={pref.id} className="flex items-center justify-between py-3 border-b border-[var(--color-border)] last:border-0">
            <div className="flex items-center gap-3">
              <Bell className="w-5 h-5 text-[var(--color-text-tertiary)]" />
              <div>
                <p className="font-medium text-[var(--color-text-primary)]">{pref.label}</p>
              </div>
            </div>
            <Switch checked={pref.default} onChange={() => {}} />
          </div>
        ))}
      </div>
    </Card>
  );
}

function AppearanceTab() {
  const { theme, setTheme, palette, setPalette } = useTheme();

  const palettes = [
    { id: 'default', name: 'Classic Red', primary: '#ff0033', secondary: '#ff5c77', label: 'Familiar & bold' },
    { id: 'red', name: 'Crimson', primary: '#ff0033', secondary: '#ff5c77', label: 'Bold & energetic' },
    { id: 'violet', name: 'Violet', primary: '#7c3aed', secondary: '#a855f7', label: 'Creative & premium' },
    { id: 'rose', name: 'Rose', primary: '#e11d48', secondary: '#fb7185', label: 'Warm & expressive' },
    { id: 'ocean', name: 'Ocean', primary: '#0284c7', secondary: '#06b6d4', label: 'Clean & calm' },
    { id: 'cyan', name: 'Cyan', primary: '#0891b2', secondary: '#22d3ee', label: 'Fresh & modern' },
    { id: 'emerald', name: 'Emerald', primary: '#059669', secondary: '#10b981', label: 'Natural & focused' },
    { id: 'green', name: 'Green', primary: '#16a34a', secondary: '#22c55e', label: 'Vibrant & positive' },
    { id: 'amber', name: 'Amber', primary: '#d97706', secondary: '#f59e0b', label: 'Warm & confident' },
    { id: 'orange', name: 'Orange', primary: '#ea580c', secondary: '#f97316', label: 'Energetic & social' },
    { id: 'pink', name: 'Pink', primary: '#db2777', secondary: '#ec4899', label: 'Expressive & playful' },
    { id: 'slate', name: 'Slate', primary: '#475569', secondary: '#64748b', label: 'Minimal & professional' },
  ] as const;

  return (
    <Card className="p-5 space-y-7">
      <div>
        <div className="flex items-start justify-between gap-4 mb-4">
          <div>
            <h2 className="text-lg font-semibold text-[var(--color-text-primary)]">Appearance</h2>
            <p className="text-sm text-[var(--color-text-secondary)] mt-1">Personalize the platform with your preferred mode and accent color.</p>
          </div>
          <span className="hidden sm:inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-[var(--color-primary)]/10 text-[var(--color-primary)]">Live preview</span>
        </div>

        <div className="grid grid-cols-3 gap-3">
          {themes.map((item) => {
            const Icon = item.icon;
            const selected = theme === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => setTheme(item.id)}
                className={`rounded-xl border-2 p-3 text-left transition-all ${selected ? 'border-[var(--color-primary)] bg-[var(--color-primary)]/5 shadow-sm' : 'border-[var(--color-border)] hover:border-[var(--color-border-strong)]'}`}
              >
                <div className="h-14 rounded-lg mb-3 border border-[var(--color-border)]" style={{ background: item.preview }} />
                <div className="flex items-center gap-2">
                  <Icon className="w-4 h-4" />
                  <span className="font-semibold text-sm text-[var(--color-text-primary)]">{item.label}</span>
                </div>
              </button>
            );
          })}
        </div>
      </div>

      <div className="pt-6 border-t border-[var(--color-border)]">
        <div className="mb-4">
          <h2 className="text-lg font-semibold text-[var(--color-text-primary)]">Accent color</h2>
          <p className="text-sm text-[var(--color-text-secondary)] mt-1">Your choice updates buttons, links, active navigation, highlights and player accents throughout the app.</p>
        </div>
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
          {palettes.map((item) => {
            const selected = palette === item.id;
            return (
              <button
                key={item.id}
                type="button"
                aria-pressed={selected}
                onClick={() => setPalette(item.id)}
                className={`palette-swatch text-left ${selected ? 'selected' : ''}`}
              >
                <div className="swatch-strip" style={{ background: `linear-gradient(135deg, ${item.primary}, ${item.secondary})` }} />
                <div className="flex items-center gap-3 p-3">
                  <span className="swatch-dot" style={{ background: item.primary }} />
                  <span className="min-w-0">
                    <span className="block text-sm font-semibold text-[var(--color-text-primary)]">{item.name}</span>
                    <span className="block text-xs text-[var(--color-text-tertiary)] truncate">{item.label}</span>
                  </span>
                </div>
                {selected && <span className="absolute top-2 right-2 w-5 h-5 rounded-full bg-white/90 text-[var(--color-primary)] flex items-center justify-center text-xs font-bold">✓</span>}
              </button>
            );
          })}
        </div>
      </div>
    </Card>
  );
}

function PrivacyTab() {
  return (
    <Card className="p-5 space-y-6">
      <h2 className="text-lg font-semibold text-[var(--color-text-primary)]">Privacy Settings</h2>
      <p className="text-[var(--color-text-secondary)]">Control your data and privacy preferences.</p>

      <div className="space-y-4">
        <div className="flex items-center justify-between py-3 border-b border-[var(--color-border)]">
          <div>
            <p className="font-medium text-[var(--color-text-primary)]">Profile Visibility</p>
            <p className="text-sm text-[var(--color-text-secondary)]">Allow others to find your profile and channel</p>
          </div>
          <Switch checked={true} onChange={() => {}} />
        </div>
        <div className="flex items-center justify-between py-3 border-b border-[var(--color-border)]">
          <div>
            <p className="font-medium text-[var(--color-text-primary)]">Watch History</p>
            <p className="text-sm text-[var(--color-text-secondary)]">Allow others to see your watch history</p>
          </div>
          <Switch checked={false} onChange={() => {}} />
        </div>
        <div className="flex items-center justify-between py-3 border-b border-[var(--color-border)]">
          <div>
            <p className="font-medium text-[var(--color-text-primary)]">Liked Videos</p>
            <p className="text-sm text-[var(--color-text-secondary)]">Allow others to see your liked videos</p>
          </div>
          <Switch checked={false} onChange={() => {}} />
        </div>
        <div className="flex items-center justify-between py-3 border-b border-[var(--color-border)]">
          <div>
            <p className="font-medium text-[var(--color-text-primary)]">Subscriptions</p>
            <p className="text-sm text-[var(--color-text-secondary)]">Allow others to see your subscriptions</p>
          </div>
          <Switch checked={false} onChange={() => {}} />
        </div>
        <div className="flex items-center justify-between py-3 border-b border-[var(--color-border)]">
          <div>
            <p className="font-medium text-[var(--color-text-primary)]">Data Collection</p>
            <p className="text-sm text-[var(--color-text-secondary)]">Allow analytics and personalized recommendations</p>
          </div>
          <Switch checked={true} onChange={() => {}} />
        </div>
      </div>

      <div className="pt-6 border-t border-[var(--color-border)]">
        <h3 className="text-lg font-semibold text-[var(--color-text-primary)] mb-4">Data Management</h3>
        <div className="flex flex-wrap gap-3">
          <Button variant="outline">
            <Download className="w-4 h-4" />
            Download Your Data
          </Button>
          <Button variant="outline">
            <Trash2 className="w-4 h-4 text-[var(--color-error)]" />
            Delete Account
          </Button>
        </div>
      </div>
    </Card>
  );
}

function Switch({ checked, onChange }: { checked: boolean; onChange: () => void }) {
  return (
    <button
      onClick={onChange}
      className={`relative w-11 h-6 rounded-full transition-colors ${checked ? 'bg-[var(--color-primary)]' : 'bg-[var(--color-border)]'}`}
      role="switch"
      aria-checked={checked}
    >
      <span className={`absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full transition-transform ${checked ? 'translate-x-5' : ''}`} />
    </button>
  );
}