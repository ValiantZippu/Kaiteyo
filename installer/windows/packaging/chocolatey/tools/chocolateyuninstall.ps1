# Chocolatey uninstall script — Kaiteyo
# Runs the Inno uninstaller silently. The uninstaller's default is to KEEP
# user data (decks, database, settings) — this wrapper preserves that default,
# so `choco uninstall kaiteyo` never deletes study data.

$ErrorActionPreference = 'Stop'

$uninstallKey = Get-ItemProperty -Path 'HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*' -ErrorAction SilentlyContinue |
    Where-Object { $_.DisplayName -like 'Kaiteyo*' -and $_.UninstallString } |
    Select-Object -First 1

if ($uninstallKey) {
    $uninstallString = $uninstallKey.UninstallString
    # Inno uninstallers accept /VERYSILENT; data-preservation is the default choice.
    $silentUninstall = "$uninstallString /VERYSILENT /SUPPRESSMSGBOXES /NORESTART"
    Start-Process -FilePath $uninstallKey.UninstallString.Split('"')[1] -ArgumentList '/VERYSILENT /SUPPRESSMSGBOXES /NORESTART' -Wait -NoNewWindow
} else {
    Write-Warning 'Kaiteyo uninstall entry not found — nothing to uninstall.'
}
