package com.teammoeg.frostedresearch.knowledge.experiment;

import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.knowledge.definition.ProjectDefinition;
import com.teammoeg.frostedresearch.knowledge.definition.ProjectRequirement;
import com.teammoeg.frostedresearch.knowledge.item.ItemRetrieval;
import com.teammoeg.frostedresearch.knowledge.state.ResearchHistory;
import com.teammoeg.frostedresearch.knowledge.state.StageRunState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Set;

/** The recipe identity survives collecting either of the two cracked kiln samples. */
public final class TrialKilnSamples {
    public static final String TAG = "TrialKilnVariant";
    public static final List<String> VARIANTS = List.of("clay_rich", "sand_rich", "balanced");

    private TrialKilnSamples() {}

    public static String variant(BlockState state) {
        if (state.is(FRContents.Blocks.TRIAL_KILN_CLAY_RICH.get())) return "clay_rich";
        if (state.is(FRContents.Blocks.TRIAL_KILN_SAND_RICH.get())) return "sand_rich";
        return "balanced";
    }

    public static String variant(ItemStack stack) {
        String value = stack.hasTag() ? stack.getTag().getString(TAG) : "";
        return VARIANTS.contains(value) ? value : "unknown";
    }

    public static ResourceLocation experiment(String variant) {
        return new ResourceLocation("frostedresearch", "trial_kiln/" + variant);
    }

    public static void registerObservationFields() {
        for (String item : List.of("fired_trial_kiln", "cracked_trial_kiln"))
            ItemRetrieval.registerField(new ResourceLocation("frostedresearch", item), "trial_kiln_variant", TrialKilnSamples::variant);
    }

    /** Existing kiln trials already reached firing before drawings became a prerequisite. */
    public static ResearchHistory adoptDrawingStage(ResearchHistory run, ProjectDefinition project) {
        var previous = run.stageState();
        if (!run.project().equals(new ResourceLocation("the_winter_rescue:earthen_smelter"))
            || !previous.order().equals(List.of("main"))) return run;
        var stages = project.orderedStages();
        if (stages.size() != 2 || !stages.get(0).id().equals("drawing") || !stages.get(1).id().equals("main")) return run;
        var drawing = stages.get(0);
        var taskIds = drawing.tasks().stream().map(task -> task.requirement().id()).toList();
        var state = new StageRunState(List.of("drawing", "main"), "main", previous.visits(),
            previous.completedTasks(), previous.legacyProgress());
        state = state.enter("drawing", new StageRunState.Visit("default", taskIds, true, drawing.completionRecipes()));
        for (String task : taskIds) state = state.success(task);
        var trial = previous.visits().getOrDefault("main", new StageRunState.Visit("default",
            stages.get(1).tasks().stream().map(task -> task.requirement().id()).toList(), false, List.of()));
        return run.withStages(state.enter("main", trial));
    }

    /** Upgrade only the former single-sample route, keeping its intact-sample credit and run identity. */
    public static ResearchHistory reconcile(ResearchHistory run, ProjectDefinition project) {
        if (!run.open()) return run;
        var trial = project.stage("main");
        if (trial.isEmpty() || !run.stageState().current().equals("main")) return run;
        var requirements = trial.get().tasks().stream().map(task -> task.requirement()).toList();
        if (requirements.size() != 3) return run;
        var visit = run.stageState().visits().get("main");
        if (visit == null) return run;
        boolean oldTasks = visit.tasks().equals(List.of("submit_intact"));
        var submitted = run.samples().get("submit_intact");
        boolean credit = submitted != null && submitted.experiment().equals(TrialKilnBlockEntity.EXPERIMENT)
            && submitted.item().equals(new ResourceLocation("frostedresearch:fired_trial_kiln"));
        if (!oldTasks && (!credit || run.stageState().completedTasks().contains("submit_intact"))) return run;
        var experiments = requirements.stream().filter(r -> r.type() == ProjectRequirement.Type.OBSERVATION)
            .flatMap(r -> r.observation().stream()).flatMap(o -> o.experiment().stream()).collect(java.util.stream.Collectors.toSet());
        if (!experiments.equals(Set.copyOf(VARIANTS.stream().map(TrialKilnSamples::experiment).toList()))
            || project.requirement("submit_intact").isEmpty()) return run;
        var state = oldTasks ? run.stageState().enter("main", new StageRunState.Visit(visit.variant(),
            requirements.stream().map(ProjectRequirement::id).toList(), false, visit.temporaryRecipes())) : run.stageState();
        if (credit) state = state.success("submit_intact");
        return run.withStages(state);
    }
}
