# Kaiteyo Tag System — YYYY.MM.DD-<channel>-v<VERSION>-<CODE>-<SHA>
# Usage: .\scripts\tag.ps1 [alpha|beta|release] [-Push] [-DryRun]
#   channel auto-detected from current branch if not supplied:
#     early-alpha-develop  -> alpha
#     early-beta-develop   -> beta
#     early-release-develop -> release
param(
    [string]$Channel,
    [switch]$Push,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$RootDir = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
if (-not $RootDir) { $RootDir = (Get-Location).Path }
# Fallback to repo root if script is in scripts/
$AppVersionKt = Join-Path $RootDir "buildSrc\src\main\kotlin\AppVersion.kt"
if (-not (Test-Path $AppVersionKt)) {
    # Try alternative relative path when run from repo root
    $AppVersionKt = "buildSrc\src\main\kotlin\AppVersion.kt"
}
if (-not (Test-Path $AppVersionKt)) {
    Write-Error "AppVersion.kt not found at $AppVersionKt"; exit 1
}

# Resolve channel
$Branch = (git rev-parse --abbrev-ref HEAD 2>$null).Trim()
if (-not $Channel -or $Channel.StartsWith("-")) {
    switch ($Branch) {
        "early-alpha-develop" { $Channel = "alpha" }
        "early-beta-develop"  { $Channel = "beta" }
        "early-release-develop" { $Channel = "release" }
        "main" { $Channel = "stable" }
        default { $Channel = "dev" }
    }
} else {
    switch -Regex ($Channel) {
        "^(alpha|beta|release|stable|rc|dev)$" { }
        "early-alpha.*" { $Channel = "alpha" }
        "early-beta.*"  { $Channel = "beta" }
        "early-release.*" { $Channel = "release" }
        default { Write-Error "Unknown channel: $Channel (use alpha|beta|release|stable)"; exit 1 }
    }
}

# Read versionName and versionCode
$Content = Get-Content $AppVersionKt -Raw
$VersionName = [regex]::Match($Content, 'const val versionName\s*=\s*"([^"]+)"').Groups[1].Value
$VersionCode = [regex]::Match($Content, 'const val versionCode\s*=\s*([0-9]+)').Groups[1].Value
if (-not $VersionName -or -not $VersionCode) { Write-Error "Failed to parse version from $AppVersionKt"; exit 1 }

$Date = (Get-Date).ToUniversalTime().ToString("yyyy.MM.dd")
$Sha = (git rev-parse --short=7 HEAD).Trim()
$Tag = "$Date-$Channel-v$VersionName-$VersionCode-$Sha"

# Ensure uniqueness — if tag exists, append -N
$Existing = git tag --list $Tag 2>$null
if ($Existing) {
    $i = 2
    while (git rev-parse "$Tag-$i" 2>$null) { $i++ }
    $Tag = "$Tag-$i"
}

$CommitMsg = "Kaiteyo $Channel $VersionName ($VersionCode) - $Date - $Branch@$Sha"

Write-Host @"
Tag System: YYYY.MM.DD-<channel>-v<VERSION>-<CODE>-<SHA>
  Date:     $Date (UTC)
  Channel:  $Channel (from $Branch)
  Version:  v$VersionName ($VersionCode)
  Commit:   $Sha ($Branch)
  Tag:      $Tag
"@

if ($DryRun) {
    Write-Host "[dry-run] Would create annotated tag: $Tag"
    exit 0
}

Write-Host "Creating annotated tag $Tag ..."
git tag -a $Tag -m $CommitMsg

if ($Push) {
    Write-Host "Pushing tag $Tag to origin ..."
    git push origin $Tag
} else {
    Write-Host "Tag created locally. Push with: git push origin $Tag"
}
