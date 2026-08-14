param([string]$Log = "build-core-errors.log")
$c = Get-Content $Log | Where-Object { $_ -match '^e: file:' }
Write-Output ("TOTAL: " + $c.Count)
$c | ForEach-Object {
    if ($_ -match 'core/(.+?):(\d+):') { $Matches[1] }
} | Group-Object | Sort-Object Count -Descending | ForEach-Object {
    '{0,4}  {1}' -f $_.Count, $_.Name
}