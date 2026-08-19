$os = Get-CimInstance Win32_OperatingSystem
$freeGB = [math]::Round($os.FreePhysicalMemory / 1MB, 1)
$totalGB = [math]::Round($os.TotalVisibleMemorySize / 1MB, 1)
Write-Output "FreeGB=$freeGB TotalGB=$totalGB"
$top = Get-Process | Sort-Object WorkingSet64 -Descending | Select-Object -First 6 Name, @{n='MB';e={[math]::Round($_.WorkingSet64/1MB,0)}}
$top | Format-Table -AutoSize | Out-String | Write-Output
