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

package com.teammoeg.frostedheart.content.climate;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.teammoeg.frostedheart.content.agriculture.CropFertilization;
import com.teammoeg.frostedheart.content.climate.data.BiomeTempData;
import com.teammoeg.frostedheart.content.climate.data.PlantTemperature;
import com.teammoeg.frostedheart.content.climate.data.PlantTemperature.TemperatureType;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.data.PlantTempData;
import com.teammoeg.frostedheart.content.climate.data.WorldTempData;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;

/** World climate and temperature calculations in degrees Celsius. */
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

    public static boolean isSkylightNeeded(BlockPos pos, LevelReader level, int minLight, int maxLight) {
        int skylight = level.getBrightness(LightLayer.SKY, pos);
        return skylight >= minLight && skylight <= maxLight;
    }

    public static boolean isSkylightNeeded(BlockPos pos, LevelReader level, PlantTempData data) {
        return isSkylightNeeded(pos, level, data.minSkylight(), data.maxSkylight());
    }

    @Deprecated
    public static boolean canBigTreeGenerate(LevelAccessor worldIn, BlockPos p, RandomSource r) {
        float temp = getTempAtBlock(worldIn, p);
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
        float temp = getTempAtBlock(worldIn, p);
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
    public static final float BLIZZARD_WARM_PEAK = WARM_PERIOD_PEAK;

    /**
     * The bottom temperature of a cold period.
     */
    //public static final float COLD_PERIOD_BOTTOM_T1 = -5;
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
            //COLD_PERIOD_BOTTOM_T1,
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

    // Altitude: long-term gradients only. 0.6 °C colder per 100 blocks above the reference level, 3 °C warmer per
    // 100 blocks below it. Keeping the weather out is the job of real cover, see coveredClimate.
    public static final int ALTITUDE_REFERENCE_LEVEL = 64;
    public static final float TEMPERATURE_CHANGE_PER_BLOCK_ABOVE_REFERENCE = -0.006F;
    public static final float TEMPERATURE_CHANGE_PER_BLOCK_BELOW_REFERENCE = 0.03F;

    // temperature cache
    public static Map<Level, Float> worldCache = new WeakHashMap<>();
    public static Object2FloatOpenHashMap<Biome> biomeCache = new Object2FloatOpenHashMap<>();

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
        if (w instanceof Level level) {
            return worldCache.computeIfAbsent(level, WorldTempData::getWorldTemp);
        }
        return FHConfig.SERVER.CLIMATE.overworldBaselineCelsius.get().floatValue();
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
        float cached = biomeCache.getOrDefault(b, Float.NaN);
        if (!Float.isNaN(cached)) {
            return cached;
        }
        float temp = BiomeTempData.getBiomeTemp(w, b);
        biomeCache.put(b, temp);
        return temp;
    }

    /**
     * Get Climate temperature adjustment, this value is consistent.
     * Range: [COLD_PERIOD_BOTTOM_T10, WARM_PERIOD_PEAK]
     * Now, it is [-90, 8]
     */
    public static float climate(LevelReader w,BlockPos pos) {
        if (w instanceof Level) {
            return WorldClimate.getTemp((Level) w,pos);
        }
        return 0;
    }

    /**
     * 到达此处的气候：露天处就是当前气候 {@link #climate}；被不透光满方块覆盖处是它按覆盖厚度滞后、衰减后的值，
     * 持续的气候最终原样到达。
     * <p>
     * The climate reaching this place: the current climate in the open; under opaque full blocks, that climate low-pass
     * filtered by the cover, so lasting climate arrives unchanged and shorter changes arrive damped and late.
     */
    public static float coveredClimate(LevelReader w, BlockPos pos) {
        int cover = ClimateCover.coverAbove(w, pos);
        return cover == 0 ? climate(w, pos) : coveredClimate(w, pos, cover);
    }

    private static float coveredClimate(LevelReader w, BlockPos pos, int cover) {
        return w instanceof Level level ? WorldClimate.getCoveredTemp(level, pos, cover) : 0;
    }


    /**
     * 超平坦维度没有地下：参考高度以下一律按参考高度算，没有地热。按维度的生成器判断，所以超平坦存档里的下界和末地
     * 照常；拿不到所属服务端世界的读取器按普通地形算。
     * <p>
     * A superflat dimension has no underground: everything below the altitude reference level reads as that level,
     * with no geothermal heat. Decided per dimension by its chunk generator, so a superflat save's Nether and End stay
     * normal; a reader without a server level counts as normal terrain.
     */
    public static boolean hasNoUnderground(LevelReader w) {
        return w instanceof WorldGenLevel level && hasNoUnderground(level.getLevel().getChunkSource().getGenerator());
    }

    /** {@link #hasNoUnderground(LevelReader)} for a chunk generator: superflat generators have no underground. */
    public static boolean hasNoUnderground(ChunkGenerator generator) {
        return generator instanceof FlatLevelSource;
    }

    /**
     * 海拔造成的长期温差：参考高度以上每格 -0.006 °C，以下每格 +0.03 °C，处处连续；超平坦维度参考高度以下为 0。
     * <p>
     * Long-term altitude temperature change, continuous everywhere: -0.006 °C per block above the reference level and
     * +0.03 °C per block below it; 0 below the reference level in a superflat dimension.
     */
    public static float altitude(LevelReader w, BlockPos pos) {
        int y = pos.getY();
        return altitude(y, y <= ALTITUDE_REFERENCE_LEVEL && hasNoUnderground(w));
    }

    /** The altitude term at height {@code y}; {@code noUnderground} is true in a superflat dimension. */
    public static float altitude(int y, boolean noUnderground) {
        if (y > ALTITUDE_REFERENCE_LEVEL)
            return (y - ALTITUDE_REFERENCE_LEVEL) * TEMPERATURE_CHANGE_PER_BLOCK_ABOVE_REFERENCE;
        return noUnderground ? 0 : (ALTITUDE_REFERENCE_LEVEL - y) * TEMPERATURE_CHANGE_PER_BLOCK_BELOW_REFERENCE;
    }


    /**
     * Default temperature query for gameplay and measurements at a block position.
     * On the server thread, a readable live or stored material temperature takes precedence.
     * The air temperature uses available
     * air data or the natural background, with local analytic fields composed at the block center.
     * Other callers receive the natural background.
     *
     * @param world world containing the position
     * @param pos block position to sample
     * @return the temperature at {@code pos} in degrees Celsius
     */
    public static float getTempAtBlock(LevelReader world, BlockPos pos) {
        return getTempAtBlock(world, pos, (MaterialSample) null);
    }

    /** Selects the temperature required by a rule with a defined sampling purpose. */
    public enum BlockTemperatureUse {
        /** Local air for rules that respond to the environment, such as snowfall and room controls. */
        SURROUNDINGS,
        /** Body temperature with a pause while a known material record is unavailable. */
        PHASE_CHANGE
    }

    /**
     * Samples a block position for a rule with an explicit air or phase-change requirement.
     * Use {@link #getTempAtBlock(LevelReader, BlockPos)} for ordinary block temperature readings.
     * Phase readings use the environmental estimate only for loaded bodies without a thermal record.
     *
     * @param world world containing the position
     * @param pos block position to sample
     * @param use temperature required by the rule
     * @return the temperature in degrees Celsius, or NaN when a phase reading is unavailable
     */
    public static double getTempAtBlock(LevelReader world, BlockPos pos, BlockTemperatureUse use) {
        return switch (use) {
            case SURROUNDINGS -> MinecraftThermalInput.sampleAirTemperature(world, pos, naturalAir(world, pos));
            case PHASE_CHANGE -> world instanceof ServerLevel server && server.getServer().isSameThread()
                    ? MinecraftThermalInput.sampleMaterialTemperatureWithEstimate(server, pos) : Double.NaN;
        };
    }

    /**
     * Reads the position with the same material-first policy as {@link #getTempAtBlock(LevelReader, BlockPos)}.
     * When ambient temperature supplies the value, the material sample is cleared.
     *
     * @param world world containing the position
     * @param pos block position to sample
     * @param material optional caller-owned output for the live or stored material record
     * @return the temperature in degrees Celsius
     */
    public static float getTempAtBlock(LevelReader world, BlockPos pos, @Nullable MaterialSample material) {
        if (world instanceof ServerLevel server && server.getServer().isSameThread()) {
            double temperature = MinecraftThermalInput.materialTemperature(server, pos, material);
            if (Double.isFinite(temperature)) return (float) temperature;
        }
        if (material != null) material.clear();
        return (float) getTempAtBlock(world, pos, BlockTemperatureUse.SURROUNDINGS);
    }

    /**
     * Samples room surroundings using an alternate biome reader for the natural background. The server
     * level supplies the dimension, altitude, cover and published temperature at the sampled position.
     *
     * @param world server level containing the position
     * @param pos block position to sample
     * @param biomeView biome reader provided by the room scan, typically restricted to loaded terrain
     * @return the air temperature in degrees Celsius
     */
    public static double getTempAtBlock(ServerLevel world, BlockPos pos, LevelReader biomeView) {
        return MinecraftThermalInput.sampleAirTemperature(world, pos, naturalBackground(world, biomeView, pos));
    }

    /**
     * Samples the environment at the player's eyes for body thermoregulation.
     *
     * @param player player being sampled
     * @param eyeBlock block containing the eyes, used for the natural background and reusable by the caller
     * @param sample caller-owned output for the thermal field and radiation readings
     * @return the air temperature in degrees Celsius
     */
    public static double samplePlayerAirTemperature(ServerPlayer player, BlockPos eyeBlock, ThermalEnvironmentSample sample) {
        return MinecraftThermalInput.samplePlayerAirTemperature(player, naturalAir(player.level(), eyeBlock), sample);
    }

    /**
     * Samples the environment at the center of a dropped item.
     *
     * @param item dropped item being sampled
     * @param sample caller-owned output for the thermal field and radiation readings
     * @return the air temperature in degrees Celsius, with the natural background as fallback
     */
    public static double sampleDroppedItemAirTemperature(ItemEntity item, ThermalEnvironmentSample sample) {
        double natural = naturalAir(item.level(), item.blockPosition());
        MinecraftThermalInput.sampleDroppedItemAirAndRadiation(item, natural, sample);
        return sample.airAvailable() ? sample.airTemperatureC() : natural;
    }

    /**
     * Samples the environment just above a placed thermal reservoir.
     *
     * @param world server level containing the reservoir
     * @param pos block containing the reservoir
     * @param sample caller-owned output for the thermal field and radiation readings
     * @return the air temperature in degrees Celsius, with the natural background as fallback
     */
    public static double samplePlacedReservoirAirTemperature(ServerLevel world, BlockPos pos, ThermalEnvironmentSample sample) {
        double natural = naturalAir(world, pos);
        MinecraftThermalInput.samplePlacedReservoirAirAndRadiation(world, pos, natural, sample);
        return sample.airAvailable() ? sample.airTemperatureC() : natural;
    }

    /** Common undisturbed background for air and matter, including the climate lag under cover. */
    public static float naturalAir(LevelReader world, BlockPos pos) {
        return naturalBackground(world, world, pos);
    }

    /** Per-block background for main-thread checkpoint readers. The supplied position belongs to the caller. */
    public static java.util.function.IntToDoubleFunction sectionBackground(LevelReader level, long section,
            BlockPos.MutableBlockPos scratch) {
        int x = net.minecraft.core.SectionPos.x(section) << 4;
        int y = net.minecraft.core.SectionPos.y(section) << 4;
        int z = net.minecraft.core.SectionPos.z(section) << 4;
        return local -> naturalAir(level, scratch.set(x + (local & 15), y + (local >>> 8), z + (local >>> 4 & 15)));
    }

    private static final ThreadLocal<BlockPos.MutableBlockPos> NATURAL_BIOME_POS =
            ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    /** The field, thermometer and infrared all use the biome at the center of this 4-block Brick. */
    public static float naturalBiome(LevelReader world, BlockPos pos) {
        return biome(world, NATURAL_BIOME_POS.get().set((pos.getX() & ~3) + 2,
                (pos.getY() & ~3) + 2, (pos.getZ() & ~3) + 2));
    }

    private static float naturalBackground(LevelReader world, LevelReader biomeView, BlockPos pos) {
        float natural = dimension(world) + naturalBiome(biomeView, pos) + altitude(world, pos) + coveredClimate(world, pos);
        return Math.max(FHConfig.SERVER.CLIMATE.absoluteZeroCelsius.get().floatValue(), natural);
    }

    /**
     * Convenience method for checking is Blizzard in specific world
     *
     * */
    public static boolean isBlizzard(LevelReader w,ChunkPos cp) {
        if (w instanceof Level l) {
            return WorldClimate.isBlizzard(l,cp);
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

    /** Evaluates a planted crop with the fertilizer at its root. */
    public static PlantStatus checkPlantStatus(LevelAccessor level, BlockPos pos, @Nullable PlantTempData data) {
        if (data == null) return PlantStatus.NOT_PLANT;
        return checkPlantStatus(level, pos, data, level.getBlockState(CropFertilization.soil(level, pos)));
    }

    /** The soil state supplies fertilizer while the crop position supplies the temperature. */
    public static PlantStatus checkPlantStatus(LevelAccessor level, BlockPos pos, PlantTempData data, BlockState soil) {
        if (plantWeatherBlocksGrowth(level, pos, data)) {
            return data.willDie() ? PlantStatus.WILL_DIE : PlantStatus.CAN_SURVIVE;
        }
        return plantTemperatureStatus(CropFertilization.temperatureData(data, soil), getTempAtBlock(level, pos));
    }

    /** Exposed plants can be harmed by their recipe's weather hazards even in a warm area. */
    public static boolean plantWeatherBlocksGrowth(LevelAccessor level, BlockPos pos, PlantTempData data) {
        if ((!data.blizzardVulnerable() && !data.snowVulnerable()) || !openToAir(level, pos)) return false;
        ChunkPos chunk = new ChunkPos(pos);
        return (data.blizzardVulnerable() && WorldClimate.isBlizzard(level, chunk))
                || (data.snowVulnerable() && WorldClimate.isSnowing(level, chunk));
    }

    /** Classifies the temperature alone using the effective recipe, after applying any fertilizer. */
    public static PlantStatus plantTemperatureStatus(PlantTempData data, float temperature) {
        if (data.isValidTemperature(TemperatureType.BONEMEAL, temperature)) return PlantStatus.CAN_FERTILIZE;
        if (data.isValidTemperature(TemperatureType.GROW, temperature)) return PlantStatus.CAN_GROW;
        if (data.isValidTemperature(TemperatureType.SURVIVE, temperature) || !data.willDie()) return PlantStatus.CAN_SURVIVE;
        return PlantStatus.WILL_DIE;
    }
}
