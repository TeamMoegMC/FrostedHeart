package com.teammoeg.frostedheart.content.climate.player;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.CompatModule;
import com.teammoeg.chorda.util.struct.FastEnumMap;
import com.teammoeg.frostedheart.bootstrap.common.FHAttributes;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.bootstrap.reference.FHDamageSources;
import com.teammoeg.frostedheart.compat.curios.CuriosCompat;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.common.util.LazyOptional;
import top.theillusivec4.curios.api.type.ISlotType;

import java.util.UUID;

public class TemperatureComputation {
    public static final UUID ENV_TEMP_ATTRIBUTE_UUID = UUID.fromString("95c1eab4-8f3a-4878-aaa7-a86722cdfb07");
    protected static float environment(ServerPlayer player, PlayerTemperatureData data) {
        // World Temp: Dimension, Biome, Climate, Time, heat adjusts
        Level world = player.level();
        BlockPos pos = new BlockPos((int) player.getX(), (int) player.getEyeY(), (int) player.getZ());
        // We use 37C based temperature here.
        // The base temperature means around -10C, which becomes -47C.
        float envtemp = WorldTemperature.air(world, pos) - 37F; // 37-based

        // Surrounding block temperature.
        // We calculate the block temperature using a separate pool.
        // See blockTemp usage for more details.
        // This shift ranges a lot.
        float bt = data.blockTemp;
        envtemp += bt;

        // Day-night temperature
        int skyLight = world.getChunkSource().getLightEngine().getLayerListener(LightLayer.SKY).getLightValue(pos);
        float dayTime = world.getDayTime() % 24000L;
        float relativeTime = Mth.sin((float) Math.toRadians(dayTime / ((float) 200 / 3))); // range from -1 to 1
        if (skyLight < FHConfig.SERVER.tempSkyLightThreshold.get()) {
            relativeTime = -1;
        }

        // Weather temperature modifier
        // This shift ranges [-10, 0]
        float weatherMultiplier = 1.0F;
        if (world.isRaining() && WorldTemperature.isRainingAt(player.blockPosition(), world)) {
            // Decrement by 5C
            envtemp -= FHConfig.SERVER.snowTempModifier.get();
            if (world.isThundering()) {
                // Decrement by 10C
                envtemp -= FHConfig.SERVER.blizzardTempModifier.get();
            }
            // Due to wetness, daily day-night amplitude shrinks
            weatherMultiplier = 0.2F;
        }

        // Apply day-night amplitude modification
        // This shift ranges [-10, 10]
        envtemp += relativeTime * FHConfig.SERVER.dayNightTempAmplitude.get() * weatherMultiplier;

        if (player.isInPowderSnow)
            envtemp = -30 - 37;
        if (player.isInWater()) {
            // water cannot freeze or boil
            envtemp = Mth.clamp(envtemp, -37, 63);
        }
        if (player.isOnFire())
            envtemp = 300 - 37;
        if (player.isInLava())
            envtemp = 1000 - 37;

        return envtemp;
    }

    protected static void equipmentHeating(ServerPlayer player, PlayerTemperatureData data, HeatingDeviceContext ctx) {
        // Curios slots
        if (CompatModule.isCuriosLoaded())
            for (Pair<ISlotType, ItemStack> i : CuriosCompat.getAllCuriosAndSlotsIfVisible(player)) {
                HeatingDeviceSlot slot = new HeatingDeviceSlot(i.getFirst());
                LazyOptional<BodyHeatingCapability> cap = FHCapabilities.EQUIPMENT_HEATING.getCapability(i.getSecond());
                if (cap.isPresent()) {
                    BodyHeatingCapability eq = cap.resolve().get();
                    eq.tickHeating(slot, i.getSecond(), ctx);
                }
            }
        // Equipment slots
        for (EquipmentSlot eslot : EquipmentSlot.values()) {
            HeatingDeviceSlot slot = new HeatingDeviceSlot(eslot);
            ItemStack item = player.getItemBySlot(eslot);
            LazyOptional<BodyHeatingCapability> cap = FHCapabilities.EQUIPMENT_HEATING.getCapability(item);
            if (cap.isPresent()) {
                BodyHeatingCapability eq = cap.resolve().get();
                eq.tickHeating(slot, item, ctx);
            }
        }
    }

    protected static void burning(ServerPlayer player, PlayerTemperatureData data) {
        // TODO: apply burn or frostbite on specific parts

        RandomSource r = player.getRandom();

        // note that all temps are 0C based
        // burning
        // https://www.sciencedirect.com/topics/medicine-and-dentistry/thermal-injury#:~:text=Mechanism%20of%20Injury,is%20given%20in%20Table%202.&text=Source:%20Data%20modified%20from%20Moritz,Churchill%20Livingstone%20(Chapter%205).
        float highestEffectiveTemperature = data.getHighestFeelTemp();
        if (highestEffectiveTemperature > 250) {
            if (r.nextFloat() < 1.0)
                player.hurt(FHDamageSources.hyperthermiaInstant(player.level()), 4.0F);
        }
        else if (highestEffectiveTemperature > 200) {
            if (r.nextFloat() < 0.75)
                player.hurt(FHDamageSources.hyperthermiaInstant(player.level()), 3.0F);
        }
        else if (highestEffectiveTemperature > 150) {
            if (r.nextFloat() < 0.5)
                player.hurt(FHDamageSources.hyperthermiaInstant(player.level()), 2.0F);
        }
        else if (highestEffectiveTemperature > 100) {
            if (r.nextFloat() < 0.25)
                player.hurt(FHDamageSources.hyperthermiaInstant(player.level()), 1.0F);
        }

        // frostbite
        float lowestEffectiveTemperature = data.getLowestFeelTemp();
        if (lowestEffectiveTemperature < -250) {
            if (r.nextFloat() < 1.0)
                player.hurt(FHDamageSources.hypothermiaInstant(player.level()), 4.0F);
        }
        else if (lowestEffectiveTemperature < -200) {
            if (r.nextFloat() < 0.75)
                player.hurt(FHDamageSources.hypothermiaInstant(player.level()), 3.0F);
        }
        else if (lowestEffectiveTemperature < -150) {
            if (r.nextFloat() < 5)
                player.hurt(FHDamageSources.hypothermiaInstant(player.level()), 2.0F);
        }
        else if (lowestEffectiveTemperature < -100) {
            if (r.nextFloat() < 0.25)
                player.hurt(FHDamageSources.hypothermiaInstant(player.level()), 1.0F);
        }
    }

    /**
     * Compute feel temperature from a realistic model.
     * <a href="https://en.wikipedia.org/wiki/Apparent_temperature">...</a>
     *
     * @param dryT 0C based dry effective temperature
     * @param relativeHumidity [0,1]
     * @param relativeWindSpeed [0,1]
     * @return 0C based feel temperature
     */
    public static double feelTemperature(double dryT, double relativeHumidity, double relativeWindSpeed) {
        double e = waterVaporPressure(dryT, relativeHumidity);
        double v = windSpeed(relativeWindSpeed);
        return dryT + 0.33 * e - 0.7 * v - 4.00;
    }

    // https://en.wikipedia.org/wiki/Apparent_temperature
    public static double waterVaporPressure(double dryT, double relativeHumidity) {
        return relativeHumidity * 6.105 * Math.exp((17.27 * dryT) / (237.7 + dryT));
    }

    // https://en.wikipedia.org/wiki/Beaufort_scale
    public static double windSpeed(double relativeWindSpeed) {
        // we assume the greatest wind speed is 35 m/s (most intense hurricane)
        return relativeWindSpeed * 35;
    }
}
