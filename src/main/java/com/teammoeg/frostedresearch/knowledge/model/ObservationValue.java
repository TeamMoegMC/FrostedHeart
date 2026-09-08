/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * 明确区分已知、未采集和不适用；没有采集的数值不会成为零。
 */
public record ObservationValue(State state, ValueType type, Optional<String> text, Optional<Double> number) {
    public static final Codec<ObservationValue> CODEC = RecordCodecBuilder.<ObservationValue>create(i -> i.group(
            KnowledgeKey.enumCodec(State.class).fieldOf("state").forGetter(ObservationValue::state),
            KnowledgeKey.enumCodec(ValueType.class).fieldOf("type").forGetter(ObservationValue::type),
            Codec.STRING.optionalFieldOf("text").forGetter(ObservationValue::text),
            Codec.DOUBLE.optionalFieldOf("number").forGetter(ObservationValue::number)
    ).apply(i, ObservationValue::new)).comapFlatMap(v -> v.valid() ? DataResult.success(v)
            : DataResult.error(() -> "Observation value state/type does not agree with its payload"), java.util.function.Function.identity());

    public static ObservationValue known(String value) {
        return new ObservationValue(State.KNOWN, ValueType.STRING, Optional.of(value), Optional.empty());
    }

    public static ObservationValue known(double value) {
        return new ObservationValue(State.KNOWN, ValueType.NUMBER, Optional.empty(), Optional.of(value));
    }

    public static ObservationValue unknown(ValueType type) {
        return new ObservationValue(State.UNKNOWN, type, Optional.empty(), Optional.empty());
    }

    public static ObservationValue notApplicable(ValueType type) {
        return new ObservationValue(State.NOT_APPLICABLE, type, Optional.empty(), Optional.empty());
    }

    private boolean valid() {
        if (state != State.KNOWN) return text.isEmpty() && number.isEmpty();
        return type == ValueType.STRING ? text.isPresent() && number.isEmpty()
                : text.isEmpty() && number.filter(Double::isFinite).isPresent();
    }

    public boolean known() {
        return state == State.KNOWN;
    }

    public enum State {KNOWN, UNKNOWN, NOT_APPLICABLE}

    public enum ValueType {STRING, NUMBER}
}
