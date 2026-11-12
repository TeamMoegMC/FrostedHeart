package com.teammoeg.frostedresearch.knowledge;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.frostedresearch.*;
import com.teammoeg.frostedresearch.knowledge.definition.KnowledgeDefinitions;
import com.teammoeg.frostedresearch.knowledge.experiment.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.gametest.*;
import java.nio.file.*;
import java.util.*;

/** Run in two separate Forge JVMs; only the read phase keeps the previous test world. */
@GameTestHolder("frostedresearch_m4_restart")
@PrefixGameTestTemplate(false)
public final class ResearchRestartGameTests {
    @GameTest(batch="m4_process_restart",template="empty",timeoutTicks=80)
    public static void durableStageAndPendingWorldTrial(GameTestHelper h) throws Exception {
        var server=h.getLevel().getServer();Path marker=server.getWorldPath(LevelResource.ROOT).resolve("m4-restart.nbt");
        var previous=KnowledgeDefinitions.current();TuyereResearchGameTests.install();
        try {
            if(System.getProperty("frostedresearch.m4RestartPhase","write").equals("write")) {
                try(var actor=new TuyereResearchGameTests.Actor(h)) {
                    UUID run=TuyereResearchGameTests.begin(actor);
                    h.setBlock(new BlockPos(1,1,1),FRContents.Blocks.DRAWING_DESK.get());
                    TuyereResearchGameTests.solve((com.teammoeg.frostedresearch.blocks.DrawingDeskTileEntity)h.getBlockEntity(new BlockPos(1,1,1)),actor,run);
                    actor.service().advanceStage(run,"airflow");
                    ServerLevel world=server.overworld();BlockPos target=new BlockPos(500,80,500),bellows=target.west();world.getChunkAt(target);
                    world.setBlockAndUpdate(bellows,charcoalPit.core.ModBlockRegistry.Bellows.defaultBlockState().setValue(charcoalPit.block.BlockBellows.FACING,Direction.EAST));
                    world.setBlockAndUpdate(target,FRContents.Blocks.PROTOTYPE_TUYERE.get().defaultBlockState());
                    world.getBlockState(target).getBlock().setPlacedBy(world,target,world.getBlockState(target),actor.player,new ItemStack(FRContents.Blocks.PROTOTYPE_TUYERE.get()));
                    charcoalPit.core.ModBlockRegistry.Bellows.blow(world,bellows,world.getBlockState(bellows));
                    h.assertTrue(PendingResearchProofs.get(world).at(target).isPresent(),"write process creates a real pending trial proof");
                    var tag=new CompoundTag();tag.putUUID("team",actor.team.getId());tag.putUUID("run",run);tag.putLong("position",target.asLong());
                    NbtIo.writeCompressed(tag,marker.toFile());
                }
            } else {
                h.assertTrue(Files.isRegularFile(marker),"read process requires the prior write process marker");
                var tag=NbtIo.readCompressed(marker.toFile());var team=CTeamDataManager.INSTANCE.get(tag.getUUID("team"));
                h.assertTrue(team!=null,"actual Chorda team was restored by the new server process");
                var service=KnowledgeService.forTeam(team,server.overworld().getGameTime());service.initialize();var run=service.state().research().get(tag.getUUID("run"));
                h.assertTrue(run!=null && run.stageState().current().equals("trial") && run.stageState().visits().get("airflow").ready(),"current stage and completed airflow survived process restart");
                h.assertTrue(TechnologyAccessResolver.project(team).recipe(TuyereResearchGameTests.PROTOTYPE).allowed(),"temporary permission is rebuilt from persisted stage history");
                var target=BlockPos.of(tag.getLong("position"));server.overworld().getChunkAt(target);
                h.assertTrue(server.overworld().getBlockState(target).is(charcoalPit.core.ModBlockRegistry.TuyereSandy)
                    && PendingResearchProofs.get(server.overworld()).at(target).filter(b -> b.team().equals(team.getId()) && b.run().equals(run.id())).isPresent(),"normal target and pending proof survived real world/ServerLevel reload");
            }
            h.succeed();
        } finally { KnowledgeDefinitions.install(previous);LegacyResearchBridge.refreshDefinitions();TemporaryResearchRecipes.refresh(); }
    }
}
