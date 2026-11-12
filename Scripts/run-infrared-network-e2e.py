"""Real dedicated server + two rendering clients through ordered, rate-limited TCP."""
import argparse, asyncio, contextlib, json, os, re, shutil, struct, subprocess, sys, time, zlib
from pathlib import Path

REPO=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--root',type=Path,default=REPO/'build/infrared-network-e2e')
parser.add_argument('--java',default=str(Path(os.environ['JAVA_HOME'])/'bin/java.exe') if os.environ.get('JAVA_HOME') else shutil.which('java'))
parser.add_argument('--gradle-init',action='append',default=[],type=Path)
parser.add_argument('--offline',action='store_true')
parser.add_argument('--reuse-launch',action='store_true',help='Reuse exported manifests for already compiled current sources')
parser.add_argument('--prepare',action='store_true')
parser.add_argument('--physical-only',action='store_true',help='Real fueled campfires and continuous Air/material only; no analytic fields')
parser.add_argument('--cooling-only',action='store_true',help='Keep the real material untouched until natural cooling retires its infrared data')
parser.add_argument('--analytic-stress-only',action='store_true',help='Controlled analytic-reference and large transport fixtures instead of the default physical backend scene')
OPTIONS=parser.parse_args()
ROOT=OPTIONS.root.resolve()
PROFILES=[dict(round=0,name='300ms-rtt',oneWayMs=150,bytesPerSecond=131072,bulk=False),
          dict(round=1,name='3s-rtt',oneWayMs=1500,bytesPerSecond=65536,bulk=False),
          dict(round=2,name='3s-rtt-16KiBps-bulk',oneWayMs=1500,bytesPerSecond=16384,bulk=True),
          dict(round=3,name='3s-rtt-16KiBps-multipart',oneWayMs=1500,bytesPerSecond=16384,bulk=True,stressFields=True),
          dict(round=4,name='3s-rtt-16KiBps-single-observer',oneWayMs=1500,bytesPerSecond=16384,bulk=True,onlySlow=True)]
if OPTIONS.physical_only or not OPTIONS.analytic_stress_only:PROFILES=[dict(round=0,name='physical-campfires-3s-rtt-16KiBps',oneWayMs=1500,bytesPerSecond=16384,bulk=False,physical=True)]
if OPTIONS.cooling_only:PROFILES=[dict(round=0,name='natural-cooling-3s-rtt-16KiBps',oneWayMs=1500,bytesPerSecond=16384,bulk=False,physical=True,coolingOnly=True)]
PROCESSES={}

def write(name,data):
    path=ROOT/name;temp=path.with_suffix(path.suffix+'.tmp')
    temp.write_text(json.dumps(data,indent=2),encoding='utf8');temp.replace(path)

def prepare():
    ROOT.mkdir(parents=True,exist_ok=True)
    generated=['round.json','result.json','processes.json','server-ready.json','server-failure.json','wire-fast.json','wire-slow.json','physical-state.json','physical-scene.json','physical-history.jsonl','natural-final-full.json','finish']
    for role in ['fast','slow']:
        generated += [f'{role}-result.json',f'{role}-world-ready.json']
        generated += [f'wire-samples-{role}.json',f'wire-error-{role}.json']
    for profile in PROFILES:
        n=profile['round']
        generated += [f'start-round-{n}',f'server-round-{n}.json',f'server-update-{n}.json',f'server-remove-{n}.json',f'round-complete-{n}.json',f'proxy-round-{n}.json']
        for role in ['fast','slow']:
            generated += [f'{role}-round-{n}-{stage}.json' for stage in ['latest','preview','warm','updated','removed']]
            generated += [f'{role}-round-{n}-physics-{stage}.json' for stage in ['latest','heated','cooled','removed','restored','natural-cleared']]
    for name in generated:
        path=ROOT/name
        if path.is_file():path.unlink()
    for role in ['server','fast','slow']:
        runtime=ROOT/role;(runtime/'config').mkdir(parents=True,exist_ok=True)
        for name in ['frostedheart-common.toml','frostedheart-client.toml','chorda-client.toml','forge-client.toml',
                     'forge-common.toml','fml.toml','embeddium-options.json','embeddium-mixins.properties','oculus.properties',
                     'immersiveengineering-common.toml','create-common.toml','caupona-common.toml']:
            source=REPO/'run/config'/name
            if source.exists():shutil.copy2(source,runtime/'config'/name)
        shutil.copytree(REPO/'src/gametest/catalog',runtime/'config/fhresearches',dirs_exist_ok=True)
        if role!='server':
            (runtime/'options.txt').write_text('version:3465\nonboardAccessibility:false\nfullscreen:false\nrenderDistance:5\nsimulationDistance:5\nmaxFps:30\nguiScale:2\npauseOnLostFocus:false\nresourcePacks:[]\ngraphicsMode:1\n',encoding='utf8')
        else:
            (runtime/'eula.txt').write_text('eula=true\n',encoding='utf8')
            values={'server-ip':'127.0.0.1','server-port':'25573','online-mode':'false','enforce-secure-profile':'false',
                    'level-name':'infrared-real-world','level-type':'minecraft:flat','difficulty':'peaceful','gamemode':'spectator',
                    'view-distance':'6','simulation-distance':'4','spawn-protection':'0','allow-flight':'true','max-players':'4',
                    'network-compression-threshold':'256','max-tick-time':'180000',
                    'generator-settings':json.dumps({'layers':[{'block':'minecraft:bedrock','height':1},{'block':'minecraft:stone','height':2},{'block':'minecraft:grass_block','height':1}],'biome':'minecraft:plains','structures':{'structures':{}}},separators=(',',':'))}
            (runtime/'server.properties').write_text(''.join(f'{k}={v}\n' for k,v in values.items()),encoding='utf8')

def export_launch():
    for role in ['server','fast','slow']:
        command=[OPTIONS.java,'-classpath',str(REPO/'gradle/wrapper/gradle-wrapper.jar'),
                 'org.gradle.wrapper.GradleWrapperMain','runServer' if role=='server' else 'runClient',
                 '-I',str(REPO/'Scripts/infrared-network-e2e.init.gradle'),f'-PirRole={role}',f'-PirRoot={ROOT}',
                 '--no-daemon','--max-workers=1','--console=plain',
                 '-Dorg.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m -Dfile.encoding=UTF-8']
        for init in OPTIONS.gradle_init:command+=['-I',str(init.resolve())]
        if OPTIONS.offline:command+=['--offline']
        log=ROOT/f'export-{role}.log'
        print('Preparing launch',role,flush=True)
        with log.open('w',encoding='utf8') as output:
            result=subprocess.run(command,cwd=REPO,stdout=output,stderr=subprocess.STDOUT,
                                  creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
        if result.returncode:raise RuntimeError(f'Launch export failed for {role}; see {log}')

def launch(role):
    manifest=json.loads((ROOT/f'launch-{role}.json').read_text(encoding='utf8'))
    command=manifest['command'];java=command[0] or OPTIONS.java
    if role=='fast':command=[arg.replace('irE2ePort=25573','irE2ePort=25575') if arg else arg for arg in command]
    def arg(value):return '"'+str(value).replace('\\','\\\\').replace('"','\\"')+'"'
    args=ROOT/f'java-{role}.args';args.write_text('\n'.join(arg(v) for v in command[1:])+'\n',encoding='utf8')
    environment=os.environ.copy();environment.update(manifest['environment'])
    stream=(ROOT/f'{role}-console.log').open('w',encoding='utf8')
    process=subprocess.Popen([java,'@'+str(args)],cwd=manifest['workingDirectory'],env=environment,
                             stdin=subprocess.PIPE,stdout=stream,stderr=subprocess.STDOUT,
                             creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
    PROCESSES[role]=(process,stream)
    write('processes.json',{name:p.pid for name,(p,_) in PROCESSES.items()})
    print('Started',role,process.pid,flush=True)

def check():
    for role in ['fast','slow']:
        if (ROOT/f'wire-error-{role}.json').exists():raise RuntimeError((ROOT/f'wire-error-{role}.json').read_text())
    for name,(process,_) in PROCESSES.items():
        path=ROOT/f'{name}-result.json'
        if path.exists():
            result=json.loads(path.read_text())
            if not result['passed']:raise RuntimeError(name+': '+str(result['failure']))
        elif process.poll() is not None:raise RuntimeError(name+' exited '+str(process.returncode))
    if (ROOT/'server-failure.json').exists():raise RuntimeError((ROOT/'server-failure.json').read_text())

async def wait_file(name,timeout=300):
    deadline=time.monotonic()+timeout
    while not (ROOT/name).exists():
        check()
        if time.monotonic()>deadline:raise TimeoutError('Waiting for '+name)
        await asyncio.sleep(.5)

def varint(data,start=0,limit=5):
    value=0
    for offset in range(limit):
        if start+offset>=len(data):return None
        byte=data[start+offset];value|=(byte&127)<<(offset*7)
        if byte<128:return value,start+offset+1
    raise ValueError('Invalid test-stream VarInt')

class WireMeter:
    """Read the real Minecraft 1.20.1 framing without modifying forwarded bytes."""
    def __init__(self,frames,label=None):
        self.frames=frames;self.label=label;self.buffer=bytearray();self.compression=False;self.play=False;self.error=None
        self.committed={'warm':-32768,'cold':-32768};self.staging=self.committed.copy();self.samples={}

    @staticmethod
    def mask(payload,index):
        mode=payload[index];index+=1
        if mode==1:return index+96
        if mode==2:
            count,index=varint(payload,index)
            for _ in range(count):_,index=varint(payload,index)
        return index

    def temperatures(self,payload,index,request,flags):
        x,z,y=struct.unpack_from('>iii',payload,index);index+=12
        _,index=varint(payload,index,10);_,index=varint(payload,index)
        if flags&4:
            presence=payload[index];index+=1
            if presence:index=self.mask(payload,index)
            index=self.mask(payload,index);_,index=varint(payload,index,10);index+=8
        length,index=varint(payload,index);end=index+length
        if flags&2:self.staging={'warm':-32768,'cold':-32768} if flags&1 else self.committed.copy()
        targets={}
        for name,(wx,wy,wz) in {'warm':(-4,67,0),'cold':(4,67,0)}.items():
            px,py,pz=(wx>>4)-(x-4),(wy>>4)-(y-4),(wz>>4)-(z-4)
            page=(py*9+pz)*9+px;brick=((wy&15)>>2)*16+((wz&15)>>2)*4+((wx&15)>>2)
            targets[page*64+brick]=(name,((wy&3)<<4)|((wz&3)<<2)|(wx&3))
        while index<end:
            address,mode=struct.unpack_from('>HB',payload,index)
            if mode==0:size=3
            elif mode==1:size=5
            elif mode==3:size=131
            elif mode==2:
                count=payload[index+3];bits=(count-1).bit_length();per=64//bits;size=4+count*2+((64+per-1)//per)*8
            else:raise ValueError(f'Unexpected infrared record mode {mode}')
            if address in targets:
                name,block=targets[address]
                if mode==0:value=-32768
                elif mode==1:value=struct.unpack_from('>h',payload,index+3)[0]
                elif mode==3:value=struct.unpack_from('>h',payload,index+3+block*2)[0]
                else:
                    packed=struct.unpack_from('>Q',payload,index+4+count*2+(block//per)*8)[0]
                    entry=(packed>>((block%per)*bits))&((1<<bits)-1)
                    value=struct.unpack_from('>h',payload,index+4+entry*2)[0]
                self.staging[name]=value
            index+=size
        if flags&4:
            self.committed=self.staging.copy();self.samples[str(request)]=self.committed.copy()
            if self.label:write(f'wire-samples-{self.label}.json',self.samples)
    def feed(self,data):
        if self.error:return
        try:
            self.buffer.extend(data)
            while header:=varint(self.buffer):
                length,start=header;end=start+length
                if len(self.buffer)<end:return
                payload=bytes(self.buffer[start:end]);del self.buffer[:end]
                if self.compression:
                    inflated,index=varint(payload)
                    payload=zlib.decompress(payload[index:]) if inflated else payload[index:]
                packet,index=varint(payload)
                if not self.play:
                    if packet==3:self.compression=True
                    elif packet==2:self.play=True
                    continue
                if packet!=0x17:continue # ClientboundCustomPayloadPacket in Minecraft 1.20.1
                size,index=varint(payload,index)
                channel=payload[index:index+size];index+=size
                if channel!=b'frostedheart:network':continue
                discriminator,index=varint(payload,index)
                if discriminator!=61:continue # FHNetwork infrared_view_s2c, existing stable registration order
                body=len(payload)-index;request,index=varint(payload,index);flags=payload[index];index+=1
                if not flags&8:self.temperatures(payload,index,request,flags)
                self.frames.append(dict(requestId=request,first=bool(flags&2),last=bool(flags&4),
                    full=bool(flags&1),ack=bool(flags&8),bodyBytes=body,wireFrameBytes=end,
                    decodedPacketBytes=len(payload)))
        except Exception as error:
            self.error=str(error)
            if self.label:write(f'wire-error-{self.label}.json',{'error':self.error})

class Proxy:
    def __init__(self,label='slow'):self.label=label;self.profile=dict(oneWayMs=0,bytesPerSecond=100000000);self.bytes={'up':0,'down':0};self.connections=0;self.frames=[];self.meters=[]
    async def accept(self,reader,writer):
        self.connections+=1
        remote_r,remote_w=await asyncio.open_connection('127.0.0.1',25573)
        meter=WireMeter(self.frames,self.label);self.meters.append(meter)
        try:await asyncio.gather(self.pipe(reader,remote_w,'up'),self.pipe(remote_r,writer,'down',meter))
        except (ConnectionError,asyncio.CancelledError):pass
        finally:
            writer.close();remote_w.close()
            with contextlib.suppress(Exception):await writer.wait_closed();await remote_w.wait_closed()
    async def pipe(self,reader,writer,direction,meter=None):
        queue=asyncio.Queue(maxsize=64)
        async def read():
            while data:=await reader.read(16384):
                if meter:meter.feed(data)
                profile=self.profile.copy()
                await queue.put((time.monotonic()+profile['oneWayMs']/1000,data,profile['bytesPerSecond']))
            await queue.put(None)
        async def send():
            available=0
            while item:=await queue.get():
                ready,data,rate=item
                when=max(ready,available);remaining=when-time.monotonic()
                if remaining>0:await asyncio.sleep(remaining)
                writer.write(data);await writer.drain()
                if self.bytes[direction]<1000:print('Proxy',self.label,direction,'bytes',len(data),flush=True)
                self.bytes[direction]+=len(data);available=max(when,time.monotonic())+len(data)/rate
            writer.close()
        await asyncio.gather(read(),send())

def verify_observations():
    for profile in PROFILES:
        n=profile['round']
        if profile.get('physical'):
            for role in ['fast','slow']:
                phases=['heated','cooled','natural-cleared'] if profile.get('coolingOnly') else ['heated','cooled','removed','restored']
                for phase in phases:
                    record=json.loads((ROOT/f'{role}-round-{n}-physics-{phase}.json').read_text())
                    if record['backend']['analyticFields']!=0 or record['gpuWarmQ']!=record['wire']['warm'] or record['gpuColdQ']!=record['wire']['cold']:
                        raise RuntimeError('Physical backend/wire/GPU evidence disagrees')
                    if 'baseline=true' not in record['status'] or 'staging=true' in record['status']:
                        raise RuntimeError('Physical GPU assertion did not observe a committed response')
                    if phase=='heated':
                        b=record['backend']
                        if b['materialSource']!='LIVE' or b['airBasisTerms']<8 or b['airNearC']<=b['airFarC']+.05 or b['materialC']<b['baselineC']+.5:
                            raise RuntimeError('Real campfire did not heat live continuous Air and material')
                    if phase=='cooled' and not record['backend']['airCooled']:raise RuntimeError('Real Air did not cool after extinguishing')
                    if phase=='removed' and record['gpuWarmQ']!=-32768:raise RuntimeError('Deleted material still has a GPU temperature')
                    if phase=='natural-cleared' and (record['gpuWarmQ']!=-32768 or not record['backend']['naturalCleared']):raise RuntimeError('Naturally retired material still has a GPU temperature')
                    if phase in ['heated','restored','natural-cleared'] and record['expectedColorError']>60:raise RuntimeError('Physical framebuffer differs from actual temperature color')
            continue
        roles=['slow'] if profile.get('onlySlow') else ['fast','slow']
        for role in roles:
            for phase in ['warm','updated','removed']:
                record=json.loads((ROOT/f'{role}-round-{n}-{phase}.json').read_text())
                expected=record['warm']==(160 if phase=='warm' else 240) if phase!='removed' else record['warm']<80
                if not expected or record['cold']!=-160:
                    raise RuntimeError(f'Unexpected GPU reference values: {role} round {n} {phase}')
                if 'baseline=true' not in record['status'] or 'staging=false' not in record['status']:
                    raise RuntimeError(f'Observed colors without completed baseline: {role} round {n} {phase}')
        if profile['bulk']:
            preview=json.loads((ROOT/f'slow-round-{n}-preview.json').read_text())
            if 'commits=0' not in preview['status'] or 'staging=true' not in preview['status']:
                raise RuntimeError(f'Missing actual progressive GPU/framebuffer evidence in round {n}')
        if profile.get('onlySlow'):
            first=json.loads((ROOT/f'slow-round-{n}-preview.json').read_text())
            request=int(re.search(r'request=(\d+)',first['status']).group(1))
            log=(ROOT/'slow-console.log').read_text(encoding='utf8',errors='replace')
            replies=[(int(i),full) for i,full in re.findall(
                r'IR_CLIENT_PART id=(\d+) accepted=true first=true last=(?:true|false) full=(true|false)',log) if int(i)>=request]
            if not replies or replies[0]!=(request,'true') or any(full!='false' for _,full in replies[1:]):
                raise RuntimeError('Single slow observer repeatedly downloaded full instead of subsequent deltas')
    if 'lost connection: Timed out' in (ROOT/'server-console.log').read_text(encoding='utf8',errors='replace'):
        raise RuntimeError('Real server timed out a test client')

async def main():
    prepare();proxy=Proxy();direct=Proxy('fast');listener=None;direct_listener=None;passed=False;error=None
    try:
        if not OPTIONS.reuse_launch:export_launch()
        launch('server');await wait_file('server-ready.json',300)
        listener=await asyncio.start_server(proxy.accept,'127.0.0.1',25574)
        direct_listener=await asyncio.start_server(direct.accept,'127.0.0.1',25575)
        launch('fast');await wait_file('fast-world-ready.json',300)
        launch('slow');await wait_file('slow-world-ready.json',300)
        for profile in PROFILES:
            proxy.profile=profile
            write('round.json',profile)
            await wait_file(f"server-round-{profile['round']}.json",30)
            (ROOT/f"start-round-{profile['round']}").touch()
            print('Round started',profile,flush=True)
            await wait_file(f"round-complete-{profile['round']}.json",900 if profile.get('coolingOnly') else 420 if profile.get('physical') else 300)
            write(f"proxy-round-{profile['round']}.json",{'profile':profile,'bytes':proxy.bytes.copy(),'connections':proxy.connections})
            print('Round passed',profile['name'],flush=True)
        (ROOT/'finish').touch()
        await wait_file('fast-result.json',30);await wait_file('slow-result.json',30)
        check();verify_observations();passed=True
    except Exception as failure:
        error=str(failure);print('E2E failure:',error,flush=True)
    finally:
        write('result.json',{'passed':passed,'failure':error,'profiles':PROFILES,'proxyBytes':proxy.bytes})
        for p in [proxy,direct]:write(f'wire-{p.label}.json',dict(frames=p.frames,errors=[m.error for m in p.meters if m.error]))
        for role in ['fast','slow']:
            if role in PROCESSES:
                process,_=PROCESSES[role]
                if process.poll() is None:process.terminate()
        if 'server' in PROCESSES:
            process,_=PROCESSES['server']
            if process.poll() is None:
                with contextlib.suppress(Exception):process.stdin.write(b'stop\n');process.stdin.flush()
                for _ in range(60):
                    if process.poll() is not None:break
                    await asyncio.sleep(.5)
                if process.poll() is None:process.terminate()
        if listener:listener.close();await listener.wait_closed()
        if direct_listener:direct_listener.close();await direct_listener.wait_closed()
        for process,stream in PROCESSES.values():
            with contextlib.suppress(Exception):process.wait(timeout=10)
            stream.close()
    return 0 if passed else 1

if __name__=='__main__':
    if OPTIONS.prepare:prepare()
    else:sys.exit(asyncio.run(main()))
