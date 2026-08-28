/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.profile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ResolverBlockViewTest {
    private static final DependencyOffsetMask.Offset EAST =
            new DependencyOffsetMask.Offset(1, 0, 0);

    @Test
    void scratchReusesScalarLookupsForPresentAndUnavailableDependencies() {
        ResolverBlockView.Scratch<String, String> scratch =
                new ResolverBlockView.Scratch<>();
        ResolverBlockView<String, String> view = scratch.begin(
                DependencyOffsetMask.explicit(EAST));
        scratch.putPresent(0, 0, 0, "stone", "empty");
        scratch.putUnavailable(
                1, 0, 0, ResolverBlockView.LookupStatus.UNLOADED);
        ResolverBlockView.Access<String, String> access = view.openAccess();

        assertEquals("stone", access.lookup(0, 0, 0).blockState());
        assertEquals(ResolverBlockView.LookupStatus.UNLOADED,
                access.lookup(EAST).status());
        assertEquals(ThermalResolution.Reason.DEPENDENCY_UNLOADED,
                access.normalize(ThermalResolution.resolved("ignored")).reason());
    }

    @Test
    void undeclaredLookupReturnsTheSharedOutsideSentinel() {
        ResolverBlockView.Scratch<String, String> scratch =
                new ResolverBlockView.Scratch<>();
        ResolverBlockView<String, String> view = scratch.begin(
                DependencyOffsetMask.SELF_ONLY);
        scratch.putPresent(0, 0, 0, "air", "empty");
        ResolverBlockView.Access<String, String> access = view.openAccess();

        ResolverBlockView.Lookup<String, String> first = access.lookup(EAST);
        ResolverBlockView.Lookup<String, String> second = access.lookup(EAST);
        assertSame(first, second);
        assertEquals(ResolverBlockView.LookupStatus.OUTSIDE_DECLARED_MASK,
                first.status());
    }

    @Test
    void blockEntityAccessNormalizesTheResultToConservativeUnsupported() {
        ResolverBlockView.Scratch<String, String> scratch =
                new ResolverBlockView.Scratch<>();
        ResolverBlockView<String, String> view = scratch.begin(
                DependencyOffsetMask.SELF_ONLY);
        scratch.putPresent(0, 0, 0, "machine", "empty");
        ResolverBlockView.Access<String, String> access = view.openAccess();

        assertEquals(ThermalResolution.Status.CONSERVATIVE_UNSUPPORTED,
                access.blockEntity(0, 0, 0).status());
        assertEquals(ThermalResolution.Reason.BLOCK_ENTITY_DEPENDENT,
                access.normalize(ThermalResolution.resolved("ignored")).reason());
    }
}
