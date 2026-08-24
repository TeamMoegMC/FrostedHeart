/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.benchmark;

import com.teammoeg.frostedheart.content.climate.thermal.profile.DependencyOffsetMask;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolverBlockView;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalResolution;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureResolver;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.StateStaticThermalResolver;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.ThermalSignatureResolverDispatcher;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Measures bounded resolver dispatch without any Level or chunk access. */
@BenchmarkMode({Mode.AverageTime, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class PhaseAResolverBenchmark {
    private static final DependencyOffsetMask.Offset EAST =
            new DependencyOffsetMask.Offset(1, 0, 0);
    private static final ResolvedThermalSignature SOLID_PROFILE =
            new ResolvedThermalSignature(0, 1, List.of(), 1, 1, 0, 0, 1);

    @Param({"generic_air", "generic_fence", "explicit_stone", "contextual_bamboo"})
    public String fixture;

    private ThermalSignatureResolverDispatcher.DispatchPlan plan;
    private ResolverBlockView<BlockState, FluidState> snapshot;

    @Setup
    public void setup() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ThermalSignatureResolverDispatcher dispatcher =
                ThermalSignatureResolverDispatcher.builder(
                                StateStaticThermalResolver.geometryOnly(64))
                        .registerExplicitProfile(
                                Blocks.STONE,
                                "frostedheart:phase_a_benchmark_stone",
                                SOLID_PROFILE)
                        .registerContextual(
                                Blocks.BAMBOO,
                                new BenchmarkContextualResolver())
                        .build();
        BlockState blockState = switch (fixture) {
            case "generic_air" -> Blocks.AIR.defaultBlockState();
            case "generic_fence" -> Blocks.OAK_FENCE.defaultBlockState();
            case "explicit_stone" -> Blocks.STONE.defaultBlockState();
            case "contextual_bamboo" -> Blocks.BAMBOO.defaultBlockState();
            default -> throw new IllegalArgumentException("unknown fixture " + fixture);
        };
        plan = dispatcher.plan(blockState);

        Map<DependencyOffsetMask.Offset,
                ResolverBlockView.SnapshotCell<BlockState, FluidState>> cells = new HashMap<>();
        for (DependencyOffsetMask.Offset offset : plan.dependencyMask().offsets()) {
            BlockState state = offset.equals(DependencyOffsetMask.SELF)
                    ? blockState
                    : Blocks.STONE.defaultBlockState();
            cells.put(offset, ResolverBlockView.SnapshotCell.present(
                    state, state.getFluidState()));
        }
        snapshot = ResolverBlockView.snapshot(plan.dependencyMask(), cells);
    }

    @Benchmark
    public ThermalResolution<ResolvedThermalSignature> resolveDispatched() {
        return plan.resolve(snapshot);
    }

    private static final class BenchmarkContextualResolver
            implements ThermalSignatureResolver<BlockState, FluidState> {
        private static final DependencyOffsetMask MASK = DependencyOffsetMask.explicit(EAST);

        @Override
        public String resolverId() {
            return "frostedheart:phase_a_benchmark_context";
        }

        @Override
        public DependencyOffsetMask dependencyMask() {
            return MASK;
        }

        @Override
        public int maxOutputRegions() {
            return 0;
        }

        @Override
        public ThermalResolution<ResolvedThermalSignature> resolve(
                ResolverBlockView.Access<BlockState, FluidState> view
        ) {
            ThermalResolution<ResolverBlockView.StateAndFluid<BlockState, FluidState>> self =
                    view.lookup(DependencyOffsetMask.SELF).asResolution();
            if (!self.isResolved()) {
                return ThermalResolution.failure(self.reason());
            }
            ThermalResolution<ResolverBlockView.StateAndFluid<BlockState, FluidState>> east =
                    view.lookup(EAST).asResolution();
            return east.isResolved()
                    ? ThermalResolution.resolved(SOLID_PROFILE)
                    : ThermalResolution.failure(east.reason());
        }
    }
}
