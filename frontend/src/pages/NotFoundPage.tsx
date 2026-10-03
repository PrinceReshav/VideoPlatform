import { Link } from 'react-router-dom';
import { Home, Search, RotateCcw } from 'lucide-react';
import { Button } from '../components/ui/Button.tsx';

export function NotFoundPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-[var(--color-bg-primary)] px-4">
      <div className="text-center max-w-md">
        <div className="mb-6">
          <span className="text-9xl font-bold text-[var(--color-text-tertiary)]">404</span>
        </div>
        <h1 className="text-3xl font-bold text-[var(--color-text-primary)] mb-4">
          Page Not Found
        </h1>
        <p className="text-[var(--color-text-secondary)] mb-8">
          Sorry, we couldn't find the page you're looking for. It might have been moved or doesn't exist.
        </p>
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <Link to="/">
            <Button size="lg" className="gap-2">
              <Home className="w-4 h-4" />
              Go Home
            </Button>
          </Link>
          <Link to="/search">
            <Button variant="outline" size="lg" className="gap-2">
              <Search className="w-4 h-4" />
              Search Videos
            </Button>
          </Link>
          <button
            onClick={() => window.history.back()}
            className="px-4 py-2 text-sm font-medium text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] transition-colors"
          >
            <RotateCcw className="w-4 h-4 inline mr-1" />
            Go Back
          </button>
        </div>
      </div>
    </div>
  );
}