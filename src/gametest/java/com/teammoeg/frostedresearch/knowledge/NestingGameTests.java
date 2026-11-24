package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedresearch.knowledge.game.NestingGame;
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

@GameTestHolder("frostedresearch_nesting")
@PrefixGameTestTemplate(false)
public final class NestingGameTests {
    @GameTest(template="empty")
    public static void generatedBoardsAreSolvableWithRealSlack(GameTestHelper h) {
        for(boolean fill:new boolean[]{false,true}) for(int seed=0;seed<1024;seed++){
            var board=new NestingGame(fill,seed);
            h.assertTrue(board.solved(board.solution) && !board.solved(board.empty),"A fresh board must have a solution");
            int blanks=0;
            for(int i=0;i<board.shapes.length;i++) if(NestingGame.at(board.solution,i)==0)blanks++;
            h.assertTrue(fill ? blanks==1 : blanks==0 && board.occupied(board.solution)!=board.groove,"Distinct objectives");
            h.assertTrue(!board.valid("xx") && !board.valid("ff".repeat(board.shapes.length)),"Reject invalid positions");
        }
        h.succeed();
    }
    private static String version(CompoundTag p){return p.getLong("seed")+"/"+p.getString("pieces");}

    @GameTest(template="trial_kiln_empty")
    public static void returnResumeAndFileBothDrawings(GameTestHelper h) {
        try(var actor=new TrialKilnGameTests.PlayerFixture(h)){
            var service=KnowledgeService.forPlayer(actor.player);
            var project=new ResourceLocation("frostedresearch:nesting_debug");
            service.learn(KnowledgeElement.idea(project),new AcquisitionSource("test",Optional.empty(),1),KnowledgeService.Grant.NORMAL);
            var run=service.startResearch(project).orElseThrow();var pos=new BlockPos(1,1,1);
            h.setBlock(pos,FRContents.Blocks.DRAWING_DESK.get());var desk=(DrawingDeskTileEntity)h.getBlockEntity(pos);
            for(String task:new String[]{"pack","fill"}){
                var paper=desk.nestingAction(actor.player,run,task,Action.START_NESTING,"","");
                var board=new NestingGame(task.equals("fill"),paper.getLong("seed"));
                var edited=desk.nestingAction(actor.player,run,task,Action.EDIT_NESTING,version(paper),board.solution);
                h.assertTrue(board.solution.equals(edited.getString("pieces")) && !edited.getBoolean("done"),"Placement alone never stamps");
                int piece=NestingGame.at(board.solution,0)==0?1:0;
                String removed=NestingGame.change(board.solution,piece,0);
                var returned=desk.nestingAction(actor.player,run,task,Action.EDIT_NESTING,version(edited),removed);
                h.assertTrue(returned.getString("pieces").equals(removed),"Return exactly one whole piece");
                desk=(DrawingDeskTileEntity)BlockEntity.loadStatic(desk.getBlockPos(),desk.getBlockState(),desk.saveWithFullMetadata());
                h.getLevel().setBlockEntity(desk);
                h.assertTrue(desk.nestingAction(actor.player,run,task,Action.START_NESTING,"","").equals(returned),"Resume after save");
                h.assertTrue(!desk.nestingAction(actor.player,run,task,Action.STAMP_NESTING,version(returned),"").getBoolean("done"),"Incomplete layout cannot be stamped");
                var restored=desk.nestingAction(actor.player,run,task,Action.EDIT_NESTING,version(returned),board.solution);
                h.assertTrue(desk.nestingAction(actor.player,run,task,Action.STAMP_NESTING,version(restored),"").getBoolean("done"),"Valid stamp credits the task");
            }
            h.assertTrue(service.finishResearch(run).get(0).succeeded(),"Both modes satisfy the project");
            h.succeed();
        }
    }
}
