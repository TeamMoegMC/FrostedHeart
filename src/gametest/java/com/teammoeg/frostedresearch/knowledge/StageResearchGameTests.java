package com.teammoeg.frostedresearch.knowledge;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.teammoeg.frostedresearch.knowledge.definition.*;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.state.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.*;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("frostedresearch_m4")
@PrefixGameTestTemplate(false)
public final class StageResearchGameTests {
    static ResourceLocation id(String path) { return new ResourceLocation("stage_test",path); }
    static final AcquisitionSource SOURCE=new AcquisitionSource("test",Optional.empty(),1);
    static ProjectDefinition definition() {
        return ProjectDefinition.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString("""
        {"title":"Stages","results":["stage_test:result"],"stages":[
          {"id":"a","conclusion":"a done","tasks":[{"requirement":{"id":"a_win","type":"calculation","level":1}},
            {"optional":true,"requirement":{"id":"optional","type":"calculation","level":1}}],"completion_recipes":["minecraft:crafting_table"]},
          {"id":"b","conclusion":"b done","tasks":[{"requirement":{"id":"b_win","type":"calculation","level":1}}],
           "variants":[{"id":"reference","when":{"knowledge_all":[{"kind":"idea","definition":"stage_test:reference"}],"tasks_all":["optional"]},"omit_tasks":["b_win"],"method":"shortcut"},{"id":"base"}]},
          {"id":"c","tasks":[{"requirement":{"id":"c_win","type":"calculation","level":1}}],
           "variants":[{"id":"reference","when":{"knowledge_all":[{"kind":"idea","definition":"stage_test:reference"}]},"omit_tasks":["c_win"]},{"id":"base"}]}
        ]}
        """)).getOrThrow(false,s -> {});
    }
    static void install(ProjectDefinition project) {
        var snapshot=new KnowledgeDefinitions.Snapshot(0,Map.of(id("idea"),new IdeaDefinition("Idea","",List.of(),List.of(id("project")),true),
            id("reference"),new IdeaDefinition("Reference","",List.of(),List.of(),true)),Map.of(id("result"),new ResultDefinition(new ResearchResult.Finding(id("result"),List.of()),"Result","",List.of(),true)),
            Map.of(),Map.of(),Map.of(id("project"),project),List.of());
        if(!KnowledgeDefinitions.validate(snapshot).isEmpty()) throw new IllegalStateException(KnowledgeDefinitions.validate(snapshot).toString());
        KnowledgeDefinitions.install(snapshot);
    }
    static KnowledgeService service() {
        var s=new KnowledgeService(UUID.randomUUID(),new KnowledgeState(),1,() -> {});
        s.learn(KnowledgeElement.idea(id("idea")),SOURCE,KnowledgeService.Grant.NORMAL);return s;
    }
    @GameTest(batch="m4_stages",template="empty",timeoutTicks=60)
    public static void stageChoicesFreezeOnlyOnEntryAndAdvanceIsExplicit(GameTestHelper h) {
        var previous=KnowledgeDefinitions.current();try {
            install(definition());var s=service();var run=s.startResearch(id("project")).orElseThrow();
            h.assertTrue(s.recordCalculation(run,"b_win",1).status()==KnowledgeService.Status.NOT_FOUND,"future task cannot be submitted");
            s.recordCalculation(run,"a_win",1);
            var a=s.state().research().get(run);
            h.assertTrue(a.stageState().current().equals("a") && a.stageState().visits().get("a").ready() && !s.requirementsMet(run),"ready keeps current stage, optional not required, cannot finish early");
            h.assertTrue(a.stageState().visits().get("a").temporaryRecipes().contains(new ResourceLocation("minecraft:crafting_table")),"grant recorded immediately on required task completion");
            s.learn(KnowledgeElement.idea(id("reference")),SOURCE,KnowledgeService.Grant.NORMAL);
            s.recordCalculation(run,"optional",1);s.advanceStage(run,"a");
            var b=s.state().research().get(run);
            h.assertTrue(b.stageState().visits().get("b").variant().equals("reference") && s.requirements(b).isEmpty(),"ready-time optional work and knowledge choose an omitted-task variant on entry");
            h.assertTrue(s.advanceStage(run,"a").status()==KnowledgeService.Status.REQUIREMENTS_UNMET,"duplicate advance cannot jump twice");
            s.forget(KnowledgeKey.idea(id("reference")));
            h.assertTrue(s.state().research().get(run).stageState().visits().get("b").ready(),"reference forgetting does not undo ready fact");
            s.advanceStage(run,"b");s.learn(KnowledgeElement.idea(id("reference")),SOURCE,KnowledgeService.Grant.NORMAL);
            h.assertTrue(s.state().research().get(run).stageState().visits().get("c").variant().equals("base"),"knowledge after entry does not reselect current path");
            h.assertTrue(s.recordCalculation(run,"optional",1).status()==KnowledgeService.Status.NOT_FOUND,"previous optional task is closed");
            s.recordCalculation(run,"c_win",1);h.assertTrue(s.finishResearch(run).get(0).succeeded(),"last stage closes once by normal finish");
            h.assertTrue(s.finishResearch(run).get(0).status()==KnowledgeService.Status.NOT_FOUND,"repeat finish cannot duplicate output");h.succeed();
        } finally { KnowledgeDefinitions.install(previous); }
    }
    @GameTest(batch="m4_stage_restore",template="empty",timeoutTicks=60)
    public static void schemaFourAndMissingDefinitionsPreserveSavedChoices(GameTestHelper h) {
        var previous=KnowledgeDefinitions.current();try {
            var flat=new ProjectDefinition("Flat","",List.of(id("result")),List.of(ProjectRequirement.calculation("old","Old",1,1)));
            install(flat);var s=service();var run=s.startResearch(id("project")).orElseThrow();s.recordCalculation(run,"old",1);
            var tag=s.state().write();tag.putInt("schema_version",4);((CompoundTag)tag.getList("research",Tag.TAG_COMPOUND).get(0)).remove("stage_state");
            KnowledgeDefinitions.install(KnowledgeDefinitions.Snapshot.empty());var restored=KnowledgeState.read(tag);
            h.assertTrue(restored.research().get(run).calculations().get("old").equals(List.of(1)),"schema4 old progress survives while definitions absent");
            install(flat);var resumed=new KnowledgeService(UUID.randomUUID(),restored,1,() -> {});resumed.initialize();
            h.assertTrue(resumed.requirementsMet(run) && resumed.finishResearch(run).get(0).succeeded(),"restored flat research still needs no extra stage click");
            var second=KnowledgeState.read(restored.write());h.assertTrue(second.research().get(run).equals(restored.research().get(run)),"schema5 reads twice without changing stage history");
            install(definition());var staged=service();var current=staged.startResearch(id("project")).orElseThrow();staged.recordCalculation(current,"a_win",1);staged.advanceStage(current,"a");
            var saved=staged.state().research().get(current);KnowledgeDefinitions.install(KnowledgeDefinitions.Snapshot.empty());
            h.assertTrue(staged.recordCalculation(current,"b_win",1).status()==KnowledgeService.Status.NOT_FOUND && staged.state().research().get(current).equals(saved),"missing content does not erase or skip chosen stage");
            install(definition());h.assertTrue(staged.recordCalculation(current,"b_win",1).succeeded(),"restoring definitions continues same choice");h.succeed();
        } finally { KnowledgeDefinitions.install(previous); }
    }
    @GameTest(batch="m4_temporary_access",template="empty",timeoutTicks=60)
    public static void temporaryRecipeSourcesUnionAndWithdrawIndependently(GameTestHelper h) {
        var previous=KnowledgeDefinitions.current();try {
            install(definition());var defs=KnowledgeDefinitions.current();
            var projects=new HashMap<>(defs.projects());projects.put(id("other"),definition());
            var results=new HashMap<>(defs.results());results.put(id("design"),new ResultDefinition(new ResearchResult.Design(id("design"),List.of(new ResourceLocation("minecraft:crafting_table"))),"Design","",List.of(),true));
            KnowledgeDefinitions.install(new KnowledgeDefinitions.Snapshot(0,defs.ideas(),results,Map.of(),Map.of(),projects,List.of()));
            var knowledge=new com.teammoeg.frostedresearch.data.TeamKnowledgeData();var legacy=new com.teammoeg.frostedresearch.data.TeamResearchData();
            var service=new KnowledgeService(UUID.randomUUID(),knowledge,1,() -> {});
            var recipe=new ResourceLocation("minecraft:crafting_table");
            h.assertTrue(!TechnologyAccessResolver.project(knowledge,legacy).recipe(recipe).allowed(),"temporary targets managed before any run");
            service.learn(KnowledgeElement.idea(id("idea")),SOURCE,KnowledgeService.Grant.NORMAL);
            var run=service.startResearch(id("project")).orElseThrow();service.recordCalculation(run,"a_win",1);
            var saved=knowledge.research().get(run);var other=UUID.randomUUID();
            knowledge.recordResearch(new ResearchHistory(other,id("other"),saved.origins(),1,false,List.of()).withStages(saved.stageState()));
            h.assertTrue(TechnologyAccessResolver.project(knowledge,legacy).recipe(recipe).sources().size()==2,"two open runs independently allow same recipe");
            service.abandonResearch(run);
            h.assertTrue(TechnologyAccessResolver.project(knowledge,legacy).recipe(recipe).sources().size()==1,"ending one run keeps the other grant");
            service.learn(KnowledgeElement.result(id("design")),SOURCE,KnowledgeService.Grant.NORMAL);
            service.completeResearch(other);
            var access=TechnologyAccessResolver.project(knowledge,legacy).recipe(recipe);
            h.assertTrue(access.allowed() && access.sources().size()==1 && access.sources().get(0) instanceof AccessSource.ResultSource,"permanent result survives all temporary grants ending");
            var serialized=AccessSource.CODEC.encodeStart(NbtOps.INSTANCE,new AccessSource.TemporaryResearchSource(id("project"),run,"a")).getOrThrow(false,e -> {});
            h.assertTrue(AccessSource.CODEC.parse(NbtOps.INSTANCE,serialized).result().isPresent(),"temporary provenance crosses snapshot codec");h.succeed();
        } finally { KnowledgeDefinitions.install(previous); }
    }
    @GameTest(batch="m4_flat_to_stages",template="empty",timeoutTicks=60)
    public static void migratedFutureFlatProgressIsRetainedWithoutCreditingLaterStages(GameTestHelper h) {
        var previous=KnowledgeDefinitions.current();try {
            install(definition());var service=service();var run=UUID.randomUUID();
            var old=new ResearchHistory(run,id("project"),List.of(KnowledgeKey.idea(id("idea"))),1,false,List.of())
                .withCalculation("a_win",1).withCalculation("b_win",1);
            service.state().recordResearch(old);service.initialize();var prepared=service.state().research().get(run);
            h.assertTrue(prepared.stageState().legacyProgress().calculations().get("b_win").equals(List.of(1)) && !prepared.calculations().containsKey("b_win"),"nonfirst flat progress is retained separately from active tasks");
            h.assertTrue(!service.snapshot().toString().contains("b_win"),"future task identity is not disclosed by stage snapshot");
            service.advanceStage(run,"a");
            h.assertTrue(!service.state().research().get(run).stageState().visits().get("b").ready(),"old future task progress cannot ready the newly entered second stage");
            service.recordCalculation(run,"b_win",1);h.assertTrue(service.state().research().get(run).stageState().visits().get("b").ready(),"new current task work is credited normally");
            var restored=KnowledgeState.read(service.state().write());
            h.assertTrue(restored.research().get(run).stageState().legacyProgress().equals(prepared.stageState().legacyProgress()),"archived legacy values survive codec round trip");h.succeed();
        } finally { KnowledgeDefinitions.install(previous); }
    }
}
