import { ReactNode } from 'react';
import { Outlet } from 'react-router-dom';

export function AuthLayout({ children }: { children?: ReactNode }) {
  return (
    <div className="min-h-screen bg-[var(--color-bg-primary)] flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {children ? (
          <>
            {children}
            <Outlet />
          </>
        ) : (
          <Outlet />
        )}
      </div>
    </div>
  );
}