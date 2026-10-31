/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ArenaSpan;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.InstallToken;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalFragment;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSolver;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_local_material_topology")
@PrefixGameTestTemplate(false)
public final class LocalMaterialTopologyGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void physicalFacesHaveOneOwnerAcrossSectionsAndCancellation(GameTestHelper helper) {
        // Both positive and negative section coordinates; all three section boundaries intersect this box.
        int contacts = verify(helper, 12) + verify(helper, -4);
        FHMain.LOGGER.info("THERMAL_LOCAL_MATERIAL_TOPOLOGY,fixtures=2,bricks=16,contacts={},duplicateOwners=0,cancelAndCommit=true,retiredReferences=0", contacts);
        helper.succeed();
    }

    private static int verify(GameTestHelper helper, int origin) {
        var arena = new ThermalCellArena(64);
        var signatures = ThermalSignatureTable.builder();
        int air = signatures.intern(new ResolvedThermalSignature(100, 0));
        int firstMaterial = signatures.intern(new ResolvedThermalSignature(0, 1));
        int secondMaterial = signatures.intern(new ResolvedThermalSignature(0, 2));
        var catalog = signatures.build();
        var materials = new MaterialBoundaryRegistry(List.of(
                new MaterialBoundaryRegistry.Profile(1, .125, MaterialThermalLaw.sensible(100)),
                new MaterialBoundaryRegistry.Profile(2, 8, MaterialThermalLaw.sensible(200))));
        var positions = new HashMap<BlockPos, Integer>();
        int[][] blocks = {{1, 1, 1}, {3, 2, 2}, {4, 2, 2}, {2, 3, 5}, {2, 4, 5}, {5, 5, 3}, {5, 5, 4}};
        for (int index = 0; index < blocks.length; index++) {
            int[] block = blocks[index];
            positions.put(new BlockPos(origin + block[0], origin + block[1], origin + block[2]), (index & 1) + 1);
        }
        try (var pages = new WorkerPageStore(8)) {
            var draftsBySection = new Long2ObjectOpenHashMap<TopologyUpdatePlanner.PageDraft>();
            var draftsBySlot = new Int2ObjectOpenHashMap<TopologyUpdatePlanner.PageDraft>();
            var view = new TopologyView(pages, draftsBySection, draftsBySlot);
            view.airRoutes(new AirRouteCompiler(catalog, arena, 4096));
            var parameters = new ThermalTopologyParameters(100, 0, 1, new BuoyancyConductance.Parameters(1, 1, 1), 64, 64);
            var compiler = new BrickTopologyCompiler(arena, catalog, materials, parameters, new FarFieldSettings(1, 16), 4096, ignored -> { });
            var pageList = new ArrayList<WorkerPageStore.PageState>();
            for (int y = origin; y < origin + 8; y += 4) for (int z = origin; z < origin + 8; z += 4)
                for (int x = origin; x < origin + 8; x += 4) {
                    int brick = (x & 15) >>> 2 | ((z & 15) >>> 2) << 2 | ((y & 15) >>> 2) << 4;
                    int[] ids = new int[64];
                    for (int block = 0; block < ids.length; block++) {
                        int profile = positions.getOrDefault(new BlockPos(x + (block & 3), y + (block >>> 4), z + (block >>> 2 & 3)), 0);
                        ids[block] = profile == 0 ? air : profile == 1 ? firstMaterial : secondMaterial;
                    }
                    var cut = PageSignatures.unresolved(catalog).withBricks(catalog, new int[]{brick}, new int[][]{ids});
                    var handle = new ThermalPageHandle(SectionPos.asLong(x >> 4, y >> 4, z >> 4), 1);
                    var page = pages.stageAdmission(new ThermalInputBatch.PageAdmission(handle, 0, 1L << brick, 0, cut, 0, new byte[256], null));
                    var draft = new TopologyUpdatePlanner.PageDraft();
                    draft.page = page; draft.nextSignatures = cut; draft.nextResidentBrickMask = 1L << brick;
                    draftsBySection.put(handle.sectionKey(), draft); draftsBySlot.put(page.pageSlot, draft); pageList.add(page);
                }
            var spans = new ArenaSpan[pageList.size()];
            for (int index = 0; index < pageList.size(); index++) {
                var page = pageList.get(index); int brick = Long.numberOfTrailingZeros(page.residentBrickMask);
                var compiled = compiler.compileCells(page, page.signatures, brick, view);
                draftsBySlot.get(page.pageSlot).replacements[brick] = compiled;
                spans[index] = compiled.span; arena.commitStagedCells(compiled.span);
            }
            // Independent oracle: enumerate the six faces of each material block, not the compiler's Brick traversal.
            var expected = new HashMap<Long, Double>();
            var faceCounts = new HashMap<Long, Integer>();
            int acrossSections = 0;
            for (var entry : positions.entrySet()) for (var face : Direction.values()) {
                BlockPos position = entry.getKey(), neighbor = position.relative(face);
                int first = slot(view, position), second = slot(view, neighbor);
                Integer other = positions.get(neighbor);
                if (other != null && first > second) continue;
                double conductance = materials.profileOrNull(entry.getValue()).faceConductanceWPerK();
                if (other != null) {
                    double next = materials.profileOrNull(other).faceConductanceWPerK();
                    conductance = 2 * conductance * next / (conductance + next);
                    if (!SectionPos.of(position).equals(SectionPos.of(neighbor))) acrossSections++;
                }
                long key = key(first, second);
                expected.merge(key, conductance, Double::sum); faceCounts.merge(key, 1, Integer::sum);
            }
            helper.assertTrue(acrossSections == 3 && faceCounts.values().stream().anyMatch(count -> count == 6),
                    "Fixture covers three section boundaries and six faces merged to one Air component");
            var solver = new ThermalSolver(arena, 8, 8);
            var fragments = new ThermalFragment[pageList.size()];
            var observed = new HashMap<Long, Double>();
            for (int index = 0; index < pageList.size(); index++) {
                var page = pageList.get(index); int brick = Long.numberOfTrailingZeros(page.residentBrickMask);
                fragments[index] = compiler.compileFragment(page, brick, view).fragment();
                var contacts = fragments[index].materialContacts();
                for (int edge = 0; edge < contacts.size(); edge++) {
                    long key = key(contacts.first(edge), contacts.second(edge));
                    helper.assertTrue(observed.put(key, contacts.conductance(edge)) == null, "Every endpoint pair has exactly one owning Brick");
                }
                solver.installFragment(index, fragments[index]);
            }
            helper.assertTrue(expected.keySet().equals(observed.keySet()), "Compiled contacts cover every physical material face");
            for (var entry : expected.entrySet()) helper.assertTrue(
                    Double.doubleToRawLongBits(entry.getValue()) == Double.doubleToRawLongBits(observed.get(entry.getKey())),
                    "Local contact sums preserve the original face conductance");

            int[] indexes = new int[fragments.length]; var empty = new ThermalFragment[fragments.length];
            for (int index = 0; index < indexes.length; index++) { indexes[index] = index; empty[index] = ThermalFragment.EMPTY; }
            helper.assertTrue(solver.preflightReplacement(indexes, empty, spans).pairOperations() == 0,
                    "Removing the complete physical graph retires every endpoint");
            var cancelled = new InstallToken(); solver.stageFragment(0, ThermalFragment.EMPTY, cancelled);
            helper.assertTrue(solver.fragment(0) == fragments[0], "Uncommitted replacement cannot change readers");
            cancelled.cancel(); solver.finishFragment(0, cancelled);
            helper.assertTrue(solver.fragment(0) == fragments[0], "Cancellation retains all old contacts");
            var committed = new InstallToken();
            for (int index : indexes) solver.stageFragment(index, ThermalFragment.EMPTY, committed);
            committed.commit();
            helper.assertTrue(solver.preflightReplacement(new int[0], new ThermalFragment[0], new ArenaSpan[0]).pairOperations() == 0,
                    "Committed graph counts are current before cleanup finishes");
            for (int index : indexes) {
                helper.assertTrue(solver.fragment(index) == ThermalFragment.EMPTY, "One token publishes the full replacement");
                solver.finishFragment(index, committed);
            }
            for (int index = 0; index < spans.length; index++) {
                helper.assertTrue(!solver.references(spans[index]), "Retired geometry has no remaining fragment references");
                var page = pageList.get(index); arena.releasePageCells(page.pageSlot, page.lifecycleGeneration, spans[index]);
                pages.releaseStagedAdmission(page);
            }
            helper.assertTrue(arena.liveCellCount() == 0, "All fixture cells retire");
            helper.assertTrue(fragments[0].materialContacts().size() > 0, "Retained immutable readers survive replacement cleanup");
            return observed.size();
        }
    }

    private static int slot(TopologyView view, BlockPos position) {
        var brick = view.brickAtWorld(position.getX(), position.getY(), position.getZ());
        return brick.slotAt((position.getX() & 3) | (position.getZ() & 3) << 2 | (position.getY() & 3) << 4);
    }
    private static long key(int first, int second) { return (long) Math.min(first, second) << 32 | Math.max(first, second) & 0xffffffffL; }
}
