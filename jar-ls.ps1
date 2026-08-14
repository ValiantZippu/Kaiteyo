param([string]$Jar, [string]$Match = "PointerType")
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($Jar)
try {
    $zip.Entries | Where-Object { $_.FullName -match $Match } | ForEach-Object { Write-Output $_.FullName }
} finally {
    $zip.Dispose()
}
