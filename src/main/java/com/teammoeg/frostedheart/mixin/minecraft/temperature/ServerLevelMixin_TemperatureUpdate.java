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
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.climate.PhysicalState;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.block.LayeredThinIceBlock;
import com.teammoeg.frostedheart.content.climate.data.PlantTempData;
import com.teammoeg.frostedheart.content.climate.data.StateTransitionData;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.ChunkHeatData;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
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
        boolean updateTempBlock = (now + chunkpos.x + chunkpos.z)
                % FHConfig.SERVER.CLIMATE.tempBlockstateUpdateIntervalTicks.get() == 0;

        boolean isRaining = level.isRaining();
        WorldClimate wc = WorldClimate.get(level);
        if (wc != null)
        {
            isRaining = wc.getClimate(chunkpos).isSnowyOrBlizzard();
        }

        int i = chunkpos.getMinBlockX();
        int j = chunkpos.getMinBlockZ();

        // Process fewer blocks for temperature checks to reduce performance impact
        // Adjust this divisor based on your performance needs
/*
        Unused int temperatureChecks = Math.max(1,
                pRandomTickSpeed / FHConfig.SERVER.CLIMATE.tempRandomTickSpeedDivisor.get());
*/

        // Custom water freezing logic
        level.getProfiler().popPush("water");
        if (pRandomTickSpeed > 0 && updateTempBlock)
        {
            // for (int l1 = 0; l1 < temperatureChecks; ++l1) {
            BlockPos blockpos1 = level.getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING,
                    level.getBlockRandomPos(i, 0, j, 15));
            BlockPos blockpos2;
            if (blockpos1.getY() > -64)
            {
                blockpos2 = blockpos1.below();
            }
            else
            {
                blockpos2 = blockpos1;
            }

            Holder<Biome> biomeHolder = CUtils.fastGetBiome(pChunk, blockpos2);
            Biome biome = biomeHolder.value();

            // TODO: for ocean freezing, we need some special handling...
            if (!biomeHolder.is(FHTags.Biomes.WATER_DO_NOT_FREEZE.tag)
                    && level.isAreaLoaded(blockpos2, 1)) // Forge: check area to avoid loading neighbors in unloaded chunks
            {
                // Check if the block should freeze based on our custom logic
                frostedheart$freezeWater(level, blockpos2);
            }

            if (isRaining)
            {
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
            // }
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
                        if (std != null && std.willTransit())
                        {
                            handled = frostedHeart$updateBlockBasedOnTemperature(
                                    pChunk, level, blockpos3, blockstate2, std);
                        }

                        // Fast gate custom plant temperature logic:
                        // only enter the method if this block actually has plant temperature data.
                        if (!handled)
                        {
                            PlantTempData plantData = PlantTempData.getPlantData(blockstate2.getBlock());
                            if (plantData != null)
                            {
                                handled = frostedHeart$updatePlantBasedOnTemperature(
                                        level, blockpos3, blockstate2, plantData);
                            }
                        }

                        if (!handled)
                        {
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

    /**
     * Custom version of shouldFreeze that keeps all checks except the light level check.
     *
     * Freezes flowing water too.
     *
     * And it freeze water based on its level, so it turns into various thin ice.
     */
    @Unique
    private boolean frostedheart$freezeWater(ServerLevel level, BlockPos pos)
    {
        int heatChunkX = SectionPos.blockToSectionCoord(pos.getX());
        int heatChunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        ChunkHeatData heatData = ChunkHeatData.get(level, heatChunkX, heatChunkZ);


        if (pos.getY() >= level.getMinBuildHeight()
                && pos.getY() < level.getMaxBuildHeight()
                && WorldTemperature.blockWithHeatData(level, pos, heatData) < WorldTemperature.WATER_FREEZES)
        {
            BlockState blockstate = level.getBlockState(pos);
            FluidState fluidstate = blockstate.getFluidState();

            // Reuse a mutable position for neighbor checks to avoid allocating
            // pos.west()/east()/north()/south() BlockPos objects on every call.
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

            // Check if block is at edge of water (from original code)
            boolean isAtEdge =
                    !level.isWaterAt(cursor.set(pos).move(Direction.WEST))
                            || !level.isWaterAt(cursor.set(pos).move(Direction.EAST))
                            || !level.isWaterAt(cursor.set(pos).move(Direction.NORTH))
                            || !level.isWaterAt(cursor.set(pos).move(Direction.SOUTH));

            // source
            if (fluidstate.getType() == Fluids.WATER && blockstate.getBlock() instanceof LiquidBlock)
            {
                if (isAtEdge)
                {
                    level.setBlockAndUpdate(pos, FHBlocks.THIN_ICE_BLOCK.get().defaultBlockState());
                    return true;
                }
            }

            // flowing
            else if (fluidstate.getType() == Fluids.FLOWING_WATER
                    && blockstate.getBlock() instanceof LiquidBlock)
            {
                if (isAtEdge)
                {
                    BlockState targetState = null;

                    // TODO: should we do Icicles?
                    if (fluidstate.hasProperty(FlowingFluid.LEVEL))
                    {
                        // range 1-8
                        int flowingLevel = fluidstate.getValue(FlowingFluid.LEVEL);
                        if (flowingLevel == 8
                                || (fluidstate.hasProperty(FlowingFluid.FALLING)
                                && fluidstate.getValue(FlowingFluid.FALLING)))
                        {
                            targetState = FHBlocks.THIN_ICE_BLOCK.get().defaultBlockState();
                        }
                        else
                        {
                            targetState = FHBlocks.LAYERED_THIN_ICE.get().defaultBlockState()
                                    .setValue(LayeredThinIceBlock.LAYERS, flowingLevel);
                        }
                    }
                    else if (fluidstate.hasProperty(FlowingFluid.FALLING)
                            && fluidstate.getValue(FlowingFluid.FALLING))
                    {
                        targetState = FHBlocks.THIN_ICE_BLOCK.get().defaultBlockState();
                    }

                    if (targetState != null)
                    {
                        level.setBlockAndUpdate(pos, targetState);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Plants should go separately
    @Unique
    private boolean frostedHeart$updatePlantBasedOnTemperature(ServerLevel level, BlockPos pos,
                                                               BlockState currentState,
                                                               PlantTempData selfData)
    {
        int heatChunkX = SectionPos.blockToSectionCoord(pos.getX());
        int heatChunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        ChunkHeatData heatData = ChunkHeatData.get(level, heatChunkX, heatChunkZ);

        float t = WorldTemperature.blockWithHeatData(level, pos, heatData);
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

    @Unique
    private boolean frostedHeart$updateBlockBasedOnTemperature(LevelChunk pChunk, ServerLevel level,
                                                               BlockPos pos, final BlockState currentState,
                                                               StateTransitionData std)
    {
        int heatChunkX = SectionPos.blockToSectionCoord(pos.getX());
        int heatChunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        ChunkHeatData heatData = ChunkHeatData.get(level, heatChunkX, heatChunkZ);
        ChunkHeatData.HeatQueryResult heatQuery = ChunkHeatData.queryAdjust(heatData, pos);

        int heatCapacity = std.heatCapacity();
        if (heatCapacity <= 0 || level.random.nextInt(heatCapacity) != 0)
        {
            return false;
        }

        // TODO: For ocean melting we need special handling...
        // Biome lookup is only needed for ice-like blocks, so delay it until necessary.
        if (currentState.is(BlockTags.ICE))
        {
            Holder<Biome> biome = CUtils.fastGetBiome(pChunk, pos);
            if (biome.is(FHTags.Biomes.ICE_DO_NOT_SMELT.tag))
            {
                return false;
            }
        }

        float t = WorldTemperature.blockWithHeatQuery(level, pos, heatQuery);

        // Determine the target state based on temperature thresholds
        // We check transitions in order of priority (solid->gas, gas->solid, etc.)
        final PhysicalState sourceState = std.state();
        PhysicalState targetState = sourceState; // Default to current state
        BlockState targetBlock = currentState;   // Default to current block

        switch (sourceState)
        {
            case SOLID:
            {
                // To save performance, we only focus on blocks that player cares more about,
                // otherwise we reduce transition rate
                boolean shouldDoAdjust = level.random.nextInt(FHConfig.SERVER.CLIMATE.ambientBlockStateUpdateDivisor.get()) == 0
                        || heatQuery.hasActiveAdjust();

                if (!shouldDoAdjust)
                {
                    return false;
                }
                else if (t >= std.evaporateTemp() && std.gas() != null)
                {
                    targetState = PhysicalState.GAS;
                    targetBlock = std.gas();
                }
                else if (t >= std.meltTemp() && std.liquid() != null)
                {
                    targetState = PhysicalState.LIQUID;
                    targetBlock = std.liquid();
                }
                break;
            }
            case LIQUID:
            {
                if (t <= std.freezeTemp() && std.solid() != null)
                {
                    targetState = PhysicalState.SOLID;
                    targetBlock = std.solid();
                }
                else
                {
                    boolean shouldDoAdjust = level.random.nextInt(FHConfig.SERVER.CLIMATE.ambientBlockStateUpdateDivisor.get()) == 0
                            || heatQuery.hasActiveAdjust();

                    if (!shouldDoAdjust)
                    {
                        return false;
                    }
                    else if (t >= std.evaporateTemp() && std.gas() != null)
                    {
                        targetState = PhysicalState.GAS;
                        targetBlock = std.gas();
                    }
                }
                break;
            }
            case GAS:
            {
                if (t <= std.freezeTemp() && std.solid() != null)
                {
                    targetState = PhysicalState.SOLID;
                    targetBlock = std.solid();
                }
                else if (t <= std.condenseTemp() && std.liquid() != null)
                {
                    targetState = PhysicalState.LIQUID;
                    targetBlock = std.liquid();
                }
                break;
            }
        }

        // Skip update if target state is the same as current state
        if (targetState == sourceState || targetBlock == currentState)
        {
            return false;
        }

        // Create effects before changing the block
        frostedHeart$addTransitionEffects(level, pos, sourceState, targetState, currentState, targetBlock);

        // Update the block state
        level.setBlockAndUpdate(pos, targetBlock);
        return true;
    }

    /**
     * Adds visual and audio effects when a block changes state
     *
     * @param level The server level
     * @param pos Position of the block
     * @param oldState The original state string ("solid", "liquid", "gas")
     * @param newState The new state string
     * @param oldBlock The original block
     * @param newBlock The new block
     */
    @Unique
    private void frostedHeart$addTransitionEffects(ServerLevel level, BlockPos pos,
                                                   PhysicalState oldState, PhysicalState newState,
                                                   BlockState oldBlock, BlockState newBlock)
    {
        // Only create transition effects when a player is nearby.
        // This avoids wasting server/network/client resources on effects
        // that nobody can see or hear.
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        if (!level.hasNearbyAlivePlayer(x, y, z, 12.0D))
        {
            return;
        }

        // Create server-side particle effects that will be sent to clients
        // Use the transition type to determine appropriate particles
        switch (oldState.translate(newState))
        {
            case MELTING:
                // Melting effect - dripping water particles
                level.sendParticles(
                        ParticleTypes.DRIPPING_WATER,
                        x,
                        y,
                        z,
                        8, // particle count
                        0.3D, // spread X
                        0.3D, // spread Y
                        0.3D, // spread Z
                        0.0D  // speed
                );
                // Melting sound
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.BLOCKS, 0.5F,
                        2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);
                break;
            case SUBLIMATION:
            case EVAPORATION:
                // Evaporation/sublimation effect - cloud particles
                level.sendParticles(
                        ParticleTypes.CLOUD,
                        x,
                        y,
                        z,
                        12, // particle count
                        0.25D, // spread X
                        0.25D, // spread Y
                        0.25D, // spread Z
                        0.05D  // speed - slightly rising
                );
                // Steam hissing sound
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.BLOCKS, 0.4F,
                        2.0F + level.random.nextFloat() * 0.4F);
                break;
            case FREEZING:
                // Freezing effect - snowflake particles
                level.sendParticles(
                        ParticleTypes.SNOWFLAKE,
                        x,
                        y,
                        z,
                        10, // particle count
                        0.3D, // spread X
                        0.3D, // spread Y
                        0.3D, // spread Z
                        0.0D  // speed
                );
                // Freezing sound
                // level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 0.5F, 1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.4F);
                break;
            case CONDENSATION:
            case DEPOSITION:
                // Condensation/deposition effect - dripping particles
                level.sendParticles(
                        ParticleTypes.DRIPPING_WATER,
                        x,
                        pos.getY() + 0.8D,
                        z,
                        15, // particle count
                        0.4D, // spread X
                        0.1D, // spread Y
                        0.4D, // spread Z
                        0.0D  // speed
                );
                // Condensation sound - light rain-like sound
                level.playSound(null, pos, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER,
                        SoundSource.AMBIENT, 0.5F, 1.0F);
                break;
            default:
                break;
        }

        // You could also add custom particles for specific block transitions
        // For example, if water is turning to ice:
        if (oldBlock.is(Blocks.WATER) && newBlock.is(Blocks.ICE))
        {
            // Special ice formation particles
            level.sendParticles(
                    ParticleTypes.ITEM_SNOWBALL,
                    x,
                    y,
                    z,
                    5,
                    0.2D,
                    0.2D,
                    0.2D,
                    0.0D
            );
        }
    }
}