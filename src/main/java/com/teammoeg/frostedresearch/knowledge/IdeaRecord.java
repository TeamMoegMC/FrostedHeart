/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Team idea and all of the independent ways in which it was learned. */
public record IdeaRecord(UUID id, ResourceLocation topicId, ResourceLocation ideaId, State state,
        Set<String> sources, Set<UUID> evidence, long lastUpdated) {
    public static final Codec<State> STATE_CODEC = Codec.STRING.xmap(State::valueOf, Enum::name);
    public static final Codec<IdeaRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(IdeaRecord::id),
            ResourceLocation.CODEC.fieldOf("topic").forGetter(IdeaRecord::topicId),
            ResourceLocation.CODEC.fieldOf("idea").forGetter(IdeaRecord::ideaId),
            STATE_CODEC.optionalFieldOf("state", State.OPEN).forGetter(IdeaRecord::state),
            Codec.STRING.listOf().optionalFieldOf("sources", List.of()).forGetter(value -> List.copyOf(value.sources)),
            UUIDUtil.CODEC.listOf().optionalFieldOf("evidence", List.of()).forGetter(value -> List.copyOf(value.evidence)),
            Codec.LONG.optionalFieldOf("last_updated", 0L).forGetter(IdeaRecord::lastUpdated)
    ).apply(instance, (id, topic, idea, state, sources, evidence, updated) ->
            new IdeaRecord(id, topic, idea, state, new LinkedHashSet<>(sources), new LinkedHashSet<>(evidence), updated)));

    public IdeaRecord {
        sources = Set.copyOf(sources);
        evidence = Set.copyOf(evidence);
    }

    public static IdeaRecord create(ResourceLocation topic, ResourceLocation idea, String source,
            Set<UUID> evidence, long time) {
        UUID id = UUID.nameUUIDFromBytes((topic + "|" + idea).getBytes(StandardCharsets.UTF_8));
        return new IdeaRecord(id, topic, idea, State.OPEN, Set.of(source), evidence, time);
    }

    public IdeaRecord merge(String source, Set<UUID> addedEvidence, long time) {
        Set<String> mergedSources = new LinkedHashSet<>(sources);
        mergedSources.add(source);
        Set<UUID> mergedEvidence = new LinkedHashSet<>(evidence);
        mergedEvidence.addAll(addedEvidence);
        return new IdeaRecord(id, topicId, ideaId, state, mergedSources, mergedEvidence,
                Math.max(lastUpdated, time));
    }

    public IdeaRecord withState(State replacement, long time) {
        return new IdeaRecord(id, topicId, ideaId, replacement, sources, evidence, Math.max(lastUpdated, time));
    }

    /** Adds field or method evidence without pretending it was another idea source. */
    public IdeaRecord withEvidence(Set<UUID> addedEvidence, long time) {
        Set<UUID> mergedEvidence = new LinkedHashSet<>(evidence);
        mergedEvidence.addAll(addedEvidence);
        return new IdeaRecord(id, topicId, ideaId, state, sources, mergedEvidence,
                Math.max(lastUpdated, time));
    }

    public enum State {
        OPEN,
        READY,
        RESOLVED,
        ORPHAN
    }
}
