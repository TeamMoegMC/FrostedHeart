"""Run existing server artifacts with external KubeJS player/chunk churn; never invokes Gradle."""
import argparse, hashlib, json, os, re, shutil, subprocess, time, zipfile
import psutil
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--variant', choices=['old', 'current'], required=True)
parser.add_argument('--artifact', type=Path, help='Use another already-built deobfuscated JAR')
parser.add_argument('--classes-directory', type=Path, help='Overlay incrementally compiled production classes without packaging a new JAR')
parser.add_argument('--active-processors', type=int, default=4)
parser.add_argument('--minutes', type=int, default=20)
parser.add_argument('--cpu-budget', type=int, default=250)
parser.add_argument('--sources-per-base', type=int, default=64)
parser.add_argument('--geometry', choices=['open', 'pockets'], default='open',
                    help='Pockets reuse the real stone/source geometry from the existing overload regression')
parser.add_argument('--resident-player', action='store_true', help='Keep one player at the heated base while two explore')
parser.add_argument('--warmup-ticks', type=int, default=400)
parser.add_argument('--mutations-per-cycle', type=int, default=0,
                    help='Replace this many loaded campfire foundation blocks per base each mutation cycle')
parser.add_argument('--mutation-interval', type=int, default=20)
parser.add_argument('--mutation-stop-tick', type=int, default=0,
                    help='Stop scripted block changes at this tick, leaving the server running to observe recovery; zero keeps changing')
parser.add_argument('--view-distance', type=int, default=10)
parser.add_argument('--native-checkpoint', action=argparse.BooleanOptionalAction, default=True,
                    help='Match native_ready in the incident log; --no-native-checkpoint is a separate control')
parser.add_argument('--root', type=Path, default=REPO/'build/thermal-oom-repro-20261109')
parser.add_argument('--prepare-only', action='store_true')
parser.add_argument('--capture-heap', action='store_true', help='Capture one live heap on sustained high post-GC use, or at the end')
parser.add_argument('--capture-at-end-only', action='store_true', help='Avoid a diagnostic Full GC before the workload finishes')
parser.add_argument('--snapshot-tick', type=int, default=0, help='One all-object heap snapshot at this game tick, without forcing GC')
options = parser.parse_args()
if options.minutes <= 0 or options.sources_per_base < 0 or options.warmup_ticks < 0 or options.active_processors <= 0 or not 2 <= options.view_distance <= 32 or options.mutations_per_cycle < 0 or options.mutation_interval <= 0 or options.mutation_stop_tick < 0:
    parser.error('Require positive minutes, nonnegative sources and view distance 2..32')
root = options.root.resolve()
runtime = root/f'{options.variant}-cpu{options.cpu_budget}'
artifact = REPO/({'old':'build/libs/frostedheart-1.20.1-0.7.10-deobf.jar',
                 'current':'build/thermal-overload-20261108/libs/frostedheart-1.20.1-0.7.12-deobf.jar'}[options.variant])
if options.artifact is not None: artifact = options.artifact.resolve()
if runtime.exists():
    raise SystemExit(f'Refusing to overwrite an existing run: {runtime}')
runtime.mkdir(parents=True)
mod = runtime/'existing-mod'
mod.mkdir()
with zipfile.ZipFile(artifact) as archive:
    for entry in archive.infolist():
        target = (mod/entry.filename).resolve()
        if not target.is_relative_to(mod.resolve()):
            raise ValueError(f'Archive entry escapes the disposable mod folder: {entry.filename}')
    archive.extractall(mod)
classes_digest = None
if options.classes_directory is not None:
    classes_directory = options.classes_directory.resolve()
    if not classes_directory.is_dir(): raise ValueError(f'Compiled classes directory not found: {classes_directory}')
    digest = hashlib.sha256()
    for path in sorted(classes_directory.rglob('*')):
        if path.is_file():
            digest.update(path.relative_to(classes_directory).as_posix().encode('utf-8')+b'\0')
            digest.update(path.read_bytes())
    classes_digest = digest.hexdigest()
    shutil.copytree(classes_directory, mod, dirs_exist_ok=True)
manifest = json.loads((REPO/'build/infrared-cooling-open-e2e-20261108/launch-server.json').read_text(encoding='utf-8-sig'))
original = manifest['command']
old_cp = original[original.index('-cp') + 1].split(';')
def project_classes(path):
    value = path.replace('\\','/').lower()
    return str(REPO/'build').replace('\\','/').lower() in value and ('/classes/java/' in value or '/resources/' in value)
cp = []
for entry in old_cp:
    if project_classes(entry):
        if str(mod) not in cp: cp.append(str(mod))
    else: cp.append(entry)
classpath = ';'.join(cp)
command = []
skip = False
for index, arg in enumerate(original[1:]):
    if skip: skip = False; continue
    if arg == '-cp': command.extend(['-cp', classpath]); skip = True; continue
    if arg.startswith('-DlegacyClassPath='): command.append('-DlegacyClassPath='+classpath); continue
    if arg.startswith(('-Dfrostedheart.irE2e', '-Dforge.enableGameTest=', '-Xmx', '-Xms', '-XX:ActiveProcessorCount=')): continue
    command.append(arg)
command = ['-Xms4G','-Xmx4G','-XX:ActiveProcessorCount='+str(options.active_processors),'-XX:+HeapDumpOnOutOfMemoryError',
           '-XX:HeapDumpPath='+str(runtime),
           '-Xlog:gc*:file=gc-%p-%t.log:time,uptime,level,tags:filecount=3,filesize=20M',
           '-Dfh.memoryRepro=true', '-Dfh.repro.minutes='+str(options.minutes),
           '-Dfh.repro.heatSources='+str(options.sources_per_base)] + command
java = Path('C:/Program Files/Java/jdk-21/bin/java.exe')
environment = os.environ.copy()
environment.update(manifest.get('environment', {}))
environment['MOD_CLASSES'] = 'frostedheart%%'+str(mod)
(runtime/'config').mkdir()
for path in (REPO/'run/config').glob('*'):
    if path.is_file() and path.suffix in ('.toml', '.json', '.properties'): shutil.copy2(path, runtime/'config'/path.name)
config = runtime/'config/frostedheart-common.toml'
text = config.read_text(encoding='utf-8-sig')
text = re.sub(r'(continuousAirCpuBudgetMsPerSecond\s*=\s*)\d+', lambda m:m[1]+str(options.cpu_budget), text)
text = re.sub(r'(thermalMemoryTargetMiB\s*=\s*)\d+', lambda m:m[1]+'128', text)
text, native_keys = re.subn(r'(continuousAirNativeCheckpoint\s*=\s*)(?:true|false)',
                          lambda m:m[1]+str(options.native_checkpoint).lower(), text)
if native_keys != 1:
    raise ValueError('Expected one explicit native checkpoint configuration key')
config.write_text(text, encoding='utf-8')
shutil.copytree(REPO/'src/gametest/catalog', runtime/'config/fhresearches')
scripts = runtime/'kubejs/server_scripts'; scripts.mkdir(parents=True)
scenario = (REPO/'Scripts/thermal-oom-repro.js').read_text(encoding='utf-8')
scenario = scenario.replace('var heatCount = 64', f'var heatCount = {options.sources_per_base}')
scenario = scenario.replace('var minutes = 20', f'var minutes = {options.minutes}')
scenario = scenario.replace("var sourceGeometry = 'open'", f"var sourceGeometry = '{options.geometry}'")
scenario = scenario.replace('var residentPlayer = false', 'var residentPlayer = '+str(options.resident_player).lower())
scenario = scenario.replace('var warmupTicks = 400', f'var warmupTicks = {options.warmup_ticks}')
scenario = scenario.replace('var mutationsPerCycle = 0', f'var mutationsPerCycle = {options.mutations_per_cycle}')
scenario = scenario.replace('var mutationInterval = 20', f'var mutationInterval = {options.mutation_interval}')
scenario = scenario.replace('var mutationStopTick = 0', f'var mutationStopTick = {options.mutation_stop_tick}')
(scripts/'thermal-oom-repro.js').write_text(scenario, encoding='utf-8')
(runtime/'eula.txt').write_text('eula=true\n')
properties = {'server-ip':'127.0.0.1','server-port':'25583' if options.variant=='old' else '25584',
              'online-mode':'false','enforce-secure-profile':'false','level-name':'reproduction-world',
              'level-seed':'71108222452','level-type':'minecraft:normal','gamemode':'survival','difficulty':'normal',
              'view-distance':str(options.view_distance),'simulation-distance':'8','max-players':'4',
              'spawn-protection':'0','allow-flight':'true','max-tick-time':'60000'}
(runtime/'server.properties').write_text(''.join(f'{key}={value}\n' for key,value in properties.items()))
def java_arg(value): return '"'+value.replace('\\','\\\\').replace('"','\\"')+'"'
argfile = runtime/'java-args.txt'; argfile.write_text('\n'.join(map(java_arg,command)),encoding='utf-8')
meta = {'variant':options.variant,'artifact':str(artifact),'artifactSha256':hashlib.sha256(artifact.read_bytes()).hexdigest(),
        'java':str(java),'heap':'4GiB','cpuBudgetMsPerSecond':options.cpu_budget,'thermalTargetMiB':128,
        'seed':properties['level-seed'],'viewDistance':options.view_distance,'minutes':options.minutes,
        'sourcesPerBase':options.sources_per_base,'gameTestClassesExcluded':True,
        'nativeCheckpoint':options.native_checkpoint,
        'sourceGeometry':options.geometry,'residentPlayer':options.resident_player,'warmupTicks':options.warmup_ticks,
        'mutationsPerBasePerCycle':options.mutations_per_cycle,'mutationIntervalTicks':options.mutation_interval,
        'mutationStopTick':options.mutation_stop_tick,
        'scenarioSha256':hashlib.sha256(scenario.encode('utf-8')).hexdigest(),
        'configSha256':hashlib.sha256(config.read_bytes()).hexdigest(),
        'activeProcessorCount':options.active_processors,'midpointAllObjectSnapshotTick':options.snapshot_tick,
        'compiledClassesDirectory':str(options.classes_directory.resolve()) if options.classes_directory else None,
        'compiledClassesSha256':classes_digest,
        'scope':'Three server player objects with vanilla chunk tracking; no real network clients and no original server world.'}
(runtime/'run-metadata.json').write_text(json.dumps(meta,indent=2),encoding='utf-8')
print(json.dumps({'runtime':str(runtime), **meta},indent=2),flush=True)
if options.prepare_only: raise SystemExit(0)
flags = subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0
def gc_samples(pid):
    rows = {}
    for path in runtime.glob(f'gc-{pid}-*.log*'):
        with path.open('rb') as source:
            source.seek(max(0,path.stat().st_size-65536)); data=source.read().decode('utf-8',errors='replace')
        for match in re.finditer(r'GC\((\d+)\).*Pause (?:Young|Full).* [\d.]+[KMG]->([\d.]+)([KMG])\(',data):
            rows[int(match[1])] = int(float(match[2])*{'K':1024,'M':1024**2,'G':1024**3}[match[3]])
    return sorted(rows.items())
def disk_chunks():
    count=0
    for path in (runtime/'reproduction-world/region').glob('*.mca'):
        with path.open('rb') as source: header=source.read(4096)
        count += sum(header[index:index+4] != b'\0\0\0\0' for index in range(0,len(header)-3,4))
    return count
def capture_heap(pid, reason):
    (runtime/'heap-capture-reason.txt').write_text(reason,encoding='utf-8')
    jcmd=java.with_name('jcmd.exe')
    for name,command,timeout in [('threads','Thread.print -l',60),('all-objects','GC.class_histogram -all',120),
                                 ('heap-dump','GC.heap_dump "'+str(runtime/'live.hprof')+'"',300)]:
        with (runtime/f'{name}.txt').open('w',encoding='utf-8') as output:
            result=subprocess.run([str(jcmd),str(pid),command],stdout=output,stderr=subprocess.STDOUT,
                                  timeout=timeout,creationflags=flags)
            if result.returncode: raise RuntimeError(f'Failed to capture {name}')
        if name == 'heap-dump' and 'Dump file is incomplete' in (runtime/f'{name}.txt').read_text(encoding='utf-8',errors='replace'):
            raise RuntimeError('JVM reported an incomplete heap dump; preserve the capture error instead of qualifying this run')
with (runtime/'console.log').open('w',encoding='utf-8') as log:
    process = subprocess.Popen([str(java),'@'+str(argfile)],cwd=runtime,env=environment,
                               stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT,creationflags=flags)
    (runtime/'process.json').write_text(json.dumps({'pid':process.pid,'started':time.time()}),encoding='utf-8')
    print('SERVER_PID',process.pid,flush=True)
    deadline = time.monotonic() + options.minutes*60 + 300
    last_sample = None
    console_offset = 0
    console_partial = ''
    last_gc=-1; high_gc=0; heap_captured=False; midpoint_captured=False
    outcome='process_exited_before_workload_completed'
    def record_console_line(line):
        if any(marker in line for marker in ['Thermal dimension worker failed for ', 'Uncaught thermal worker task failure', 'Thermal close failed after retaining', 'Failed to save chunk ']):
            failure_file=runtime/'runtime-failed.json'
            if not failure_file.exists():
                failure_file.write_text(json.dumps({'error':line,'lastObservedTick':last_sample}),encoding='utf-8')
        if 'java.lang.OutOfMemoryError:' in line:
            (runtime/'oom-failed.json').write_text(json.dumps({'error':line,'lastObservedTick':last_sample}),encoding='utf-8')
        if re.search(r'thermal-oom-repro\.js#\d+: (?:Failed to load|Error in)', line) or "[KubeJS Server/]: Error in 'ServerEvents.tick'" in line:
            (runtime/'scenario-failed.json').write_text(json.dumps({'error':line}),encoding='utf-8')
        event=re.search(r'FH_REPRO_EVENT ([\w.-]+) (\{.*\})\s*$',line)
        if event:
            try:
                value=json.loads(event[2]); (runtime/event[1]).write_text(json.dumps(value),encoding='utf-8')
            except json.JSONDecodeError: pass
    try:
        while process.poll() is None and time.monotonic() < deadline:
            with (runtime/'console.log').open('rb') as source:
                source.seek(console_offset); new_text=source.read(); console_offset=source.tell()
            console_partial += new_text.decode('utf-8',errors='replace')
            lines=console_partial.split('\n'); console_partial=lines.pop()
            for line in lines:
                record_console_line(line)
            if (runtime/'oom-failed.json').exists():
                outcome='jvm_out_of_memory'; break
            if (runtime/'scenario-failed.json').exists():
                outcome='invalid_scenario'
                print('SCENARIO_FAILED', (runtime/'scenario-failed.json').read_text(),flush=True); break
            if (runtime/'runtime-failed.json').exists():
                outcome='thermal_runtime_failed'
                print('THERMAL_RUNTIME_FAILED', (runtime/'runtime-failed.json').read_text(),flush=True)
                if options.capture_heap and not list(runtime.glob('java_pid*.hprof')):
                    heap_captured=True
                    with (runtime/'runtime-failure-threads.txt').open('w',encoding='utf-8') as output:
                        subprocess.run([str(java.with_name('jcmd.exe')),str(process.pid),'Thread.print -l'],
                            stdout=output,stderr=subprocess.STDOUT,timeout=30,creationflags=flags)
                    with (runtime/'runtime-failure-dump.txt').open('w',encoding='utf-8') as output:
                        subprocess.run([str(java.with_name('jcmd.exe')),str(process.pid),
                            'GC.heap_dump -all "'+str(runtime/'runtime-failure-all.hprof')+'"'],
                            stdout=output,stderr=subprocess.STDOUT,timeout=180,creationflags=flags)
                break
            telemetry = runtime/'telemetry.json'
            if telemetry.exists():
                try:
                    value=json.loads(telemetry.read_text(encoding='utf-8-sig'))
                    if value.get('ticks') != last_sample:
                        last_sample=value.get('ticks')
                        observed=gc_samples(process.pid)
                        for number,after in observed:
                            if number<=last_gc: continue
                            last_gc=number; high_gc=high_gc+1 if after>=4*1024**3*0.8 else 0
                        memory=psutil.Process(process.pid).memory_info()
                        value.update({'time':time.time(),'afterGcBytes':observed[-1][1] if observed else None,
                                      'rssBytes':memory.rss,'privateBytes':getattr(memory,'private',memory.vms),
                                      'actualThreadCount':psutil.Process(process.pid).num_threads(),'chunksOnDisk':disk_chunks()})
                        sample=json.dumps(value,separators=(',',':'))
                        with (runtime/'telemetry-samples.jsonl').open('a',encoding='utf-8') as samples: samples.write(sample+'\n')
                        print(sample,flush=True)
                        if options.snapshot_tick > 0 and last_sample >= options.snapshot_tick and not midpoint_captured:
                            midpoint_captured=True
                            with (runtime/'midrun-dump.txt').open('w',encoding='utf-8') as output:
                                result=subprocess.run([str(java.with_name('jcmd.exe')),str(process.pid),
                                    'GC.heap_dump -all "'+str(runtime/'midrun-all.hprof')+'"'],
                                    stdout=output,stderr=subprocess.STDOUT,timeout=180,creationflags=flags)
                            if result.returncode: raise RuntimeError('Midpoint snapshot failed')
                            print('MIDPOINT_ALL_OBJECT_SNAPSHOT',last_sample,flush=True)
                        if options.capture_heap and not options.capture_at_end_only and high_gc>=3 and not heap_captured:
                            heap_captured=True; capture_heap(process.pid,'Three high post-GC observations during roaming load')
                except (json.JSONDecodeError,OSError): pass
            if (runtime/'scenario-finished.json').exists():
                finished=json.loads((runtime/'scenario-finished.json').read_text())
                if finished.get('ticks') != options.minutes*1200:
                    raise RuntimeError('Scenario completion receipt does not match the requested workload')
                if options.capture_heap and not heap_captured:
                    heap_captured=True; capture_heap(process.pid,'End of completed reproduction load; not an OOM attribution by itself')
                outcome='workload_completed'
                print('SCENARIO_FINISHED',flush=True); break
            time.sleep(5)
        if process.poll() is None and outcome=='process_exited_before_workload_completed':
            outcome='wall_deadline_before_workload_completed'
            if options.capture_heap and not heap_captured:
                heap_captured=True; capture_heap(process.pid,'Wall deadline before game-tick completion; incomplete workload')
    except Exception as error:
        outcome='runner_error'
        (runtime/'runner-error.json').write_text(json.dumps({'error':str(error)}),encoding='utf-8')
        raise
    finally:
        if process.poll() is None:
            try: process.stdin.write(b'stop\n'); process.stdin.flush(); process.wait(timeout=90)
            except (OSError, subprocess.TimeoutExpired): process.terminate(); process.wait(timeout=30)
        # Include failures during the last heap capture and normal shutdown, or
        # immediately before a process exit between two telemetry polls.
        with (runtime/'console.log').open('rb') as source:
            source.seek(console_offset)
            for line in (console_partial+source.read().decode('utf-8',errors='replace')).splitlines():
                record_console_line(line)
        if (runtime/'oom-failed.json').exists(): outcome='jvm_out_of_memory'
        elif (runtime/'runtime-failed.json').exists(): outcome='thermal_runtime_failed'
        elif (runtime/'scenario-failed.json').exists(): outcome='invalid_scenario'
        elif process.returncode != 0 and outcome == 'workload_completed': outcome='process_failed_after_workload'
        (runtime/'exit.json').write_text(json.dumps({'exitCode':process.returncode,'ended':time.time(),
                  'outcome':outcome,'lastObservedTick':last_sample,'heapCaptureAttempted':heap_captured}),encoding='utf-8')
    print('SERVER_EXIT',process.returncode,flush=True)
