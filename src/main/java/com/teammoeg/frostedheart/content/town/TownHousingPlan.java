/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Ordered residential-care queue plus guaranteed-ration targets. */
public final class TownHousingPlan {
    public static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockPos.CODEC.fieldOf("building").forGetter(Entry::building),
                    Codec.INT.optionalFieldOf("guaranteedResidents", 0)
                            .forGetter(Entry::guaranteedResidents)
            ).apply(instance, Entry::new));
    public static final Codec<TownHousingPlan> CODEC = ENTRY_CODEC.listOf()
            .xmap(TownHousingPlan::new, TownHousingPlan::entries);
    public static final TownHousingPlan EMPTY = new TownHousingPlan(List.of());

    private final List<Entry> entries;

    public TownHousingPlan(List<Entry> entries) {
        List<Entry> sanitized = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        if (entries != null) {
            for (Entry entry : entries) {
                if (entry != null && seen.add(entry.building())) sanitized.add(entry);
            }
        }
        this.entries = List.copyOf(sanitized);
    }

    public List<Entry> entries() {
        return entries;
    }

    public int guaranteedResidents(BlockPos building) {
        return entries.stream()
                .filter(entry -> entry.building().equals(building))
                .mapToInt(Entry::guaranteedResidents)
                .findFirst().orElse(0);
    }

    public TownHousingPlan normalize(Map<BlockPos, AbstractTownBuilding> buildings) {
        List<Entry> result = new ArrayList<>();
        Set<BlockPos> present = new HashSet<>();
        for (Entry entry : entries) {
            if (buildings.get(entry.building()) instanceof HouseBuilding
                    && present.add(entry.building())) {
                result.add(entry);
            }
        }
        buildings.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof HouseBuilding)
                .filter(entry -> !present.contains(entry.getKey()))
                .sorted((first, second) -> {
                    HouseBuilding a = (HouseBuilding) first.getValue();
                    HouseBuilding b = (HouseBuilding) second.getValue();
                    int rating = Double.compare(b.getRating(), a.getRating());
                    return rating != 0 ? rating : comparePositions(first.getKey(), second.getKey());
                })
                .forEach(entry -> {
                    present.add(entry.getKey());
                    result.add(new Entry(entry.getKey(), 0));
                });
        return new TownHousingPlan(result);
    }

    public Optional<TownHousingPlan> withGuarantee(
            BlockPos building,
            int target,
            Map<BlockPos, AbstractTownBuilding> buildings
    ) {
        if (!(buildings.get(building) instanceof HouseBuilding house)) {
            return Optional.empty();
        }
        int bounded = Math.max(0, Math.min(target, Math.max(0, house.getMaxResidents())));
        List<Entry> changed = new ArrayList<>(normalize(buildings).entries);
        for (int index = 0; index < changed.size(); index++) {
            if (changed.get(index).building().equals(building)) {
                changed.set(index, new Entry(building, bounded));
                return Optional.of(new TownHousingPlan(changed));
            }
        }
        return Optional.empty();
    }

    public Optional<TownHousingPlan> move(
            BlockPos building,
            Optional<BlockPos> before,
            Map<BlockPos, AbstractTownBuilding> buildings
    ) {
        if (!(buildings.get(building) instanceof HouseBuilding)) return Optional.empty();
        List<Entry> changed = new ArrayList<>(normalize(buildings).entries);
        Entry moved = null;
        for (int index = 0; index < changed.size(); index++) {
            if (changed.get(index).building().equals(building)) {
                moved = changed.remove(index);
                break;
            }
        }
        if (moved == null) return Optional.empty();
        int insertion = changed.size();
        if (before.isPresent()) {
            insertion = -1;
            for (int index = 0; index < changed.size(); index++) {
                if (changed.get(index).building().equals(before.get())) {
                    insertion = index;
                    break;
                }
            }
            if (insertion < 0) return Optional.empty();
        }
        changed.add(insertion, moved);
        return Optional.of(new TownHousingPlan(changed));
    }

    private static int comparePositions(BlockPos first, BlockPos second) {
        int x = Integer.compare(first.getX(), second.getX());
        if (x != 0) return x;
        int y = Integer.compare(first.getY(), second.getY());
        return y != 0 ? y : Integer.compare(first.getZ(), second.getZ());
    }

    @Override
    public boolean equals(Object value) {
        return value instanceof TownHousingPlan other && entries.equals(other.entries);
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }

    public record Entry(BlockPos building, int guaranteedResidents) {
        public Entry {
            Objects.requireNonNull(building, "building");
            guaranteedResidents = Math.max(0, guaranteedResidents);
        }
    }
}
