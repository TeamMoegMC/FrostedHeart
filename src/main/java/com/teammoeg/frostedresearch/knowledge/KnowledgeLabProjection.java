/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * Complete client-safe read model for the Knowledge Lab archive.
 *
 * <p>Unlike {@link KnowledgeProjection}, this model is organised for browsing rather than for
 * moment-to-moment gameplay. It deliberately includes orphan result IDs, while still excluding
 * every server-sealed observation fact.</p>
 */
public record KnowledgeLabProjection(
        List<KnowledgeProjection.ObservationSummary> observations,
        List<KnowledgeProjection.IdeaSummary> ideas,
        List<KnowledgeProjection.ComparisonSummary> artifacts,
        List<ResultSummary> results) {
    public static final Codec<KnowledgeLabProjection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            KnowledgeProjection.ObservationSummary.CODEC.listOf().optionalFieldOf("observations", List.of())
                    .forGetter(KnowledgeLabProjection::observations),
            KnowledgeProjection.IdeaSummary.CODEC.listOf().optionalFieldOf("ideas", List.of())
                    .forGetter(KnowledgeLabProjection::ideas),
            KnowledgeProjection.ComparisonSummary.CODEC.listOf().optionalFieldOf("artifacts", List.of())
                    .forGetter(KnowledgeLabProjection::artifacts),
            ResultSummary.CODEC.listOf().optionalFieldOf("results", List.of())
                    .forGetter(KnowledgeLabProjection::results)
    ).apply(instance, KnowledgeLabProjection::new));
    public static final KnowledgeLabProjection EMPTY = new KnowledgeLabProjection(
            List.of(), List.of(), List.of(), List.of());

    public KnowledgeLabProjection {
        observations = List.copyOf(observations);
        ideas = List.copyOf(ideas);
        artifacts = List.copyOf(artifacts);
        results = List.copyOf(results);
    }

    /** One acquired result, including recoverable IDs no longer present in the active catalogue. */
    public record ResultSummary(ResearchResult.ResultType type, ResourceLocation id,
            Optional<ResourceLocation> topicId, List<ResourceLocation> targets, boolean orphan) {
        public static final Codec<ResultSummary> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResearchResult.RESULT_TYPE_CODEC.fieldOf("type").forGetter(ResultSummary::type),
                ResourceLocation.CODEC.fieldOf("id").forGetter(ResultSummary::id),
                ResourceLocation.CODEC.optionalFieldOf("topic").forGetter(ResultSummary::topicId),
                ResourceLocation.CODEC.listOf().optionalFieldOf("targets", List.of()).forGetter(ResultSummary::targets),
                Codec.BOOL.optionalFieldOf("orphan", false).forGetter(ResultSummary::orphan)
        ).apply(instance, ResultSummary::new));

        public ResultSummary {
            targets = List.copyOf(targets);
        }
    }
}
