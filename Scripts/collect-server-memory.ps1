# Collect evidence from an existing server. Does not launch, rebuild or change the server.
[CmdletBinding(DefaultParameterSetName = 'Watch')]
param(
    [Parameter(ParameterSetName = 'Watch')][ValidateRange(0, 2147483647)][int]$ServerProcessId = 0,
    [Parameter(Mandatory = $true, ParameterSetName = 'Watch')][string]$ServerDirectory,
    [Parameter(Mandatory = $true, ParameterSetName = 'Inspect')][string]$InspectGcLog,
    [string]$OutputDirectory,
    [string]$JcmdPath,
    [ValidateRange(5, 60)][int]$IntervalSeconds = 15,
    [ValidateRange(0, 1440)][int]$DurationMinutes = 0,
    [ValidateRange(0.5, 0.95)][double]$HighAfterGcRatio = 0.8,
    [ValidateRange(1, 10)][int]$HighGcCount = 3,
    [switch]$CaptureHeapNow
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Convert-GcSize([string]$Value, [string]$Unit) {
    $number = [double]::Parse($Value, [Globalization.CultureInfo]::InvariantCulture)
    $scale = switch ($Unit) { 'K' { 1KB }; 'M' { 1MB }; 'G' { 1GB } }
    return [long]($number * $scale)
}

function Read-GcPauses([string[]]$Lines) {
    foreach ($line in $Lines) {
        $match = [regex]::Match($line, '\[(?<time>[^\]]+)\]\[(?<uptime>[\d.]+)s\].*GC\((?<id>\d+)\).*Pause (?:Young|Full).* (?<before>[\d.]+)(?<bu>[KMG])->(?<after>[\d.]+)(?<au>[KMG])\((?<capacity>[\d.]+)(?<cu>[KMG])\)')
        if (!$match.Success) { continue }
        [pscustomobject]@{
            time = $match.Groups['time'].Value
            gcId = [int]$match.Groups['id'].Value
            beforeBytes = Convert-GcSize $match.Groups['before'].Value $match.Groups['bu'].Value
            afterBytes = Convert-GcSize $match.Groups['after'].Value $match.Groups['au'].Value
            committedBytes = Convert-GcSize $match.Groups['capacity'].Value $match.Groups['cu'].Value
        }
    }
}

# Windows CRT argument quoting. No command interpreter or Invoke-Expression is used.
function ConvertTo-NativeArgument([string]$Value) {
    if ($Value -notmatch '[\s"]') { return $Value }
    $quoted = [regex]::Replace($Value, '(\\*)"', '$1$1\"')
    $quoted = [regex]::Replace($quoted, '(\\+)$', '$1$1')
    return '"' + $quoted + '"'
}

if ($PSCmdlet.ParameterSetName -eq 'Inspect') {
    $taskRows = @(Read-GcPauses (Get-Content -LiteralPath $InspectGcLog))
    [pscustomobject]@{ file = (Resolve-Path -LiteralPath $InspectGcLog).Path; gcPauses = $taskRows } |
        ConvertTo-Json -Depth 5
    return
}

$taskRoot = (Resolve-Path -LiteralPath $ServerDirectory).Path

function Find-DedicatedServerProcess([string]$Root) {
    foreach ($candidate in @(Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'")) {
        $line = [string]$candidate.CommandLine
        if ($line -match 'GradleDaemon|GradleWrapperMain|net\.minecraft\.client\.main\.Main|--launchTarget\s+forgeclient') { continue }
        $dedicated = $line -match '--launchTarget["\s]+forgeserver(?:userdev)?\b|net\.minecraft\.server\.Main\b'
        if (!$dedicated) {
            foreach ($argument in [regex]::Matches($line, '@(?:"(?<quoted>[^"]+)"|(?<plain>[^\s"]+))')) {
                $file = if ($argument.Groups['quoted'].Success) { $argument.Groups['quoted'].Value } else { $argument.Groups['plain'].Value }
                if ($file -notmatch '(?:win_args|unix_args|java-args)\.txt$') { continue }
                if (![IO.Path]::IsPathRooted($file)) { $file = Join-Path $Root $file }
                if (!(Test-Path -LiteralPath $file -PathType Leaf)) { continue }
                if ((Get-Content -LiteralPath $file -Raw) -match '--launchTarget["\s]+forgeserver(?:userdev)?\b') { $dedicated = $true; break }
            }
        }
        if ($dedicated) { $candidate }
    }
}

if ($ServerProcessId -eq 0) {
    $taskCandidates = @(Find-DedicatedServerProcess $taskRoot)
    if ($taskCandidates.Count -ne 1) {
        throw "Expected one identifiable dedicated server; found $($taskCandidates.Count). Supply -ServerProcessId explicitly."
    }
    $ServerProcessId = [int]$taskCandidates[0].ProcessId
}
$taskServer = Get-CimInstance Win32_Process -Filter "ProcessId=$ServerProcessId"
if (!$taskServer -or $taskServer.Name -notmatch '^javaw?\.exe$') { throw 'The specified process is not a running Java server.' }
$taskServerStarted = (Get-Process -Id $ServerProcessId).StartTime
if ($taskServer.CommandLine -match 'GradleDaemon|GradleWrapperMain|net\.minecraft\.client\.main\.Main|--launchTarget\s+forgeclient') {
    throw 'Refusing to collect from a Gradle or Minecraft client process. Specify the dedicated server PID.'
}
if (!$JcmdPath) {
    if (!$taskServer.ExecutablePath) { throw 'Cannot read the Java executable path; supply -JcmdPath explicitly.' }
    $JcmdPath = Join-Path (Split-Path -Parent $taskServer.ExecutablePath) 'jcmd.exe'
}
if (!(Test-Path -LiteralPath $JcmdPath -PathType Leaf)) { throw "jcmd.exe not found: $JcmdPath" }
if (!$CaptureHeapNow -and @(Get-ChildItem -LiteralPath $taskRoot -File -Filter "gc-$ServerProcessId-*.log*").Count -eq 0) {
    throw 'No GC log for this PID was found in ServerDirectory. Check the diagnostic JVM arguments and actual server working directory, or explicitly use -CaptureHeapNow.'
}
if (!$OutputDirectory) { $OutputDirectory = Join-Path $taskRoot ('memory-evidence-' + $ServerProcessId + '-' + (Get-Date -Format 'yyyyMMdd-HHmmss')) }
if (Test-Path -LiteralPath $OutputDirectory) { throw 'Use a new output directory; existing evidence will not be overwritten.' }
$taskOutput = (New-Item -ItemType Directory -Path $OutputDirectory).FullName

function Invoke-Diagnostic([string]$Command, [string]$Name, [int]$TimeoutSeconds = 30) {
    $destination = Join-Path $taskOutput ($Name + '.txt')
    $errors = Join-Path $taskOutput ($Name + '.stderr.txt')
    # A complete diagnostic command is one argv item, preserving its inner path quotes for jcmd.
    $arguments = (ConvertTo-NativeArgument ([string]$ServerProcessId)) + ' ' + (ConvertTo-NativeArgument $Command)
    $taskProbe = Start-Process -FilePath $JcmdPath -ArgumentList $arguments -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput $destination -RedirectStandardError $errors
    if (!$taskProbe.WaitForExit($TimeoutSeconds * 1000)) {
        $taskProbe.Kill() # Only this jcmd client; never the server process.
        throw "Diagnostic command timed out: $Command. A requested VM operation may still be completing."
    }
    $taskProbe.WaitForExit()
    if ($taskProbe.ExitCode -ne 0) { throw "Diagnostic command failed: $Command. See $errors" }
    return Get-Content -LiteralPath $destination -Raw
}

function Save-ServerLogs([string]$Prefix) {
    $latest = Join-Path $taskRoot 'logs/latest.log'
    if (Test-Path -LiteralPath $latest) { Copy-Item -LiteralPath $latest -Destination (Join-Path $taskOutput ($Prefix + '-latest.log')) }
    foreach ($log in @(Get-ChildItem -LiteralPath $taskRoot -File -Filter "gc-$ServerProcessId-*.log*")) {
        Copy-Item -LiteralPath $log.FullName -Destination (Join-Path $taskOutput ($Prefix + '-' + $log.Name))
    }
}

$taskFlags = Invoke-Diagnostic 'VM.flags' 'vm-flags'
$taskVersion = Invoke-Diagnostic 'VM.version' 'vm-version'
$taskMaximum = [regex]::Match($taskFlags, '\bMaxHeapSize=(\d+)')
if (!$taskMaximum.Success) { throw 'Cannot determine MaxHeapSize from VM.flags; automatic heap evidence capture is disabled.' }
$taskMaxHeap = [long]$taskMaximum.Groups[1].Value
$taskMods = Join-Path $taskRoot 'mods'
$taskModList = @()
if (Test-Path -LiteralPath $taskMods) {
    $taskModList = @(Get-ChildItem -LiteralPath $taskMods -File -Filter '*.jar' | ForEach-Object {
        [pscustomobject]@{ name = $_.Name; bytes = $_.Length; sha256 = $(if ($_.Name -match 'frosted.?heart') { (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash } else { $null }) }
    })
}
[pscustomobject]@{ serverPid = $ServerProcessId; serverDirectory = $taskRoot; java = $taskServer.ExecutablePath;
    maximumHeapBytes = $taskMaxHeap; highAfterGcRatio = $HighAfterGcRatio; highGcCount = $HighGcCount;
    capturedAt = (Get-Date).ToString('o'); mods = $taskModList } |
    ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $taskOutput 'metadata.json') -Encoding UTF8
Save-ServerLogs 'start'

function Capture-HeapEvidence([string]$Reason) {
    $taskFreeBytes = (Get-Item -LiteralPath $taskOutput).PSDrive.Free
    if ($null -ne $taskFreeBytes -and $taskFreeBytes -lt $taskMaxHeap + 1GB) {
        throw 'Not enough free space for a live heap dump. Choose an output drive with at least maximum heap + 1GiB free.'
    }
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
    $Reason | Set-Content -LiteralPath (Join-Path $taskOutput ($stamp + '-capture-reason.txt')) -Encoding UTF8
    Save-ServerLogs $stamp
    $null = Invoke-Diagnostic 'Thread.print -l' ($stamp + '-threads') 60
    $null = Invoke-Diagnostic 'GC.class_histogram -all' ($stamp + '-all-objects') 120
    $heapFile = Join-Path $taskOutput ($stamp + '-live.hprof')
    $null = Invoke-Diagnostic ('GC.heap_dump "' + $heapFile + '"') ($stamp + '-heap-dump') 300
    Write-Host "Live heap captured: $heapFile"
}

$taskLastGcId = -1
$taskConsecutiveHigh = 0
$taskCaptured = $false
$taskStarted = Get-Date
Write-Host "Watching server PID $ServerProcessId. Evidence: $taskOutput"
Write-Host 'Normal sampling does not force GC. Live heap capture pauses the JVM and can require several GiB of disk.'
try {
    if ($CaptureHeapNow) { $taskCaptured = $true; Capture-HeapEvidence 'Requested immediate live heap capture.' }
    while (Get-Process -Id $ServerProcessId -ErrorAction SilentlyContinue) {
        if ($DurationMinutes -gt 0 -and ((Get-Date) - $taskStarted).TotalMinutes -ge $DurationMinutes) { break }
        $stamp = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
        $taskHeap = Invoke-Diagnostic 'GC.heap_info' ($stamp + '-heap-info')
        $taskProcess = Get-Process -Id $ServerProcessId -ErrorAction Stop
        if ($taskProcess.StartTime -ne $taskServerStarted) { break } # Never follow a reused PID.
        $taskGcRows = @()
        foreach ($log in @(Get-ChildItem -LiteralPath $taskRoot -File -Filter "gc-$ServerProcessId-*.log*")) {
            $taskGcRows += @(Read-GcPauses (Get-Content -LiteralPath $log.FullName -Tail 2000))
        }
        foreach ($row in @($taskGcRows | Sort-Object gcId -Unique)) {
            if ($row.gcId -le $taskLastGcId) { continue }
            $taskLastGcId = $row.gcId
            if ($row.afterBytes -ge $taskMaxHeap * $HighAfterGcRatio) { $taskConsecutiveHigh++ } else { $taskConsecutiveHigh = 0 }
        }
        $taskUsage = [regex]::Match($taskHeap, 'heap\s+total\s+(\d+)K,\s+used\s+(\d+)K')
        [pscustomobject]@{ time = (Get-Date).ToString('o'); gcId = $taskLastGcId;
            heapUsedBytes = $(if ($taskUsage.Success) { [long]$taskUsage.Groups[2].Value * 1KB } else { $null });
            privateBytes = $taskProcess.PrivateMemorySize64; workingSetBytes = $taskProcess.WorkingSet64;
            threadCount = $taskProcess.Threads.Count; consecutiveHighAfterGc = $taskConsecutiveHigh } |
            ConvertTo-Json -Compress | Add-Content -LiteralPath (Join-Path $taskOutput 'samples.jsonl') -Encoding UTF8
        if (!$taskCaptured -and $taskConsecutiveHigh -ge $HighGcCount) {
            $taskCaptured = $true # At most one capture, including a failed or timed-out attempt.
            Capture-HeapEvidence "After-GC occupancy exceeded $HighAfterGcRatio of the maximum heap for $HighGcCount observed collections."
        }
        Start-Sleep -Seconds $IntervalSeconds
    }
} finally {
    Save-ServerLogs 'finish'
    Write-Host "Evidence retained in $taskOutput. The collector has not stopped the server."
}
