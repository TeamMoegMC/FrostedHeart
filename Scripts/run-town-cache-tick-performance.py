"""Measure server-main-thread tick cost in a disposable saved 32-building natural-world fixture.

Start a fresh profiling client at the title screen with:
  ./gradlew --offline runClient -I Scripts/dev_client_bridge.init.gradle -I Scripts/town-path-profile.init.gradle -PtownPathHeap=4G
Use only the disposable fixture made by run-town-path-performance.py, not a player save.
Options are restored and the client is closed after measurement.
"""
import sys,json,time,traceback,argparse,datetime
from pathlib import Path
sys.path.insert(0,'Scripts')
from agent_client import send
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--world',default='town-path-natural-20261113')
parser.add_argument('--output',default='build/town-cache-tick-performance-'+datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%d-%H%M%S'))
args=parser.parse_args()
out=Path(args.output);out.mkdir(parents=True,exist_ok=True)
data={'world':args.world,'buildingCount':32,'cases':[],'tickBudgetMs':50}
def save():(out/'results.json').write_text(json.dumps(data,ensure_ascii=False,indent=2))
def run(command,timeout=240):
 result=send('run/agent',command,timeout)
 with (out/'commands.jsonl').open('a') as f:f.write(json.dumps(result,ensure_ascii=False)+'\n')
 if not result.get('ok'):raise RuntimeError(result)
 return result

def finish_case(p):
 p['p95CpuBudgetPercent']=p['mainTickCpu']['p95Ms']/50*100
 p['maxCpuBudgetPercent']=p['mainTickCpu']['maxMs']/50*100
 p['p95WallBudgetPercent']=p['mainTickWall']['p95Ms']/50*100
 data['cases'].append(p);save();print(json.dumps(p,ensure_ascii=False),flush=True)

def window(label,ticks):
 run('roads path baseline '+label);run('wait '+str(ticks));run('roads path finish');finish_case(run('roads path status')['roads']['probe'])

def search(label):
 print('START '+label,flush=True);before=run('roads path status')['roads']['probe'].get('cacheStatistics',{})
 run('roads path arm '+label);start=time.monotonic()
 while True:
  run('wait 5');p=run('roads path status')['roads']['probe']
  if p.get('done'):
   assert not p['failed'] and p['workerReturned'] and p['maxSectionsPerTick']<=2,p
   p['cacheBefore']=before
   finish_case(p);return
  if time.monotonic()-start>600:raise RuntimeError('search timed out '+label)
initial=run('state',30)
if initial.get('state',{}).get('inWorld'):raise SystemExit('Start a fresh development client at the title screen; an existing player world will not be closed.')
try:
 run('world '+args.world,600)
 run('roads perf settings');data['hardware']=run('roads perf hardware')['roads'];save()
 data['resume']=run('roads path resume',300)['roads'];save()
 for _ in range(900):
  ready=run('roads path ready')['roads']
  if ready['loadedChunks']==221 and not ready['busy'] and len(ready['entranceCounts'])==32 and ready['entranceCounts'].count(0)<=1:break
  run('wait 20')
 else:raise RuntimeError('world not ready')
 window('idle-before',200)
 run('roads path clearcache');search('cold-1');search('immediate-recompute-1');window('refresh-tail-1',80)
 run('roads path clearcache');search('cold-2');search('immediate-recompute-2')
 run('roads path dirtycache');run('wait 25');search('dirty-grace-recompute');window('refresh-tail-2',80)
 run('wait 160');window('idle-after',200)
except Exception as error:
 data['error']=str(error);save();traceback.print_exc();raise
finally:
 try:data['cleanup']=run('roads path cleanup',180)['roads'];save()
 except Exception:traceback.print_exc()
 try:run('roads perf restore',120)
 except Exception:traceback.print_exc()
 try:run('quit',180)
 except Exception:traceback.print_exc()
