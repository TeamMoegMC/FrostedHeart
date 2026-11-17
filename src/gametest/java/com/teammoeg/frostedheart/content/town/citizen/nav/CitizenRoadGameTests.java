package com.teammoeg.frostedheart.content.town.citizen.nav;

import java.util.*;
import com.teammoeg.frostedheart.content.town.block.blockscanner.TownConnectivity;
import com.teammoeg.frostedheart.content.town.citizen.sim.*;
import com.teammoeg.frostedheart.content.town.citizen.sync.S2CCitizenSpawnPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_citizen_roads")
@PrefixGameTestTemplate(false)
public final class CitizenRoadGameTests {
    private static final BlockPos A = new BlockPos(0, 2, 0), B = new BlockPos(8, 2, 0), C = new BlockPos(8, 7, 8);
    private static TownConnectivity.Result graph() {
        return new TownConnectivity.Result(Set.of(A,B,C), List.of(
            new TownConnectivity.Route(A, B, List.of(A.east(), A.east(2), A.east(3)), true),
            // The two B ports are deliberately separated by walls/distance; the building is still one node.
            new TownConnectivity.Route(B, C, List.of(B.south(3), B.south(3).above(), B.south(3).above(2)), true)));
    }
    @GameTest(template="phase0a_empty")
    public static void buildingPortsTransferWithoutIndoorPath(GameTestHelper h) {
        TownRoadNetwork net = new TownRoadNetwork(graph());
        var path = net.between(A,C);
        h.assertTrue(path.size()==6 && path.get(3).buildingTransfer(), "building B must transfer between all its ports without indoor navigation");
        h.assertTrue(!path.get(2).buildingTransfer() && path.get(5).pos().getY()==4, "only segment boundaries transfer; preserve every 3D point");
        h.assertTrue(net.between(C,A).get(0).pos().equals(path.get(5).pos()), "roads must work in reverse too");
        h.succeed();
    }
    @GameTest(template="phase0a_empty")
    public static void disconnectedAndInactiveNodesExcluded(GameTestHelper h) {
        var graph = graph(); BlockPos isolated = new BlockPos(500,2,0);
        var net = new TownRoadNetwork(new TownConnectivity.Result(Set.of(A,B,C), Set.of(A,B,C,isolated), graph.routes()));
        h.assertTrue(net.between(A,isolated).isEmpty(), "never make a straight fallback between disconnected components");
        h.assertTrue(net.between(A,new BlockPos(1,1,1)).isEmpty(), "unknown or foreign building is not a road node");
        h.assertTrue(net.wanderTarget(A,123).equals(net.wanderTarget(A,123)), "stable wander selection");
        var inactive = new TownRoadNetwork(new TownConnectivity.Result(Set.of(), Set.of(), graph.routes()));
        h.assertTrue(inactive.between(A,B).isEmpty(), "inactive routes cannot grant movement"); h.succeed();
    }
    @GameTest(template="phase0a_empty")
    public static void midRoadReplanningKeepsPhysicalSteps(GameTestHelper h) {
        var net = new TownRoadNetwork(graph());
        var path = net.fromPosition(A.east(2), A, C, A.east());
        h.assertTrue(!path.isEmpty() && path.get(0).pos().equals(A.east(2)), "resume on the current road instead of teleporting back home");
        h.assertTrue(path.get(path.size()-1).pos().equals(B.south(3).above(2)), "resume through the correct building port"); h.succeed();
    }
    @GameTest(template="phase0a_empty")
    public static void stableOffsetsStayWithinOneBlock(GameTestHelper h) {
        var level=h.getLevel(); BlockPos floor=h.absolutePos(new BlockPos(5,2,5));
        for (BlockPos p:BlockPos.betweenClosed(floor.offset(-3,0,-3),floor.offset(3,0,3))) level.setBlock(p,Blocks.STONE.defaultBlockState(),2);
        Set<Integer> offsets=new HashSet<>();
        for(int id=1;id<=32;id++) {
            var pose=RoadPresentation.resolve(level,id,floor.getX()+.5,floor.getY()+1,floor.getZ()+.5,1,0);
            h.assertTrue(Math.abs(pose.z()-floor.getZ()-.5)<=1 && pose.y()==floor.getY()+1,"offset remains supported and within one block");
            h.assertTrue(pose.equals(RoadPresentation.resolve(level,id,floor.getX()+.5,floor.getY()+1,floor.getZ()+.5,1,0)),"no per-frame random jitter");
            offsets.add((int)Math.round((pose.z()-floor.getZ())*100));
        }
        h.assertTrue(offsets.size()>8,"different residents have visibly varied stable lanes"); h.succeed();
    }
    @GameTest(template="phase0a_empty")
    public static void wallAndSingleBlockBridgeConstrainOffsets(GameTestHelper h) {
        var level=h.getLevel(); BlockPos floor=h.absolutePos(new BlockPos(5,2,5));
        for (int x=-2;x<=2;x++) level.setBlock(floor.offset(x,0,0),Blocks.STONE.defaultBlockState(),2);
        for(int id=1;id<=32;id++) {
            var pose=RoadPresentation.resolve(level,id,floor.getX()+.5,floor.getY()+1,floor.getZ()+.5,1,0);
            h.assertTrue(Math.abs(pose.z()-floor.getZ()-.5)<.24,"do not offset into unsupported air beside the bridge");
        }
        for(int y=1;y<=3;y++) level.setBlock(floor.offset(0,y,1),Blocks.STONE.defaultBlockState(),2);
        var pose=RoadPresentation.resolve(level,5,floor.getX()+.5,floor.getY()+1,floor.getZ()+.5,1,0);
        h.assertTrue(RoadPresentation.clearBody(level,pose.x(),pose.y(),pose.z()),"a chosen offset cannot intersect the wall"); h.succeed();
    }
    @GameTest(template="phase0a_empty")
    public static void ladderCentersAndFacesSupport(GameTestHelper h) {
        var level=h.getLevel(); BlockPos pos=h.absolutePos(new BlockPos(5,3,5));
        level.setBlock(pos.south(),Blocks.STONE.defaultBlockState(),2);
        level.setBlock(pos,Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,Direction.NORTH),2);
        var a=RoadPresentation.resolve(level,1,pos.getX()+.5,pos.getY()+.25,pos.getZ()+.5,1,0);
        var b=RoadPresentation.resolve(level,27,pos.getX()+.5,pos.getY()+.25,pos.getZ()+.5,1,0);
        h.assertTrue(a.equals(b)&&a.climbing()&&a.ladderYaw()==0,"ladder ignores side preference and faces its supporting south wall");
        h.assertTrue(a.y()==pos.getY()+.25,"preserve vertical progress instead of snapping to the floor"); h.succeed();
    }
    @GameTest(template="phase0a_empty")
    public static void stairsUseLocalCollisionSurface(GameTestHelper h) {
        var level=h.getLevel(); BlockPos pos=h.absolutePos(new BlockPos(5,3,5));
        level.setBlock(pos,Blocks.STONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.EAST).setValue(StairBlock.HALF,Half.BOTTOM),2);
        Double low=RoadPresentation.surface(level,pos.getX()+.2,pos.getY()+1,pos.getZ()+.5);
        Double high=RoadPresentation.surface(level,pos.getX()+.8,pos.getY()+1,pos.getZ()+.5);
        h.assertTrue(low!=null&&high!=null&&low==pos.getY()+.5&&high==pos.getY()+1,"stair foot height follows the correct half, not the column top");
        level.setBlock(pos.above(6),Blocks.STONE.defaultBlockState(),2);
        h.assertTrue(low.equals(RoadPresentation.surface(level,pos.getX()+.2,pos.getY()+1,pos.getZ()+.5)),"an overhead roof must not move feet upstairs"); h.succeed();
    }
    @GameTest(template="phase0a_empty")
    public static void roadSpawnMetadataRoundTrips(GameTestHelper h) {
        UUID owner=UUID.randomUUID();
        var packet=new S2CCitizenSpawnPacket(List.of(new S2CCitizenSpawnPacket.Entry(1,512,2048,-512,(byte)1,(byte)2,"Resident",owner),
                new S2CCitizenSpawnPacket.Entry(2,512,2048,-512,(byte)1,(byte)2,"Unmanaged")));
        var buf=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try { packet.encode(buf); new S2CCitizenSpawnPacket(buf); h.assertTrue(!buf.isReadable(),"owned and unmanaged metadata must consume precisely the packet"); }
        finally { buf.release(); } h.succeed();
    }
    private record NavigationFixture(TownRoadNavigation nav, CitizenContainer container, CitizenSim sim,
            int index, BlockPos ladder, CitizenSimScheduler scheduler) {}
    private static NavigationFixture verticalFixture(GameTestHelper h) {
        var level=h.getLevel(); BlockPos ladder=h.absolutePos(new BlockPos(5,3,5));
        for(int y=0;y<=5;y++) {
            level.setBlock(ladder.above(y).south(),Blocks.STONE.defaultBlockState(),2);
            level.setBlock(ladder.above(y),Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,Direction.NORTH),2);
        }
        BlockPos home=ladder.west(), goal=ladder.above(4).east();
        List<BlockPos> steps=new ArrayList<>(); for(int y=0;y<=4;y++)steps.add(ladder.above(y));
        var nav=new TownRoadNavigation();
        nav.update(UUID.randomUUID(),new TownConnectivity.Result(Set.of(home,goal),List.of(new TownConnectivity.Route(home,goal,steps,true))));
        var scheduler=CitizenSimScheduler.get(level); var sim=new CitizenSim(4);
        int i=sim.add(scheduler.allocId(level),(home.getX()<<10)+512,home.getY()<<10,(home.getZ()<<10)+512);
        sim.homePos[i]=home.asLong();sim.state[i]=CitizenState.WANDER;
        CitizenContainer container=new CitizenContainer() {
            public CitizenSim sim(){return sim;}
            public String getCitizenName(int id){return "Road test";}
            public com.teammoeg.frostedheart.content.trade.FHVillagerData getTradeData(int id){return null;}
            public long homeEntrancePosition(int index){return ladder.asLong();}
        };
        return new NavigationFixture(nav,container,sim,i,ladder,scheduler);
    }
    @GameTest(template="phase0a_empty")
    public static void verticalNavigationUsesYUntilTrueArrival(GameTestHelper h) {
        var f=verticalFixture(h); int start=f.sim.py[f.index];
        for(int tick=0;tick<80;tick++) f.nav.tick(f.scheduler,f.container,f.sim,h.getLevel(),f.index,tick);
        h.assertTrue(f.sim.py[f.index]==start+4096 && f.nav.arrived(f.sim,f.index),
                "same X/Z on a ladder is not arrival until the full four-block vertical route finishes");
        h.succeed();
    }
    @GameTest(template="phase0a_empty")
    public static void newWallStopsAndRemovedWallResumes(GameTestHelper h) {
        var f=verticalFixture(h);
        f.nav.tick(f.scheduler,f.container,f.sim,h.getLevel(),f.index,0);
        h.getLevel().setBlock(f.ladder.above(2),Blocks.STONE.defaultBlockState(),2);
        int stopped=f.sim.py[f.index];
        for(int tick=1;tick<20;tick++)f.nav.tick(f.scheduler,f.container,f.sim,h.getLevel(),f.index,tick);
        h.assertTrue(f.sim.py[f.index]==stopped&&!f.nav.arrived(f.sim,f.index),"new obstacle must stop progress, never fall back to a straight path");
        h.getLevel().setBlock(f.ladder.above(2),Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,Direction.NORTH),2);
        for(int tick=20;tick<100;tick++)f.nav.tick(f.scheduler,f.container,f.sim,h.getLevel(),f.index,tick);
        h.assertTrue(f.nav.arrived(f.sim,f.index),"restored terrain allows the existing route to resume");h.succeed();
    }

}
