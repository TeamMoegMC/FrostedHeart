package com.teammoeg.frostedheart.climate;

import com.stereowalker.survive.Survive;
import com.stereowalker.survive.events.SurviveEvents;
import com.stereowalker.survive.util.TemperatureUtil;
import com.stereowalker.survive.util.data.BlockTemperatureData;
import com.stereowalker.unionlib.state.properties.UBlockStateProperties;
import com.teammoeg.frostedheart.world.chunkdata.ChunkData;
import net.minecraft.block.BlockState;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

/**
 * Re-write of survive's temperature methods since SurviveEvents.TempType is private.
 * Original Author: Stereowalker
 */
public class SurviveTemperature {

    public static double getExactTemperature(World world, BlockPos pos, SurviveTemperature.TempType type) {
        float biomeTemp = (TemperatureUtil.getTemperature(world.getBiome(pos), pos) * 2) - 2;
        float skyLight = world.getChunkProvider().getLightManager().getLightEngine(LightType.SKY).getLightFor(pos);
        float gameTime = world.getDayTime() % 24000L;
        gameTime = gameTime / (200 / 3);
        gameTime = (float) Math.sin(Math.toRadians(gameTime));

        switch (type) {
            case SUN:
                if (skyLight > 5.0F) return gameTime * 5.0F;
                else return -1.0F * 5.0F;

            case BIOME:
                // we use our own chunk temperature system here
                ChunkData data = ChunkData.get(world, pos);
                biomeTemp = data.getTemperatureAtBlock(pos) / 4f;
                return biomeTemp;

            case BLOCK:
                float blockTemp = 0;
                int rangeInBlocks = 5;
                for (int x = -rangeInBlocks; x <= rangeInBlocks; x++) {
                    for (int y = -rangeInBlocks; y <= rangeInBlocks; y++) {
                        for (int z = -rangeInBlocks; z <= rangeInBlocks; z++) {

                            BlockPos heatSource = new BlockPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                            float blockLight = world.getChunkProvider().getLightManager().getLightEngine(LightType.BLOCK).getLightFor(heatSource);
                            BlockState heatState = world.getBlockState(heatSource);
                            int sourceRange = Survive.blockTemperatureMap.containsKey(heatState.getBlock().getRegistryName()) ? Survive.blockTemperatureMap.get(heatState.getBlock().getRegistryName()).getRange() : 5;

                            if (pos.withinDistance(heatSource, sourceRange)) {
                                blockTemp += blockLight / 500.0F;
                                if (Survive.blockTemperatureMap.containsKey(heatState.getBlock().getRegistryName())) {
                                    BlockTemperatureData blockTemperatureData = Survive.blockTemperatureMap.get(heatState.getBlock().getRegistryName());
                                    if (blockTemperatureData.usesLitOrActiveProperty()) {
                                        boolean litOrActive = false;
                                        if (heatState.hasProperty(BlockStateProperties.LIT) && heatState.get(BlockStateProperties.LIT))
                                            litOrActive = true;
                                        if (heatState.hasProperty(UBlockStateProperties.ACTIVE) && heatState.get(UBlockStateProperties.ACTIVE))
                                            litOrActive = true;
                                        if (litOrActive) blockTemp += blockTemperatureData.getTemperatureModifier();
                                    } else
                                        blockTemp += blockTemperatureData.getTemperatureModifier();

                                    if (blockTemperatureData.usesLevelProperty()) {
                                        if (heatState.hasProperty(BlockStateProperties.LEVEL_0_15)) {
                                            blockTemp *= (heatState.get(BlockStateProperties.LEVEL_0_15) + 1) / 16;
                                        } else if (heatState.hasProperty(BlockStateProperties.LEVEL_0_8)) {
                                            blockTemp *= (heatState.get(BlockStateProperties.LEVEL_0_8) + 1) / 9;
                                        } else if (heatState.hasProperty(BlockStateProperties.LEVEL_1_8)) {
                                            blockTemp *= (heatState.get(BlockStateProperties.LEVEL_1_8)) / 8;
                                        } else if (heatState.hasProperty(BlockStateProperties.LEVEL_0_3)) {
                                            blockTemp *= (heatState.get(BlockStateProperties.LEVEL_0_3) + 1) / 4;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return blockTemp;

            case SHADE:
                return ((skyLight / 7.5F) - 1);

            default:
                return Survive.DEFAULT_TEMP;
        }
    }

    public static double getBlendedTemperature(World world, BlockPos mainPos, BlockPos blendPos, SurviveTemperature.TempType type) {
        float distance = (float) Math.sqrt(mainPos.distanceSq(blendPos));// 2 - 10 - 0
        if (distance <= 5.0D) {
            float blendRatio0 = distance / 5.0F;   // 0.2 - 1.0 - 0.0
            float blendRatio1 = 1.0F - blendRatio0; // 0.8 - 0.0 - 1.0
            double temp0 = getExactTemperature(world, blendPos, type);
            double temp1 = getExactTemperature(world, mainPos, type);
            return ((temp0 * blendRatio0) + (temp1 * blendRatio1));
        } else {
            return getExactTemperature(world, mainPos, type);
        }
    }

    public static float getAverageTemperature(World world, BlockPos pos, SurviveTemperature.TempType type, int rangeInBlocks, SurviveEvents.TempMode mode) {
        float temp = 0;
        int tempAmount = 0;
        for (int x = -rangeInBlocks; x <= rangeInBlocks; x++) {
            for (int y = -rangeInBlocks; y <= rangeInBlocks; y++) {
                for (int z = -rangeInBlocks; z <= rangeInBlocks; z++) {
                    if (mode == SurviveEvents.TempMode.BLEND)
                        temp += getBlendedTemperature(world, new BlockPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z), pos, type);
                    else if (mode == SurviveEvents.TempMode.NORMAL)
                        temp += getExactTemperature(world, new BlockPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z), type);
                    tempAmount++;
                }
            }
        }
        return temp / ((float) tempAmount);
    }

    public enum TempType {
        BIOME("biome", 10, 7, false), BLOCK("block", 10, 9, true), SHADE("shade", 10, 200, true), SUN("sun", 10, 200, true);

        String name;
        int tickInterval;
        double reductionAmount;
        boolean usingExact;

        private TempType(String name, int tickIntervalIn, double reductionAmountIn, boolean usingExactIn) {
            this.tickInterval = tickIntervalIn;
            this.reductionAmount = reductionAmountIn;
            this.usingExact = usingExactIn;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public int getTickInterval() {
            return tickInterval;
        }

        public double getReductionAmount() {
            return reductionAmount;
        }

        public boolean isUsingExact() {
            return usingExact;
        }
    }
}
