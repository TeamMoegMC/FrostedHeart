/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.observation;

import com.teammoeg.frostedresearch.knowledge.KnowledgeRecord;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Ordered runtime registry for observation adapters. */
public final class ObservationProviderRegistry {
    public static final ResourceLocation NOTEBOOK_CHANNEL = new ResourceLocation("frostedresearch", "research_notebook");
    private static final Map<ResourceLocation, ObservationProvider> PROVIDERS = new LinkedHashMap<>();

    static {
        register(new GenericBlockObservationProvider());
    }

    private ObservationProviderRegistry() {
    }

    /** Replaces a provider with the same stable ID, which keeps development reloads deterministic. */
    public static synchronized void register(ObservationProvider provider) {
        PROVIDERS.put(provider.id(), provider);
    }

    public static KnowledgeRecord observeBlock(BlockGetter level, ObservationContext context) {
        ObservationProvider provider = orderedProviders().stream()
                .filter(candidate -> candidate.supports(context))
                .findFirst().orElseThrow();
        ObservationProvider.Contribution contribution = provider.observe(level, context);
        Map<String, String> state = visibleState(context.state());
        Set<ResourceLocation> facets = new LinkedHashSet<>();
        facets.add(KnowledgeRecord.BLOCK_OBSERVATION_FACET);
        facets.addAll(contribution.publicFacets());
        String semanticKey = contribution.deduplication()
                .semanticKey(contribution.kindId(), context, state);
        return KnowledgeRecord.create(contribution.compatibilityType(), contribution.kindId(),
                context.dimension(), context.position(), context.subject(),
                context.retainedFields().contains(ObservationContext.Field.BLOCK_STATE) ? state : Map.of(),
                context.publicFacts(),
                context.observedAt(), context.observer(), facets, Set.of(context.channel()),
                contribution.sealedFacts(), semanticKey);
    }

    /** Generic entity fallback. Domain mods can later enrich it through a dedicated provider registry. */
    public static KnowledgeRecord observeEntity(ObservationContext context, java.util.UUID entityId) {
        if (context.targetType() != ObservationContext.TargetType.ENTITY) {
            throw new IllegalArgumentException("Entity observation requires an ENTITY context");
        }
        Map<String, String> facts = new LinkedHashMap<>(context.publicFacts());
        facts.put("entity_uuid", entityId.toString());
        String semanticKey = KnowledgeRecord.ENTITY_KIND + "|" + entityId + "|" + context.observedAt();
        return KnowledgeRecord.create(KnowledgeRecord.Type.ENTITY, KnowledgeRecord.ENTITY_KIND,
                context.dimension(), context.position(), context.subject(), Map.of(), facts,
                context.observedAt(), context.observer(), Set.of(KnowledgeRecord.ENTITY_OBSERVATION_FACET),
                Set.of(context.channel()), java.util.Optional.empty(), semanticKey);
    }

    /** Stable, registry-independent representation of the visible block-state properties. */
    public static Map<String, String> visibleState(BlockState state) {
        Map<String, String> properties = new java.util.TreeMap<>();
        state.getValues().forEach((property, value) ->
                properties.put(property.getName(), valueName(property, value)));
        return Map.copyOf(properties);
    }

    private static synchronized List<ObservationProvider> orderedProviders() {
        List<ObservationProvider> providers = new ArrayList<>(PROVIDERS.values());
        providers.sort(Comparator.comparingInt(ObservationProvider::priority).reversed()
                .thenComparing(provider -> provider.id().toString()));
        return providers;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String valueName(Property property, Comparable value) {
        return property.getName(value);
    }
}
