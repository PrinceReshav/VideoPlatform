
## Live TV V10

Live TV now preserves the complete IPTV-org channel/stream universe, performs candidate-level health checks, uses direct HLS before the compatibility proxy, and can fall back to an official embeddable YouTube Live broadcast when IPTV candidates are exhausted. See `LIVE_TV_V10_RELEASE_NOTES.md`.

# Video Platform

A production-grade, scalable full-stack video streaming and creator platform built with modern technologies.

## Architecture Overview

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   React     │     │  React      │     │   Admin     │
│   Web App   │     │  Native     │     │   Panel     │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                           ▼
              ┌────────────────────────┐
              │    API Gateway         │
              │  (Spring Cloud Gateway)│
              └───────────┬────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        ▼                 ▼                 ▼
   ┌─────────┐       ┌─────────┐       ┌─────────┐
   │  Auth   │       │  User   │       │ Video   │
   │ Service │       │ Service │       │ Service │
   └────┬────┘       └────┬────┘       └────┬────┘
        │                 │                 │
        ▼                 ▼                 ▼
   ┌─────────────────────────────────────────────┐
   │     H2 (DEV) / PostgreSQL (UAT)             │
   │  (Logical schemas per service)              │
   └─────────────────────────────────────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        ▼                 ▼                 ▼
   ┌─────────┐       ┌─────────┐       ┌─────────┐
   │  Kafka  │       │ Redis   │       │MinIO/S3 │
   │(Events) │       │(Cache)  │       │(Storage)│
   └────┬────┘       └────┬────┘       └────┬────┘
        │                 │                 │
        ▼                 ▼                 ▼
   ┌─────────────────────────────────────────────┐
   │         Transcoding Service (FFmpeg)        │
   └─────────────────────────────────────────────┘
                          │
                          ▼
   ┌─────────────────────────────────────────────┐
   │         Elasticsearch (Search)              │
   └─────────────────────────────────────────────┘
```

## Technology Stack

### Backend
- **Java 21+** with **Spring Boot 3.x**
- **Spring Cloud Gateway** for API Gateway
- **Spring Security** with JWT authentication
- **Spring Data JPA** with Hibernate
- **H2 (DEV)** shared file database; **PostgreSQL 16 (UAT)** with logical schemas per service
- **Redis 7** (caching, sessions, rate limiting)
- **Apache Kafka** (KRaft mode, event-driven architecture)
- **Elasticsearch 8** (full-text search, autocomplete)
- **MinIO** (S3-compatible object storage)
- **FFmpeg** (video transcoding, HLS generation)
- **Docker & Docker Compose** for local development

### Frontend (Web)
- **React 18** with **TypeScript**
- **Vite** for fast development
- **React Router v6** for routing
- **TanStack Query** for server state
- **Zustand** for client state
- **Axios** for HTTP client
- **CSS Modules** with design tokens

### Mobile
- **React Native** with **Expo**
- **TypeScript**
- **React Navigation** for routing
- **Expo AV** for video playback
- **Expo SecureStore** for token storage
- Shared API client with web

## Quick Start

### Prerequisites
- Docker 24+ and Docker Compose 2+
- 8GB+ RAM available for containers
- 20GB+ free disk space

### 1. Clone and Configure

```bash
git clone <repository-url>
cd video-platform

# Copy environment template and customize
cp .env.example .env
# Edit .env with your settings (JWT secret, passwords, etc.)
```

### 2. Start Infrastructure

```bash
# Start all infrastructure services
make up

# Or use the script directly
./scripts/local-dev.sh up
```

This starts:
- PostgreSQL (port 5432)
- Redis (port 6379)
- Kafka (port 9092)
- Elasticsearch (port 9200)
- MinIO (port 9000/9001)
- API Gateway (port 8080)
- Kafka UI (port 8095)

### 3. Verify Health

```bash
make health
# or
./scripts/health-check.sh
```

### 4. Access Services

| Service | URL | Credentials |
|---------|-----|-------------|
| API Gateway | http://localhost:8080 | - |
| Kafka UI | http://localhost:8095 | - |
| MinIO Console | http://localhost:9001 | minioadmin / minioadmin123 |
| Elasticsearch | http://localhost:9200 | - |
| H2 DEV | `../../data/video-platform-dev` | file-based shared H2 |
| PostgreSQL UAT | localhost:5432 | configured through `UAT_POSTGRES_*` |

## Development Workflow

### Start Development Environment
```bash
make dev  # Starts infrastructure + runs health checks
```

### View Logs
```bash
make logs                    # All services
make logs SERVICE=postgres   # Specific service
```

### Check Status
```bash
make status
```

### Run Tests
```bash
make test
```

### Generate API Client (after backend services have OpenAPI endpoints)
```bash
make generate-api
```

### Seed Database
```bash
make seed
```

### Clean Everything
```bash
make clean
```

## Project Structure

```
video-platform/
├── backend/                    # Spring Boot microservices
│   ├── api-gateway/           # Spring Cloud Gateway
│   ├── auth-service/          # Authentication & authorization
│   ├── user-creator-service/  # Users, creators, channels
│   ├── video-service/         # Video metadata & lifecycle
│   ├── upload-service/        # Chunked/resumable uploads
│   ├── transcoding-service/   # FFmpeg video processing
│   ├── search-service/        # Elasticsearch integration
│   ├── engagement-service/    # Likes, comments, views
│   ├── subscription-service/  # Subscriptions & feeds
│   ├── playlist-service/      # Playlist management
│   ├── notification-service/  # In-app notifications
│   ├── analytics-service/     # Creator analytics
│   ├── moderation-service/    # Content moderation
│   ├── live-tv-service/       # Live TV channels (IPTV-org)
│   └── common/                # Shared libraries
├── frontend/                   # React web application
├── mobile/                     # React Native (Expo) app
├── infrastructure/             # Docker configs, K8s manifests
│   ├── docker/                # Service-specific Dockerfiles
│   ├── kubernetes/            # K8s/Helm deployment configs
│   └── scripts/               # Operational scripts
├── docs/                       # Architecture & API docs
├── scripts/                    # Development utilities
├── docker-compose.yml          # Local development stack
├── .env.example               # Environment template
├── Makefile                   # Common commands
└── README.md
```

## Core Features (MVP)

### Viewer Experience
- Browse, search, and discover videos
- Adaptive HLS video playback (144p–4K)
- Continue watching / watch history
- Like, comment, share
- Subscribe to creators
- Create and manage playlists
- Light/dark/system themes with palettes
- **Live TV streaming with 10,000+ channels from IPTV-org**
- **Country-wise channel browsing (200+ countries)**
- **Category filtering (News, Sports, Entertainment, Movies, Music, Kids, etc.)**
- **Real-time stream health monitoring**
- **EPG (Electronic Program Guide) support**
- **Favorites and recently watched**
- **Picture-in-picture and fullscreen playback**

### Creator Experience
- Create and customize channel
- Resumable large file uploads (10GB+)
- Upload progress tracking
- Video metadata management
- Publish/unpublish control
- Basic analytics (views, watch time, engagement)

### Admin Experience
- User/creator management
- Video moderation (block/unblock)
- Category/genre taxonomy management
- Report review queue

## API Documentation

API is versioned at `/api/v1/`. OpenAPI specs are generated from Spring Boot services.

Key endpoints:
```
POST   /api/v1/auth/register           # Register
POST   /api/v1/auth/login              # Login
POST   /api/v1/auth/refresh            # Refresh token
POST   /api/v1/auth/logout             # Logout

GET    /api/v1/users/me                # Current user profile
PUT    /api/v1/users/me                # Update profile

POST   /api/v1/creators                # Become creator
GET    /api/v1/creators/me             # My creator profile
PUT    /api/v1/creators/me             # Update creator profile

POST   /api/v1/channels                # Create channel
GET    /api/v1/channels/{id}           # Get channel
PUT    /api/v1/channels/{id}           # Update channel

POST   /api/v1/uploads/sessions        # Create upload session
PUT    /api/v1/uploads/sessions/{id}/chunks/{part}  # Upload chunk
POST   /api/v1/uploads/sessions/{id}/complete       # Complete upload

GET    /api/v1/videos                  # List videos (paginated)
GET    /api/v1/videos/{id}             # Get video detail
POST   /api/v1/videos                  # Create video metadata
PUT    /api/v1/videos/{id}             # Update video
POST   /api/v1/videos/{id}/publish     # Publish video
DELETE /api/v1/videos/{id}             # Delete video

GET    /api/v1/search/videos           # Search videos
GET    /api/v1/search/suggest          # Autocomplete suggestions

POST   /api/v1/engagement/likes        # Like/unlike video
POST   /api/v1/engagement/comments     # Add comment
GET    /api/v1/engagement/comments/{videoId}  # Get comments

POST   /api/v1/subscriptions           # Subscribe
DELETE /api/v1/subscriptions/{channelId}  # Unsubscribe
GET    /api/v1/subscriptions/feed      # Subscription feed

POST   /api/v1/playlists               # Create playlist
GET    /api/v1/playlists               # List playlists
POST   /api/v1/playlists/{id}/items    # Add video to playlist

# Live TV
GET    /api/v1/live/channels           # List live channels (filtered)
GET    /api/v1/live/countries          # List countries with channel counts
GET    /api/v1/live/categories         # List categories with channel counts
GET    /api/v1/live/channels/{id}      # Get channel details
POST   /api/v1/live/channels/{id}/favorite  # Toggle favorite
POST   /api/v1/live/channels/{id}/view      # Record view
GET    /api/v1/live/channels/{id}/epg       # Get EPG data
```

## Environment Variables

Key variables in `.env`:

| Variable | Description | Default |
|----------|-------------|---------|
| `POSTGRES_PASSWORD` | Database password | `platform_dev_password` |
| `JWT_SECRET` | JWT signing key (256-bit) | `dev_secret...` |
| `MINIO_ROOT_PASSWORD` | MinIO admin password | `minioadmin123` |
| `UPLOAD_MAX_FILE_SIZE_GB` | Max upload size | `50` |
| `FFMPEG_THREADS` | Transcoding threads | `2` |
| `LIVETV_IPTV_ORG_CHANNELS_URL` | IPTV-org channels JSON URL | `https://raw.githubusercontent.com/iptv-org/iptv/master/channels.json` |
| `LIVETV_SYNC_ENABLED` | Enable auto-sync from IPTV-org | `true` |
| `LIVETV_SYNC_INTERVAL_HOURS` | Sync interval in hours | `6` |
| `LIVETV_HEALTH_CHECK_ENABLED` | Enable stream health checks | `true` |
| `LIVETV_HEALTH_CHECK_INTERVAL_MINUTES` | Health check interval in minutes | `30` |

See `.env.example` for complete list.

## Kubernetes Deployment

Production-ready Kubernetes manifests in `infrastructure/kubernetes/`:

```bash
# Deploy to development
kubectl apply -k infrastructure/kubernetes/overlays/dev

# Deploy to production
kubectl apply -k infrastructure/kubernetes/overlays/prod
```

Helm chart available at `infrastructure/kubernetes/helm/video-platform/`.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes with tests
4. Run `make validate` (build + test + health check)
5. Submit a pull request

## License

MIT License - see LICENSE file for details.

## Support

- Documentation: `/docs`
- Issues: GitHub Issues
- Architecture questions: See `docs/architecture/`

---

**Status**: Phase 1 (Infrastructure) Complete ✓
**Next**: Phase 2 - Auth & User Service Implementation