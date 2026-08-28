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
import com.teammoeg.frostedheart.content.climate.thermal.profile.DependencyOffsetMask;
import com.teammoeg.frostedheart.content.climate.thermal.profile.LocalAirRegionPattern;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolverBlockView;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalResolution;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Forge/Minecraft bridge for geometry that is a pure function of one stored
 * {@link BlockState}. Every non-dynamic state uses the same path, including
 * states supplied by other mods. Dynamic and contextual resolvers are separate
 * registrations and never fall through to this resolver.
 */
public final class StateStaticThermalResolver
        implements ThermalSignatureResolver<BlockState, FluidState> {
    public static final String RESOLVER_ID = "frostedheart:state_static";
    private static final SignatureMetadata GEOMETRY_ONLY_NEUTRAL =
            new SignatureMetadata(0, 0, 0, 0, 0, 0, 0);

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

    @Override
    public String resolverId() {
        return RESOLVER_ID;
    }

    @Override
    public DependencyOffsetMask dependencyMask() {
        return DependencyOffsetMask.SELF_ONLY;
    }

    @Override
    public int maxOutputRegions() {
        return maximumRegions;
    }

    @Override
    public ThermalResolution<ResolvedThermalSignature> resolve(
            ResolverBlockView.Access<BlockState, FluidState> view
    ) {
        Objects.requireNonNull(view, "view");
        ResolverBlockView.Lookup<BlockState, FluidState> self =
                view.lookup(DependencyOffsetMask.SELF);
        if (self.status() != ResolverBlockView.LookupStatus.PRESENT) {
            return ThermalResolution.failure(self.reason());
        }
        return resolvePresent(self.blockState(), self.fluidState());
    }

    /** Convenience entry point for a main-thread census of one captured state pair. */
    public ThermalResolution<ResolvedThermalSignature> resolve(
            BlockState blockState,
            FluidState fluidState
    ) {
        return resolvePresent(
                Objects.requireNonNull(blockState, "blockState"),
                Objects.requireNonNull(fluidState, "fluidState"));
    }

    private ThermalResolution<ResolvedThermalSignature> resolvePresent(
            BlockState blockState,
            FluidState fluidState
    ) {
        if (blockState.is(Blocks.MOVING_PISTON)) {
            return ThermalResolution.unresolved(ThermalResolution.Reason.UNRESOLVED_DYNAMIC);
        }
        if (blockState.getBlock().hasDynamicShape()) {
            return ThermalResolution.unsupported(
                    ThermalResolution.Reason.DYNAMIC_SHAPE_UNSUPPORTED);
        }

        ThermalResolution<List<ConservativeAirGeometry.UnitBox>> blockers =
                resolveBlockers(blockState, fluidState);
        if (!blockers.isResolved()) {
            return ThermalResolution.failure(blockers.reason());
        }

        ConservativeAirGeometry.Resolution geometry = ConservativeAirGeometry.resolve(
                blockers.value(),
                maximumRegions
        );
        if (geometry.status() == ConservativeAirGeometry.Status.CONSERVATIVE_UNSUPPORTED) {
            return ThermalResolution.unsupported(ThermalResolution.Reason.REGION_LIMIT_EXCEEDED);
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
            return ThermalResolution.unsupported(
                    ThermalResolution.Reason.INVALID_RESOLVER_OUTPUT);
        }
        if (metadata == null) {
            return ThermalResolution.unsupported(
                    ThermalResolution.Reason.INVALID_RESOLVER_OUTPUT);
        }

        return ThermalResolution.resolved(metadata.toSignature(toPatterns(geometry)));
    }

    private static ThermalResolution<List<ConservativeAirGeometry.UnitBox>> resolveBlockers(
            BlockState blockState,
            FluidState fluidState
    ) {
        // FluidState#getShape reads neighbors. Full-block occupancy is the
        // SELF_ONLY conservative fallback: it may close air, never create it.
        if (!fluidState.isEmpty()) {
            return ThermalResolution.resolved(
                    List.of(ConservativeAirGeometry.UnitBox.fullBlock()));
        }

        VoxelShape collisionShape;
        try {
            collisionShape = blockState.getCollisionShape(
                    EmptyBlockGetter.INSTANCE,
                    BlockPos.ZERO,
                    CollisionContext.empty()
            );
        } catch (RuntimeException ignored) {
            return ThermalResolution.unsupported(
                    ThermalResolution.Reason.INVALID_RESOLVER_OUTPUT);
        }
        if (collisionShape == null) {
            return ThermalResolution.unsupported(
                    ThermalResolution.Reason.INVALID_RESOLVER_OUTPUT);
        }

        VoxelShapeUnitBoxAdapter.Adaptation adaptation =
                VoxelShapeUnitBoxAdapter.adapt(collisionShape);
        if (adaptation.status() == VoxelShapeUnitBoxAdapter.Status.CONSERVATIVE_UNSUPPORTED) {
            return ThermalResolution.unsupported(
                    ThermalResolution.Reason.INVALID_RESOLVER_OUTPUT);
        }
        return ThermalResolution.resolved(adaptation.blockers());
    }

    private static List<LocalAirRegionPattern> toPatterns(
            ConservativeAirGeometry.Resolution geometry
    ) {
        List<LocalAirRegionPattern> patterns = new ArrayList<>(geometry.components().size());
        for (ConservativeAirGeometry.AirComponent component : geometry.components()) {
            patterns.add(new LocalAirRegionPattern(
                    component.id(),
                    component.microcellMask(),
                    component.negativeXMask(),
                    component.positiveXMask(),
                    component.negativeYMask(),
                    component.positiveYMask(),
                    component.negativeZMask(),
                    component.positiveZMask()
            ));
        }
        return List.copyOf(patterns);
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
            int mediumId,
            int materialProfileId,
            int materialContactPatternId,
            int radiationOcclusionPatternId,
            int sourceProfileId,
            int gateKind,
            int flags
    ) {
        public SignatureMetadata {
            requireId("mediumId", mediumId);
            requireId("materialProfileId", materialProfileId);
            requireId("materialContactPatternId", materialContactPatternId);
            requireId("radiationOcclusionPatternId", radiationOcclusionPatternId);
            requireId("sourceProfileId", sourceProfileId);
            requireId("gateKind", gateKind);
        }

        private ResolvedThermalSignature toSignature(List<LocalAirRegionPattern> airRegions) {
            return new ResolvedThermalSignature(
                    mediumId,
                    materialProfileId,
                    airRegions,
                    materialContactPatternId,
                    radiationOcclusionPatternId,
                    sourceProfileId,
                    gateKind,
                    flags
            );
        }

        private static void requireId(String name, int value) {
            if (value < 0) {
                throw new IllegalArgumentException(name + " must be a non-negative int ID");
            }
        }
    }
}
