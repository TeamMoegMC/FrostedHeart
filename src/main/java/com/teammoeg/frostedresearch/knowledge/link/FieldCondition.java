/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.link;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedresearch.knowledge.model.*;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * 有类型的单字段条件；未知和不适用不会满足普通比较（包括不等于）。
 */
public record FieldCondition(String field, Operator operator, Optional<ObservationValue> value,
                             Optional<ResourceLocation> tag) {
    public enum Operator {EQ, NE, LT, LTE, GT, GTE, EXISTS, UNKNOWN, NOT_APPLICABLE, TAG}

    public static final Codec<FieldCondition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("field").forGetter(FieldCondition::field),
            KnowledgeKey.enumCodec(Operator.class).fieldOf("operator").forGetter(FieldCondition::operator),
            ObservationValue.CODEC.optionalFieldOf("value").forGetter(FieldCondition::value),
            ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(FieldCondition::tag)
    ).apply(i, FieldCondition::new));

    public boolean matches(Observation observation, TagLookup tags) {
        ObservationValue actual = observation.value(field);
        return switch (operator) {
            case EXISTS -> actual.known();
            case UNKNOWN -> actual.state() == ObservationValue.State.UNKNOWN;
            case NOT_APPLICABLE -> actual.state() == ObservationValue.State.NOT_APPLICABLE;
            case TAG -> actual.known() && actual.text().isPresent() && tag.isPresent()
                    && ResourceLocation.tryParse(actual.text().get()) != null
                    && tags.contains(observation.type(), field, new ResourceLocation(actual.text().get()), tag.get());
            default -> value.isPresent() && compare(actual, value.get(), operator);
        };
    }

    public void validate(Optional<Observation.Type> type, List<String> errors) {
        var expected = ObservationFields.type(field);
        if (expected.isEmpty()) {
            errors.add("Unknown observation field " + field);
            return;
        }
        if (type.isPresent() && !ObservationFields.applies(field, type.get()) && operator != Operator.NOT_APPLICABLE)
            errors.add(field + " is not applicable to " + type.get());
        switch (operator) {
            case EXISTS, UNKNOWN, NOT_APPLICABLE -> {
                if (value.isPresent() || tag.isPresent()) errors.add(operator + " must not declare value/tag");
            }
            case TAG -> {
                if (!field.equals("object") && !field.equals("biome"))
                    errors.add("Tags apply to object or biome fields");
                if (tag.isEmpty() || value.isPresent()) errors.add("Tag condition requires tag only");
                if (field.equals("object") && type.isEmpty())
                    errors.add("Object tag condition requires observation_type");
            }
            default -> {
                if (value.isEmpty() || !value.get().known() || value.get().type() != expected.get())
                    errors.add("Missing or mistyped comparison value for " + field);
                if (tag.isPresent()) errors.add("Comparison must not declare tag");
                if (operator != Operator.EQ && operator != Operator.NE && expected.get() != ObservationValue.ValueType.NUMBER)
                    errors.add("Ordered comparison requires a numeric field: " + field);
            }
        }
    }

    public static boolean compare(ObservationValue a, ObservationValue b, Operator op) {
        if (!a.known() || !b.known() || a.type() != b.type()) return false;
        if (a.type() == ObservationValue.ValueType.STRING)
            return op == Operator.EQ ? a.text().equals(b.text()) : op == Operator.NE && !a.text().equals(b.text());
        if (a.number().isEmpty() || b.number().isEmpty()) return false;
        double left = a.number().get(), right = b.number().get();
        if (!Double.isFinite(left) || !Double.isFinite(right)) return false;
        return switch (op) {
            case EQ -> left == right;
            case NE -> left != right;
            case LT -> left < right;
            case LTE -> left <= right;
            case GT -> left > right;
            case GTE -> left >= right;
            default -> false;
        };
    }
}
