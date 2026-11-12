package com.teammoeg.frostedresearch.knowledge.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Saved stage order and entry choices; no future branch definitions are copied into a run. */
public record StageRunState(List<String> order,String current,Map<String,Visit> visits,Set<String> completedTasks,LegacyStageProgress legacyProgress) {
    public static final Codec<StageRunState> CODEC=RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.listOf().fieldOf("order").forGetter(StageRunState::order),
        Codec.STRING.fieldOf("current").forGetter(StageRunState::current),
        Codec.unboundedMap(Codec.STRING,Visit.CODEC).fieldOf("visits").forGetter(StageRunState::visits),
        Codec.STRING.listOf().optionalFieldOf("completed_tasks",List.of()).forGetter(s -> s.completedTasks.stream().sorted().toList()),
        com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey.strictOptionalField(LegacyStageProgress.CODEC,"legacy_progress").forGetter(s -> s.legacyProgress.isEmpty() ? Optional.empty() : Optional.of(s.legacyProgress))
    ).apply(i,(order,current,visits,tasks,legacy) -> new StageRunState(order,current,visits,new HashSet<>(tasks),legacy.orElseGet(LegacyStageProgress::empty))));
    public StageRunState { order=List.copyOf(order);visits=Map.copyOf(visits);completedTasks=Set.copyOf(completedTasks); }
    public StageRunState(List<String> order,String current,Map<String,Visit> visits,Set<String> tasks) {
        this(order,current,visits,tasks,LegacyStageProgress.empty());
    }
    public static StageRunState legacy() { return new StageRunState(List.of("main"),"main",Map.of(),Set.of()); }
    public record Visit(String variant,List<String> tasks,boolean ready,List<ResourceLocation> temporaryRecipes) {
        public static final Codec<Visit> CODEC=RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("variant").forGetter(Visit::variant),
            Codec.STRING.listOf().fieldOf("tasks").forGetter(Visit::tasks),
            Codec.BOOL.optionalFieldOf("ready",false).forGetter(Visit::ready),
            ResourceLocation.CODEC.listOf().optionalFieldOf("temporary_recipes",List.of()).forGetter(Visit::temporaryRecipes)
        ).apply(i,Visit::new));
        public Visit { tasks=List.copyOf(tasks);temporaryRecipes=List.copyOf(temporaryRecipes); }
    }
    public StageRunState enter(String stage,Visit visit) {
        var next=new LinkedHashMap<>(visits);next.put(stage,visit);return new StageRunState(order,stage,next,completedTasks,legacyProgress);
    }
    public StageRunState success(String task) {
        var next=new HashSet<>(completedTasks);next.add(task);return new StageRunState(order,current,visits,next,legacyProgress);
    }
}
