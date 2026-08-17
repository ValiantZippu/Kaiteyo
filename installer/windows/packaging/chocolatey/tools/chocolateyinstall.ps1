# Chocolatey install script — Kaiteyo
# Silent install of the signed Inno EXE. Checksum verified before running.
# NOTE: checksum64 must be regenerated from the actual signed artifact at
# publish time — never ship a stale or guessed hash.

$ErrorActionPreference = 'Stop'

$version = '2.2.1'
$url = "https://github.com/ValiantZippu/Kaiteyo/releases/download/v$version/Kaiteyo-$version-windows-setup.exe"

# 0000... placeholder — replace with Get-FileHash -Algorithm SHA256 of the EXE.
$checksum = '0000000000000000000000000000000000000000000000000000000000000000'

$packageArgs = @{
    packageName    = 'kaiteyo'
    fileType       = 'exe'
    url            = $url
    checksum       = $checksum
    checksumType   = 'sha256'
    silentArgs     = '/VERYSILENT /SUPPRESSMSGBOXES /NORESTART'
    validExitCodes = @(0)
}

Install-ChocolateyPackage @packageArgs
