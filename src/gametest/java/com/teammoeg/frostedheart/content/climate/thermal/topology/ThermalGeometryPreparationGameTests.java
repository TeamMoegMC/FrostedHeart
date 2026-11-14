/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.GeometryHalo;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Neighboring signatures retain their owners while a private geometry overlay changes. */
@GameTestHolder("frostedheart_geometry_preparation")
@PrefixGameTestTemplate(false)
public final class ThermalGeometryPreparationGameTests {
    @GameTest(template = "phase0a_empty", batch = "geometry_halo_full_reference", timeoutTicks = 200)
    public static void incrementalGeometryMatchesSequentialFullReplacementAcrossOverlappingOwners(GameTestHelper helper) {
        var pages = new WorkerPageStore(16);
        var view = new TopologyView(pages, new Long2ObjectOpenHashMap<>(), new Int2ObjectOpenHashMap<>());
        var owners = new java.util.IdentityHashMap<ThermalPageHandle, GeometryHalo>();
        var references = new java.util.HashMap<Long, Integer>(); var signatures = new java.util.HashMap<Long, Integer>();
        var handles = new ThermalPageHandle[8];
        for (int index = 0; index < handles.length; index++) handles[index] = new ThermalPageHandle(index + 1, 1);
        var random = new java.util.Random(71114053); int samples = 0;
        for (int cut = 0; cut < 96; cut++) {
            var writes = new java.util.LinkedHashMap<ThermalPageHandle, GeometryHalo>();
            for (int update = 0; update < 4; update++) {
                var owner = handles[random.nextInt(handles.length)]; GeometryHalo next = null;
                if (random.nextInt(4) == 0) next = owners.get(owner);
                else if (random.nextInt(5) != 0) {
                    long members = random.nextLong(); long[] positions = new long[Long.bitCount(members)]; int[] values = new int[positions.length];
                    int index = 0;
                    while (members != 0) {
                        int position = Long.numberOfTrailingZeros(members); members &= members - 1;
                        positions[index] = BlockPos.asLong(position * 3, 64, 0);
                        values[index++] = random.nextInt(5) == 0 ? ThermalSignatureTable.UNRESOLVED : 3 + random.nextInt(4);
                    }
                    next = new GeometryHalo(positions, values);
                }
                writes.put(owner, next);
            }
            var before = new java.util.HashMap<>(signatures);
            for (var write : writes.entrySet()) {
                view.stageHalo(write.getKey(), write.getValue());
                replaceFull(owners, references, signatures, write.getKey(), write.getValue());
            }
            var changed = new it.unimi.dsi.fastutil.longs.LongOpenHashSet(); view.collectChangedHaloSections(changed);
            var expectedChanged = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
            var frozen = view.copyHaloSignatures(view.capturedHaloSignatureBound()); int expectedFrozen = 0;
            for (int point = 0; point < 64; point++) {
                long position = BlockPos.asLong(point * 3, 64, 0);
                int expected = signatures.getOrDefault(position, ThermalSignatureTable.UNRESOLVED);
                if (before.getOrDefault(position, ThermalSignatureTable.UNRESOLVED) != expected)
                    expectedChanged.add(net.minecraft.core.SectionPos.asLong(point * 3 >> 4, 4, 0));
                helper.assertTrue(view.signatureAtWorld(point * 3, 64, 0) == expected,
                        "Incremental private geometry matches complete replacement; cut=" + cut + ",point=" + point);
                if (expected != ThermalSignatureTable.UNRESOLVED) {
                    expectedFrozen++;
                    helper.assertTrue(frozen.containsKey(position) && frozen.get(position) == expected, "Frozen geometry contains the same known values");
                } else helper.assertTrue(frozen.get(position) == ThermalSignatureTable.UNRESOLVED,
                        "Frozen geometry preserves the unknown result, whether stored explicitly or supplied by its default; cut=" + cut + ",point=" + point);
                samples++;
            }
            int known = 0;
            for (int signature : frozen.values()) if (signature != ThermalSignatureTable.UNRESOLVED) known++;
            helper.assertTrue(known == expectedFrozen && changed.equals(expectedChanged),
                    "The frozen set and changed Sections match the full reference; cut=" + cut);
            helper.assertTrue(view.dependenciesCurrent(), "Private preparation retains a coherent committed baseline");
            view.commitHalos(); view.resetPreparation();
            for (int point = 0; point < 64; point++) {
                long position = BlockPos.asLong(point * 3, 64, 0);
                helper.assertTrue(pages.haloReferences(position) == references.getOrDefault(position, 0)
                                && pages.haloSignature(position) == signatures.getOrDefault(position, ThermalSignatureTable.UNRESOLVED),
                        "Commit retains the complete replacement model's ownership and values");
            }
        }
        FHMain.LOGGER.info("THERMAL_HALO_FULL_REFERENCE,cuts=96,owners=8,privateAndFrozenSamples={},signatureDifferences=0,ownershipDifferences=0", samples);
        helper.succeed();
    }

    /** Independent full replacement model, retaining writer order and every owner count. */
    private static void replaceFull(java.util.Map<ThermalPageHandle, GeometryHalo> owners,
            java.util.Map<Long, Integer> references, java.util.Map<Long, Integer> signatures, ThermalPageHandle owner, GeometryHalo next) {
        var previous = owners.remove(owner);
        if (previous != null) for (long position : previous.positions()) {
            int remaining = references.get(position) - 1;
            if (remaining == 0) { references.remove(position); signatures.remove(position); }
            else references.put(position, remaining);
        }
        if (next == null) return;
        owners.put(owner, next);
        for (int index = 0; index < next.size(); index++) {
            long position = next.positions()[index]; references.merge(position, 1, Integer::sum);
            signatures.put(position, next.signatures()[index]);
        }
    }

    @GameTest(template = "phase0a_empty", batch = "geometry_halo_sparse_values", timeoutTicks = 100)
    public static void oneChangedSignatureDoesNotDuplicateTheWholeNeighborSnapshot(GameTestHelper helper) throws ReflectiveOperationException {
        var pages = new WorkerPageStore(16); var owner = new ThermalPageHandle(1, 1);
        long[] positions = new long[4096]; int[] values = new int[4096]; java.util.Arrays.fill(values, 3);
        for (int index = 0; index < positions.length; index++) positions[index] = BlockPos.asLong(index & 15, 64 + (index >>> 8), index >>> 4 & 15);
        pages.replaceHalo(owner, new GeometryHalo(positions, values));
        int[] changed = values.clone(); changed[0] = 4;
        var view = new TopologyView(pages, new Long2ObjectOpenHashMap<>(), new Int2ObjectOpenHashMap<>());
        view.stageHalo(owner, new GeometryHalo(positions.clone(), changed));
        helper.assertTrue(view.signatureAtWorld(0, 64, 0) == 4 && view.signatureAtWorld(15, 79, 15) == 3,
                "One changed value and all unchanged geometry remain readable");
        var valuesField = TopologyView.class.getDeclaredField("haloValues"); valuesField.setAccessible(true);
        var overlay = (Long2IntOpenHashMap) valuesField.get(view);
        FHMain.LOGGER.info("THERMAL_HALO_SPARSE_VALUES,snapshotPositions=4096,changedPositions=1,overlayPositions={}", overlay.size());
        helper.assertTrue(overlay.size() == 1, "Private value storage follows actual changes, not the complete neighboring snapshot; entries=" + overlay.size());
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "geometry_halo_commit_delta", timeoutTicks = 100)
    public static void unchangedMembershipOnlyRevisesSectionsWhoseValuesChange(GameTestHelper helper) {
        var pages = new WorkerPageStore(16); var owner = new ThermalPageHandle(1, 1);
        long p0 = BlockPos.asLong(0, 64, 0), p1 = BlockPos.asLong(32, 64, 0);
        long s0 = net.minecraft.core.SectionPos.asLong(0, 4, 0), s1 = net.minecraft.core.SectionPos.asLong(2, 4, 0);
        long[] positions = {p0, p1}; var initial = new GeometryHalo(positions, new int[] {3, 3});
        pages.replaceHalo(owner, initial); long v0 = pages.haloVersion(s0), v1 = pages.haloVersion(s1);
        pages.replaceHalo(owner, new GeometryHalo(positions.clone(), new int[] {3, 3}));
        helper.assertTrue(pages.haloVersion(s0) == v0 && pages.haloVersion(s1) == v1,
                "Repeating the same captured geometry does not invalidate either Section");
        pages.replaceHalo(owner, new GeometryHalo(positions.clone(), new int[] {4, 3}));
        helper.assertTrue(pages.haloVersion(s0) > v0 && pages.haloVersion(s1) == v1,
                "Only the Section with an actual signature change receives a new dependency version");
        helper.assertTrue(pages.haloReferences(p0) == 1 && pages.haloReferences(p1) == 1,
                "In-place replacement preserves both original owners");
        FHMain.LOGGER.info("THERMAL_HALO_COMMIT_DELTA,unchangedSectionsRevised=0,changedSectionsRevised=1,referencesPreserved=true");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "geometry_halo_overlap_restore", timeoutTicks = 100)
    public static void aLaterOverlappingWriterCanRestoreTheCommittedValue(GameTestHelper helper) {
        var pages = new WorkerPageStore(16); var a = new ThermalPageHandle(1, 1); var b = new ThermalPageHandle(2, 1);
        long position = BlockPos.asLong(1, 64, 1);
        pages.replaceHalo(a, new GeometryHalo(new long[] {position}, new int[] {3}));
        pages.replaceHalo(b, new GeometryHalo(new long[] {position}, new int[] {3}));
        var view = new TopologyView(pages, new Long2ObjectOpenHashMap<>(), new Int2ObjectOpenHashMap<>());
        view.stageHalo(a, new GeometryHalo(new long[] {position}, new int[] {5}));
        view.stageHalo(b, new GeometryHalo(new long[] {position}, new int[] {3}));
        helper.assertTrue(view.signatureAtWorld(1, 64, 1) == 3, "A later overlapping snapshot restores the original signature");
        var frozen = view.copyHaloSignatures(view.capturedHaloSignatureBound());
        helper.assertTrue(frozen.get(position) == 3, "Frozen and live private readers agree after restoring the baseline");
        view.commitHalos(); view.resetPreparation();
        helper.assertTrue(pages.haloSignature(position) == 3 && pages.haloReferences(position) == 2,
                "Commit preserves the same writer order without changing ownership");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "geometry_halo_baseline_dependency", timeoutTicks = 100)
    public static void removingTheBaselineOwnerDuringPreparationInvalidatesTheRead(GameTestHelper helper) {
        var pages = new WorkerPageStore(16); var old = new ThermalPageHandle(1, 1); var next = new ThermalPageHandle(2, 1);
        long position = BlockPos.asLong(1, 64, 1);
        pages.replaceHalo(old, new GeometryHalo(new long[] {position}, new int[] {3}));
        var view = new TopologyView(pages, new Long2ObjectOpenHashMap<>(), new Int2ObjectOpenHashMap<>());
        view.stageHalo(next, new GeometryHalo(new long[] {position}, new int[] {3}));
        helper.assertTrue(view.signatureAtWorld(1, 64, 1) == 3 && view.dependenciesCurrent(), "The original neighboring geometry is current");
        pages.removeHalo(old);
        helper.assertTrue(!view.dependenciesCurrent(), "A baseline used by private preparation cannot silently change during unload");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "geometry_halo_shared_removal_dependency", timeoutTicks = 100)
    public static void anotherOwnerRemovalInvalidatesAPreparedLastRemoval(GameTestHelper helper) {
        var pages = new WorkerPageStore(16); var a = new ThermalPageHandle(1, 1); var b = new ThermalPageHandle(2, 1);
        long position = BlockPos.asLong(1, 64, 1); var halo = new GeometryHalo(new long[] {position}, new int[] {3});
        pages.replaceHalo(a, halo); pages.replaceHalo(b, halo);
        var view = new TopologyView(pages, new Long2ObjectOpenHashMap<>(), new Int2ObjectOpenHashMap<>());
        view.stageHalo(a, null);
        var changed = new it.unimi.dsi.fastutil.longs.LongOpenHashSet(); view.collectChangedHaloSections(changed);
        helper.assertTrue(changed.isEmpty() && view.dependenciesCurrent(),
                "The prepared removal initially leaves the other committed owner's geometry unchanged");
        pages.removeHalo(b);
        helper.assertTrue(!view.dependenciesCurrent(),
                "Losing another shared owner changes the pending removal even while the committed signature stays the same");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "geometry_halo_unknown_overlap", timeoutTicks = 100)
    public static void removingAnOverlapDoesNotFreezeAnUnresolvedSignature(GameTestHelper helper) {
        var pages = new WorkerPageStore(16); var a = new ThermalPageHandle(1, 1); var b = new ThermalPageHandle(2, 1);
        long position = BlockPos.asLong(1, 64, 0);
        var unknown = new GeometryHalo(new long[] {position}, new int[] {ThermalSignatureTable.UNRESOLVED});
        pages.replaceHalo(a, unknown); pages.replaceHalo(b, unknown);
        var view = new TopologyView(pages, new Long2ObjectOpenHashMap<>(), new Int2ObjectOpenHashMap<>());
        view.stageHalo(a, null);
        helper.assertTrue(view.copyHaloSignatures(view.capturedHaloSignatureBound()).isEmpty(),
                "The remaining unresolved owner contributes no captured signature to frozen geometry");
        view.commitHalos(); view.resetPreparation();
        helper.assertTrue(pages.haloReferences(position) == 1 && view.signatureAtWorld(1, 64, 0) == ThermalSignatureTable.UNRESOLVED,
                "Removing a duplicate owner preserves the remaining unresolved reference");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "geometry_halo_same_members", timeoutTicks = 100)
    public static void unchangedHaloMembershipUpdatesValuesWithoutReferenceScratch(GameTestHelper helper) throws ReflectiveOperationException {
        var pages = new WorkerPageStore(16); var owner = new ThermalPageHandle(1, 1);
        long first = BlockPos.asLong(1, 64, 1), second = BlockPos.asLong(2, 64, 1);
        long[] positions = {first, second};
        pages.replaceHalo(owner, new GeometryHalo(positions, new int[] {3, 4}));
        var view = new TopologyView(pages, new Long2ObjectOpenHashMap<>(), new Int2ObjectOpenHashMap<>());
        view.stageHalo(owner, new GeometryHalo(positions.clone(), new int[] {5, 6}));
        helper.assertTrue(view.signatureAtWorld(1, 64, 1) == 5 && view.signatureAtWorld(2, 64, 1) == 6,
                "An unchanged coordinate set must still publish all new signatures");
        var referenceField = TopologyView.class.getDeclaredField("haloReferenceDelta"); referenceField.setAccessible(true);
        var references = (Long2IntOpenHashMap) referenceField.get(view);
        FHMain.LOGGER.info("THERMAL_HALO_REFERENCE_SCRATCH,unchangedPositions=2,deltaEntries={},valuesUpdated=true", references.size());
        helper.assertTrue(references.isEmpty(), "Identical old/new membership must not build a coordinate map of zero reference deltas");
        view.commitHalos(); view.resetPreparation();
        helper.assertTrue(pages.haloReferences(first) == 1 && pages.haloReferences(second) == 1
                        && view.signatureAtWorld(1, 64, 1) == 5, "Commit keeps the same owners and new values");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "geometry_halo_shared_members", timeoutTicks = 100)
    public static void overlappingHaloUpdatesAndRemovalPreserveTheRemainingOwner(GameTestHelper helper) {
        var pages = new WorkerPageStore(16); var a = new ThermalPageHandle(1, 1); var b = new ThermalPageHandle(2, 1);
        long p0 = BlockPos.asLong(0, 64, 0), p1 = BlockPos.asLong(1, 64, 0), p2 = BlockPos.asLong(2, 64, 0), p3 = BlockPos.asLong(3, 64, 0);
        pages.replaceHalo(a, new GeometryHalo(new long[] {p0, p1}, new int[] {3, 3}));
        pages.replaceHalo(b, new GeometryHalo(new long[] {p1, p2}, new int[] {3, 4}));
        var view = new TopologyView(pages, new Long2ObjectOpenHashMap<>(), new Int2ObjectOpenHashMap<>());
        view.stageHalo(a, new GeometryHalo(new long[] {p0, p1}, new int[] {5, 6}));
        view.stageHalo(b, new GeometryHalo(new long[] {p3, p2}, new int[] {7, 8}));
        var frozen = view.copyHaloSignatures(view.capturedHaloSignatureBound());
        helper.assertTrue(frozen.size() == 4 && frozen.get(p0) == 5 && frozen.get(p1) == 6 && frozen.get(p2) == 8 && frozen.get(p3) == 7,
                "Frozen geometry combines updated values, retained overlap, changed membership and coordinate order");
        view.commitHalos(); view.resetPreparation();
        helper.assertTrue(pages.haloReferences(p1) == 1 && view.signatureAtWorld(1, 64, 0) == 6,
                "Removing one old overlap keeps the other owner's new signature");
        view.stageHalo(a, null);
        helper.assertTrue(view.signatureAtWorld(0, 64, 0) == ThermalSignatureTable.UNRESOLVED
                        && view.signatureAtWorld(1, 64, 0) == ThermalSignatureTable.UNRESOLVED
                        && view.signatureAtWorld(2, 64, 0) == 8, "Deleting the last owners clears only their positions");
        view.commitHalos(); view.resetPreparation();
        helper.assertTrue(pages.haloReferences(p0) == 0 && pages.haloReferences(p1) == 0
                        && pages.haloReferences(p2) == 1 && pages.haloReferences(p3) == 1,
                "Private and committed membership agree after deletion");
        helper.succeed();
    }
}
