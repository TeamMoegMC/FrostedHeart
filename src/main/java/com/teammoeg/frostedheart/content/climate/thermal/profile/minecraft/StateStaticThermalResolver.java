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
    private static final SignatureMetadata GEOMETRY_ONLY_NEUTRAL =
            new SignatureMetadata(0, 0);

    private final int maximumRegions;
    private final StateStaticProfileClassifier profileClassifier;
    private final StateStaticGeometryProfileClassifier geometryProfileClassifier;

    public StateStaticThermalResolver(
            int maximumRegions,
            StateStaticProfileClassifier profileClassifier
    ) {
        if (maximumRegions <= 0) {
            throw new IllegalArgumentException("maximumRegions must be positive");
        }
        this.maximumRegions = maximumRegions;
        this.profileClassifier = Objects.requireNonNull(profileClassifier, "profileClassifier");
        this.geometryProfileClassifier = null;
    }

    private StateStaticThermalResolver(
            int maximumRegions,
            StateStaticGeometryProfileClassifier geometryProfileClassifier
    ) {
        if (maximumRegions <= 0) {
            throw new IllegalArgumentException("maximumRegions must be positive");
        }
        this.maximumRegions = maximumRegions;
        this.profileClassifier = null;
        this.geometryProfileClassifier = Objects.requireNonNull(
                geometryProfileClassifier, "geometryProfileClassifier");
    }

    /**
     * Phase A geometry-only classifier. Zero IDs are explicitly neutral and
     * do not claim water, lava, block material, contact, or radiation physics.
     */
    public static StateStaticThermalResolver geometryOnly(int maximumRegions) {
        return new StateStaticThermalResolver(
                maximumRegions,
                (blockState, fluidState) -> GEOMETRY_ONLY_NEUTRAL
        );
    }

    /**
     * Builds a state-static resolver whose metadata classifier can reuse the
     * conservative material mask already produced by the geometry pass.
     */
    public static StateStaticThermalResolver withMaterialMask(
            int maximumRegions,
            StateStaticGeometryProfileClassifier profileClassifier
    ) {
        return new StateStaticThermalResolver(maximumRegions, profileClassifier);
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

        SignatureMetadata metadata;
        try {
            metadata = geometryProfileClassifier == null
                    ? profileClassifier.classify(blockState, fluidState)
                    : geometryProfileClassifier.classify(
                            blockState,
                            fluidState,
                            ~geometry.provenAirMicrocellMask());
        } catch (RuntimeException ignored) {
            return null;
        }
        if (metadata == null) {
            return null;
        }

        return metadata.toSignature(geometry);
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

    /** Supplies non-geometric signature channels without owning shape classification. */
    @FunctionalInterface
    public interface StateStaticProfileClassifier {
        SignatureMetadata classify(BlockState blockState, FluidState fluidState);
    }

    /** Supplies metadata using the geometry pass's block-local material mask. */
    @FunctionalInterface
    public interface StateStaticGeometryProfileClassifier {
        SignatureMetadata classify(
                BlockState blockState,
                FluidState fluidState,
                long materialMicrocellMask);
    }

    /** Immutable non-geometric fields used to finish one resolved signature. */
    public record SignatureMetadata(
            int materialProfileId,
            int materialContactPatternId
    ) {
        public SignatureMetadata {
            requireId("materialProfileId", materialProfileId);
            requireId("materialContactPatternId", materialContactPatternId);
        }

        private ResolvedThermalSignature toSignature(
                ConservativeAirGeometry.Resolution airGeometry
        ) {
            return new ResolvedThermalSignature(
                    airGeometry,
                    materialProfileId,
                    materialContactPatternId);
        }

        private static void requireId(String name, int value) {
            if (value < 0) {
                throw new IllegalArgumentException(name + " must be a non-negative int ID");
            }
        }
    }
}
