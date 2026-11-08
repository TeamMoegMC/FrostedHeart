param(
    [Parameter(Mandatory)][string]$ReferenceDirectory,
    [Parameter(Mandatory)][string]$CandidateDirectory,
    [double]$ToleranceC = 0.1,
    [int]$DiagnosticFromTick = 300
)
$ErrorActionPreference = 'Stop'
$thermalComparison = [ordered]@{reference=$ReferenceDirectory; candidate=$CandidateDirectory; toleranceC=$ToleranceC; diagnosticFromTick=$DiagnosticFromTick}
foreach ($thermalKind in @('air','material')) {
    $thermalReference = @{}
    foreach ($thermalRow in Import-Csv -LiteralPath (Join-Path $ReferenceDirectory "curves.$thermalKind.csv")) {
        $thermalReference["$($thermalRow.scene)|$($thermalRow.point)|$($thermalRow.elapsedTick)"] = $thermalRow
    }
    $thermalCandidates = @(Import-Csv -LiteralPath (Join-Path $CandidateDirectory "curves.$thermalKind.csv"))
    $thermalFields = $(if ($thermalKind -eq 'air') { @('airC','naturalC','radiantWPerM2','wind') } else { @('temperatureC','enthalpyJ','branch') })
    $thermalMaximums = [ordered]@{}; foreach ($thermalField in $thermalFields) { $thermalMaximums[$thermalField] = 0.0 }
    $thermalMissing = 0; $thermalTickMismatches = 0; $thermalAvailabilityMismatches = 0; $thermalDiagnosticMaximum = 0.0
    $thermalDifferences = [System.Collections.Generic.List[object]]::new()
    foreach ($thermalRow in $thermalCandidates) {
        $thermalKey = "$($thermalRow.scene)|$($thermalRow.point)|$($thermalRow.elapsedTick)"
        if (-not $thermalReference.ContainsKey($thermalKey)) { $thermalMissing++; continue }
        $thermalPrevious = $thermalReference[$thermalKey]
        if ($thermalPrevious.sampleTick -ne $thermalRow.sampleTick) { $thermalTickMismatches++ }
        if ($thermalPrevious.available -ne $thermalRow.available) { $thermalAvailabilityMismatches++ }
        foreach ($thermalField in $thermalFields) {
            $thermalBefore = [double]::Parse($thermalPrevious.$thermalField, [Globalization.CultureInfo]::InvariantCulture)
            $thermalAfter = [double]::Parse($thermalRow.$thermalField, [Globalization.CultureInfo]::InvariantCulture)
            if (-not [double]::IsFinite($thermalBefore) -or -not [double]::IsFinite($thermalAfter)) { continue }
            $thermalDifference = [Math]::Abs($thermalBefore-$thermalAfter)
            $thermalMaximums[$thermalField] = [Math]::Max($thermalMaximums[$thermalField],$thermalDifference)
            if ($thermalField -eq $(if ($thermalKind -eq 'air') { 'airC' } else { 'temperatureC' })) {
                if ([int]$thermalRow.elapsedTick -ge $DiagnosticFromTick) { $thermalDiagnosticMaximum = [Math]::Max($thermalDiagnosticMaximum,$thermalDifference) }
                $thermalDifferences.Add([pscustomobject]@{scene=$thermalRow.scene; point=$thermalRow.point; elapsedTick=[int]$thermalRow.elapsedTick
                    referenceSampleTick=[long]$thermalPrevious.sampleTick; candidateSampleTick=[long]$thermalRow.sampleTick
                    referenceC=$thermalBefore; candidateC=$thermalAfter; differenceC=$thermalDifference})
            }
        }
    }
    $thermalComparison[$thermalKind] = [ordered]@{referenceRows=$thermalReference.Count; candidateRows=$thermalCandidates.Count
        missingCandidateKeys=$thermalMissing; rowCountDifference=($thermalCandidates.Count-$thermalReference.Count)
        sampleTickMismatches=$thermalTickMismatches; availabilityMismatches=$thermalAvailabilityMismatches
        maximumAbsoluteDifference=$thermalMaximums; diagnosticWindowMaximumC=$thermalDiagnosticMaximum
        largestTemperatureDifferences=@($thermalDifferences | Sort-Object differenceC -Descending | Select-Object -First 8)}
}
$thermalComparison.qualified = $thermalComparison.air.maximumAbsoluteDifference.airC -le $ToleranceC `
    -and $thermalComparison.material.maximumAbsoluteDifference.temperatureC -le $ToleranceC `
    -and $thermalComparison.air.missingCandidateKeys -eq 0 -and $thermalComparison.air.rowCountDifference -eq 0 `
    -and $thermalComparison.air.sampleTickMismatches -eq 0 -and $thermalComparison.air.availabilityMismatches -eq 0 `
    -and $thermalComparison.material.missingCandidateKeys -eq 0 -and $thermalComparison.material.rowCountDifference -eq 0 `
    -and $thermalComparison.material.sampleTickMismatches -eq 0 -and $thermalComparison.material.availabilityMismatches -eq 0
$thermalJson = $thermalComparison | ConvertTo-Json -Depth 7
[IO.File]::WriteAllText((Join-Path $CandidateDirectory 'replay-comparison.json'),$thermalJson,[Text.UTF8Encoding]::new($false))
Write-Output $thermalJson
if (-not $thermalComparison.qualified) { exit 1 }
