/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.gamedata.climate;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.teammoeg.chorda.math.Rect;
import net.minecraft.core.Direction;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhiteCurtainDescriptorCodecTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void legacyFixtureDecodesWithoutFieldMigration() {
        JsonElement legacy = JsonParser.parseString("""
                {
                  "area": {"x": 10, "y": -7, "w": 20, "h": 14},
                  "move": "south",
                  "climate": {
                    "type": "intrp",
                    "startTime": 1000,
                    "peakTime": 1100,
                    "peakTemp": -10.0,
                    "bottomTime": 1300,
                    "bottomTemp": -50.0,
                    "endTime": 2000,
                    "calmEndTime": 2400,
                    "isCold": true,
                    "isBlizzard": true
                  }
                }
                """);

        WhiteCurtainInfo decoded = WhiteCurtainInfo.CODEC.parse(JsonOps.INSTANCE, legacy)
                .result().orElseThrow();

        assertEquals(new Rect(10, -7, 20, 14), decoded.descriptor().affectedArea());
        assertEquals(Direction.SOUTH, decoded.descriptor().moveDirection());
        JsonElement encoded = WhiteCurtainInfo.CODEC.encodeStart(JsonOps.INSTANCE, decoded)
                .result().orElseThrow();
        assertTrue(encoded.getAsJsonObject().has("area"));
        assertTrue(encoded.getAsJsonObject().has("move"));
        assertTrue(encoded.getAsJsonObject().has("climate"));
        assertEquals(3, encoded.getAsJsonObject().size());
    }

    @Test
    void descriptorListRoundTripPreservesOrder() {
        ClimateEvent event = event();
        List<WhiteCurtainDescriptor> source = List.of(
                new WhiteCurtainDescriptor(new Rect(1, 2, 3, 4), Direction.NORTH, event),
                new WhiteCurtainDescriptor(new Rect(-5, 6, 7, 8), Direction.EAST, event));

        JsonElement encoded = WhiteCurtainDescriptor.LIST_CODEC.encodeStart(JsonOps.INSTANCE, source)
                .result().orElseThrow();
        List<WhiteCurtainDescriptor> decoded = WhiteCurtainDescriptor.LIST_CODEC
                .parse(JsonOps.INSTANCE, encoded).result().orElseThrow();

        assertEquals(source.size(), decoded.size());
        assertEquals(source.get(0).affectedArea(), decoded.get(0).affectedArea());
        assertEquals(source.get(0).moveDirection(), decoded.get(0).moveDirection());
        assertEquals(source.get(1).affectedArea(), decoded.get(1).affectedArea());
        assertEquals(source.get(1).moveDirection(), decoded.get(1).moveDirection());
    }

    static InterpolationClimateEvent event() {
        return new InterpolationClimateEvent(
                1000L, 1100L, -10.0F, 1300L, -50.0F,
                2000L, 2400L, true, true);
    }
}
