package com.teammoeg.frostedheart.devtools;

import java.util.*;
import java.util.concurrent.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import com.teammoeg.frostedheart.content.town.block.blockscanner.TownConnectivity;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;

/** One selected real town, with main/worker CPU distinguished from snapshot wait wall time. */
public final class TownPathProbe {
    private static final java.lang.management.ThreadMXBean CPU=ManagementFactory.getThreadMXBean();
    private static final ThreadLocal<Round> WORKER=new ThreadLocal<>();
    private static volatile Round current;
    private static final Field BATCH=field(TownConnectivity.class,"batch"),NEXT=field(TownConnectivity.class,"nextScan");
    static {if(CPU.isThreadCpuTimeSupported()&&!CPU.isThreadCpuTimeEnabled())CPU.setThreadCpuTimeEnabled(true);}
    private static final class Round {
        final TownConnectivity target;final Map<?,?> expected;final String label;final boolean baseline;
        final long armed=System.nanoTime();
        final List<Long> mainWall=new ArrayList<>(),mainCpu=new ArrayList<>(),copyWall=new ArrayList<>(),copyCpu=new ArrayList<>(),serverWall=new ArrayList<>(),serverCpu=new ArrayList<>(),singleCopyWall=new ArrayList<>(),singleCopyCpu=new ArrayList<>();
        final Map<Object,SectionPos> requests=new ConcurrentHashMap<>();
        ServerLevel level;long cacheStart,cacheCpuStart,cacheWall,cacheCpu,futureLookups,serverCpuStart,singleCopyStart,singleCopyCpuStart;
        volatile boolean done;boolean serverDone,workerReturned,failed;int served,maxPerTick,tickServed;
        long tickWall,tickCpu,serverStart,workerStart,workerCpuStart,workerWall,workerCpu,waitWall,expansions,doneNanos;
        Object batch;CompletableFuture<?> future;
        Round(TownConnectivity target,Map<?,?> expected,String label,boolean baseline){this.target=target;this.expected=new HashMap<>(expected);this.label=label;this.baseline=baseline;}
    }
    private static Field field(Class<?> type,String name){try{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(Exception e){throw new IllegalStateException(e);}}
    private static Object get(Object object,String name){try{return field(object.getClass(),name).get(object);}catch(Exception e){throw new IllegalStateException(e);}}
    private static long cpu(){return CPU.isCurrentThreadCpuTimeSupported()?CPU.getCurrentThreadCpuTime():0;}
    public static boolean busy(TownConnectivity target){try{return BATCH.get(target)!=null;}catch(Exception e){throw new IllegalStateException(e);}}
    public static void arm(TownConnectivity target,Map<?,?> buildings,String label,boolean baseline){
        if(busy(target))throw new IllegalStateException("town still has a running round");
        if(current!=null&&!current.done)throw new IllegalStateException("previous probe not finished");
        current=new Round(target,buildings,label,baseline);
        if(!baseline)try{NEXT.setLong(target,0);}catch(Exception e){throw new IllegalStateException(e);}
    }
    public static void selectLevel(ServerLevel level){Round r=current;if(r!=null)r.level=level;}
    public static void finish(){Round r=current;if(r!=null){r.done=true;r.doneNanos=System.nanoTime();}}
    public static void beginTick(TownConnectivity target,ServerLevel level){
        Round r=current;if(r==null||r.target!=target||r.done)return;
        r.level=level;r.tickWall=System.nanoTime();r.tickCpu=cpu();
    }
    public static void endTick(TownConnectivity target,ServerLevel level){
        Round r=current;if(r==null||r.target!=target||r.done)return;
        long wall=System.nanoTime()-r.tickWall+r.cacheWall,cpu=cpu()-r.tickCpu+r.cacheCpu;
        r.cacheWall=r.cacheCpu=0;
        r.mainWall.add(wall);r.mainCpu.add(cpu);
        if(r.tickServed>0){r.copyWall.add(wall);r.copyCpu.add(cpu);}
        r.maxPerTick=Math.max(r.maxPerTick,r.tickServed);r.tickServed=0;
        try{
            Object batch=BATCH.get(target);
            if(batch!=null&&r.batch==null)r.batch=batch;
            if(r.batch!=null&&r.future==null)r.future=(CompletableFuture<?>)get(r.batch,"result");
            if(!r.baseline&&r.future!=null&&r.future.isDone()&&batch==null){r.failed=r.future.isCompletedExceptionally();r.doneNanos=System.nanoTime();r.done=true;}
        }catch(Exception e){throw new IllegalStateException(e);}
    }
    public static void cacheBegin(ServerLevel level){Round r=current;if(r!=null&&!r.done&&r.level==level){r.cacheStart=System.nanoTime();r.cacheCpuStart=cpu();}}
    public static void cacheEnd(ServerLevel level){Round r=current;if(r!=null&&!r.done&&r.level==level&&r.cacheStart!=0){r.cacheWall+=System.nanoTime()-r.cacheStart;r.cacheCpu+=cpu()-r.cacheCpuStart;r.cacheStart=0;}}
    public static void cacheCopied(ServerLevel level){Round r=current;if(r!=null&&!r.done&&r.level==level){r.served++;r.tickServed++;r.singleCopyStart=System.nanoTime();r.singleCopyCpuStart=cpu();}}
    public static void cacheCopyEnd(ServerLevel level){Round r=current;if(r!=null&&!r.done&&r.level==level&&r.singleCopyStart!=0){r.singleCopyWall.add(System.nanoTime()-r.singleCopyStart);r.singleCopyCpu.add(cpu()-r.singleCopyCpuStart);r.singleCopyStart=0;}}
    public static void served(TownConnectivity target){Round r=current;if(r!=null&&r.target==target&&!r.done){r.served++;r.tickServed++;}}
    public static void beginWorker(Object batch){
        Round r=current;if(r==null||r.done||r.baseline||!r.expected.equals(get(batch,"buildings")))return;
        r.batch=batch;WORKER.set(r);r.workerStart=System.nanoTime();r.workerCpuStart=cpu();
    }
    public static void endWorker(){Round r=WORKER.get();if(r!=null){r.workerWall=System.nanoTime()-r.workerStart;r.workerCpu=cpu()-r.workerCpuStart;r.workerReturned=true;WORKER.remove();}}
    public static void expanded(){Round r=WORKER.get();if(r!=null)r.expansions++;}
    public static long beforeWait(Object future,SectionPos section){Round r=WORKER.get();if(r==null)return 0;r.futureLookups++;r.requests.put(future,section);return System.nanoTime();}
    public static void afterWait(long start){Round r=WORKER.get();if(r!=null&&start!=0)r.waitWall+=System.nanoTime()-start;}
    public static void serverStart(){Round r=current;if(r!=null&&!r.serverDone){r.serverStart=System.nanoTime();r.serverCpuStart=cpu();}}
    public static void serverEnd(){Round r=current;if(r!=null&&!r.serverDone&&r.serverStart!=0){if(r.mainWall.size()>1){r.serverWall.add(System.nanoTime()-r.serverStart);r.serverCpu.add(cpu()-r.serverCpuStart);}if(r.done)r.serverDone=true;}}
    public static Map<String,Object> summary(ServerLevel level){
        Round r=current;if(r==null)return Map.of("armed",false);
        Map<String,Object> out=new LinkedHashMap<>();out.put("probeSchema",2);out.put("futureLookups",r.futureLookups);out.put("cacheStatistics",com.teammoeg.frostedheart.content.town.block.blockscanner.TownSectionSnapshots.statistics(level));out.put("label",r.label);out.put("done",r.done);out.put("failed",r.failed);out.put("buildings",r.expected.size());out.put("baseline",r.baseline);
        out.put("elapsedSeconds",((r.done?r.doneNanos:System.nanoTime())-r.armed)/1e9);out.put("mainTickCount",r.mainWall.size());
        out.put("sectionsServed",r.served);out.put("maxSectionsPerTick",r.maxPerTick);out.put("sectionRequests",r.requests.size());
        Set<SectionPos> unique=new HashSet<>(r.requests.values());out.put("uniqueSections",unique.size());out.put("repeatedSectionRequests",r.requests.size()-unique.size());
        out.put("requestsInMissingChunks",r.requests.values().stream().filter(s->level.getChunkSource().getChunkNow(s.x(),s.z())==null).count());
        out.put("expandedNodes",r.expansions);out.put("workerReturned",r.workerReturned);out.put("workerWallMs",r.workerWall/1e6);out.put("workerCpuMs",r.workerCpu/1e6);out.put("snapshotWaitWallMs",r.waitWall/1e6);
        out.put("mainTickWall",stats(r.mainWall));out.put("mainTickCpu",stats(r.mainCpu));out.put("copyTickWall",stats(r.copyWall));out.put("copyTickCpu",stats(r.copyCpu));out.put("serverTickWall",stats(r.serverWall));out.put("serverTickCpu",stats(r.serverCpu));out.put("singleSectionCpu",stats(r.singleCopyCpu));out.put("singleSectionWall",stats(r.singleCopyWall));
        out.put("mainCpuTotalMs",r.mainCpu.stream().mapToLong(Long::longValue).sum()/1e6);out.put("mainWallTotalMs",r.mainWall.stream().mapToLong(Long::longValue).sum()/1e6);
        out.put("routes",r.target.view().routes().size());out.put("working",r.target.view().working().size());out.put("cpuClockSupported",CPU.isCurrentThreadCpuTimeSupported());
        return out;
    }
    private static Map<String,Object> stats(List<Long> values){long[] a=values.stream().mapToLong(Long::longValue).sorted().toArray();double sum=0;for(long v:a)sum+=v;return Map.of("samples",a.length,"meanMs",a.length==0?0:sum/a.length/1e6,"p95Ms",a.length==0?0:a[Math.max(0,(int)Math.ceil(a.length*.95)-1)]/1e6,"maxMs",a.length==0?0:a[a.length-1]/1e6);}
}
