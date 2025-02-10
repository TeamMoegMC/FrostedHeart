/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.frostedheart.content.climate.data.BiomeTempData;
import com.teammoeg.frostedheart.content.climate.data.PlantTemperature;
import com.teammoeg.frostedheart.content.climate.data.PlantTemperature.TemperatureType;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.ChunkHeatData;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.data.PlantTempData;
import com.teammoeg.frostedheart.content.climate.data.WorldTempData;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * World Temperature API on the server side.
 *
 * <p>This class provides a set of methods to get realistic temperature in the world.
 *
 * Methods here are cheap, so you can call them tick wise frequently.
 *
 * <p>There are 4 types of temperature:
 *
 * <p>1. Dimension temperature: The temperature of the world defined by datapack. This is fixed on registry.
 *
 * <p>2. Biome temperature: The temperature of the biome defined by datapack. This is fixed on registry.
 *
 * <p>3. Climate temperature: The temperature of the climate. This is dynamic, see {@link WorldClimate}.
 *
 * <p>4. Heat adjusts: The temperature of the heat source or sink. This is dynamic, see {@link ChunkHeatData}.
 *
 * <p>You can access these temperature by calling the methods in this class.
 *
 * <p>
 *     Methods:
 *     <ul>
 *         <li>{@link #dimension(LevelReader)}: Get Dimension temperature.</li>
 *         <li>{@link #biome(LevelReader, BlockPos)}: Get Biome temperature.</li>
 *         <li>{@link #climate(LevelReader)}: Get Climate temperature.</li>
 *         <li>{@link #base(LevelReader, BlockPos)}: Get World temperature without heat adjusts.</li>
 *         <li>{@link #heat(LevelReader, BlockPos)}: Get Heat adjusts temperature.</li>
 *         <li>{@link #block(LevelReader, BlockPos)}: Get World temperature with heat adjusts. USE THIS!</li>
 *         <li>{@link #air(LevelReader, BlockPos)}: Get Air temperature with heat adjusts.</li>
 *         <li>{@link #isBlizzard(LevelReader)}: Check if it is blizzard.</li>
 *         <li>{@link #wind(LevelReader)}: Get wind speed.</li>
 *         <li>{@link #clear()}: Clear cache.</li>
 *     </ul>
 * </p>
 */
public class WorldTemperature {


    public static boolean isBlizzardHarming(LevelAccessor iWorld, BlockPos p) {
        return WorldClimate.isBlizzard(iWorld) && openToAir(iWorld,p);
    }

    public static boolean openToAir(LevelAccessor iWorld, BlockPos p) {
        return iWorld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getX(), p.getZ()) <= p.getY();
    }

    public static boolean isRainingAt(BlockPos pos, Level world) {

        if (!world.isRaining()) {
            return false;
        } else if (!world.canSeeSky(pos)) {
            return false;
        } else return world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() <= pos.getY();
    }

    public static boolean canBigTreeGenerate(LevelAccessor worldIn, BlockPos p, RandomSource r) {
    	float temp = block(worldIn, p);
    	 BlockState bs=worldIn.getBlockState(p);
    	 float minTemp=getMinGrowTemp(bs.getBlock());
        if (temp < minTemp || WorldClimate.isBlizzard(worldIn))
            return false;
        if (temp > getMaxGrowTemp(bs.getBlock()))
            return false;
        if (temp > 0)
            return canTreeGenerate(worldIn, p, r, 7);
        return canTreeGenerate(worldIn, p, r,7*Mth.ceil(Math.max(1, minTemp+6-temp / 2)));

    }

    public static boolean canTreeGrow(LevelAccessor worldIn, BlockPos p, RandomSource rand) {
        float temp = block(worldIn, p);
        BlockState bs=worldIn.getBlockState(p);
        float minTemp=getMinGrowTemp(bs.getBlock());
        if (temp < minTemp || WorldClimate.isBlizzard(worldIn))
            return false;
        if (temp > getMaxGrowTemp(bs.getBlock()))
            return false;
        if (temp > 0)
            return true;
        return canTreeGenerate(worldIn, p, rand,Math.max(1, Mth.ceil(minTemp+6-temp / 2)));
    }

    public static boolean canGrassSurvive(LevelReader world, BlockPos pos) {
        float t = block(world, pos);
        return t >= HEMP_GROW_TEMPERATURE && t <= VANILLA_PLANT_GROW_TEMPERATURE_MAX;
    }

    public static boolean canNetherTreeGrow(BlockGetter w, BlockPos p) {
        if (!(w instanceof LevelAccessor)) {
            return false;
        }
        float temp = block((LevelAccessor) w, p);
        if (temp <= 300)
            return false;
        return !(temp > 300 + VANILLA_PLANT_GROW_TEMPERATURE_MAX);
    }

    public static boolean canTreeGenerate(LevelAccessor w, BlockPos p, RandomSource r, int chance) {
        return r.nextInt(chance) == 0;

    }

    public enum TemperatureCheckResult{
    	INVALID,
    	BLIZZARD_HARM,
    	TOO_COLD,
    	COLD,
    	TOO_HOT,
    	SUITABLE;
    	public boolean isValid() {
    		return this!=INVALID;
    	}
    	public boolean isSuitable() {
    		return this==SUITABLE;
    	}
    	public boolean isCold() {
    		return this==TOO_COLD||this==COLD;
    	}
    	public boolean isHot() {
    		return this==TOO_HOT;
    	}
     	public boolean isDeadly() {
    		return this==TOO_COLD||this==TOO_HOT;
    	}
     	public boolean isRipedOff() {
    		return this==TOO_COLD||this==BLIZZARD_HARM;
    	}
    }
    /**
     * Baseline temperature for temperate period.
     */
    public static final float CALM_PERIOD_BASELINE = -10;

    /**
     * The temporary uprising peak temperature of a cold period.
     */
    public static final float COLD_PERIOD_PEAK = -5;

    /**
     * The peak temperature of a warm period.
     */
    public static final float WARM_PERIOD_PEAK = 8;
    /**
     * The peak temperature of a warm period.
     */
    public static final float WARM_PERIOD_LOWER_PEAK = 6;
    /**
     * The peak temperature of a blizzard period.
     */
    public static final float BLIZZARD_WARM_PEAK = WARM_PERIOD_LOWER_PEAK;

    /**
     * The bottom temperature of a cold period.
     */
    public static final float COLD_PERIOD_BOTTOM_T1 = -5;
    public static final float COLD_PERIOD_BOTTOM_T2 = -10;
    public static final float COLD_PERIOD_BOTTOM_T3 = -20;
    public static final float COLD_PERIOD_BOTTOM_T4 = -30;
    public static final float COLD_PERIOD_BOTTOM_T5 = -40;
    public static final float COLD_PERIOD_BOTTOM_T6 = -50;
    public static final float COLD_PERIOD_BOTTOM_T7 = -60;
    public static final float COLD_PERIOD_BOTTOM_T8 = -70;
    public static final float COLD_PERIOD_BOTTOM_T9 = -80;
    public static final float COLD_PERIOD_BOTTOM_T10 = -90;
    public static final float[] BOTTOMS = new float[]{
            COLD_PERIOD_BOTTOM_T1,
            COLD_PERIOD_BOTTOM_T2,
            COLD_PERIOD_BOTTOM_T3,
            COLD_PERIOD_BOTTOM_T4,
            COLD_PERIOD_BOTTOM_T5,
            COLD_PERIOD_BOTTOM_T6,
            COLD_PERIOD_BOTTOM_T7,
            COLD_PERIOD_BOTTOM_T8,
            COLD_PERIOD_BOTTOM_T9,
            COLD_PERIOD_BOTTOM_T10
    };

    public static final float WATER_FREEZE_TEMP = 0;
    public static final float CO2_FREEZE_TEMP = -78;
    public static final float O2_FREEZE_TEMP = -218;
    public static final float O2_LIQUID_TEMP = -182;
    public static final float N2_FREEZE_TEMP = -209;
    public static final float N2_LIQUID_TEMP = -195;

    /**
     * The temperature when snow can reach the ground.
     */
    public static final float SNOW_TEMPERATURE = -13;
    /**
     * The temperature when snow layer melts.
     */
    public static final float SNOW_MELT_TEMPERATURE = 0.5F;
    /**
     * The temperature when snow becomes blizzard.
     */
    public static final float BLIZZARD_TEMPERATURE = -30;
    /**
     * The temperature when cold plant can grow.
     */
    public static final int COLD_RESIST_GROW_TEMPERATURE = -20;
    /**
     * The temperature when hemp can grow.
     */
    public static final float HEMP_GROW_TEMPERATURE = -15;
    /**
     * The temperature when vanilla plants can grow.
     */
    public static final float VANILLA_PLANT_GROW_TEMPERATURE = -10;
    public static final int BONEMEAL_TEMPERATURE = 5;
    public static final float FOOD_FROZEN_TEMPERATURE = -5;

    public static final float ANIMAL_ALIVE_TEMPERATURE = -9f;

    public static final float FEEDED_ANIMAL_ALIVE_TEMPERATURE = -30f;

    public static final float VANILLA_PLANT_GROW_TEMPERATURE_MAX = 50;
    public static final float CLIMATE_BLOCK_AFFECTION=0.5f;
    
    public static Map<Level, Float> worldCache = new HashMap<>();
    public static Map<Biome, Float> biomeCache = new HashMap<>();

    public static void clear() {
        worldCache.clear();
        biomeCache.clear();
    }

    /**
     * Get World temperature adjustment, this value is consistent.
     * @param w
     * @return
     */
    public static float dimension(LevelReader w) {
        float wt = 0;
        if (w instanceof Level level) {
            wt = worldCache.computeIfAbsent(level,l-> WorldTempData.getWorldTemp(l));
        }
        return wt;
    }

    /**
     * Get Biome temperature adjustment, this value is consistent.
     * @param w
     * @param pos
     * @return
     */
    public static float biome(LevelReader w, BlockPos pos) {
        Biome b = w.getBiome(pos).get();
        return biomeCache.computeIfAbsent(b,t-> BiomeTempData.getBiomeTemp(t));
    }

    /**
     * Get Climate temperature adjustment, this value is consistent.
     * @param w
     * @return
     */
    public static float climate(LevelReader w) {
        if (w instanceof Level) {
            return WorldClimate.getTemp((Level) w);
        }
        return 0;
    }

    /**
     * Get World temperature for a specific world, in absolute value.
     *
     * Result = Dimension + Biome + Climate.
     *
     * This value is dynamic in game.
     *
     * @param w the world<br>
     * @return world temperature<br>
     */
    public static float base(LevelReader w, BlockPos pos) {
        return dimension(w) + biome(w, pos) + climate(w);
    }

    /**
     * Get heat adjust (from heating device) temperature adjustment.
     *
     * Called to get temperature when a world context is available.
     * on server, will either query capability falling back to cache, or query
     * provider to generate the data.
     * This value is dynamic in game.
     */
    public static float heat(LevelReader world, BlockPos pos) {
        return ChunkHeatData.get(world, new ChunkPos(pos)).map(t -> t.getAdditionTemperatureAtBlock(world, pos)).orElse(0f);
    }

    /**
     * This is the most common method to get temperature for blocks like crops and machines.
     *
     * Result = Dimension + Biome + Climate + HeatAdjusts.
     * A factor would be applied to climate value to lower the climate affect on block, simulates lower heat transfer rate between block and air
     * Called to get temperature when a world context is available.
     * on server, will either query capability falling back to cache, or query
     * provider to generate the data.
     * This method directly get temperature at any positions.
     * This value is dynamic in game.
     */
    public static float block(LevelReader world, BlockPos pos) {
        return dimension(world) + biome(world, pos) + climate(world) * CLIMATE_BLOCK_AFFECTION + heat(world,pos);
    }
    /**
     * This is the most common method to get air temperature which may affect player.
     *
     * Result = Dimension + Biome + Climate + HeatAdjusts.
     *
     * Called to get temperature when a world context is available.
     * on server, will either query capability falling back to cache, or query
     * provider to generate the data.
     * This method directly get temperature at any positions.
     * This value is dynamic in game.
     */
    public static float air(LevelReader world, BlockPos pos) {
        return dimension(world) + biome(world, pos) + climate(world) + heat(world,pos);
    }
    
    /**
     * Convenience method for checking is Blizzard in specific world
     * 
     * */
    public static boolean isBlizzard(LevelReader w) {
        if (w instanceof Level l) {
            return WorldClimate.isBlizzard(l);
        }
        return false;
    }
    /**
     * Convenience method for checking wind strength in specific world.
     * 
     * */
    public static int wind(LevelReader w) {
        if (w instanceof Level l) {
            return WorldClimate.getWind(l);
        }
        return 0;
    }

    @Nonnull
    public static PlantTemperature getPlantDataWithDefault(Block block) {
        
    	PlantTempData data = PlantTempData.getPlantData(block);
        
        // We can't really do any instanceof check here, since so many potential blocks
        // may be invoked with the crop event.
        if (data == null) {
        	if(block instanceof SaplingBlock)
        		return PlantTemperature.DEFAULT_SAPLINGS;
            return PlantTemperature.DEFAULT_PLANTS;
        }
        return data;
    }

    private static float getMinFertilizeTemp(Block block) {
        return getPlantDataWithDefault(block).minFertilize();
    }

    private static float getMinGrowTemp(Block block) {
        return getPlantDataWithDefault(block).minGrow();
    }

    private static float getMinSurviveTemp(Block block) {
        return getPlantDataWithDefault(block).minSurvive();
    }

    private static float getMaxFertilizeTemp(Block block) {
        return getPlantDataWithDefault(block).maxFertilize();
    }

    private static float getMaxGrowTemp(Block block) {
        return getPlantDataWithDefault(block).maxGrow();
    }

    private static float getMaxSurviveTemp(Block block) {
        return getPlantDataWithDefault(block).maxSurvive();
    }

    private static boolean isSnowVulnerable(Block block) {
        return getPlantDataWithDefault(block).snowVulnerable();
    }

    public static boolean isBlizzardVulnerable(Block block) {
        return getPlantDataWithDefault(block).blizzardVulnerable();
    }



    public enum PlantStatus {
        CAN_FERTILIZE,
        CAN_GROW,
        CAN_SURVIVE,
        WILL_DIE;

        public boolean canFertilize() {
            return this == CAN_FERTILIZE;
        }

        public boolean canGrow() {
            return this == CAN_GROW || this == CAN_FERTILIZE;
        }

        public boolean canSurvive() {
            return this == CAN_SURVIVE || this == CAN_GROW || this == CAN_FERTILIZE;
        }

        public boolean willDie() {
            return this == WILL_DIE;
        }
    }

    /**
     * Use this when you need a comprehensive check on plant status
     */
    public static PlantStatus checkPlantStatus(LevelAccessor level, BlockPos pos, Block block) {
        float blockTemp = block(level, pos);
        PlantTemperature data=getPlantDataWithDefault(block);
        if(openToAir(level,pos)) {
	        if (WorldClimate.isBlizzard(level)&&data.blizzardVulnerable()) {
	            return PlantStatus.WILL_DIE;
	        }
	        if (WorldClimate.isSnowing(level)&&data.snowVulnerable()) {
	            return PlantStatus.WILL_DIE;
	        }
        }
        if (data.isValidTemperature(TemperatureType.BONEMEAL, blockTemp)) {
            return PlantStatus.CAN_FERTILIZE;
        }
        if (data.isValidTemperature(TemperatureType.GROW, blockTemp)) {
            return PlantStatus.CAN_GROW;
        }
        if (data.isValidTemperature(TemperatureType.SURVIVE, blockTemp)) {
            return PlantStatus.CAN_SURVIVE;
        }
        return PlantStatus.WILL_DIE;
    }
}
