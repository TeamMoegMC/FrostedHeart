/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MaterialSectionState;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPhaseController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** A completed physical phase must commit and continue cooling through the real world state. */
@GameTestHolder("frostedheart_blocked_phase")
@PrefixGameTestTemplate(false)
public final class ThermalBlockedPhaseProductionGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 28000)
    public static void completedInteriorWaterCommitsPreservesEnergyAndRetires(GameTestHelper helper) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        boolean daylight = level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        int randomTicks = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(3, level.getServer());
        var position = new BlockPos(12296, 199, 12296);
        level.setChunkForced(position.getX() >> 4, position.getZ() >> 4, true);
        // A covered real pool keeps vanilla surface sampling from changing the
        // deliberately interior liquid boundary during the maintenance observation.
        for (int y = -1; y <= 1; y++) for (int z = -3; z <= 3; z++) for (int x = -3; x <= 3; x++)
            level.setBlockAndUpdate(position.offset(x, y, z),
                    (y == 0 && Math.abs(x) < 3 && Math.abs(z) < 3 ? Blocks.WATER : Blocks.STONE).defaultBlockState());
        var chunk = level.getChunkAt(position);
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.FROZEN_RIVER);
        chunk.fillBiomesFromNoise((qx, qy, qz, sampler) -> biome, level.getChunkSource().randomState().sampler());
        helper.assertTrue(MinecraftThermalInput.tryMaterialPhaseAtRandomTick(level, chunk, position,
                        Blocks.WATER.defaultBlockState()) == MinecraftPhaseController.PhaseAttempt.GAMEPLAY
                        && level.getBlockState(position).is(Blocks.WATER),
                "Untracked natural interior water retains its river-boundary rule");
        var bankWater = position.east(2);
        helper.assertTrue(MinecraftThermalInput.tryMaterialPhaseAtRandomTick(level, chunk, bankWater,
                        Blocks.WATER.defaultBlockState()) == MinecraftPhaseController.PhaseAttempt.CHANGED,
                "Untracked natural bank water still starts its ordinary freezing transition");
        level.setBlockAndUpdate(bankWater, Blocks.WATER.defaultBlockState());
        int section = position.getY() >> 4;
        int local = local(position);
        var initial = ThermalMaterialBehaviorGameTests.storedBrick(helper, position, 0);
        int index = initial.find(local);
        helper.assertTrue(index >= 0, "The real water body is present in the solver checkpoint");
        var law = initial.law(index);
        var edge = law.cooling();
        helper.assertTrue(edge != null && edge.transitionTemperatureC() == -5,
                "Use the production source-water freezing recipe");
        helper.assertTrue(!MinecraftPhaseController.applyDormantTransition(level, position,
                        Blocks.WATER.defaultBlockState(), edge,
                        (edge.sourceEnthalpyJ() + edge.targetEnthalpyJ()) * .5, level.getGameTime())
                        && level.getBlockState(position).is(Blocks.WATER),
                "A half-completed latent interval must not bypass its remaining energy");
        var editor = new MaterialSectionState.Editor(initial);
        for (int i = 0; i < initial.size(); i++)
            if (initial.position(i) != local) editor.remove(initial.position(i));
        editor.update(local, initial.stateId(index), law, edge.targetEnthalpyJ(),
                MaterialThermalLaw.COOLING, level.getGameTime());
        var stored = new DormantChunkThermalState(chunk.getMinSection(), chunk.getSections().length);
        stored.replaceMaterials(section, editor.snapshot());
        var serialized = new CompoundTag();
        stored.encode(serialized);
        stored = DormantChunkThermalState.decode(serialized, chunk.getMinSection(), chunk.getSections().length);
        MinecraftThermalInput.setDormantState(chunk, stored);
        long started = level.getGameTime();
        var sample = new MaterialSample();
        FHMain.LOGGER.info("THERMAL_BLOCKED_PHASE_START,tick={},position={},naturalC={},plateauC={},completeH={}",
                started, position, natural(helper, position), edge.transitionTemperatureC(), edge.targetEnthalpyJ());
        for (int delay = 2000; delay <= 24000; delay += 2000) {
            int elapsed = delay;
            helper.runAfterDelay(delay, () -> {
                boolean readable = MinecraftThermalInput.sampleMaterial(level, position, sample);
                FHMain.LOGGER.info("THERMAL_BLOCKED_PHASE_SAMPLE,elapsed={},state={},readable={},source={},temperatureC={},enthalpyJ={},branch={},naturalC={},statistics={}",
                        elapsed, level.getBlockState(position), readable, sample.source(), sample.temperatureC(), sample.enthalpyJ(),
                        sample.branch(), natural(helper, position), DormantThermalMaintenance.statistics());
            });
        }
        helper.runAfterDelay(500, () -> helper.assertTrue(!level.getBlockState(position).is(Blocks.WATER),
                "Completed saved interior water must commit without changing its liquid neighbors"));
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                        Block.getId(level.getBlockState(position)) == edge.targetStateId(),
                        "Complete physical freezing must commit while the water is still interior"))
                .thenExecute(() -> {
            helper.assertTrue(MinecraftThermalInput.enabledPhysicalSourceCount(level) == 0,
                    "The saved H, without a new source, completes its actual conversion");
            helper.assertTrue(level.getFluidState(position.west()).is(net.minecraft.tags.FluidTags.WATER)
                            && level.getFluidState(position.east()).is(net.minecraft.tags.FluidTags.WATER)
                            && level.getFluidState(position.north()).is(net.minecraft.tags.FluidTags.WATER)
                            && level.getFluidState(position.south()).is(net.minecraft.tags.FluidTags.WATER),
                    "The funded transition does not require constructing a bank");
            var committed = MinecraftThermalInput.dormantState(chunk).materials(section);
            int converted = committed.find(local);
            helper.assertTrue(converted >= 0 && committed.stateId(converted) == edge.targetStateId()
                            && Double.doubleToLongBits(committed.enthalpyJ(converted))
                                    == Double.doubleToLongBits(edge.targetEnthalpyJ()),
                    "The actual world mutation carries the same H and does not pay latent heat twice");
            FHMain.LOGGER.info("THERMAL_COMPLETED_PHASE_COMMIT,elapsedTicks={},enthalpyJ={},state={}",
                    level.getGameTime() - started, committed.enthalpyJ(converted), level.getBlockState(position));
        }).thenIdle(200).thenExecute(() -> {
            helper.assertTrue(MinecraftThermalInput.sampleMaterial(level, position, sample)
                            && sample.temperatureC() < edge.transitionTemperatureC() - 1,
                    "The committed body must continue losing sensible heat below the old plateau");
            FHMain.LOGGER.info("THERMAL_COMPLETED_PHASE_COOLING,elapsedTicks={},temperatureC={},enthalpyJ={}",
                    level.getGameTime() - started, sample.temperatureC(), sample.enthalpyJ());
        }).thenIdle(25000).thenWaitUntil(() -> helper.assertTrue(
                        level.getBlockState(position).is(Blocks.ICE)
                                && MinecraftThermalInput.dormantState(chunk) == null,
                        "The remaining real ice-stage latent interval finishes and ordinary cold history releases"))
                .thenExecute(() -> {
                    FHMain.LOGGER.info("THERMAL_COMPLETED_PHASE_RETIRED,elapsedTicks={},state={},temperatureC={},statistics={}",
                            level.getGameTime() - started, level.getBlockState(position), WorldTemperature.material(level, position),
                            DormantThermalMaintenance.statistics());
                    MinecraftThermalInput.setDormantState(chunk, null);
                    level.setChunkForced(position.getX() >> 4, position.getZ() >> 4, false);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(daylight, level.getServer());
                    level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(randomTicks, level.getServer());
                }).thenSucceed();
    }

    private static int local(BlockPos p) { return (p.getX() & 15) | (p.getZ() & 15) << 4 | (p.getY() & 15) << 8; }

    private static double natural(GameTestHelper helper, BlockPos p) {
        return WorldTemperature.naturalAir(helper.getLevel(), new BlockPos((p.getX() & ~15) + 8,
                (p.getY() & ~15) + 8, (p.getZ() & ~15) + 8));
    }
}
