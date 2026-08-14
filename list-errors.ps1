$lines = Select-String -Path build-core-errors.log -Pattern 'e: file'
$files = @()
foreach ($l in $lines) {
    $line = $l.Line
    if ($line -match 'core/src/(commonMain|jvmMain|androidMain|iosMain)/kotlin/ua/syt0r/kanji/(.*?):\d+') {
        $files += $matches[2]
    }
}
$files | Sort-Object -Unique | ForEach-Object { Write-Output $_ }