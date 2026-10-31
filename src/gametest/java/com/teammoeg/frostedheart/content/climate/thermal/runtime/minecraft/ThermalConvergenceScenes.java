/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

final class ThermalConvergenceScenes {
    static void force(GameTestHelper helper, BlockPos origin, int width, boolean forced) {
        for (int x = (origin.getX() >> 4) - 1; x <= ((origin.getX() + width + 1) >> 4) + 1; x++)
            for (int z = (origin.getZ() >> 4) - 1; z <= ((origin.getZ() + width + 1) >> 4) + 1; z++)
                helper.getLevel().setChunkForced(x, z, forced);
    }
    /** Fixed batches of actual world edits; construction participates in ordinary server ticks. */
    static final class HallBuild {
        final GameTestHelper helper;
        final BlockPos origin;
        final int width, height;
        final BlockPos[] fires;
        int blocks, ignited, checked;
        boolean complete;
        HallBuild(GameTestHelper helper, BlockPos origin, int width) {
            this(helper, origin, width, 6);
        }
        HallBuild(GameTestHelper helper, BlockPos origin, int width, int height) {
            this(helper, origin, width, height, 1);
        }
        HallBuild(GameTestHelper helper, BlockPos origin, int sourceSide, int height, int spacing) {
            this.helper = helper; this.origin = origin; width = sourceSide * spacing; this.height = height; fires = new BlockPos[sourceSide * sourceSide];
            for (int z = 0; z < sourceSide; z++) for (int x = 0; x < sourceSide; x++)
                fires[z * sourceSide + x] = origin.offset(x * spacing + 1, 1, z * spacing + 1);
        }
        boolean advance() {
            if (complete) return true;
            var level = helper.getLevel(); int side = width + 2, count = side * side * height;
            int end = Math.min(count, blocks + 256);
            while (blocks < end) {
                // Finish the roof before clearing any cavity beneath falling terrain.
                int x = blocks % side, z = blocks / side % side, y = height - 1 - blocks++ / (side * side);
                boolean boundary = y == 0 || y == height - 1 || x == 0 || z == 0 || x == side - 1 || z == side - 1;
                level.setBlockAndUpdate(origin.offset(x, y, z), (boundary ? Blocks.STONE : Blocks.AIR).defaultBlockState());
            }
            if (blocks != count) return false;
            end = Math.min(fires.length, ignited + 16);
            while (ignited < end) fire(helper, fires[ignited++]);
            if (ignited != fires.length) return false;
            end = Math.min(fires.length, checked + 16);
            while (checked < end) {
                var fire = fires[checked]; var state = level.getBlockState(fire);
                helper.assertTrue(state.is(Blocks.CAMPFIRE) && state.getValue(CampfireBlock.LIT)
                                && level.getBlockState(fire.above()).isAir(),
                        "The real hall needs a lit campfire with an unobstructed outlet at " + fire);
                checked++;
            }
            if (checked != fires.length) return false;
            var chunks = new java.util.HashSet<net.minecraft.world.level.chunk.LevelChunk>();
            for (var fire : fires) chunks.add(level.getChunkAt(fire));
            for (var chunk : chunks) MinecraftThermalInput.onChunkLoad(level, chunk);
            return complete = true;
        }
    }
    static void fire(GameTestHelper helper, BlockPos fire) {
        helper.getLevel().setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        fuel(helper, fire, 4);
        ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
    }
    static void fuel(GameTestHelper helper, BlockPos fire, int count) {
        var entity = helper.getLevel().getBlockEntity(fire);
        if (entity == null) throw new IllegalStateException("Actual campfire disappeared: " + fire);
        var fuel = entity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(
                () -> new IllegalStateException("Actual campfire has no fuel inventory"));
        fuel.insertItem(0, new ItemStack(Items.COAL, count), false);
    }
    static java.util.function.BooleanSupplier shrink(GameTestHelper helper, BlockPos origin, int width, int height, BlockPos remaining) {
        int[] cursor = {0}; int side = width + 2, count = side * side * height;
        return () -> {
            int end = Math.min(count, cursor[0] + 256);
            while (cursor[0] < end) {
                int x = cursor[0] % side, z = cursor[0] / side % side, y = cursor[0]++ / (side * side);
                var block = origin.offset(x, y, z); if (block.equals(remaining)) continue;
                boolean wall = x <= 8 && z <= 8 && (y == 0 || y == 5 || x == 0 || z == 0 || x == 8 || z == 8);
                helper.getLevel().setBlockAndUpdate(block, (wall ? Blocks.STONE : Blocks.AIR).defaultBlockState());
            }
            return cursor[0] == count;
        };
    }
    static void read(GameTestHelper helper, BlockPos point, ThermalEnvironmentSample sample) {
        MinecraftThermalInput.gameplayPlacedReservoirEnvironment(helper.getLevel(), point,
                WorldTemperature.naturalAir(helper.getLevel(), point), sample);
    }
    static boolean fresh(GameTestHelper helper, BlockPos point, ThermalEnvironmentSample sample) {
        read(helper, point, sample);
        return sample.airAvailable() && sample.airSampleTick() >= 0 && helper.getLevel().getGameTime() - sample.airSampleTick() <= 40
                && MinecraftThermalInput.describeAirRead(helper.getLevel(), point).startsWith("geometry_current");
    }
    /** Pressure qualification checks service; age and quality remain measured tradeoffs. */
    static boolean readable(GameTestHelper helper, BlockPos point, ThermalEnvironmentSample sample) {
        read(helper, point, sample);
        return sample.airAvailable() && sample.airSampleTick() >= 0
                && sample.airSampleTick() <= helper.getLevel().getGameTime()
                && MinecraftThermalInput.describeAirRead(helper.getLevel(), point).startsWith("geometry_current");
    }
    static int step(GameTestHelper helper, BlockPos point) {
        var publication = MinecraftThermalInput.queryPublicationForObservation(helper.getLevel());
        var cursor = new QueryPublication.ReadCursor();
        try {
            return publication != null && publication.beginRead(cursor)
                    ? cursor.airAggregationSpanAt(point.getX() + .5, point.getY() + .5, point.getZ() + .5) : -1;
        } finally { cursor.clear(); }
    }
}
