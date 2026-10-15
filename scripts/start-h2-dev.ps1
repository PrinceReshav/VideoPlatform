$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$userProfile = $env:USERPROFILE
$port = 9123
$logDir = Join-Path $projectRoot 'logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

# Do not start a second server if the configured H2 TCP port is already occupied.
$listener = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
    Select-Object -First 1
if ($listener) {
    Write-Host "H2 TCP port $port is already listening (PID $($listener.OwningProcess)). Leaving it untouched." -ForegroundColor Yellow
    exit 0
}

# Preserve existing databases. The documented location resolves to USERPROFILE\data,
# while older local runs may have used USERPROFILE\Downloads\data.
$candidates = @(
    (Join-Path $userProfile 'Downloads\data'),
    (Join-Path $userProfile 'data')
)
$existing = @($candidates | Where-Object {
    (Test-Path (Join-Path $_ 'video-platform-dev.mv.db')) -or
    (Test-Path (Join-Path $_ 'video-platform-dev.h2.db'))
} | Select-Object -Unique)

if ($existing.Count -gt 1) {
    Write-Host 'Found more than one existing video-platform-dev H2 database. No database was selected or modified.' -ForegroundColor Red
    $existing | ForEach-Object { Write-Host "  $_" }
    Write-Host 'Choose the intended database directory, then rerun this script after resolving the duplicate locations.' -ForegroundColor Yellow
    exit 2
}

if ($existing.Count -eq 1) {
    $dbDir = $existing[0]
} else {
    $dbDir = Join-Path $userProfile 'data'
    New-Item -ItemType Directory -Force -Path $dbDir | Out-Null
}

$h2Root = Join-Path $userProfile '.m2\repository\com\h2database\h2'
$h2Jar = Get-ChildItem $h2Root -Recurse -File -Filter 'h2-*.jar' -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -notmatch '\\(sources|javadoc)\\' } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $h2Jar) {
    throw "H2 JAR not found under $h2Root. Build a backend module once so Maven downloads the H2 dependency, then rerun this script."
}

$stdout = Join-Path $logDir 'h2-tcp.stdout.log'
$stderr = Join-Path $logDir 'h2-tcp.stderr.log'
$startArguments = "-cp `"$($h2Jar.FullName)`" org.h2.tools.Server -tcp -tcpPort $port -baseDir `"$dbDir`" -ifNotExists"
$process = Start-Process -FilePath 'java' -ArgumentList $startArguments -PassThru -WindowStyle Hidden -RedirectStandardOutput $stdout -RedirectStandardError $stderr

for ($i = 0; $i -lt 20; $i++) {
    Start-Sleep -Milliseconds 500
    $listener = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($listener) { break }
    if ($process.HasExited) {
        throw "H2 server exited during startup. Check logs: $stdout and $stderr"
    }
}

if (-not $listener) {
    throw "H2 server did not start listening on port $port. Check logs: $stdout and $stderr"
}

Set-Content -Path (Join-Path $logDir 'h2-tcp.pid') -Value $process.Id -Encoding ascii
Write-Host 'H2 TCP server started successfully.' -ForegroundColor Green
Write-Host "Database directory: $dbDir"
Write-Host "TCP endpoint:      tcp://localhost:$port"
Write-Host "Process ID:        $($process.Id)"
Write-Host "Logs:              $stdout"
