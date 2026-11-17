/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.player;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.GsonBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalFieldKey;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftGameplayFields;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import com.teammoeg.frostedheart.content.water.capability.WaterLevelCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Full player-tick replay isolates finite food and cold injury; water stays available. */
@GameTestHolder("frostedheart_survival_balance")
@PrefixGameTestTemplate(false)
public final class ThermalSurvivalBalanceGameTests {
    @GameTest(template = "phase0a_empty", batch = "survival_balance", timeoutTicks = 4200)
    public static void finiteFoodColdSurvivalUsesRealFoodEffectsAndDamage(GameTestHelper helper) {
        List<Trace> traces = new ArrayList<>();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO).atY(160).east(512);
        for (double air : new double[]{-50, -80}) for (int clothing : new int[]{0, 1})
            for (boolean legacy : new boolean[]{true, false})
                traces.add(new Trace(helper.getLevel(), origin.east(traces.size() * 16), air, clothing, legacy));
        int[] seconds = {0};
        helper.startSequence().thenExecuteFor(3601, () -> {
            if (seconds[0] >= 3600) return;
            seconds[0]++;
            for (Trace trace : traces) trace.advance(seconds[0]);
        }).thenExecute(() -> {
            try {
                Path output = Path.of("thermal-balance");
                Files.createDirectories(output);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("scope", "Accelerated player-tick replay, 20 player ticks per native tick; static air and roof, no world-source simulation");
                result.put("resources", "20 food plus 5 saturation initially, no food refill, natural regeneration active; water replenished to isolate cold and hunger");
                result.put("legacy", "Audited old body/regulation formulas plus current vanilla food and current thermal illness handling; not an exact historical server replay");
                result.put("bodyTimeSource", "Elapsed ticks only; temperatureChangeRate affects food/drink, not body time");
                result.put("horizonGameSeconds", 3600);
                result.put("cases", traces.stream().map(Trace::result).toList());
                Files.writeString(output.resolve("finite-food-results.json"), new GsonBuilder().setPrettyPrinting().create().toJson(result), StandardCharsets.UTF_8);
                Files.writeString(output.resolve("finite-food-curves.csv"), "case,second,core_c,torso_c,health,food,saturation,exhaustion,last_damage\n"
                        + String.join("\n", traces.stream().flatMap(trace -> trace.rows.stream()).toList()), StandardCharsets.UTF_8);
                helper.assertTrue(traces.stream().allMatch(trace -> trace.player.tickCount > 100), "each player ran the full gameplay tick");
                FHMain.LOGGER.info("THERMAL_FINITE_FOOD_BALANCE,{}", new GsonBuilder().create().toJson(result));
                for (Trace current : traces) {
                    if (current.old) continue;
                    Trace reference = traces.stream().filter(trace -> trace.old && trace.air == current.air
                            && trace.clothing == current.clothing).findFirst().orElseThrow();
                    if (reference.foodZero > 0) helper.assertTrue(current.foodZero > 0
                                    && Math.abs(current.foodZero - reference.foodZero) <= reference.foodZero * 0.25D,
                            current.name() + " food depletion stays within 25% of the old reference");
                    if (current.clothing == 0) helper.assertTrue(current.death > 0
                                    && Math.abs(current.death - reference.death) <= reference.death * 0.25D,
                            current.name() + " cold death stays within 25% of the old reference");
                    else {
                        helper.assertTrue(current.player.isAlive(), current.name() + " keeps the old clothed survival outcome");
                        helper.assertTrue(Math.abs(current.data.getAbsoluteCoreBodyTemp()
                                        - reference.data.getAbsoluteCoreBodyTemp()) < 0.8D,
                                current.name() + " clothed core stays close to the old reference");
                        if (reference.foodZero < 0) helper.assertTrue(Math.abs(current.player.getFoodData().getFoodLevel()
                                        - reference.player.getFoodData().getFoodLevel()) <= 4,
                                current.name() + " remaining food stays within four points of the old reference");
                    }
                }
            } catch (java.io.IOException failure) {
                throw new IllegalStateException("Cannot save finite-food measurement", failure);
            } finally {
                for (Trace trace : traces) trace.close();
                MinecraftThermalInput.closeActiveLevel(helper.getLevel());
            }
        }).thenSucceed();
    }

    private static final class Trace {
        final ServerLevel level;
        final BlockPos feet;
        final double air;
        final int clothing;
        final boolean old;
        final ThermalGameplayBalanceGameTests.Fixture fixture;
        final ServerPlayer player;
        final PlayerTemperatureData data;
        final ThermalFieldKey field;
        final ThermalGameplayBalanceGameTests.LegacyBody reference = new ThermalGameplayBalanceGameTests.LegacyBody();
        final List<String> rows = new ArrayList<>();
        int foodZero = -1, firstHurt = -1, torso34 = -1, death = -1;
        final int initialTick;
        int completedPlayerTick;
        String firstDamage = "", deathDamage = "";

        Trace(ServerLevel level, BlockPos origin, double air, int clothing, boolean old) {
            this.level = level; feet = origin.above(); this.air = air; this.clothing = clothing; this.old = old;
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                level.setBlockAndUpdate(origin.offset(x, 0, z), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(origin.offset(x, 4, z), Blocks.STONE.defaultBlockState());
                for (int y = 1; y <= 3; y++) level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
            fixture = ThermalGameplayBalanceGameTests.fixture(level, "survive", clothing);
            player = fixture.player();
            initialTick = completedPlayerTick = player.tickCount;
            data = PlayerTemperatureData.getCapability(player).orElseThrow(IllegalStateException::new);
            data.setAllPartsBodyTemp(0);
            // Freeze only the new integrator for reference players; the old formulas own their energy.
            // Torso illness and its damage remain active. These air cases do not cross instant-injury thresholds.
            if (old) player.addEffect(new MobEffectInstance(FHMobEffects.INSULATION.get(), -1));
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(5);
            player.getFoodData().setExhaustion(0);
            field = ThermalFieldKey.of(new ResourceLocation("frostedheart", "survival_measure"), UUID.randomUUID(), 0);
            MinecraftGameplayFields.upsert(level, new ThermalAnalyticField(field, 10000,
                    ThermalAnalyticField.CombineMode.OVERRIDE, feet.getX() + .5, feet.getY() + 1.6, feet.getZ() + .5, 3, air));
            hold(); sample(0);
        }

        String name() { return (old ? "legacy" : "current") + "_air" + air + "_clothing" + clothing; }
        void hold() { player.setPos(feet.getX() + .5, feet.getY(), feet.getZ() + .5); player.setDeltaMovement(Vec3.ZERO); }
        void advance(int second) {
            if (death >= 0) return;
            WaterLevelCapability.getCapability(player).ifPresent(water -> {
                water.setWaterLevel(20); water.setWaterSaturationLevel(6); water.setWaterExhaustionLevel(0);
            });
            if (old) {
                double previousExhaustion = reference.exhaustion;
                reference.step(player, data, air, 0, player.getFoodData().getFoodLevel() > 0);
                for (BodyPart part : BodyPart.values()) data.setBodyTempByPart(part, reference.t[part.ordinal()]);
                data.refreshCoreTemperature();
                player.causeFoodExhaustion((float) (reference.exhaustion - previousExhaustion));
            }
            int targetTick = initialTick + second * 20;
            // ServerLevel increments tickCount and calls tick; the connection then calls doTick.
            // Complete native level ticks first, then advance the remaining virtual player ticks.
            while (completedPlayerTick < player.tickCount && player.isAlive()) {
                hold(); player.doTick(); completedPlayerTick++;
            }
            while (player.tickCount < targetTick && player.isAlive()) {
                player.tickCount++; hold(); player.tick(); player.doTick();
                completedPlayerTick = player.tickCount;
            }
            if (foodZero < 0 && player.getFoodData().getFoodLevel() == 0) foodZero = second;
            if (torso34 < 0 && data.getAbsoluteBodyTempByPart(BodyPart.TORSO) < 34) torso34 = second;
            if (firstHurt < 0 && player.getHealth() < player.getMaxHealth() - .01F) {
                firstHurt = second; firstDamage = lastDamage();
            }
            if (!player.isAlive()) { death = second; deathDamage = lastDamage(); }
            if (second % 60 == 0 || second == firstHurt || second == death || second == foodZero) sample(second);
        }

        String lastDamage() { var source = player.getLastDamageSource(); return source == null ? "" : source.getMsgId(); }
        void sample(int second) {
            rows.add(String.format(java.util.Locale.ROOT, "%s,%d,%.6f,%.6f,%.6f,%d,%.6f,%.6f,%s", name(), second,
                    data.getAbsoluteCoreBodyTemp(), data.getAbsoluteBodyTempByPart(BodyPart.TORSO), player.getHealth(),
                    player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel(),
                    player.getFoodData().getExhaustionLevel(), lastDamage()));
        }
        Map<String, Object> result() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("case", name()); result.put("airC", air); result.put("clothing", clothing); result.put("legacyReference", old);
            result.put("foodZeroSeconds", foodZero); result.put("torso34Seconds", torso34); result.put("firstHurtSeconds", firstHurt);
            result.put("firstDamage", firstDamage); result.put("deathSeconds", death); result.put("deathDamage", deathDamage);
            result.put("coreC", data.getAbsoluteCoreBodyTemp()); result.put("health", player.getHealth());
            result.put("food", player.getFoodData().getFoodLevel()); result.put("saturation", player.getFoodData().getSaturationLevel());
            return result;
        }
        void close() { MinecraftGameplayFields.remove(level, field); fixture.close(); }
    }
}
