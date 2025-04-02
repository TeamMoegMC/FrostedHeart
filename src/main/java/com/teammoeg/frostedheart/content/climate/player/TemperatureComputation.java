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
        if (player.isInWater())
            envtemp = Math.max(envtemp, -37);
        if (player.isOnFire())
            envtemp = 300 - 37;
        if (player.isInLava())
            envtemp = 1000 - 37;

        return envtemp;
    }

    protected static void effective(ServerPlayer player, PlayerTemperatureData data, HeatingDeviceContext ctx) {
        float envtemp = (float) player.getAttributeValue(FHAttributes.ENV_TEMPERATURE.get());

        // Environment-Body Exchange
        for (PlayerTemperatureData.BodyPart part : PlayerTemperatureData.BodyPart.values()) {
            // ranges [0, 1]
            float partConductivity = data.getThermalConductivityByPart(player, part);
            // This is a body part's "Body Temperature" from last time
            float partBodyTemp = data.getBodyTempByPart(part);
            // Body ends have a 5C additional effect
            float partEnvTemp = envtemp - (part.isBodyEnd() ? 5 : 0);
            // Env and Body exchanges temperature
            float partBodyEnvExchangeTemp = (partEnvTemp - partBodyTemp) * partConductivity;
            float partEffectiveTemp = partBodyTemp + partBodyEnvExchangeTemp;
            // Store them in context
            ctx.setPartData(part, partBodyTemp, partEffectiveTemp);
        }
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

        // note that all temps are 37C based
        // burning
        // https://www.sciencedirect.com/topics/medicine-and-dentistry/thermal-injury#:~:text=Mechanism%20of%20Injury,is%20given%20in%20Table%202.&text=Source:%20Data%20modified%20from%20Moritz,Churchill%20Livingstone%20(Chapter%205).
        float highestEffectiveTemperature = data.getHighestFeelTemp();
        if (highestEffectiveTemperature > 200) {
            if (r.nextFloat() < 1.0)
                player.hurt(FHDamageSources.hyperthermiaInstant(player.level()), 4.0F);
        }
        else if (highestEffectiveTemperature > 150) {
            if (r.nextFloat() < 0.75)
                player.hurt(FHDamageSources.hyperthermiaInstant(player.level()), 3.0F);
        }
        else if (highestEffectiveTemperature > 100) {
            if (r.nextFloat() < 0.5)
                player.hurt(FHDamageSources.hyperthermiaInstant(player.level()), 2.0F);
        }
        else if (highestEffectiveTemperature > 70) {
            if (r.nextFloat() < 0.25)
                player.hurt(FHDamageSources.hyperthermiaInstant(player.level()), 1.0F);
        }

        // frostbite
        float lowestEffectiveTemperature = data.getLowestFeelTemp();
        if (lowestEffectiveTemperature < -200) {
            if (r.nextFloat() < 1.0)
                player.hurt(FHDamageSources.hypothermiaInstant(player.level()), 4.0F);
        }
        else if (lowestEffectiveTemperature < -150) {
            if (r.nextFloat() < 0.75)
                player.hurt(FHDamageSources.hypothermiaInstant(player.level()), 3.0F);
        }
        else if (lowestEffectiveTemperature < -100) {
            if (r.nextFloat() < 5)
                player.hurt(FHDamageSources.hypothermiaInstant(player.level()), 2.0F);
        }
        else if (lowestEffectiveTemperature < -50) {
            if (r.nextFloat() < 0.25)
                player.hurt(FHDamageSources.hypothermiaInstant(player.level()), 1.0F);
        }
    }

    public static final float FOOD_EXHAUST_COLD=.05F;
    protected static FastEnumMap<PlayerTemperatureData.BodyPart, Float> body(ServerPlayer player, PlayerTemperatureData data, HeatingDeviceContext ctx) {
        Level world = player.level();
        // Range 0-100
        int wind = WorldTemperature.wind(world);
        // Range 0-1
        float openness = data.getAirOpenness();
        // [0,1]
        float effectiveWind = openness * Mth.clamp(wind, 0, 100) / 100F;

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

            // fluid conductivity is different in different medium,
            // fluid resistance [0,1] from clothing helps dealing with this
            // by linearly diminishing the conductivity multiplier due to various fluid movement

            // wikipedia: https://en.wikipedia.org/wiki/Thermal_conductivity_and_resistivity
            // thermal conductivity:
            // water: 0.6089
            // air: 0.026
            // powdered snow: 0.05
            // water/air = 23.41
            // we use ratio = 25

            float fluidModifier = 0F;
            float partFluidResist = data.getFluidResistanceByPart(player, part);
            if (player.isInWater())
                fluidModifier = 25F * (1 - partFluidResist);
            // interestingly powdered snow does not affect conductivity that much, it just makes envtemp low
            // however, the human body melts snow, and that generates water, which may go into the body,
            // if clothing is not fluid resisting enough, and take away heats.
            // thus a solution here is an average...
            else if (player.isInPowderSnow)
                fluidModifier = 15F * (1 - partFluidResist);
            else {
                // gets up to 5F
                fluidModifier += 5F * effectiveWind * (1 - partFluidResist);
                // evaporation takes away a LOT of heat. it gets up to 10F
                if (player.hasEffect(FHMobEffects.WET.get())) {
                    fluidModifier += 10F * (1 - partFluidResist);
                }
            }

            // May be negative! (when dt < 0)
            float heatExchangedUnits = (float) ((1 + fluidModifier) * unit * (dt / FHConfig.SERVER.heatExchangeTempConstant.get()));

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
                movementHeatedUnits += 4 * selfHeatRate * unit; // Running increases temperature by 4 units
            } else if (isWalking) { // Assuming there's a method to check walking
                movementHeatedUnits += 2 * selfHeatRate * unit; // Walking increases temperature by 2 units
            } else {
                movementHeatedUnits += 1F * selfHeatRate *unit;
            }

            // Additional Homeostasis using Stored (Food) Energy
            float homeostasisUnits = 0;
            // homeostasis only happens when deviation is negative even after heat exchange and movement
            // Note 0Y here represents the normal body temperature of 37C
            final float deviation = 0 + (pbTemp + heatExchangedUnits + movementHeatedUnits);
            // We apply additional units based on a deviation need, exhausting more food
            if (deviation < 0 && player.getFoodData().getFoodLevel() > 0) {
                if (deviation > -0.5) {
                    homeostasisUnits += 2F * selfHeatRate * unit;
                    player.causeFoodExhaustion(FOOD_EXHAUST_COLD * 2F * part.area);
                } else if (deviation > -1) {
                    homeostasisUnits += 3F * selfHeatRate * unit;
                    player.causeFoodExhaustion(FOOD_EXHAUST_COLD * 3F * part.area);
                } else {
                    homeostasisUnits += 4F * selfHeatRate * unit;
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
            pbTemp += heatExchangedUnits;

            if (part.canGenerateHeat()) {
                pbTemp += movementHeatedUnits + homeostasisUnits;
            } else {
                pbTemp += movementHeatedUnits;
            }

            // FHMain.LOGGER.debug("pbTemp: " + pbTemp);

            fem.put(part, pbTemp);
        }

        // Calculate heat transfer between each part
        // Core parts share temperature
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
