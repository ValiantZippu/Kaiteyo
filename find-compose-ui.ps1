$cacheRoot = "C:/Users/Kawaru/.gradle/caches/modules-2/files-2.1"
if (-not (Test-Path $cacheRoot)) { Write-Output "NO CACHE"; exit 1 }
Get-ChildItem $cacheRoot -Directory -Recurse -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -eq 'ui-desktop' -or $_.Name -eq 'ui-jvm' -or ($_.FullName -match 'compose.ui' -and $_.Name -match '^ui$') } |
    Select-Object -First 10 -ExpandProperty FullName
