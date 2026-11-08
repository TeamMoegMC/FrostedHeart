/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.player;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.content.climate.FHTemperatureDifficulty;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import com.teammoeg.frostedheart.content.water.capability.WaterLevelCapability;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Measurement completion is not an assertion of old/new balance equivalence. */
@GameTestHolder("frostedheart_gameplay_balance")
@PrefixGameTestTemplate(false)
public final class ThermalGameplayBalanceGameTests {
    private static final int HORIZON_SECONDS = 3600;
    private static final BodyPart[] PARTS = BodyPart.values();
    private static final Path OUTPUT = Path.of("thermal-balance");

    @GameTest(template = "phase0a_empty", batch = "gameplay_balance", timeoutTicks = 2400)
    public static void measureLegacyAndCurrentBodyCurves(GameTestHelper helper) {
        List<BodyTrace> traces = new ArrayList<>();
        for (double air : new double[]{-20, -50, -80})
                for (int clothing = 0; clothing < 3; clothing++)
                    for (boolean food : new boolean[]{true, false})
                        traces.add(new BodyTrace(helper.getLevel(), air, 0, 0, clothing, food, 37, true));
        for (int clothing = 0; clothing < 3; clothing++)
            for (double flux : new double[]{0, 20, 100})
                traces.add(new BodyTrace(helper.getLevel(), 12, flux, 0, clothing, true, 35, false));
        for (int clothing : new int[]{0, 1})
            traces.add(new BodyTrace(helper.getLevel(), -50, 0, 100, clothing, true, 37, true));
        int[] cursor = {0};
        helper.startSequence().thenExecuteFor(1800, () -> {
            for (int work = 0; work < 160 && cursor[0] < traces.size(); work++) {
                BodyTrace trace = traces.get(cursor[0]);
                trace.step();
                if (trace.second == HORIZON_SECONDS) {
                    FHMain.LOGGER.info("THERMAL_BODY_BALANCE,{}", json(trace.result()));
                    cursor[0]++;
                }
            }
        }).thenExecute(() -> {
            helper.assertTrue(cursor[0] == traces.size(), "every numeric trace completes its full one-hour horizon");
            List<Map<String, Object>> result = traces.stream().map(BodyTrace::result).toList();
            write("body-results.json", json(Map.of(
                    "scope", "Production body kernel replay inside Forge; fixed inputs, not an old-server gameplay/death replay",
                    "legacy", "0c9fe6209 body formulas; no local particle contribution or day/night/weather adjustment",
                    "resources", "Food availability is fixed per trace; exhaustion is measured, no claim of finite-food survival",
                    "cases", result)));
            List<String> csv = new ArrayList<>();
            csv.add("case,second,air_c,flux_w_m2,world_wind,clothing,food_available,current_core_c,current_torso_c,current_feet_c,legacy_core_c,legacy_torso_c,current_food_exhaustion,legacy_food_exhaustion");
            for (BodyTrace trace : traces) csv.addAll(trace.rows);
            write("body-curves.csv", String.join("\n", csv));
            for (BodyTrace trace : traces) {
                if (trace.wind <= 0 || !trace.comparable) continue;
                helper.assertTrue(trace.current36 > 0
                                && Math.abs(trace.current36 - trace.legacy36) <= trace.legacy36 * 0.25D,
                        trace.name() + " strong-wind core cooling time stays within 25% of the old reference");
                helper.assertTrue(trace.current34 > 0
                                && Math.abs(trace.current34 - trace.legacy34) <= trace.legacy34 * 0.25D,
                        trace.name() + " strong-wind injury threshold stays within 25% of the old reference");
            }
        }).thenSucceed();
    }

    private static final class BodyTrace {
        final ServerPlayer player;
        final PlayerTemperatureData data = new PlayerTemperatureData();
        final HeatingDeviceContext context = new HeatingDeviceContext();
        final LegacyBody legacy = new LegacyBody();
        final List<String> rows = new ArrayList<>();
        final double air, flux, wind, initial;
        final boolean food, comparable;
        final int clothing;
        final double hNatural;
        int second, current36 = -1, current34 = -1, legacy36 = -1, legacy34 = -1;
        double exhaustion, maxCoreDifference;

        BodyTrace(ServerLevel level, double air, double flux, double wind, int clothing, boolean food,
                  double initial, boolean comparable) {
            this.air = air; this.flux = flux; this.wind = wind; this.clothing = clothing;
            this.food = food; this.initial = initial; this.comparable = comparable;
            player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "body-replay"));
            player.setGameMode(GameType.SURVIVAL);
            player.setDeltaMovement(Vec3.ZERO);
            player.setSprinting(false);
            data.setDifficulty(FHTemperatureDifficulty.normal);
            data.setAllPartsBodyTemp((float) initial - 37);
            data.refreshCoreTemperature();
            clothing(player, data, clothing);
            for (BodyPart part : PARTS) legacy.t[part.ordinal()] = (float) initial - 37;
            hNatural = PlayerThermalModel.naturalConvectionCoefficientWPerM2K(air);
            sample();
        }

        String name() { return "air" + air + "_flux" + flux + "_wind" + wind + "_cloth" + clothing + "_fed" + food; }

        void step() {
            double seconds = PlayerThermalModel.physiologicalSeconds(1);
            context.reset(player, 1, seconds, data);
            for (BodyPart part : PARTS) PlayerThermalModel.preparePart(data, context, part, air, hNatural,
                    flux, PlayerThermalModel.outdoorWindMPerS(wind), PlayerThermalModel.waterTemperatureC(air),
                    0, 0, 0, false, false);
            double shiver = PlayerThermoregulation.shiveringPowerW(data.getAbsoluteCoreBodyTemp(), 1, food);
            double sweat = PlayerThermoregulation.sweatingPowerW(data.getAbsoluteCoreBodyTemp(), 1, true);
            double evaporation = PlayerThermalModel.evaporationScale(air, 0.5,
                    PlayerThermalModel.outdoorWindMPerS(wind), 1.8, sweat);
            double power = PlayerThermoregulation.sharedBodyPowerW(player, 1, shiver, sweat * evaporation);
            PlayerThermalModel.integrateBody(data, context, air, false, 0, power, evaporation, seconds, false);
            data.refreshCoreTemperature();
            exhaustion += shiver * seconds / PlayerThermoregulation.FOOD_ENERGY_J_PER_EXHAUSTION;
            if (comparable) legacy.step(player, data, air, wind, food);
            second++;
            double core = data.getAbsoluteCoreBodyTemp();
            if (!Double.isFinite(core)) throw new IllegalStateException("nonfinite body in " + name());
            if (core < 36 && current36 < 0) current36 = second;
            if (data.getAbsoluteBodyTempByPart(BodyPart.TORSO) < 34 && current34 < 0) current34 = second;
            if (comparable) {
                if (legacy.core() < 36 && legacy36 < 0) legacy36 = second;
                if (legacy.t[BodyPart.TORSO.ordinal()] + 37 < 34 && legacy34 < 0) legacy34 = second;
                maxCoreDifference = Math.max(maxCoreDifference, Math.abs(core - legacy.core()));
            }
            if (second <= 120 && second % 10 == 0 || second % 60 == 0) sample();
        }

        void sample() {
            rows.add(String.format(Locale.ROOT, "%s,%d,%.3f,%.3f,%.3f,%d,%s,%.6f,%.6f,%.6f,%s,%s,%.6f,%.6f",
                    name(), second, air, flux, wind, clothing, food, data.getAbsoluteCoreBodyTemp(),
                    data.getAbsoluteBodyTempByPart(BodyPart.TORSO), data.getAbsoluteBodyTempByPart(BodyPart.FEET),
                    comparable ? Float.toString(legacy.core()) : "", comparable ? Float.toString(legacy.t[BodyPart.TORSO.ordinal()] + 37) : "",
                    exhaustion, legacy.exhaustion));
        }

        Map<String, Object> result() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("case", name()); result.put("seconds", second); result.put("airC", air);
            result.put("fluxWPerM2", flux); result.put("wind", wind); result.put("clothing", clothing);
            result.put("foodAvailable", food); result.put("physiologicalSecondsPerGameSecond", 8); result.put("initialC", initial);
            result.put("legacyComparable", comparable); result.put("currentCoreC", data.getAbsoluteCoreBodyTemp());
            result.put("currentTorsoC", data.getAbsoluteBodyTempByPart(BodyPart.TORSO));
            result.put("currentCore36Seconds", current36); result.put("currentTorso34Seconds", current34);
            result.put("currentFoodExhaustion", exhaustion);
            if (comparable) {
                result.put("legacyCoreC", legacy.core()); result.put("legacyCore36Seconds", legacy36);
                result.put("legacyTorso34Seconds", legacy34); result.put("legacyFoodExhaustion", legacy.exhaustion);
                result.put("maximumCoreDifferenceC", maxCoreDifference);
                result.put("injuryThresholdOutcomeChanged", (current34 < 0) != (legacy34 < 0));
            }
            return result;
        }
    }

    /** Independent old body reference; source anchors are recorded in the plan, not a copied old runtime. */
    static final class LegacyBody {
        final float[] t = new float[PARTS.length], next = new float[PARTS.length];
        double exhaustion;
        float core() { return t[0] * .1F + t[1] * .5F + t[3] * .4F + 37; }
        void step(ServerPlayer player, PlayerTemperatureData clothingData, double air, double wind, boolean food) {
            double v16 = Math.pow(wind / 100 * 70, .16);
            float environment = (float) (13.12 + .6215 * (air - 37) - 11.37 * v16 + .3965 * (air - 37) * v16);
            for (BodyPart part : PARTS) {
                int i = part.ordinal();
                float insulation = (float) clothingData.getClothDataByPart(player, part).environmentExchangeFactor;
                float exchange = ((environment - (part.isBodyEnd() ? 5 : 0)) - t[i]) * insulation / 1000 / 10;
                float correction = 0, deviation = t[i] + exchange + .001F;
                if (part.canGenerateHeat()) {
                    if (food && deviation < -.1F) {
                        float units = deviation > -.5F ? 2 : deviation > -1 ? 3 : 4;
                        correction += units / 1000;
                        exhaustion += .05F * units * part.area;
                    }
                    if (deviation > .1F) correction -= (deviation > .5F ? 2 : 1) / 1000.0F;
                }
                next[i] = t[i] + exchange + .001F + correction;
            }
            float core = next[0] * .1F + next[1] * .5F + next[3] * .4F;
            next[0] = next[1] = next[3] = core;
            transfer(BodyPart.LEGS, BodyPart.FEET);
            transfer(BodyPart.TORSO, BodyPart.HANDS);
            System.arraycopy(next, 0, t, 0, t.length);
        }
        void transfer(BodyPart first, BodyPart second) {
            float delta = Math.max(-3, Math.min(3, next[first.ordinal()] - next[second.ordinal()]));
            if (Math.abs(delta) > .1F) {
                next[first.ordinal()] -= delta * .1F;
                next[second.ordinal()] += delta * .1F;
            }
        }
    }

    @GameTest(template = "phase0a_empty", batch = "gameplay_balance", timeoutTicks = 5000)
    public static void measureRealRoomsAndCampfireBodyRecovery(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<Room> rooms = new ArrayList<>();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO).atY(160).east(256);
        rooms.add(new Room(helper, "wood_2x2x2", origin, 2, Blocks.OAK_PLANKS.defaultBlockState(), true));
        rooms.add(new Room(helper, "stone_2x2x2", origin.east(96), 2, Blocks.STONE.defaultBlockState(), true));
        rooms.add(new Room(helper, "outdoor", origin.east(192), 2, Blocks.OAK_PLANKS.defaultBlockState(), false));
        rooms.add(new Room(helper, "wood_6x6x2", origin.east(288), 6, Blocks.OAK_PLANKS.defaultBlockState(), true));
        boolean daylight = level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        long dayTime = level.getDayTime();
        Float previousDimension = WorldTemperature.worldCache.get(level);
        long start = level.getGameTime();
        List<String> csv = new ArrayList<>();
        csv.add("scene,second,phase,clothing,natural_air_c,sampled_air_c,shared_air,air_age_ticks,radiant_w_m2,core_c,torso_c,health,food,saturation,exhaustion,water,water_saturation,net_body_w,fuel_ticks");
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.setDayTime(6000);
        helper.startSequence().thenExecuteFor(4401, () -> {
            int elapsed = Math.toIntExact(level.getGameTime() - start);
            if (elapsed == 40) {
                BlockPos point = rooms.get(0).fire.above();
                float dimension = WorldTemperature.dimension(level);
                float air = WorldTemperature.naturalAir(level, point);
                WorldTemperature.worldCache.put(level, dimension - 40 - air);
                for (Room room : rooms) room.resetBodies();
            }
            if (elapsed == 100) for (Room room : rooms) room.ignite(helper);
            if (elapsed == 2500) for (Room room : rooms) room.extinguish(helper);
            for (Room room : rooms) room.holdPositions();
            if (elapsed >= 60 && elapsed % 20 == 0) {
                String phase = elapsed < 100 ? "before" : elapsed < 2500 ? "burning" : "extinguished";
                for (Room room : rooms) room.sample(csv, (elapsed - 100) / 20, phase);
            }
        }).thenExecute(() -> {
            try {
                write("room-curves.csv", String.join("\n", csv));
                write("room-conditions.json", json(Map.of("ambientTargetC", -40, "actualAmbientRecordedPerSample", true,
                        "physiologicalSecondsPerGameSecond", 8, "bodyInitialC", 35, "burningSeconds", 120, "coolingSeconds", 95,
                        "sampling", "Actual player body ticks, real fuel and world air/radiation; natural regeneration and resource systems remain active",
                        "scenes", rooms.stream().map(room -> room.name).toList())));
                helper.assertTrue(csv.size() > 1000, "all room and player histories were collected");
                for (Room room : rooms) {
                    helper.assertTrue(room.radiationObserved, room.name + " must include actual campfire radiation");
                    helper.assertTrue(room.bodyAdvanced, room.name + " must run real player physiology, not only world ticks");
                }
                FHMain.LOGGER.info("THERMAL_ROOM_BALANCE,rows={},output={}", csv.size() - 1, OUTPUT.toAbsolutePath());
            } finally {
                for (Room room : rooms) room.close();
                MinecraftThermalInput.closeActiveLevel(level);
                if (previousDimension == null) WorldTemperature.worldCache.remove(level);
                else WorldTemperature.worldCache.put(level, previousDimension);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(daylight, level.getServer());
                level.setDayTime(dayTime);
            }
        }).thenSucceed();
    }

    private static final class Room {
        final ServerLevel level;
        final String name;
        final BlockPos origin, fire;
        final List<Fixture> players = new ArrayList<>();
        final List<Long> chunks = new ArrayList<>();
        final ThermalEnvironmentSample sample = new ThermalEnvironmentSample();
        boolean radiationObserved, bodyAdvanced;

        Room(GameTestHelper helper, String name, BlockPos origin, int width, BlockState wall, boolean closed) {
            this.level = helper.getLevel(); this.name = name; this.origin = origin; fire = origin.offset(1, 1, 1);
            for (int cx = origin.getX() >> 4; cx <= (origin.getX() + width + 2) >> 4; cx++)
                for (int cz = origin.getZ() >> 4; cz <= (origin.getZ() + width + 2) >> 4; cz++) {
                    level.setChunkForced(cx, cz, true);
                    chunks.add(net.minecraft.world.level.ChunkPos.asLong(cx, cz));
                }
            for (int x = 0; x <= width + 1; x++) for (int z = 0; z <= width + 1; z++)
                for (int y = 0; y <= 3; y++) {
                    boolean shell = y == 0 || closed && (y == 3 || x == 0 || z == 0 || x == width + 1 || z == width + 1);
                    level.setBlockAndUpdate(origin.offset(x, y, z), shell ? wall : Blocks.AIR.defaultBlockState());
                }
            level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
            var fuel = level.getBlockEntity(fire).getCapability(ForgeCapabilities.ITEM_HANDLER)
                    .orElseThrow(IllegalStateException::new);
            ItemStack remaining = fuel.insertItem(0, new ItemStack(Items.STICK, 16), false);
            if (!remaining.isEmpty()) throw new IllegalStateException("room needs its recorded real stick fuel");
            for (int clothes = 0; clothes <= 1; clothes++) players.add(fixture(level, name, clothes));
            holdPositions();
        }

        void holdPositions() {
            for (Fixture fixture : players) if (fixture.player.isAlive()) {
                fixture.player.setPos(fire.getX() + 1.5, fire.getY(), fire.getZ() + .5);
                fixture.player.setDeltaMovement(Vec3.ZERO);
                // EmbeddedChannel is not in ServerConnectionListener. Drive the same full player tick
                // called by ServerGamePacketListenerImpl.tick, including Forge events, effects and food.
                fixture.player.doTick();
            }
        }
        void resetBodies() {
            for (Fixture fixture : players) {
                var data = PlayerTemperatureData.getCapability(fixture.player).orElseThrow(IllegalStateException::new);
                data.setAllPartsBodyTemp(-2);
                fixture.player.setHealth(fixture.player.getMaxHealth());
                fixture.player.getFoodData().setFoodLevel(20);
                fixture.player.getFoodData().setSaturation(5);
                fixture.player.getFoodData().setExhaustion(0);
                WaterLevelCapability.getCapability(fixture.player).ifPresent(water -> {
                    water.setWaterLevel(20); water.setWaterSaturationLevel(6); water.setWaterExhaustionLevel(0);
                });
            }
        }
        void ignite(GameTestHelper helper) {
            level.setBlockAndUpdate(fire, level.getBlockState(fire).setValue(CampfireBlock.LIT, true));
            helper.assertTrue(level.getBlockState(fire).getValue(CampfireBlock.LIT), "real fire becomes lit");
        }
        void extinguish(GameTestHelper helper) {
            level.setBlockAndUpdate(fire, level.getBlockState(fire).setValue(CampfireBlock.LIT, false));
            helper.assertTrue(!level.getBlockState(fire).getValue(CampfireBlock.LIT), "real fire extinguishes");
        }
        void sample(List<String> rows, int second, String phase) {
            int fuel = ((ICampfireExtra) level.getBlockEntity(fire)).getLifeTime();
            for (Fixture fixture : players) {
                ServerPlayer player = fixture.player;
                var data = PlayerTemperatureData.getCapability(player).orElseThrow(IllegalStateException::new);
                bodyAdvanced |= Math.abs(data.getNetBodyPowerW()) > 0.01F;
                double natural = WorldTemperature.naturalAir(level,
                        BlockPos.containing(player.getX(), player.getEyeY(), player.getZ()));
                double air = MinecraftThermalInput.gameplayPlayerEnvironment(player, natural, sample);
                radiationObserved |= sample.radiantFluxWPerM2() > 0;
                var water = WaterLevelCapability.getCapability(player).orElseThrow(IllegalStateException::new);
                long age = sample.airSampleTick() < 0 ? -1 : level.getGameTime() - sample.airSampleTick();
                rows.add(String.format(Locale.ROOT, "%s,%d,%s,%d,%.6f,%.6f,%s,%d,%.6f,%.6f,%.6f,%.3f,%d,%.3f,%.6f,%d,%d,%.6f,%d",
                        name, second, phase, fixture.clothing, natural, air, sample.sharedAirRegion(), age,
                        sample.radiantFluxWPerM2(), data.getAbsoluteCoreBodyTemp(), data.getAbsoluteBodyTempByPart(BodyPart.TORSO),
                        player.getHealth(), player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel(),
                        player.getFoodData().getExhaustionLevel(), water.getWaterLevel(), water.getWaterSaturationLevel(),
                        data.getNetBodyPowerW(), fuel));
            }
        }
        void close() {
            for (Fixture fixture : players) fixture.close();
            level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
            for (long chunk : chunks) level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(chunk),
                    net.minecraft.world.level.ChunkPos.getZ(chunk), false);
        }
    }

    static Fixture fixture(ServerLevel level, String name, int clothing) {
        ServerPlayer player = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "bal-" + name.substring(0, Math.min(8, name.length())) + clothing));
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        connection.setProtocol(ConnectionProtocol.PLAY);
        level.getServer().getPlayerList().placeNewPlayer(connection, player);
        player.setGameMode(GameType.SURVIVAL);
        PlayerTemperatureData data = PlayerTemperatureData.getCapability(player).orElseThrow(IllegalStateException::new);
        data.setDifficulty(FHTemperatureDifficulty.normal);
        clothing(player, data, clothing);
        return new Fixture(player, channel, clothing);
    }

    record Fixture(ServerPlayer player, EmbeddedChannel channel, int clothing) implements AutoCloseable {
        @Override public void close() { player.server.getPlayerList().remove(player); channel.finishAndReleaseAll(); }
    }

    private static void clothing(ServerPlayer player, PlayerTemperatureData data, int tier) {
        for (EquipmentSlot slot : EquipmentSlot.values()) player.setItemSlot(slot, ItemStack.EMPTY);
        data.clearAllClothes();
        if (tier == 0) return;
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(tier == 1 ? FHItems.wool_hat.get() : FHItems.polar_bear_hat.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(tier == 1 ? FHItems.wool_jacket.get() : FHItems.polar_bear_jacket.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(tier == 1 ? FHItems.wool_pants.get() : FHItems.polar_bear_pants.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(tier == 1 ? FHItems.wool_boots.get() : FHItems.polar_bear_boots.get()));
        data.setClothesByPart(BodyPart.HANDS, 0, new ItemStack(tier == 1 ? FHItems.wool_gloves.get() : FHItems.polar_bear_gloves.get()));
    }

    private static String json(Object value) { return new GsonBuilder().serializeNulls().create().toJson(value); }
    private static void write(String file, String text) {
        try { Files.createDirectories(OUTPUT); Files.writeString(OUTPUT.resolve(file), text, StandardCharsets.UTF_8); }
        catch (IOException exception) { throw new IllegalStateException("Cannot save balance measurement", exception); }
    }
}
