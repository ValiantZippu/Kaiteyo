param(
    [string]$Log = "build-core-errors.log",
    [string]$Out = "errors-only.log"
)
$lines = Get-Content $Log
$result = @()
foreach ($line in $lines) {
    if ($line -match '^e: ') {
        $result += $line
    }
}
$result | Set-Content $Out -Encoding UTF8
Write-Output ("Wrote {0} error lines to {1}" -f $result.Count, $Out)
