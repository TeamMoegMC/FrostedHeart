import json,collections,datetime,argparse,subprocess
from pathlib import Path
parser=argparse.ArgumentParser(description='Summarize a road performance JFR; omit anomalous first allocation samples and record boundaries.')
parser.add_argument('results_dir',type=Path)
args=parser.parse_args();root=args.results_dir
results=json.loads((root/'results.json').read_text());recording=Path(results['jfr']['jfr'])
with (root/'jfr-events-full.json').open('w') as output:
 subprocess.run(['jfr','print','--json','--stack-depth','64','--events','jdk.ExecutionSample,jdk.ObjectAllocationSample,jdk.GarbageCollection,jdk.GCConfiguration',str(recording)],stdout=output,check=True)
events=json.loads((root/'jfr-events-full.json').read_text())['recording']['events']
def tm(s):return datetime.datetime.fromisoformat(s.replace('Z','+00:00')).timestamp()
def frames(v):return [f['method']['type']['name'].replace('/','.')+'.'+f['method']['name'] for f in (v.get('stackTrace')or{}).get('frames',[])]
start=min(tm(x['values']['startTime']) for x in events);end=max(tm(x['values']['startTime']) for x in events);lo=start+2;hi=end-1
threads=collections.Counter();cpu=collections.Counter();inclusive=collections.Counter();owners=collections.Counter();classes=collections.Counter();total=0;gc=[];first=set();excluded=[]
for e in sorted(events,key=lambda x:tm(x['values']['startTime'])):
 v=e['values'];f=frames(v);t=tm(v['startTime'])
 if e['type']=='jdk.GarbageCollection':gc.append(v)
 if e['type']=='jdk.ObjectAllocationSample':
  thread=v.get('eventThread',{}).get('javaThreadId')
  if thread not in first:first.add(thread);excluded.append({'thread':v.get('eventThread',{}).get('javaName'),'weightBytes':v.get('weight',0)});continue
 if not lo<=t<=hi:continue
 if e['type']=='jdk.ExecutionSample':
  thread=v.get('sampledThread',{}).get('javaName','?');threads[thread]+=1
  if f:cpu[(thread,f[0])]+=1
  for family in ('CitizenRoadPresentation','RoadPresentation','TownRoadNavigation','TownRoadNetwork','TownConnectivity','TownRoadPerformance'):
   if any('.'+family+'.' in x for x in f):inclusive[(thread,family)]+=1
 if e['type']=='jdk.ObjectAllocationSample':
  w=v.get('weight',0);total+=w;classes[v.get('objectClass',{}).get('name','?').replace('/','.')]+=w
  owner=next((x for x in f if x.startswith(('com.teammoeg.frostedheart','com.teammoeg.chorda'))),f[0] if f else '?');owners[owner]+=w
result={'steadyWindowSeconds':hi-lo,'boundaryPolicy':'trim first 2s and last 1s; exclude first allocation sample per thread due anomalous accumulated weights','excludedFirstSamples':excluded,'executionSamplesByThread':dict(threads),'inclusiveFamilies':[{'thread':t,'family':f,'samples':n,'percentOfThreadSamples':round(n/threads[t]*100,2)} for (t,f),n in inclusive.most_common()], 'topLeafMethods':[{'thread':t,'method':f,'samples':n} for (t,f),n in cpu.most_common(12)],'allocationSampleWeightBytes':total,'allocationFirstOwner':[{'method':f,'weightBytes':n,'percent':round(n/total*100,2)} for f,n in owners.most_common(12)],'allocationClasses':[{'class':c,'weightBytes':n} for c,n in classes.most_common(8)],'gc':gc}
(root/'profile-summary.json').write_text(json.dumps(result,ensure_ascii=False,indent=2));print(json.dumps(result,ensure_ascii=False,indent=2))
