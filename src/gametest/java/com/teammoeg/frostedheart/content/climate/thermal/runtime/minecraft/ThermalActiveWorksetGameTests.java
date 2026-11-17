/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPageManager;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftThermalSectionAttachment;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Resource ownership in the real existing runtime, with a heated control base. */
@GameTestHolder("frostedheart_active_workset")
@PrefixGameTestTemplate(false)
public final class ThermalActiveWorksetGameTests {
    @GameTest(template = "phase0a_empty", batch = "thermal_active_workset", timeoutTicks = 900)
    public static void coldChunksAndColdChangesDoNotCreateThermalWork(GameTestHelper helper) {
        var level = helper.getLevel(); MinecraftThermalInput.closeActiveLevel(level);
        var origin = new BlockPos(-40000, 192, -40000);
        var fire = origin.offset(8, 1, 8);
        for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) level.setChunkForced((origin.getX() >> 4) + x, (origin.getZ() >> 4) + z, true);
        ContinuousAirProductionGameTests.room(helper, origin);
        ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
        var sample = new ThermalEnvironmentSample(); var cold = new LevelChunk[32];
        helper.startSequence().thenIdle(60).thenWaitUntil(() -> {
            MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, fire.above(), WorldTemperature.naturalAir(level, fire.above()), sample);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample), "Keep an actual committed control base heated throughout the cold workload");
        }).thenExecute(() -> {
            try {
                var input = ThermalLoadedWorldGameTests.owner(level, fire).input();
                MinecraftPageManager pages = ThermalLoadedWorldGameTests.read(input, "pages");
                Object environment = ThermalLoadedWorldGameTests.read(input, "environment");
                LongLinkedOpenHashSet dirtySky = ThermalLoadedWorldGameTests.read(environment, "dirtyColumns");
                for (int index = 0; index < cold.length; index++) {
                    int x = (-42000 >> 4) + index % 8, z = (-40000 >> 4) + index / 8;
                    level.setChunkForced(x, z, true); cold[index] = level.getChunk(x, z);
                }
                int eagerOwners = attachedOwners(cold);
                int beforeSky = dirtySky.size();
                for (var chunk : cold) {
                    // A real geometry/radiation reader may explicitly observe a
                    // cold Section. That must still not create a thermal Page.
                    long section = SectionPos.asLong(chunk.getPos().x, 12, chunk.getPos().z);
                    var owner = pages.loadedSectionOrAttach(section);
                    helper.assertTrue(owner != null && owner.page() == null, "Explicit observation watches mutations without activating a cold numerical field");
                    for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) {
                        var position = new BlockPos(chunk.getPos().getMinBlockX() + x, 196, chunk.getPos().getMinBlockZ() + z);
                        var block = level.getBlockState(position).is(Blocks.STONE) ? Blocks.AIR : Blocks.STONE;
                        helper.assertTrue(level.setBlockAndUpdate(position, block.defaultBlockState()), "Every cold construction event actually changes a block");
                    }
                }
                pages.tick(level.getGameTime());
                int afterSky = dirtySky.size();
                MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, fire.above(), WorldTemperature.naturalAir(level, fire.above()), sample);
                FHMain.LOGGER.info("THERMAL_ACTIVE_WORKSET_RESULT,coldChunks=32,eagerOwners={},explicitColdOwners={},blockChanges=2048,skyBefore={},skyAfter={},controlAvailable={}",
                        eagerOwners, attachedOwners(cold), beforeSky, afterSky, ContinuousAirProductionTiming.sourceField(sample));
                helper.assertTrue(eagerOwners == 0, "Cold Chunk loading creates no unused per-Section thermal owners; observed=" + eagerOwners);
                helper.assertTrue(attachedOwners(cold) == cold.length, "Only the explicitly observed cold Sections own mutation inboxes");
                helper.assertTrue(afterSky <= beforeSky, "Cold mutations do not build a sky-refresh backlog for unrelated thermal columns; before=" + beforeSky + ",after=" + afterSky);
                helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample), "The original public query still serves the heated control base");
            } finally {
                level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
                MinecraftThermalInput.closeActiveLevel(level);
                for (var chunk : cold) if (chunk != null) level.setChunkForced(chunk.getPos().x, chunk.getPos().z, false);
                for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) level.setChunkForced((origin.getX() >> 4) + x, (origin.getZ() >> 4) + z, false);
            }
        }).thenSucceed();
    }

    private static int attachedOwners(LevelChunk[] chunks) {
        int count = 0;
        for (var chunk : chunks) for (var section : chunk.getSections())
            if (((MinecraftThermalSectionAttachment) (Object) section).frostedheart$getThermalInputOwner() != null) count++;
        return count;
    }
}
