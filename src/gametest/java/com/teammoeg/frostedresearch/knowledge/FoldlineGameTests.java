package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedresearch.knowledge.game.FoldlineGame;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeElement;
import com.teammoeg.frostedresearch.knowledge.state.AcquisitionSource;
import com.teammoeg.frostedresearch.knowledge.network.KnowledgeWorkbenchActionPacket.Action;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.gametest.*;

import java.util.Optional;

@GameTestHolder("frostedresearch_foldline")
@PrefixGameTestTemplate(false)
public final class FoldlineGameTests {
    @GameTest(template="empty")
    public static void transformedDrawingsKeepTheirSolution(GameTestHelper h) {
        for (int level=0;level<3;level++) for (int seed=0;seed<1024;seed++) {
            var g=new FoldlineGame(level, seed * 2654435761L);
            h.assertTrue(g.trace(g.draft,0,null)==g.exits[0] && !g.solved(g.draft),"Only the first route starts connected");
            h.assertTrue(g.solved(g.solution),"Transformed reference solution must fit budget and all outlets");
        }
        h.succeed();
    }

    private static String version(CompoundTag paper) { return paper.getLong("seed")+"/"+paper.getString("mirrors"); }

    @GameTest(template="trial_kiln_empty")
    public static void stampAndResumeActualResearch(GameTestHelper h) {
        var id=new ResourceLocation("frostedresearch:foldline_debug");
        try(var actor=new TrialKilnGameTests.PlayerFixture(h)) {
            var player=actor.player;
            var service=KnowledgeService.forPlayer(player);
            service.learn(KnowledgeElement.idea(id),new AcquisitionSource("test",Optional.empty(),1),KnowledgeService.Grant.NORMAL);
            var run=service.startResearch(id).orElseThrow();
            var pos=new BlockPos(1,1,1);
            h.setBlock(pos,FRContents.Blocks.DRAWING_DESK.get());
            var desk=(DrawingDeskTileEntity)h.getBlockEntity(pos);
            for(int level=0;level<3;level++) {
                String task="foldline_"+level;
                h.assertTrue(!desk.startAirflow(player,run,task),"Airflow cannot replace a foldline task");
                var paper=desk.foldlineAction(player,run,task,Action.START_FOLDLINE,"","");
                h.assertTrue(paper.getInt("level")==level && !paper.getBoolean("done"),"Open configured difficulty");
                h.assertTrue(!desk.foldlineAction(player,run,task,Action.STAMP_FOLDLINE,version(paper),"").getBoolean("done"),"Cannot stamp the draft");
                var fresh=desk.foldlineAction(player,run,task,Action.REROLL_FOLDLINE,version(paper),"");
                var g=new FoldlineGame(level,fresh.getLong("seed"));
                var edited=desk.foldlineAction(player,run,task,Action.EDIT_FOLDLINE,version(fresh),g.solution);
                h.assertTrue(!edited.getBoolean("done") && edited.getString("mirrors").equals(g.solution),"Editing alone never credits");
                var saved=desk.saveWithFullMetadata();
                desk=(DrawingDeskTileEntity)BlockEntity.loadStatic(desk.getBlockPos(),desk.getBlockState(),saved);
                h.getLevel().setBlockEntity(desk);
                h.assertTrue(desk.foldlineAction(player,run,task,Action.START_FOLDLINE,"","").equals(edited),"Reopening resumes the saved draft");
                h.assertTrue(!desk.foldlineAction(player,run,task,Action.STAMP_FOLDLINE,version(fresh),"").getBoolean("done"),"Old draft cannot stamp another version");
                var stamped=desk.foldlineAction(player,run,task,Action.STAMP_FOLDLINE,version(edited),"");
                h.assertTrue(stamped.getBoolean("done") && service.state().research().get(run).stageState().completedTasks().contains(task),"Stamp credits the selected research task");
                h.assertTrue(desk.foldlineAction(player,run,task,Action.REROLL_FOLDLINE,version(stamped),"").equals(stamped),"Sealed task stays closed");
            }
            h.assertTrue(service.finishResearch(run).get(0).succeeded(),"All three tasks complete the real project");
            h.succeed();
        }
    }
}
