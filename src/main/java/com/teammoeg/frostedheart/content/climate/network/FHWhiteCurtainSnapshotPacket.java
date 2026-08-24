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

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WhiteCurtainDescriptor;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.render.weather.ClientWeatherState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/** Atomic, low-frequency replacement of one dimension's sparse white-curtain authority. */
public final class FHWhiteCurtainSnapshotPacket implements CMessage {
    private static final String CURTAINS_KEY = "curtains";
    private static final int MAX_DECODE_DIAGNOSTICS = 4;
    private static final AtomicInteger DECODE_DIAGNOSTICS = new AtomicInteger();

    private final ResourceKey<Level> dimension;
    private final long climateSeconds;
    private final long clockDayTime;
    private final List<WhiteCurtainDescriptor> descriptors;
    private final boolean validPayload;

    public FHWhiteCurtainSnapshotPacket() {
        this(Level.OVERWORLD, 0L, 0L, List.of(), true);
    }

    public FHWhiteCurtainSnapshotPacket(WorldClimate climate, ServerLevel level) {
        this(level.dimension(), climate == null ? 0L : climate.getSec(),
                climate == null ? 0L : climate.getClockDayTime(),
                climate == null ? List.of() : climate.getWhiteCurtainDescriptors(), true);
    }

    public FHWhiteCurtainSnapshotPacket(ResourceKey<Level> dimension, long climateSeconds,
                                        List<WhiteCurtainDescriptor> descriptors) {
        this(dimension, climateSeconds, 0L, List.copyOf(descriptors), true);
    }

    public FHWhiteCurtainSnapshotPacket(ResourceKey<Level> dimension, long climateSeconds,
                                        long clockDayTime, List<WhiteCurtainDescriptor> descriptors) {
        this(dimension, climateSeconds, clockDayTime, List.copyOf(descriptors), true);
    }

    private FHWhiteCurtainSnapshotPacket(ResourceKey<Level> dimension, long climateSeconds,
                                         long clockDayTime, List<WhiteCurtainDescriptor> descriptors,
                                         boolean validPayload) {
        this.dimension = dimension;
        this.climateSeconds = climateSeconds;
        this.clockDayTime = clockDayTime;
        this.descriptors = descriptors;
        this.validPayload = validPayload;
    }

    FHWhiteCurtainSnapshotPacket(FriendlyByteBuf buffer) {
        ResourceKey<Level> decodedDimension = Level.OVERWORLD;
        long decodedSeconds = 0L;
        long decodedDayTime = 0L;
        List<WhiteCurtainDescriptor> decodedDescriptors = List.of();
        boolean decoded = false;
        try {
            ResourceLocation location = buffer.readResourceLocation();
            decodedDimension = ResourceKey.create(Registries.DIMENSION, location);
            decodedSeconds = buffer.readVarLong();
            decodedDayTime = buffer.readVarLong();
            CompoundTag root = buffer.readNbt();
            if (root != null && root.contains(CURTAINS_KEY)) {
                List<WhiteCurtainDescriptor> result = WhiteCurtainDescriptor.LIST_CODEC
                        .parse(NbtOps.INSTANCE, root.get(CURTAINS_KEY)).result().orElse(null);
                if (result != null) {
                    decodedDescriptors = List.copyOf(result);
                    decoded = true;
                }
            }
        } catch (RuntimeException exception) {
            diagnoseDecode(exception.getMessage());
        }
        dimension = decodedDimension;
        climateSeconds = decodedSeconds;
        clockDayTime = decodedDayTime;
        descriptors = decodedDescriptors;
        validPayload = decoded;
        if (!decoded) {
            diagnoseDecode("invalid logical payload");
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(dimension.location());
        buffer.writeVarLong(climateSeconds);
        buffer.writeVarLong(clockDayTime);
        CompoundTag root = new CompoundTag();
        WhiteCurtainDescriptor.LIST_CODEC.encodeStart(NbtOps.INSTANCE, descriptors)
                .resultOrPartial(message -> FHMain.LOGGER.error("Could not encode white curtain snapshot: {}", message))
                .ifPresent(tag -> root.put(CURTAINS_KEY, tag));
        buffer.writeNbt(root);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (!validPayload) {
                return;
            }
            Level level = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getWorld);
            ResourceKey<Level> loadedDimension = level == null ? null : level.dimension();
            long dayTime = level == null ? 0L : level.getDayTime();
            // Decode -> client thread -> dimension check -> one atomic state replacement.
            ClientWeatherState.INSTANCE.receiveSnapshot(
                    dimension, climateSeconds, clockDayTime, descriptors, loadedDimension, dayTime);
        });
        context.get().setPacketHandled(true);
    }

    private static void diagnoseDecode(String detail) {
        if (DECODE_DIAGNOSTICS.getAndIncrement() < MAX_DECODE_DIAGNOSTICS) {
            FHMain.LOGGER.warn("Ignored malformed white curtain snapshot: {}", detail);
        }
    }

    ResourceKey<Level> dimension() {
        return dimension;
    }

    long climateSeconds() {
        return climateSeconds;
    }

    long clockDayTime() {
        return clockDayTime;
    }

    List<WhiteCurtainDescriptor> descriptors() {
        return descriptors;
    }

    boolean validPayload() {
        return validPayload;
    }
}
