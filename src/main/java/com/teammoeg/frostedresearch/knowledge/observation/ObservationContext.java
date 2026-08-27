/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.observation;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Extensible public context captured around one observation.
 *
 * <p>The subject identity and server position always remain available for validation and semantic
 * matching. {@code retainedFields} controls which contextual facts are kept in the public archive.</p>
 */
public record ObservationContext(TargetType targetType, ResourceLocation dimension, BlockPos position,
        ResourceLocation subject, BlockState state, long observedAt, long dayTime, UUID observer,
        ResourceLocation channel, ResourceLocation biome, Weather weather,
        Set<Field> retainedFields, Map<String, String> measurements) {
    public ObservationContext {
        Objects.requireNonNull(targetType);
        Objects.requireNonNull(dimension);
        position = position.immutable();
        Objects.requireNonNull(subject);
        Objects.requireNonNull(state);
        Objects.requireNonNull(observer);
        Objects.requireNonNull(channel);
        Objects.requireNonNull(biome);
        Objects.requireNonNull(weather);
        retainedFields = Set.copyOf(retainedFields);
        measurements = Map.copyOf(measurements);
    }

    /** Compatibility constructor for existing block providers and tests. */
    public ObservationContext(ResourceLocation dimension, BlockPos position,
            ResourceLocation subject, BlockState state, long observedAt, UUID observer,
            ResourceLocation channel) {
        this(TargetType.BLOCK, dimension, position, subject, state, observedAt,
                observedAt % 24000L, observer, channel,
                new ResourceLocation("minecraft", "the_void"), Weather.CLEAR,
                Set.of(Field.LOCATION, Field.TIME, Field.BLOCK_STATE), Map.of());
    }

    /** Public, player-selected context facts. Adding a field does not require a record schema change. */
    public Map<String, String> publicFacts() {
        Map<String, String> facts = new LinkedHashMap<>();
        facts.put("target", targetType.name().toLowerCase());
        if (retainedFields.contains(Field.LOCATION)) {
            facts.put("dimension", dimension.toString());
            facts.put("position", position.getX() + "," + position.getY() + "," + position.getZ());
        }
        if (retainedFields.contains(Field.TIME)) {
            facts.put("game_time", Long.toString(observedAt));
            facts.put("day_time", Long.toString(dayTime));
            facts.put("time_period", timePeriod(dayTime));
        }
        if (retainedFields.contains(Field.BIOME)) facts.put("biome", biome.toString());
        if (retainedFields.contains(Field.WEATHER)) facts.put("weather", weather.name().toLowerCase());
        if (retainedFields.contains(Field.TEMPERATURE) && measurements.containsKey("temperature")) {
            facts.put("temperature", measurements.get("temperature"));
        }
        measurements.forEach((key, value) -> {
            if (!"temperature".equals(key)) facts.put("measurement." + key, value);
        });
        return Map.copyOf(facts);
    }

    private static String timePeriod(long dayTime) {
        long time = Math.floorMod(dayTime, 24000L);
        if (time < 1000 || time >= 23000) return "dawn";
        if (time < 12000) return "day";
        if (time < 14000) return "dusk";
        return "night";
    }

    public enum TargetType { BLOCK, ENTITY }

    public enum Weather { CLEAR, RAIN, THUNDER }

    public enum Field { LOCATION, TIME, BIOME, WEATHER, BLOCK_STATE, TEMPERATURE }
}
