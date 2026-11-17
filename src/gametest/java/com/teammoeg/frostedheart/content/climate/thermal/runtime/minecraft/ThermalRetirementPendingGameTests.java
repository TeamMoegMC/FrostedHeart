/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.PendingHeatCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_retirement_pending")
@PrefixGameTestTemplate(false)
public final class ThermalRetirementPendingGameTests {
    private ThermalRetirementPendingGameTests() { }

    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void acceptingTheLastColdInputReleasesItsEmptyChunkAttachment(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos p = new BlockPos(24577, 196, 24577);
        level.setChunkForced(p.getX() >> 4, p.getZ() >> 4, true);
        level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
        var chunk = level.getChunkAt(p);
        long tick = level.getGameTime();
        var input = new SpatialHeatInput.Builder();
        input.appendAggregate(new SpatialHeatInput.Target(p.asLong(), (byte) 0, -1, 0, 0, 0, 0, 0), tick, tick + 20, 20);
        var pending = PendingHeatCheckpoint.capture(input.freeze(), ignored -> -1, tick);
        var stored = new DormantChunkThermalState(chunk.getMinSection(), chunk.getSections().length);
        stored.setPending(pending, pending, 0);
        MinecraftThermalInput.setDormantState(chunk, stored);
        MinecraftThermalInput.onChunkLoad(level, chunk);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(stored.coldPending().empty(),
                "The real worker intake must accept the finite saved energy before cleanup"))
                .thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.dormantState(chunk) == null,
                        "An input-only checkpoint must release its now-empty Chunk attachment after ownership transfers"))
                .thenExecute(() -> {
                    helper.assertTrue(MinecraftThermalInput.enabledPhysicalSourceCount(level) == 0,
                            "Restoring saved J must not create a physical emitter");
                    level.setChunkForced(p.getX() >> 4, p.getZ() >> 4, false);
                    FHMain.LOGGER.info("THERMAL_RETIREMENT_PENDING,statistics={},worker={}",
                            DormantThermalMaintenance.statistics(), MinecraftThermalInput.describeWorkerProgress(level));
                }).thenSucceed();
    }
}
