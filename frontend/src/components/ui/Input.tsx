import React, { forwardRef, InputHTMLAttributes, TextareaHTMLAttributes } from 'react';
import { clsx } from 'clsx';

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  as?: 'input' | 'textarea';
  rows?: number;
  label?: string;
  error?: string;
  hint?: string;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, label, error, hint, leftIcon, rightIcon, id, as='input', rows, ...props }, ref) => {
    const inputId = id || `input-${Math.random().toString(36).slice(2, 9)}`;
    const hintId = hint ? `${inputId}-hint` : undefined;
    const errorId = error ? `${inputId}-error` : undefined;

    return (
      <div className="w-full">
        {label && (
          <label htmlFor={inputId} className="block text-sm font-medium text-[var(--color-text-primary)] mb-1.5">
            {label}
          </label>
        )}
        <div className="relative">
          {leftIcon && (
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-[var(--color-text-tertiary)]">
              {leftIcon}
            </div>
          )}
          {as === 'textarea' ? (
            <textarea id={inputId} rows={rows} className={clsx('w-full bg-[var(--color-surface)] border border-[var(--color-border)] rounded-lg text-[var(--color-text-primary)] placeholder-[var(--color-text-tertiary)] transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)] px-4 py-2.5 text-base resize-y', className)} aria-describedby={clsx(hintId, errorId)} aria-invalid={!!error} ref={ref as any} {...props as any} />
          ) : (
            <input id={inputId} className={clsx('w-full bg-[var(--color-surface)] border border-[var(--color-border)] rounded-lg text-[var(--color-text-primary)] placeholder-[var(--color-text-tertiary)] transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]', leftIcon ? 'pl-10' : 'px-4', rightIcon ? 'pr-10' : 'px-4', 'py-2.5 text-base', className)} aria-describedby={clsx(hintId, errorId)} aria-invalid={!!error} ref={ref} {...props} />
          )}
          {rightIcon && (
            <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none text-[var(--color-text-tertiary)]">
              {rightIcon}
            </div>
          )}
        </div>
        {hint && !error && (
          <p id={hintId} className="mt-1.5 text-sm text-[var(--color-text-tertiary)]">
            {hint}
          </p>
        )}
        {error && (
          <p id={errorId} className="mt-1.5 text-sm text-[var(--color-error)]" role="alert">
            {error}
          </p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';

export interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
  hint?: string;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, label, error, hint, id, ...props }, ref) => {
    const textareaId = id || `textarea-${Math.random().toString(36).slice(2, 9)}`;
    const hintId = hint ? `${textareaId}-hint` : undefined;
    const errorId = error ? `${textareaId}-error` : undefined;

    return (
      <div className="w-full">
        {label && (
          <label htmlFor={textareaId} className="block text-sm font-medium text-[var(--color-text-primary)] mb-1.5">
            {label}
          </label>
        )}
        <textarea
          ref={ref}
          id={textareaId}
          className={clsx(
            'w-full bg-[var(--color-surface)] border border-[var(--color-border)] rounded-lg',
            'text-[var(--color-text-primary)] placeholder-[var(--color-text-tertiary)]',
            'transition-all duration-200 resize-y min-h-[100px]',
            'hover:border-[var(--color-border-strong)]',
            'focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)] focus:border-transparent',
            'disabled:bg-[var(--color-bg-tertiary)] disabled:cursor-not-allowed',
            'aria-invalid="true" border-[var(--color-error)] focus:ring-[var(--color-error)]',
            'px-4 py-2.5 text-base',
            className
          )}
          aria-describedby={clsx(hintId, errorId)}
          aria-invalid={!!error}
          {...props}
        />
        {hint && !error && (
          <p id={hintId} className="mt-1.5 text-sm text-[var(--color-text-tertiary)]">
            {hint}
          </p>
        )}
        {error && (
          <p id={errorId} className="mt-1.5 text-sm text-[var(--color-error)]" role="alert">
            {error}
          </p>
        )}
      </div>
    );
  }
);

Textarea.displayName = 'Textarea';