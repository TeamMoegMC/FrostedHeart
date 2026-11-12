package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.knowledge.definition.*;
import com.teammoeg.frostedresearch.knowledge.state.*;
import java.util.*;

/** Pure stage resolution; callers persist entry choices only at actual lifecycle/actions. */
public final class ResearchStages {
    private ResearchStages() {}
    public static ResearchHistory prepare(KnowledgeService service,ResearchHistory run) {
        var project=KnowledgeDefinitions.current().projects().get(run.project());
        if(project==null || !run.open() || !run.stageState().visits().isEmpty()) return run;
        var stages=project.orderedStages();
        var ids=stages.get(0).tasks().stream().map(t -> t.requirement().id()).collect(java.util.stream.Collectors.toSet());
        var archived=project.stages().isEmpty() ? LegacyStageProgress.empty() : LegacyStageProgress.excluded(run,ids);
        if(!project.stages().isEmpty()) run=run.retainTasks(ids);
        var state=new StageRunState(stages.stream().map(StageDefinition::id).toList(),stages.get(0).id(),Map.of(),Set.of(),archived);
        return run.withStages(enter(service,run.withStages(state),stages.get(0)));
    }
    public static StageRunState enter(KnowledgeService service,ResearchHistory run,StageDefinition stage) {
        StageDefinition.Variant selected=stage.variants().stream().filter(v -> v.when().isEmpty() ||
            v.when().get().knowledgeAll().stream().allMatch(c -> service.matchKnowledge(c).isPresent()) &&
            v.when().get().tasksAll().stream().allMatch(run.stageState().completedTasks()::contains)).findFirst().orElse(null);
        Set<String> omitted=selected==null ? Set.of() : new HashSet<>(selected.omitTasks());
        return run.stageState().enter(stage.id(),new StageRunState.Visit(selected==null ? "default" : selected.id(),
            stage.tasks().stream().map(t -> t.requirement().id()).filter(id -> !omitted.contains(id)).toList(),false,List.of()));
    }
    public static Optional<StageDefinition> current(ResearchHistory run) {
        var project=KnowledgeDefinitions.current().projects().get(run.project());
        return project==null ? Optional.empty() : project.stage(run.stageState().current());
    }
    public static boolean available(ResearchHistory run) {
        return availableVisit(run,run.stageState().current());
    }
    public static boolean availableVisit(ResearchHistory run,String stageId) {
        var project=KnowledgeDefinitions.current().projects().get(run.project());
        var stage=project==null ? null : project.stage(stageId).orElse(null);var visit=run.stageState().visits().get(stageId);
        if(stage==null || visit==null) return false;
        if(stage.variants().isEmpty() ? !visit.variant().equals("default") : stage.variants().stream().noneMatch(v -> v.id().equals(visit.variant()))) return false;
        Set<String> defined=new HashSet<>();stage.tasks().forEach(t -> defined.add(t.requirement().id()));
        return defined.containsAll(visit.tasks());
    }
    public static List<StageDefinition.Task> tasks(ResearchHistory run) {
        if(!available(run)) return List.of();
        var visit=run.stageState().visits().get(run.stageState().current());
        return current(run).orElseThrow().tasks().stream().filter(t -> visit.tasks().contains(t.requirement().id())).toList();
    }
    public static ResearchHistory update(KnowledgeService service,ResearchHistory raw) {
        var run=prepare(service,raw);
        if(!available(run)) return run;
        var state=run.stageState();var stage=current(run).orElseThrow();var visit=state.visits().get(state.current());
        for(var task:tasks(run)) if(service.requirementMet(run,task.requirement())) state=state.success(task.requirement().id());
        boolean flat=KnowledgeDefinitions.current().projects().get(run.project()).stages().isEmpty();
        boolean ready=tasks(run).stream().filter(t -> !t.optional()).allMatch(t -> service.requirementMet(run,t.requirement())) && (!flat || !tasks(run).isEmpty());
        if(!visit.ready() && ready) state=state.enter(state.current(),new StageRunState.Visit(visit.variant(),visit.tasks(),true,stage.completionRecipes()));
        return run.withStages(state);
    }
    public static boolean last(ResearchHistory run) {
        return !run.stageState().order().isEmpty() && run.stageState().current().equals(run.stageState().order().get(run.stageState().order().size()-1));
    }
}
