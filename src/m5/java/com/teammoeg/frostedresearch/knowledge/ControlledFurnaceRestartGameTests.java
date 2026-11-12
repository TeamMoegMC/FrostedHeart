package com.teammoeg.frostedresearch.knowledge;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.knowledge.definition.KnowledgeDefinitions;
import com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding;
import com.teammoeg.frostedresearch.knowledge.furnace.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.gametest.*;
import java.nio.file.*;
import java.util.*;

@GameTestHolder("frostedresearch_m5_restart")
@PrefixGameTestTemplate(false)
public final class ControlledFurnaceRestartGameTests {
    @GameTest(batch="m5_process_restart",template="empty",timeoutTicks=80)
    public static void actualProcessRestoresRunningAndBlockedFinishedBatch(GameTestHelper h)throws Exception{
        var server=h.getLevel().getServer();var world=server.overworld();var marker=server.getWorldPath(LevelResource.ROOT).resolve("m5-restart.nbt");
        var previous=KnowledgeDefinitions.current();ControlledFurnaceGameTests.install();
        try{
            h.assertTrue(RefractoryObservationIndex.items().contains(new ResourceLocation("minecraft:smooth_basalt")),"startup final index built with zero online players");
            if(System.getProperty("frostedresearch.m5RestartPhase","write").equals("write")){
                try(var a=new ControlledFurnaceGameTests.Actor(h)){
                    UUID run=ControlledFurnaceGameTests.firing(a);BlockPos running=new BlockPos(600,80,600),blocked=running.east(3);world.getChunkAt(running);
                    world.setBlockAndUpdate(running,FRContents.Blocks.CONTROLLED_FURNACE.get().defaultBlockState());world.setBlockAndUpdate(blocked,FRContents.Blocks.CONTROLLED_FURNACE.get().defaultBlockState());
                    var f=(ControlledFurnaceBlockEntity)world.getBlockEntity(running);var g=(ControlledFurnaceBlockEntity)world.getBlockEntity(blocked);
                    for(var furnace:List.of(f,g)){ControlledFurnaceGameTests.fill(furnace,1,1);ControlledFurnaceGameTests.temp(furnace,1100);furnace.start(a.player);}
                    ControlledFurnaceGameTests.ticks(f,100);f.adjust(1);
                    g.inventory().setStackInSlot(2,new ItemStack(Items.STONE,64));ControlledFurnaceGameTests.ticks(g,400);
                    var n=new CompoundTag();n.putUUID("team",a.team.getId());n.putUUID("run",run);n.putLong("running",running.asLong());n.putLong("blocked",blocked.asLong());n.putInt("remaining",f.remaining());NbtIo.writeCompressed(n,marker.toFile());
                }
            }else{
                h.assertTrue(Files.isRegularFile(marker),"requires previous independent write process");var n=NbtIo.readCompressed(marker.toFile());
                var team=CTeamDataManager.INSTANCE.get(n.getUUID("team"));h.assertTrue(team!=null,"new JVM restores real team files");
                var service=KnowledgeService.forTeam(team,world.getGameTime());service.initialize();var run=n.getUUID("run");
                h.assertTrue(service.state().research().get(run).stageState().current().equals("firing"),"real team current run persists");
                BlockPos running=BlockPos.of(n.getLong("running")),blocked=BlockPos.of(n.getLong("blocked"));world.getChunkAt(running);
                var f=(ControlledFurnaceBlockEntity)world.getBlockEntity(running);var g=(ControlledFurnaceBlockEntity)world.getBlockEntity(blocked);
                h.assertTrue(f!=null && f.remaining()>0 && f.remaining()<=n.getInt("remaining") && f.temperature()==1150 && f.lockedTemperature()==1100,"remaining loaded ticks and distinct next/process settings survive without offline catchup");
                h.assertTrue(f.binding().filter(b->b.team().equals(team.getId())&&b.run().equals(run)).isPresent(),"running batch keeps real trial binding");
                h.assertTrue(g!=null && g.busy() && g.remaining()==0 && g.inventory().getStackInSlot(2).getCount()==64,"blocked pending result survives new process");
                g.inventory().setStackInSlot(2,ItemStack.EMPTY);g.tick();var product=g.inventory().getStackInSlot(2);
                h.assertTrue(product.is(ControlledFurnaceGameTests.item("frostedheart:refractory_brick")) && product.getCount()==1 && ResearchSampleBinding.read(product).filter(b->b.run().equals(run)).isPresent(),"restored pending delivers exactly one real bound brick");
                var drops=f.settle();h.assertTrue(drops.stream().filter(s->s.is(ControlledFurnaceGameTests.item("frostedheart:fire_clay_ball"))).mapToInt(ItemStack::getCount).sum()==1,"unfinished persisted batch remains refundable");
            }
            h.succeed();
        }finally{KnowledgeDefinitions.install(previous);TechnologyAccessResolver.invalidateAll();}
    }
}
