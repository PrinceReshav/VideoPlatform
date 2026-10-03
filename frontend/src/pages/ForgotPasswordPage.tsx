import React from "react";
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Mail, ArrowLeft, Send } from 'lucide-react';
import { Card } from '../components/ui/Card.tsx';
import { Button } from '../components/ui/Button.tsx';
import { Input } from '../components/ui/Input.tsx';
import { authApi } from '../services/api/authApi';

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);
    try {
      await authApi.forgotPassword(email.trim());
      setSuccess(true);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to send reset email');
    } finally {
      setIsLoading(false);
    }
  };

  if (success) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[var(--color-bg-primary)] px-4">
        <Card className="w-full max-w-md p-8 text-center">
          <div className="w-16 h-16 mx-auto mb-4 bg-[var(--color-success)]/10 rounded-full flex items-center justify-center">
            <Mail className="w-8 h-8 text-[var(--color-success)]" />
          </div>
          <h1 className="text-2xl font-bold text-[var(--color-text-primary)] mb-2">Check Your Email</h1>
          <p className="text-[var(--color-text-secondary)] mb-6">
            If an account with that email exists, we've sent password reset instructions.
          </p>
          <Link to="/login">
            <Button>Back to Login</Button>
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
        <h1 className="text-2xl font-bold text-[var(--color-text-primary)] mb-2">Forgot Password</h1>
        <p className="text-[var(--color-text-secondary)] mb-6">Enter your email and we'll send you reset instructions</p>

        {error && (
          <div className="mb-4 p-3 bg-[var(--color-error)]/10 text-[var(--color-error)] rounded-lg text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-[var(--color-text-primary)] mb-1">Email</label>
            <Input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              required
              leftIcon={<Mail className="w-4 h-4" />}
            />
          </div>

          <Button type="submit" className="w-full" loading={isLoading}>
            <Send className="w-4 h-4" />
            Send Reset Link
          </Button>
        </form>
      </Card>
    </div>
  );
}