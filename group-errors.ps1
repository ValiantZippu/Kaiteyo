param([string]$Log)
$lines = Get-Content $Log
$files = @{}
foreach ($line in $lines) {
    if ($line -match 'file:///(.+?):(\d+):(\d+)') {
        $f = $matches[1]
        if ($files.ContainsKey($f)) { $files[$f]++ } else { $files[$f] = 1 }
    }
}
$files.GetEnumerator() | Sort-Object Value -Descending | ForEach-Object { "{0,4}  {1}" -f $_.Value, $_.Key }
