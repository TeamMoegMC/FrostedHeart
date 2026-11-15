/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import java.util.Arrays;

import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialEnergyBanks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The field perturbs a per-block natural background: no flow at rest, and background changes reach every cell. */
@GameTestHolder("frostedheart_climate_cover")
@PrefixGameTestTemplate(false)
public final class NaturalBackgroundFieldGameTests {
    private NaturalBackgroundFieldGameTests() {
    }

    @GameTest(template = "phase0a_empty", batch = "climate_cover_field", timeoutTicks = 200)
    public static void differentBackgroundsExchangeNothingAtRest(GameTestHelper helper) {
        var profiles = MinecraftThermalProfiles.prepare();
        var budget = new ThermalMemoryBudget(8L << 20);
        var base = aligned(helper, 4864);
        long west = SectionPos.asLong(base.offset(15, 0, 0)), east = SectionPos.asLong(base.offset(16, 0, 0));
        int[] westCapture = airColumns(helper, profiles, base, 12, 16), eastCapture = airColumns(helper, profiles, base, 16, 20);
        var naturals = new NaturalProfiles();
        // A 30 °C step across the Section boundary and a gradient across the cover of the western Section.
        naturals.put(west, profile(SectionPos.y(west) << 4, 10, true));
        naturals.put(east, profile(SectionPos.y(east) << 4, -20, false));
        try (var banks = new SpatialEnergyBanks(budget, 8, 128, 0); var field = field(budget, profiles, naturals)) {
            helper.assertTrue(field.admit(west, westCapture, 1L << 3, 0) && field.admit(east, eastCapture, 1L, 0),
                    "admit both sides of the boundary");
            double before = field.totalEnthalpyJ();
            for (long tick = 20; tick <= 400; tick += 20) step(field, banks, tick);
            var sample = new TiledThermalField.Sample();
            for (int x = 12; x < 20; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) {
                BlockPos position = base.offset(x, y, z);
                helper.assertTrue(field.read(position.asLong(), false, sample), "read the air cell at " + position);
                near(helper, sample.naturalC(), sample.temperatureC(), 1e-4, "a cell at rest stays on its own natural at " + position);
                helper.assertTrue(field.sampleAir(position.getX() + .5, position.getY() + .5, position.getZ() + .5, sample),
                        "query the air cell at " + position);
                near(helper, sample.naturalC(), sample.temperatureC(), 1e-4, "a query at rest reads the natural at " + position);
            }
            near(helper, before, field.totalEnthalpyJ(), 1e-3, "no energy moves between different backgrounds at rest");
        }
        helper.assertTrue(budget.usedBytes() == 0, "field storage is released");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "climate_cover_field", timeoutTicks = 200)
    public static void backgroundChangeMovesEveryCellWithoutLeavingAPerturbation(GameTestHelper helper) {
        var profiles = MinecraftThermalProfiles.prepare();
        var budget = new ThermalMemoryBudget(8L << 20);
        var base = aligned(helper, 5120);
        long section = SectionPos.asLong(base.offset(12, 0, 0));
        int[] captured = airColumns(helper, profiles, base, 12, 16);
        var naturals = new NaturalProfiles();
        int minimumY = SectionPos.y(section) << 4;
        naturals.put(section, profile(minimumY, -10, false));
        try (var banks = new SpatialEnergyBanks(budget, 8, 128, 0); var field = field(budget, profiles, naturals)) {
            helper.assertTrue(field.admit(section, captured, 1L << 3, 0), "admit the air brick");
            step(field, banks, 20);
            double energy = field.totalEnthalpyJ(), external = field.externalEnergyJ();
            naturals.put(section, profile(minimumY, -25, false));
            step(field, banks, 40);
            var sample = new TiledThermalField.Sample();
            for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) {
                BlockPos position = base.offset(13, y, z);
                helper.assertTrue(field.read(position.asLong(), false, sample), "read the air cell at " + position);
                near(helper, sample.naturalC(), sample.temperatureC(), 1e-4, "the cell follows its new background at " + position);
            }
            // Natural values are stored as floats; 64 cells of ~1.2 kJ/K allow a few joules of rounding.
            double moved = -15 * field.airCapacityJPerK() * 64;
            near(helper, energy + moved, field.totalEnthalpyJ(), 10, "the background change sets the energy of every cell");
            near(helper, external - moved, field.externalEnergyJ(), 10, "and is accounted as external energy");
        }
        helper.succeed();
    }

    /** Uniform Brick base, open-air climate {@code climate}; with {@code coverGradient}, deeper cover is warmer. */
    private static NaturalProfile profile(int minimumY, float climate, boolean coverGradient) {
        float[] brickBase = new float[64];
        byte[] cover = new byte[4096];
        if (coverGradient) for (int block = 0; block < 4096; block++) cover[block] = (byte) ((block >>> 4 & 15) % 4);
        float[] coveredClimate = {climate, climate + 3, climate + 6, climate + 9};
        return new NaturalProfile(brickBase, cover, coveredClimate, minimumY, false, -273);
    }

    private static int[] airColumns(GameTestHelper helper, MinecraftThermalProfiles.Snapshot profiles, BlockPos base,
            int fromX, int toX) {
        int[] captured = new int[4096];
        Arrays.fill(captured, com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable.UNRESOLVED);
        int air = profiles.states().signatureId(Blocks.AIR.defaultBlockState());
        for (int x = fromX; x < toX; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) {
            helper.getLevel().setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
            captured[(x & 15) | z << 4 | y << 8] = air;
        }
        return captured;
    }

    private static TiledThermalField field(ThermalMemoryBudget budget, MinecraftThermalProfiles.Snapshot profiles,
            NaturalProfiles naturals) {
        return new TiledThermalField(budget, profiles.signatures(), profiles.materials(),
                profiles.tuning().airHeatCapacityJPerBlockK(), profiles.tuning().airMixingWPerBlockK(), 0, 16, 0,
                naturals);
    }

    private static BlockPos aligned(GameTestHelper helper, int offset) {
        var position = helper.absolutePos(new BlockPos(offset, 192, 0));
        return new BlockPos(position.getX() & ~15, position.getY() & ~15, position.getZ() & ~15);
    }

    private static void step(TiledThermalField field, SpatialEnergyBanks banks, long tick) {
        if (!field.begin(banks.seal(tick))) throw new AssertionError("Could not start tiled step");
        long limit = System.nanoTime() + 5_000_000_000L;
        while (!field.advance(System.nanoTime() + 1_000_000L))
            if (System.nanoTime() > limit) throw new AssertionError("Tiled numerical step did not finish");
        if (!field.commit()) throw new AssertionError("Could not commit tiled step");
    }

    private static void near(GameTestHelper helper, double expected, double actual, double tolerance, String message) {
        helper.assertTrue(Math.abs(expected - actual) <= tolerance, message + ": expected " + expected + ", got " + actual);
    }
}
