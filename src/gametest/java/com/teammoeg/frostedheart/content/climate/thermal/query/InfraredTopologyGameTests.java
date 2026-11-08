/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.query;

import java.util.List;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalBrickCellLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Production query-directory changes must invalidate previously displayed material Bricks. */
@GameTestHolder("frostedheart_infrared_refresh")
@PrefixGameTestTemplate(false)
public final class InfraredTopologyGameTests {
    private InfraredTopologyGameTests() { }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void retiredRegionInvalidatesItsBrickWhileThePageRemainsReadable(GameTestHelper helper) {
        var arena = new ThermalCellArena(8);
        int retired = material(arena, 0, 0), survivor = material(arena, 0, 4);
        var first = field(arena, new RegionalQuerySnapshot.Handle(1), new int[]{retired}, new double[]{20}, 1);
        var second = field(arena, new RegionalQuerySnapshot.Handle(2), new int[]{survivor}, new double[]{40}, 1);
        var budget = new ThermalMemoryBudget(8L << 20);
        var query = QueryPublication.createRegional(budget.createCategory(), 4);
        var cursor = new QueryPublication.ReadCursor();
        try {
            query.noteInfraredRequest(1, 80);
            install(helper, query, List.of(), List.of(first, second), 1, 1);
            query.noteInfraredRequest(2, 80);
            helper.assertTrue(query.beginRead(cursor), "The initial material display must be readable");
            int baseline = cursor.infraredEpoch();
            cursor.clear();

            install(helper, query, List.of(first), List.of(), 2, 3);
            helper.assertTrue(query.beginRead(cursor) && cursor.hasFreshSample(3, 40),
                    "Another region keeps this Page and the global display readable after retirement");
            helper.assertTrue(cursor.brickChangeEpoch(0, 0) > baseline,
                    "A retired region must invalidate its old Brick even when Page presence does not change");
            helper.assertTrue(cursor.brickChangeEpoch(0, 1) <= baseline,
                    "An unrelated surviving region does not need a temperature resend");
            var sample = new QueryPublication.MutableSample();
            helper.assertTrue(!cursor.tryRead(retired, arena.lifecycleGeneration(retired), 0, sample),
                    "The retired material really disappeared from the production query directory");
            helper.assertTrue(cursor.tryRead(survivor, arena.lifecycleGeneration(survivor), 0, sample)
                            && sample.temperatureC() == 40,
                    "The other region retains its actual material temperature");
            helper.succeed();
        } finally {
            cursor.clear();
            query.close();
        }
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void shrinkingRegionInvalidatesBricksAbsentFromItsReplacement(GameTestHelper helper) {
        var arena = new ThermalCellArena(8);
        int retired = material(arena, 0, 0), survivor = material(arena, 0, 4);
        var handle = new RegionalQuerySnapshot.Handle(1);
        var initial = field(arena, handle, new int[]{retired, survivor}, new double[]{20, 40}, 1);
        var budget = new ThermalMemoryBudget(8L << 20);
        var query = QueryPublication.createRegional(budget.createCategory(), 4);
        var cursor = new QueryPublication.ReadCursor();
        try {
            query.noteInfraredRequest(1, 80);
            install(helper, query, List.of(), List.of(initial), 1, 1);
            query.noteInfraredRequest(2, 80);
            helper.assertTrue(query.beginRead(cursor), "The initial material display must be readable");
            int baseline = cursor.infraredEpoch();
            cursor.clear();

            var replacement = field(arena, handle, new int[]{survivor}, new double[]{40}, 3);
            install(helper, query, List.of(initial), List.of(replacement), 2, 3);
            helper.assertTrue(query.beginRead(cursor) && cursor.hasFreshSample(3, 40),
                    "Shrinking leaves a fresh material Page with the same region identity");
            helper.assertTrue(cursor.brickChangeEpoch(0, 0) > baseline,
                    "The replacement must clear an old Brick that is absent from its new footprint");
            helper.assertTrue(cursor.brickChangeEpoch(0, 1) > baseline,
                    "The current footprint still receives its normal topology update");
            helper.succeed();
        } finally {
            cursor.clear();
            query.close();
        }
    }

    private static int material(ThermalCellArena arena, int page, int x) {
        var cells = new ThermalBrickCellLayout();
        cells.reset(x, 64, 0);
        cells.addMaterialCell(x, 64, 0, 100, 0);
        var span = arena.stageBrickCells(page, 1, cells, 0, 0, 8);
        arena.stageMaterialLaw(span.firstSlot(), 1, MaterialThermalLaw.sensible(100), 0);
        arena.commitStagedCells(span);
        return span.firstSlot();
    }

    private static RegionalQuerySnapshot field(ThermalCellArena arena, RegionalQuerySnapshot.Handle handle,
            int[] slots, double[] temperatures, long tick) {
        var air = new AirFieldLayout(new AirFieldLayout.Component[0], new Long2ObjectOpenHashMap<>(), 0);
        var preparing = new RegionalQuerySnapshot.TemplatePreparation(handle, air, slots, arena, slot -> 0);
        while (!preparing.run(Long.MAX_VALUE)) { }
        double[] enthalpy = temperatures.clone();
        for (int i = 0; i < enthalpy.length; i++) enthalpy[i] *= 100;
        return new RegionalQuerySnapshot(preparing.result(), new double[0], enthalpy,
                new byte[slots.length], new boolean[slots.length], arena, 0, tick, 1, null);
    }

    private static void install(GameTestHelper helper, QueryPublication query, List<RegionalQuerySnapshot> removed,
            List<RegionalQuerySnapshot> added, long topology, long tick) {
        try (var change = query.prepareRegionInstallation(removed, added)) {
            helper.assertTrue(change != null, "The production query installation must be available");
            while (!change.run(Long.MAX_VALUE)) { }
            helper.assertTrue(query.installRegions(change, topology, tick, frames -> {
                for (var frame : frames) frame.template.handle.publish(frame, tick, 0);
            }), "The production query installation must commit");
        }
    }
}
