package com.teammoeg.frostedresearch.knowledge.definition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
/** Static mapping of recorded legacy facts, not experiment history. */
public record LegacyReplacement(String research, Map<String, ResourceLocation> effects, ResourceLocation partialIdea) {
    public static final Codec<LegacyReplacement> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("research").forGetter(LegacyReplacement::research),
        Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC).fieldOf("effects").forGetter(LegacyReplacement::effects),
        ResourceLocation.CODEC.fieldOf("partial_idea").forGetter(LegacyReplacement::partialIdea)
    ).apply(i, LegacyReplacement::new));
    public LegacyReplacement { effects = Map.copyOf(effects); }
}
