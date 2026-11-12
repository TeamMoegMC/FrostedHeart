package com.teammoeg.frostedresearch.knowledge;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.*;
import com.teammoeg.chorda.dataholders.team.*;
import com.teammoeg.frostedresearch.*;
import com.teammoeg.frostedresearch.api.*;
import com.teammoeg.frostedresearch.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedresearch.knowledge.definition.*;
import com.teammoeg.frostedresearch.knowledge.experiment.*;
import com.teammoeg.frostedresearch.knowledge.game.AirflowGame;
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
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Only loaded by the opt-in source set with the actual Charcoal Pit server dependency. */
@GameTestHolder("frostedresearch_m4_cp")
@PrefixGameTestTemplate(false)
public final class TuyereResearchGameTests {
    static ResourceLocation id(String path) { return new ResourceLocation("the_winter_rescue",path); }
    static final ResourceLocation PROJECT=id("iron_bloomery"),IDEA=id("iron_smelting"),DESIGN=id("iron_bloomery_design");
    static final ResourceLocation PROTOTYPE=id("research/prototype_tuyere"),NORMAL=id("research/sandy_tuyere"),BLOOMERY=id("research/main_bloomery");
    static final AcquisitionSource SOURCE=new AcquisitionSource("test_external",Optional.empty(),1);
    static <T> T formal(String path,Codec<T> codec) {
        try(var reader=new java.io.InputStreamReader(Objects.requireNonNull(TuyereResearchGameTests.class.getResourceAsStream("/m4-formal-fixtures/"+path+".json")),java.nio.charset.StandardCharsets.UTF_8)) {
            return codec.parse(JsonOps.INSTANCE,JsonParser.parseReader(reader)).getOrThrow(false,error -> {});
        } catch(java.io.IOException e) { throw new IllegalStateException(e); }
    }
    static void install() {
        var snapshot=new KnowledgeDefinitions.Snapshot(0,Map.of(IDEA,formal("ideas/iron_smelting",IdeaDefinition.CODEC)),
            Map.of(DESIGN,formal("results/iron_bloomery_design",ResultDefinition.CODEC)),Map.of(IDEA,formal("links/iron_smelting",com.teammoeg.frostedresearch.knowledge.link.LinkRule.CODEC)),
            Map.of(),Map.of(PROJECT,formal("projects/iron_bloomery",ProjectDefinition.CODEC)),List.of());
        if(!KnowledgeDefinitions.validate(snapshot).isEmpty()) throw new IllegalStateException(KnowledgeDefinitions.validate(snapshot).toString());
        KnowledgeDefinitions.install(snapshot);LegacyResearchBridge.refreshDefinitions();TemporaryResearchRecipes.refresh();
    }
    static final class Actor implements AutoCloseable {
        final ServerPlayer player;final io.netty.channel.embedded.EmbeddedChannel channel;final TeamDataHolder team;boolean closed;
        Actor(GameTestHelper h) {
            var server=h.getLevel().getServer();player=new ServerPlayer(server,h.getLevel(),new GameProfile(UUID.randomUUID(),"m4-trial"));
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            channel=new io.netty.channel.embedded.EmbeddedChannel(connection);connection.setProtocol(net.minecraft.network.ConnectionProtocol.PLAY);
            server.getPlayerList().placeNewPlayer(connection,player);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            team=CTeamDataManager.get(player);
        }
        KnowledgeService service() { return KnowledgeService.forTeam(team,player.serverLevel().getGameTime()); }
        @Override public void close() { if(!closed) { closed=true;player.server.getPlayerList().remove(player);channel.finishAndReleaseAll(); } }
    }
    static final class Scope implements AutoCloseable {
        final KnowledgeDefinitions.Snapshot previous=KnowledgeDefinitions.current();
        final TeamsAPIProvider provider=TeamsAPI.getAPI();final List<Actor> actors=new ArrayList<>();boolean closed;
        Actor actor(GameTestHelper h) { var actor=new Actor(h);actors.add(actor);return actor; }
        void share(Actor member,Actor owner) {
            var ownerTeam=owner.team.getTeam();
            TeamsAPI.register(new TeamsAPIProvider() {
                public AbstractTeam getTeamByPlayer(ServerPlayer player) { return player==member.player ? ownerTeam : provider.getTeamByPlayer(player); }
                public AbstractTeam getTeamByUuid(UUID uuid) { return provider.getTeamByUuid(uuid); }
                public String getProviderName() { return provider.getProviderName(); }
            });
        }
        @Override public void close() { if(!closed) {
            closed=true;for(var actor:actors) actor.close();TeamsAPI.register(provider);KnowledgeDefinitions.install(previous);LegacyResearchBridge.refreshDefinitions();TemporaryResearchRecipes.refresh();
        } }
    }
    static void learnMaterials(Actor a,Item metal,Item fuel) {
        var service=a.service();service.learn(KnowledgeElement.idea(IDEA),SOURCE,KnowledgeService.Grant.NORMAL);
        for(var item:List.of(metal,fuel)) service.learn(ItemRetrieval.observe(a.player,new ItemStack(item)),SOURCE,KnowledgeService.Grant.NORMAL);
    }
    static UUID begin(Actor actor) {
        learnMaterials(actor,Items.COPPER_INGOT,Items.CHARCOAL);return actor.service().startResearch(PROJECT).orElseThrow();
    }
    static DrawingDeskTileEntity desk(GameTestHelper h,BlockPos local) { h.setBlock(local,FRContents.Blocks.DRAWING_DESK.get());return (DrawingDeskTileEntity)h.getBlockEntity(local); }
    static void solve(DrawingDeskTileEntity desk,Actor actor,UUID run) {
        if(!desk.startAirflow(actor.player,run,"airflow_win")) throw new IllegalStateException("Could not start actual airflow session");
        int[] solved={0,6,12,10,9,3,0,0,0};
        for(int cell:new int[]{3,4,1,2,5}) for(int rotation=0;rotation<4 && desk.airflowView().getIntArray("masks")[cell]!=solved[cell] && !desk.airflowView().getBoolean("won");rotation++)
            desk.rotateAirflow(actor.player,run,"airflow_win",cell,false);
        if(!desk.airflowView().getBoolean("won")) throw new IllegalStateException("Real rotations failed to solve airflow");
    }
    static void placePrototype(GameTestHelper h,Actor actor,BlockPos local) {
        h.setBlock(local,FRContents.Blocks.PROTOTYPE_TUYERE.get());
        var pos=h.absolutePos(local);var state=h.getLevel().getBlockState(pos);
        state.getBlock().setPlacedBy(h.getLevel(),pos,state,actor.player,new ItemStack(FRContents.Blocks.PROTOTYPE_TUYERE.get()));
    }
    static void guard(Scope scope,Runnable work) { try { work.run(); } catch(RuntimeException|Error error) { scope.close();throw error; } }
    @GameTest(batch="m4_formal_airflow",template="trial_kiln_empty",timeoutTicks=80)
    public static void formalGroupsRealRotationsAndDeskPersistence(GameTestHelper h) {
        try(var scope=new Scope()) {
            install();var actor=scope.actor(h);var service=actor.service();
            h.assertTrue(service.startResearch(PROJECT).isEmpty(),"undiscovered formal project cannot start");
            service.learn(KnowledgeElement.idea(IDEA),SOURCE,KnowledgeService.Grant.NORMAL);
            h.assertTrue(service.startResearch(PROJECT).isEmpty(),"idea alone lacks both observation groups");
            var copper=ItemRetrieval.observe(actor.player,new ItemStack(Items.COPPER_INGOT));service.receive(copper,SOURCE);
            service.learn(ItemRetrieval.observe(actor.player,new ItemStack(Items.COAL)),SOURCE,KnowledgeService.Grant.NORMAL);
            h.assertTrue(service.startResearch(PROJECT).isEmpty(),"metal in inbox does not satisfy access");service.learnInbox(copper.key());
            UUID run=service.startResearch(PROJECT).orElseThrow();
            h.assertTrue(!TechnologyAccessResolver.project(actor.team).recipe(PROTOTYPE).allowed(),"prototype managed before airflow success");
            var desk=desk(h,new BlockPos(1,1,1));h.assertTrue(desk.startAirflow(actor.player,run,"airflow_win"),"formal minigame target starts");
            h.assertTrue(!desk.airflowView().getBoolean("won") && !service.state().research().get(run).stageState().visits().get("airflow").ready(),"shuffled board is not an automatic win");
            var before=desk.airflowView();var saved=desk.saveWithFullMetadata();var reloaded=BlockEntity.loadStatic(desk.getBlockPos(),desk.getBlockState(),saved);
            h.getLevel().setBlockEntity(reloaded);desk=(DrawingDeskTileEntity)reloaded;
            h.assertTrue(desk.airflowView().equals(before) && desk.startAirflow(actor.player,run,"airflow_win"),"desk NBT reload and reopening preserve current board");
            solve(desk,actor,run);
            h.assertTrue(desk.getInventory().getStackInSlot(DrawingDeskTileEntity.PAPER_SLOT).isEmpty() && desk.getInventory().getStackInSlot(DrawingDeskTileEntity.INK_SLOT).isEmpty(),"airflow needs no paper/ink");
            h.assertTrue(TechnologyAccessResolver.project(actor.team).recipe(PROTOTYPE).allowed() && service.state().research().get(run).stageState().current().equals("airflow"),"real win immediately grants prototype without auto advance");
            var history=service.state().research().get(run);desk.rotateAirflow(actor.player,run,"airflow_win",3,true);
            h.assertTrue(service.state().research().get(run).equals(history),"repeated winning game cannot duplicate task");
            service.advanceStage(run,"airflow");h.assertTrue(!desk.rotateAirflow(actor.player,run,"airflow_win",3,false),"old desk callback cannot submit to later stage");
            h.assertTrue(LegacyResearchBridge.isReplaced("iron"),"single Design bridge validates both real recipe targets");
            h.succeed();
        }
    }
    @GameTest(batch="m4_bellows_observation",template="trial_kiln_empty",timeoutTicks=150)
    public static void actualDelayedBellowsWithoutBloomeryAndTeammateFreshObservation(GameTestHelper h) {
        var scope=new Scope();guard(scope,() -> {
            install();var placer=scope.actor(h);var observer=scope.actor(h);scope.share(observer,placer);
            var service=placer.service();UUID run=begin(placer);solve(desk(h,new BlockPos(1,1,1)),placer,run);service.advanceStage(run,"airflow");
            BlockPos bellows=new BlockPos(3,1,2),target=new BlockPos(4,1,2);
            h.setBlock(bellows,charcoalPit.core.ModBlockRegistry.Bellows.defaultBlockState().setValue(charcoalPit.block.BlockBellows.FACING,Direction.EAST));
            placePrototype(h,placer,target);
            var world=h.getLevel();var absolute=h.absolutePos(bellows);var targetPos=h.absolutePos(target);
            h.assertTrue(world.getBlockEntity(targetPos) instanceof PrototypeTuyereBlockEntity,"real prototype exists before blow");
            world.getBlockState(absolute).use(world,placer.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(absolute),Direction.UP,absolute,false));
            h.assertTrue(world.getBlockState(targetPos).is(FRContents.Blocks.PROTOTYPE_TUYERE.get()),"use click alone does not count");
            placer.close(); // No personal owner/online requirement: another current team member can observe.
            observer.player.setNoGravity(true);observer.player.setInvulnerable(true);observer.player.setDeltaMovement(Vec3.ZERO);
            h.onEachTick(() -> { if(!observer.closed) ObservationSessions.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,observer.player)); });
            var position=targetPos.offset(-1,1,2);observer.player.teleportTo(world,position.getX()+0.5,position.getY(),position.getZ()+0.5,Set.of(),0,0);
            h.runAfterDelay(8,() -> guard(scope,() -> h.assertTrue(world.getBlockState(targetPos).is(FRContents.Blocks.PROTOTYPE_TUYERE.get()),"no conversion before actual delayed blow")));
            h.runAfterDelay(13,() -> guard(scope,() -> {
                h.assertTrue(world.getBlockState(targetPos).is(charcoalPit.core.ModBlockRegistry.TuyereSandy) && world.getBlockEntity(targetPos)==null,"real blow converts without Bloomery, producing the normal block without a BE");
                h.assertTrue(PendingResearchProofs.get(world).at(targetPos).isPresent(),"successful conversion writes position proof");
                var saved=PendingResearchProofs.get(world).save(new CompoundTag());h.assertTrue(PendingResearchProofs.load(saved).at(targetPos).isPresent(),"pending proof survives SavedData codec round trip");
                observer.player.setItemInHand(InteractionHand.MAIN_HAND,FRContents.Items.MAGNIFYING_GLASS.get().getDefaultInstance());
                ObservationSessions.handle(observer.player,new ObservationActionPacket(ObservationActionPacket.Action.BLOCK,targetPos,-1,-1,ObservationActionPacket.Method.MAIN_HAND_GLASS));
            }));
            h.runAfterDelay(78,() -> { try {
                h.assertTrue(service.state().research().get(run).stageState().completedTasks().contains("observe_tuyere"),"fresh sampling by another current team member completes trial after placer logged out");
                h.assertTrue(PendingResearchProofs.get(world).at(targetPos).isEmpty(),"completion clears pending proof");
                h.assertTrue(service.finishResearch(run).get(0).succeeded(),"normal final stage produces formal Design");
                var access=TechnologyAccessResolver.project(placer.team);h.assertTrue(!access.recipe(PROTOTYPE).allowed() && access.recipe(NORMAL).allowed() && access.recipe(BLOOMERY).allowed(),"temporary prototype source withdrawn, final Design grants both production recipes");
                h.succeed();
            } finally { scope.close(); } });
        });
    }
    @GameTest(batch="m4_proof_recovery",template="trial_kiln_empty",timeoutTicks=80)
    public static void proofLatestPositionWrongTeamRemovalAndRebuild(GameTestHelper h) {
        try(var scope=new Scope()) {
            install();var actor=scope.actor(h);var outsider=scope.actor(h);var service=actor.service();var run=begin(actor);
            solve(desk(h,new BlockPos(1,1,1)),actor,run);service.advanceStage(run,"airflow");
            var world=h.getLevel();BlockPos bellows=new BlockPos(2,1,3),first=new BlockPos(3,1,3),second=new BlockPos(3,1,5);
            h.setBlock(bellows,charcoalPit.core.ModBlockRegistry.Bellows.defaultBlockState().setValue(charcoalPit.block.BlockBellows.FACING,Direction.WEST));placePrototype(h,actor,first);
            var prototype=(PrototypeTuyereBlockEntity)h.getBlockEntity(first);var original=prototype.binding();
            outsider.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            world.getBlockState(h.absolutePos(first)).use(world,outsider.player,InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(h.absolutePos(first)),Direction.UP,h.absolutePos(first),false));
            h.assertTrue(prototype.binding().equals(original),"another team with no candidate cannot clear an existing valid trial binding");
            h.assertTrue(!TuyereTrials.tryBlow(world,h.absolutePos(bellows),world.getBlockState(h.absolutePos(bellows))),"wrong facing does not convert trial");
            world.setBlockAndUpdate(h.absolutePos(bellows),world.getBlockState(h.absolutePos(bellows)).setValue(charcoalPit.block.BlockBellows.FACING,Direction.EAST));
            h.assertTrue(TuyereTrials.tryBlow(world,h.absolutePos(bellows),world.getBlockState(h.absolutePos(bellows))),"actual callback accepts valid bound prototype");
            var normal=ObservationSampling.block(actor.player,h.absolutePos(first));
            outsider.service().observeResearchTarget(outsider.player,normal,h.absolutePos(first),ItemStack.EMPTY);
            h.assertTrue(!service.state().research().get(run).stageState().completedTasks().contains("observe_tuyere"),"another team cannot use the proof");
            var firstProof=PendingResearchProofs.get(world).at(h.absolutePos(first)).orElseThrow();
            h.setBlock(new BlockPos(2,1,5),world.getBlockState(h.absolutePos(bellows)));placePrototype(h,actor,second);
            TuyereTrials.tryBlow(world,h.absolutePos(new BlockPos(2,1,5)),world.getBlockState(h.absolutePos(new BlockPos(2,1,5))));
            h.assertTrue(PendingResearchProofs.get(world).at(h.absolutePos(first)).isEmpty() && PendingResearchProofs.get(world).at(h.absolutePos(second)).filter(firstProof::equals).isPresent(),"new successful specimen replaces the old proof");
            world.setBlockAndUpdate(h.absolutePos(second),Blocks.AIR.defaultBlockState());world.setBlockAndUpdate(h.absolutePos(second),charcoalPit.core.ModBlockRegistry.TuyereSandy.defaultBlockState());
            h.assertTrue(PendingResearchProofs.get(world).at(h.absolutePos(second)).isEmpty(),"actual world replacement invalidates proof even if the same normal block is put back");
            service.observeResearchTarget(actor.player,ObservationSampling.block(actor.player,h.absolutePos(second)),h.absolutePos(second),ItemStack.EMPTY);
            h.assertTrue(!service.state().research().get(run).stageState().completedTasks().contains("observe_tuyere") && TechnologyAccessResolver.project(actor.team).recipe(PROTOTYPE).allowed(),"ordinary replacement is not a trial; recipe remains available for recovery");
            placePrototype(h,actor,second);TuyereTrials.tryBlow(world,h.absolutePos(new BlockPos(2,1,5)),world.getBlockState(h.absolutePos(new BlockPos(2,1,5))));
            var nether=world.getServer().getLevel(net.minecraft.world.level.Level.NETHER);var cross=new BlockPos(500,80,500);nether.getChunkAt(cross);
            nether.setBlockAndUpdate(cross.west(),charcoalPit.core.ModBlockRegistry.Bellows.defaultBlockState().setValue(charcoalPit.block.BlockBellows.FACING,Direction.EAST));
            nether.setBlockAndUpdate(cross,FRContents.Blocks.PROTOTYPE_TUYERE.get().defaultBlockState());
            nether.getBlockState(cross).getBlock().setPlacedBy(nether,cross,nether.getBlockState(cross),actor.player,new ItemStack(FRContents.Blocks.PROTOTYPE_TUYERE.get()));
            charcoalPit.core.ModBlockRegistry.Bellows.blow(nether,cross.west(),nether.getBlockState(cross.west()));
            h.assertTrue(PendingResearchProofs.get(world).at(h.absolutePos(second)).isEmpty() && PendingResearchProofs.get(nether).at(cross).isPresent(),"new successful trial in another dimension replaces the previous proof");
            service.abandonResearch(run);h.assertTrue(PendingResearchProofs.get(nether).at(cross).isEmpty(),"abandon clears cross-dimension run proofs");
            UUID replacement=begin(actor);solve((DrawingDeskTileEntity)h.getBlockEntity(new BlockPos(1,1,1)),actor,replacement);service.advanceStage(replacement,"airflow");
            placePrototype(h,actor,second);charcoalPit.core.ModBlockRegistry.Bellows.blow(world,h.absolutePos(new BlockPos(2,1,5)),world.getBlockState(h.absolutePos(new BlockPos(2,1,5))));
            h.assertTrue(PendingResearchProofs.get(world).at(h.absolutePos(second)).isPresent(),"new run has a pending trial before completion credit merge");
            outsider.team.getData(FRSpecialDataTypes.RESEARCH_DATA).getData("iron").setFinished(true);outsider.service().initialize();
            service.merge(outsider.team);
            h.assertTrue(service.legacyCompleted(PROJECT) && PendingResearchProofs.get(world).at(h.absolutePos(second)).isEmpty()
                && !TechnologyAccessResolver.project(actor.team).recipe(PROTOTYPE).allowed(),"merged legacy completion credit immediately clears proof and temporary source without closing real history");
            h.succeed();
        }
    }
    @GameTest(batch="m4_access_groups",template="empty",timeoutTicks=60)
    public static void formalAllMetalFuelAlternativesAndSingleLegacyCompensation(GameTestHelper h) {
        try(var scope=new Scope()) {
            install();var actor=scope.actor(h);var idea=KnowledgeDefinitions.current().ideas().get(IDEA);
            h.assertTrue(idea.acquisition().required().isEmpty() && idea.acquisition().optional().size()==2,"formal idea acquisition keeps both observation groups optional");
            var bronze=ForgeRegistries.ITEMS.getValue(new ResourceLocation("frostedheart:bronze_ingot"));var coke=ForgeRegistries.ITEMS.getValue(new ResourceLocation("immersiveengineering:coal_coke"));
            h.assertTrue(bronze!=null && coke!=null,"actual registry contains group alternatives");
            for(var metal:List.of(Items.COPPER_INGOT,bronze)) for(var fuel:List.of(Items.COAL,Items.CHARCOAL,coke)) {
                var service=new KnowledgeService(UUID.randomUUID(),new KnowledgeState(),1,() -> {});
                service.learn(KnowledgeElement.idea(IDEA),SOURCE,KnowledgeService.Grant.NORMAL);
                service.learn(ItemRetrieval.observe(actor.player,new ItemStack(metal)),SOURCE,KnowledgeService.Grant.NORMAL);
                h.assertTrue(service.startResearch(PROJECT).isEmpty(),"metal alone still lacks fuel group");
                service.learn(ItemRetrieval.observe(actor.player,new ItemStack(fuel)),SOURCE,KnowledgeService.Grant.NORMAL);
                h.assertTrue(service.startResearch(PROJECT).isPresent(),"each formal metal/fuel combination starts");
            }
            actor.team.getData(FRSpecialDataTypes.RESEARCH_DATA).getData("iron").setFinished(true);actor.service().initialize();
            h.assertTrue(actor.service().isActive(KnowledgeKey.result(DESIGN)) && actor.service().legacyCompleted(PROJECT) && actor.service().state().research().isEmpty(),"old iron completion imports one dual-recipe Design without new run");
            h.assertTrue(TechnologyAccessResolver.project(actor.team).recipe(NORMAL).allowed() && TechnologyAccessResolver.project(actor.team).recipe(BLOOMERY).allowed(),"legacy compensation grants both actual recipe targets");
            h.succeed();
        }
    }
    @GameTest(batch="m4_mechanical_bellows",template="trial_kiln_empty",timeoutTicks=60)
    public static void mechanicalBellowsActualTickAndOrdinaryBloomerySupply(GameTestHelper h) {
        var scope=new Scope();guard(scope,() -> {
            install();var actor=scope.actor(h);var service=actor.service();var run=begin(actor);solve(desk(h,new BlockPos(1,1,1)),actor,run);service.advanceStage(run,"airflow");
            BlockPos bellows=new BlockPos(3,1,4),target=new BlockPos(4,1,4),redstone=new BlockPos(3,1,3);
            h.setBlock(bellows,charcoalPit.core.ModBlockRegistry.MechanicalBellows.defaultBlockState().setValue(charcoalPit.block.BlockBellows.FACING,Direction.EAST));
            placePrototype(h,actor,target);h.setBlock(redstone,Blocks.REDSTONE_BLOCK);
            h.runAfterDelay(13,() -> { try {
                h.assertTrue(h.getBlockState(target).is(charcoalPit.core.ModBlockRegistry.TuyereSandy),"real redstone-triggered mechanical bellows tick uses inherited blow hook");
                BlockPos ordinaryBellows=new BlockPos(1,1,6),normal=new BlockPos(2,1,6),bloomery=new BlockPos(2,1,7);
                h.setBlock(ordinaryBellows,charcoalPit.core.ModBlockRegistry.Bellows.defaultBlockState().setValue(charcoalPit.block.BlockBellows.FACING,Direction.EAST));
                h.setBlock(normal,charcoalPit.core.ModBlockRegistry.TuyereSandy);h.setBlock(bloomery,charcoalPit.core.ModBlockRegistry.Bloomery.defaultBlockState().setValue(charcoalPit.block.BlockBloomery.DUMMY,false));
                var tile=(charcoalPit.tile.TileBloomery)h.getBlockEntity(bloomery);tile.burnTime=100;
                int before=tile.airBuffer;charcoalPit.core.ModBlockRegistry.Bellows.blow(h.getLevel(),h.absolutePos(ordinaryBellows),h.getBlockState(ordinaryBellows));
                h.assertTrue(tile.airBuffer>before,"ordinary tuyere and Bloomery retain original supply logic");h.succeed();
            } finally { scope.close(); } });
        });
    }
    @GameTest(batch="m4_bound_item_observation",template="trial_kiln_empty",timeoutTicks=60)
    public static void existingSampleBindingSupportsFreshTeamObservationWithoutSchemaChanges(GameTestHelper h) {
        try(var scope=new Scope()) {
            install();var definitions=KnowledgeDefinitions.current();var project=definitions.projects().get(PROJECT);
            var observation=ProjectRequirement.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString("""
                {"id":"observe_item","type":"observation","observation":{"experiment":"test:sample","team_current_run":true,
                 "condition":{"kind":"observation","observation_type":"item","conditions":[{"field":"object","operator":"eq","value":{"state":"known","type":"string","text":"minecraft:stone"}}]}}}
                """)).getOrThrow(false,e -> {});
            var trial=new StageDefinition("trial","Trial","Observe","","",List.of(new StageDefinition.Task(observation,false,"")),List.of(),List.of());
            var modified=new ProjectDefinition(project.title(),project.body(),project.results(),List.of(),project.access(),project.legacyReplacement(),List.of(project.stages().get(0),trial));
            KnowledgeDefinitions.install(new KnowledgeDefinitions.Snapshot(0,definitions.ideas(),definitions.results(),definitions.links(),Map.of(),Map.of(PROJECT,modified),List.of()));
            var actor=scope.actor(h);var service=actor.service();var run=begin(actor);solve(desk(h,new BlockPos(1,1,1)),actor,run);service.advanceStage(run,"airflow");
            var stack=new ItemStack(Items.STONE);var record=ItemRetrieval.observe(actor.player,stack).observation().orElseThrow();
            service.observeResearchTarget(actor.player,record,null,stack);
            h.assertTrue(!service.requirementsMet(run),"ordinary item is not a trial sample");
            var binding=new ResearchSampleBinding(actor.team.getId(),run,"observe_item",new ResourceLocation("test:sample"));binding.write(stack);
            var wrongRun=new ItemStack(Items.STONE);new ResearchSampleBinding(actor.team.getId(),UUID.randomUUID(),"observe_item",binding.experiment()).write(wrongRun);
            service.observeResearchTarget(actor.player,record,null,wrongRun);h.assertTrue(!service.requirementsMet(run),"wrong run binding is rejected");
            int count=stack.getCount();service.observeResearchTarget(actor.player,record,null,stack);
            h.assertTrue(service.requirementsMet(run) && stack.getCount()==count,"existing M3 binding proves current team item without consumption or extra fields");
            h.succeed();
        }
    }
    @GameTest(batch="m4_real_chunk_unload",template="trial_kiln_empty",timeoutTicks=420)
    public static void realChunkUnloadRetainsPrototypeBindingAndPendingProof(GameTestHelper h) {
        var scope=new Scope();guard(scope,() -> {
            install();var actor=scope.actor(h);var service=actor.service();var run=begin(actor);
            solve(desk(h,new BlockPos(1,1,1)),actor,run);service.advanceStage(run,"airflow");
            var world=h.getLevel();var target=new BlockPos(500,80,500);var unblown=target.offset(1,0,1);var bellows=target.west();
            int cx=target.getX()>>4,cz=target.getZ()>>4;world.setChunkForced(cx,cz,true);world.getChunkAt(target);
            for(var pos:List.of(target,unblown)) {
                world.setBlockAndUpdate(pos,FRContents.Blocks.PROTOTYPE_TUYERE.get().defaultBlockState());
                world.getBlockState(pos).getBlock().setPlacedBy(world,pos,world.getBlockState(pos),actor.player,new ItemStack(FRContents.Blocks.PROTOTYPE_TUYERE.get()));
            }
            world.setBlockAndUpdate(bellows,charcoalPit.core.ModBlockRegistry.Bellows.defaultBlockState().setValue(charcoalPit.block.BlockBellows.FACING,Direction.EAST));
            charcoalPit.core.ModBlockRegistry.Bellows.blow(world,bellows,world.getBlockState(bellows));
            h.assertTrue(PendingResearchProofs.get(world).at(target).isPresent(),"trial proof exists before unload");world.setChunkForced(cx,cz,false);
            h.runAfterDelay(350,() -> { try {
                h.assertTrue(!world.getChunkSource().hasChunk(cx,cz),"far chunk actually left the ServerChunkCache");
                h.assertTrue(PendingResearchProofs.get(world).at(target).isPresent(),"chunk unload does not remove pending position proof");
                world.getChunkAt(target);
                h.assertTrue(world.getBlockState(target).is(charcoalPit.core.ModBlockRegistry.TuyereSandy) && world.getBlockEntity(target)==null,"normal tuyere restored as the real block without BE");
                var prototype=(PrototypeTuyereBlockEntity)world.getBlockEntity(unblown);
                h.assertTrue(prototype!=null && prototype.binding().filter(binding -> binding.team().equals(actor.team.getId()) && binding.run().equals(run)).isPresent(),"unblown prototype restores its actual BE binding after chunk reload");
                h.succeed();
            } finally { scope.close(); } });
        });
    }
}
