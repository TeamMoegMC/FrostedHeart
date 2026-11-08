param(
    [string]$Label = ('baseline-' + [DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(8)).ToString('yyyyMMdd-HHmmss')),
    [string[]]$Only = @(),
    [string]$JavaDirectory = 'C:/Program Files/Java/jdk-17',
    [string]$GradleInit = '',
    [string]$ReferenceClimate = '',
    [string]$WorldType = '',
    [string]$PrecompiledDirectory = '',
    [string]$MainClassesDirectory = '',
    [switch]$Offline,
    [switch]$SkipCompile,
    [switch]$Profile,
    [switch]$NativeCpu
)
$ErrorActionPreference = 'Stop'
$thermalRepo = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $thermalRepo
if (-not $JavaDirectory -or -not (Test-Path -LiteralPath (Join-Path $JavaDirectory 'bin/java.exe'))) {
    $JavaDirectory = 'C:/Program Files/Java/jdk-17'
}
$env:JAVA_HOME = $JavaDirectory
$thermalRoot = Join-Path $thermalRepo "build/thermal-baselines/$Label"
$thermalRun = Join-Path $thermalRoot 'server'
$thermalConfig = Join-Path $thermalRun 'config'
[IO.Directory]::CreateDirectory($thermalConfig) | Out-Null
foreach ($thermalFile in Get-ChildItem -LiteralPath 'run-gametest/config' -File | Where-Object Name -NotLike '*.bak') {
    Copy-Item -LiteralPath $thermalFile.FullName -Destination $thermalConfig -Force
}
$thermalSettings = [IO.File]::ReadAllText((Join-Path $thermalConfig 'frostedheart-common.toml'))
$thermalValues = [ordered]@{
    thermalMemoryTargetMiB = '128'; continuousAirCpuBudgetMsPerSecond = '1000'
    airHeatCapacityJPerBlockK = '1200.0'; airMixingWPerBlockK = '96.0'
    dormantTemperatureHalfLifeSeconds = '30.0'; continuousAirNativeCheckpoint = 'true'
    continuousAirOverloadProtection = 'true'
}
foreach ($thermalKey in $thermalValues.Keys) {
    $thermalSettings = $thermalSettings -replace "(?m)^\s*$thermalKey\s*=.*$", "`t$thermalKey = $($thermalValues[$thermalKey])"
}
[IO.File]::WriteAllText((Join-Path $thermalConfig 'frostedheart-common.toml'), $thermalSettings, [Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText((Join-Path $thermalRun 'eula.txt'), 'eula=true', [Text.UTF8Encoding]::new($false))
$thermalProperties = [IO.File]::ReadAllText((Join-Path $thermalRepo 'run-gametest/server.properties'))
$thermalProperties = $thermalProperties -replace '(?m)^level-seed=.*$', 'level-seed=755111011717574879'
if ($WorldType) { $thermalProperties = $thermalProperties -replace '(?m)^level-type=.*$', "level-type=$WorldType" }
if ($WorldType -eq 'minecraft:flat') {
    $thermalProperties = $thermalProperties -replace '(?m)^generator-settings=.*$', 'generator-settings={"biome":"minecraft:plains","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"lakes":false,"features":false}'
}
$thermalCases = @(
    @{Name='coarse-cooling'; Namespace='frostedheart_coarse_cooling'; Count=1},
    @{Name='curves'; Namespace='frostedheart_thermal_baseline'; Count=1},
    @{Name='precision-room'; Namespace='frostedheart_precision_room'; Count=1},
    @{Name='background'; Namespace='frostedheart_regional_background'; Count=1},
    @{Name='grid-switch'; Namespace='frostedheart_regional_grid_switch'; Count=1},
    @{Name='materials'; Namespace='frostedheart_material_runtime'; Count=8},
    @{Name='retention'; Namespace='frostedheart_retirement'; Count=4},
    @{Name='retention-room'; Namespace='frostedheart_retirement_room'; Count=2},
    @{Name='pending'; Namespace='frostedheart_retirement_pending'; Count=1},
    @{Name='reload'; Namespace='frostedheart_retirement_reload'; Count=1},
    @{Name='concurrent-save'; Namespace='frostedheart_concurrent_save'; Count=2},
    @{Name='normal-load'; Namespace='frostedheart_two_dense_machine_bases'; Count=1},
    @{Name='capacity'; Namespace='frostedheart_geometry_capacity'; Count=1},
    @{Name='blocked-phase'; Namespace='frostedheart_blocked_phase'; Count=1; Clock='lockstep'},
    @{Name='river'; Namespace='frostedheart_river_thermal'; Count=1},
    @{Name='restart-write'; Namespace='frostedheart_retirement_restart'; Count=1; World='restart'; Marker='THERMAL_RETIREMENT_RESTART_WRITE'},
    @{Name='restart-read'; Namespace='frostedheart_retirement_restart'; Count=1; World='restart'; Marker='THERMAL_RETIREMENT_RESTART_DELETE'},
    @{Name='restart-empty'; Namespace='frostedheart_retirement_restart'; Count=1; World='restart'; Marker='THERMAL_RETIREMENT_RESTART_EMPTY'}
)
if ($Only) { $thermalSelected = @($Only | ForEach-Object { $_.Split(',') }); $thermalCases = @($thermalCases | Where-Object { $_.Name -in $thermalSelected }) }
$thermalGradle = @('--no-daemon', '--max-workers=1', '--console=plain',
    '-Dnet.minecraftforge.gradle.check.certs=false',
    '-Dorg.gradle.jvmargs=-Xmx1g -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8 -Dsun.net.client.defaultConnectTimeout=5000 -Dsun.net.client.defaultReadTimeout=5000')
if ($GradleInit) { foreach ($thermalInitFile in $GradleInit.Split(',')) { $thermalGradle += @('-I', $thermalInitFile) } }
$thermalGradle += @('-I', (Join-Path $PSScriptRoot 'thermal-baseline.init.gradle'),
    "-PthermalBaselineBuildDirectory=$thermalRoot/compiled")
if ($PrecompiledDirectory) {
    $PrecompiledDirectory = (Resolve-Path -LiteralPath $PrecompiledDirectory).Path
    if (-not $MainClassesDirectory) { $MainClassesDirectory = Join-Path $PrecompiledDirectory 'classes/java/main' }
    $MainClassesDirectory = (Resolve-Path -LiteralPath $MainClassesDirectory).Path
    foreach ($thermalOutput in @('classes/java/gameTest', 'resources/main', 'resources/gameTest')) {
        $null = Resolve-Path -LiteralPath (Join-Path $PrecompiledDirectory $thermalOutput)
    }
    $thermalGradle += @("-PthermalPrecompiledDirectory=$PrecompiledDirectory", "-PthermalMainClassesDirectory=$MainClassesDirectory")
    $SkipCompile = $true
}
if ($Offline) { $thermalGradle += '--offline' }
foreach ($thermalVariable in @(Get-ChildItem Env: | Where-Object { $_.Name -like 'FH_CONTINUOUS_AIR_*' -or $_.Name -like 'FH_THERMAL_*' -or $_.Name -eq 'FH_PRECISION_CLIMATE_SNAPSHOT' })) {
    Remove-Item -LiteralPath "Env:$($thermalVariable.Name)"
}
$env:FH_CONTINUOUS_AIR_SHUTDOWN_OBSERVATION = 'true'
$env:FH_CONTINUOUS_AIR_REGION_DIAGNOSTICS = 'false'
$env:FH_CONTINUOUS_AIR_PROFILE = 'false'
$env:FH_THERMAL_DEEP_MEMORY_PROFILE = 'false'
$env:FH_THERMAL_NATIVE_CPU_OBSERVATION = $NativeCpu.IsPresent.ToString().ToLowerInvariant()
$env:FH_RIVER_TEST_TICKS = '3600'
$env:FH_THERMAL_BASELINE_CLIMATE_OUTPUT = Join-Path $thermalRoot 'climate.dat'
$thermalCompileExit = 0
if ($SkipCompile -and -not $PrecompiledDirectory -and -not (Test-Path -LiteralPath (Join-Path $thermalRoot 'compiled/classes/java/gameTest'))) {
    Write-Output 'This label has no compiled GameTest output; compiling current sources.'
    $SkipCompile = $false
}
if (-not $SkipCompile) {
    Write-Output "Compiling current main and GameTest sources: $thermalRoot"
    & ./gradlew.bat compileJava compileGameTestJava @thermalGradle *> (Join-Path $thermalRoot 'compile.log')
    $thermalCompileExit = $LASTEXITCODE
}
$thermalResults = [System.Collections.Generic.List[object]]::new()
foreach ($thermalPrevious in Get-ChildItem -LiteralPath $thermalRoot -Filter '*.result.json') {
    $thermalRecorded = Get-Content -LiteralPath $thermalPrevious.FullName -Raw | ConvertFrom-Json
    if ($thermalRecorded.name -notin @($thermalCases | ForEach-Object Name)) { $thermalResults.Add($thermalRecorded) }
}
if ($thermalCompileExit -eq 0) {
    foreach ($thermalCase in $thermalCases) {
        $thermalName = $thermalCase.Name
        $env:FH_CONTINUOUS_AIR_TEST_CLOCK = $(if ($thermalCase.Clock) { $thermalCase.Clock } else { 'realtime20' })
        $thermalClimate = Join-Path $thermalRoot 'climate.dat'
        $thermalClimateInput = $(if ($ReferenceClimate) { (Resolve-Path -LiteralPath $ReferenceClimate).Path }
            elseif (Test-Path -LiteralPath $thermalClimate) { $thermalClimate } else { '' })
        if ($thermalClimateInput) {
            $thermalClimateCopy = Join-Path $thermalRoot "$thermalName.climate.dat"
            Copy-Item -LiteralPath $thermalClimateInput -Destination $thermalClimateCopy -Force
            $env:FH_PRECISION_CLIMATE_SNAPSHOT = $thermalClimateCopy
        }
        $thermalWorld = $(if ($thermalCase.World) { $thermalCase.World } else { $thermalName })
        [IO.File]::WriteAllText((Join-Path $thermalRun 'server.properties'),
            ($thermalProperties -replace '(?m)^level-name=.*$', "level-name=world-$thermalWorld"), [Text.UTF8Encoding]::new($false))
        Copy-Item -LiteralPath (Join-Path $thermalConfig 'frostedheart-common.toml') -Destination (Join-Path $thermalRoot "$thermalName.config.toml") -Force
        Copy-Item -LiteralPath (Join-Path $thermalRun 'server.properties') -Destination (Join-Path $thermalRoot "$thermalName.server.properties") -Force
        $thermalLog = Join-Path $thermalRoot "$thermalName.log"
        $thermalArguments = @('runGameTestServer', "-PgameTestNamespaces=$($thermalCase.Namespace)",
            "-PgameTestDirectory=$thermalRun", '-PthermalTestHeap=4g', '-PthermalTestProcessors=5',
            '-x', 'compileJava', '-x', 'compileGameTestJava', '-x', 'cleanResearchGameTestWorld')
        if ($Profile) { $thermalArguments += "-PthermalJfrFile=$thermalRoot/$thermalName.jfr" }
        if ($PrecompiledDirectory) { $thermalArguments += @('-x', 'processResources', '-x', 'processGameTestResources') }
        Write-Output "Starting $thermalName ($($thermalCase.Namespace), $env:FH_CONTINUOUS_AIR_TEST_CLOCK)"
        $thermalStarted = [DateTimeOffset]::UtcNow
        & ./gradlew.bat @thermalArguments @thermalGradle *> $thermalLog
        $thermalExit = $LASTEXITCODE
        $thermalText = [IO.File]::ReadAllText($thermalLog)
        $thermalPassed = [regex]::Match($thermalText, 'All ([1-9][0-9]*) required tests passed')
        $thermalFailed = [regex]::Match($thermalText, '([1-9][0-9]*) required tests failed')
        $thermalRoots = [regex]::Matches($thermalText, 'THERMAL_SHUTDOWN_ROOT,[^\r\n]*rootBytes=(\d+)')
        $thermalPassCount = $(if ($thermalPassed.Success) { [int]$thermalPassed.Groups[1].Value } else { 0 })
        $thermalErrorCount = [regex]::Matches($thermalText, 'Failed to save chunk|Thermal close failed|Uncaught thermal worker task failure|Regional runtime failed').Count
        $thermalRootZero = $thermalRoots.Count -gt 0 -and $thermalRoots[$thermalRoots.Count - 1].Groups[1].Value -eq '0'
        $thermalMarker = -not $thermalCase.Marker -or $thermalText.Contains($thermalCase.Marker)
        $thermalResult = [ordered]@{
            name=$thermalName; namespace=$thermalCase.Namespace; clock=$env:FH_CONTINUOUS_AIR_TEST_CLOCK
            world="world-$thermalWorld"; expected=$thermalCase.Count; passed=$thermalPassCount
            failed=$(if ($thermalFailed.Success) { [int]$thermalFailed.Groups[1].Value } else { 0 })
            exit=$thermalExit; thermalErrors=$thermalErrorCount; rootZero=$thermalRootZero; phaseMarkerPresent=$thermalMarker
            climateInput=$env:FH_PRECISION_CLIMATE_SNAPSHOT
            qualified=($thermalExit -eq 0 -and $thermalPassCount -eq $thermalCase.Count -and $thermalErrorCount -eq 0 -and $thermalRootZero -and $thermalMarker)
            started=$thermalStarted.ToOffset([TimeSpan]::FromHours(8)).ToString('o')
            elapsedSeconds=([DateTimeOffset]::UtcNow - $thermalStarted).TotalSeconds; log=$thermalLog
        }
        $thermalResults.Add($thermalResult)
        if ($thermalName -eq 'curves' -and $thermalResult.qualified) {
            $thermalSavedClimate = Join-Path $thermalRun 'world-curves/data/capabilities.dat'
            if (Test-Path -LiteralPath $thermalSavedClimate) {
                Copy-Item -LiteralPath $thermalClimate -Destination (Join-Path $thermalRoot 'climate-setup.dat') -Force
                Copy-Item -LiteralPath $thermalSavedClimate -Destination $thermalClimate -Force
            }
        }
        [IO.File]::WriteAllText((Join-Path $thermalRoot "$thermalName.result.json"), ($thermalResult | ConvertTo-Json -Depth 5), [Text.UTF8Encoding]::new($false))
        foreach ($thermalKind in @('AIR', 'MATERIAL')) {
            $thermalRows = @([regex]::Matches($thermalText, "THERMAL_BASELINE_$thermalKind,([^\r\n]+)") | ForEach-Object {
                $thermalRow = [ordered]@{}
                foreach ($thermalPair in $_.Groups[1].Value.Split(',')) {
                    $thermalParts = $thermalPair.Split('=', 2)
                    if ($thermalParts.Count -eq 2) { $thermalRow[$thermalParts[0]] = $thermalParts[1] }
                }
                [pscustomobject]$thermalRow
            })
            if ($thermalRows.Count) { $thermalRows | Export-Csv -LiteralPath (Join-Path $thermalRoot "$thermalName.$($thermalKind.ToLowerInvariant()).csv") -NoTypeInformation -Encoding utf8 }
        }
        Write-Output ($thermalResult | ConvertTo-Json -Compress)
    }
}
$thermalSummary = [ordered]@{
    label=$Label; compileExit=$thermalCompileExit; java=$JavaDirectory; heap='4g'; availableProcessors=5; thermalWorkers=4
    seed='755111011717574879'; settings=$thermalValues; reusedCompilation=[bool]$SkipCompile
    gradleInit=$GradleInit; referenceClimate=$ReferenceClimate; worldType=$WorldType; results=@($thermalResults.ToArray())
    precompiledDirectory=$PrecompiledDirectory; mainClassesDirectory=$MainClassesDirectory; nativeCpu=[bool]$NativeCpu; profile=[bool]$Profile
}
[IO.File]::WriteAllText((Join-Path $thermalRoot 'summary.json'), ($thermalSummary | ConvertTo-Json -Depth 8), [Text.UTF8Encoding]::new($false))
Write-Output "Baseline results: $thermalRoot"
if ($thermalCompileExit -ne 0 -or @($thermalResults | Where-Object { -not $_.qualified }).Count) { exit 1 }
