/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.logistics;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBuilding;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseBuilding;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TransportStationBuildingCodecTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        AbstractTownBuilding.CODEC.getClass();
    }

    @Test
    void defaultFieldsCanBeDecodedRepeatedly() {
        var legacy = JsonParser.parseString("{\"pos\":[4,64,-2]}");

        TransportStationBuilding first = TransportStationBuilding.CODEC.parse(JsonOps.INSTANCE, legacy)
                .result().orElseThrow();
        TransportStationBuilding second = TransportStationBuilding.CODEC.parse(JsonOps.INSTANCE, legacy)
                .result().orElseThrow();

        assertEquals(0, first.getArea());
        assertEquals(0, second.getVolume());
        assertEquals(0, first.getMaxResidents());
        assertFalse(first.isInitialized());
        assertEquals(List.of(), first.getResidentsID().stream().toList());
    }

    @Test
    void concreteAndPolymorphicCodecsRoundTrip() {
        UUID firstResident = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID secondResident = UUID.fromString("00000000-0000-0000-0000-000000000001");
        OccupiedVolume occupiedVolume = new OccupiedVolume();
        occupiedVolume.add(new BlockPos(4, 64, -2));
        occupiedVolume.add(new BlockPos(5, 64, -2));
        TransportStationBuilding source = new TransportStationBuilding(
                new BlockPos(4, 64, -2), true, false, true,
                occupiedVolume,
                List.of(firstResident, secondResident), 24, 72, 6);

        Tag concreteTag = TransportStationBuilding.CODEC.encodeStart(NbtOps.INSTANCE, source)
                .result().orElseThrow();
        TransportStationBuilding concrete = TransportStationBuilding.CODEC.parse(NbtOps.INSTANCE, concreteTag)
                .result().orElseThrow();
        Tag polymorphicTag = ITownBuilding.CODEC.encodeStart(NbtOps.INSTANCE, source)
                .result().orElseThrow();
        TransportStationBuilding polymorphic = assertInstanceOf(TransportStationBuilding.class,
                ITownBuilding.CODEC.parse(NbtOps.INSTANCE, polymorphicTag).result().orElseThrow());

        assertBuildingEquals(source, concrete);
        assertBuildingEquals(source, polymorphic);
    }

    @Test
    void residentEncodingIsStableRegardlessOfRosterInsertionOrder() {
        UUID firstResident = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID secondResident = UUID.fromString("00000000-0000-0000-0000-000000000002");
        TransportStationBuilding first = new TransportStationBuilding(
                BlockPos.ZERO, false, false, false,
                com.teammoeg.frostedheart.content.town.block.OccupiedVolume.EMPTY,
                List.of(firstResident, secondResident), 0, 0, 0);
        TransportStationBuilding second = new TransportStationBuilding(
                BlockPos.ZERO, false, false, false,
                com.teammoeg.frostedheart.content.town.block.OccupiedVolume.EMPTY,
                List.of(secondResident, firstResident), 0, 0, 0);

        assertEquals(
                TransportStationBuilding.CODEC.encodeStart(NbtOps.INSTANCE, first).result().orElseThrow(),
                TransportStationBuilding.CODEC.encodeStart(NbtOps.INSTANCE, second).result().orElseThrow());
    }

    @Test
    void legacyWarehouseIndexRemainsBoundToWarehouse() {
        var legacyCodec = CodecUtil.dispatch(ITownBuilding.class)
                .type("house", HouseBuilding.class, HouseBuilding.CODEC)
                .type("huntingBase", HuntingBaseBuilding.class, HuntingBaseBuilding.CODEC)
                .type("mine", MineBuilding.class, MineBuilding.CODEC)
                .type("mineBase", MineBaseBuilding.class, MineBaseBuilding.CODEC)
                .type("warehouse", WarehouseBuilding.class, WarehouseBuilding.CODEC)
                .buildByInt();
        Tag legacyTag = legacyCodec.encodeStart(NbtOps.INSTANCE, new WarehouseBuilding(BlockPos.ZERO))
                .result().orElseThrow();

        assertInstanceOf(WarehouseBuilding.class,
                ITownBuilding.CODEC.parse(NbtOps.INSTANCE, legacyTag).result().orElseThrow());
    }

    private static void assertBuildingEquals(TransportStationBuilding expected, TransportStationBuilding actual) {
        assertEquals(expected.getPos(), actual.getPos());
        assertEquals(expected.isInitialized(), actual.isInitialized());
        assertEquals(expected.isOccupiedAreaOverlapped(), actual.isOccupiedAreaOverlapped());
        assertEquals(expected.isStructureValid(), actual.isStructureValid());
        assertEquals(expected.getOccupiedVolume(), actual.getOccupiedVolume());
        assertEquals(expected.getResidentsID(), actual.getResidentsID());
        assertEquals(expected.getArea(), actual.getArea());
        assertEquals(expected.getVolume(), actual.getVolume());
        assertEquals(expected.getMaxResidents(), actual.getMaxResidents());
    }
}
