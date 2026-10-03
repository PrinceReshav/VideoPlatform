# Video Platform - Production Deployment Checklist

## Pre-Deployment Requirements

### Infrastructure
- [ ] Kubernetes cluster (v1.28+) with 3+ worker nodes (16+ vCPU, 64GB RAM each)
- [ ] PostgreSQL 16 (managed or self-hosted) with 500GB+ storage
- [ ] Redis Cluster (6+ nodes) with 32GB+ memory
- [ ] Kafka Cluster (3+ brokers) with 500GB+ storage
- [ ] Elasticsearch Cluster (3+ nodes) with 500GB+ storage
- [ ] MinIO/S3 (4+ nodes) with 10TB+ storage
- [ ] Load Balancer (AWS ALB / NGINX Ingress)
- [ ] SSL Certificates (Let's Encrypt or custom)
- [ ] DNS configured for api.yourdomain.com, yourdomain.com, cdn.yourdomain.com

### Security
- [ ] All secrets rotated and stored in Kubernetes Secrets / Vault
- [ ] JWT secret (64-byte base64) generated and stored
- [ ] Database passwords rotated
- [ ] TLS 1.3 enforced everywhere
- [ ] Network policies configured
- [ ] Pod Security Standards enforced
- [ ] Image scanning enabled (Trivy/Snyk)

### Monitoring
- [ ] Prometheus + Grafana deployed
- [ ] AlertManager configured with Slack/PagerDuty
- [ ] Loki for log aggregation
- [ ] Jaeger for distributed tracing
- [ ] Uptime monitoring (external)

### Backup & DR
- [ ] PostgreSQL: Daily snapshots, point-in-time recovery
- [ ] Redis: RDB snapshots every 6 hours
- [ ] Kafka: MirrorMaker to secondary cluster
- [ ] MinIO: Cross-region replication
- [ ] ETCD: Daily snapshots
- [ ] DR runbook tested quarterly

## Deployment Steps

### 1. Prepare Environment
```bash
# Clone repository
git clone https://github.com/your-org/video-platform.git
cd video-platform

# Copy production environment
cp .env.production .env

# Edit with production values
vim .env
```

### 2. Build & Push Images
```bash
# Build all services
make build

# Or manually:
./scripts/deploy-prod.sh build ghcr.io/your-org v1.0.0
```

### 3. Create Kubernetes Secrets
```bash
# Create namespace
kubectl apply -f infrastructure/kubernetes/base/namespace.yaml

# Create secrets (replace with actual values)
kubectl create secret generic video-platform-secrets -n video-platform-prod \
  --from-literal=POSTGRES_PASSWORD="your-secure-password" \
  --from-literal=JWT_SECRET="your-64-byte-base64-secret" \
  --from-literal=REDIS_PASSWORD="your-redis-password" \
  --from-literal=MINIO_ROOT_PASSWORD="your-minio-password" \
  --from-literal=ELASTICSEARCH_PASSWORD="your-elastic-password" \
  --from-literal=JWT_SECRET="your-64-byte-base64-secret" \
  --from-literal=SMTP_PASSWORD="your-smtp-password" \
  --from-literal=ADMIN_PASSWORD="your-admin-password"
```

### 4. Deploy to Kubernetes
```bash
# Deploy all services
./scripts/deploy-prod.sh deploy ghcr.io/your-org v1.0.0

# Or with kustomize directly:
cd infrastructure/kubernetes/overlays/prod
kustomize build . | kubectl apply -f -
```

### 5. Verify Deployment
```bash
# Check all pods running
kubectl get pods -n video-platform-prod -o wide

# Check services
kubectl get svc -n video-platform-prod

# Check ingress
kubectl get ingress -n video-platform-prod

# Run health checks
./scripts/deploy-prod.sh health
```

### 6. Configure DNS & SSL
```bash
# Point DNS to Load Balancer IP
# A record: api.yourdomain.com -> LB IP
# A record: yourdomain.com -> LB IP
# A record: www.yourdomain.com -> LB IP
# CNAME: cdn.yourdomain.com -> CloudFront/S3 endpoint

# Verify SSL
curl -I https://api.yourdomain.com/actuator/health
curl -I https://yourdomain.com/
```

### 6. Configure CDN
```bash
# CloudFront / Cloudflare / AWS CloudFront
# Origin: api.yourdomain.com
# Cache behaviors:
# - /api/v1/videos/*/hls/* -> Cache 1 year
# - /api/v1/videos/*/thumbnails/* -> Cache 1 year
# - /api/v1/search/* -> No cache
# - /api/v1/auth/* -> No cache
```

## Post-Deployment Verification

### Functional Tests
- [ ] User registration & login
- [ ] Video upload (100MB, 1GB, 10GB)
- [ ] Video playback (HLS adaptive)
- [ ] Search & autocomplete
- [ ] Comments, likes, subscriptions
- [ ] Creator dashboard & analytics
- [ ] Admin panel access

### Performance Tests
- [ ] Load test: 10k concurrent viewers
- [ ] Upload: 10GB file resume
- [ ] Transcode: 4K video < 30 min
- [ ] Search: < 200ms p99
- [ ] API: < 100ms p99

### Security Scan
```bash
# Container scan
trivy image ghcr.io/your-org/video-platform-api-gateway:latest

# Dependency scan
cd backend && mvn org.owasp:dependency-check-maven:check

# SAST
# Run SonarQube / CodeQL
```

## Rollback Procedure

```bash
# Quick rollback
kubectl rollout undo deployment -n video-platform-prod

# Specific revision
kubectl rollout undo deployment/api-gateway -n video-platform-prod --to-revision=3

# Verify
kubectl rollout status deployment -n video-platform-prod
```

## Monitoring Alerts

### Critical Alerts (Page Immediately)
- Pod crash loop > 5 min
- API error rate > 5%
- Database CPU > 90%
- Disk usage > 85%
- Kafka consumer lag > 100k

### Warning Alerts (Notify within 15 min)
- Pod restart > 3/hour
- API latency p99 > 500ms
- Memory usage > 80%
- Kafka consumer lag > 10k

## Maintenance Windows

- **Weekly**: Sunday 02:00-04:00 UTC (DB maintenance, index rebuild)
- **Monthly**: 1st Sunday 00:00-06:00 UTC (OS patches, k8s upgrade)
- **Quarterly**: DR drill, backup restore test

## Contact

- **On-call**: #video-platform-oncall (Slack/PagerDuty)
- **Escalation**: platform-leads@yourdomain.com
- **Security**: security@yourdomain.com

---

**Sign-off Required**: 
- [ ] Platform Lead: _____________ Date: _______
- [ ] Security Lead: _____________ Date: _______
- [ ] DevOps Lead: _______________ Date: _______