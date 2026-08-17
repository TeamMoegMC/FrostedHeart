/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.town;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TownHousingPlanTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void legacyTownUsesEmptyMigratableHousingPlanAndDefaultPolicy() {
        TeamTownData decoded = TeamTownData.CODEC.parse(
                        JsonOps.INSTANCE, JsonParser.parseString("{\"name\":\"Legacy\"}"))
                .result().orElseThrow();

        assertEquals(TownHousingPlan.EMPTY, decoded.getHousingPlan());
        assertEquals(TownCareLaw.CLINICAL_TRIAGE, decoded.getPolicyState().careLaw());
    }

    @Test
    void normalizationAddsBetterHousingFirstAndEditsAreBounded() {
        HouseBuilding basic = house(new BlockPos(1, 64, 0), 0.0, 2);
        HouseBuilding decorated = house(new BlockPos(9, 64, 0), 1.0, 4);
        Map<BlockPos, AbstractTownBuilding> buildings = new LinkedHashMap<>();
        buildings.put(basic.getPos(), basic);
        buildings.put(decorated.getPos(), decorated);

        TownHousingPlan plan = TownHousingPlan.EMPTY.normalize(buildings);

        assertEquals(List.of(decorated.getPos(), basic.getPos()), plan.entries().stream()
                .map(TownHousingPlan.Entry::building).toList());
        plan = plan.withGuarantee(decorated.getPos(), 99, buildings).orElseThrow();
        assertEquals(4, plan.guaranteedResidents(decorated.getPos()));
        plan = plan.move(basic.getPos(), Optional.of(decorated.getPos()), buildings)
                .orElseThrow();
        assertEquals(List.of(basic.getPos(), decorated.getPos()), plan.entries().stream()
                .map(TownHousingPlan.Entry::building).toList());
    }

    private static HouseBuilding house(BlockPos pos, double decoration, int capacity) {
        return new HouseBuilding(pos, true, 20, 60, 20, decoration, capacity, 0);
    }
}
