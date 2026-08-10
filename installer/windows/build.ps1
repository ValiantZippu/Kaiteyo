# ============================================================================
# Kaiteyo — Windows installer build (PowerShell wrapper around ISCC)
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File installer/windows/build.ps1 `
#       -Version 2.2.1 [-Sign] [-CertThumbprint ABC...] [-OutDir build]
#
# -Version is required and must match installer/common/version.json.
# -Sign uses signtool.exe (Windows SDK) with the given certificate thumbprint.
# ============================================================================
param(
    [Parameter(Mandatory = $true)]
    [string]$Version,

    [switch]$Sign,
    [string]$CertThumbprint = ""
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path

# --- Find ISCC ---------------------------------------------------------------
$iscc = Get-Command "ISCC.exe" -ErrorAction SilentlyContinue
if (-not $iscc) {
    $candidates = @(
        "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe",
        "${env:ProgramFiles}\Inno Setup 6\ISCC.exe"
    )
    $isccPath = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
    if (-not $isccPath) {
        Write-Error "Inno Setup 6 not found. Install from https://jrsoftware.org/isinfo.php"
        exit 1
    }
} else {
    $isccPath = $iscc.Source
}

# --- Validate version against the single source of truth ---------------------
$versionJson = Join-Path $Root "installer/common/version.json"
if (Test-Path $versionJson) {
    $manifestVersion = (Get-Content $versionJson | ConvertFrom-Json).version
    if ($manifestVersion -ne $Version) {
        Write-Warning "version.json says $manifestVersion but -Version is $Version; using $Version"
    }
}

# --- Ensure generated assets exist -------------------------------------------
$banner = Join-Path $PSScriptRoot "assets/generated/windows/banner.bmp"
if (-not (Test-Path $banner)) {
    Write-Host "Brand assets missing — running generate-assets.sh (requires rsvg-convert/ImageMagick)…"
    bash "$Root/installer/scripts/generate-assets.sh" 2>$null
    if (-not (Test-Path $banner)) {
        Write-Error "Brand assets not generated; run installer/scripts/generate-assets.sh first."
        exit 1
    }
}

# --- Signing -----------------------------------------------------------------
$signCmd = ""
if ($Sign) {
    if (-not $CertThumbprint) {
        Write-Error "-Sign requires -CertThumbprint <sha1 thumbprint of the code-signing cert>"
        exit 1
    }
    $signCmd = "signtool.exe sign /fd SHA256 /tr http://timestamp.digicert.com /td SHA256 /sha1 $CertThumbprint"
}

# --- Compile -----------------------------------------------------------------
$iss = Join-Path $PSScriptRoot "kaiteyo.iss"
$outDir = Join-Path $PSScriptRoot "build/kaiteyo-$Version"

# The .iss references a staged dictionary starter pack; make sure the folder
# exists so ISCC never fails on a missing source (empty is fine — the task is
# opt-in and onlyifdoesntexist).
$starterPack = Join-Path $Root "desktopApp/build/dictionary-starter-pack"
New-Item -ItemType Directory -Force -Path $starterPack | Out-Null

Write-Host "== Building Kaiteyo $Version installer with ISCC =="
# OutputDir in the script controls placement (installer/windows/build/kaiteyo-<v>);
# do NOT pass /O here so CI/staging globs keep matching.
& $isccPath "/DMyAppVersion=$Version" $iss
if ($LASTEXITCODE -ne 0) {
    Write-Error "ISCC failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}

$exe = Join-Path $outDir "Kaiteyo-Setup-$Version.exe"
if ($Sign) {
    Write-Host "Signing $exe …"
    Invoke-Expression "$signCmd `"$exe`""
    if ($LASTEXITCODE -ne 0) { Write-Error "Signing failed"; exit $LASTEXITCODE }
}

Write-Host "== Done: $exe =="
