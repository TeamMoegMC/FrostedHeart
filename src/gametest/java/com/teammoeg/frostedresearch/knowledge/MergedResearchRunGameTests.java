package com.teammoeg.frostedresearch.knowledge;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.teammoeg.frostedresearch.data.*;
import com.teammoeg.frostedresearch.knowledge.client.KnowledgeJournalData;
import com.teammoeg.frostedresearch.knowledge.definition.*;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.state.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("frostedresearch_m6")
@PrefixGameTestTemplate(false)
public final class MergedResearchRunGameTests {
    public static final ResourceLocation PROJECT=id("study"),OTHER=id("other"),RESULT=id("result"),IDEA=id("idea");
    public static final ResourceLocation TEMP=new ResourceLocation("minecraft:crafting_table");
    public static final UUID EARLY=UUID.fromString("ffffffff-0000-0000-0000-000000000001"),LATE=UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final AcquisitionSource SOURCE=new AcquisitionSource("test_external",Optional.empty(),1);
    public static ResourceLocation id(String s){return new ResourceLocation("m6_test",s);}
    public static ProjectDefinition project(){
        return ProjectDefinition.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString("""
        {"title":"Merged study","results":["m6_test:result"],"stages":[
          {"id":"early","title":"Prepare","completion_recipes":["minecraft:crafting_table"],"tasks":[
            {"requirement":{"id":"first","type":"calculation","level":1}},
            {"optional":true,"requirement":{"id":"early_experiment","title":"Early experiment","type":"experiment","duration":200,"materials":[{"item":"minecraft:clay_ball","count":1}]}},
            {"optional":true,"requirement":{"id":"early_sample","title":"Early sample","type":"sample","sample":{"experiment":"frostedresearch:trial_kiln","item":"frostedresearch:fired_trial_kiln"}}},
            {"optional":true,"requirement":{"id":"early_brick","title":"Early brick","type":"observation","observation":{"experiment":"frostedresearch:refractory_trial","team_current_run":true,"condition":{"kind":"observation","observation_type":"item"}}}},
            {"optional":true,"requirement":{"id":"early_tuyere","title":"Early tuyere","type":"observation","observation":{"experiment":"frostedresearch:tuyere_trial","team_current_run":true,"condition":{"kind":"observation","observation_type":"block"}}}}
          ]},
          {"id":"late","title":"Fire","tasks":[
            {"requirement":{"id":"finish","type":"calculation","level":1}},
            {"optional":true,"requirement":{"id":"late_experiment","title":"Late experiment","type":"experiment","duration":200,"materials":[{"item":"minecraft:clay_ball","count":1}]}},
            {"optional":true,"requirement":{"id":"late_sample","title":"Late sample","type":"sample","sample":{"experiment":"frostedresearch:trial_kiln","item":"frostedresearch:fired_trial_kiln"}}},
            {"optional":true,"requirement":{"id":"late_brick","title":"Late brick","type":"observation","observation":{"experiment":"frostedresearch:refractory_trial","team_current_run":true,"condition":{"kind":"observation","observation_type":"item"}}}},
            {"optional":true,"requirement":{"id":"late_tuyere","title":"Late tuyere","type":"observation","observation":{"experiment":"frostedresearch:tuyere_trial","team_current_run":true,"condition":{"kind":"observation","observation_type":"block"}}}}
          ]}
        ]}
        """)).getOrThrow(false,e->{});
    }
    public static void install(){
        var p=project();var other=new ProjectDefinition(p.title(),p.body(),List.of(id("other_result")),p.requirements(),p.access(),p.legacyReplacement(),p.stages());
        var defs=new KnowledgeDefinitions.Snapshot(0,
            Map.of(IDEA,new IdeaDefinition("Idea","",List.of(),List.of(PROJECT,OTHER),true)),
            Map.of(RESULT,new ResultDefinition(new ResearchResult.Design(RESULT,List.of(new ResourceLocation("minecraft:oak_planks"))),"Result","",List.of(),true),
                id("other_result"),new ResultDefinition(new ResearchResult.Finding(id("other_result"),List.of()),"Other","",List.of(),true)),
            Map.of(),Map.of(),Map.of(PROJECT,p,OTHER,other),List.of());
        if(!KnowledgeDefinitions.validate(defs).isEmpty())throw new IllegalStateException(KnowledgeDefinitions.validate(defs).toString());
        KnowledgeDefinitions.install(defs);
    }
    public static ResearchHistory record(UUID id,long time,boolean late){
        var s=new KnowledgeService(UUID.randomUUID(),new KnowledgeState(),time,()->{});
        var run=new ResearchHistory(id,PROJECT,List.of(KnowledgeKey.idea(IDEA)),time,false,List.of());
        s.state().recordResearch(ResearchStages.update(s,run));
        if(late){s.recordCalculation(id,"first",1);s.advanceStage(id,"early");}
        return s.state().research().get(id);
    }
    /** Real merge keeps destination insertion LATE then incoming EARLY, opposing the required time order. */
    public static void mergePair(KnowledgeService s){
        s.learn(KnowledgeElement.idea(IDEA),SOURCE,KnowledgeService.Grant.NORMAL);
        s.state().recordResearch(record(LATE,20,true));var incoming=new KnowledgeState();incoming.recordResearch(record(EARLY,10,false));s.merge(incoming);
    }
    static boolean completedView(KnowledgeService s){
        return KnowledgeView.create(s).getList("projects",Tag.TAG_COMPOUND).stream().map(t -> (CompoundTag)t)
            .filter(t -> t.getString("id").equals(PROJECT.toString())).findFirst().orElseThrow().getBoolean("completed");
    }
    static KnowledgeService service(){return new KnowledgeService(UUID.randomUUID(),new TeamKnowledgeData(),30,()->{});}
    @GameTest(batch="m6_order",template="empty",timeoutTicks=80)
    public static void mergeOrdersSnapshotAndClientSelectionByTimeThenUuid(GameTestHelper h){
        var previous=KnowledgeDefinitions.current();try{
            install();var s=service();mergePair(s);
            h.assertTrue(new ArrayList<>(s.state().research().keySet()).equals(List.of(LATE,EARLY)),"merge really retains opposing insertion order");
            h.assertTrue(s.openRun(PROJECT).orElseThrow().id().equals(EARLY),"time order overrides UUID and insertion order");
            var snapshot=KnowledgeView.create(s);var data=new KnowledgeJournalData(snapshot);var list=snapshot.getList("research",Tag.TAG_COMPOUND);
            h.assertTrue(list.getCompound(0).getString("id").equals(EARLY.toString()) && data.openRun(PROJECT.toString()).id().equals(EARLY.toString()),"snapshot and client default agree");
            var selected=data.openRun(PROJECT.toString(),LATE.toString());h.assertTrue(selected.id().equals(LATE.toString()) && selected.stage().id().equals("late"),"client explicitly continues second retained stage");
            h.assertTrue(data.openRun(PROJECT.toString(),UUID.randomUUID().toString()).id().equals(EARLY.toString()),"missing selection falls back deterministically");
            var a=s.sampleTargets(new ResourceLocation("frostedresearch:trial_kiln"));var b=s.observationTargets(new ResourceLocation("frostedresearch:refractory_trial"));
            h.assertTrue(a.stream().map(v->v.run()).toList().equals(List.of(EARLY,LATE)) && b.stream().map(v->v.run()).toList().equals(List.of(EARLY,LATE)),"M3/M5 target order agrees with journal");
            h.assertTrue(data.openRuns(PROJECT.toString()).get(0).number()==1 && data.openRuns(PROJECT.toString()).get(1).number()==2,"shared derived record ordinals");
            h.assertTrue(!s.targetLabel(b.get(1)).getString().contains(LATE.toString()),"target labels do not expose UUID");
            var restored=KnowledgeState.read(s.state().write());var round=new KnowledgeService(s.teamId(),restored,30,()->{});round.initialize();
            h.assertTrue(ResearchRuns.sorted(restored).equals(ResearchRuns.sorted(s.state())) && round.openRun(PROJECT).orElseThrow().id().equals(EARLY),"schema5 roundtrip retains both stages and order");
            var tied=new KnowledgeState();tied.recordResearch(record(EARLY,10,false));tied.recordResearch(record(LATE,10,false));
            h.assertTrue(ResearchRuns.sorted(tied).get(0).id().equals(LATE),"UUID consistently breaks equal creation times");h.succeed();
        }finally{KnowledgeDefinitions.install(previous);}
    }
    @GameTest(batch="m6_standby",template="empty",timeoutTicks=80)
    public static void realCompletionPausesBackupTasksAndTemporarySourcesThenForgettingResumes(GameTestHelper h){
        var previous=KnowledgeDefinitions.current();try{
            install();var s=service();mergePair(s);s.recordCalculation(EARLY,"first",1);var backup=s.state().research().get(EARLY);
            var knowledge=(TeamKnowledgeData)s.state();var legacy=new TeamResearchData();
            h.assertTrue(TechnologyAccessResolver.project(knowledge,legacy).recipe(TEMP).sources().size()==2,"both entered early visits grant independent temporary sources");
            s.recordCalculation(LATE,"finish",1);s.finishResearch(LATE);
            h.assertTrue(s.researchStartStatus(PROJECT)==KnowledgeService.Status.ALREADY_OWNED && s.openRun(PROJECT).isEmpty() && completedView(s),"true completion has priority over retained open history");
            h.assertTrue(s.standby(EARLY) && s.state().research().get(EARLY).equals(backup),"backup stored progress remains untouched");
            h.assertTrue(s.advanceStage(EARLY,"early").status()==KnowledgeService.Status.NOT_FOUND && !s.recordCalculation(EARLY,"first",1).succeeded()
                && !s.recordObservationTask(EARLY,"early_brick").succeeded() && s.sampleTargets(new ResourceLocation("frostedresearch:trial_kiln")).isEmpty(),"all backup task entrances pause");
            h.assertTrue(!TechnologyAccessResolver.project(knowledge,legacy).recipe(TEMP).allowed(),"backup temporary grants pause without deleting visit facts");
            var data=new KnowledgeJournalData(KnowledgeView.create(s));h.assertTrue(data.openRun(PROJECT.toString())==null && data.runs.stream().anyMatch(r->r.status().equals("standby")),"client preserves standby history but offers no active task");
            var restored=KnowledgeState.read(s.state().write());var r=new KnowledgeService(s.teamId(),restored,30,()->{});r.initialize();h.assertTrue(r.standby(EARLY) && restored.research().get(EARLY).equals(backup),"initialization/reload does not rewrite standby progress");
            s.forget(KnowledgeKey.result(RESULT));h.assertTrue(!s.standby(EARLY) && !completedView(s) && s.openRun(PROJECT).orElseThrow().id().equals(EARLY)
                && TechnologyAccessResolver.project(knowledge,legacy).recipe(TEMP).allowed(),"forgetting restores the original backup and permission");
            h.assertTrue(s.advanceStage(EARLY,"early").succeeded(),"resumed stage advances using saved ready progress");h.succeed();
        }finally{KnowledgeDefinitions.install(previous);}
    }
    @GameTest(batch="m6_external_and_union",template="empty",timeoutTicks=80)
    public static void externalResultsDoNotCompleteHistoryAndCrossProjectTemporaryUnionSurvives(GameTestHelper h){
        var previous=KnowledgeDefinitions.current();try{
            install();var s=service();mergePair(s);s.recordCalculation(EARLY,"first",1);
            s.learn(KnowledgeElement.result(RESULT),SOURCE,KnowledgeService.Grant.NORMAL);
            h.assertTrue(!ResearchRuns.completed(s.state(),PROJECT) && s.researchStartStatus(PROJECT)==KnowledgeService.Status.ACTIVE && !s.standby(EARLY),"external learned result cannot fabricate a completed run");
            var other=new ResearchHistory(UUID.randomUUID(),OTHER,List.of(KnowledgeKey.idea(IDEA)),30,false,List.of()).withStages(s.state().research().get(EARLY).stageState());s.state().recordResearch(other);
            s.recordCalculation(LATE,"finish",1);s.finishResearch(LATE);var p=TechnologyAccessResolver.project((TeamKnowledgeData)s.state(),new TeamResearchData());
            h.assertTrue(p.recipe(TEMP).sources().size()==1 && p.recipe(TEMP).allowed() && s.runActive(other),"another project's temporary source survives this project's standby");
            s.forget(KnowledgeKey.result(RESULT));h.assertTrue(TechnologyAccessResolver.project((TeamKnowledgeData)s.state(),new TeamResearchData()).recipe(TEMP).sources().size()==2,"forget restores its source into the union");h.succeed();
        }finally{KnowledgeDefinitions.install(previous);}
    }
    @GameTest(batch="m6_abandon",template="empty",timeoutTicks=80)
    public static void abandonOnlySelectedRunAndNormalSingleRunReopeningStillWork(GameTestHelper h){
        var previous=KnowledgeDefinitions.current();try{
            install();var s=service();mergePair(s);var untouched=s.state().research().get(EARLY);s.abandonResearch(LATE);
            h.assertTrue(s.state().research().get(EARLY).equals(untouched) && s.state().research().get(LATE).abandoned(),"abandon clears selected record only");
            var data=new KnowledgeJournalData(KnowledgeView.create(s));h.assertTrue(data.openRuns(PROJECT.toString()).size()==1 && data.openRun(PROJECT.toString(),LATE.toString()).id().equals(EARLY.toString()),"selection falls back after abandoning its run");
            s.recordCalculation(EARLY,"first",1);s.advanceStage(EARLY,"early");s.recordCalculation(EARLY,"finish",1);s.finishResearch(EARLY);
            h.assertTrue(s.researchStartStatus(PROJECT)==KnowledgeService.Status.ALREADY_OWNED,"single active record completes normally");s.forget(KnowledgeKey.result(RESULT));
            h.assertTrue(s.researchStartStatus(PROJECT)==KnowledgeService.Status.LEARNABLE && s.startResearch(PROJECT).isPresent(),"without backup, normal forget/reopen still creates a fresh run");h.succeed();
        }finally{KnowledgeDefinitions.install(previous);}
    }
    @GameTest(batch="m6_result_validity",template="empty",timeoutTicks=80)
    public static void standbyRequiresEveryResultToRemainValidAndLearned(GameTestHelper h){
        var previous=KnowledgeDefinitions.current();try{
            install();var base=KnowledgeDefinitions.current();var projects=new HashMap<>(base.projects());var original=projects.get(PROJECT);
            projects.put(PROJECT,new ProjectDefinition(original.title(),original.body(),List.of(RESULT,id("other_result")),original.requirements(),original.access(),original.legacyReplacement(),original.stages()));
            KnowledgeDefinitions.install(new KnowledgeDefinitions.Snapshot(0,base.ideas(),base.results(),base.links(),base.itemRetrievalRules(),projects,base.initialKnowledge()));
            var s=service();mergePair(s);s.recordCalculation(LATE,"finish",1);s.finishResearch(LATE);h.assertTrue(s.standby(EARLY),"initially paused");
            var defs=KnowledgeDefinitions.current();var results=new HashMap<>(defs.results());var d=results.get(RESULT);results.put(RESULT,new ResultDefinition(d.result(),d.title(),d.body(),d.understanding(),false));
            KnowledgeDefinitions.install(new KnowledgeDefinitions.Snapshot(0,defs.ideas(),results,defs.links(),defs.itemRetrievalRules(),defs.projects(),defs.initialKnowledge()));
            h.assertTrue(!s.standby(EARLY) && !completedView(s) && s.researchStartStatus(PROJECT)==KnowledgeService.Status.ACTIVE,"disabled result is not valid learned knowledge");
            KnowledgeDefinitions.install(defs);h.assertTrue(s.standby(EARLY),"definition restoration pauses backup again without rewriting it");
            s.forget(KnowledgeKey.result(id("other_result")));h.assertTrue(s.isActive(KnowledgeKey.result(RESULT)) && !s.standby(EARLY),"every result must remain learned; retaining the other is insufficient");h.succeed();
        }finally{KnowledgeDefinitions.install(previous);}
    }
}
