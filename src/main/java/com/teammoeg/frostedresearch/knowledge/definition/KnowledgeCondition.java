package com.teammoeg.frostedresearch.knowledge.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedresearch.knowledge.link.*;
import com.teammoeg.frostedresearch.knowledge.model.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** An existence predicate, independent of acquisition and research tasks. */
public record KnowledgeCondition(KnowledgeKey.Kind kind, Optional<ResourceLocation> definition,
        Optional<Observation.Type> observationType, List<FieldCondition> conditions, String title, String hint) {
    public static final Codec<KnowledgeCondition> CODEC = RecordCodecBuilder.create(i -> i.group(
        KnowledgeKey.KIND_CODEC.fieldOf("kind").forGetter(KnowledgeCondition::kind),
        KnowledgeKey.strictOptionalField(ResourceLocation.CODEC, "definition").forGetter(KnowledgeCondition::definition),
        KnowledgeKey.strictOptionalField(KnowledgeKey.enumCodec(Observation.Type.class), "observation_type").forGetter(KnowledgeCondition::observationType),
        field(FieldCondition.CODEC.listOf(), "conditions", List.of()).forGetter(KnowledgeCondition::conditions),
        field(Codec.STRING, "title", "").forGetter(KnowledgeCondition::title),
        field(Codec.STRING, "hint", "").forGetter(KnowledgeCondition::hint)
    ).apply(i, KnowledgeCondition::new));
    // Do not initialize ProjectRequirement from this codec: observation tasks refer back to KnowledgeCondition.
    private static <T> com.mojang.serialization.MapCodec<T> field(Codec<T> codec,String name,T fallback) {
        return KnowledgeKey.strictOptionalField(codec,name).xmap(value -> value.orElse(fallback),Optional::of);
    }
    public KnowledgeCondition { conditions = List.copyOf(new LinkedHashSet<>(conditions)); }
    public LinkRule.Slot slot() { return new LinkRule.Slot("knowledge", kind, definition, observationType, conditions); }
    public SlotPattern pattern() { return new SlotPattern(List.of(slot()), List.of()); }
    public boolean matches(KnowledgeElement element) { return slot().matches(element, TagLookup.SERVER); }
    public boolean samePredicate(KnowledgeCondition other) {
        return kind == other.kind && definition.equals(other.definition) && observationType.equals(other.observationType)
            && new HashSet<>(conditions).equals(new HashSet<>(other.conditions));
    }
    public void validate(List<String> errors) {
        slot().validate(errors);
        if (kind == KnowledgeKey.Kind.OBSERVATION && observationType.isEmpty()) errors.add("observation_type required");
        if (conditions.stream().anyMatch(c -> c.field().equals("record_id") || c.field().equals("entity_instance")))
            errors.add("runtime observation identities cannot be prerequisites");
    }
}
