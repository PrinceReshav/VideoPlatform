# Video Platform - Test Suite

## Backend Tests

### Unit Tests
- AuthService: register, login, refresh, logout, password reset
- VideoService: CRUD, publish/unpublish, access control
- UploadService: session creation, chunk upload, completion, cancellation
- EngagementService: likes, comments, views, watch history
- SubscriptionService: subscribe/unsubscribe, feed generation
- PlaylistService: CRUD, video management, reordering
- SearchService: full-text search, autocomplete, filters

### Integration Tests
- Auth flow: register → login → refresh → logout
- Upload flow: create session → upload chunks → complete → transcode → publish
- Video lifecycle: create → upload → transcode → publish → search → view
- Engagement: like → comment → view → history
- Subscription: subscribe → feed → unsubscribe

### Security Tests
- JWT token validation and rotation
- Role-based access control (RBAC)
- Ownership validation
- Rate limiting
- SQL injection prevention
- XSS protection

## Frontend Tests

### Unit Tests (Vitest + React Testing Library)
- Components: Button, Input, Card, Avatar, VideoPlayer, VideoCard
- Hooks: useAuth, useTheme, useVideoPlayer
- Stores: authStore, themeStore, uiStore
- Services: http, authApi, videoApi, engagementApi

### E2E Tests (Playwright)
- Auth: register, login, logout, password reset
- Video: browse, search, play, quality selection
- Creator: upload, edit, publish, analytics
- Engagement: like, comment, subscribe, playlist
- Admin: user management, video moderation, reports

## Mobile Tests (Jest + React Native Testing Library)
- Screens: Home, Search, Watch, Library, Profile, Settings
- Creator: Dashboard, Videos, Upload
- Navigation: tabs, stack, deep links
- Services: http, authApi, videoApi
- Offline: upload queue, cached data

## CI/CD Pipeline (GitHub Actions)

### Workflow: CI
- Trigger: push to main, PR to main
- Jobs:
  1. Lint: backend (checkstyle), frontend (eslint), mobile (eslint)
  2. Typecheck: TypeScript strict mode
  3. Test: backend (mvn test), frontend (npm test), mobile (npm test)
  4. Build: Docker images for all services
  5. Security: dependency scan, container scan

### Workflow: CD (on main branch)
- Deploy to staging
- Run smoke tests
- Deploy to production (manual approval)

## Test Commands

```bash
# Backend
cd backend
mvn test                    # All tests
mvn test -pl auth-service   # Specific service
mvn verify                  # Integration tests

# Frontend
cd frontend
npm test                    # Unit tests
npm run test:e2e           # E2E tests

# Mobile
cd mobile
npm test                    # Unit tests
npm run test:e2e           # Detox E2E tests
```

## Test Coverage Targets
- Backend: >80% line coverage
- Frontend: >80% line coverage  
- Mobile: >70% line coverage
- Critical paths: 100% coverage