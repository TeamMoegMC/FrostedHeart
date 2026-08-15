/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownResidentWorkBuilding;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 城镇级岗位队列及每栋工作建筑的保障目标人数。
 * <p>
 * Town-level staffing queue. List order is the complete player-facing
 * priority relation; no numeric priority is persisted on individual buildings.
 */
public final class TownStaffingPlan {
    public static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("building").forGetter(Entry::building),
            Codec.INT.optionalFieldOf("targetWorkers", 0).forGetter(Entry::targetWorkers)
    ).apply(instance, Entry::new));
    public static final Codec<TownStaffingPlan> CODEC = ENTRY_CODEC.listOf()
            .xmap(TownStaffingPlan::new, TownStaffingPlan::entries);
    public static final TownStaffingPlan EMPTY = new TownStaffingPlan(List.of());

    private final List<Entry> entries;

    public TownStaffingPlan(List<Entry> entries) {
        List<Entry> sanitized = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        for (Entry entry : entries) {
            if (entry != null && seen.add(entry.building())) sanitized.add(entry);
        }
        this.entries = List.copyOf(sanitized);
    }

    public List<Entry> entries() {
        return entries;
    }

    public int target(BlockPos building) {
        return entries.stream()
                .filter(entry -> entry.building().equals(building))
                .mapToInt(Entry::targetWorkers)
                .findFirst()
                .orElse(0);
    }

    /**
     * Removes stale entries and appends missing work buildings. A legacy
     * building's current roster becomes its initial guaranteed target.
     */
    public TownStaffingPlan normalize(Map<BlockPos, AbstractTownBuilding> buildings) {
        Map<BlockPos, ITownResidentWorkBuilding> workBuildings = new LinkedHashMap<>();
        buildings.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof ITownResidentWorkBuilding)
                .sorted(Map.Entry.<BlockPos, AbstractTownBuilding>comparingByKey(
                        TownStaffingPlan::comparePositions))
                .forEach(entry -> workBuildings.put(
                        entry.getKey(), (ITownResidentWorkBuilding) entry.getValue()));

        List<Entry> result = new ArrayList<>();
        Set<BlockPos> present = new HashSet<>();
        for (Entry entry : entries) {
            if (workBuildings.containsKey(entry.building()) && present.add(entry.building())) {
                result.add(entry);
            }
        }
        for (Map.Entry<BlockPos, ITownResidentWorkBuilding> entry : workBuildings.entrySet()) {
            if (present.add(entry.getKey())) {
                result.add(new Entry(entry.getKey(), entry.getValue().getResidentsID().size()));
            }
        }
        return new TownStaffingPlan(result);
    }

    public Optional<TownStaffingPlan> withTarget(
            BlockPos building,
            int target,
            Map<BlockPos, AbstractTownBuilding> buildings
    ) {
        AbstractTownBuilding value = buildings.get(building);
        if (!(value instanceof ITownResidentWorkBuilding workBuilding)) return Optional.empty();
        int bounded = Math.max(0, Math.min(target, Math.max(0, workBuilding.getMaxResidents())));
        TownStaffingPlan normalized = normalize(buildings);
        List<Entry> changed = new ArrayList<>(normalized.entries);
        for (int index = 0; index < changed.size(); index++) {
            if (changed.get(index).building().equals(building)) {
                changed.set(index, new Entry(building, bounded));
                return Optional.of(new TownStaffingPlan(changed));
            }
        }
        return Optional.empty();
    }

    public Optional<TownStaffingPlan> move(
            BlockPos building,
            Optional<BlockPos> before,
            Map<BlockPos, AbstractTownBuilding> buildings
    ) {
        if (!(buildings.get(building) instanceof ITownResidentWorkBuilding)) {
            return Optional.empty();
        }
        TownStaffingPlan normalized = normalize(buildings);
        List<Entry> changed = new ArrayList<>(normalized.entries);
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
        return Optional.of(new TownStaffingPlan(changed));
    }

    private static int comparePositions(BlockPos first, BlockPos second) {
        int x = Integer.compare(first.getX(), second.getX());
        if (x != 0) return x;
        int y = Integer.compare(first.getY(), second.getY());
        return y != 0 ? y : Integer.compare(first.getZ(), second.getZ());
    }

    @Override
    public boolean equals(Object value) {
        return value instanceof TownStaffingPlan other && entries.equals(other.entries);
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }

    @Override
    public String toString() {
        return "TownStaffingPlan" + entries;
    }

    public record Entry(BlockPos building, int targetWorkers) {
        public Entry {
            Objects.requireNonNull(building, "building");
            targetWorkers = Math.max(0, targetWorkers);
        }
    }
}
