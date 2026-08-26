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
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolverBlockView;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalResolution;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureResolver;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Immutable main-thread dispatcher for explicit, generic state-static, and
 * bounded contextual thermal signature resolvers. It contains no per-mod
 * policy: every non-dynamic state uses the same generic resolver.
 */
public final class ThermalSignatureResolverDispatcher {
    public static final String UNREGISTERED_RESOLVER_ID = "frostedheart:not_registered";

    private final RegisteredResolver genericStateStatic;
    private final Map<Block, RegisteredResolver> explicitOverrides;
    private final Map<Block, RegisteredResolver> contextualResolvers;

    private ThermalSignatureResolverDispatcher(
            RegisteredResolver genericStateStatic,
            Map<Block, RegisteredResolver> explicitOverrides,
            Map<Block, RegisteredResolver> contextualResolvers
    ) {
        this.genericStateStatic = genericStateStatic;
        this.explicitOverrides = Map.copyOf(explicitOverrides);
        this.contextualResolvers = Map.copyOf(contextualResolvers);
    }

    public static Builder builder(StateStaticThermalResolver genericStateStatic) {
        return new Builder(genericStateStatic);
    }

    /** Freezes one route before its loaded-only dependency snapshot is captured. */
    public DispatchPlan plan(BlockState targetState) {
        Objects.requireNonNull(targetState, "targetState");
        Block block = targetState.getBlock();

        if (targetState.is(Blocks.MOVING_PISTON)) {
            return DispatchPlan.unregistered(ThermalResolution.Reason.UNRESOLVED_DYNAMIC);
        }
        RegisteredResolver explicit = explicitOverrides.get(block);
        if (explicit != null) {
            return DispatchPlan.registered(Route.EXPLICIT_OVERRIDE, explicit);
        }
        if (!block.hasDynamicShape()) {
            return DispatchPlan.registered(Route.GENERIC_STATE_STATIC, genericStateStatic);
        }
        RegisteredResolver contextual = contextualResolvers.get(block);
        if (contextual != null) {
            return DispatchPlan.registered(Route.CONTEXTUAL, contextual);
        }

        return DispatchPlan.unregistered(ThermalResolution.Reason.NOT_REGISTERED);
    }

    public enum Route {
        EXPLICIT_OVERRIDE,
        GENERIC_STATE_STATIC,
        CONTEXTUAL,
        UNREGISTERED
    }

    /**
     * Immutable selected route. The caller captures exactly
     * {@link #dependencyMask()} and then resolves and interns on the main thread.
     */
    public static final class DispatchPlan {
        private final Route route;
        private final String resolverId;
        private final DependencyOffsetMask dependencyMask;
        private final int maxOutputRegions;
        private final RegisteredResolver resolver;
        private final ThermalResolution.Reason unregisteredReason;

        private DispatchPlan(
                Route route,
                String resolverId,
                DependencyOffsetMask dependencyMask,
                int maxOutputRegions,
                RegisteredResolver resolver,
                ThermalResolution.Reason unregisteredReason
        ) {
            this.route = route;
            this.resolverId = resolverId;
            this.dependencyMask = dependencyMask;
            this.maxOutputRegions = maxOutputRegions;
            this.resolver = resolver;
            this.unregisteredReason = unregisteredReason;
        }

        private static DispatchPlan registered(Route route, RegisteredResolver resolver) {
            return new DispatchPlan(
                    route,
                    resolver.resolverId(),
                    resolver.dependencyMask(),
                    resolver.maxOutputRegions(),
                    resolver,
                    null
            );
        }

        private static DispatchPlan unregistered(ThermalResolution.Reason reason) {
            return new DispatchPlan(
                    Route.UNREGISTERED,
                    UNREGISTERED_RESOLVER_ID,
                    DependencyOffsetMask.SELF_ONLY,
                    0,
                    null,
                    reason
            );
        }

        public Route route() {
            return route;
        }

        public String resolverId() {
            return resolverId;
        }

        public DependencyOffsetMask dependencyMask() {
            return dependencyMask;
        }

        public int maxOutputRegions() {
            return maxOutputRegions;
        }

        /** Resolves through the audited snapshot API without interning a signature. */
        public ThermalResolution<ResolvedThermalSignature> resolve(
                ResolverBlockView<BlockState, FluidState> snapshot
        ) {
            Objects.requireNonNull(snapshot, "snapshot");
            return resolver == null
                    ? ThermalResolution.failure(unregisteredReason)
                    : resolver.resolveSnapshot(snapshot);
        }

    }

    /** Builder is main-thread confined; built dispatchers are immutable snapshots. */
    public static final class Builder {
        private final Map<Block, RegisteredResolver> explicitOverrides = new LinkedHashMap<>();
        private final Map<Block, RegisteredResolver> contextualResolvers = new LinkedHashMap<>();
        private final Map<ThermalSignatureResolver<BlockState, FluidState>, RegisteredResolver>
                resolversByInstance = new IdentityHashMap<>();
        private final Map<String, RegisteredResolver> resolversById = new TreeMap<>();
        private final RegisteredResolver genericStateStatic;

        private Builder(StateStaticThermalResolver genericStateStatic) {
            this.genericStateStatic = freezeResolver(
                    Objects.requireNonNull(genericStateStatic, "genericStateStatic"));
        }

        public Builder registerExplicitOverride(
                Block block,
                ThermalSignatureResolver<BlockState, FluidState> resolver
        ) {
            bind("explicit override", explicitOverrides, block, resolver);
            return this;
        }

        /** Registers one immutable fixed signature through the normal resolver contract. */
        public Builder registerExplicitProfile(
                Block block,
                String resolverId,
                ResolvedThermalSignature signature
        ) {
            return registerExplicitOverride(
                    block,
                    new FixedProfileResolver(resolverId, signature)
            );
        }

        public Builder registerContextual(
                Block block,
                ThermalSignatureResolver<BlockState, FluidState> resolver
        ) {
            bind("contextual", contextualResolvers, block, resolver);
            return this;
        }

        public ThermalSignatureResolverDispatcher build() {
            return new ThermalSignatureResolverDispatcher(
                    genericStateStatic,
                    explicitOverrides,
                    contextualResolvers
            );
        }

        private void bind(
                String registrationKind,
                Map<Block, RegisteredResolver> bindings,
                Block block,
                ThermalSignatureResolver<BlockState, FluidState> resolver
        ) {
            Objects.requireNonNull(block, "block");
            Objects.requireNonNull(resolver, "resolver");
            if (bindings.containsKey(block)) {
                throw new IllegalArgumentException(
                        "duplicate " + registrationKind + " registration for block");
            }
            bindings.put(block, freezeResolver(resolver));
        }

        private RegisteredResolver freezeResolver(
                ThermalSignatureResolver<BlockState, FluidState> resolver
        ) {
            RegisteredResolver existingInstance = resolversByInstance.get(resolver);
            if (existingInstance != null) {
                return existingInstance;
            }

            String resolverId = requireResolverId(resolver.resolverId());
            if (resolversById.containsKey(resolverId)) {
                throw new IllegalArgumentException("duplicate resolver ID: " + resolverId);
            }
            DependencyOffsetMask dependencyMask = Objects.requireNonNull(
                    resolver.dependencyMask(), "resolver dependencyMask");
            if (!dependencyMask.contains(DependencyOffsetMask.SELF)
                    || dependencyMask.offsetCount() > DependencyOffsetMask.MAXIMUM_OFFSET_COUNT
                    || dependencyMask.offsets().stream().anyMatch(offset ->
                    Math.abs(offset.x()) > 1
                            || Math.abs(offset.y()) > 1
                            || Math.abs(offset.z()) > 1)) {
                throw new IllegalArgumentException("resolver dependency mask must fit SELF + 3x3x3");
            }
            int maxOutputRegions = resolver.maxOutputRegions();
            if (maxOutputRegions < 0) {
                throw new IllegalArgumentException("maxOutputRegions must be non-negative");
            }

            RegisteredResolver frozen = new RegisteredResolver(
                    resolverId,
                    dependencyMask,
                    maxOutputRegions,
                    resolver
            );
            resolversByInstance.put(resolver, frozen);
            resolversById.put(resolverId, frozen);
            return frozen;
        }

        private static String requireResolverId(String resolverId) {
            Objects.requireNonNull(resolverId, "resolverId");
            ResourceLocation parsed = ResourceLocation.tryParse(resolverId);
            if (resolverId.isBlank()
                    || resolverId.indexOf(':') <= 0
                    || parsed == null
                    || !parsed.toString().equals(resolverId)) {
                throw new IllegalArgumentException(
                        "resolverId must be a canonical namespaced resource location");
            }
            return resolverId;
        }
    }

    /** Freezes registration metadata while delegating only the resolver function. */
    private static final class RegisteredResolver
            implements ThermalSignatureResolver<BlockState, FluidState> {
        private final String resolverId;
        private final DependencyOffsetMask dependencyMask;
        private final int maxOutputRegions;
        private final ThermalSignatureResolver<BlockState, FluidState> delegate;

        private RegisteredResolver(
                String resolverId,
                DependencyOffsetMask dependencyMask,
                int maxOutputRegions,
                ThermalSignatureResolver<BlockState, FluidState> delegate
        ) {
            this.resolverId = resolverId;
            this.dependencyMask = dependencyMask;
            this.maxOutputRegions = maxOutputRegions;
            this.delegate = delegate;
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
            return delegate.resolve(view);
        }
    }

    private static final class FixedProfileResolver
            implements ThermalSignatureResolver<BlockState, FluidState> {
        private final String resolverId;
        private final ResolvedThermalSignature signature;

        private FixedProfileResolver(String resolverId, ResolvedThermalSignature signature) {
            this.resolverId = resolverId;
            this.signature = Objects.requireNonNull(signature, "signature");
        }

        @Override
        public String resolverId() {
            return resolverId;
        }

        @Override
        public DependencyOffsetMask dependencyMask() {
            return DependencyOffsetMask.SELF_ONLY;
        }

        @Override
        public int maxOutputRegions() {
            return signature.localAirRegionCount();
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
            return ThermalResolution.resolved(signature);
        }
    }
}
