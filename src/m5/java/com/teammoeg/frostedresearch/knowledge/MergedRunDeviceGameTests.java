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
    @GameTest(batch="m6_device_selection", template="trial_kiln_empty", timeoutTicks=100)
    public static void parallelResearchRecordsDoNotGateFurnace(GameTestHelper h) {
        try (var scope = new ControlledFurnaceGameTests.Scope()) {
            install();
            var actor = scope.actor(h);
            var service = actor.service();
            mergePair(service);
            var journal = new KnowledgeJournalData(KnowledgeView.create(service));
            h.assertTrue(journal.openRun(PROJECT.toString(), LATE.toString()).id().equals(LATE.toString()),
                "journal can still select either research record");
            var furnace = ControlledFurnaceGameTests.furnace(h, new BlockPos(1, 1, 1));
            ControlledFurnaceGameTests.fill(furnace, 2, 1);
            furnace.start(1100, 21);
            ControlledFurnaceGameTests.ticks(furnace, 420);
            for (int slot = 0; slot < 2; slot++) {
                var product = furnace.inventory().getStackInSlot(slot);
                h.assertTrue(product.is(ControlledFurnaceGameTests.item("frostedheart:refractory_brick"))
                    && ResearchSampleBinding.read(product).isEmpty(), "parallel records leave furnace specimens unbound");
            }
            h.succeed();
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

    @GameTest(batch="experiment_returns",template="trial_kiln_empty",timeoutTicks=100)
    public static void experimentReturnsSurviveSaveAndOnlyPayOnce(GameTestHelper h) {
        var scope = new ControlledFurnaceGameTests.Scope();
        try {
            install();
            var actor = scope.actor(h);
            mergePair(actor.service());
            var tool = new ItemStack(Items.SHEARS);
            tool.setDamageValue(17);
            tool.setHoverName(net.minecraft.network.chat.Component.literal("Lab shears"));
            var tables = new ArrayList<ExperimentTableBlockEntity>();
            for (var pos : List.of(new BlockPos(1, 1, 1), new BlockPos(3, 1, 1))) {
                h.setBlock(pos, FRContents.Blocks.EXPERIMENT_TABLE.get());
                var table = (ExperimentTableBlockEntity) h.getBlockEntity(pos);
                tables.add(table);
                com.teammoeg.frostedresearch.mixinutil.IOwnerTile.trySetOwner(table, actor.team.getId());
                h.assertTrue(table.select(actor.player, EARLY, "return_test"), "target is available");
                table.getInventory().setStackInSlot(0, tool.copy());
                table.getInventory().setStackInSlot(1, new ItemStack(Items.CLAY_BALL));
                for (int i = 0; i < table.getOutputs().getSlots(); i++)
                    table.getOutputs().setStackInSlot(i, new ItemStack(Items.STONE, 64));
                h.assertTrue(!table.start(actor.player) && ItemStack.matches(tool, table.getInventory().getStackInSlot(0))
                        && table.getInventory().getStackInSlot(1).getCount() == 1, "full outputs preserve both inputs");
                for (int i = 0; i < table.getOutputs().getSlots(); i++) table.getOutputs().extractItem(i, 64, false);
                table.getOutputs().setStackInSlot(0, new ItemStack(Items.GLASS_BOTTLE, 8));
                h.assertTrue(table.start(actor.player), "can reserve room alongside earlier products");
                var menu = new ExperimentTableMenu(1, actor.player.getInventory(), table);
                int outputIndex = ExperimentTableBlockEntity.MATERIAL_SLOTS;
                h.assertTrue(!menu.getSlot(outputIndex).mayPlace(new ItemStack(Items.GLASS_BOTTLE)),
                        "players cannot fill reserved output slots");
                h.assertTrue(menu.quickMoveStack(actor.player, outputIndex).getCount() == 8
                        && table.getOutputs().getStackInSlot(0).isEmpty(), "previous output remains collectible during a run");
                var saved = table.saveWithFullMetadata();
                table.stop();
                table.load(saved);
            }
            h.runAfterDelay(45, () -> ControlledFurnaceGameTests.guard(scope, () -> {
                try {
                    int bottles = 0, tools = 0, completed = 0;
                    for (var table : tables) {
                        completed += table.completed();
                        for (int i = 0; i < table.getOutputs().getSlots(); i++) {
                            var stack = table.getOutputs().getStackInSlot(i);
                            if (stack.is(Items.GLASS_BOTTLE)) bottles += stack.getCount();
                            else if (ItemStack.matches(tool, stack)) tools++;
                            else h.assertTrue(stack.isEmpty(), "only the exact original and the defined result are returned");
                        }
                    }
                    h.assertTrue(completed == 1 && tools == 1 && bottles == 2, "one credited run delivers both products exactly once");
                    h.succeed();
                } finally {
                    scope.close();
                }
            }));
        } catch (RuntimeException | Error e) {
            scope.close();
            throw e;
        }
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
                    var f=ControlledFurnaceGameTests.furnace(h,new BlockPos(4,1,1));
                    f.claim(a.player);
                    ControlledFurnaceGameTests.fill(f, 1, 2);
                    var menuA=new ControlledFurnaceMenu(30,a.player.getInventory(),f);
                    var menuB=new ControlledFurnaceMenu(31,b.player.getInventory(),f);
                    a.player.containerMenu=menuA;b.player.containerMenu=menuB;
                    var queued=wire(new ControlledFurnaceStartPacket(menuA.containerId, f.getBlockPos(), 998, 20));
                    wire(new ControlledFurnaceStartPacket(menuB.containerId, f.getBlockPos(), 1107, 21)).apply(b.player);
                    queued.apply(a.player);
                    h.assertTrue(f.running() && f.temperature()==1107 && f.timerSeconds()==21
                        && f.inventory().getStackInSlot(ControlledFurnaceBlockEntity.FUEL_SLOT).getCount()==1,
                        "a second start cannot replace settings or spend fuel on a running furnace");
                    h.succeed();
                }finally{scope.close();}
            }));
        }catch(RuntimeException|Error e){scope.close();throw e;}
    }

}
