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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.common.util.LazyOptional;
import top.theillusivec4.curios.api.type.ISlotType;

import java.util.UUID;

public class TemperatureComputation {
    public static final UUID ENV_TEMP_ATTRIBUTE_UUID = UUID.fromString("95c1eab4-8f3a-4878-aaa7-a86722cdfb07");
    protected static float environment(Player player, PlayerTemperatureData data) {
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

        // Burning temperature
        // This shift ranges [150, 150]
        if (player.isOnFire())
            envtemp += FHConfig.SERVER.onFireTempModifier.get();

        return envtemp;
    }

    protected static void effective(Player player, PlayerTemperatureData data, HeatingDeviceContext ctx) {
        float envtemp = (float) player.getAttributeValue(FHAttributes.ENV_TEMPERATURE.get());
        // Range [0, 100]
        int wind = WorldTemperature.wind(player.level());
        // Environment-Body Exchange
        for (PlayerTemperatureData.BodyPart part : PlayerTemperatureData.BodyPart.values()) {
            // ranges [0, 1]
            float partConductivity = data.getThermalConductivityByPart(player, part);
            // fluid conductivity is different in different medium,
            // fluid resistance [0,1] from clothing helps dealing with this
            // by linearly diminishing the conductivity multiplier due to various fluid movement
            float partFluidResist = data.getFluidResistanceByPart(player, part);
            if (player.isInWater())
                partConductivity *= 0.9F * (1 - partFluidResist);
            else if (player.isInPowderSnow)
                partConductivity *= 0.8F * (1 - partFluidResist);
            else {
                float airConductivity = 0.3F; // base air conductivity stays
                // [0,1]
                float effectiveWind = Mth.clamp(wind, 0, 100) / 100F;
                // gets up to 0.7F
                airConductivity += effectiveWind * 0.4F * (1 - partFluidResist);
                // gets up to 0.9F
                if (player.hasEffect(FHMobEffects.WET.get())) {
                    airConductivity += 0.2F * (1 - partFluidResist);
                }
                partConductivity *= airConductivity;
            }

            // This is a body part's "Body Temperature" from last time
            float partBodyTemp = data.getTemperatureByPart(part);
            // Body ends have a 5C additional effect
            float partEnvTemp = envtemp - (part.isBodyEnd() ? 5 : 0);
            // Env and Body exchanges temperature
            float partBodyEnvExchangeTemp = (partEnvTemp - partBodyTemp) * partConductivity;
            float partEffectiveTemp = partBodyTemp + partBodyEnvExchangeTemp;
            // Store them in context
            ctx.setPartData(part, partBodyTemp, partEffectiveTemp);
        }
    }

    protected static void equipmentHeating(Player player, PlayerTemperatureData data, HeatingDeviceContext ctx) {
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

    public static final float FOOD_EXHAUST_COLD=.05F;
    protected static FastEnumMap<PlayerTemperatureData.BodyPart, Float> body(Player player, PlayerTemperatureData data, HeatingDeviceContext ctx) {
        Level world = player.level();
        // Apply Exchanged Temperature, and Self-Heating
        // Temporary storage map
        FastEnumMap<PlayerTemperatureData.BodyPart, Float> fem = new FastEnumMap<>(PlayerTemperatureData.BodyPart.values());
        for (PlayerTemperatureData.BodyPart part : PlayerTemperatureData.BodyPart.values()) {
            // Apply effective heat exchange to part temperature
            HeatingDeviceContext.BodyPartContext pctx = ctx.getPartData(part);
            // Part Body Temperature
            float pbTemp = pctx.getBodyTemperature();
            float dt = pctx.getEffectiveTemperature() - pbTemp;
            // By default heatExchangeTimeConstant = 167
            // Since this logic is invoked every 20 ticks (1s), this means
            // 1 unit = 0.006 degrees per second
            float unit = 1F / FHConfig.SERVER.heatExchangeTimeConstant.get();
            // Since 1Y degree deviation leads to hypothermia, that means at one unit rate,
            // it takes 167 (= 1/0.006) seconds to reach hypothermia
            // For every heatExchangeTimeConstant (default = 5) degrees of deviation, we increase the loss rate by 1 unit.
            // Time = 167 / (Deviation / 5)
            // Scenarios: (no self-heating, environment effective temp -13C)
            // Deviation 50Y, Rate 10 units, 16.7 sec to hypothermia
            // Deviation 53% * 50Y = 24Y, Rate 5 units, 34.8 sec to hypothermia (effectively, straw suit)
            // Deviation 36% * 50Y = 18Y, Rate 3.6 units, 46.4 sec to hypothermia (effectively, leather suit)
            // Deviation 26% * 50Y = 13Y, Rate 2.6 units, 64.2 sec to hypothermia (effectively, wool suit)

            // May be negative! (when dt < 0)
            float heatExchangedUnits = (float) (unit * (dt / FHConfig.SERVER.heatExchangeTempConstant.get()));

            // Self-Heating
            float selfHeatRate = data.getDifficulty().heat_unit; // normally 1
            float movementHeatedUnits = 0;
            // Apply Self-heating based on movement status
            // Food exhaustion is handled by Vanilla, so we don't repeat here
            double speedSquared = player.getDeltaMovement().horizontalDistanceSqr(); // Horizontal movement speed squared
            boolean isSprinting = player.isSprinting();
            boolean isOnVehicle = player.getVehicle() != null;
            boolean isWalking = speedSquared > 0.001 && !isSprinting && !isOnVehicle;
            if (isSprinting) {
                movementHeatedUnits += 2 * selfHeatRate * unit; // Running increases temperature by 4 units
            } else if (isWalking) { // Assuming there's a method to check walking
                movementHeatedUnits += 1 * selfHeatRate * unit; // Walking increases temperature by 2 units
            } else {
                movementHeatedUnits += 0.5F * selfHeatRate *unit;
            }

            // Additional Homeostasis using Stored (Food) Energy
            float homeostasisUnits = 0;
            // homeostasis only happens when deviation is negative even after heat exchange and movement
            // Note 0Y here represents the normal body temperature of 37C
            final float deviation = 0 + (pbTemp + heatExchangedUnits + movementHeatedUnits);
            // We apply additional units based on a deviation need, exhausting more food
            if (deviation < 0 && player.getFoodData().getFoodLevel() > 0) {
                if (deviation > -0.5) {
                    homeostasisUnits += 1F * selfHeatRate * unit;
                    player.causeFoodExhaustion(FOOD_EXHAUST_COLD * 2F * part.area);
                } else if (deviation > -1) {
                    homeostasisUnits += 1.5F * selfHeatRate * unit;
                    player.causeFoodExhaustion(FOOD_EXHAUST_COLD * 3F * part.area);
                } else {
                    homeostasisUnits += 2F * selfHeatRate * unit;
                    player.causeFoodExhaustion(FOOD_EXHAUST_COLD * 4F * part.area);
                }
            }

                            /*
                            FHMain.LOGGER.debug("Deviation: " + deviation);
                            FHMain.LOGGER.debug("Homeostasis: " + homeostasisUnits);
                            FHMain.LOGGER.debug("Movement: " + movementHeatedUnits);
                            FHMain.LOGGER.debug("Exchange: " + heatExchangedUnits);
                             */

            // Apply all to pbTemp
            pbTemp += heatExchangedUnits + movementHeatedUnits + homeostasisUnits;

            // FHMain.LOGGER.debug("pbTemp: " + pbTemp);

            // degree I/II/III burn if dt=+20/+30/+40
            if (dt > 40) {
                player.hurt(FHDamageSources.hyperthermiaInstant(world), 2.0F);
            } else if (dt > 30) {
                player.hurt(FHDamageSources.hyperthermiaInstant(world), 1.5F);
            } else if (dt > 20) {
                player.hurt(FHDamageSources.hyperthermiaInstant(world), 1.0F);
            }

            // degree I/II/III freeze if dt=-50/-60/-70
            if (dt < -70) {
                player.hurt(FHDamageSources.hyperthermiaInstant(world), 2.0F);
            } else if (dt < -60) {
                player.hurt(FHDamageSources.hyperthermiaInstant(world), 1.5F);
            } else if (dt < -50) {
                player.hurt(FHDamageSources.hyperthermiaInstant(world), 1.0F);
            }

            fem.put(part, pbTemp);
        }

        // Calculate heat transfer between each part
        //From leg/chest/head share temperature.
        float coreTemp = 0;
        for (PlayerTemperatureData.BodyPart corePart : PlayerTemperatureData.BodyPart.CoreParts) {
            coreTemp += fem.get(corePart) * corePart.affectsCore;
        }
        for (PlayerTemperatureData.BodyPart corePart : PlayerTemperatureData.BodyPart.CoreParts)
            fem.put(corePart, coreTemp);

        //From leg to feets
        final float transferRate = 0.1F;
        final float maxDelta = 3F;
        final float minDelta = 0.1F;

        float dlegfeet = Mth.clamp(fem.get(PlayerTemperatureData.BodyPart.LEGS) - fem.get(PlayerTemperatureData.BodyPart.FEET), -maxDelta, maxDelta);
        if (Mth.abs(dlegfeet) > minDelta) {
            float newfeet = fem.get(PlayerTemperatureData.BodyPart.FEET) + dlegfeet * transferRate;
            float newleg = fem.get(PlayerTemperatureData.BodyPart.LEGS) - dlegfeet * transferRate;
            fem.put(PlayerTemperatureData.BodyPart.FEET, newfeet);
            fem.put(PlayerTemperatureData.BodyPart.LEGS, newleg);
        }

        //from chest to hands
        float dhandchest = Mth.clamp(fem.get(PlayerTemperatureData.BodyPart.TORSO) - fem.get(PlayerTemperatureData.BodyPart.HANDS), -maxDelta, maxDelta);
        if (Mth.abs(dhandchest) > minDelta) {
            float newhands = fem.get(PlayerTemperatureData.BodyPart.HANDS) + dhandchest * transferRate;
            float newtorso = fem.get(PlayerTemperatureData.BodyPart.TORSO) - dhandchest * transferRate;
            fem.put(PlayerTemperatureData.BodyPart.HANDS, newhands);
            fem.put(PlayerTemperatureData.BodyPart.TORSO, newtorso);
        }

        return fem;
    }
}
