/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Run twice against one actual world: first write, then load the disk record in a new JVM. */
@GameTestHolder("frostedheart_retirement_restart")
@PrefixGameTestTemplate(false)
public final class ThermalRetirementRestartGameTests {
    private ThermalRetirementRestartGameTests() { }

    @GameTest(template = "phase0a_empty", timeoutTicks = 2400)
    public static void diskRestartPreservesHotStateThenPermanentlyDeletesExpiredRecords(GameTestHelper helper) {
        var level = helper.getLevel();
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        BlockPos hot = new BlockPos(16385, 196, 16385), cold = hot.east();
        level.setChunkForced(hot.getX() >> 4, hot.getZ() >> 4, true);
        var chunk = level.getChunkAt(hot);
        var observation = level.getDataStorage().computeIfAbsent(Observation::load, Observation::new,
                "frostedheart_retirement_restart");
        int section = hot.getY() >> 4, local = (hot.getX() & 15) | (hot.getZ() & 15) << 4 | (hot.getY() & 15) << 8;
        if (observation.phase == 0) {
            double natural = WorldTemperature.naturalAir(level,
                    new BlockPos((hot.getX() & ~15) + 8, (hot.getY() & ~15) + 8, (hot.getZ() & ~15) + 8));
            var stored = ThermalRetirementProductionGameTests.fixture(helper,
                    new BlockPos[]{hot, cold}, new double[]{natural + 2, natural + .2});
            var material = stored.materials(section); int index = material.find(local);
            observation.energy = Double.doubleToRawLongBits(material.enthalpyJ(index));
            observation.tick = material.savedTick(index); observation.branch = material.branch(index);
            observation.phase = 1; observation.setDirty();
            MinecraftThermalInput.setDormantState(chunk, stored);
            chunk.setUnsaved(true);
            level.getServer().saveEverything(true, true, true);
            FHMain.LOGGER.info("THERMAL_RETIREMENT_RESTART_WRITE,tick={},energyBits={},branch={}",
                    observation.tick, observation.energy, observation.branch);
            helper.succeed(); return;
        }
        if (observation.phase == 1) {
            helper.assertTrue(level.getBlockState(hot).is(Blocks.STONE), "The second JVM loads the actual saved world block");
            var stored = MinecraftThermalInput.dormantState(chunk);
            helper.assertTrue(stored != null && stored.materials(section) != null, "Hot checkpoint survives a new process");
            var material = stored.materials(section); int index = material.find(local);
            helper.assertTrue(index >= 0 && Double.doubleToRawLongBits(material.enthalpyJ(index)) == observation.energy
                            && material.savedTick(index) == observation.tick && material.branch(index) == observation.branch,
                    "Disk round trip keeps exact H, phase branch and the original cooling clock");
            helper.assertTrue(Double.isFinite(WorldTemperature.material(level, hot)), "The restored hot body is readable");
            helper.startSequence().thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.dormantState(chunk) == null,
                    "Real server ticks retire the loaded residual without any physical runtime"))
                    .thenExecute(() -> {
                        observation.phase = 2; observation.setDirty(); chunk.setUnsaved(true);
                        level.getServer().saveEverything(true, true, true);
                        FHMain.LOGGER.info("THERMAL_RETIREMENT_RESTART_DELETE,tick={},statistics={}",
                                level.getGameTime(), DormantThermalMaintenance.statistics());
                    }).thenSucceed(); return;
        }
        helper.assertTrue(MinecraftThermalInput.dormantState(chunk) == null,
                "The third JVM does not resurrect the deleted thermal tag");
        helper.assertTrue(level.getBlockState(hot).is(Blocks.STONE) && level.getBlockState(cold).is(Blocks.STONE),
                "Deleting history preserves the real world blocks");
        FHMain.LOGGER.info("THERMAL_RETIREMENT_RESTART_EMPTY,tick={}", level.getGameTime());
        helper.succeed();
    }

    private static final class Observation extends SavedData {
        int phase;
        long energy, tick;
        byte branch;
        static Observation load(CompoundTag tag) {
            var value = new Observation(); value.phase = tag.getInt("phase");
            value.energy = tag.getLong("energy"); value.tick = tag.getLong("tick"); value.branch = tag.getByte("branch");
            return value;
        }
        @Override public CompoundTag save(CompoundTag tag) {
            tag.putInt("phase", phase); tag.putLong("energy", energy); tag.putLong("tick", tick); tag.putByte("branch", branch);
            return tag;
        }
    }
}
