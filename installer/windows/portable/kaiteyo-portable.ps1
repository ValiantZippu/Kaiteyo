# ============================================================================
# Kaiteyo Portable launcher helper
# Redirects the JVM's user.home to the portable data folder so every file the
# app writes lands inside the portable directory (never the host profile).
# ============================================================================
param(
    [Parameter(Mandatory = $true)][string]$Exe,
    [Parameter(ValueFromRemainingArguments = $true)][string[]]$AppArgs
)

$dataDir = Join-Path (Split-Path -Parent $Exe) "data"
New-Item -ItemType Directory -Force -Path $dataDir | Out-Null

# The app stores its data under user.home/.kaiteyo — point it at the portable
# folder. (The app also honours %KAITEYO_DATA_DIR% if set, as a secondary hook.)
$env:KAITEYO_DATA_DIR = $dataDir
$env:USERPROFILE = $dataDir

& $Exe @AppArgs
exit $LASTEXITCODE
