package com.teammoeg.frostedresearch.knowledge.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Ordered stage; variants only omit tasks and change the method. */
public record StageDefinition(String id, String title, String objective, String method, String conclusion,
        List<Task> tasks, List<Variant> variants, List<ResourceLocation> completionRecipes) {
    public static final Codec<StageDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("id").forGetter(StageDefinition::id),
        ProjectRequirement.field(Codec.STRING,"title","").forGetter(StageDefinition::title),
        ProjectRequirement.field(Codec.STRING,"objective","").forGetter(StageDefinition::objective),
        ProjectRequirement.field(Codec.STRING,"method","").forGetter(StageDefinition::method),
        ProjectRequirement.field(Codec.STRING,"conclusion","").forGetter(StageDefinition::conclusion),
        ProjectRequirement.field(Task.CODEC.listOf(),"tasks",List.of()).forGetter(StageDefinition::tasks),
        ProjectRequirement.field(Variant.CODEC.listOf(),"variants",List.of()).forGetter(StageDefinition::variants),
        ProjectRequirement.field(ResourceLocation.CODEC.listOf(),"completion_recipes",List.of()).forGetter(StageDefinition::completionRecipes)
    ).apply(i,StageDefinition::new));
    public StageDefinition { tasks=List.copyOf(tasks);variants=List.copyOf(variants);completionRecipes=List.copyOf(completionRecipes); }
    public record Task(ProjectRequirement requirement, boolean optional, String method) {
        public static final Codec<Task> CODEC=RecordCodecBuilder.create(i -> i.group(
            ProjectRequirement.CODEC.fieldOf("requirement").forGetter(Task::requirement),
            ProjectRequirement.field(Codec.BOOL,"optional",false).forGetter(Task::optional),
            ProjectRequirement.field(Codec.STRING,"method","").forGetter(Task::method)
        ).apply(i,Task::new));
    }
    public record When(List<KnowledgeCondition> knowledgeAll,List<String> tasksAll) {
        public static final Codec<When> CODEC=RecordCodecBuilder.create(i -> i.group(
            ProjectRequirement.field(KnowledgeCondition.CODEC.listOf(),"knowledge_all",List.of()).forGetter(When::knowledgeAll),
            ProjectRequirement.field(Codec.STRING.listOf(),"tasks_all",List.of()).forGetter(When::tasksAll)
        ).apply(i,When::new));
        public When { knowledgeAll=List.copyOf(knowledgeAll);tasksAll=List.copyOf(tasksAll); }
    }
    public record Variant(String id,Optional<When> when,List<String> omitTasks,Optional<String> method) {
        public static final Codec<Variant> CODEC=RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(Variant::id),
            KnowledgeKey.strictOptionalField(When.CODEC,"when").forGetter(Variant::when),
            ProjectRequirement.field(Codec.STRING.listOf(),"omit_tasks",List.of()).forGetter(Variant::omitTasks),
            KnowledgeKey.strictOptionalField(Codec.STRING,"method").forGetter(Variant::method)
        ).apply(i,Variant::new));
        public Variant { omitTasks=List.copyOf(omitTasks); }
    }
}
