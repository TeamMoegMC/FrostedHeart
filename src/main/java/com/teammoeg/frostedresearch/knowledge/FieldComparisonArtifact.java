/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

/** Persistent report produced by comparing the nearby and control rock samples. */
public record FieldComparisonArtifact(UUID id, ResourceLocation topicId, UUID ideaId,
        UUID nearbySample, UUID controlSample, Outcome outcome, long createdAt) {
    public static final Codec<Outcome> OUTCOME_CODEC = Codec.STRING.xmap(Outcome::valueOf, Enum::name);
    public static final Codec<FieldComparisonArtifact> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(FieldComparisonArtifact::id),
            ResourceLocation.CODEC.fieldOf("topic").forGetter(FieldComparisonArtifact::topicId),
            UUIDUtil.CODEC.fieldOf("idea").forGetter(FieldComparisonArtifact::ideaId),
            UUIDUtil.CODEC.fieldOf("nearby_sample").forGetter(FieldComparisonArtifact::nearbySample),
            UUIDUtil.CODEC.fieldOf("control_sample").forGetter(FieldComparisonArtifact::controlSample),
            OUTCOME_CODEC.fieldOf("outcome").forGetter(FieldComparisonArtifact::outcome),
            Codec.LONG.fieldOf("created_at").forGetter(FieldComparisonArtifact::createdAt)
    ).apply(instance, FieldComparisonArtifact::new));

    public enum Outcome {
        MATCH,
        NO_MATCH,
        INSUFFICIENT
    }
}
