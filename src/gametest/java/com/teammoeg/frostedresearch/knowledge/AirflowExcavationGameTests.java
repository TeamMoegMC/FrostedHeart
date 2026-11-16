package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.knowledge.game.AirflowGame;
import com.teammoeg.frostedresearch.knowledge.game.AirflowParticles;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;
import java.util.List;

@GameTestHolder("frostedresearch_m4")
@PrefixGameTestTemplate(false)
public final class AirflowExcavationGameTests {
    static AirflowGame.Stroke stroke(int x1,int y1,int x2,int y2) { return new AirflowGame.Stroke(y1*28+x1,y2*28+x2); }
    static List<AirflowGame.Stroke> good() { return List.of(stroke(0,20,18,20),stroke(18,20,18,14)); }

    @GameTest(batch="airflow_excavation",template="empty",timeoutTicks=20)
    public static void sideBricksAreDiggableButColdChimneyAndOuterLeaksFail(GameTestHelper h) {
        var game=new AirflowGame();
        h.assertTrue(game.cell(20*28+4)==AirflowGame.OPEN && game.cell(18*28+4)==AirflowGame.OPEN,"starter pocket shows where to continue");
        h.assertTrue(!game.dig(17*28,23*28),"inlet rim resists excavation without a visible extra wall");
        h.assertTrue(!game.dig(-1,0) && !game.dig(0,AirflowGame.SIZE),"outside strokes have no effect");
        game.dig(List.of(stroke(0,20,10,20),stroke(10,20,10,8),stroke(10,8,18,8)));
        h.assertTrue(game.cell(8*28+13)==AirflowGame.OPEN,"former side wall is ordinary diggable brick");
        h.assertTrue(game.failed() && !game.won() && AirflowGame.chimneyOpening(game.view().getInt("leak")),"cold chimney bypass fails");
        h.assertTrue(!game.dig(0,27),"failed boards freeze until reset");
        game.reset();game.dig(20*28,20*28+27);
        h.assertTrue(game.failed() && !game.won() && game.view().getInt("leak")%28==27,"cold external breach fails");
        game.reset();game.dig(good());
        h.assertTrue(game.won() && !game.failed(),"normal inlet is exempt and charcoal-to-chimney route succeeds");
        int[] path=game.view().getIntArray("path");
        h.assertTrue(path[0]==AirflowGame.INLET && path[path.length-1]==AirflowGame.OUTLET
            && Arrays.stream(path).anyMatch(c -> AirflowGame.terrain(c)==AirflowGame.BED),"success path really crosses charcoal");
        for(int i=1;i<path.length;i++)h.assertTrue(Math.abs(path[i]%28-path[i-1]%28)+Math.abs(path[i]/28-path[i-1]/28)==1,"fast strokes yield a continuous path");
        h.succeed();
    }

    @GameTest(batch="airflow_excavation",template="empty",timeoutTicks=20)
    public static void leakageWinsAgainstSimultaneousSuccessButHotOnlyVentsDoNotFail(GameTestHelper h) {
        var game=new AirflowGame();
        game.dig(List.of(good().get(0),good().get(1),stroke(18,20,27,20)));
        h.assertTrue(game.failed() && !game.won(),"whole batch is applied before cold leak takes priority over success");
        game.reset();game.dig(List.of(stroke(27,8,27,11)));
        h.assertTrue(!game.failed(),"unconnected excavation at the outer wall is not a gas leak yet");
        game.reset();game.dig(List.of(good().get(0),good().get(1),stroke(18,12,27,12)));
        h.assertTrue(game.won() && !game.failed(),"a hot-only vent after charcoal does not invent an extra cold-leak failure");
        h.succeed();
    }

    @GameTest(batch="airflow_excavation",template="empty",timeoutTicks=20)
    public static void partialFailedAndCompletedSavesMigrateWithoutLosingCreditedResearch(GameTestHelper h) {
        var game=new AirflowGame();game.dig(20*28,20*28+10);
        var saved=game.save();var restored=new AirflowGame();restored.load(saved);
        h.assertTrue(restored.view().equals(game.view()) && !restored.failed(),"partial excavation round-trips");
        saved.putInt("format",2);restored.load(saved);
        h.assertTrue(restored.dugCount()==game.dugCount() && restored.save().getInt("format")==3,"format2 keeps excavation under new rules");
        game.reset();game.dig(20*28,20*28+27);restored.load(game.save());
        h.assertTrue(restored.view().equals(game.view()) && restored.failed(),"failed leak and freeze survive reload");
        var credited=game.save();credited.putInt("format",2);credited.putBoolean("completed",true);restored.load(credited);
        h.assertTrue(restored.won() && !restored.failed(),"previously credited completion is retained even if the map would now leak");
        game.reset();game.dig(good());restored.load(game.save());
        h.assertTrue(restored.view().equals(game.view()),"new successful route survives save too");
        var legacy=new CompoundTag();legacy.putIntArray("rotations",new int[9]);restored.load(legacy);
        h.assertTrue(!restored.won() && !restored.failed() && restored.dugCount()==0,"rotating board becomes a fresh pocket layout");
        h.succeed();
    }

    @GameTest(batch="airflow_excavation",template="empty",timeoutTicks=20)
    public static void coldParticlesWanderBothWaysWithoutEnteringBricksOrDisconnectedCharcoal(GameTestHelper h) {
        var game=new AirflowGame();game.dig(20*28,20*28+10);
        var cloud=new AirflowParticles(1717);
        for(int tick=0;tick<240;tick++) {
            cloud.tick(game);
            for(var p:cloud.particles()) {
                int type=game.cell((int)p.y*28+(int)p.x);
                h.assertTrue(p.x>=0 && p.x<28 && p.y>=0 && p.y<28 && (type==AirflowGame.OPEN || type==AirflowGame.BED),"collisions keep air out of solid bricks");
                h.assertTrue(!p.heated,"unconnected charcoal cannot heat inlet particles");
            }
        }
        h.assertTrue(cloud.particles().size()==AirflowParticles.LIMIT,"particle count remains bounded");
        h.assertTrue(cloud.particles().stream().anyMatch(p -> p.vx<0) && cloud.particles().stream().anyMatch(p -> p.vx>0),"cold air wanders both directions");
        h.assertTrue(cloud.particles().stream().anyMatch(p -> p.x>5),"particles enter the excavated channel before reaching charcoal");
        game.dig(good());boolean warmed=false;
        for(int tick=0;tick<1000 && !warmed;tick++) {
            cloud.tick(game);warmed=cloud.particles().stream().anyMatch(p -> p.heated);
        }
        h.assertTrue(warmed,"particles warm only after reaching the connected charcoal bed");
        h.succeed();
    }
}
