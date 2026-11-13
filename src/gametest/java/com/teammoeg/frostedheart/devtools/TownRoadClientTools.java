package com.teammoeg.frostedheart.devtools;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.citizen.client.*;
import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenSimScheduler;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;

/** Opt-in agent bridge fixture and observations; never included in production builds. */
public final class TownRoadClientTools {
    public static CompletableFuture<Map<String,Object>> run(Minecraft mc, String action) {
        if (action.startsWith("path")) return TownPathPerformance.run(mc, action);
        if (action.startsWith("perf")) return TownRoadPerformance.run(mc, action);
        var future = new CompletableFuture<Map<String,Object>>();
        if (mc.getSingleplayerServer()==null || mc.player==null) { future.completeExceptionally(new IllegalStateException("singleplayer world required")); return future; }
        UUID playerId=mc.player.getUUID();
        Map<String,Object> client=new LinkedHashMap<>();
        client.put("backend",CitizenRenderCoordinator.backendName());
        client.put("clientCitizens",ClientCitizenCache.values().size());
        client.put("presented",ClientCitizenCache.values().stream().filter(ClientCitizen::hasRoadPresentation).count());
        client.put("climbing",ClientCitizenCache.values().stream().filter(ClientCitizen::isRoadClimbing).count());
        client.put("samples",ClientCitizenCache.values().stream().limit(8).map(c -> {
            double[] raw=c.renderPos().clone(), display=c.presentationPos().clone();
            return Map.of("id",c.id,"raw",raw,"display",display,"road",c.hasRoadPresentation(),"climb",c.isRoadClimbing());
        }).toList());
        mc.getSingleplayerServer().execute(() -> {
            try {
                var player=mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);
                var level=player.serverLevel(); var town=TeamTown.from(player);
                if (action.equals("setup")) {
                    if (!town.getTownBuildings().isEmpty()) throw new IllegalStateException("use a disposable empty town");
                    level.setDayTime(6000); level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,level.getServer());
                    fill(level,new BlockPos(-3,64,-3),new BlockPos(40,64,14),Blocks.STONE.defaultBlockState());
                    fill(level,new BlockPos(23,64,-3),new BlockPos(26,64,14),Blocks.AIR.defaultBlockState());
                    house(level,player,0,64,0,false); house(level,player,12,64,0,false); house(level,player,30,64,0,false);
                    for(int step=0;step<5;step++) set(level,new BlockPos(3,64+step,9+step),
                            Blocks.STONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.SOUTH));
                    fill(level,new BlockPos(2,69,14),new BlockPos(4,69,19),Blocks.STONE.defaultBlockState());
                    house(level,player,0,69,20,true);
                    fill(level,new BlockPos(17,65,10),new BlockPos(17,74,10),Blocks.STONE.defaultBlockState());
                    for(int y=65;y<=73;y++) set(level,new BlockPos(16,y,10),Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,Direction.WEST));
                    fill(level,new BlockPos(14,73,11),new BlockPos(17,73,11),Blocks.STONE.defaultBlockState());
                    house(level,player,12,73,12,true);
                    player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                    player.teleportTo(level,19,88,39,180,42);
                } else if (action.equals("populate")) {
                    for(var building:town.getTownBuildings().values()) if(building instanceof HouseBuilding house) house.setTemperature(20);
                    int added=0;
                    for(int n=0;n<16;n++) {
                        if(town.addResident(Resident.createRandomRecruit("Road", "Walker"+n,Resident.AGE_ADULT))) added++;
                        else break;
                    }
                    client.put("added",added);
                    if(added<4) throw new IllegalStateException("housing not ready: only added "+added);
                } else if(!action.equals("status")) throw new IllegalArgumentException("roads setup|populate|status");
                var connectivity=town.getTownData().orElseThrow().connectivityView();
                client.put("workingBuildings",connectivity.view().working().size());
                client.put("routes",connectivity.view().routes().size());
                client.put("residents",town.getResidents().size());
                List<Object> simulations=new ArrayList<>();
                var scheduler=CitizenSimScheduler.get(level);
                for(var resident:town.getAllResidents()) {
                    var container=scheduler.findById(resident.getSimId()); if(container==null)continue;
                    var sim=container.sim(); int i=sim.indexOf(resident.getSimId()); if(i<0)continue;
                    simulations.add(Map.of("id",sim.id[i],"x",sim.px[i]/1024.0,"y",sim.py[i]/1024.0,"z",sim.pz[i]/1024.0,"state",sim.state[i]));
                }
                client.put("serverCitizens",simulations);
                future.complete(client);
            } catch(Throwable failure) { future.completeExceptionally(failure); }
        });
        return future;
    }
    private static void set(ServerLevel level,BlockPos p,BlockState s) { level.setBlock(p,s,2); }
    private static void fill(ServerLevel level,BlockPos a,BlockPos b,BlockState state) { for(var p:BlockPos.betweenClosed(a,b))set(level,p,state); }
    private static void house(ServerLevel level,net.minecraft.server.level.ServerPlayer owner,int x,int y,int z,boolean north) {
        fill(level,new BlockPos(x,y,z),new BlockPos(x+6,y+5,z+6),Blocks.OAK_PLANKS.defaultBlockState());
        fill(level,new BlockPos(x+1,y+1,z+1),new BlockPos(x+5,y+4,z+5),Blocks.AIR.defaultBlockState());
        fill(level,new BlockPos(x+1,y+5,z+1),new BlockPos(x+5,y+5,z+5),Blocks.GLASS.defaultBlockState());
        BlockPos door=new BlockPos(x+3,y+1,north?z:z+6);
        var doorState=Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,north?Direction.NORTH:Direction.SOUTH);
        set(level,door,doorState.setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER));
        set(level,door.above(),doorState.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER));
        for(int bz:new int[]{2,4}) for(int bx:new int[]{1,4}) {
            var bed=Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH);
            set(level,new BlockPos(x+bx,y+1,z+bz),bed.setValue(BedBlock.PART,BedPart.FOOT));
            set(level,new BlockPos(x+bx,y+1,z+bz+1),bed.setValue(BedBlock.PART,BedPart.HEAD));
        }
        BlockPos core=new BlockPos(x+3,y+1,z+3); var block=FHBlocks.HOUSE.get();
        set(level,core,block.defaultBlockState());
        block.setPlacedBy(level,core,level.getBlockState(core),owner,new ItemStack(FHBlocks.HOUSE.get().asItem()));
        var be=(AbstractTownBuildingBlockEntity<?>)level.getBlockEntity(core);
        be.executeTask();
    }
}
