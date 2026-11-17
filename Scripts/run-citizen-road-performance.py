"""Run the town-road performance matrix against a freshly started agent client at the title screen.

From the repository root:
  ./gradlew --offline runClient -I Scripts/dev_client_bridge.init.gradle
  python3 Scripts/run-citizen-road-performance.py
The runner creates a disposable world, restores options and closes the client on completion.
"""
import sys,json,time,traceback,argparse,datetime
from pathlib import Path
sys.path.insert(0,'Scripts')
from agent_client import send
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--world',default='citizen-road-perf-'+datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%d-%H%M%S'))
parser.add_argument('--output',default='build/citizen-road-performance-'+datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%d-%H%M%S'))
options=parser.parse_args()
out=Path(options.output);out.mkdir(parents=True,exist_ok=True)
raw=out/'commands.jsonl';summary={'cases':[]}
def run(cmd,timeout=240):
 r=send('run/agent',cmd,timeout)
 with raw.open('a') as f:f.write(json.dumps(r,ensure_ascii=False)+'\n')
 if not r.get('ok'):raise RuntimeError(r)
 return r

def save(): (out/'results.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2))
def phase(label,count,backend,mode,offsets=True):
 print('START '+label,flush=True)
 run('command /citizen_debug backend '+backend)
 run('roads perf mode '+mode)
 run('roads perf offsets '+str(offsets).lower())
 run('wait 200')
 status=run('roads status')['roads']
 assert status['clientCitizens']==count,(label,status['clientCitizens'],count)
 assert status['presented']==(count if offsets else 0),(label,status['presented'])
 run('roads perf begin '+label)
 run('wait 400')
 result=run('roads perf end')['roads'];result['population']=count
 assert result['serverTick']['samples']>100,result
 result['estimatedFPS']=1000/result['frameInterval']['meanMs'] if result['frameInterval']['meanMs'] else 0
 result['observedTPS']=result['serverTick']['samples']/result['wallSeconds']
 summary['cases'].append(result);save()
 print(json.dumps({k:result[k] for k in ['label','population','backend','meanPositionChanges','estimatedFPS','observedTPS','serverTick','clientTick','frameInterval','gcMs']},ensure_ascii=False),flush=True)
initial=run('state',30)
if initial.get('state',{}).get('inWorld'):
 raise SystemExit('Start a fresh development client at the title screen; this runner will not close an existing world.')
try:
 run('newworld '+options.world+' flat 7152026',600)
 run('roads perf settings')
 summary['hardware']=run('roads perf hardware')['roads'];save()
 t=time.monotonic();run('roads setup')
 for i in range(60):
  run('wait 20');status=run('roads status')['roads']
  if status['workingBuildings']==4 and status['routes']==3:break
 else:raise RuntimeError('road graph did not become ready')
 summary['fixtureBootstrapSeconds']=time.monotonic()-t
 summary['graphMicrobenchmark']=run('roads perf graph')['roads'];save()
 run('lookat 16 67 12')
 phase('n0-flywheel-idle',0,'flywheel','idle')
 run('roads perf populate 100')
 phase('n100-flywheel-moving',100,'flywheel','moving')
 run('roads perf populate 1000')
 phase('n1000-flywheel-idle',1000,'flywheel','idle')
 phase('n1000-flywheel-moving',1000,'flywheel','moving')
 phase('n1000-flywheel-moving-no-offsets',1000,'flywheel','moving',False)
 phase('n1000-cpu-moving',1000,'cpu_batch','moving')
 # Second on/off pair checks ordering and cache effects without population changes.
 phase('n1000-flywheel-moving-repeat',1000,'flywheel','moving')
 run('roads perf jfr start')
 run('wait 400')
 summary['jfr']=run('roads perf jfr stop')['roads'];save()
 summary['screenshot']=run('screenshot citizen-road-performance-1000').get('file');save()
except Exception as error:
 summary['error']=str(error);save();traceback.print_exc();raise
finally:
 try:run('roads perf restore',60)
 except Exception:traceback.print_exc()
 try:run('quit',120)
 except Exception:traceback.print_exc()
