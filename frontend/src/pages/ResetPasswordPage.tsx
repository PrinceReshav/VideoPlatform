import React from "react";
import { useState } from 'react';
import { useSearchParams, Link, useNavigate } from 'react-router-dom';
import { Lock, ArrowLeft, Check } from 'lucide-react';
import { Card } from '../components/ui/Card.tsx';
import { Button } from '../components/ui/Button.tsx';
import { Input } from '../components/ui/Input.tsx';
import { authApi } from '../services/api/authApi';

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');

  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }
    if (password.length < 8) {
      setError('Password must be at least 8 characters');
      return;
    }
    if (!token) {
      setError('Invalid or missing reset token');
      return;
    }

    setIsLoading(true);
    setError(null);
    try {
      await authApi.resetPassword(token, password);
      setSuccess(true);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to reset password');
    } finally {
      setIsLoading(false);
    }
  };

  if (success) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[var(--color-bg-primary)] px-4">
        <Card className="w-full max-w-md p-8 text-center">
          <div className="w-16 h-16 mx-auto mb-4 bg-[var(--color-success)]/10 rounded-full flex items-center justify-center">
            <Check className="w-8 h-8 text-[var(--color-success)]" />
          </div>
          <h1 className="text-2xl font-bold text-[var(--color-text-primary)] mb-2">Password Reset</h1>
          <p className="text-[var(--color-text-secondary)] mb-6">
            Your password has been successfully updated.
          </p>
          <Link to="/login">
            <Button>Go to Login</Button>
          </Link>
        </Card>
      </div>
    );
  }

  if (!token) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[var(--color-bg-primary)] px-4">
        <Card className="w-full max-w-md p-8 text-center">
          <div className="w-16 h-16 mx-auto mb-4 bg-[var(--color-error)]/10 rounded-full flex items-center justify-center">
            <Lock className="w-8 h-8 text-[var(--color-error)]" />
          </div>
          <h1 className="text-2xl font-bold text-[var(--color-text-primary)] mb-2">Invalid Reset Link</h1>
          <p className="text-[var(--color-text-secondary)] mb-6">
            This password reset link is invalid or has expired.
          </p>
          <Link to="/forgot-password">
            <Button>Request New Link</Button>
          </Link>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-[var(--color-bg-primary)] px-4">
      <Card className="w-full max-w-md p-8">
        <Link to="/login" className="inline-flex items-center gap-2 text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] mb-6">
          <ArrowLeft className="w-4 h-4" />
          Back to Login
        </Link>
        <h1 className="text-2xl font-bold text-[var(--color-text-primary)] mb-2">Reset Password</h1>
        <p className="text-[var(--color-text-secondary)] mb-6">Enter your new password below</p>

        {error && (
          <div className="mb-4 p-3 bg-[var(--color-error)]/10 text-[var(--color-error)] rounded-lg text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-[var(--color-text-primary)] mb-1">New Password</label>
            <Input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter new password"
              required
              minLength={8}
              leftIcon={<Lock className="w-4 h-4" />}
            />
            <p className="text-xs text-[var(--color-text-tertiary)] mt-1">Must be at least 8 characters</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-[var(--color-text-primary)] mb-1">Confirm Password</label>
            <Input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="Confirm new password"
              required
              leftIcon={<Lock className="w-4 h-4" />}
            />
          </div>

          <Button type="submit" className="w-full" loading={isLoading}>
            Reset Password
          </Button>
        </form>
      </Card>
    </div>
  );
}