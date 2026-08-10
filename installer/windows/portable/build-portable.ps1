# ============================================================================
# Kaiteyo — portable (zip) build
#
# Produces a zero-install folder + zip from the jpackage image. The launcher
# points all data (settings, study DBs) into the folder so the build is
# genuinely portable — a USB stick carries the whole app.
#
# Usage: powershell -File installer/windows/portable/build-portable.ps1 -Version 2.2.1
# ============================================================================
param(
    [Parameter(Mandatory = $true)]
    [string]$Version
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "../../..")).Path
$App = Join-Path $Root "desktopApp/build/compose/binaries/main/app/Kaiteyo"
$Out = Join-Path $Root "installer/windows/build/kaiteyo-$Version"
$PortableDir = Join-Path $Out "Kaiteyo-Portable-$Version"

if (-not (Test-Path $App)) {
    Write-Error "App bundle not found at $App — run :desktopApp:createDistributable first."
    exit 1
}

# --- Assemble portable folder ------------------------------------------------
if (Test-Path $PortableDir) { Remove-Item $PortableDir -Recurse -Force }
New-Item -ItemType Directory -Force -Path $PortableDir | Out-Null

Copy-Item "$App\*" $PortableDir -Recurse -Force
Copy-Item (Join-Path $PSScriptRoot "kaiteyo-portable.bat") (Join-Path $PortableDir "Kaiteyo.bat")
Copy-Item (Join-Path $PSScriptRoot "kaiteyo-portable.ps1") (Join-Path $PortableDir "launcher.ps1")

# Portable data root: next to the exe (the bat sets KAITEYO_DATA_DIR).
New-Item -ItemType Directory -Force -Path (Join-Path $PortableDir "data") | Out-Null
Set-Content -Path (Join-Path $PortableDir "data/.gitkeep") -Value "" -NoNewline

# --- Zip it ------------------------------------------------------------------
$zip = Join-Path $Out "Kaiteyo-Portable-$Version.zip"
if (Test-Path $zip) { Remove-Item $zip -Force }

# Compress-Archive is fine for CI; 7z gives better ratios locally.
if (Get-Command 7z -ErrorAction SilentlyContinue) {
    7z a -tzip -mx9 $zip "$PortableDir\*" | Out-Null
} else {
    Compress-Archive -Path "$PortableDir\*" -DestinationPath $zip -CompressionLevel Optimal
}

Write-Host "== Portable build ready: $zip =="
Write-Host "   (folder kept at $PortableDir for direct copying)"
