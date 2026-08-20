/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.house;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.resource.ItemStackResourceKey;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HouseBuildingLayoutCodecTest {

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        AbstractTownBuilding.CODEC.getClass();
    }

    @Test
    void legacyCodecCanBeDecodedRepeatedlyWithoutAConsumedDefaultStream() {
        var legacy = JsonParser.parseString("{\"pos\":[4,64,-2]}");

        HouseBuilding first = HouseBuilding.CODEC.parse(JsonOps.INSTANCE, legacy)
                .result().orElseThrow();
        HouseBuilding second = HouseBuilding.CODEC.parse(JsonOps.INSTANCE, legacy)
                .result().orElseThrow();

        assertEquals(0, first.getBedCount());
        assertEquals(0, second.getBedCount());
        assertFalse(first.hasEntrance());
        assertFalse(first.getDailyReport().meal().hasData());
        assertThrows(IllegalStateException.class, first::getEntrancePositionLong);
    }

    @Test
    void dailyMealRoundTripPreservesNbtFractionalAmountsAndEmptyServedDays() {
        ItemStack ration = new ItemStack(Items.COOKED_BEEF);
        ration.getOrCreateTag().putString("menuVariant", "test");
        ItemStackResourceKey key = new ItemStackResourceKey(ration);
        HouseBuilding.DailyMeal source = HouseBuilding.DailyMeal.settled(
                17L, Map.of(key, 0.3125));

        Tag encoded = HouseBuilding.DailyMeal.CODEC.encodeStart(NbtOps.INSTANCE, source)
                .result().orElseThrow();
        HouseBuilding.DailyMeal decoded = HouseBuilding.DailyMeal.CODEC
                .parse(NbtOps.INSTANCE, encoded).result().orElseThrow();

        assertTrue(decoded.hasData());
        assertEquals(17L, decoded.settlementDay());
        assertEquals(1, decoded.entries().size());
        assertEquals(0.3125, decoded.entries().get(0).amount(), 1.0e-12);
        assertEquals("test", decoded.entries().get(0).item().toItemStack()
                .getTag().getString("menuVariant"));

        HouseBuilding.DailyMeal empty = HouseBuilding.DailyMeal.settled(18L, Map.of());
        assertTrue(empty.hasData());
        assertTrue(empty.entries().isEmpty());
        assertFalse(HouseBuilding.DailyMeal.EMPTY.hasData());
    }

    @Test
    void layoutRoundTripUsesCompactLongTags() {
        BlockPos firstBed = new BlockPos(-8, 65, 12);
        BlockPos secondBed = new BlockPos(3, 70, -5);
        BlockPos entrance = BlockPos.ZERO;
        HouseBuilding source = new HouseBuilding(new BlockPos(4, 64, -2));
        source.setLayout(List.of(secondBed, firstBed), entrance);

        Tag encoded = HouseBuilding.CODEC.encodeStart(NbtOps.INSTANCE, source)
                .result().orElseThrow();
        CompoundTag compound = (CompoundTag) encoded;
        HouseBuilding decoded = HouseBuilding.CODEC.parse(NbtOps.INSTANCE, encoded)
                .result().orElseThrow();

        assertTrue(compound.contains("bedPositions", Tag.TAG_LONG_ARRAY));
        assertTrue(compound.contains("entrancePosition", Tag.TAG_LONG));
        assertEquals(2, decoded.getBedCount());
        assertEquals(Math.min(firstBed.asLong(), secondBed.asLong()), decoded.getBedPositionLong(0));
        assertEquals(Math.max(firstBed.asLong(), secondBed.asLong()), decoded.getBedPositionLong(1));
        assertTrue(decoded.hasEntrance());
        assertEquals(entrance.asLong(), decoded.getEntrancePositionLong());
    }

    @Test
    void layoutIsCanonicalAndOnlyFiresWhenItsContentsChange() {
        BlockPos firstBed = new BlockPos(9, 66, -4);
        BlockPos secondBed = new BlockPos(-2, 63, 7);
        BlockPos entrance = new BlockPos(1, 64, 1);
        HouseBuilding house = new HouseBuilding(BlockPos.ZERO);
        AtomicInteger changes = new AtomicInteger();
        house.setChangeEventListener(event -> changes.incrementAndGet());

        house.setLayout(List.of(firstBed, secondBed, firstBed), entrance);

        assertEquals(1, changes.get());
        assertEquals(2, house.getBedCount());
        assertEquals(Math.min(firstBed.asLong(), secondBed.asLong()), house.getBedPositionLong(0));
        assertEquals(Math.max(firstBed.asLong(), secondBed.asLong()), house.getBedPositionLong(1));

        house.setLayout(List.of(secondBed, firstBed), entrance);
        assertEquals(1, changes.get());

        house.clearLayout();
        assertEquals(2, changes.get());
        assertEquals(0, house.getBedCount());
        assertFalse(house.hasEntrance());

        house.clearLayout();
        assertEquals(2, changes.get());
    }
}
