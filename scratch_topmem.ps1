Get-Process | Sort-Object WorkingSet64 -Descending | Select-Object -First 12 | ForEach-Object { "{0,-28} {1,8:N0} MB  {2}" -f $_.ProcessName, ($_.WorkingSet64/1MB), $_.Id }
