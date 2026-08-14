param([string]$Aar, [string]$OutDir = "C:\Users\Admin\AppData\Local\Temp\compose-inspect")
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($Aar)
try {
    $entry = $zip.Entries | Where-Object { $_.FullName -eq 'classes.jar' }
    if ($entry -eq $null) { Write-Output "no classes.jar"; exit 1 }
    New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
    $jarPath = Join-Path $OutDir 'classes.jar'
    [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $jarPath, $true)
    Write-Output "extracted to $jarPath"
} finally {
    $zip.Dispose()
}
