import { forwardRef, ButtonHTMLAttributes } from 'react';
import { clsx } from 'clsx';

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger' | 'warning';
  size?: 'sm' | 'md' | 'lg' | 'icon';
  loading?: boolean;
  fullWidth?: boolean;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = 'primary', size = 'md', loading, fullWidth, disabled, children, ...props }, ref) => {
    const baseStyles = `
      inline-flex items-center justify-center font-medium transition-all
      focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2
      disabled:opacity-50 disabled:cursor-not-allowed
      ${fullWidth ? 'w-full' : ''}
    `;

    const variants = {
      primary: 'bg-[var(--color-primary)] text-white hover:bg-[var(--color-primary-hover)] focus-visible:ring-[var(--color-primary)]',
      secondary: 'bg-[var(--color-secondary)] text-white hover:bg-[var(--color-secondary)]/90 focus-visible:ring-[var(--color-secondary)]',
      outline: 'border border-[var(--color-border)] bg-transparent hover:bg-[var(--color-bg-hover)] focus-visible:ring-[var(--color-primary)]',
      ghost: 'bg-transparent hover:bg-[var(--color-bg-hover)] focus-visible:ring-[var(--color-primary)]',
      danger: 'bg-[var(--color-error)] text-white hover:bg-[var(--color-error)]/90 focus-visible:ring-[var(--color-error)]',
      warning: 'bg-[var(--color-warning)] text-white hover:opacity-90 focus-visible:ring-[var(--color-warning)]',
    };

    const sizes = {
      sm: 'px-3 py-1.5 text-sm gap-1.5 rounded-md',
      md: 'px-4 py-2 text-base gap-2 rounded-lg',
      lg: 'px-6 py-3 text-lg gap-2 rounded-xl',
      icon: 'p-2 rounded-lg',
    };

    return (
      <button
        ref={ref}
        className={clsx(baseStyles, variants[variant], sizes[size], className)}
        disabled={disabled || loading}
        {...props}
      >
        {loading && (
          <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
          </svg>
        )}
        {children}
      </button>
    );
  }
);

Button.displayName = 'Button';
