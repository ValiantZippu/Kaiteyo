param([string]$Path, [string]$Pattern = "fun ", [string]$Out = "sig.txt")
$lines = Get-Content $Path
$result = @()
foreach ($line in $lines) {
    if ($line -match $Pattern) { $result += $line }
}
$result | Set-Content $Out -Encoding UTF8
Write-Output ("Wrote {0} lines" -f $result.Count)
