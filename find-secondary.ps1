param([string]$Dir = "D:/Local/jar-extracted/androidx/compose/ui/input/pointer")
Get-ChildItem -Path $Dir -File | ForEach-Object {
    $bytes = [System.IO.File]::ReadAllBytes($_.FullName)
    $text = [System.Text.Encoding]::ASCII.GetString($bytes)
    if ($text -match 'Secondary') {
        Write-Output ('HIT: ' + $_.Name)
    }
}
