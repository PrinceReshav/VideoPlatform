$ErrorActionPreference = 'Stop'
$root = Join-Path $PSScriptRoot '..\iptv'
if (!(Test-Path $root)) { throw "iptv directory not found: $root" }
$required = @('data\channels.json','data\feeds.json','data\logos.json','data\categories.json','data\countries.json','data\languages.json','streams')
foreach ($path in $required) { if (!(Test-Path (Join-Path $root $path))) { throw "Missing IPTV-org path: $path" } }
$m3u = @(Get-ChildItem (Join-Path $root 'streams') -Recurse -File -Filter *.m3u).Count
Write-Host "IPTV-org checkout detected. M3U playlists: $m3u"
if ($m3u -lt 300) { throw "Expected the complete IPTV-org streams checkout; fewer than 300 M3U files were found." }
Write-Host 'IPTV-org verification PASS'
