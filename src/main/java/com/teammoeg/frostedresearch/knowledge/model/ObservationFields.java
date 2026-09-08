/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.model;

import java.util.Optional;
import java.util.Set;

import static com.teammoeg.frostedresearch.knowledge.model.ObservationValue.ValueType;

/**
 * 字段类型及适用性。context.* 是离散字符串，field.* 是带自定义标识的数值快照。
 */
public final class ObservationFields {
    private static final Set<String> NUMBERS = Set.of("x", "y", "z", "time", "year", "month", "day", "hour", "temperature", "source_position.x", "source_position.y", "source_position.z");
    private static final Set<String> STRINGS = Set.of("object", "type", "record_id", "dimension", "biome", "climate", "temperature_type", "time_period", "entity_instance", "item_block", "source_kind", "original_observer", "organization", "source_dimension");
    private static final Set<String> UNIVERSAL = Set.of("object", "type", "record_id", "source_kind", "original_observer", "organization", "source_dimension");

    private ObservationFields() {
    }

    public static Optional<ValueType> type(String field) {
        if (NUMBERS.contains(field) || extension(field, "field.")) return Optional.of(ValueType.NUMBER);
        if (STRINGS.contains(field) || extension(field, "context.") || extension(field, "block_state.") || extension(field, "item."))
            return Optional.of(ValueType.STRING);
        return Optional.empty();
    }

    private static boolean extension(String field, String prefix) {
        return field.startsWith(prefix) && field.length() > prefix.length();
    }

    public static boolean applies(String field, Observation.Type type) {
        if (UNIVERSAL.contains(field) || field.startsWith("source_position.")) return true;
        if (field.equals("entity_instance")) return type == Observation.Type.ENTITY;
        if (field.startsWith("block_state.")) return type == Observation.Type.BLOCK;
        if (field.equals("item_block") || field.startsWith("item.")) return type == Observation.Type.ITEM;
        return type != Observation.Type.ITEM;
    }
}
