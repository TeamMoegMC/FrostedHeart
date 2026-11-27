package com.teammoeg.frostedresearch.knowledge;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.*;
import com.teammoeg.chorda.dataholders.team.*;
import com.teammoeg.frostedresearch.*;
import com.teammoeg.frostedresearch.knowledge.definition.*;
import com.teammoeg.frostedresearch.knowledge.experiment.*;
import com.teammoeg.frostedresearch.knowledge.furnace.*;
import com.teammoeg.frostedresearch.knowledge.item.ItemRetrieval;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.observation.*;
import com.teammoeg.frostedresearch.knowledge.state.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

@GameTestHolder("frostedresearch_m5")
@PrefixGameTestTemplate(false)
public final class ControlledFurnaceGameTests {
    static ResourceLocation id(String p){return new ResourceLocation("the_winter_rescue",p);}
    static final ResourceLocation IDEA=id("refractory_firing"),PROJECT=id("refractory_brick"),DESIGN=id("controlled_refractory_production");
    static final AcquisitionSource SOURCE=new AcquisitionSource("test_external",Optional.empty(),1);
    static Item item(String id){return Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)));}
    static <T>T formal(String path,Codec<T> codec){
        try(var r=new java.io.InputStreamReader(Objects.requireNonNull(ControlledFurnaceGameTests.class.getResourceAsStream("/m5-formal-fixtures/"+path+".json")),java.nio.charset.StandardCharsets.UTF_8)){
            return codec.parse(JsonOps.INSTANCE,JsonParser.parseReader(r)).getOrThrow(false,e->{});
        }catch(java.io.IOException e){throw new RuntimeException(e);}
    }
    static void install(){
        var s=new KnowledgeDefinitions.Snapshot(0,Map.of(IDEA,formal("ideas/refractory_firing",IdeaDefinition.CODEC)),
            Map.of(DESIGN,formal("results/controlled_refractory_production",ResultDefinition.CODEC)),
            Map.of(id("refractory_firing"),formal("links/refractory_firing",com.teammoeg.frostedresearch.knowledge.link.LinkRule.CODEC)),Map.of(),
            Map.of(PROJECT,formal("projects/refractory_brick",ProjectDefinition.CODEC)),List.of());
        if(!KnowledgeDefinitions.validate(s).isEmpty())throw new IllegalStateException(KnowledgeDefinitions.validate(s).toString());
        KnowledgeDefinitions.install(s);TechnologyAccessResolver.invalidateAll();
    }
    static final class Actor implements AutoCloseable {
        final ServerPlayer player;final io.netty.channel.embedded.EmbeddedChannel channel;final TeamDataHolder team;boolean closed;
        Actor(GameTestHelper h){
            var server=h.getLevel().getServer();player=new ServerPlayer(server,h.getLevel(),new GameProfile(UUID.randomUUID(),"m5-furnace"));
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            channel=new io.netty.channel.embedded.EmbeddedChannel(connection);connection.setProtocol(net.minecraft.network.ConnectionProtocol.PLAY);
            server.getPlayerList().placeNewPlayer(connection,player);team=CTeamDataManager.get(player);
            player.setNoGravity(true);player.setInvulnerable(true);var pos=h.absolutePos(new BlockPos(1,2,2));player.teleportTo(h.getLevel(),pos.getX(),pos.getY(),pos.getZ(),Set.of(),0,0);
        }
        KnowledgeService service(){return KnowledgeService.forTeam(team,player.serverLevel().getGameTime());}
        public void close(){if(!closed){closed=true;player.server.getPlayerList().remove(player);channel.finishAndReleaseAll();}}
    }
    static final class Scope implements AutoCloseable {
        final KnowledgeDefinitions.Snapshot previous=KnowledgeDefinitions.current();final TeamsAPIProvider provider=TeamsAPI.getAPI();
        final List<Actor> actors=new ArrayList<>();boolean closed;
        Actor actor(GameTestHelper h){var a=new Actor(h);actors.add(a);return a;}
        void share(Actor b,Actor a){var team=a.team.getTeam();TeamsAPI.register(new TeamsAPIProvider(){
            public AbstractTeam getTeamByPlayer(ServerPlayer p){return p==b.player?team:provider.getTeamByPlayer(p);}
            public AbstractTeam getTeamByUuid(UUID id){return provider.getTeamByUuid(id);}
            public String getProviderName(){return provider.getProviderName();}
        });}
        public void close(){if(!closed){closed=true;actors.forEach(Actor::close);TeamsAPI.register(provider);KnowledgeDefinitions.install(previous);TechnologyAccessResolver.invalidateAll();}}
    }
    static UUID begin(Actor a){
        var s=a.service();s.learn(KnowledgeElement.idea(IDEA),SOURCE,KnowledgeService.Grant.NORMAL);
        for(String id:List.of("charcoal_pit:sandy_brick_item","frostedheart:kaolin","frostedheart:bauxite","minecraft:clay_ball"))s.learn(ItemRetrieval.observe(a.player,new ItemStack(item(id))),SOURCE,KnowledgeService.Grant.NORMAL);
        return s.startResearch(PROJECT).orElseThrow();
    }
    static UUID firing(Actor a){var run=begin(a);a.service().advanceStage(run,"prepare");return run;}
    static ControlledFurnaceBlockEntity furnace(GameTestHelper h,BlockPos pos){h.setBlock(pos,FRContents.Blocks.CONTROLLED_FURNACE.get());return (ControlledFurnaceBlockEntity)h.getBlockEntity(pos);}
    static void fill(ControlledFurnaceBlockEntity f, int clay, int fuel) {
        for (int i = 0; i < clay; i++) f.inventory().setStackInSlot(i, new ItemStack(item("frostedheart:fire_clay_ball")));
        f.inventory().setStackInSlot(ControlledFurnaceBlockEntity.FUEL_SLOT, new ItemStack(Items.CHARCOAL, fuel));
    }
    static void ticks(ControlledFurnaceBlockEntity f,int n){for(int i=0;i<n;i++)f.tick();}
    static void guard(Scope scope,Runnable r){try{r.run();}catch(RuntimeException|Error e){scope.close();throw e;}}

    @GameTest(batch="m5_furnace_specimens",template="trial_kiln_empty",timeoutTicks=80)
    public static void independentFiringLocksSlotsAndSettlesInPlace(GameTestHelper h) {
        try (var scope = new Scope()) {
            var actor = scope.actor(h);
            var furnace = furnace(h, new BlockPos(1,1,1));
            furnace.claim(actor.player);
            fill(furnace, 0, 4);
            var menu = new ControlledFurnaceMenu(1, actor.player.getInventory(), furnace);
            actor.player.containerMenu = menu;
            ItemStack clay = new ItemStack(item("frostedheart:fire_clay_ball"), 4);
            h.assertTrue(menu.quickMoveIn(clay) && clay.isEmpty(), "shift-click spreads a stack over single-item slots");
            for (int slot = 0; slot < 4; slot++) h.assertTrue(menu.getSlot(slot).getItem().getCount() == 1, "one item per specimen slot");
            furnace.inventory().setStackInSlot(5, new ItemStack(Items.STONE));
            var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                new ControlledFurnaceStartPacket(menu.containerId, furnace.getBlockPos(), 1107, 20).encode(buffer);
                new ControlledFurnaceStartPacket(buffer).apply(actor.player);
            } finally { buffer.release(); }
            h.assertTrue(actor.service().state().research().isEmpty() && furnace.running() && furnace.temperature() == 1107,
                "manual temperature is exact and firing starts before research");
            h.assertTrue(!menu.getSlot(0).mayPickup(actor.player) && !menu.getSlot(4).mayPlace(new ItemStack(Items.STONE))
                && menu.quickMoveStack(actor.player, 0).isEmpty() && furnace.inventory().extractItem(0, 1, false).isEmpty(),
                "all sample slots stay locked");
            ticks(furnace, 400);
            h.assertTrue(!furnace.running() && menu.getSlot(0).getItem().is(item("frostedheart:fire_clay_ball")),
                "the exact duration threshold retains the input");
            menu.start(actor.player, 1107, 0);
            ticks(furnace, 480);
            h.assertTrue(furnace.running() && menu.getSlot(0).getItem().is(item("frostedheart:fire_clay_ball")),
                "an untimed run holds samples until stop");
            menu.clickMenuButton(actor.player, ControlledFurnaceMenu.STOP);
            for (int slot = 0; slot < 4; slot++) {
                var result = menu.getSlot(slot).getItem();
                h.assertTrue(result.is(item("frostedheart:refractory_brick")) && ResearchSampleBinding.read(result).isEmpty(),
                    "each sample becomes its unbound result in the original slot");
            }
            h.assertTrue(menu.getSlot(5).getItem().is(Items.STONE), "unrecognized samples remain in place");
            h.assertTrue(menu.getSlot(ControlledFurnaceBlockEntity.FUEL_SLOT).getItem().getCount() == 2, "one charcoal per run");
            h.succeed();
        }
    }

    @GameTest(batch="m5_firing_ranges", template="trial_kiln_empty", timeoutTicks=80)
    public static void multipleRecipesMatchByTemperatureAndDuration(GameTestHelper h) {
        var furnace = furnace(h, new BlockPos(1, 1, 1));
        fill(furnace, 1, 9);
        var inventory = furnace.inventory();
        record FiringCase(int temperature, int elapsed, Item expected) {}
        var cases = List.of(
                new FiringCase(1100, 20, Items.SAND),
                new FiringCase(1100, 21, Items.GLASS),
                new FiringCase(1100, 40, Items.GLASS),
                new FiringCase(1100, 41, Items.FLINT),
                new FiringCase(1250, 21, Items.GRAVEL),
                new FiringCase(1250, 41, Items.SAND),
                new FiringCase(1000, 30, Items.SAND));
        for (var test : cases) {
            var original = new ItemStack(Items.SAND);
            original.getOrCreateTag().putBoolean("test_specimen", true);
            inventory.setStackInSlot(1, original);
            furnace.start(test.temperature(), 0);
            ticks(furnace, test.elapsed());
            h.assertTrue(furnace.running() && inventory.getStackInSlot(1) == original,
                    "samples remain unchanged until shutdown");
            furnace.stop();
            var result = inventory.getStackInSlot(1);
            h.assertTrue(result.is(test.expected()), "result for " + test);
            if (test.expected() == Items.SAND) {
                h.assertTrue(result == original, "an unmatched recipe preserves the original stack and its data");
            }
        }

        inventory.setStackInSlot(1, new ItemStack(Items.SAND));
        furnace.start(1100, 21);
        ticks(furnace, 30);
        CompoundTag saved = new CompoundTag();
        furnace.writeCustomNBT(saved, false);
        var restored = new ControlledFurnaceBlockEntity(furnace.getBlockPos(), furnace.getBlockState());
        restored.setLevel(h.getLevel());
        restored.readCustomNBT(saved, false);
        ticks(restored, 390);
        h.assertTrue(!restored.running() && restored.inventory().getStackInSlot(1).is(Items.FLINT),
                "timed shutdown after reload matches another duration range of the same specimen");
        h.assertTrue(restored.inventory().getStackInSlot(0).is(item("frostedheart:refractory_brick")),
                "the uncapped specimen in the same run resolves its own recipe");

        var candidates = saved.getList("firing", Tag.TAG_COMPOUND);
        candidates.removeIf(tag -> ((CompoundTag) tag).getInt("slot") == 1);
        var old = new CompoundTag();
        old.putInt("slot", 1);
        old.putInt("duration", 20);
        old.putInt("duration_max", 40);
        old.put("product", new ItemStack(Items.GLASS).save(new CompoundTag()));
        old.put("overfire_result", new ItemStack(Items.FLINT).save(new CompoundTag()));
        candidates.add(old);
        restored.readCustomNBT(saved, false);
        ticks(restored, 390);
        h.assertTrue(restored.inventory().getStackInSlot(1).is(Items.FLINT),
                "an already running furnace from an older save keeps its pending outcome");
        h.succeed();
    }

    @GameTest(batch="m5_formal_branches",template="trial_kiln_empty",timeoutTicks=80)
    public static void formalAccessAndReferenceFreeze(GameTestHelper h){
        try(var scope=new Scope()){
            install();var a=scope.actor(h);var s=a.service();s.learn(KnowledgeElement.idea(IDEA),SOURCE,KnowledgeService.Grant.NORMAL);
            h.assertTrue(s.startResearch(PROJECT).isEmpty(),"three optional acquisition observations are actual project start requirements");
            var run=begin(a);h.assertTrue(s.state().research().get(run).stageState().visits().get("prepare").ready(),"empty prepare ready but explicit advance");
            var brick=ItemRetrieval.observe(a.player,new ItemStack(item("frostedheart:refractory_brick")));
            s.receive(brick,SOURCE);s.advanceStage(run,"prepare");
            h.assertTrue(s.state().research().get(run).stageState().visits().get("firing").variant().equals("default"),"inbox reference is not archived knowledge");
            s.learnInbox(brick.key());h.assertTrue(s.state().research().get(run).stageState().visits().get("firing").variant().equals("default"),"reference after entry keeps default method");
            var b=scope.actor(h);var r=begin(b);b.service().learn(ItemRetrieval.observe(b.player,new ItemStack(item("frostedheart:refractory_brick"))),SOURCE,KnowledgeService.Grant.NORMAL);b.service().advanceStage(r,"prepare");
            h.assertTrue(b.service().state().research().get(r).stageState().visits().get("firing").variant().equals("reference_item"),"reference before entry selects exact-temperature hint");
            h.assertTrue(KnowledgeState.SCHEMA_VERSION==5 && !s.requirementsMet(run),"schema5 and observation task still required on both paths");h.succeed();
        }
    }
    @GameTest(batch="m5_temperature_boundaries",template="trial_kiln_empty",timeoutTicks=80)
    public static void timingTemperatureAndReload(GameTestHelper h) {
        for (int temperature : new int[]{600, 1049, 1050, 1107, 1150, 1151, 1400}) {
            var f = furnace(h, new BlockPos(1,1,1));
            fill(f, 1, 1);
            f.start(temperature, 21);
            ticks(f, 411);
            var restored = new ControlledFurnaceBlockEntity(f.getBlockPos(), f.getBlockState());
            restored.load(f.saveWithFullMetadata());
            restored.setLevel(h.getLevel());
            f.stop();
            ticks(restored, 8);
            h.assertTrue(restored.running() && restored.timerSeconds() == 1, "resume the precise saved timer");
            ticks(restored, 1);
            boolean good = temperature >= 1050 && temperature <= 1150;
            h.assertTrue(!restored.running() && restored.inventory().getStackInSlot(0).is(good
                ? item("frostedheart:refractory_brick") : FRContents.Items.CRACKED_REFRACTORY_BRICK.get()),
                "timeout resolves the inclusive temperature range in place");
        }
        h.succeed();
    }

    @GameTest(batch="m5_settlement",template="trial_kiln_empty",timeoutTicks=80)
    public static void legacyInventoryMigratesWithoutItemLoss(GameTestHelper h) {
        var f = furnace(h, new BlockPos(1,1,1));
        var old = new net.minecraftforge.items.ItemStackHandler(3);
        old.setStackInSlot(0, new ItemStack(item("frostedheart:fire_clay_ball"), 10));
        old.setStackInSlot(1, new ItemStack(Items.CHARCOAL, 3));
        old.setStackInSlot(2, new ItemStack(item("frostedheart:refractory_brick"), 11));
        var data = new CompoundTag();
        data.put("inventory", old.serializeNBT());
        data.put("pending", new ItemStack(item("frostedheart:refractory_brick"), 2).save(new CompoundTag()));
        data.put("refund", new ItemStack(item("frostedheart:fire_clay_ball"), 4).save(new CompoundTag()));
        f.load(data);
        h.assertTrue(!f.running() && f.inventory().getStackInSlot(0).getCount() == 1, "old furnaces migrate to single-item slots");
        var drops = f.settle();
        h.assertTrue(drops.stream().filter(stack -> stack.is(item("frostedheart:fire_clay_ball"))).mapToInt(ItemStack::getCount).sum() == 14
            && drops.stream().filter(stack -> stack.is(item("frostedheart:refractory_brick"))).mapToInt(ItemStack::getCount).sum() == 13
            && drops.stream().filter(stack -> stack.is(Items.CHARCOAL)).mapToInt(ItemStack::getCount).sum() == 3
            && f.settle().isEmpty(), "old stacks, refundable input and pending output survive exactly once");
        h.succeed();
    }

    @GameTest(batch="m5_live_sampling",template="trial_kiln_empty",timeoutTicks=520)
    public static void trueLoadedTicksAndTeammateFreshObservationWithoutSubmission(GameTestHelper h){
        var scope=new Scope();try{
            install();var a=scope.actor(h);var b=scope.actor(h);scope.share(b,a);var s=a.service();var run=firing(a);
            var f=furnace(h,new BlockPos(1,1,1));fill(f,1,1);f.claim(a.player);f.start(1100,21);h.assertTrue(f.running(),"start actual ticking furnace");
            var outsider=scope.actor(h);h.assertTrue(!f.claim(outsider.player),"other teams cannot operate this furnace");
            h.onEachTick(()->{if(!b.closed)ObservationSessions.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,b.player));});
            h.runAfterDelay(398,()->guard(scope,()->h.assertTrue(f.inventory().getStackInSlot(0).is(item("frostedheart:fire_clay_ball")),"real server ticks still running before400")));
            h.runAfterDelay(425,()->guard(scope,()->{
                var sample=f.inventory().extractItem(0,1,false);
                h.assertTrue(sample.is(item("frostedheart:refractory_brick")) && ResearchSampleBinding.read(sample).isEmpty(),
                    "real server ticker produces an unbound sample");
                h.assertTrue(!s.requirementsMet(run),"the brick still needs a fresh observation");
                b.player.setItemInHand(InteractionHand.MAIN_HAND,sample);a.close();
                ObservationSessions.handle(b.player,new ObservationActionPacket(ObservationActionPacket.Action.HELD_ITEM,BlockPos.ZERO,0,-1,ObservationActionPacket.Method.KEY));
            }));
            h.runAfterDelay(425 + ObservationSessions.DURATION_TICKS + 5,()->guard(scope,()->{
                h.assertTrue(s.requirementsMet(run) && b.player.getMainHandItem().getCount()==1,"teammate fresh observation completes without consuming or submitting; maker offline");
                h.assertTrue(!s.query(KnowledgeKey.result(DESIGN)).inArchive(),"observation does not silently grant result");
                s.finishResearch(run);h.assertTrue(s.query(KnowledgeKey.result(DESIGN)).inArchive()
                   ,"normal produce learns the completed result when understanding is already met");scope.close();h.succeed();
            }));
        }catch(RuntimeException|Error e){scope.close();throw e;}
    }
    @GameTest(batch="m5_index_reload",template="trial_kiln_empty",timeoutTicks=80)
    public static void finalIndexOneHopExtraTagsAndObservationUse(GameTestHelper h){
        var server=h.getLevel().getServer();var manager=server.getRecipeManager();List<Recipe<?>> previous=new ArrayList<>(manager.getRecipes());
        var defs=KnowledgeDefinitions.current();try{
            RefractoryObservationIndex.refresh(server);var items=RefractoryObservationIndex.items();var blocks=RefractoryObservationIndex.blocks();
            h.assertTrue(items.contains(new ResourceLocation("minecraft:smooth_basalt")) && blocks.contains(new ResourceLocation("minecraft:smooth_basalt")),"tag ingredient direct BlockItem indexed as item and block");
            h.assertTrue(!items.contains(new ResourceLocation("minecraft:gilded_blackstone")),"one hop excludes product of product");
            h.assertTrue(items.contains(new ResourceLocation("minecraft:diamond")) && blocks.contains(new ResourceLocation("minecraft:diamond_block")),"explicit extras and absent optional mod tags load");
            var record=new Observation(UUID.randomUUID(),Observation.Type.ITEM,new ResourceLocation("minecraft:smooth_basalt"),Map.of(),Observation.Source.command(),Optional.empty());
            h.assertTrue(KnowledgeDefinitions.current().hasObservationUse(record),"indexed product is usable observation knowledge");
            long revision=KnowledgeDefinitions.current().revision();manager.replaceRecipes(previous.stream().filter(r->!r.getId().equals(new ResourceLocation("m5_test:direct"))).toList());
            RefractoryObservationIndex.refresh(server);h.assertTrue(!RefractoryObservationIndex.matches(record) && KnowledgeDefinitions.current().revision()>revision,"reload drops stale index entries and revises uses");
            manager.replaceRecipes(previous);RefractoryObservationIndex.refresh(server);h.assertTrue(RefractoryObservationIndex.matches(record),"final recipe restoration restores use");h.succeed();
        }finally{manager.replaceRecipes(previous);KnowledgeDefinitions.install(defs);RefractoryObservationIndex.refresh(server);}
    }
    @GameTest(batch="m5_idea_signals",template="trial_kiln_empty",timeoutTicks=80)
    public static void mineralFormsShareObservationPrerequisitesAndDiscounts(GameTestHelper h){
        try(var scope=new Scope()){
            install();var actor=scope.actor(h);
            var kaolins=List.of("frostedheart:kaolin","frostedheart:crushed_raw_kaolin","frostedheart:kaolin_dust","frostedheart:kaolin_block","frostedheart:kaolin_permafrost");
            var bauxites=List.of("frostedheart:bauxite","frostedheart:crushed_raw_bauxite","frostedheart:bauxite_dust","frostedheart:bauxite_block","frostedheart:bauxite_permafrost");
            var link=KnowledgeDefinitions.current().links().get(id("refractory_firing"));
            for(String kaolin:kaolins)for(String bauxite:bauxites){
                var s=new KnowledgeService(UUID.randomUUID(),new KnowledgeState(),1,()->{});
                for(String material:List.of("charcoal_pit:sandy_brick_item","minecraft:clay_ball",kaolin,bauxite))
                    s.learn(ItemRetrieval.observe(actor.player,new ItemStack(item(material))),SOURCE,KnowledgeService.Grant.NORMAL);
                h.assertTrue(KnowledgeLinkSessions.numbers(s,link)==0,"any form pair provides all three auxiliary groups: "+kaolin+" / "+bauxite);
                s.learn(KnowledgeElement.idea(IDEA),SOURCE,KnowledgeService.Grant.NORMAL);
                h.assertTrue(s.startResearch(PROJECT).isPresent(),"same form pair satisfies project access");
                for(String extra:kaolins)s.learn(ItemRetrieval.observe(actor.player,new ItemStack(item(extra))),SOURCE,KnowledgeService.Grant.NORMAL);
                for(String extra:bauxites)s.learn(ItemRetrieval.observe(actor.player,new ItemStack(item(extra))),SOURCE,KnowledgeService.Grant.NORMAL);
                h.assertTrue(KnowledgeLinkSessions.numbers(s,link)==0,"additional forms never count as additional auxiliary groups");
            }
            for(var family:List.of(kaolins,bauxites))for(String material:family){
                var s=new KnowledgeService(UUID.randomUUID(),new KnowledgeState(),1,()->{});
                var subject=ItemRetrieval.observe(actor.player,new ItemStack(item(material)));
                IdeaAcquisitionService.fire(s,List.of(new IdeaAcquisitionService.Signal(IdeaAcquisitionService.ITEM_OBTAINED,subject)),()->0);
                h.assertTrue(s.isActive(KnowledgeKey.idea(IDEA)),"obtaining any mineral form can inspire the idea");
            }
            h.succeed();
        }
    }
    @GameTest(batch="m5_idea_signals",template="trial_kiln_empty",timeoutTicks=80)
    public static void formalIdeaHasFourIndependentEntrancesAndNoBrickGate(GameTestHelper h){
        try(var scope=new Scope()){
            install();var a=scope.actor(h);var idea=KnowledgeDefinitions.current().ideas().get(IDEA);
            h.assertTrue(idea.acquisition().required().isEmpty() && idea.acquisition().optional().size()==3,"brick reference absent from idea gates");
            List<Observation> obs=List.of(ItemRetrieval.observe(a.player,new ItemStack(item("frostedheart:kaolin"))).observation().orElseThrow(),
                ItemRetrieval.observe(a.player,new ItemStack(item("frostedheart:refractory_brick"))).observation().orElseThrow(),
                new Observation(UUID.randomUUID(),Observation.Type.BLOCK,new ResourceLocation("frostedheart:refractory_bricks"),Map.of(),Observation.Source.command(),Optional.empty()),
                ItemRetrieval.observe(a.player,new ItemStack(Items.SMOOTH_BASALT)).observation().orElseThrow());
            var events=List.of(IdeaAcquisitionService.ITEM_OBTAINED,IdeaAcquisitionService.OBSERVATION_COMPLETED,IdeaAcquisitionService.OBSERVATION_COMPLETED,RefractoryObservationIndex.EVENT);
            for(int i=0;i<4;i++){var s=new KnowledgeService(UUID.randomUUID(),new KnowledgeState(),1,()->{});IdeaAcquisitionService.fire(s,List.of(new IdeaAcquisitionService.Signal(events.get(i),KnowledgeElement.observation(obs.get(i)))),()->0);
                h.assertTrue(s.query(KnowledgeKey.idea(IDEA)).inArchive(),"each formal trigger can independently acquire the idea: "+i);}
            for(String brick:List.of("charcoal_pit:sandy_brick_item","charcoal_pit:sandy_brick")){
                var s=new KnowledgeService(UUID.randomUUID(),new KnowledgeState(),1,()->{});
                var record=ItemRetrieval.observe(a.player,new ItemStack(item(brick)));
                s.learn(record,SOURCE,KnowledgeService.Grant.NORMAL);
                var keys=List.of(record.key());
                h.assertTrue(s.matchLink(id("refractory_firing"),keys).isPresent(),"real sandy-brick tag accepts either form without raw-material auxiliaries");
                h.assertTrue(KnowledgeLinkSessions.numbers(s,KnowledgeDefinitions.current().links().get(id("refractory_firing")))==3,"brick-only link has three numbers");
                s.learn(KnowledgeElement.idea(IDEA),SOURCE,KnowledgeService.Grant.NORMAL);
                h.assertTrue(s.startResearch(PROJECT).isEmpty(),"idea and mandatory link input do not waive project raw-material knowledge");
                for(String material:List.of("frostedheart:kaolin","frostedheart:bauxite","minecraft:clay_ball"))
                    s.learn(ItemRetrieval.observe(a.player,new ItemStack(item(material))),SOURCE,KnowledgeService.Grant.NORMAL);
                h.assertTrue(s.startResearch(PROJECT).isPresent(),"both real sandy-brick forms satisfy the formal project prerequisite");
            }
            h.succeed();
        }
    }
    @GameTest(batch="m5_unloaded_furnace",template="trial_kiln_empty",timeoutTicks=480)
    public static void actualChunkUnloadPausesAndRestoresBatch(GameTestHelper h){
        var scope=new Scope();guard(scope,()->{
            install();var world=h.getLevel();BlockPos pos=new BlockPos(720,80,720);int cx=pos.getX()>>4,cz=pos.getZ()>>4;
            world.setChunkForced(cx,cz,true);world.getChunkAt(pos);world.setBlockAndUpdate(pos,FRContents.Blocks.CONTROLLED_FURNACE.get().defaultBlockState());
            var f=(ControlledFurnaceBlockEntity)world.getBlockEntity(pos);fill(f,1,1);f.start(1100,30);world.setChunkForced(cx,cz,false);
            h.runAfterDelay(350,()->guard(scope,()->{
                h.assertTrue(!world.getChunkSource().hasChunk(cx,cz),"far furnace chunk really left ServerChunkCache");int frozen=f.timerSeconds();
                h.assertTrue(frozen>0 && frozen<=30,"unloaded batch is unfinished");
                h.runAfterDelay(40,()->guard(scope,()->{
                    try{
                        h.assertTrue(!world.getChunkSource().hasChunk(cx,cz) && f.timerSeconds()==frozen,"unloaded furnace executes no ticks");world.getChunkAt(pos);
                        var restored=(ControlledFurnaceBlockEntity)world.getBlockEntity(pos);
                        h.assertTrue(restored.timerSeconds()==frozen && restored.running(),"actual chunk restore keeps remaining duration and locked slots without catchup");
                        h.succeed();
                    }finally{world.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());scope.close();}
                }));
            }));
        });
    }
    @GameTest(batch="m5_zero_online_reload",template="empty",timeoutTicks=600)
    public static void actualServerReloadWithZeroPlayersRefreshesFinalTagsAndUses(GameTestHelper h){
        var server=h.getLevel().getServer();h.assertTrue(server.getPlayerList().getPlayers().isEmpty(),"actual zero online reload");
        RefractoryObservationIndex.clear();long revision=KnowledgeDefinitions.current().revision();
        var reload=server.reloadResources(server.getPackRepository().getSelectedIds());
        h.succeedWhen(()->{
            h.assertTrue(reload.isDone(),"waiting for actual server resource reload");reload.join();
            h.assertTrue(RefractoryObservationIndex.items().contains(new ResourceLocation("minecraft:smooth_basalt"))
                && RefractoryObservationIndex.blocks().contains(new ResourceLocation("minecraft:diamond_block")),"final tag and RecipeManager contents rebuild with no online player");
            h.assertTrue(KnowledgeDefinitions.current().revision()>revision,"observation uses refresh definitions revision after reload");
        });
    }

    @GameTest(batch="m5_codec_and_crafting",template="trial_kiln_empty",timeoutTicks=80)
    public static void formalRecipesNetworkRoundTripAndPublicEarlyCrafting(GameTestHelper h){
        try(var scope=new Scope()){
            install();var a=scope.actor(h);var manager=h.getLevel().getRecipeManager();var registry=h.getLevel().registryAccess();
            {
                var recipe=(ControlledFiringRecipe)manager.byKey(id("research/refractory_brick_trial")).orElseThrow();
                var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                try{
                    var serializer=new ControlledFiringRecipe.Serializer();serializer.toNetwork(buffer,recipe);var restored=serializer.fromNetwork(recipe.id(),buffer);
                    h.assertTrue(restored.input().is(recipe.input().getItem()) && restored.duration()==recipe.duration()
                        && restored.temperatureMin()==recipe.temperatureMin() && restored.durationMax() == 0,
                        "custom recipe retains sample item, temperature and uncapped duration");
                    var capped = (ControlledFiringRecipe) manager.byKey(new ResourceLocation("m5_test", "capped_firing")).orElseThrow();
                    buffer.clear();
                    serializer.toNetwork(buffer, capped);
                    var restoredCapped = serializer.fromNetwork(capped.id(), buffer);
                    h.assertTrue(restoredCapped.durationMax() == 40 && restoredCapped.result().is(Items.GLASS),
                            "custom recipe retains upper duration bound and result");
                }finally{buffer.release();}
            }
            var grid=new net.minecraft.world.inventory.TransientCraftingContainer(a.player.inventoryMenu,3,3);
            for(int i=0;i<9;i++)grid.setItem(i,new ItemStack(i<4?item("frostedheart:kaolin_dust"):i<8?item("frostedheart:bauxite_dust"):Items.CLAY_BALL));
            var hand=manager.byKey(id("research/fire_clay_hand_mix")).orElseThrow();
            h.assertTrue(((CraftingRecipe)hand).matches(grid,h.getLevel()) && ((CraftingRecipe)hand).assemble(grid,registry).getCount()==1 && ResearchHooks.canUseRecipe(a.team.getId(),hand),"actual public nine-grid hand recipe yields one clay before any research");
            for(int i=0;i<9;i++)grid.setItem(i,new ItemStack(i==3||i==5?Items.SAND:i==4?Items.CAMPFIRE:Items.CLAY_BALL));
            var furnaceRecipe=(CraftingRecipe)manager.byKey(id("research/controlled_furnace")).orElseThrow();
            h.assertTrue(furnaceRecipe.matches(grid,h.getLevel()) && furnaceRecipe.assemble(grid,registry).is(FRContents.Blocks.CONTROLLED_FURNACE.get().asItem())
                && ResearchHooks.canUseRecipe(a.team.getId(),furnaceRecipe),"clay/sand/campfire craft real furnace without brick or Design");
            var f=furnace(h,new BlockPos(1,1,1));f.claim(a.player);fill(f,1,0);
            var menu=new ControlledFurnaceMenu(1,a.player.getInventory(),f);a.player.containerMenu=menu;
            menu.start(a.player, 1100, 21);
            h.assertTrue(!f.running() && f.message().equals("need_fuel") && menu.getSlot(0).hasItem(),"missing fuel leaves the specimen in place");
            h.succeed();
        }
    }

}
