package com.teammoeg.frostedresearch.knowledge;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.*;
import com.teammoeg.chorda.dataholders.team.*;
import com.teammoeg.frostedresearch.*;
import com.teammoeg.frostedresearch.data.*;
import com.teammoeg.frostedresearch.knowledge.definition.*;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.state.*;
import com.teammoeg.frostedresearch.knowledge.event.KnowledgeOperationEvent;
import com.teammoeg.frostedresearch.research.*;
import com.teammoeg.frostedresearch.research.effects.*;
import com.teammoeg.frostedresearch.research.clues.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.gametest.*;
import net.minecraftforge.common.MinecraftForge;
import java.util.*;

@GameTestHolder("frostedresearch_m3b")
@PrefixGameTestTemplate(false)
public final class LegacyResearchBridgeGameTests {
    private static ResourceLocation id(String name) { return new ResourceLocation("bridge_test", name); }
    private static final ResourceLocation PROJECT=id("earthen_smelter"), IDEA=id("earthen_smelting"), DESIGN=id("design"), CONSTRUCTION=id("construction");
    private static final ResourceLocation RECIPE=new ResourceLocation("minecraft:crafting_table"), MULTIBLOCK=new ResourceLocation("immersiveengineering:multiblocks/alloy_smelter");
    private static final String OLD="alloy_kiln", DN="350c485a4b92425c", CN="1bb68ce233ef4f21";
    private static final AcquisitionSource SOURCE=new AcquisitionSource("fixture_external",Optional.empty(),42);
    private static KnowledgeDefinitions.Snapshot definitions(boolean complete, boolean mapped) {
        var results=new HashMap<ResourceLocation,ResultDefinition>();
        results.put(DESIGN,new ResultDefinition(new ResearchResult.Design(DESIGN,List.of(RECIPE)),"Design","",List.of(),true));
        if (complete) results.put(CONSTRUCTION,new ResultDefinition(new ResearchResult.Construction(CONSTRUCTION,List.of(MULTIBLOCK)),"Construction","",List.of(),true));
        var project=new ProjectDefinition("Earthen smelter","",List.of(DESIGN,CONSTRUCTION),List.of(ProjectRequirement.calculation("calculate","Calculate",1,1)),Optional.empty(),
            mapped ? Optional.of(new LegacyReplacement(OLD,Map.of(DN,DESIGN,CN,CONSTRUCTION),IDEA)) : Optional.empty());
        return new KnowledgeDefinitions.Snapshot(0,Map.of(IDEA,new IdeaDefinition("Idea","",List.of(),List.of(PROJECT),true)),results,Map.of(),Map.of(),Map.of(PROJECT,project),List.of());
    }
    private static void install(boolean complete, boolean mapped) { KnowledgeDefinitions.install(definitions(complete,mapped)); LegacyResearchBridge.refreshDefinitions(); }
    private static final class Actor implements AutoCloseable {
        final ServerPlayer player; final io.netty.channel.embedded.EmbeddedChannel channel;
        final TeamDataHolder team; final TeamResearchData legacy; final KnowledgeService service;
        Actor(GameTestHelper h) {
            var server=h.getLevel().getServer(); player=new ServerPlayer(server,h.getLevel(),new GameProfile(UUID.randomUUID(),"m3b-bridge"));
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            channel=new io.netty.channel.embedded.EmbeddedChannel(connection); connection.setProtocol(net.minecraft.network.ConnectionProtocol.PLAY);
            server.getPlayerList().placeNewPlayer(connection,player);
            team=CTeamDataManager.get(player); legacy=team.getData(FRSpecialDataTypes.RESEARCH_DATA); service=KnowledgeService.forPlayer(player);
        }
        @Override public void close() { player.server.getPlayerList().remove(player); channel.finishAndReleaseAll(); }
        ResearchData raw() { return legacy.getData(OLD); }
        void reconcile() { LegacyResearchBridge.reconcile(team,service); }
    }
    private static CompoundTag receipt(Actor a) { return a.legacy.getVariants().getCompound(LegacyResearchBridge.RECEIPTS).getCompound(PROJECT.toString()); }
    private static CompoundTag project(Actor a) { return a.service.snapshot().getList("projects",Tag.TAG_COMPOUND).stream().map(t -> (CompoundTag)t).filter(t -> t.getString("id").equals(PROJECT.toString())).findFirst().orElseThrow(); }
    public static final class CancelLearn {
        final UUID team; final ResourceLocation result;
        CancelLearn(UUID team, ResourceLocation result) { this.team=team;this.result=result; }
        @net.minecraftforge.eventbus.api.SubscribeEvent public void check(KnowledgeOperationEvent.Check event) {
            if(event.team().equals(team) && event.operation().equals("learn") && event.element().key().equals(KnowledgeKey.result(result))) event.setCanceled(true);
        }
    }
    @GameTest(batch="m3_bridge_import",template="empty",timeoutTicks=80)
    public static void legacyFactsRetryReceiptsAndNoFabricatedHistory(GameTestHelper h) {
        var previous=KnowledgeDefinitions.current(); install(true,true);
        try (var done=new Actor(h); var single=new Actor(h); var partial=new Actor(h); var fresh=new Actor(h)) {
            h.assertTrue(LegacyResearchBridge.isReplaced(OLD),"real recipe and IE multiblock make mapping active");
            done.raw().setFinished(true);
            var cancel=new CancelLearn(done.team.getId(),CONSTRUCTION); MinecraftForge.EVENT_BUS.register(cancel);
            try { done.reconcile(); } finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
            h.assertTrue(done.service.isActive(KnowledgeKey.result(DESIGN)) && !done.service.query(KnowledgeKey.result(CONSTRUCTION)).inArchive(),"cancel one result independently");
            h.assertTrue(receipt(done).getList("imported_results",Tag.TAG_STRING).size()==1,"receipt records only successful import");
            done.reconcile();
            h.assertTrue(done.service.isActive(KnowledgeKey.result(CONSTRUCTION)) && receipt(done).getList("imported_results",Tag.TAG_STRING).size()==2,"next lifecycle retries missing import");
            h.assertTrue(done.raw().getEffectData().isEmpty() && done.service.state().research().isEmpty(),"no claimed legacy rewards or fabricated new run");
            h.assertTrue(project(done).getString("completion_source").equals("legacy") && done.service.startResearch(PROJECT).isEmpty(),"legacy completion visible without discovery and cannot repeat");
            int acquisitions=done.service.state().acquisitions().size(); done.service.initialize(); done.service.initialize();
            h.assertTrue(done.service.state().acquisitions().size()==acquisitions,"repeated initialize imports once");
            done.service.forget(KnowledgeKey.result(DESIGN)); done.reconcile();
            h.assertTrue(!done.service.query(KnowledgeKey.result(DESIGN)).inArchive() && done.service.legacyCompleted(PROJECT),"intentional forgetting does not reimport or erase credit");
            single.raw().setEffectGranted(DN,true); single.reconcile();
            h.assertTrue(single.service.isActive(KnowledgeKey.result(DESIGN)) && !single.service.isActive(KnowledgeKey.result(CONSTRUCTION)) && !single.service.legacyCompleted(PROJECT),"one effect imports only matching result");
            partial.legacy.setData(OLD,new ResearchData(7,new boolean[]{true,false},0,Map.of("known",new ClueData(true)),new HashMap<>()));
            partial.reconcile();
            h.assertTrue(partial.service.isActive(KnowledgeKey.idea(IDEA)) && partial.raw().getCommitted()==7 && partial.raw().isActive() && partial.raw().isClueTriggered("known"),"partial ideas preserve points, clue and materials");
            partial.service.forget(KnowledgeKey.idea(IDEA)); partial.reconcile();
            h.assertTrue(!partial.service.query(KnowledgeKey.idea(IDEA)).inArchive() && receipt(partial).getBoolean("partial_idea_granted"),"partial grant exactly once");
            fresh.reconcile(); h.assertTrue(fresh.legacy.getExistingData(OLD).isEmpty() && fresh.service.snapshot().getList("projects",Tag.TAG_COMPOUND).isEmpty(),"new team neither gets legacy records nor hidden project requirements");
            var encoded=TeamResearchData.CODEC.encodeStart(NbtOps.INSTANCE,done.legacy).getOrThrow(false,s -> {});
            var restored=TeamResearchData.CODEC.parse(NbtOps.INSTANCE,encoded).getOrThrow(false,s -> {});
            h.assertTrue(restored.getVariants().equals(done.legacy.getVariants()),"receipts round trip through actual team codec");
            h.succeed();
        } finally { KnowledgeDefinitions.install(previous); LegacyResearchBridge.refreshDefinitions(); }
    }
    @GameTest(batch="m3_bridge_new",template="empty",timeoutTicks=80)
    public static void realRunInboxLearningForgetAndRawLegacySources(GameTestHelper h) {
        var previous=KnowledgeDefinitions.current(); install(true,true);
        Research saved=FHResearch.getResearch(OLD); Research legacy=legacyDefinition(); FHResearch.register(legacy);
        try (var actor=new Actor(h)) {
            Research child=new Research(); child.setParents(OLD);
            try(var external=new Actor(h)) {
                external.service.learn(KnowledgeElement.result(DESIGN),SOURCE,KnowledgeService.Grant.NORMAL);
                external.service.learn(KnowledgeElement.result(CONSTRUCTION),SOURCE,KnowledgeService.Grant.NORMAL);
                h.assertTrue(!child.isUnlocked(external.team) && TechnologyAccessResolver.project(external.team).recipe(RECIPE).allowed(),"external results grant access without inventing a completed project");
            }
            actor.service.learn(KnowledgeElement.idea(IDEA),SOURCE,KnowledgeService.Grant.NORMAL);
            UUID run=actor.service.startResearch(PROJECT).orElseThrow();
            actor.service.recordCalculation(run,"calculate",1);
            var cancel=new CancelLearn(actor.team.getId(),CONSTRUCTION); MinecraftForge.EVENT_BUS.register(cancel);
            try { actor.service.finishResearch(run); } finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
            h.assertTrue(actor.service.state().research().get(run).completed() && actor.service.query(KnowledgeKey.result(CONSTRUCTION)).inInbox(),"actual normal finish keeps cancelled result in inbox");
            h.assertTrue(!child.isUnlocked(actor.team),"pending result does not open old child");
            actor.service.learnInbox(KnowledgeKey.result(CONSTRUCTION));
            h.assertTrue(child.isUnlocked(actor.team) && LegacyResearchBridge.projection(actor.team).getCompound(0).getBoolean("effective_completed"),"learning last result opens child and synchronized projection");
            var projection=TechnologyAccessResolver.project(actor.team);
            h.assertTrue(projection.recipe(RECIPE).allowed() && projection.multiblock(MULTIBLOCK).allowed(),"new route grants real recipe and forming permission");
            h.assertTrue(!actor.raw().isCompleted() && actor.raw().getEffectData().isEmpty(),"new completion never rewrites raw legacy facts");
            actor.service.forget(KnowledgeKey.result(DESIGN));
            h.assertTrue(!child.isUnlocked(actor.team) && !TechnologyAccessResolver.project(actor.team).recipe(RECIPE).allowed(),"forget removes new prerequisite and new permission source");
            actor.raw().setFinished(true); actor.raw().setEffectGranted(DN,true);
            com.teammoeg.frostedresearch.api.TeamResearchService.sync(actor.team);
            h.assertTrue(child.isUnlocked(actor.team) && TechnologyAccessResolver.project(actor.team).recipe(RECIPE).allowed(),"real old finished plus claimed effect remains independent source");
            actor.service.learn(KnowledgeElement.result(DESIGN),SOURCE,KnowledgeService.Grant.NORMAL);
            h.assertTrue(TechnologyAccessResolver.project(actor.team).recipe(RECIPE).sources().size()==2,"old and new access sources merge");
            actor.service.forget(KnowledgeKey.result(DESIGN)); h.assertTrue(TechnologyAccessResolver.project(actor.team).recipe(RECIPE).allowed(),"forget preserves raw legacy source");
            h.succeed();
        } finally { FHResearch.researches.remove(legacy); if(saved!=null) FHResearch.register(saved); FHResearch.clearCache(); KnowledgeDefinitions.install(previous); LegacyResearchBridge.refreshDefinitions(); }
    }
    private static <T> T formal(String path, Codec<T> codec) {
        try(var reader=new java.io.InputStreamReader(Objects.requireNonNull(LegacyResearchBridgeGameTests.class.getResourceAsStream("/m3b-formal-fixtures/"+path+".json")),java.nio.charset.StandardCharsets.UTF_8)) {
            return codec.parse(JsonOps.INSTANCE,com.google.gson.JsonParser.parseReader(reader)).getOrThrow(false,message -> {});
        } catch(java.io.IOException e) { throw new IllegalStateException(e); }
    }
    @GameTest(batch="m3_bridge_formal",template="empty",timeoutTicks=80)
    public static void formalCompanionContentCodecsAndReferences(GameTestHelper h) {
        var ideaId=new ResourceLocation("the_winter_rescue:earthen_smelting");
        var projectId=new ResourceLocation("the_winter_rescue:earthen_smelter");
        var designId=new ResourceLocation("the_winter_rescue:earthen_smelter_design");
        var constructionId=new ResourceLocation("the_winter_rescue:earthen_smelter_construction");
        var project=formal("projects/earthen_smelter",ProjectDefinition.CODEC);
        var design=formal("results/earthen_smelter_design",ResultDefinition.CODEC);
        var construction=formal("results/earthen_smelter_construction",ResultDefinition.CODEC);
        var snapshot=new KnowledgeDefinitions.Snapshot(0,Map.of(ideaId,formal("ideas/earthen_smelting",IdeaDefinition.CODEC)),
            Map.of(designId,design,constructionId,construction),Map.of(ideaId,formal("links/earthen_smelting",com.teammoeg.frostedresearch.knowledge.link.LinkRule.CODEC)),
            Map.of(),Map.of(projectId,project),List.of());
        h.assertTrue(KnowledgeDefinitions.validate(snapshot).isEmpty(),"formal project/idea/link/results parse and references validate");
        h.assertTrue(project.legacyReplacement().orElseThrow().effects().equals(Map.of(DN,designId,CN,constructionId)),"formal legacy schema and effect nonces match bridge");
        h.assertTrue(((ResearchResult.Design)design.result()).recipes().equals(List.of(new ResourceLocation("the_winter_rescue:research/alloybrick"))),"formal stable recipe target is exact; actual pack registration is separate");
        h.assertTrue(((ResearchResult.Construction)construction.result()).multiblocks().equals(List.of(MULTIBLOCK)) && blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.getByUniqueName(MULTIBLOCK)!=null,"formal forming target matches real IE registration");
        h.assertTrue(project.requirements().size()==1 && project.requirements().get(0).sample().orElseThrow().equals(new ProjectRequirement.Sample(new ResourceLocation("frostedresearch:trial_kiln"),new ResourceLocation("frostedresearch:fired_trial_kiln"))),"formal task is M3-A physical successful sample");
        h.succeed();
    }
    @GameTest(batch="m3_bridge_lifecycle",template="empty",timeoutTicks=80)
    public static void finalRecipesRefreshWithNoOnlinePlayersAndSameRevision(GameTestHelper h) {
        var previous=KnowledgeDefinitions.current();
        var server=h.getLevel().getServer(); var manager=server.getRecipeManager();
        var recipes=List.copyOf(manager.getRecipes());
        try {
            h.assertTrue(server.getPlayerList().getPlayers().isEmpty(),"global refresh fixture has zero online players");
            install(true,true); long revision=KnowledgeDefinitions.current().revision();
            manager.replaceRecipes(recipes.stream().filter(recipe -> !recipe.getId().equals(RECIPE)).toList());
            LegacyResearchBridge.refreshDefinitions();
            h.assertTrue(!LegacyResearchBridge.isReplaced(OLD),"early team load caches absent target");
            manager.replaceRecipes(recipes);
            h.assertTrue(!LegacyResearchBridge.isReplaced(OLD),"target restored with same definition revision needs final lifecycle refresh");
            KnowledgeRuntime.datapackSync(new net.minecraftforge.event.OnDatapackSyncEvent(server.getPlayerList(),null));
            h.assertTrue(LegacyResearchBridge.isReplaced(OLD) && KnowledgeDefinitions.current().revision()==revision,"zero-online final reload refreshes target availability without revision change");
            manager.replaceRecipes(recipes.stream().filter(recipe -> !recipe.getId().equals(RECIPE)).toList());
            LegacyResearchBridge.refreshDefinitions(); manager.replaceRecipes(recipes);
            KnowledgeRuntime.started(new net.minecraftforge.event.server.ServerStartedEvent(server));
            h.assertTrue(LegacyResearchBridge.isReplaced(OLD),"initial server start refreshes late recipes as well");
            LegacyResearchBridge.clear();
            h.assertTrue(LegacyResearchBridge.isReplaced(OLD),"cleared static cache rebuilds from current definitions");
            h.succeed();
        } finally { manager.replaceRecipes(recipes); KnowledgeDefinitions.install(previous); LegacyResearchBridge.refreshDefinitions(); }
    }
    private static Research legacyDefinition() {
        var crafting=new EffectCrafting(com.mojang.datafixers.util.Either.right(List.of(RECIPE)),new Effect.BaseData("",List.of(),Optional.empty(),DN,false,List.of()));
        var building=new EffectBuilding(new Effect.BaseData("",List.of(),Optional.empty(),CN,false,List.of()),blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.getByUniqueName(MULTIBLOCK));
        return new Research(OLD,"Old kiln",0,100,com.teammoeg.chorda.client.icon.CIcons.nop(),ResearchCategory.RESCUE,List.of(),List.of(),List.of(),List.of(),List.of(),List.of(crafting,building),List.of(new ItemClue("old_clue","Inspect stone","","",0.5f,false,com.mojang.datafixers.util.Pair.of(net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.STONE),1),false)),true,false,false,false,false,false);
    }
    @GameTest(batch="m3_bridge_guards",template="empty",timeoutTicks=80)
    public static void selectedOldCannotAdvanceAndMappingRemovalRestoresRoute(GameTestHelper h) {
        var previous=KnowledgeDefinitions.current(); install(true,false);
        Research saved=FHResearch.getResearch(OLD); Research legacy=legacyDefinition(); FHResearch.register(legacy);
        try (var a=new Actor(h)) {
            a.raw().setActive(); a.legacy.setCurrentResearch(a.team,legacy);
            h.assertTrue(a.legacy.getCurrentResearchValue()==legacy,"fixture selects old research while mapping absent");
            a.legacy.doResearch(a.team,legacy,7); install(true,true); a.reconcile();
            h.assertTrue(a.legacy.getCurrentResearchValue()==null && a.raw().isActive() && a.raw().getCommitted()==7,"reload clears current selection preserving original investment");
            a.legacy.setCurrentResearch(a.team,legacy);
            h.assertTrue(a.legacy.getCurrentResearchValue()==null && !a.legacy.commitItem(a.player,a.team,legacy),"selection and activation cannot bypass replacement");
            h.assertTrue(a.legacy.doResearch(a.team,legacy,100)==100 && !a.legacy.checkResearchComplete(a.team,legacy) && a.raw().getCommitted()==7,"direct points and completion cannot bypass");
            a.legacy.setClueCompleted(a.team,legacy,0,true);
            a.legacy.setClueCompleted(a.team,legacy,legacy.getClues().get(0),true);
            h.assertTrue(a.raw().getClueData().isEmpty(),"both clue routes reject before touching state");
            h.assertTrue(KnowledgeDefinitions.validate(definitions(false,true)).isEmpty(),"missing mapped result is a local bridge fallback, not a rejected stale snapshot");
            install(false,true); a.reconcile();
            h.assertTrue(!LegacyResearchBridge.isReplaced(OLD),"one missing result restores legacy entry");
            install(true,false); a.legacy.setCurrentResearch(a.team,legacy); a.legacy.doResearch(a.team,legacy,5);
            h.assertTrue(a.raw().getCommitted()==12 && a.legacy.getCurrentResearchValue()==legacy,"mapping removal resumes original route");
            KnowledgeDefinitions.install(KnowledgeDefinitions.Snapshot.empty()); a.reconcile();
            a.legacy.doResearch(a.team,legacy,1);
            h.assertTrue(a.raw().getCommitted()==13,"entire new package absent preserves and resumes legacy progress");
            install(true,true); a.reconcile(); h.assertTrue(a.raw().getCommitted()==13 && a.service.state().research().isEmpty(),"reinstall is idempotent and never manufactures experiments");
            h.succeed();
        } finally { FHResearch.researches.remove(legacy); if(saved!=null) FHResearch.register(saved); FHResearch.clearCache(); KnowledgeDefinitions.install(previous); LegacyResearchBridge.refreshDefinitions(); }
    }
    @GameTest(batch="m3_bridge_merge",template="empty",timeoutTicks=80)
    public static void receiptUnionAndExistingRunSurviveMerge(GameTestHelper h) {
        var previous=KnowledgeDefinitions.current(); install(true,true);
        try(var source=new Actor(h);var target=new Actor(h)) {
            source.raw().setFinished(true); source.reconcile();
            target.raw().setActive(); target.reconcile();
            UUID run=target.service.startResearch(PROJECT).orElseThrow();
            target.legacy.getVariants().putString("unrelated","keep");
            target.service.merge(source.team); target.service.merge(source.team);
            h.assertTrue(receipt(target).getBoolean("completed_credit") && receipt(target).getBoolean("partial_idea_granted") && receipt(target).getList("imported_results",Tag.TAG_STRING).size()==2,"receipt flags and imported IDs union without duplicates");
            h.assertTrue(target.service.state().research().get(run).open() && target.legacy.getVariants().getString("unrelated").equals("keep"),"existing run and unrelated variants preserved");
            h.assertTrue(target.service.recordCalculation(run,"calculate",1).status()==KnowledgeService.Status.NOT_FOUND && target.service.startResearch(PROJECT).isEmpty(),"credited project neither progresses nor repeats old open run");
            h.assertTrue(target.service.snapshot().getList("research",Tag.TAG_COMPOUND).getCompound(0).getString("status").equals("legacy_superseded"),"retained open run explicitly no longer needed");
            target.service.forget(KnowledgeKey.result(DESIGN)); target.reconcile();
            h.assertTrue(!target.service.query(KnowledgeKey.result(DESIGN)).inArchive(),"merged receipt prevents reimport from raw legacy facts");
            h.succeed();
        } finally { KnowledgeDefinitions.install(previous); LegacyResearchBridge.refreshDefinitions(); }
    }
}
