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
    public static void populationTracksAllCavitiesAndNewParticlesAppearInExcavation(GameTestHelper h) {
        var game=new AirflowGame();var cloud=new AirflowParticles(1717);
        for(int tick=0;tick<120;tick++)cloud.tick(game);
        int initial=cloud.targetCount(),area=cloud.openCount();
        h.assertTrue(initial==(int)Math.ceil(area*AirflowParticles.DENSITY) && cloud.particles().size()==initial,"initial population follows all pre-open cavities");
        var old=Collections.newSetFromMap(new IdentityHashMap<AirflowParticles.Particle,Boolean>());old.addAll(cloud.particles());
        game.dig(8*28+7,10*28+10);
        for(int tick=0;tick<8;tick++)cloud.tick(game);
        h.assertTrue(cloud.openCount()>area && cloud.particles().size()==cloud.targetCount() && cloud.targetCount()>initial,"excavation increases population at fixed density");
        h.assertTrue(cloud.particles().stream().anyMatch(p -> !old.contains(p) && p.x>5 && p.x<12 && p.y<12),"new air can appear in a disconnected freshly excavated cavity");
        for(var p:cloud.particles())h.assertTrue(AirflowParticles.fits(game,p.x,p.y),"spawn and contact correction keep enlarged colliders outside bricks");
        cloud.clear();h.assertTrue(cloud.particles().isEmpty() && cloud.targetCount()==0,"reset removes population and geometry history");
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
