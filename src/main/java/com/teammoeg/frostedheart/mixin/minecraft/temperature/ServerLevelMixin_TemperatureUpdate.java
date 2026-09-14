/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.data.PlantTempData;
import com.teammoeg.frostedheart.content.climate.data.StateTransitionData;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPhaseController;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin_TemperatureUpdate
{
    @Shadow public abstract boolean isFlat();

    /**
     * Adds our custom temperature section before iceandsnow.
     * Implement the tickBlocks section too.
     */
    @Inject(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                    ordinal = 0, // This targets the profiler.popPush("iceandsnow") call
                    shift = At.Shift.BEFORE
            ),
            cancellable = true)
    private void addTemperatureSection(LevelChunk pChunk, int pRandomTickSpeed, CallbackInfo ci)
    {
        ServerLevel level = (ServerLevel) (Object) this;
        final long now = level.getGameTime();
        ChunkPos chunkpos = pChunk.getPos();
        // ConfigValue.get() 为 spec 值表查询：同一 tick 内配置恒定，hoist 到局部变量
        int tempBlockstateUpdateIntervalTicks = FHConfig.SERVER.CLIMATE.tempBlockstateUpdateIntervalTicks.get();

        float climateBase=0;
        WorldClimate wc = WorldClimate.get(level);
        if (wc != null) {
            climateBase = wc.getTemp(chunkpos);
        }

        boolean updateTempBlock = (now + chunkpos.x + chunkpos.z)
                % tempBlockstateUpdateIntervalTicks == 0;

        boolean isRaining = level.isRaining();
        if (wc != null)
        {
            isRaining = wc.getClimate(chunkpos).isSnowyOrBlizzard();
        }

        int i = chunkpos.getMinBlockX();
        int j = chunkpos.getMinBlockZ();

        level.getProfiler().popPush("weather");
        if (pRandomTickSpeed > 0 && updateTempBlock)
        {
            BlockPos blockpos1 = level.getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING,
                    level.getBlockRandomPos(i, 0, j, 15));
            // Empty heightmap columns resolve to the dimension's minimum build height.
            // Do not move below it: dimensions such as the End use 0 rather than -64.
            BlockPos blockpos2 = blockpos1.getY() > level.getMinBuildHeight()
                    ? blockpos1.below()
                    : blockpos1;

            BlockState surface = pChunk.getBlockState(blockpos2);
            if (surface.is(Blocks.WATER)) {
                MinecraftThermalInput.tryMaterialPhaseAtRandomTick(level, pChunk, blockpos2, surface);
            }

            if (isRaining)
            {
                Biome biome = CUtils.fastGetBiome(pChunk, blockpos2).value();
                int i1 = level.getGameRules().getInt(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT);
                if (i1 > 0 && frostedHeart$shouldSnowCustom(level, blockpos1))
                {
                    BlockState blockstate = level.getBlockState(blockpos1);
                    if (blockstate.is(Blocks.SNOW))
                    {
                        int k = blockstate.getValue(SnowLayerBlock.LAYERS);
                        if (k < Math.min(i1, 8))
                        {
                            BlockState blockstate1 = blockstate.setValue(SnowLayerBlock.LAYERS, k + 1);
                            Block.pushEntitiesUp(blockstate, blockstate1, level, blockpos1);
                            level.setBlockAndUpdate(blockpos1, blockstate1);
                        }
                    }
                    else
                    {
                        level.setBlockAndUpdate(blockpos1, Blocks.SNOW.defaultBlockState());
                    }
                }

                Biome.Precipitation biome$precipitation = biome.getPrecipitationAt(blockpos2);
                if (biome$precipitation != Biome.Precipitation.NONE)
                {
                    BlockState blockstate3 = level.getBlockState(blockpos2);
                    blockstate3.getBlock().handlePrecipitation(blockstate3, level, blockpos2, biome$precipitation);
                }
            }
        }

        // Add temperature profiler section

        // Continue with tickBlocks section
        level.getProfiler().popPush("tickBlocks");

        // Now manually implement the tickBlocks section from the original method
        if (pRandomTickSpeed > 0)
        {
            LevelChunkSection[] alevelchunksection = pChunk.getSections();

            for (int l = 0; l < alevelchunksection.length; ++l)
            {
                LevelChunkSection levelchunksection = alevelchunksection[l];
                if (levelchunksection.isRandomlyTicking())
                {
                    int j1 = pChunk.getSectionYFromSectionIndex(l);
                    int k1 = SectionPos.sectionToBlockCoord(j1);

                    for (int l1 = 0; l1 < pRandomTickSpeed; ++l1)
                    {
                        BlockPos blockpos3 = level.getBlockRandomPos(i, k1, j, 15);
                        level.getProfiler().push("randomTick");

                        BlockState blockstate2 = levelchunksection.getBlockState(
                                blockpos3.getX() - i,
                                blockpos3.getY() - k1,
                                blockpos3.getZ() - j);

                        boolean handled = false;

                        // Fast gate custom block state transition logic:
                        // only enter the method if this state actually has transition data.
                        StateTransitionData std = StateTransitionData.getData(blockstate2);
                        if (std != null && std.hasRandomTransitions())
                        {
                            handled = MinecraftThermalInput.tryMaterialPhaseAtRandomTick(level, pChunk, blockpos3, blockstate2)
                                    == MinecraftPhaseController.PhaseAttempt.CHANGED;
                        }

                        // Fast gate custom plant temperature logic:
                        // only enter the method if this block actually has plant temperature data.
                        if (!handled)
                        {
                            PlantTempData plantData = PlantTempData.getPlantData(blockstate2.getBlock());
                            if (plantData != null)
                            {
                                handled = frostedHeart$updatePlantBasedOnTemperature(
                                        level, blockpos3, plantData, climateBase);
                            }
                        }

                        if (!handled)
                        {
                            //@khjxiaogu: omit randomtick from the original block if temperature modification occurred
                            if (blockstate2.isRandomlyTicking())
                            {
                                blockstate2.randomTick(level, blockpos3, level.random);
                            }

                            FluidState fluidstate = blockstate2.getFluidState();
                            if (fluidstate.isRandomlyTicking())
                            {
                                fluidstate.randomTick(level, blockpos3, level.random);
                            }
                        }

                        level.getProfiler().pop();
                    }
                }
            }
        }

        // Pop the final profiler section
        level.getProfiler().pop();

        // Cancel the original method
        ci.cancel();
    }

    /**
     * Custom version of shouldSnow that keeps all checks except the light level check
     */
    @Unique
    public boolean frostedHeart$shouldSnowCustom(LevelReader pLevel, BlockPos pPos)
    {
        if (pPos.getY() >= pLevel.getMinBuildHeight()
                && pPos.getY() < pLevel.getMaxBuildHeight()
                && WorldTemperature.air(pLevel, pPos) < WorldTemperature.SNOW_REACHES_GROUND)
        {
            BlockState blockstate = pLevel.getBlockState(pPos);
            if ((blockstate.isAir() || blockstate.is(Blocks.SNOW))
                    && Blocks.SNOW.defaultBlockState().canSurvive(pLevel, pPos))
            {
                return true;
            }
        }

        return false;
    }

    // Plants should go separately
    @Unique
    private boolean frostedHeart$updatePlantBasedOnTemperature(
            ServerLevel level,
            BlockPos pos,
            PlantTempData selfData,
            float climateBase
    )
    {
        float t = (float) MinecraftThermalInput.gameplayCropEnvironment(
                level,
                pos,
                WorldTemperature.naturalBlock(level, pos, climateBase));
        var selfStatus = WorldTemperature.checkPlantStatus(level, pos, selfData, t);
        int heatCapacity = selfData.heatCapacity();
        if (selfStatus.willDie() && heatCapacity > 0 && level.getRandom().nextInt(heatCapacity) == 0)
        {
            var dead = selfData.dead();
            BlockPos belowPos = pos.below();
            BlockState belowBlockState = level.getBlockState(belowPos);

            if (dead == Blocks.DEAD_BUSH
                    && !belowBlockState.isAir()
                    && !belowBlockState.is(BlockTags.DEAD_BUSH_MAY_PLACE_ON))
            {
                level.setBlockAndUpdate(belowPos, Blocks.DIRT.defaultBlockState());
            }

            level.setBlockAndUpdate(pos, dead.defaultBlockState());
            return true;
        }
        return false;
    }

}
