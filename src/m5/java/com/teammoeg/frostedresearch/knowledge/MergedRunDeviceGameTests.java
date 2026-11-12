package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.knowledge.client.KnowledgeJournalData;
import com.teammoeg.frostedresearch.knowledge.definition.KnowledgeDefinitions;
import com.teammoeg.frostedresearch.knowledge.experiment.*;
import com.teammoeg.frostedresearch.knowledge.furnace.*;
import com.teammoeg.frostedresearch.knowledge.item.ItemRetrieval;
import com.teammoeg.frostedresearch.knowledge.model.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import java.util.*;
import static com.teammoeg.frostedresearch.knowledge.MergedResearchRunGameTests.*;

@GameTestHolder("frostedresearch_m6_devices")
@PrefixGameTestTemplate(false)
public final class MergedRunDeviceGameTests {
    @GameTest(batch="m6_device_selection",template="trial_kiln_empty",timeoutTicks=100)
    public static void mergedStageOrderAndExplicitFurnaceSelectionSurviveNbt(GameTestHelper h){
        try(var scope=new ControlledFurnaceGameTests.Scope()){
            install();var a=scope.actor(h);var s=a.service();mergePair(s);var data=new KnowledgeJournalData(KnowledgeView.create(s));
            var f=ControlledFurnaceGameTests.furnace(h,new BlockPos(1,1,1));ControlledFurnaceGameTests.fill(f,2,2);ControlledFurnaceGameTests.temp(f,1100);
            h.assertTrue(f.start(a.player).equals("choose_target") && f.remaining()==0 && f.inventory().getStackInSlot(0).getCount()==2,"multiple run furnace does not silently choose another run");
            var menu=new ControlledFurnaceMenu(1,a.player.getInventory(),f);a.player.containerMenu=menu;
            h.assertTrue(menu.clickMenuButton(a.player,5) && f.selectedTarget().orElseThrow().run().equals(EARLY),"vanilla menu selects journal default order");
            h.assertTrue(menu.clickMenuButton(a.player,5) && f.selectedTarget().orElseThrow().run().equals(LATE),"same menu explicitly selects second current task");
            f.refreshTargets(a.player);h.assertTrue(f.selectedTarget().orElseThrow().run().toString().equals(data.openRun(PROJECT.toString(),LATE.toString()).id()),"valid explicit furnace selection and chosen journal record agree");
            var saved=f.saveWithFullMetadata();var restored=new ControlledFurnaceBlockEntity(f.getBlockPos(),f.getBlockState());restored.load(saved);restored.setLevel(h.getLevel());
            h.assertTrue(restored.selectedTarget().equals(f.selectedTarget()) && restored.start(a.player).equals("running") && restored.binding().orElseThrow().run().equals(LATE),"real BE NBT preserves selection; start keeps it");
            h.assertTrue(restored.binding().orElseThrow().requirement().equals("late_brick"),"batch belongs to selected record's current stage task");
            var old=restored.saveWithFullMetadata();old.remove("selected_target");old.remove("target_title");old.remove("target_count");
            var compatible=new ControlledFurnaceBlockEntity(f.getBlockPos(),f.getBlockState());compatible.load(old);compatible.setLevel(h.getLevel());
            h.assertTrue(compatible.binding().equals(restored.binding()) && compatible.remaining()==restored.remaining(),"old M5 NBT without optional selection keeps invested batch API");
            compatible.cancel();restored.cancel();h.succeed();
        }
    }
    @GameTest(batch="m6_m3_m4_targets",template="trial_kiln_empty",timeoutTicks=100)
    public static void existingM3M4CycleUsesSameRecordAndStageLabelsWithoutMovingMergeBindings(GameTestHelper h){
        try(var scope=new ControlledFurnaceGameTests.Scope()){
            install();var a=scope.actor(h);var s=a.service();mergePair(s);
            h.setBlock(new BlockPos(1,1,1),FRContents.Blocks.TRIAL_KILN_BALANCED.get());var kiln=(TrialKilnBlockEntity)h.getBlockEntity(new BlockPos(1,1,1));
            var k1=kiln.cycleTarget(s).orElseThrow();var k2=kiln.cycleTarget(s).orElseThrow();
            h.assertTrue(k1.run().equals(EARLY) && k2.run().equals(LATE) && !s.targetLabel(k1).equals(s.targetLabel(k2)),"M3 cycles distinguish record ordinal and stage");
            h.setBlock(new BlockPos(4,1,1),FRContents.Blocks.PROTOTYPE_TUYERE.get());var prototype=(PrototypeTuyereBlockEntity)h.getBlockEntity(new BlockPos(4,1,1));
            prototype.placed(s);h.assertTrue(prototype.binding().isEmpty(),"M4 multiple targets still require explicit choice");
            var t1=prototype.cycle(s).orElseThrow();var t2=prototype.cycle(s).orElseThrow();
            h.assertTrue(t1.run().equals(EARLY) && t2.run().equals(LATE) && !s.targetLabel(t1).equals(s.targetLabel(t2)),"M4 follows same ordered records");
            var serialized=prototype.saveWithFullMetadata();var copy=new PrototypeTuyereBlockEntity(prototype.getBlockPos(),prototype.getBlockState());copy.load(serialized);
            h.assertTrue(copy.binding().equals(prototype.binding()),"existing M4 binding persistence unchanged");
            var oldTeam=UUID.randomUUID();var brick=new ItemStack(ControlledFurnaceGameTests.item("frostedheart:refractory_brick"));
            new ResearchSampleBinding(oldTeam,LATE,"late_brick",new ResourceLocation("frostedresearch:refractory_trial")).write(brick);
            var originalTag=brick.getTag().copy();var incoming=new com.teammoeg.frostedresearch.knowledge.state.KnowledgeState();incoming.recordResearch(s.state().research().get(LATE));s.merge(incoming);
            s.observeResearchTarget(a.player,ItemRetrieval.observe(a.player,brick).observation().orElseThrow(),null,brick);
            h.assertTrue(brick.getTag().equals(originalTag) && !s.state().research().get(LATE).stageState().completedTasks().contains("late_brick"),"knowledge merge neither rewrites old team sample binding nor credits it");
            h.succeed();
        }
    }
    @GameTest(batch="m6_selected_backup",template="trial_kiln_empty",timeoutTicks=100)
    public static void pausedBackupStopsNewBatchesRetainsChoiceAndResumesAfterForgetting(GameTestHelper h){
        try(var scope=new ControlledFurnaceGameTests.Scope()){
            install();var a=scope.actor(h);var s=a.service();mergePair(s);var f=ControlledFurnaceGameTests.furnace(h,new BlockPos(1,1,1));
            ControlledFurnaceGameTests.fill(f,1,1);f.cycleTarget(a.player);var selected=f.selectedTarget().orElseThrow();h.assertTrue(selected.run().equals(EARLY),"chosen early backup");
            var invested=ControlledFurnaceGameTests.furnace(h,new BlockPos(4,1,1));ControlledFurnaceGameTests.fill(invested,1,1);ControlledFurnaceGameTests.temp(invested,1100);
            invested.cycleTarget(a.player);h.assertTrue(invested.start(a.player).equals("running"),"start paid batch before standby");
            s.recordCalculation(LATE,"finish",1);s.finishResearch(LATE);f.refreshTargets(a.player);
            ControlledFurnaceGameTests.ticks(invested,400);var brick=invested.inventory().getStackInSlot(2);
            s.observeResearchTarget(a.player,ItemRetrieval.observe(a.player,brick).observation().orElseThrow(),null,brick);
            h.assertTrue(brick.getCount()==1 && !s.state().research().get(EARLY).stageState().completedTasks().contains("early_brick"),"paid batch delivers but standby does not advance from its sample");
            h.assertTrue(f.targetCount()==0 && f.selectedTarget().filter(selected::equals).isPresent() && f.start(a.player).equals("no_trial")
                && f.inventory().getStackInSlot(0).getCount()==1,"standby cannot start; optional selected binding and unspent clay are retained");
            s.forget(KnowledgeKey.result(RESULT));f.refreshTargets(a.player);
            h.assertTrue(f.selectedTarget().filter(selected::equals).isPresent() && f.start(a.player).equals("running") && f.binding().filter(selected::equals).isPresent(),"forget resumes same selected record without changing its identity");
            f.cancel();s.observeResearchTarget(a.player,ItemRetrieval.observe(a.player,brick).observation().orElseThrow(),null,brick);
            h.assertTrue(s.state().research().get(EARLY).stageState().completedTasks().contains("early_brick") && brick.getCount()==1,"fresh sample can advance the same preserved task after forgetting");h.succeed();
        }
    }
    @GameTest(batch="m6_invested_experiment",template="trial_kiln_empty",timeoutTicks=160)
    public static void experimentStandbyPreservesSpentMaterialsAndProgressIncludingStaleMenuActions(GameTestHelper h){
        var scope=new ControlledFurnaceGameTests.Scope();try{
            install();var a=scope.actor(h);var s=a.service();mergePair(s);
            h.setBlock(new BlockPos(1,1,1),FRContents.Blocks.EXPERIMENT_TABLE.get());var table=(ExperimentTableBlockEntity)h.getBlockEntity(new BlockPos(1,1,1));
            com.teammoeg.frostedresearch.mixinutil.IOwnerTile.trySetOwner(table,a.team.getId());
            table.getInventory().setStackInSlot(0,new ItemStack(Items.CLAY_BALL));
            h.assertTrue(table.select(a.player,EARLY,"early_experiment") && table.start(a.player),"start actual optional experiment with one paid clay");
            h.runAfterDelay(25,()->ControlledFurnaceGameTests.guard(scope,()->{
                int progress=table.progress();h.assertTrue(progress>0 && table.getInventory().getStackInSlot(0).isEmpty(),"real ticks accrued paid progress");
                s.recordCalculation(LATE,"finish",1);s.finishResearch(LATE);table.check(a.player);
                h.assertTrue(!table.start(a.player) && table.progress()==progress && table.run().equals(EARLY),"stale check/start cannot clear standby investment");
                h.runAfterDelay(30,()->ControlledFurnaceGameTests.guard(scope,()->{
                    h.assertTrue(table.progress()==progress && table.status()==ExperimentTableBlockEntity.Status.PAUSED,"loaded ticks pause without clearing progress");
                    s.forget(KnowledgeKey.result(RESULT));
                    h.runAfterDelay(30,()->ControlledFurnaceGameTests.guard(scope,()->{
                        try{h.assertTrue(table.progress()>progress && table.run().equals(EARLY) && !table.message().equals("standby"),"forget resumes paid experiment and clears obsolete pause message");h.succeed();}
                        finally{scope.close();}
                    }));
                }));
            }));
        }catch(RuntimeException|Error e){scope.close();throw e;}
    }

    private static ExperimentTableActionPacket wire(ExperimentTableActionPacket packet){
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{packet.encode(buffer);return new ExperimentTableActionPacket(buffer);}finally{buffer.release();}
    }
    private static ControlledFurnaceStartPacket wire(ControlledFurnaceStartPacket packet){
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{packet.encode(buffer);return new ControlledFurnaceStartPacket(buffer);}finally{buffer.release();}
    }
    @GameTest(batch="m6_shared_device_clicks",template="trial_kiln_empty",timeoutTicks=160)
    public static void staleSharedDeviceClicksKeepOtherPlayersChosenAndPaidRun(GameTestHelper h){
        var scope=new ControlledFurnaceGameTests.Scope();try{
            install();var a=scope.actor(h);var b=scope.actor(h);scope.share(b,a);var service=a.service();mergePair(service);
            h.setBlock(new BlockPos(1,1,1),FRContents.Blocks.EXPERIMENT_TABLE.get());var table=(ExperimentTableBlockEntity)h.getBlockEntity(new BlockPos(1,1,1));
            com.teammoeg.frostedresearch.mixinutil.IOwnerTile.trySetOwner(table,a.team.getId());
            a.player.containerMenu=new ExperimentTableMenu(20,a.player.getInventory(),table);b.player.containerMenu=new ExperimentTableMenu(21,b.player.getInventory(),table);
            wire(new ExperimentTableActionPacket(table.getBlockPos(),ExperimentTableActionPacket.Action.SELECT,EARLY.toString(),"early_experiment")).apply(a.player);
            var delayedStop=wire(new ExperimentTableActionPacket(table.getBlockPos(),ExperimentTableActionPacket.Action.STOP,EARLY.toString(),"early_experiment"));
            var delayedStart=wire(new ExperimentTableActionPacket(table.getBlockPos(),ExperimentTableActionPacket.Action.START,EARLY.toString(),"early_experiment"));
            table.getInventory().setStackInSlot(0,new ItemStack(Items.CLAY_BALL));
            wire(new ExperimentTableActionPacket(table.getBlockPos(),ExperimentTableActionPacket.Action.SELECT,LATE.toString(),"late_experiment")).apply(b.player);
            wire(new ExperimentTableActionPacket(table.getBlockPos(),ExperimentTableActionPacket.Action.START,LATE.toString(),"late_experiment")).apply(b.player);
            h.runAfterDelay(25,()->ControlledFurnaceGameTests.guard(scope,()->{
                try{
                    int progress=table.progress();h.assertTrue(progress>0 && table.status()==ExperimentTableBlockEntity.Status.RUNNING,"B really started and paid for R2 on the shared table");
                    delayedStop.apply(a.player);delayedStart.apply(a.player);
                    h.assertTrue(table.run().equals(LATE) && table.requirement().equals("late_experiment") && table.progress()==progress
                        && table.status()==ExperimentTableBlockEntity.Status.RUNNING && table.getInventory().getStackInSlot(0).isEmpty(),"A's old R1 stop/start only refresh; R2 paid progress is intact");
                    var f=ControlledFurnaceGameTests.furnace(h,new BlockPos(4,1,1));ControlledFurnaceGameTests.fill(f,1,1);ControlledFurnaceGameTests.temp(f,1100);
                    var menuA=new ControlledFurnaceMenu(30,a.player.getInventory(),f);var menuB=new ControlledFurnaceMenu(31,b.player.getInventory(),f);
                    a.player.containerMenu=menuA;b.player.containerMenu=menuB;f.cycleTarget(a.player);
                    var oldSeen=f.selectedTarget();h.assertTrue(oldSeen.orElseThrow().run().equals(EARLY),"A saw furnace R1");
                    var queued=wire(new ControlledFurnaceStartPacket(menuA.containerId,f.getBlockPos(),true,oldSeen));
                    f.cycleTarget(b.player);h.assertTrue(f.selectedTarget().orElseThrow().run().equals(LATE),"B selected furnace R2");
                    queued.apply(a.player);
                    h.assertTrue(!f.busy() && f.inventory().getStackInSlot(0).getCount()==1 && f.inventory().getStackInSlot(1).getCount()==1,"A's stale furnace start does not consume or start R2");
                    wire(new ControlledFurnaceStartPacket(menuB.containerId,f.getBlockPos(),true,f.selectedTarget())).apply(b.player);
                    h.assertTrue(f.busy() && f.binding().orElseThrow().run().equals(LATE) && f.inventory().getStackInSlot(0).isEmpty(),"B's current endpoint starts exactly the selected R2 batch");
                    int remaining=f.remaining();queued.apply(a.player);
                    h.assertTrue(f.remaining()==remaining && f.binding().orElseThrow().run().equals(LATE),"late stale request cannot replace the paid batch");
                    h.succeed();
                }finally{scope.close();}
            }));
        }catch(RuntimeException|Error e){scope.close();throw e;}
    }

}
