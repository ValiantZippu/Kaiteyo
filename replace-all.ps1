param([string]$Path, [string]$Old, [string]$New)
$c = Get-Content $Path
$t = $c -join [Environment]::NewLine
$t = $t.Replace($Old, $New)
Set-Content $Path -Value $t -NoNewline
Write-Output "done"
