Get-Process java -ErrorAction SilentlyContinue | ForEach-Object { "{0} CPU={1} MemMB={2} Start={3}" -f $_.Id, [math]::Round($_.CPU), [math]::Round($_.WorkingSet64/1MB), $_.StartTime }
