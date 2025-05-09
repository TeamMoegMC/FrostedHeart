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
import javax.annotation.Nullable;

import com.teammoeg.frostedheart.content.climate.data.BiomeTempData;
import com.teammoeg.frostedheart.content.climate.data.PlantTemperature;
import com.teammoeg.frostedheart.content.climate.data.PlantTemperature.TemperatureType;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.ChunkHeatData;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.data.PlantTempData;
import com.teammoeg.frostedheart.content.climate.data.WorldTempData;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

    @Deprecated
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

    @Deprecated
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

    @Deprecated
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

    // Climate
    public static final float SNOW_REACHES_GROUND = -13F;
    public static final float BLIZZARD_REACHES_GROUND = -30;
    public static final float OVERWORLD_BASELINE = -10;
    /**
     * The temporary uprising peak temperature of a cold period.
     */
    public static final float COLD_PERIOD_PEAK = -5;
    /**
     * The peak temperature of a warm period.
     */
    public static final float WARM_PERIOD_PEAK = 8;
    /**
     * The lower peak temperature of a warm period.
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

    // Matter state transitions
    public static final float CO2_FREEZES = -78;
    public static final float OXYGEN_FREEZES = -218;
    public static final float OXYGEN_CONDENSATES = -182;
    public static final float NITROGEN_FREEZES = -209;
    public static final float NITROGEN_CONDENSATES = -195;
    public static final float WATER_ICE_MELTS = 2F;
    public static final float WATER_FREEZES = -5F;
    public static final float LAVA_FREEZES = 700F;

    // Agriculture
    public static final float FOOD_FROZEN_TEMPERATURE = -5;
    public static final float ANIMAL_ALIVE_TEMPERATURE = -9f;
    public static final float FEEDED_ANIMAL_ALIVE_TEMPERATURE = -30f;
    public static final float VANILLA_PLANT_GROW_TEMPERATURE_MAX = 50;

    // Altitudes
    public static final int SEA_LEVEL = 63;
    public static final int BUILD_UPPER_LIMIT = 320;
    public static final int STONE_INTERFACE_LEVEL = 0;
    public static final int LAVA_INTERFACE_LEVEL = -55;
    public static final int BUILD_LOWER_LIMIT = -64;
    public static final float TEMPERATURE_CHANGE_PER_BLOCK_ABOVE_SEA_LEVEL = -0.1F;
    public static final float TEMPERATURE_CHANGE_PER_BLOCK_BELOW_STONE_INTERFACE = 0.1F;
    public static final float TEMPERATURE_CHANGE_PER_BLOCK_BELOW_LAVA_INTERFACE = 20F;

    // temperature cache
    public static Map<Level, Float> worldCache = new HashMap<>();
    public static Map<Biome, Float> biomeCache = new HashMap<>();

    public static void clear() {
        worldCache.clear();
        biomeCache.clear();
    }

    /**
     * Get World temperature adjustment, this value is consistent.
     * Range: [minWorldTemp, maxWorldTemp], See Data.
     * Now, it is constant: [-10, -10].
     */
    public static float dimension(LevelReader w) {
        float wt = OVERWORLD_BASELINE;
        if (w instanceof Level level) {
            wt = worldCache.computeIfAbsent(level,l-> WorldTempData.getWorldTemp(l));
        }
        return wt;
    }

    /**
     * Get Biome temperature adjustment, this value is consistent.
     * Range: [minBiomeTemp, maxBiomeTemp], See Data.
     * Now, it is [-30, 20].
     *
     * This is biology induced temperature!
     * This should exclude altitude effects.
     * For example, frozen peaks and gravel desert should have same biome temp.
     * Because they both have no plant!
     * But wintry forest would have higher biome temp, because of trees!
     * Similarly for bushes etc.
     */
    public static float biome(LevelReader w, BlockPos pos) {
        Biome b = w.getBiome(pos).get();
        return biomeCache.computeIfAbsent(b,t-> BiomeTempData.getBiomeTemp(w, t));
    }

    /**
     * Get Climate temperature adjustment, this value is consistent.
     * Range: [COLD_PERIOD_BOTTOM_T10, WARM_PERIOD_PEAK]
     * Now, it is [-90, 8]
     */
    public static float climate(LevelReader w) {
        if (w instanceof Level) {
            return WorldClimate.getTemp((Level) w);
        }
        return 0;
    }


    /**
     * Altitude-induced temperature change, this value is consistent.
     * Range: [(BUILD_UPPER_LIMIT - SEA_LEVEL) * TEMPERATURE_CHANGE_PER_BLOCK_ABOVE_SEA_LEVEL,
     * (STONE_INTERFACE_LEVEL - BUILD_LOWER_LIMIT) * TEMPERATURE_CHANGE_PER_BLOCK_BELOW_STONE_INTERFACE]
     * Now, it is [-25.7, 6.4]
     */
    public static float altitude(LevelReader w, BlockPos pos) {
        // TODO: This is for only overworld
        int y = pos.getY();
        // decrease above sea
        if (y > SEA_LEVEL) {
            int yc = Mth.clamp(y, SEA_LEVEL, BUILD_UPPER_LIMIT);
            return (yc - SEA_LEVEL) * TEMPERATURE_CHANGE_PER_BLOCK_ABOVE_SEA_LEVEL;
        }
        // an insulated zone in normal stone area
        else if (y > STONE_INTERFACE_LEVEL) {
            return 0;
        }
        // increase in deepslate
        else if (y > LAVA_INTERFACE_LEVEL) {
            int yc = Mth.clamp(y, LAVA_INTERFACE_LEVEL, STONE_INTERFACE_LEVEL);
            return (STONE_INTERFACE_LEVEL - yc) * TEMPERATURE_CHANGE_PER_BLOCK_BELOW_STONE_INTERFACE;
        }
        // significant increase in lava to bedrock
        else {
            int yc = Mth.clamp(y, BUILD_LOWER_LIMIT, LAVA_INTERFACE_LEVEL);
            return (LAVA_INTERFACE_LEVEL - yc) * TEMPERATURE_CHANGE_PER_BLOCK_BELOW_LAVA_INTERFACE;
        }
    }

    /**
     * Get World temperature for a specific world, in absolute value.
     *
     * Result = Dimension + Biome + Altitude + Climate.
     *
     * This value is dynamic in game.
     *
     * @param w the world<br>
     * @return world temperature<br>
     */
    public static float base(LevelReader w, BlockPos pos) {
        return dimension(w) + biome(w, pos) + altitude(w, pos) + climate(w);
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

    public static float gaussian(LevelReader world, float mean, float std) {
        if (world instanceof Level level) {
            return (float) (level.getRandom().nextGaussian() * std + mean);
        } else
            return 0;
    }

    /**
     * This is the most common method to get temperature for blocks like crops and machines.
     *
     * Result = Dimension + Biome + Altitude + Climate + HeatAdjusts.
     * A factor would be applied to climate value to lower the climate affect on block, simulates lower heat transfer rate between block and air
     * Called to get temperature when a world context is available.
     * on server, will either query capability falling back to cache, or query
     * provider to generate the data.
     * This method directly get temperature at any positions.
     * This value is dynamic in game.
     */
    public static float block(LevelReader world, BlockPos pos) {
        int y = pos.getY();

        float climateBlockAffection;
        // above sea level, climate significantly influences soil
        if (y > SEA_LEVEL) {
            climateBlockAffection = 0.5F;
        }
        // a thin layer of stone beneath sea level serves as a climate insulation layer
        else if (y > STONE_INTERFACE_LEVEL) {
            climateBlockAffection = 0.5F * (y - STONE_INTERFACE_LEVEL) / (SEA_LEVEL - STONE_INTERFACE_LEVEL);
        }
        // below that, climate no longer affects block temperature
        else {
            climateBlockAffection = 0.0F;
        }
        float climateAffection=climate(world) * climateBlockAffection +dimension(world) + biome(world, pos) + altitude(world, pos);
        float heat=heat(world,pos);

        return  Math.min(climateAffection+heat*2, heat)/*+ gaussian(world, 0, 0.3F)*/;
    }

    /**
     * This is the most common method to get air temperature which may affect player.
     *
     * Result = Dimension + Biome + Altitude + Climate + HeatAdjusts.
     *
     * Called to get temperature when a world context is available.
     * on server, will either query capability falling back to cache, or query
     * provider to generate the data.
     * This method directly get temperature at any positions.
     * This value is dynamic in game.
     */
    public static float air(LevelReader world, BlockPos pos) {
        int y = pos.getY();

        float climateAirAffection;
        // above sea level, climate significantly influences air
        if (y > SEA_LEVEL) {
            climateAirAffection = 1.0F;
        }
        // a thin layer of stone beneath sea level serves as a climate insulation layer
        else if (y > STONE_INTERFACE_LEVEL) {
            climateAirAffection = 1.0F * (y - STONE_INTERFACE_LEVEL) / (SEA_LEVEL - STONE_INTERFACE_LEVEL);
        }
        // below that, climate no longer affects block temperature
        else {
            climateAirAffection = 0.0F;
        }
        return dimension(world) + biome(world, pos) + altitude(world, pos) +
                climate(world) * climateAirAffection + heat(world,pos) + gaussian(world, 0, 0.3F);
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
     * @return Range 0-100
     * */
    public static int wind(LevelReader w) {
        if (w instanceof Level l) {
            return WorldClimate.getWind(l);
        }
        return 0;
    }

    @Nonnull
    private static PlantTemperature getPlantDataWithDefault(Block block) {

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
        WILL_DIE,
        NOT_PLANT;

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

        public boolean notPlant() {
            return this == NOT_PLANT;
        }
    }

    /**
     * Use this when you need a comprehensive check on plant status
     */
    public static PlantStatus checkPlantStatus(LevelAccessor level, BlockPos pos, Block block) {
        PlantTempData data = PlantTempData.getPlantData(block);
        return checkPlantStatus(level,pos,data);
    }

    public static PlantStatus checkPlantStatus(LevelAccessor level, BlockPos pos,@Nullable PlantTempData data) {
        if (data == null) {
            return PlantStatus.NOT_PLANT;
        }

        if(openToAir(level,pos)) {
            if (WorldTemperature.isBlizzard(level)&&data.blizzardVulnerable()) {
                return data.willDie() ? PlantStatus.WILL_DIE : PlantStatus.CAN_SURVIVE;
            }
            if (WorldClimate.isSnowing(level)&&data.snowVulnerable()) {
                return data.willDie() ? PlantStatus.WILL_DIE : PlantStatus.CAN_SURVIVE;
            }
        }
        float blockTemp = block(level, pos);
        if (data.isValidTemperature(TemperatureType.BONEMEAL, blockTemp)) {
            return PlantStatus.CAN_FERTILIZE;
        }
        if (data.isValidTemperature(TemperatureType.GROW, blockTemp)) {
            return PlantStatus.CAN_GROW;
        }
        if (data.isValidTemperature(TemperatureType.SURVIVE, blockTemp)) {
            return PlantStatus.CAN_SURVIVE;
        }
        return data.willDie() ? PlantStatus.WILL_DIE : PlantStatus.CAN_SURVIVE;
    }

    /**
     * A cheaper call, using existing temperature fetch from earlier
     * @param level
     * @param pos
     * @param data
     * @param blockTemp
     * @return
     */
    public static PlantStatus checkPlantStatus(LevelAccessor level, BlockPos pos,@Nullable PlantTempData data, float blockTemp) {
        if (data == null) {
            return PlantStatus.NOT_PLANT;
        }

        if(openToAir(level,pos)) {
            if (WorldTemperature.isBlizzard(level)&&data.blizzardVulnerable()) {
                return data.willDie() ? PlantStatus.WILL_DIE : PlantStatus.CAN_SURVIVE;
            }
            if (WorldClimate.isSnowing(level)&&data.snowVulnerable()) {
                return data.willDie() ? PlantStatus.WILL_DIE : PlantStatus.CAN_SURVIVE;
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
        return data.willDie() ? PlantStatus.WILL_DIE : PlantStatus.CAN_SURVIVE;
    }

    public static PlantStatus checkPlantStatus(LevelAccessor level, BlockPos pos,@Nullable PlantTempData data,boolean withTempPreserve) {
        if(withTempPreserve){
            return checkPlantStatus(level,pos,data);
        }
        else{
            if (data == null) {
                return PlantStatus.NOT_PLANT;
            }

            if(openToAir(level,pos)) {
                if (WorldTemperature.isBlizzard(level)&&data.blizzardVulnerable()) {
                    return data.willDie() ? PlantStatus.WILL_DIE : PlantStatus.CAN_SURVIVE;
                }
                if (WorldClimate.isSnowing(level)&&data.snowVulnerable()) {
                    return data.willDie() ? PlantStatus.WILL_DIE : PlantStatus.CAN_SURVIVE;
                }
            }
            float blockTemp = block(level, pos);
            if (data.isValidTemperature(TemperatureType.BONEMEAL, blockTemp)) {
                return PlantStatus.CAN_FERTILIZE;
            }
            if (data.isValidTemperature(TemperatureType.GROW, blockTemp)) {
                return PlantStatus.CAN_GROW;
            }
            return PlantStatus.CAN_SURVIVE;
        }
    }
}
