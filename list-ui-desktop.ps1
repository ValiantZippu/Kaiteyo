$dirs = @(
    "C:/Users/Kawaru/.gradle/caches/modules-2/files-2.1/org.jetbrains.compose.ui/ui-desktop",
    "C:/Users/Kawaru/.gradle/caches/modules-2/files-2.1/org.jetbrains.compose.ui/ui"
)
foreach ($d in $dirs) {
    Write-Output "== $d =="
    if (Test-Path $d) {
        Get-ChildItem $d -Recurse -File -ErrorAction SilentlyContinue | ForEach-Object { Write-Output $_.FullName }
    }
}
