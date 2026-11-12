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
    static final ResourceLocation IDEA=id("refractory_firing"),PROJECT=id("refractory_brick"),DESIGN=id("controlled_refractory_production"),BATCH=id("research/refractory_brick_batch");
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
        for(String id:List.of("frostedheart:kaolin","frostedheart:bauxite","minecraft:clay_ball"))s.learn(ItemRetrieval.observe(a.player,new ItemStack(item(id))),SOURCE,KnowledgeService.Grant.NORMAL);
        return s.startResearch(PROJECT).orElseThrow();
    }
    static UUID firing(Actor a){var run=begin(a);a.service().advanceStage(run,"prepare");return run;}
    static ControlledFurnaceBlockEntity furnace(GameTestHelper h,BlockPos pos){h.setBlock(pos,FRContents.Blocks.CONTROLLED_FURNACE.get());return (ControlledFurnaceBlockEntity)h.getBlockEntity(pos);}
    static void fill(ControlledFurnaceBlockEntity f,int clay,int fuel){f.inventory().setStackInSlot(0,new ItemStack(item("frostedheart:fire_clay_ball"),clay));f.inventory().setStackInSlot(1,new ItemStack(Items.CHARCOAL,fuel));}
    static void temp(ControlledFurnaceBlockEntity f,int temp){while(f.temperature()!=temp)f.adjust(Integer.compare(temp,f.temperature()));}
    static void ticks(ControlledFurnaceBlockEntity f,int n){for(int i=0;i<n;i++)f.tick();}
    static void guard(Scope scope,Runnable r){try{r.run();}catch(RuntimeException|Error e){scope.close();throw e;}}
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
    public static void actualBatchBoundaryTicksAndLatchedSettings(GameTestHelper h){
        try(var scope=new Scope()){
            install();var a=scope.actor(h);firing(a);int x=0;
            for(int t:new int[]{600,1000,1050,1100,1150,1200,1400}){
                var f=furnace(h,new BlockPos(1+(x++%3)*2,1,1+(x/3)*2));fill(f,2,2);temp(f,t);
                h.assertTrue(f.start(a.player).equals("running") && f.remaining()==400,"real recipe start consumes one batch");
                ticks(f,399);h.assertTrue(f.inventory().getStackInSlot(2).isEmpty(),"399 loaded ticks do not finish");
                f.adjust(t<1400?1:-1);h.assertTrue(f.lockedTemperature()==t,"running adjustment does not change locked process");ticks(f,1);
                var out=f.inventory().getStackInSlot(2);boolean good=t>=1050&&t<=1150;
                h.assertTrue(out.is(good?item("frostedheart:refractory_brick"):FRContents.Items.CRACKED_REFRACTORY_BRICK.get()) && out.getCount()==1,"inclusive good window; all others cracked");
                h.assertTrue(ResearchSampleBinding.read(out).isPresent()==good,"only actual good trial output is bound");
                ticks(f,800);h.assertTrue(out.getCount()==1 && f.inventory().getStackInSlot(0).getCount()==1 && f.inventory().getStackInSlot(1).getCount()==1,"no automatic second batch");
            }h.succeed();
        }
    }
    @GameTest(batch="m5_settlement",template="trial_kiln_empty",timeoutTicks=80)
    public static void cancelFullOutputRemovalAndDurableBatch(GameTestHelper h){
        try(var scope=new Scope()){
            install();var a=scope.actor(h);firing(a);var f=furnace(h,new BlockPos(1,1,1));fill(f,2,2);temp(f,1100);f.start(a.player);ticks(f,50);
            var saved=f.saveWithFullMetadata();var restored=new ControlledFurnaceBlockEntity(f.getBlockPos(),f.getBlockState());restored.load(saved);restored.setLevel(h.getLevel());
            h.assertTrue(restored.remaining()==350 && restored.binding().equals(f.binding()),"NBT owns remaining duration and exact trial identity");restored.cancel();restored.cancel();
            h.assertTrue(restored.inventory().getStackInSlot(0).getCount()==2 && restored.inventory().getStackInSlot(1).getCount()==1,"cancel returns clay exactly once and never spent fuel");
            f.inventory().setStackInSlot(2,new ItemStack(Items.STONE,64));ticks(f,350);var pending=f.saveWithFullMetadata();
            h.assertTrue(f.remaining()==0 && !ItemStack.of(pending.getCompound("pending")).isEmpty() && ItemStack.of(pending.getCompound("refund")).isEmpty(),"blocked finished output replaces refundable clay");
            f.cancel();h.assertTrue(f.busy(),"cancel cannot turn finished pending product back into raw clay");
            var drops=f.settle();h.assertTrue(drops.stream().filter(s->s.is(item("frostedheart:refractory_brick"))).mapToInt(ItemStack::getCount).sum()==1 && f.settle().isEmpty(),"pending settlement once, no duplication");
            var g=furnace(h,new BlockPos(4,1,1));fill(g,1,1);temp(g,1100);g.start(a.player);g.inventory().setStackInSlot(2,new ItemStack(Items.STONE,64));ticks(g,400);g.inventory().setStackInSlot(2,ItemStack.EMPTY);g.tick();
            h.assertTrue(g.inventory().getStackInSlot(2).is(item("frostedheart:refractory_brick")) && !g.busy(),"clearing blocked output delivers preserved product");
            var raw=furnace(h,new BlockPos(6,1,1));fill(raw,1,1);raw.start(a.player);h.getLevel().destroyBlock(raw.getBlockPos(),true,a.player);
            var entities=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(raw.getBlockPos()).inflate(1));
            h.assertTrue(entities.stream().filter(e->e.getItem().is(item("frostedheart:fire_clay_ball"))).mapToInt(e->e.getItem().getCount()).sum()==1
                && entities.stream().noneMatch(e->e.getItem().is(Items.CHARCOAL)) && raw.settle().isEmpty(),"actual removal refunds internal clay, never fuel");h.succeed();
        }
    }
    @GameTest(batch="m5_permissions",template="trial_kiln_empty",timeoutTicks=80)
    public static void batchRequiresCurrentDesignAndForgettingOnlyAffectsNext(GameTestHelper h){
        try(var scope=new Scope()){
            install();var a=scope.actor(h);var run=firing(a);var f=furnace(h,new BlockPos(1,1,1));f.claim(a.player);f.mode();fill(f,32,4);temp(f,1100);
            h.assertTrue(f.start(a.player).equals("need_design"),"research trial does not unlock batch");
            a.service().receive(KnowledgeElement.result(DESIGN),SOURCE);h.assertTrue(f.start(a.player).equals("need_design"),"inbox result grants no machine capability");
            a.service().learnInbox(KnowledgeKey.result(DESIGN));var recipe=(ControlledFiringRecipe)h.getLevel().getRecipeManager().byKey(BATCH).orElseThrow();
            h.assertTrue(ResearchHooks.canUseRecipe(a.team.getId(),recipe) && f.start(a.player).equals("running"),"valid Design and actual access API permit production");
            a.service().forget(KnowledgeKey.result(DESIGN));ticks(f,599);h.assertTrue(f.inventory().getStackInSlot(2).isEmpty(),"production duration600");ticks(f,1);
            var out=f.inventory().getStackInSlot(2);h.assertTrue(out.getCount()==16 && ResearchSampleBinding.read(out).isEmpty(),"spent batch delivers sixteen unbound normal bricks after forgetting");
            f.inventory().setStackInSlot(2,ItemStack.EMPTY);h.assertTrue(f.start(a.player).equals("need_design"),"forget blocks next production");
            var defs=KnowledgeDefinitions.current();KnowledgeDefinitions.install(KnowledgeDefinitions.Snapshot.empty());h.assertTrue(!ControlledFurnaceBlockEntity.managed(recipe) && f.start(a.player).equals("need_design"),"missing Design never exposes unmanaged production default");KnowledgeDefinitions.install(defs);
            a.service().abandonResearch(run);f.mode();h.assertTrue(f.start(a.player).equals("no_trial"),"abandoned run cannot continue trial supply");h.succeed();
        }
    }
    @GameTest(batch="m5_live_sampling",template="trial_kiln_empty",timeoutTicks=500)
    public static void trueLoadedTicksAndTeammateFreshObservationWithoutSubmission(GameTestHelper h){
        var scope=new Scope();try{
            install();var a=scope.actor(h);var b=scope.actor(h);scope.share(b,a);var s=a.service();var run=firing(a);
            var f=furnace(h,new BlockPos(1,1,1));fill(f,1,1);temp(f,1100);h.assertTrue(f.start(a.player).equals("running"),"start actual ticking furnace");
            var outsider=scope.actor(h);h.assertTrue(f.start(outsider.player).equals("not_owned"),"other teams cannot operate this furnace");
            h.onEachTick(()->{if(!b.closed)ObservationSessions.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,b.player));});
            h.runAfterDelay(398,()->guard(scope,()->h.assertTrue(f.inventory().getStackInSlot(2).isEmpty(),"real server ticks still running before400")));
            h.runAfterDelay(405,()->guard(scope,()->{
                var sample=f.inventory().extractItem(2,1,false);h.assertTrue(sample.getCount()==1,"real server ticker produced sample");
                ItemStack ordinary=new ItemStack(item("frostedheart:refractory_brick"));var record=ItemRetrieval.observe(b.player,ordinary).observation().orElseThrow();
                s.observeResearchTarget(b.player,record,null,ordinary);var wrong=sample.copy();new ResearchSampleBinding(a.team.getId(),UUID.randomUUID(),"observe_fired_brick",new ResourceLocation("frostedresearch:refractory_trial")).write(wrong);s.observeResearchTarget(b.player,record,null,wrong);
                outsider.service().observeResearchTarget(outsider.player,record,null,sample);
                h.assertTrue(!s.requirementsMet(run),"unbound, old run and foreign observer fail");
                b.player.setItemInHand(InteractionHand.MAIN_HAND,sample);a.close();
                ObservationSessions.handle(b.player,new ObservationActionPacket(ObservationActionPacket.Action.HELD_ITEM,BlockPos.ZERO,0,-1,ObservationActionPacket.Method.KEY));
            }));
            h.runAfterDelay(470,()->guard(scope,()->{
                h.assertTrue(s.requirementsMet(run) && b.player.getMainHandItem().getCount()==1,"teammate fresh observation completes without consuming or submitting; maker offline");
                h.assertTrue(!s.query(KnowledgeKey.result(DESIGN)).inArchive(),"observation does not silently grant result");
                s.finishResearch(run);h.assertTrue(s.query(KnowledgeKey.result(DESIGN)).inArchive()
                    && ResearchHooks.canUseRecipe(a.team.getId(),(ControlledFiringRecipe)h.getLevel().getRecipeManager().byKey(BATCH).orElseThrow()),"normal produce learns the completed result when understanding is already met");scope.close();h.succeed();
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
            h.succeed();
        }
    }
    @GameTest(batch="m5_unloaded_furnace",template="trial_kiln_empty",timeoutTicks=480)
    public static void actualChunkUnloadPausesAndRestoresBatch(GameTestHelper h){
        var scope=new Scope();guard(scope,()->{
            install();var a=scope.actor(h);var run=firing(a);var world=h.getLevel();BlockPos pos=new BlockPos(720,80,720);int cx=pos.getX()>>4,cz=pos.getZ()>>4;
            world.setChunkForced(cx,cz,true);world.getChunkAt(pos);world.setBlockAndUpdate(pos,FRContents.Blocks.CONTROLLED_FURNACE.get().defaultBlockState());
            var f=(ControlledFurnaceBlockEntity)world.getBlockEntity(pos);fill(f,1,1);temp(f,1100);f.start(a.player);world.setChunkForced(cx,cz,false);
            h.runAfterDelay(350,()->guard(scope,()->{
                h.assertTrue(!world.getChunkSource().hasChunk(cx,cz),"far furnace chunk really left ServerChunkCache");int frozen=f.remaining();
                h.assertTrue(frozen>0 && frozen<=400,"unloaded batch is unfinished");
                h.runAfterDelay(40,()->guard(scope,()->{
                    try{
                        h.assertTrue(!world.getChunkSource().hasChunk(cx,cz) && f.remaining()==frozen,"unloaded furnace executes no ticks");world.getChunkAt(pos);
                        var restored=(ControlledFurnaceBlockEntity)world.getBlockEntity(pos);
                        h.assertTrue(restored.remaining()==frozen && restored.binding().filter(b->b.run().equals(run)).isPresent(),"actual chunk restore keeps remaining duration and binding without catchup");
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
            for(String mode:List.of("trial","batch")){
                var recipe=(ControlledFiringRecipe)manager.byKey(id("research/refractory_brick_"+mode)).orElseThrow();
                var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                try{
                    var serializer=new ControlledFiringRecipe.Serializer();serializer.toNetwork(buffer,recipe);var restored=serializer.fromNetwork(recipe.id(),buffer);
                    h.assertTrue(restored.input().getCount()==recipe.input().getCount() && restored.fuel().getCount()==recipe.fuel().getCount()
                        && restored.duration()==recipe.duration() && restored.experiment().equals(recipe.experiment()),"actual custom recipe packet retains counted format and independent experiment");
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
            var run=firing(a);var f=furnace(h,new BlockPos(1,1,1));f.claim(a.player);fill(f,1,0);
            var menu=new ControlledFurnaceMenu(1,a.player.getInventory(),f);a.player.containerMenu=menu;
            h.assertTrue(!menu.getSlot(2).mayPlace(new ItemStack(Items.STONE)),"real output slot rejects manual insertion");
            h.assertTrue(menu.clickMenuButton(a.player,3) && f.message().equals("materials") && f.inventory().getStackInSlot(0).getCount()==1,"actual server menu start reports insufficient fuel without consumption");
            for(int i=0;i<30;i++)menu.clickMenuButton(a.player,0);h.assertTrue(f.temperature()==600,"lower clamp");
            for(int i=0;i<30;i++)menu.clickMenuButton(a.player,1);h.assertTrue(f.temperature()==1400,"upper clamp");
            h.succeed();
        }
    }

}
