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

import com.teammoeg.frostedheart.content.climate.thermal.profile.DependencyOffsetMask;
import com.teammoeg.frostedheart.content.climate.thermal.profile.LocalAirRegionPattern;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolverBlockView;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalResolution;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureResolution;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureResolver;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalSignatureResolverDispatcherTest {
    private static final DependencyOffsetMask.Offset EAST =
            new DependencyOffsetMask.Offset(1, 0, 0);
    private static final DependencyOffsetMask.Offset NORTH =
            new DependencyOffsetMask.Offset(0, 0, -1);

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void dispatchPriorityIsExplicitThenGenericThenContextualThenUnsupported() {
        SyntheticResolver contextual = new SyntheticResolver(
                "test:shared_context",
                DependencyOffsetMask.SELF_ONLY,
                1,
                view -> ThermalResolution.resolved(signature(202))
        );
        ThermalSignatureResolverDispatcher dispatcher =
                ThermalSignatureResolverDispatcher.builder(
                                StateStaticThermalResolver.geometryOnly(8))
                        .registerContextual(Blocks.STONE, contextual)
                        .registerContextual(Blocks.OAK_SLAB, contextual)
                        .registerContextual(Blocks.BAMBOO, contextual)
                        .registerExplicitProfile(Blocks.STONE, "test:stone_profile", signature(101))
                        .build();

        ThermalSignatureResolverDispatcher.DispatchPlan explicit =
                dispatcher.plan(Blocks.STONE.defaultBlockState());
        ThermalSignatureResolverDispatcher.DispatchPlan generic =
                dispatcher.plan(Blocks.OAK_SLAB.defaultBlockState());
        ThermalSignatureResolverDispatcher.DispatchPlan contextualPlan =
                dispatcher.plan(Blocks.BAMBOO.defaultBlockState());
        ThermalSignatureResolverDispatcher empty =
                ThermalSignatureResolverDispatcher.builder(
                        StateStaticThermalResolver.geometryOnly(8)).build();
        ThermalSignatureResolverDispatcher.DispatchPlan unsupported =
                empty.plan(Blocks.BAMBOO.defaultBlockState());

        assertEquals(ThermalSignatureResolverDispatcher.Route.EXPLICIT_OVERRIDE,
                explicit.route());
        assertEquals("test:stone_profile", explicit.resolverId());
        assertEquals(ThermalSignatureResolverDispatcher.Route.GENERIC_STATE_STATIC,
                generic.route());
        assertEquals(StateStaticThermalResolver.RESOLVER_ID, generic.resolverId());
        assertEquals(ThermalSignatureResolverDispatcher.Route.CONTEXTUAL,
                contextualPlan.route());
        assertEquals(ThermalSignatureResolverDispatcher.Route.UNREGISTERED,
                unsupported.route());

        ThermalSignatureRegistry.Builder signatures = ThermalSignatureRegistry.builder();
        ThermalSignatureResolution explicitResolution = intern(
                explicit,
                selfView(explicit.dependencyMask(), Blocks.STONE.defaultBlockState()),
                signatures);
        intern(generic,
                selfView(generic.dependencyMask(), Blocks.OAK_SLAB.defaultBlockState()),
                signatures);
        ThermalSignatureResolution unsupportedResolution = intern(
                unsupported,
                selfView(unsupported.dependencyMask(), Blocks.BAMBOO.defaultBlockState()),
                signatures);

        assertEquals(101, signatures.build().signature(
                explicitResolution.signatureId()).orElseThrow().flags());
        assertEquals(0, contextual.invocations());
        assertEquals(ThermalResolution.Status.CONSERVATIVE_UNSUPPORTED,
                unsupportedResolution.status());
        assertEquals(ThermalResolution.Reason.NOT_REGISTERED,
                unsupportedResolution.reason());
    }

    @Test
    void movingPistonHardExclusionPrecedesExplicitRegistration() {
        ThermalSignatureResolverDispatcher dispatcher =
                ThermalSignatureResolverDispatcher.builder(
                                StateStaticThermalResolver.geometryOnly(8))
                        .registerExplicitProfile(
                                Blocks.MOVING_PISTON,
                                "test:forbidden_moving_profile",
                                signature(505))
                        .registerContextual(
                                Blocks.MOVING_PISTON,
                                resolver("test:forbidden_moving_context",
                                        DependencyOffsetMask.NEIGHBOR_26, 1))
                        .build();
        ThermalSignatureResolverDispatcher.DispatchPlan plan =
                dispatcher.plan(Blocks.MOVING_PISTON.defaultBlockState());
        ThermalSignatureResolution result = intern(
                plan,
                selfView(plan.dependencyMask(), Blocks.MOVING_PISTON.defaultBlockState()),
                ThermalSignatureRegistry.builder());

        assertEquals(ThermalSignatureResolverDispatcher.Route.UNREGISTERED, plan.route());
        assertEquals(ThermalResolution.Status.UNRESOLVED, result.status());
        assertEquals(ThermalResolution.Reason.UNRESOLVED_DYNAMIC, result.reason());
        assertEquals(ThermalSignatureResolution.NO_SIGNATURE_ID, result.signatureId());
    }

    @Test
    void registrationFreezesCanonicalIdsAndRejectsDuplicateBindings() {
        SyntheticResolver contextual = resolver(
                "test:z_context", DependencyOffsetMask.NEIGHBOR_6, 1);
        ThermalSignatureResolverDispatcher.Builder builder =
                ThermalSignatureResolverDispatcher.builder(
                                StateStaticThermalResolver.geometryOnly(8))
                        .registerContextual(Blocks.BAMBOO, contextual)
                        .registerExplicitProfile(Blocks.STONE, "test:a_profile", signature(1));

        assertThrows(IllegalArgumentException.class,
                () -> builder.registerContextual(Blocks.BAMBOO, contextual));
        assertThrows(IllegalArgumentException.class,
                () -> builder.registerContextual(
                        Blocks.BAMBOO_SAPLING,
                        resolver("test:z_context", DependencyOffsetMask.SELF_ONLY, 1)));
        builder.registerContextual(Blocks.BAMBOO_SAPLING, contextual);

        assertThrows(IllegalArgumentException.class,
                () -> ThermalSignatureResolverDispatcher.builder(
                                StateStaticThermalResolver.geometryOnly(8))
                        .registerContextual(
                                Blocks.BAMBOO,
                                resolver("not_namespaced", DependencyOffsetMask.SELF_ONLY, 1)));

        ThermalSignatureResolverDispatcher frozen = builder.build();
        builder.registerExplicitProfile(Blocks.DIRT, "test:later_profile", signature(2));

        assertEquals(ThermalSignatureResolverDispatcher.Route.GENERIC_STATE_STATIC,
                frozen.plan(Blocks.DIRT.defaultBlockState()).route());
    }

    @Test
    void registrationRejectsInvalidBoundsAndRuntimeEnforcesRegionDeclaration() {
        assertThrows(IllegalArgumentException.class,
                () -> new DependencyOffsetMask.Offset(2, 0, 0));
        assertThrows(NullPointerException.class,
                () -> ThermalSignatureResolverDispatcher.builder(
                                StateStaticThermalResolver.geometryOnly(8))
                        .registerContextual(
                                Blocks.BAMBOO,
                                resolver("test:null_mask", null, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> ThermalSignatureResolverDispatcher.builder(
                                StateStaticThermalResolver.geometryOnly(8))
                        .registerContextual(
                                Blocks.BAMBOO,
                                resolver("test:negative_regions",
                                        DependencyOffsetMask.SELF_ONLY, -1)));

        SyntheticResolver bounded = resolver(
                "test:bounded_27", DependencyOffsetMask.NEIGHBOR_26, 1);
        ThermalSignatureResolverDispatcher boundedDispatcher =
                ThermalSignatureResolverDispatcher.builder(
                                StateStaticThermalResolver.geometryOnly(8))
                        .registerContextual(Blocks.BAMBOO, bounded)
                        .build();
        assertEquals(27, boundedDispatcher.plan(
                Blocks.BAMBOO.defaultBlockState()).dependencyMask().offsetCount());

        SyntheticResolver underDeclared = resolver(
                "test:under_declared", DependencyOffsetMask.SELF_ONLY, 0);
        ThermalSignatureResolverDispatcher dispatcher =
                ThermalSignatureResolverDispatcher.builder(
                                StateStaticThermalResolver.geometryOnly(8))
                        .registerContextual(Blocks.BAMBOO, underDeclared)
                        .build();
        ThermalSignatureResolverDispatcher.DispatchPlan plan =
                dispatcher.plan(Blocks.BAMBOO.defaultBlockState());
        ThermalSignatureResolution result = intern(
                plan,
                selfView(plan.dependencyMask(), Blocks.BAMBOO.defaultBlockState()),
                ThermalSignatureRegistry.builder());

        assertEquals(ThermalResolution.Status.CONSERVATIVE_UNSUPPORTED, result.status());
        assertEquals(ThermalResolution.Reason.REGION_LIMIT_EXCEEDED, result.reason());
    }

    @Test
    void contextualSnapshotSentinelsNormalizeBeforeSignatureInterning() {
        ResolvedThermalSignature contextualSignature = signature(303);
        SyntheticResolver contextual = new SyntheticResolver(
                "test:east_context",
                DependencyOffsetMask.explicit(EAST),
                1,
                view -> {
                    view.lookup(DependencyOffsetMask.SELF);
                    ResolverBlockView.Lookup<BlockState, FluidState> east = view.lookup(EAST);
                    if (east.status() == ResolverBlockView.LookupStatus.PRESENT
                            && east.value().orElseThrow().blockState().is(Blocks.GOLD_BLOCK)) {
                        view.lookup(NORTH);
                    }
                    return ThermalResolution.resolved(contextualSignature);
                }
        );
        ThermalSignatureResolverDispatcher dispatcher =
                ThermalSignatureResolverDispatcher.builder(
                                StateStaticThermalResolver.geometryOnly(8))
                        .registerContextual(Blocks.BAMBOO, contextual)
                        .build();
        ThermalSignatureResolverDispatcher.DispatchPlan plan =
                dispatcher.plan(Blocks.BAMBOO.defaultBlockState());
        ThermalSignatureRegistry.Builder signatures = ThermalSignatureRegistry.builder();

        ThermalSignatureResolution first = intern(plan,
                contextualView(plan.dependencyMask(),
                        ResolverBlockView.SnapshotCell.present(
                                Blocks.STONE.defaultBlockState(),
                                Blocks.STONE.defaultBlockState().getFluidState())),
                signatures);
        ThermalSignatureResolution duplicate = intern(plan,
                contextualView(plan.dependencyMask(),
                        ResolverBlockView.SnapshotCell.present(
                                Blocks.STONE.defaultBlockState(),
                                Blocks.STONE.defaultBlockState().getFluidState())),
                signatures);
        ThermalSignatureResolution missing = intern(plan,
                ResolverBlockView.snapshot(
                        plan.dependencyMask(),
                        Map.of(DependencyOffsetMask.SELF,
                                present(Blocks.BAMBOO.defaultBlockState()))),
                signatures);
        ThermalSignatureResolution unloaded = intern(plan,
                contextualView(plan.dependencyMask(), ResolverBlockView.SnapshotCell.unloaded()),
                signatures);
        ThermalSignatureResolution outsideMask = intern(plan,
                contextualView(plan.dependencyMask(),
                        ResolverBlockView.SnapshotCell.present(
                                Blocks.GOLD_BLOCK.defaultBlockState(),
                                Blocks.GOLD_BLOCK.defaultBlockState().getFluidState())),
                signatures);

        assertTrue(first.isResolved());
        assertEquals(first.signatureId(), duplicate.signatureId());
        assertEquals(1, signatures.signatureCount());
        assertEquals(contextualSignature,
                signatures.build().signature(first.signatureId()).orElseThrow());
        assertEquals(ThermalResolution.Reason.SNAPSHOT_DATA_MISSING, missing.reason());
        assertEquals(ThermalResolution.Reason.DEPENDENCY_UNLOADED, unloaded.reason());
        assertEquals(ThermalResolution.Reason.DEPENDENCY_OUTSIDE_DECLARED_MASK,
                outsideMask.reason());
        assertEquals(ThermalSignatureResolution.NO_SIGNATURE_ID, missing.signatureId());
        assertEquals(ThermalSignatureResolution.NO_SIGNATURE_ID, unloaded.signatureId());
        assertEquals(ThermalSignatureResolution.NO_SIGNATURE_ID, outsideMask.signatureId());
        assertEquals(5, contextual.invocations());
    }

    private static SyntheticResolver resolver(
            String resolverId,
            DependencyOffsetMask dependencyMask,
            int maxOutputRegions
    ) {
        return new SyntheticResolver(
                resolverId,
                dependencyMask,
                maxOutputRegions,
                view -> ThermalResolution.resolved(signature(404))
        );
    }

    private static ThermalSignatureResolution intern(
            ThermalSignatureResolverDispatcher.DispatchPlan plan,
            ResolverBlockView<BlockState, FluidState> view,
            ThermalSignatureRegistry.Builder signatures
    ) {
        ThermalResolution<ResolvedThermalSignature> resolution = plan.resolve(view);
        return resolution.isResolved()
                ? ThermalSignatureResolution.resolved(
                        signatures.intern(resolution.value().orElseThrow()))
                : ThermalSignatureResolution.failure(resolution);
    }

    private static ResolverBlockView<BlockState, FluidState> selfView(
            DependencyOffsetMask mask,
            BlockState state
    ) {
        return ResolverBlockView.snapshot(
                mask,
                Map.of(DependencyOffsetMask.SELF, present(state))
        );
    }

    private static ResolverBlockView<BlockState, FluidState> contextualView(
            DependencyOffsetMask mask,
            ResolverBlockView.SnapshotCell<BlockState, FluidState> east
    ) {
        return ResolverBlockView.snapshot(
                mask,
                Map.of(
                        DependencyOffsetMask.SELF, present(Blocks.BAMBOO.defaultBlockState()),
                        EAST, east
                )
        );
    }

    private static ResolverBlockView.SnapshotCell<BlockState, FluidState> present(
            BlockState state
    ) {
        return ResolverBlockView.SnapshotCell.present(state, state.getFluidState());
    }

    private static ResolvedThermalSignature signature(int flags) {
        return new ResolvedThermalSignature(
                1,
                2,
                List.of(new LocalAirRegionPattern(
                        0,
                        -1L,
                        0xffff,
                        0xffff,
                        0xffff,
                        0xffff,
                        0xffff,
                        0xffff
                )),
                3,
                4,
                5,
                0,
                flags
        );
    }

    private static final class SyntheticResolver
            implements ThermalSignatureResolver<BlockState, FluidState> {
        private final String resolverId;
        private final DependencyOffsetMask dependencyMask;
        private final int maxOutputRegions;
        private final Function<ResolverBlockView.Access<BlockState, FluidState>,
                ThermalResolution<ResolvedThermalSignature>> resolution;
        private int invocations;

        private SyntheticResolver(
                String resolverId,
                DependencyOffsetMask dependencyMask,
                int maxOutputRegions,
                Function<ResolverBlockView.Access<BlockState, FluidState>,
                        ThermalResolution<ResolvedThermalSignature>> resolution
        ) {
            this.resolverId = resolverId;
            this.dependencyMask = dependencyMask;
            this.maxOutputRegions = maxOutputRegions;
            this.resolution = resolution;
        }

        @Override
        public String resolverId() {
            return resolverId;
        }

        @Override
        public DependencyOffsetMask dependencyMask() {
            return dependencyMask;
        }

        @Override
        public int maxOutputRegions() {
            return maxOutputRegions;
        }

        @Override
        public ThermalResolution<ResolvedThermalSignature> resolve(
                ResolverBlockView.Access<BlockState, FluidState> view
        ) {
            invocations++;
            return resolution.apply(view);
        }

        private int invocations() {
            return invocations;
        }
    }
}
