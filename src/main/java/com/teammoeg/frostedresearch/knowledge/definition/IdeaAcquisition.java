package com.teammoeg.frostedresearch.knowledge.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Acquisition gates apply to behavior and linking, never to external grants or learning. */
public record IdeaAcquisition(List<Trigger> triggers, List<KnowledgeCondition> required,
        List<KnowledgeCondition> optional, Optional<Double> chance) {
    public static final IdeaAcquisition NONE = new IdeaAcquisition(List.of(), List.of(), List.of(), Optional.empty());
    // JsonOps accepts booleans as numbers; author probabilities must be actual finite numeric values.
    private static final Codec<Double> PROBABILITY = Codec.PASSTHROUGH.comapFlatMap(raw -> {
        var json = raw.convert(com.mojang.serialization.JsonOps.INSTANCE).getValue();
        if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber())
            return com.mojang.serialization.DataResult.error(() -> "chance must be a number in [0,1]");
        double value = json.getAsDouble();
        return Double.isFinite(value) && value >= 0 && value <= 1 ? com.mojang.serialization.DataResult.success(value)
            : com.mojang.serialization.DataResult.error(() -> "chance must be a finite number in [0,1]");
    }, value -> new com.mojang.serialization.Dynamic<>(com.mojang.serialization.JsonOps.INSTANCE, new com.google.gson.JsonPrimitive(value)));
    public static final Codec<IdeaAcquisition> CODEC = RecordCodecBuilder.create(i -> i.group(
        ProjectRequirement.field(Trigger.CODEC.listOf(), "triggers", List.of()).forGetter(IdeaAcquisition::triggers),
        ProjectRequirement.field(KnowledgeCondition.CODEC.listOf(), "required", List.of()).forGetter(IdeaAcquisition::required),
        ProjectRequirement.field(KnowledgeCondition.CODEC.listOf(), "optional", List.of()).forGetter(IdeaAcquisition::optional),
        KnowledgeKey.strictOptionalField(PROBABILITY, "chance").forGetter(IdeaAcquisition::chance)
    ).apply(i, IdeaAcquisition::new));
    public record Trigger(ResourceLocation event, Optional<KnowledgeCondition> subject) {
        public static final Codec<Trigger> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("event").forGetter(Trigger::event),
            KnowledgeKey.strictOptionalField(KnowledgeCondition.CODEC, "subject").forGetter(Trigger::subject)
        ).apply(i, Trigger::new));
    }
    public IdeaAcquisition { triggers = List.copyOf(triggers); required = List.copyOf(required); optional = List.copyOf(optional); }
    public List<KnowledgeCondition> allConditions() {
        List<KnowledgeCondition> all = new ArrayList<>(required); all.addAll(optional);
        triggers.forEach(t -> t.subject().ifPresent(all::add)); return List.copyOf(all);
    }
}
