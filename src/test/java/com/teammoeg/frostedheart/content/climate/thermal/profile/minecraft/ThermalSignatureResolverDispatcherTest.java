/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.ThermalTestFixtures;
import com.teammoeg.frostedheart.content.climate.thermal.profile.DependencyOffsetMask;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolverBlockView;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalResolution;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalSignatureResolverDispatcherTest {
    private static final DependencyOffsetMask.Offset EAST =
            new DependencyOffsetMask.Offset(1, 0, 0);

    @Test
    void selectedPlansAreFrozenAndReusedPerResolver() {
        ResolvedThermalSignature override =
                ThermalTestFixtures.fullAirSignature();
        ThermalSignatureResolverDispatcher dispatcher =
                ThermalSignatureResolverDispatcher.builder(
                                StateStaticThermalResolver.geometryOnly(64))
                        .registerExplicitProfile(
                                Blocks.STONE, "test:stone", override)
                        .build();

        ThermalSignatureResolverDispatcher.DispatchPlan first =
                dispatcher.plan(Blocks.STONE.defaultBlockState());
        ThermalSignatureResolverDispatcher.DispatchPlan second =
                dispatcher.plan(Blocks.STONE.defaultBlockState());
        assertSame(first, second);
        assertEquals(override, first.resolve(view(
                first.dependencyMask(),
                Blocks.STONE.defaultBlockState(),
                Blocks.AIR.defaultBlockState())).value());

        ThermalSignatureResolverDispatcher.DispatchPlan generic =
                dispatcher.plan(Blocks.DIRT.defaultBlockState());
        assertSame(generic,
                dispatcher.plan(Blocks.OAK_SLAB.defaultBlockState()));
        assertTrue(generic.resolve(view(
                generic.dependencyMask(),
                Blocks.DIRT.defaultBlockState(),
                Blocks.AIR.defaultBlockState())).isResolved());
    }

    @Test
    void movingPistonAlwaysUsesTheCachedDynamicFailurePlan() {
        ThermalSignatureResolverDispatcher dispatcher =
                ThermalSignatureResolverDispatcher.builder(
                                StateStaticThermalResolver.geometryOnly(64))
                        .registerExplicitProfile(
                                Blocks.MOVING_PISTON,
                                "test:moving",
                                ThermalTestFixtures.fullAirSignature())
                        .build();
        ThermalSignatureResolverDispatcher.DispatchPlan plan =
                dispatcher.plan(Blocks.MOVING_PISTON.defaultBlockState());

        assertSame(plan,
                dispatcher.plan(Blocks.MOVING_PISTON.defaultBlockState()));
        ThermalResolution<ResolvedThermalSignature> result = plan.resolve(view(
                plan.dependencyMask(),
                Blocks.MOVING_PISTON.defaultBlockState(),
                Blocks.AIR.defaultBlockState()));
        assertEquals(ThermalResolution.Status.UNRESOLVED, result.status());
        assertEquals(ThermalResolution.Reason.UNRESOLVED_DYNAMIC,
                result.reason());
    }

    @Test
    void contextualResolverReceivesOnlyItsDeclaredScratch() {
        ThermalSignatureResolverDispatcher dispatcher =
                ThermalSignatureResolverDispatcher.builder(
                                StateStaticThermalResolver.geometryOnly(64))
                        .registerContextual(
                                Blocks.BAMBOO,
                                new EastResolver())
                        .build();
        ThermalSignatureResolverDispatcher.DispatchPlan plan =
                dispatcher.plan(Blocks.BAMBOO.defaultBlockState());

        ThermalResolution<ResolvedThermalSignature> resolved = plan.resolve(view(
                plan.dependencyMask(),
                Blocks.BAMBOO.defaultBlockState(),
                Blocks.AIR.defaultBlockState()));
        assertTrue(resolved.isResolved());

        ResolverBlockView.Scratch<BlockState, FluidState> scratch =
                new ResolverBlockView.Scratch<>();
        ResolverBlockView<BlockState, FluidState> unavailable =
                scratch.begin(plan.dependencyMask());
        BlockState bamboo = Blocks.BAMBOO.defaultBlockState();
        scratch.putPresent(0, 0, 0, bamboo, bamboo.getFluidState());
        scratch.putUnavailable(
                1, 0, 0, ResolverBlockView.LookupStatus.UNLOADED);
        assertEquals(ThermalResolution.Reason.DEPENDENCY_UNLOADED,
                plan.resolve(unavailable).reason());
    }

    @Test
    void builderRejectsDuplicateBindingsAndNonCanonicalIds() {
        ThermalSignatureResolverDispatcher.Builder builder =
                ThermalSignatureResolverDispatcher.builder(
                        StateStaticThermalResolver.geometryOnly(64));
        builder.registerContextual(Blocks.BAMBOO, new EastResolver());
        assertThrows(IllegalArgumentException.class,
                () -> builder.registerContextual(
                        Blocks.BAMBOO, new EastResolver()));
        assertThrows(IllegalArgumentException.class,
                () -> ThermalSignatureResolverDispatcher.builder(
                                StateStaticThermalResolver.geometryOnly(64))
                        .registerContextual(
                                Blocks.BAMBOO,
                                new InvalidIdResolver()));
    }

    private static ResolverBlockView<BlockState, FluidState> view(
            DependencyOffsetMask mask,
            BlockState self,
            BlockState east
    ) {
        ResolverBlockView.Scratch<BlockState, FluidState> scratch =
                new ResolverBlockView.Scratch<>();
        ResolverBlockView<BlockState, FluidState> view = scratch.begin(mask);
        for (DependencyOffsetMask.Offset offset : mask.offsets()) {
            BlockState state = offset.equals(EAST) ? east : self;
            scratch.putPresent(
                    offset.x(), offset.y(), offset.z(),
                    state, state.getFluidState());
        }
        return view;
    }

    private static class EastResolver
            implements ThermalSignatureResolver<BlockState, FluidState> {
        @Override
        public String resolverId() {
            return "test:east";
        }

        @Override
        public DependencyOffsetMask dependencyMask() {
            return DependencyOffsetMask.explicit(EAST);
        }

        @Override
        public int maxOutputRegions() {
            return 1;
        }

        @Override
        public ThermalResolution<ResolvedThermalSignature> resolve(
                ResolverBlockView.Access<BlockState, FluidState> view
        ) {
            ResolverBlockView.Lookup<BlockState, FluidState> east =
                    view.lookup(EAST);
            return east.status() == ResolverBlockView.LookupStatus.PRESENT
                    ? ThermalResolution.resolved(
                            ThermalTestFixtures.fullAirSignature())
                    : ThermalResolution.failure(east.reason());
        }
    }

    private static final class InvalidIdResolver extends EastResolver {
        @Override
        public String resolverId() {
            return "not_namespaced";
        }
    }
}
