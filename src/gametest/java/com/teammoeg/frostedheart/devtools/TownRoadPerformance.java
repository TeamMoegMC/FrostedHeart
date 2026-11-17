package com.teammoeg.frostedheart.devtools;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.lang.management.ManagementFactory;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.citizen.client.*;
import com.teammoeg.frostedheart.content.town.citizen.sim.*;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Development-only integrated-server load and render sampling. No production timing hooks. */
@Mod.EventBusSubscriber(modid=FHMain.MODID,value=Dist.CLIENT)
public final class TownRoadPerformance {
    private static final long[] SERVER = new long[32768], FRAMES = new long[65536], CLIENT = new long[32768];
    private static int serverCount, frameCount, clientCount;
    private static long serverStart, frameStart, clientStart;
    private static volatile boolean measuring;
    private static UUID playerId;
    private static volatile String mode="natural";
    private static long populationMovingSum, actualMovingSum, populationSamples;
    private static volatile boolean offsets=true;
    private static java.lang.reflect.Field ownerField;
    private static final Map<Integer,UUID> owners=new HashMap<>();
    private static final Map<Integer,int[]> previousPositions=new HashMap<>();
    private static jdk.jfr.Recording recording;
    private static long gcCount0, gcTime0, startedAt;
    private static String label="";
    private static Integer savedFps, savedDistance;
    private static Boolean savedVsync;

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void serverStart(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.START)return;
        serverStart=System.nanoTime();
        if(playerId==null)return;
        var player=event.getServer().getPlayerList().getPlayer(playerId); if(player==null)return;
        if(!mode.equals("natural")) {
            var town=TeamTown.from(player); var scheduler=CitizenSimScheduler.get(player.serverLevel());
            for(var resident:town.getAllResidents()) {
                var container=scheduler.findById(resident.getSimId());if(container==null)continue;
                var sim=container.sim();int i=sim.indexOf(resident.getSimId());if(i<0)continue;
                if(mode.equals("idle")) {sim.state[i]=CitizenState.IDLE;sim.halt[i]=1;}
                else if(sim.state[i]!=CitizenState.WANDER) {
                    sim.state[i]=CitizenState.WANDER;var roads=container.roadNavigation();if(roads!=null)roads.remove(sim.id[i]);
                }
            }
        }
        serverStart=System.nanoTime(); // exclude the development-only load stimulus
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void serverEnd(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!measuring)return;
        if(serverCount<SERVER.length)SERVER[serverCount++]=System.nanoTime()-serverStart;
        var player=event.getServer().getPlayerList().getPlayer(playerId);if(player==null)return;
        var town=TeamTown.from(player);var scheduler=CitizenSimScheduler.get(player.serverLevel());
        int moving=0,changed=0;
        for(var resident:town.getAllResidents()) {
            var container=scheduler.findById(resident.getSimId());if(container==null)continue;
            var sim=container.sim();int i=sim.indexOf(resident.getSimId());if(i<0)continue;
            if(CitizenState.MOVING[sim.state[i]]&&sim.halt[i]==0)moving++;
            int[] old=previousPositions.computeIfAbsent(sim.id[i],key->new int[]{sim.px[i],sim.py[i],sim.pz[i]});
            if(old[0]!=sim.px[i]||old[1]!=sim.py[i]||old[2]!=sim.pz[i])changed++;
            old[0]=sim.px[i];old[1]=sim.py[i];old[2]=sim.pz[i];
        }
        populationMovingSum+=moving;actualMovingSum+=changed;populationSamples++;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void clientStart(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.START)return;
        clientStart=System.nanoTime();
        if(!offsets) for(var c:ClientCitizenCache.values()) {
            try {if(ownerField==null){ownerField=ClientCitizen.class.getDeclaredField("roadTown");ownerField.setAccessible(true);}
                UUID owner=(UUID)ownerField.get(c);if(owner!=null){owners.put(c.id,owner);ownerField.set(c,null);}
            }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
        }
        clientStart=System.nanoTime(); // exclude the development-only presentation toggle
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void clientEnd(TickEvent.ClientTickEvent event) {
        if(event.phase==TickEvent.Phase.END&&measuring&&clientCount<CLIENT.length)CLIENT[clientCount++]=System.nanoTime()-clientStart;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void frame(TickEvent.RenderTickEvent event) {
        if(event.phase!=TickEvent.Phase.START)return;
        long now=System.nanoTime();
        if(measuring&&frameStart!=0&&frameCount<FRAMES.length)FRAMES[frameCount++]=now-frameStart;
        frameStart=now;
    }
    private static long gcCount(){return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b->Math.max(0,b.getCollectionCount())).sum();}
    private static long gcTime(){return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b->Math.max(0,b.getCollectionTime())).sum();}
    public static CompletableFuture<Map<String,Object>> run(Minecraft mc,String action) {
        String[] words=action.split("\\s+");
        var result=new CompletableFuture<Map<String,Object>>();
        try {
            if(mc.player==null||mc.getSingleplayerServer()==null)throw new IllegalStateException("singleplayer fixture required");
            playerId=mc.player.getUUID();
            if(words.length<2)throw new IllegalArgumentException("roads perf populate N | mode idle/moving/natural | offsets true/false | begin LABEL | end | hardware | jfr start/stop");
            switch(words[1]) {
                case "settings" -> {
                    if(savedFps==null){savedFps=mc.options.framerateLimit().get();savedDistance=mc.options.renderDistance().get();savedVsync=mc.options.enableVsync().get();}
                    mc.options.framerateLimit().set(260);mc.options.enableVsync().set(false);mc.options.renderDistance().set(6);
                    result.complete(Map.of("fpsLimit",260,"vsync",false,"renderDistance",6));
                }
                case "restore" -> {
                    mode="natural";offsets=true;
                    if(ownerField!=null)for(var c:ClientCitizenCache.values()){UUID owner=owners.get(c.id);if(owner!=null)ownerField.set(c,owner);}
                    if(savedFps!=null){mc.options.framerateLimit().set(savedFps);mc.options.enableVsync().set(savedVsync);mc.options.renderDistance().set(savedDistance);}
                    result.complete(Map.of("restored",true));
                }
                case "graph" -> {result.complete(graphBenchmark());}
                case "hardware" -> {
                    var data=new LinkedHashMap<String,Object>();
                    data.put("processors",Runtime.getRuntime().availableProcessors());data.put("maxHeapBytes",Runtime.getRuntime().maxMemory());
                    data.put("java",System.getProperty("java.version"));data.put("os",System.getProperty("os.name"));
                    data.put("gpu",org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER));
                    data.put("gl",org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION));
                    data.put("window",List.of(mc.getWindow().getWidth(),mc.getWindow().getHeight()));
                    data.put("vsync",mc.options.enableVsync().get());data.put("fpsLimit",mc.options.framerateLimit().get());
                    data.put("renderDistance",mc.options.renderDistance().get());result.complete(data);
                }
                case "mode" -> {
                    if(!Set.of("idle","moving","natural").contains(words[2]))throw new IllegalArgumentException("idle/moving/natural");
                    mode=words[2];result.complete(Map.of("mode",mode));
                }
                case "offsets" -> {
                    offsets=Boolean.parseBoolean(words[2]);
                    if(offsets&&ownerField!=null)for(var c:ClientCitizenCache.values()){UUID owner=owners.get(c.id);if(owner!=null)ownerField.set(c,owner);}
                    result.complete(Map.of("offsets",offsets));
                }
                case "begin" -> {
                    mc.getSingleplayerServer().execute(()->{
                        measuring=false;serverCount=frameCount=clientCount=0;frameStart=0;
                        populationMovingSum=actualMovingSum=populationSamples=0;previousPositions.clear();
                        gcCount0=gcCount();gcTime0=gcTime();startedAt=System.nanoTime();label=words.length>2?words[2]:"sample";
                        measuring=true;result.complete(Map.of("label",label));
                    });
                }
                case "end" -> {
                    measuring=false;
                    var clientStats=stats(CLIENT,clientCount);var frameStats=stats(FRAMES,frameCount);
                    var metrics=renderMetrics();String backend=CitizenRenderCoordinator.backendName();
                    int cache=ClientCitizenCache.values().size();
                    long presented=ClientCitizenCache.values().stream().filter(ClientCitizen::hasRoadPresentation).count();
                    mc.getSingleplayerServer().execute(()->{
                        var data=new LinkedHashMap<String,Object>();data.put("label",label);data.put("mode",mode);data.put("offsets",offsets);
                        data.put("backend",backend);data.put("clientCitizens",cache);data.put("presented",presented);data.put("citizenRenderSubmission",metrics);
                        data.put("wallSeconds",(System.nanoTime()-startedAt)/1e9);data.put("serverTick",stats(SERVER,serverCount));
                        data.put("clientTick",clientStats);data.put("frameInterval",frameStats);
                        data.put("gcCollections",gcCount()-gcCount0);data.put("gcMs",gcTime()-gcTime0);
                        data.put("usedHeapBytes",Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
                        data.put("meanMovingWithXZ",populationSamples==0?0:populationMovingSum/(double)populationSamples);
                        data.put("meanPositionChanges",populationSamples==0?0:actualMovingSum/(double)populationSamples);
                        result.complete(data);
                    });
                }
                case "populate" -> mc.getSingleplayerServer().execute(()->{
                    try {
                        int target=Integer.parseInt(words[2]);if(target<0||target>1024)throw new IllegalArgumentException("0..1024");
                        var player=mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);var town=TeamTown.from(player);
                        for(var building:town.getTownBuildings().values())if(building instanceof HouseBuilding house) {
                            house.setTemperature(20);house.setMaxResidents(target+16);
                        }
                        int before=town.getResidents().size();
                        for(int n=before;n<target;n++)if(!town.addResident(Resident.createRandomRecruit("Perf","Resident"+n,Resident.AGE_ADULT)))throw new IllegalStateException("housing rejected at "+n);
                        result.complete(Map.of("residents",town.getResidents().size(),"added",town.getResidents().size()-before,"housingCapacityOverride",target+16));
                    }catch(Throwable failure){result.completeExceptionally(failure);}
                });
                case "jfr" -> {
                    if(words[2].equals("start")) {
                        recording=new jdk.jfr.Recording(jdk.jfr.Configuration.getConfiguration("profile"));recording.setMaxSize(64L*1024*1024);recording.start();
                        result.complete(Map.of("recording",true));
                    }else {
                        var path=mc.gameDirectory.toPath().resolve("agent/perf/citizen-roads-profile.jfr");java.nio.file.Files.createDirectories(path.getParent());
                        recording.stop();recording.dump(path);recording.close();recording=null;result.complete(Map.of("jfr",path.toAbsolutePath().toString()));
                    }
                }
                default -> throw new IllegalArgumentException("unknown perf action");
            }
        }catch(Throwable failure){result.completeExceptionally(failure);}
        return result;
    }
    private static Map<String,Object> renderMetrics() throws ReflectiveOperationException {
        Class<?> type=Class.forName("com.teammoeg.frostedheart.content.town.citizen.client.CitizenRenderMetrics");
        var method=type.getDeclaredMethod("snapshot");method.setAccessible(true);Object snapshot=method.invoke(null);
        Map<String,Object> result=new LinkedHashMap<>();
        for(var component:snapshot.getClass().getRecordComponents()) {
            var getter=component.getAccessor();getter.setAccessible(true);result.put(component.getName(),getter.invoke(snapshot));
        }
        return result;
    }
    private static Map<String,Object> graphBenchmark() {
        List<Object> rows=new ArrayList<>();
        for(int count:new int[]{32,128,512}) {
            Set<net.minecraft.core.BlockPos> nodes=new HashSet<>();
            List<com.teammoeg.frostedheart.content.town.block.blockscanner.TownConnectivity.Route> roads=new ArrayList<>();
            for(int i=0;i<count;i++)nodes.add(new net.minecraft.core.BlockPos(0,65,i*16));
            for(int i=0;i<count-1;i++) {
                var a=new net.minecraft.core.BlockPos(0,65,i*16);var b=a.south(16);List<net.minecraft.core.BlockPos> steps=new ArrayList<>();
                for(int k=0;k<=16;k++)steps.add(a.south(k));
                roads.add(new com.teammoeg.frostedheart.content.town.block.blockscanner.TownConnectivity.Route(a,b,steps,true));
            }
            var view=new com.teammoeg.frostedheart.content.town.block.blockscanner.TownConnectivity.Result(nodes,roads);
            long[] builds=new long[8];com.teammoeg.frostedheart.content.town.citizen.nav.TownRoadNetwork graph=null;
            for(int repeat=-2;repeat<8;repeat++) {
                long start=System.nanoTime();graph=new com.teammoeg.frostedheart.content.town.citizen.nav.TownRoadNetwork(view);
                if(repeat>=0)builds[repeat]=System.nanoTime()-start;
            }
            var first=new net.minecraft.core.BlockPos(0,65,0);var last=new net.minecraft.core.BlockPos(0,65,(count-1)*16);
            long start=System.nanoTime();int waypoints=graph.between(first,last).size();long cold=System.nanoTime()-start;
            start=System.nanoTime();long checksum=0;for(int k=0;k<10000;k++)checksum+=graph.between(first,last).size();long hits=System.nanoTime()-start;
            rows.add(Map.of("buildings",count,"segmentSteps",16,"build",stats(builds,builds.length),"coldRouteMs",cold/1e6,"cacheHitNs",hits/10000.0,"waypoints",waypoints,"checksum",checksum));
        }
        return Map.of("syntheticGraph",rows,"includesWorldSectionSampling",false);
    }
    private static Map<String,Object> stats(long[] values,int count) {
        long[] copy=Arrays.copyOf(values,count);Arrays.sort(copy);double sum=0;for(long n:copy)sum+=n;
        return Map.of("samples",count,"meanMs",count==0?0:sum/count/1e6,"p50Ms",percentile(copy,.5),"p95Ms",percentile(copy,.95),"p99Ms",percentile(copy,.99),"maxMs",count==0?0:copy[count-1]/1e6);
    }
    private static double percentile(long[] sorted,double q){return sorted.length==0?0:sorted[Math.max(0,(int)Math.ceil(sorted.length*q)-1)]/1e6;}
}
