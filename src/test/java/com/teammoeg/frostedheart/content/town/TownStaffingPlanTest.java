/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.teammoeg.frostedheart.content.town.building.AbstractTownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TownStaffingPlanTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void legacyNormalizationUsesExistingRosterAndStableCoordinates() {
        TestWorkBuilding later = building(new BlockPos(9, 64, 0), 5);
        TestWorkBuilding earlier = building(new BlockPos(1, 64, 0), 5);
        earlier.addRosterMember(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        Map<BlockPos, com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding> values =
                new LinkedHashMap<>();
        values.put(later.getPos(), later);
        values.put(earlier.getPos(), earlier);

        TownStaffingPlan normalized = TownStaffingPlan.EMPTY.normalize(values);

        assertEquals(List.of(earlier.getPos(), later.getPos()), normalized.entries().stream()
                .map(TownStaffingPlan.Entry::building).toList());
        assertEquals(1, normalized.target(earlier.getPos()));
        assertEquals(0, normalized.target(later.getPos()));
    }

    @Test
    void oldTownCodecWithoutStaffingFieldUsesAnEmptyMigratablePlan() {
        TeamTownData decoded = TeamTownData.CODEC.parse(
                        JsonOps.INSTANCE, JsonParser.parseString("{\"name\":\"Legacy\"}"))
                .result().orElseThrow();

        assertEquals(TownStaffingPlan.EMPTY, decoded.getStaffingPlan());
        assertEquals(0L, decoded.getTownDay());
    }

    @Test
    void townCodecPreservesPersistentTownDay() {
        TeamTownData source = TeamTownData.CODEC.parse(
                        JsonOps.INSTANCE,
                        JsonParser.parseString("{\"name\":\"Established\",\"townDay\":42}"))
                .result().orElseThrow();
        var encoded = TeamTownData.CODEC.encodeStart(JsonOps.INSTANCE, source)
                .result().orElseThrow();
        TeamTownData decoded = TeamTownData.CODEC.parse(JsonOps.INSTANCE, encoded)
                .result().orElseThrow();

        assertEquals(42L, decoded.getTownDay());
    }

    @Test
    void staffingPlanCodecPreservesQueueAndTargets() {
        TownStaffingPlan source = new TownStaffingPlan(List.of(
                new TownStaffingPlan.Entry(new BlockPos(8, 64, 2), 5),
                new TownStaffingPlan.Entry(new BlockPos(1, 64, 9), 2)));
        var encoded = TownStaffingPlan.CODEC.encodeStart(JsonOps.INSTANCE, source)
                .result().orElseThrow();
        TownStaffingPlan decoded = TownStaffingPlan.CODEC.parse(JsonOps.INSTANCE, encoded)
                .result().orElseThrow();

        assertEquals(source, decoded);
    }

    @Test
    void targetEditIsBoundedAtEditTimeButSurvivesLaterCapacityShrink() {
        TestWorkBuilding building = building(BlockPos.ZERO, 8);
        Map<BlockPos, com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding> values =
                Map.of(building.getPos(), building);
        TownStaffingPlan plan = TownStaffingPlan.EMPTY.normalize(values)
                .withTarget(building.getPos(), 99, values).orElseThrow();

        assertEquals(8, plan.target(building.getPos()));
        building.setMaxResidents(3);
        assertEquals(8, plan.normalize(values).target(building.getPos()));
    }

    @Test
    void moveUsesRelationalBeforeAnchorAndRejectsUnknownAnchor() {
        TestWorkBuilding first = building(new BlockPos(1, 0, 0), 1);
        TestWorkBuilding second = building(new BlockPos(2, 0, 0), 1);
        TestWorkBuilding third = building(new BlockPos(3, 0, 0), 1);
        Map<BlockPos, com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding> values =
                Map.of(first.getPos(), first, second.getPos(), second, third.getPos(), third);
        TownStaffingPlan plan = TownStaffingPlan.EMPTY.normalize(values);

        TownStaffingPlan moved = plan.move(
                third.getPos(), Optional.of(first.getPos()), values).orElseThrow();
        assertEquals(List.of(third.getPos(), first.getPos(), second.getPos()),
                moved.entries().stream().map(TownStaffingPlan.Entry::building).toList());
        assertEquals(Optional.empty(), moved.move(
                first.getPos(), Optional.of(new BlockPos(99, 0, 0)), values));
    }

    private static TestWorkBuilding building(BlockPos pos, int capacity) {
        TestWorkBuilding building = new TestWorkBuilding(pos);
        building.setMaxResidents(capacity);
        return building;
    }

    private static final class TestWorkBuilding extends AbstractTownResidentWorkBuilding {
        private TestWorkBuilding(BlockPos pos) {
            super(pos);
        }

        private void addRosterMember(UUID residentId) {
            residentsID.add(residentId);
        }
        @Override
        public double getResidentScore(Resident resident) {
            return 1;
        }
    }
}
