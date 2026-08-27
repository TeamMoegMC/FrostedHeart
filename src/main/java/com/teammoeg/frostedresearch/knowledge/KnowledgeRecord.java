/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.utility.oredetect.OreProspectingModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * A semantically deduplicated observation owned by a team.
 *
 * <p>{@link Type} remains as a compatibility view for the first geology slice. New
 * observation code should use {@link #kindId()} and {@link #publicFacets()} instead.
 */
public record KnowledgeRecord(UUID id, String semanticKey, Type type, ResourceLocation kindId,
        ResourceLocation dimension, BlockPos position, ResourceLocation subject,
        Map<String, String> stateProperties, Map<String, String> contextFacts,
        long firstObserved, long lastObserved,
        Set<UUID> observers, Set<ResourceLocation> publicFacets,
        Set<ResourceLocation> channels, Optional<OreProspectingModel.Snapshot> sealedFacts) {
    public static final ResourceLocation BLOCK_KIND = id("block_observation");
    public static final ResourceLocation ENTITY_KIND = id("entity_observation");
    public static final ResourceLocation COPPER_OUTCROP_KIND = heartId("copper_outcrop");
    public static final ResourceLocation ROCK_SAMPLE_KIND = heartId("rock_sample");
    public static final ResourceLocation BLOCK_OBSERVATION_FACET = id("block_observation");
    public static final ResourceLocation ENTITY_OBSERVATION_FACET = id("entity_observation");
    public static final ResourceLocation COPPER_OUTCROP_FACET = heartId("copper_ore_sighting");
    public static final ResourceLocation ROCK_SAMPLE_FACET = heartId("rock_sample");

    private static final Codec<Set<ResourceLocation>> RESOURCE_SET_CODEC = ResourceLocation.CODEC.listOf()
            .xmap(LinkedHashSet::new, KnowledgeRecord::sortedResources);
    public static final Codec<Type> TYPE_CODEC = Codec.STRING.xmap(Type::valueOf, Enum::name);
    public static final Codec<KnowledgeRecord> CODEC = codec(OreProspectingModel.Snapshot.CODEC.optionalFieldOf("sealed_facts"));

    private static Codec<KnowledgeRecord> codec(com.mojang.serialization.MapCodec<Optional<OreProspectingModel.Snapshot>> facts) {
        return RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(KnowledgeRecord::id),
                Codec.STRING.fieldOf("semantic_key").forGetter(KnowledgeRecord::semanticKey),
                TYPE_CODEC.fieldOf("type").forGetter(KnowledgeRecord::type),
                ResourceLocation.CODEC.optionalFieldOf("kind", BLOCK_KIND).forGetter(KnowledgeRecord::kindId),
                ResourceLocation.CODEC.fieldOf("dimension").forGetter(KnowledgeRecord::dimension),
                BlockPos.CODEC.fieldOf("position").forGetter(KnowledgeRecord::position),
                ResourceLocation.CODEC.fieldOf("subject").forGetter(KnowledgeRecord::subject),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("state", Map.of())
                        .forGetter(KnowledgeRecord::stateProperties),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("context", Map.of())
                        .forGetter(KnowledgeRecord::contextFacts),
                Codec.LONG.fieldOf("first_observed").forGetter(KnowledgeRecord::firstObserved),
                Codec.LONG.fieldOf("last_observed").forGetter(KnowledgeRecord::lastObserved),
                UUIDUtil.CODEC.listOf().optionalFieldOf("observers", List.of())
                        .forGetter(value -> List.copyOf(value.observers)),
                RESOURCE_SET_CODEC.optionalFieldOf("public_facets", Set.of())
                        .forGetter(KnowledgeRecord::publicFacets),
                RESOURCE_SET_CODEC.optionalFieldOf("channels", Set.of())
                        .forGetter(KnowledgeRecord::channels),
                facts.forGetter(KnowledgeRecord::sealedFacts)
        ).apply(instance, (id, key, type, kind, dimension, position, subject, state, context, first, last,
                observers, facets, channels, sealed) -> new KnowledgeRecord(id, key, type,
                        normalizeKind(type, kind), dimension, position, subject, state, context, first, last,
                        new LinkedHashSet<>(observers), facets.isEmpty() ? defaultFacets(type) : facets,
                        channels, sealed)));
    }

    public KnowledgeRecord {
        position = position.immutable();
        stateProperties = Collections.unmodifiableMap(new TreeMap<>(stateProperties));
        contextFacts = Collections.unmodifiableMap(new TreeMap<>(contextFacts));
        observers = Set.copyOf(observers);
        publicFacets = Set.copyOf(publicFacets);
        channels = Set.copyOf(channels);
    }

    /** Compatibility factory used by the initial geology implementation and old tests. */
    public static KnowledgeRecord create(Type type, ResourceLocation dimension, BlockPos position,
            ResourceLocation subject, long observedAt, UUID observer,
            Optional<OreProspectingModel.Snapshot> sealedFacts) {
        String key = semanticKey(type, dimension, position, subject);
        return create(type, defaultKind(type), dimension, position, subject, Map.of(), observedAt,
                observer, defaultFacets(type), Set.of(), sealedFacts, key);
    }

    /**
     * Creates an observation whose kind/provider already chose its semantic key.
     */
    public static KnowledgeRecord create(Type type, ResourceLocation kindId,
            ResourceLocation dimension, BlockPos position, ResourceLocation subject,
            Map<String, String> stateProperties, long observedAt, UUID observer,
            Set<ResourceLocation> publicFacets, Set<ResourceLocation> channels,
            Optional<OreProspectingModel.Snapshot> sealedFacts, String semanticKey) {
        return create(type, kindId, dimension, position, subject, stateProperties, Map.of(), observedAt,
                observer, publicFacets, channels, sealedFacts, semanticKey);
    }

    public static KnowledgeRecord create(Type type, ResourceLocation kindId,
            ResourceLocation dimension, BlockPos position, ResourceLocation subject,
            Map<String, String> stateProperties, Map<String, String> contextFacts,
            long observedAt, UUID observer, Set<ResourceLocation> publicFacets,
            Set<ResourceLocation> channels, Optional<OreProspectingModel.Snapshot> sealedFacts,
            String semanticKey) {
        UUID id = UUID.nameUUIDFromBytes(semanticKey.getBytes(StandardCharsets.UTF_8));
        return new KnowledgeRecord(id, semanticKey, type, kindId, dimension, position, subject,
                stateProperties, contextFacts, observedAt, observedAt, Set.of(observer), publicFacets,
                channels, sealedFacts);
    }

    public KnowledgeRecord merge(KnowledgeRecord newer) {
        if (!semanticKey.equals(newer.semanticKey)) throw new IllegalArgumentException("Cannot merge different observations");
        Set<UUID> merged = new LinkedHashSet<>(observers);
        merged.addAll(newer.observers);
        Set<ResourceLocation> mergedFacets = new LinkedHashSet<>(publicFacets);
        mergedFacets.addAll(newer.publicFacets);
        Set<ResourceLocation> mergedChannels = new LinkedHashSet<>(channels);
        mergedChannels.addAll(newer.channels);
        return new KnowledgeRecord(id, semanticKey, newer.type, newer.kindId, dimension,
                newer.position, subject, newer.stateProperties, newer.contextFacts,
                Math.min(firstObserved, newer.firstObserved), Math.max(lastObserved, newer.lastObserved),
                merged, mergedFacets, mergedChannels,
                newer.sealedFacts.isPresent() ? newer.sealedFacts : sealedFacts);
    }

    public KnowledgeRecord withoutSealedFacts() {
        return new KnowledgeRecord(id, semanticKey, type, kindId, dimension, position, subject,
                stateProperties, contextFacts, firstObserved, lastObserved, observers, publicFacets, channels,
                Optional.empty());
    }

    public static String semanticKey(Type type, ResourceLocation dimension, BlockPos position, ResourceLocation subject) {
        if (type == Type.BLOCK || type == Type.ENTITY) {
            return BLOCK_KIND + "|" + dimension + "|" + position.getX() + "," +
                    position.getY() + "," + position.getZ() + "|" + subject + "|[]";
        }
        return type.name() + "|" + dimension + "|" + (position.getX() >> 4) + "," +
                (position.getY() >> 4) + "," + (position.getZ() >> 4) + "|" + subject;
    }

    public enum Type {
        BLOCK,
        ENTITY,
        COPPER_OUTCROP,
        ROCK_SAMPLE
    }

    private static ResourceLocation normalizeKind(Type type, ResourceLocation kind) {
        if (kind.equals(BLOCK_KIND) && type != Type.BLOCK) return defaultKind(type);
        return kind;
    }

    private static ResourceLocation defaultKind(Type type) {
        return switch (type) {
            case BLOCK -> BLOCK_KIND;
            case ENTITY -> ENTITY_KIND;
            case COPPER_OUTCROP -> COPPER_OUTCROP_KIND;
            case ROCK_SAMPLE -> ROCK_SAMPLE_KIND;
        };
    }

    private static Set<ResourceLocation> defaultFacets(Type type) {
        Set<ResourceLocation> facets = new LinkedHashSet<>();
        facets.add(BLOCK_OBSERVATION_FACET);
        if (type == Type.ENTITY) {
            facets.remove(BLOCK_OBSERVATION_FACET);
            facets.add(ENTITY_OBSERVATION_FACET);
        }
        if (type == Type.COPPER_OUTCROP) facets.add(COPPER_OUTCROP_FACET);
        if (type == Type.ROCK_SAMPLE) facets.add(ROCK_SAMPLE_FACET);
        return Set.copyOf(facets);
    }

    private static List<ResourceLocation> sortedResources(Set<ResourceLocation> values) {
        List<ResourceLocation> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.comparing(ResourceLocation::toString));
        return sorted;
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("frostedresearch", path);
    }

    private static ResourceLocation heartId(String path) {
        return new ResourceLocation("frostedheart", path);
    }
}
