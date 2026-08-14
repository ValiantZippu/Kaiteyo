$roots = @(
    "D:/Local Disk C/Users/Admin",
    "D:/Users",
    "D:/Local",
    "C:/Users/Admin",
    "D:/Local Disk C/Users/Admin/AppData"
)
foreach ($r in $roots) {
    if (Test-Path $r) {
        Write-Output "== $r =="
        Get-ChildItem $r -Directory -Force -ErrorAction SilentlyContinue | ForEach-Object { Write-Output $_.FullName }
    }
}
