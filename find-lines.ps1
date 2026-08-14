param([string]$Path, [string]$Out = "inserts.txt")
$lines = Get-Content $Path
$result = @()
for ($i = 0; $i -lt $lines.Count; $i++) {
    $line = $lines[$i]
    if ($line -match 'insertWritingAttempt|insertExam|completeExam|abandonExam|updateExamQuestionAnswer|insertExamQuestion|insertLearningMistake|insertStudySession|completeStudySession|updateStudySessionCounters|class UserData_statisticsQueries') {
        $result += ("{0}: {1}" -f ($i + 1), $line.Trim())
    }
}
$result | Set-Content $Out -Encoding UTF8
Write-Output ("Wrote {0} lines" -f $result.Count)
