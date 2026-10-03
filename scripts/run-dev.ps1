$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root 'backend'

$services = @(
  'api-gateway',
  'auth-service',
  'user-creator-service',
  'video-service',
  'upload-service',
  'transcoding-service',
  'search-service',
  'engagement-service',
  'subscription-service',
  'playlist-service',
  'notification-service',
  'analytics-service',
  'moderation-service'
)

foreach ($service in $services) {
  $dir = Join-Path $backend $service
  Start-Process powershell -ArgumentList '-NoExit','-Command', "Set-Location '$dir'; `$env:SPRING_PROFILES_ACTIVE='dev'; mvn spring-boot:run"
  Start-Sleep -Milliseconds 700
}

Write-Host 'Started all Spring Boot services with SPRING_PROFILES_ACTIVE=dev.' -ForegroundColor Green
Write-Host 'Frontend: cd frontend; npm ci; npm run dev' -ForegroundColor Cyan
