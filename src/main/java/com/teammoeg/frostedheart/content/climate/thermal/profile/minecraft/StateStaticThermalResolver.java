/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.VoxelShapeUnitBoxAdapter;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Objects;

/**
 * Forge/Minecraft bridge for geometry that is a pure function of one stored
 * {@link BlockState}. Every non-dynamic state uses the same path, including
 * states supplied by other mods. Dynamic shapes are conservatively unsupported.
 */
public final class StateStaticThermalResolver {
    private final int maximumRegions;

    private StateStaticThermalResolver(int maximumRegions) {
        if (maximumRegions <= 0) {
            throw new IllegalArgumentException("maximumRegions must be positive");
        }
        this.maximumRegions = maximumRegions;
    }

    /**
     * Phase A geometry-only classifier. Zero IDs are explicitly neutral and
     * do not claim water, lava, block material, contact, or radiation physics.
     */
    public static StateStaticThermalResolver geometryOnly(int maximumRegions) {
        return new StateStaticThermalResolver(maximumRegions);
    }

    /** Resolves one state pair, or returns null when it is unsupported. */
    public ResolvedThermalSignature resolve(
            BlockState blockState,
            FluidState fluidState
    ) {
        return resolvePresent(
                Objects.requireNonNull(blockState, "blockState"),
                Objects.requireNonNull(fluidState, "fluidState"));
    }

    private ResolvedThermalSignature resolvePresent(
            BlockState blockState,
            FluidState fluidState
    ) {
        if (blockState.is(Blocks.MOVING_PISTON)) {
            return null;
        }
        if (blockState.getBlock().hasDynamicShape()) {
            return null;
        }

        List<ConservativeAirGeometry.UnitBox> blockers =
                resolveBlockers(blockState, fluidState);
        if (blockers == null) {
            return null;
        }

        ConservativeAirGeometry.Resolution geometry = ConservativeAirGeometry.resolve(
                blockers,
                maximumRegions
        );
        if (geometry.status() == ConservativeAirGeometry.Status.CONSERVATIVE_UNSUPPORTED) {
            return null;
        }

        return new ResolvedThermalSignature(geometry, 0, 0);
    }

    private static List<ConservativeAirGeometry.UnitBox> resolveBlockers(
            BlockState blockState,
            FluidState fluidState
    ) {
        // FluidState#getShape reads neighbors. Full-block occupancy is the
        // SELF_ONLY conservative fallback: it may close air, never create it.
        if (!fluidState.isEmpty()) {
            return List.of(ConservativeAirGeometry.UnitBox.fullBlock());
        }

        VoxelShape collisionShape;
        try {
            collisionShape = blockState.getCollisionShape(
                    EmptyBlockGetter.INSTANCE,
                    BlockPos.ZERO,
                    CollisionContext.empty()
            );
        } catch (RuntimeException ignored) {
            return null;
        }
        if (collisionShape == null) {
            return null;
        }

        VoxelShapeUnitBoxAdapter.Adaptation adaptation =
                VoxelShapeUnitBoxAdapter.adapt(collisionShape);
        if (adaptation.status() == VoxelShapeUnitBoxAdapter.Status.CONSERVATIVE_UNSUPPORTED) {
            return null;
        }
        return adaptation.blockers();
    }
}
