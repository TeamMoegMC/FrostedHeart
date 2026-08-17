/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Extensible town policy state. A domain owns one selected option; dependencies
 * and conflicts belong to the code-side catalogue rather than save structure.
 */
public final class TownPolicyState {
    public static final String RESIDENTIAL_CARE = "residential_care";
    public static final long GLOBAL_COOLDOWN_DAYS = 7L;

    public static final Codec<TownPolicyState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(Codec.STRING, Codec.STRING)
                            .optionalFieldOf("selections", Map.of())
                            .forGetter(TownPolicyState::selections),
                    Codec.unboundedMap(Codec.STRING, Codec.STRING)
                            .optionalFieldOf("pending", Map.of())
                            .forGetter(TownPolicyState::pending),
                    Codec.LONG.optionalFieldOf("changedAtTownDay", -GLOBAL_COOLDOWN_DAYS)
                            .forGetter(TownPolicyState::changedAtTownDay)
            ).apply(instance, TownPolicyState::new));

    public static final TownPolicyState DEFAULT = new TownPolicyState(
            Map.of(RESIDENTIAL_CARE, TownCareLaw.CLINICAL_TRIAGE.id()),
            Map.of(), -GLOBAL_COOLDOWN_DAYS);

    private final Map<String, String> selections;
    private final Map<String, String> pending;
    private final long changedAtTownDay;

    public TownPolicyState(
            Map<String, String> selections,
            Map<String, String> pending,
            long changedAtTownDay
    ) {
        this.selections = sanitizeSelections(selections);
        this.pending = sanitizePending(pending);
        this.changedAtTownDay = changedAtTownDay;
    }

    public Map<String, String> selections() {
        return selections;
    }

    public Map<String, String> pending() {
        return pending;
    }

    public long changedAtTownDay() {
        return changedAtTownDay;
    }

    public TownCareLaw careLaw() {
        return TownCareLaw.fromId(selections.get(RESIDENTIAL_CARE));
    }

    public TownCareLaw displayedCareLaw() {
        return TownCareLaw.fromId(pending.getOrDefault(
                RESIDENTIAL_CARE, selections.get(RESIDENTIAL_CARE)));
    }

    public boolean hasPendingChanges() {
        return !pending.isEmpty();
    }

    public long remainingCooldown(long townDay) {
        return Math.max(0L, changedAtTownDay + GLOBAL_COOLDOWN_DAYS - Math.max(0L, townDay));
    }

    public EditResult requestCareLaw(TownCareLaw law, long townDay) {
        if (law == null) return new EditResult(false, this);
        if (remainingCooldown(townDay) > 0L) return new EditResult(false, this);
        if (law == displayedCareLaw()) return new EditResult(false, this);
        Map<String, String> nextPending = new LinkedHashMap<>(pending);
        nextPending.put(RESIDENTIAL_CARE, law.id());
        return new EditResult(true, new TownPolicyState(
                selections, nextPending, Math.max(0L, townDay)));
    }

    /** Activates every accepted choice at one daily-settlement boundary. */
    public TownPolicyState activatePending() {
        if (pending.isEmpty()) return this;
        Map<String, String> active = new LinkedHashMap<>(selections);
        active.putAll(pending);
        return new TownPolicyState(active, Map.of(), changedAtTownDay);
    }

    private static Map<String, String> sanitizeSelections(Map<String, String> input) {
        Map<String, String> result = new LinkedHashMap<>();
        if (input != null) result.putAll(input);
        result.put(RESIDENTIAL_CARE, TownCareLaw.fromId(
                result.get(RESIDENTIAL_CARE)).id());
        return Map.copyOf(result);
    }

    private static Map<String, String> sanitizePending(Map<String, String> input) {
        Map<String, String> result = new LinkedHashMap<>();
        if (input != null && input.containsKey(RESIDENTIAL_CARE)) {
            result.put(RESIDENTIAL_CARE,
                    TownCareLaw.fromId(input.get(RESIDENTIAL_CARE)).id());
        }
        return Map.copyOf(result);
    }

    @Override
    public boolean equals(Object value) {
        return value instanceof TownPolicyState other
                && selections.equals(other.selections)
                && pending.equals(other.pending)
                && changedAtTownDay == other.changedAtTownDay;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(selections, pending, changedAtTownDay);
    }

    public record EditResult(boolean changed, TownPolicyState state) {
    }
}
