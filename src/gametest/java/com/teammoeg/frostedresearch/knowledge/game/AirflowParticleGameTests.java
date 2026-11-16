package com.teammoeg.frostedresearch.knowledge.game;

import com.teammoeg.frostedresearch.FRMain;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.stream.IntStream;

@GameTestHolder("frostedresearch_m4")
@PrefixGameTestTemplate(false)
public final class AirflowParticleGameTests {
    @GameTest(batch="airflow_particles",template="empty",timeoutTicks=20)
    public static void collisionRadiusExceedsPointAndPairsExchangeMomentum(GameTestHelper h) {
        var game=new AirflowGame();
        h.assertTrue(AirflowParticles.RADIUS*2*4>1,"physical diameter exceeds the one-pixel point");
        h.assertTrue(!AirflowParticles.fits(game,4.85,20.5) && AirflowParticles.fits(game,4.6,20.5),"brick collision uses physical radius rather than visible center");
        var a=new AirflowParticles.Particle();var b=new AirflowParticles.Particle();
        a.x=1.6;a.y=20.5;a.vx=.2;b.x=2;b.y=20.5;b.vx=-.2;
        h.assertTrue(AirflowParticles.resolvePair(game,a,b),"overlapping physical volumes collide");
        h.assertTrue(a.vx<0 && b.vx>0 && Math.abs(a.vx+b.vx)<1e-9,"equal masses exchange normal momentum");
        h.assertTrue(Math.abs(a.vx*a.vx+b.vx*b.vx-.08)<1e-9,"elastic collision preserves pair kinetic energy");
        h.assertTrue(b.x-a.x>=AirflowParticles.RADIUS*2,"positional correction separates overlapping volumes");
        h.succeed();
    }

    @GameTest(batch="airflow_particles",template="empty",timeoutTicks=20)
    public static void populationTracksAvailableCavitiesAndNewParticlesAppearInExcavation(GameTestHelper h) {
        var game=new AirflowGame();var cloud=new AirflowParticles(1717);
        for(int tick=0;tick<120;tick++)cloud.tick(game);
        int initial=cloud.targetCount(),area=cloud.openCount();
        h.assertTrue(initial==(int)Math.ceil(area*AirflowParticles.DENSITY) && cloud.particles().size()==initial,"initial population follows the available air volume");
        h.assertTrue(initial==6 && area==23,"quarter-cell density counts only the starter pocket before the furnace is ventilated");
        h.assertTrue(cloud.particles().stream().noneMatch(p -> AirflowGame.furnaceCavity((int)p.y*28+(int)p.x)),"preset furnace cavity has no random air before ventilation");
        var old=Collections.newSetFromMap(new IdentityHashMap<AirflowParticles.Particle,Boolean>());old.addAll(cloud.particles());
        game.dig(8*28+7,10*28+10);
        for(int tick=0;tick<8;tick++)cloud.tick(game);
        h.assertTrue(cloud.openCount()>area && cloud.particles().size()==cloud.targetCount() && cloud.targetCount()>initial,"excavation increases population at fixed density");
        h.assertTrue(cloud.particles().stream().anyMatch(p -> !old.contains(p) && p.x>5 && p.x<12 && p.y<12),"new air can appear in a disconnected freshly excavated cavity");
        for(var p:cloud.particles())h.assertTrue(AirflowParticles.fits(game,p.x,p.y),"spawn and contact correction keep enlarged colliders outside bricks");
        cloud.clear();h.assertTrue(cloud.particles().isEmpty() && cloud.targetCount()==0,"reset removes population and geometry history");
        h.succeed();
    }

    private static double distanceOnPath(AirflowParticles.Particle p,int[] path) {
        for(int i=0;i<path.length-1;i++) {
            double x=path[i]%28+.5,y=path[i]/28+.5;
            int dx=path[i+1]%28-path[i]%28,dy=path[i+1]/28-path[i]/28;
            double along=(p.x-x)*dx+(p.y-y)*dy,across=(p.x-x)*dy-(p.y-y)*dx;
            if(Math.abs(across)<1e-8 && along>=-1e-8 && along<=1+1e-8)return i+along;
        }
        return -1;
    }

    @GameTest(batch="airflow_particles",template="empty",timeoutTicks=20)
    public static void completedGasFollowsRouteWarmsAtBedAndLoopsWithoutCrossingBricks(GameTestHelper h) {
        var game=new AirflowGame();var cloud=new AirflowParticles(1717);
        for(int tick=0;tick<120;tick++)cloud.tick(game);
        long contacts=cloud.collisions();
        game.dig(java.util.List.of(new AirflowGame.Stroke(20*28,20*28+18),new AirflowGame.Stroke(20*28+18,14*28+18)));
        h.assertTrue(game.won(),"route fixture wins");
        int[] path=game.path();int firstBed=0;
        while(game.cell(path[firstBed])!=AirflowGame.BED)firstBed++;
        cloud.tick(game);
        h.assertTrue(cloud.targetCount()==45 && cloud.particles().size()==45,"winning cavity retains halved volume-based population");
        var tracked=cloud.particles().get(0);double previous=distanceOnPath(tracked,path);boolean looped=false;
        for(int tick=0;tick<500;tick++) {
            cloud.tick(game);
            h.assertTrue(cloud.lastPairChecks()==0 && cloud.collisions()==contacts,"completed presentation bypasses random turns and particle collisions");
            for(var p:cloud.particles()) {
                double distance=distanceOnPath(p,path);
                h.assertTrue(distance>=0 && AirflowParticles.fits(game,p.x,p.y),"every particle stays on the inlet-to-outlet route outside bricks");
                h.assertTrue(p.heated==(distance>=firstBed-.5),"particles heat at charcoal and become cold again when recycled to inlet");
            }
            double distance=distanceOnPath(tracked,path),expected=(previous+.25)%(path.length-1);
            h.assertTrue(Math.abs(distance-expected)<1e-8,"fixed-speed motion always advances toward the outlet");
            if(distance<previous) {
                looped=true;h.assertTrue(tracked.previousX==tracked.x && tracked.previousY==tracked.y,"recycling cannot interpolate a streak across the board");
            }
            previous=distance;
        }
        h.assertTrue(looped,"completed gas recycles after reaching the outlet");
        var restored=new AirflowGame();restored.load(game.save());var reopened=new AirflowParticles(1234);reopened.tick(restored);
        h.assertTrue(reopened.particles().size()==45 && reopened.particles().stream().allMatch(p -> distanceOnPath(p,path)>=0),"reopening a completed board immediately restores directed flow");
        game.reset();cloud.clear();for(int tick=0;tick<120;tick++)cloud.tick(game);
        h.assertTrue(cloud.targetCount()==6 && cloud.collisions()>0,"reset restores random air only in the starter pocket");
        h.succeed();
    }

    @GameTest(batch="airflow_particles",template="empty",timeoutTicks=20)
    public static void largeCloudRemainsBoundedAndUsesNearbyPairs(GameTestHelper h) {
        var tag=new CompoundTag();tag.putInt("format",3);
        tag.putIntArray("dug",IntStream.range(0,AirflowGame.SIZE).filter(c -> AirflowGame.terrain(c)==AirflowGame.BRICK).toArray());
        var game=new AirflowGame();game.load(tag);var cloud=new AirflowParticles(1717);
        for(int tick=0;tick<240;tick++)cloud.tick(game);
        h.assertTrue(cloud.particles().size()==cloud.targetCount() && cloud.targetCount()<=AirflowParticles.LIMIT,"large cavity fills its bounded volume target");
        int count=cloud.particles().size();
        h.assertTrue(cloud.lastPairChecks()<count*(count-1)/2,"four correction passes still examine fewer pairs than one all-pairs scan");
        long start=System.nanoTime();int ticks=1000;
        for(int tick=0;tick<ticks;tick++)cloud.tick(game);
        long elapsed=System.nanoTime()-start;
        for(var p:cloud.particles())h.assertTrue(Double.isFinite(p.x+p.y+p.vx+p.vy) && AirflowParticles.fits(game,p.x,p.y),"long-running crowded cloud stays finite and outside bricks");
        FRMain.LOGGER.info("AIRFLOW_PARTICLE_PROFILE particles={} mean_us_per_tick={} nearby_pair_checks={}",cloud.particles().size(),Math.round(elapsed/(ticks*1000.0)),cloud.lastPairChecks());
        h.succeed();
    }
}
