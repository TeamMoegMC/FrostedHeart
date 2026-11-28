"""Profile real TownConnectivity searches in an ordinary natural-terrain world.

Start a fresh development client with both init scripts and -PtownPathHeap=4G:
  ./gradlew --offline runClient -I Scripts/dev_client_bridge.init.gradle -I Scripts/town-path-profile.init.gradle -PtownPathHeap=4G
Then run this script from the repository root. It creates a disposable world,
measures first/repeated searches and real blocked entrances, restores settings and quits.
"""
import sys,json,time,traceback,argparse,datetime
from pathlib import Path
sys.path.insert(0,'Scripts')
from agent_client import send
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--world',default='town-path-natural-'+datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%d-%H%M%S'))
parser.add_argument('--output',default='build/town-path-natural-performance-'+datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%d-%H%M%S'))
options=parser.parse_args()
out=Path(options.output);out.mkdir(parents=True,exist_ok=True)
summary={'world':options.world,'seed':12345,'preset':'normal','cases':[]}
def save(): (out/'results.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2))
def run(cmd,timeout=240):
 r=send('run/agent',cmd,timeout)
 with (out/'commands.jsonl').open('a') as f:f.write(json.dumps(r,ensure_ascii=False)+'\n')
 if not r.get('ok'):raise RuntimeError(r)
 return r

def wait_round(label,limit=900):
 start=time.monotonic();last_print=0
 while True:
  run('wait 20');data=run('roads path status')['roads'];p=data['probe']
  if time.monotonic()-last_print>15:
   print(json.dumps({'case':label,'elapsed':round(p.get('elapsedSeconds',0),1),'served':p.get('sectionsServed'),'requests':p.get('sectionRequests'),'expanded':p.get('expandedNodes'),'done':p.get('done')}),flush=True);last_print=time.monotonic()
  if p.get('done'):
   assert not p['failed'] and p['workerReturned'],p
   assert p['maxSectionsPerTick']<=2,p
   assert p['mainTickCount']>0 and p['workerCpuMs']>0,'profiling mixins did not record a real solver run'
   summary['cases'].append(p);save();print('DONE '+json.dumps(p),flush=True);return p
  if time.monotonic()-start>limit:raise RuntimeError('round limit reached; partial data retained: '+json.dumps(p))
state=run('state',30)
if state.get('state',{}).get('inWorld'): raise SystemExit('Start a fresh development client at the title screen; an existing player world will not be closed.')
try:
 run('newworld '+summary['world']+' normal 12345',600)
 run('roads perf settings')
 summary['hardware']=run('roads perf hardware')['roads'];save()
 started=time.monotonic();prep=run('roads path prepare',300)['roads'];summary['prepare']=prep;save()
 for i in range(180):
  ready=run('roads path ready')['roads']
  if ready['loadedChunks']==ready['forcedChunks'] and ready['forcedChunks']>0:break
  run('wait 20')
 else:raise RuntimeError('chunks did not finish preloading')
 summary['preloadSeconds']=time.monotonic()-started;run('wait 100')
 for count in (8,16,32):
  label=str(count)+'-first';print('START '+label,flush=True)
  build=run('roads path build '+str(count)+' '+label)['roads'];summary.setdefault('builds',[]).append(build);save()
  first=wait_round(label)
  run('wait 40')
  label=str(count)+'-recompute';print('START '+label,flush=True)
  run('roads path arm '+label);wait_round(label)
 # Idle control at the same final 32-building town and same terrain.
 run('roads path baseline 32-idle-control');run('wait 200');run('roads path finish')
 summary['idleControl']=run('roads path status')['roads']['probe'];save()
 print('START 32-blocked',flush=True)
 summary['barrier']=run('roads path barrier')['roads'];save();run('wait 100')
 run('roads path arm 32-blocked');blocked=wait_round('32-blocked')
 assert blocked['working']<32 and blocked['routes']<31, 'barrier case did not produce unreachable buildings'
 run('wait 40')
 run('roads path arm 32-loaded-control');control=wait_round('32-loaded-control')
 sealed=run('roads path seal')['roads'];summary['sealedEntrance']=sealed;save()
 assert sealed['previousWorking']>1,sealed
 sealed_result=wait_round('32-sealed-reachable-entrance')
 # A different equal-size largest component may take over; use forest connectivity, not largest size alone.
 assert sealed_result['routes']<sealed['previousRoutes'],sealed_result
 summary['sealedValidation']={'componentsBefore':32-sealed['previousRoutes'],'componentsAfter':32-sealed_result['routes'],'valid':True};save()
 prep=summary['prepare'];ox,oz=prep['origin'];heights=summary['builds'][-1]['groundHeights'];cy=max(heights)+35
 run(f'command /tp @s {ox+56} {cy} {oz+80}')
 run(f'lookat {ox+56} {sum(heights)/len(heights):.1f} {oz+24}')
 run('key j');run('wait 30');summary['screenshot']=run('screenshot town-path-natural-performance').get('file');save()
except Exception as e:
 summary['error']=str(e);save();traceback.print_exc();raise
finally:
 try:summary['cleanup']=run('roads path cleanup',180)['roads'];save()
 except Exception:traceback.print_exc()
 try:run('roads perf restore',120)
 except Exception:traceback.print_exc()
 try:run('quit',180)
 except Exception:traceback.print_exc()
