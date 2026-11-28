package com.teammoeg.frostedheart.devtools;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.block.blockscanner.TownConnectivity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Natural terrain fixture; no artificial height snapshots or replacement solver. */
@Mod.EventBusSubscriber(modid=FHMain.MODID,value=Dist.CLIENT)
public final class TownPathPerformance {
    private static final Set<ChunkPos> forced=new HashSet<>();
    private static final List<BlockPos> sites=new ArrayList<>();
    private static int ox,oz;
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void tickStart(TickEvent.ServerTickEvent event){if(event.phase==TickEvent.Phase.START)TownPathProbe.serverStart();}
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void tickEnd(TickEvent.ServerTickEvent event){if(event.phase==TickEvent.Phase.END)TownPathProbe.serverEnd();}
    public static CompletableFuture<Map<String,Object>> run(Minecraft mc,String action){
        var future=new CompletableFuture<Map<String,Object>>();
        if(mc.player==null||mc.getSingleplayerServer()==null){future.completeExceptionally(new IllegalStateException("singleplayer required"));return future;}
        UUID id=mc.player.getUUID();String[] args=action.split("\\s+");
        mc.getSingleplayerServer().execute(()->{
            try{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayer(id);var level=player.serverLevel();var town=TeamTown.from(player);
                Map<String,Object> result=new LinkedHashMap<>();
                switch(args[1]){
                    case "prepare","resume" -> {
                        if(args[1].equals("prepare")&&!town.getTownBuildings().isEmpty())throw new IllegalStateException("fresh disposable world required");
                        var spawn=level.getSharedSpawnPos();ox=(spawn.getX()>>4)*16-48;oz=(spawn.getZ()>>4)*16-32;
                        level.setDayTime(6000);level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,level.getServer());
                        level.setWeatherParameters(12000,0,false,false);
                        for(int x=(ox>>4)-4;x<=(ox>>4)+12;x++)for(int z=(oz>>4)-4;z<=(oz>>4)+8;z++){
                            ChunkPos chunk=new ChunkPos(x,z);if(!level.getForcedChunks().contains(chunk.toLong())){forced.add(chunk);level.setChunkForced(x,z,true);}
                        }
                        player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);player.teleportTo(level,ox+56,110,oz+85,180,35);
                        result.put("origin",List.of(ox,oz));result.put("forcedChunks",forced.size());
                    }
                    case "ready" -> {result.put("loadedChunks",forced.stream().filter(c->level.getChunkSource().getChunkNow(c.x,c.z)!=null).count());result.put("forcedChunks",forced.size());}
                    case "build" -> {
                        int count=Integer.parseInt(args[2]);if(!Set.of(8,16,32).contains(count))throw new IllegalArgumentException("8/16/32");
                        var connectivity=town.getTownData().orElseThrow().connectivityView();if(TownPathProbe.busy(connectivity))throw new IllegalStateException("previous automatic round is still active");
                        if(sites.isEmpty())for(int index=0;index<32;index++){
                            int x=ox+(index%8)*16,z=oz+(index/8)*16;
                            int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x+3,z+3)-1;
                            sites.add(new BlockPos(x,y,z));
                        }
                        int before=town.getTownBuildings().size();
                        for(int index=before;index<count;index++)house(level,player,sites.get(index));
                        TownPathProbe.arm(connectivity,town.getTownBuildings(),args.length>3?args[3]:"first-"+count,false);
                        result.put("buildings",town.getTownBuildings().size());result.put("groundHeights",sites.subList(0,count).stream().map(BlockPos::getY).toList());
                        result.put("biomes",sites.subList(0,count).stream().map(p->level.getBiome(p).unwrapKey().map(k->k.location().toString()).orElse("unknown")).distinct().toList());
                        result.put("originalTerrainBetweenBuildings",true);
                    }
                    case "arm","baseline" -> {
                        var connectivity=town.getTownData().orElseThrow().connectivityView();
                        TownPathProbe.arm(connectivity,town.getTownBuildings(),args.length>2?args[2]:"repeat",args[1].equals("baseline"));
                    }
                    case "barrier" -> {
                        var connectivity=town.getTownData().orElseThrow().connectivityView();if(TownPathProbe.busy(connectivity))throw new IllegalStateException("round still active");
                        // Real five-block-high wall across the town; the solver must search around it or report disconnected components.
                        for(int z=oz-20;z<=oz+84;z++){
                            int x=ox+56,y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
                            fill(level,new BlockPos(x,y,z),new BlockPos(x,y+4,z),Blocks.STONE_BRICKS.defaultBlockState());
                        }
                        result.put("wallBlocks",105*5);
                    }
                    case "seal" -> {
                        var connectivity=town.getTownData().orElseThrow().connectivityView();
                        if(TownPathProbe.busy(connectivity))throw new IllegalStateException("round still active");
                        var candidate=connectivity.view().working().stream().sorted()
                                .map(pos->town.getTownBuildings().get(pos)).filter(Objects::nonNull)
                                .filter(building->!building.getRoadEntrances().isEmpty()).findFirst().orElseThrow();
                        result.put("previousWorking",connectivity.view().working().size());result.put("previousRoutes",connectivity.view().routes().size());
                        result.put("sealedController",List.of(candidate.getPos().getX(),candidate.getPos().getY(),candidate.getPos().getZ()));
                        for(BlockPos port:candidate.getRoadEntrances()) {
                            set(level,port,Blocks.STONE_BRICKS.defaultBlockState());set(level,port.above(),Blocks.STONE_BRICKS.defaultBlockState());
                        }
                        candidate.setRoadEntrances(List.of());
                        TownPathProbe.arm(connectivity,town.getTownBuildings(),"32-sealed-reachable-entrance",false);
                    }
                    case "clearcache" -> {
                        var connectivity=town.getTownData().orElseThrow().connectivityView();
                        if(TownPathProbe.busy(connectivity))throw new IllegalStateException("wait for current search before clearing the cache");
                        com.teammoeg.frostedheart.content.town.block.blockscanner.TownSectionSnapshots.get(level).invalidateAll();
                    }
                    case "dirtycache" -> com.teammoeg.frostedheart.content.town.block.blockscanner.TownSectionSnapshots.get(level).markAllDirty(level.getGameTime());
                    case "finish" -> TownPathProbe.finish();
                    case "status" -> {}
                    case "cleanup" -> {for(var chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);result.put("releasedForcedChunks",forced.size());forced.clear();}
                    default -> throw new IllegalArgumentException("path prepare|resume|ready|build N LABEL|arm LABEL|baseline LABEL|barrier|seal|finish|status|cleanup");
                }
                TownPathProbe.selectLevel(level);
                result.put("probe",TownPathProbe.summary(level));result.put("residents",town.getResidents().size());
                var data=town.getTownData().orElse(null);if(data!=null){result.put("busy",TownPathProbe.busy(data.connectivityView()));result.put("entranceCounts",town.getTownBuildings().values().stream().map(b->b.getRoadEntrances().size()).toList());}
                future.complete(result);
            }catch(Throwable failure){future.completeExceptionally(failure);}
        });return future;
    }
    private static void set(ServerLevel level,BlockPos p,BlockState s){level.setBlock(p,s,2);}
    private static void fill(ServerLevel level,BlockPos a,BlockPos b,BlockState s){for(var p:BlockPos.betweenClosed(a,b))set(level,p,s);}
    private static void house(ServerLevel level,net.minecraft.server.level.ServerPlayer player,BlockPos site){
        int x=site.getX(),y=site.getY(),z=site.getZ();
        // Foundation/room only; do not flatten or precompute the terrain between houses.
        fill(level,site,site.offset(6,5,6),Blocks.OAK_PLANKS.defaultBlockState());
        fill(level,site.offset(1,1,1),site.offset(5,4,5),Blocks.AIR.defaultBlockState());
        fill(level,site.offset(1,5,1),site.offset(5,5,5),Blocks.GLASS.defaultBlockState());
        BlockPos door=new BlockPos(x+3,y+1,z+6);var state=Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.SOUTH);
        set(level,door,state.setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER));set(level,door.above(),state.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER));
        BlockPos core=site.offset(3,1,3);var block=FHBlocks.HOUSE.get();set(level,core,block.defaultBlockState());
        block.setPlacedBy(level,core,level.getBlockState(core),player,new ItemStack(block.asItem()));
        ((AbstractTownBuildingBlockEntity<?>)level.getBlockEntity(core)).executeTask();
    }
}
