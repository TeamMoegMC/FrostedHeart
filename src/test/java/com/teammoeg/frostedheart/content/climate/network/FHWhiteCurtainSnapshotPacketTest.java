/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.network;

import com.teammoeg.chorda.math.Rect;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.InterpolationClimateEvent;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WhiteCurtainDescriptor;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FHWhiteCurtainSnapshotPacketTest {
    private static ResourceKey<Level> testDimension;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        testDimension = ResourceKey.create(
                Registries.DIMENSION, new ResourceLocation("frostedheart", "snapshot_test"));
    }

    @Test
    void zeroOneEightAndThirtyTwoDescriptorsRoundTrip() {
        for (int count : new int[]{0, 1, 8, 32}) {
            List<WhiteCurtainDescriptor> descriptors = descriptors(count);
            FHWhiteCurtainSnapshotPacket decoded = roundTrip(
                    new FHWhiteCurtainSnapshotPacket(testDimension, 123456L, 98760L, descriptors));
            assertTrue(decoded.validPayload());
            assertEquals(testDimension, decoded.dimension());
            assertEquals(123456L, decoded.climateSeconds());
            assertEquals(98760L, decoded.clockDayTime());
            assertEquals(count, decoded.descriptors().size());
            for (int i = 0; i < count; i++) {
                assertEquals(descriptors.get(i).affectedArea(), decoded.descriptors().get(i).affectedArea());
                assertEquals(descriptors.get(i).moveDirection(), decoded.descriptors().get(i).moveDirection());
            }
        }
    }

    @Test
    void malformedLogicalPayloadIsMarkedInvalid() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeResourceLocation(testDimension.location());
        buffer.writeVarLong(99L);
        buffer.writeVarLong(80L);
        CompoundTag root = new CompoundTag();
        root.putString("curtains", "not a descriptor list");
        buffer.writeNbt(root);

        FHWhiteCurtainSnapshotPacket decoded = new FHWhiteCurtainSnapshotPacket(buffer);

        assertFalse(decoded.validPayload());
        assertTrue(decoded.descriptors().isEmpty());
    }

    @Test
    void encodedDescriptorsStayWithinSparseSnapshotBudget() {
        int one = encodedBytes(new FHWhiteCurtainSnapshotPacket(testDimension, 1L, descriptors(1)));
        int thirtyTwo = encodedBytes(new FHWhiteCurtainSnapshotPacket(testDimension, 1L, descriptors(32)));
        assertTrue(one < 1024, "one descriptor encoded to " + one + " bytes");
        assertTrue(thirtyTwo < 32 * 1024, "32 descriptors encoded to " + thirtyTwo + " bytes");
    }

    private static FHWhiteCurtainSnapshotPacket roundTrip(FHWhiteCurtainSnapshotPacket packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(buffer);
        return new FHWhiteCurtainSnapshotPacket(buffer);
    }

    private static int encodedBytes(FHWhiteCurtainSnapshotPacket packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(buffer);
        return buffer.readableBytes();
    }

    private static List<WhiteCurtainDescriptor> descriptors(int count) {
        List<WhiteCurtainDescriptor> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(new WhiteCurtainDescriptor(new Rect(i * 64, i * -32, 20, 14),
                    Direction.from2DDataValue(i & 3), event(i * 5000L)));
        }
        return result;
    }

    private static InterpolationClimateEvent event(long offset) {
        return new InterpolationClimateEvent(
                1000L + offset, 1100L + offset, -10.0F,
                1300L + offset, -50.0F, 2000L + offset,
                2400L + offset, true, true);
    }
}
