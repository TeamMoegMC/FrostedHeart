/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.observation;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.stream.Collectors;

/** Provider-owned policy for deciding when a new observation updates an existing record. */
@FunctionalInterface
public interface ObservationDeduplication {
    String semanticKey(ResourceLocation kindId, ObservationContext context,
            Map<String, String> stateProperties);

    /** Static blocks only merge when kind, position, subject and visible state all match. */
    static ObservationDeduplication exactBlock() {
        return (kind, context, state) -> kind + "|" + context.dimension() + "|" +
                context.position().getX() + "," + context.position().getY() + "," +
                context.position().getZ() + "|" + context.subject() + "|" + canonicalState(state);
    }

    /**
     * Merges observations of the same subject within a provider-defined three-dimensional cell.
     * The key prefix can preserve an earlier provider's stable semantic keys.
     */
    static ObservationDeduplication cell(String keyPrefix, int xSize, int ySize, int zSize) {
        if (xSize <= 0 || ySize <= 0 || zSize <= 0) {
            throw new IllegalArgumentException("Observation cell sizes must be positive");
        }
        return (kind, context, state) -> keyPrefix + "|" + context.dimension() + "|" +
                Math.floorDiv(context.position().getX(), xSize) + "," +
                Math.floorDiv(context.position().getY(), ySize) + "," +
                Math.floorDiv(context.position().getZ(), zSize) + "|" + context.subject();
    }

    private static String canonicalState(Map<String, String> state) {
        return state.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(",", "[", "]"));
    }
}
