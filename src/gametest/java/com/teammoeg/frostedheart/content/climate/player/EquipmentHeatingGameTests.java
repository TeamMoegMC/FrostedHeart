package com.teammoeg.frostedheart.content.climate.player;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.compat.curios.CuriosCompat;
import com.teammoeg.frostedheart.content.climate.FHTemperatureDifficulty;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.content.climate.player.thermalitem.WearableThermalExchangeHandler;
import com.teammoeg.frostedheart.content.climate.player.thermalitem.WearableThermalReservoir;
import com.teammoeg.frostedheart.content.climate.player.thermalitem.WearableThermalState;
import com.teammoeg.frostedheart.content.climate.player.thermalitem.ThreeNodeWearableHeatExchange;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalFieldKey;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftGameplayFields;
import com.teammoeg.frostedheart.content.utility.HeatingPadItem;
import com.teammoeg.frostedheart.content.utility.OxygenCandleItem;
import com.teammoeg.frostedheart.content.utility.handstoves.CoalHandStove;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

/** Real item capabilities and production integration; fixed-input curves keep world sampling separate. */
@GameTestHolder("frostedheart_equipment_heating")
@PrefixGameTestTemplate(false)
public final class EquipmentHeatingGameTests {
    private static final String TEMPLATE = "phase0a_empty";
    private static final String[] DEVICES = {"none", "main_stove", "off_stove", "pad", "steam", "candle",
            "mushroom", "vest", "stacked", "warm_stone", "hot_water_bag", "stacked_reservoir"};

    @GameTest(template = TEMPLATE, batch = "equipment_curves", timeoutTicks = 400)
    public static void sustainedEquipmentAndThickClothingDoNotOverheat(GameTestHelper helper) {
        int[] cursor = {0};
        helper.startSequence().thenExecuteFor(72, () -> {
            int index = cursor[0]++;
            if (index >= 72) return;
            String device = DEVICES[index % DEVICES.length];
            double air = new double[]{-50, 0, 20}[index / DEVICES.length % 3];
            boolean clothed = index >= 36;
            var trace = new Trace(helper, device, clothed, 37.0D);
            double maximumPartC = 37.0D;
            for (int second = 0; second < 1200; second++) {
                // Continuous availability intentionally stresses repeated refuelling/recharging.
                trace.step(air, 0, 1, true);
                for (BodyPart part : BodyPart.VALUES) {
                    maximumPartC = Math.max(maximumPartC, trace.data.getAbsoluteBodyTempByPart(part));
                }
            }
            String label = device + ",air=" + air + ",clothed=" + clothed;
            helper.assertTrue(maximumPartC < 38.0D,
                    "twenty minutes of supplied equipment cannot overheat an ordinary environment: "
                            + label + ", maximum=" + maximumPartC);
            FHMain.LOGGER.info("EQUIPMENT_BALANCE,{},seconds=1200,maxPartC={},coreC={},torsoC={}", label,
                    maximumPartC, trace.data.getAbsoluteCoreBodyTemp(),
                    trace.data.getAbsoluteBodyTempByPart(BodyPart.TORSO));
        }).thenExecute(() -> helper.assertTrue(cursor[0] >= 72, "all equipment/clothing curves completed"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "equipment_logic", timeoutTicks = 80)
    public static void everyDeviceStillHelpsColdBodyAndCorrectHand(GameTestHelper helper) {
        for (String device : DEVICES) {
            if (device.equals("none")) continue;
            double initial = device.equals("mushroom") ? 36.5D : 35.0D;
            var baseline = new Trace(helper, "none", false, initial);
            var heated = new Trace(helper, device, false, initial);
            baseline.step(-50, 0, 1, false);
            heated.step(-50, 0, 1, true);
            helper.assertTrue(bodyEnergy(heated.data) > bodyEnergy(baseline.data) + 1.0D,
                    device + " retains useful warming in cold conditions");
            if (device.equals("main_stove")) {
                helper.assertTrue(heated.context.getPowerW(BodyPart.HANDS) > 0
                                && heated.context.getPowerW(BodyPart.TORSO) == 0,
                        "main-hand stove targets hands");
            } else if (device.equals("off_stove")) {
                helper.assertTrue(heated.context.getPowerW(BodyPart.TORSO) > 0
                                && heated.context.getPowerW(BodyPart.HANDS) == 0,
                        "off-hand stove targets torso");
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "equipment_logic", timeoutTicks = 80)
    public static void stackedHeatingRespectsLongAndShortUpdateIntervals(GameTestHelper helper) {
        for (double interval : new double[]{0.25D, 1.0D, 5.0D, 60.0D}) {
            var trace = new Trace(helper, "stacked_reservoir", true, 36.99D);
            for (double second = 0; second < 600; second += interval) {
                trace.step(0, 0, interval, true);
                for (BodyPart part : BodyPart.VALUES) helper.assertTrue(
                        trace.data.getAbsoluteBodyTempByPart(part) < 38.0D,
                        "stacked devices cannot jump into overheating at interval " + interval);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "equipment_logic", timeoutTicks = 80)
    public static void vestOnlyConsumesForUncoveredDemandIncludingWetAndFire(GameTestHelper helper) {
        var normal = new Trace(helper, "vest", false, 37);
        supply(normal.player.getItemBySlot(EquipmentSlot.CHEST));
        float before = stored(normal.player.getItemBySlot(EquipmentSlot.CHEST));
        normal.step(20, 0, 1, false);
        near(helper, before, stored(normal.player.getItemBySlot(EquipmentSlot.CHEST)), 0,
                "normal body metabolism already covers mild-air loss; vest spends nothing");

        var stacked = new Trace(helper, "stacked", false, 36.95);
        supply(stacked.player.getItemBySlot(EquipmentSlot.CHEST));
        before = stored(stacked.player.getItemBySlot(EquipmentSlot.CHEST));
        stacked.step(-50, 0, 1, true);
        near(helper, before, stored(stacked.player.getItemBySlot(EquipmentSlot.CHEST)), 0,
                "fixed devices cover demand before the vest, irrespective of slot traversal");

        var dry = new Trace(helper, "vest", false, 35);
        var wet = new Trace(helper, "vest", false, 35);
        prepare(dry, 0, 0, 1, false);
        prepare(wet, 0, 0, 1, true);
        dry.context.setBodyRegulation(32, 1, false, 0);
        wet.context.setBodyRegulation(32, 1, false, 0);
        helper.assertTrue(wet.context.remainingHeatingDemandW(BodyPart.TORSO, 55)
                        > dry.context.remainingHeatingDemandW(BodyPart.TORSO, 55),
                "vest accounts for applied wet heat loss");
        wet.context.setBodyRegulation(32, 1, true, 0);
        near(helper, 0, wet.context.remainingHeatingDemandW(BodyPart.TORSO, 55), 0,
                "being on fire already supplies more than the requested warming");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "equipment_logic", timeoutTicks = 80)
    public static void equipmentCannotCancelRealEnvironmentalHeatOrResetHotBody(GameTestHelper helper) {
        for (String device : new String[]{"none", "stacked", "stacked_reservoir"}) {
            var trace = new Trace(helper, device, false, 37);
            for (int second = 0; second < 180; second++) trace.step(100, 300, 1, true);
            helper.assertTrue(trace.data.getAbsoluteBodyTempByPart(BodyPart.TORSO) > 38,
                    device + " still overheats in genuinely hot air and radiation");
        }
        var control = new Trace(helper, "none", false, 42);
        var heated = new Trace(helper, "stacked", false, 42);
        control.step(0, 0, 1, false);
        heated.step(0, 0, 1, true);
        for (BodyPart part : BodyPart.VALUES) near(helper,
                control.data.getAbsoluteBodyTempByPart(part), heated.data.getAbsoluteBodyTempByPart(part), 1.0e-5D,
                "hot body cools naturally without being clamped or held hot by equipment");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "equipment_logic", timeoutTicks = 80)
    public static void fixedDeviceFuelAndStoredHeatFollowElapsedTimeAndReload(GameTestHelper helper) {
        for (String device : new String[]{"off_stove", "pad", "steam", "candle", "mushroom"}) {
            double referenceJ = -1;
            for (double interval : new double[]{0.25D, 1.0D, 2.0D}) {
                var trace = new Trace(helper, device, false, 36.5);
                ItemStack stack = trace.player.getOffhandItem();
                limitToTwoSeconds(stack, device);
                double delivered = 0;
                for (int step = 0; step < Math.round(3 / interval); step++) {
                    trace.context.reset(trace.player, interval, interval * 8, trace.data);
                    var capability = FHCapabilities.EQUIPMENT_HEATING.getCapability(stack).orElseThrow(IllegalStateException::new);
                    capability.tickHeating(HeatingDeviceSlot.vanilla(EquipmentSlot.OFFHAND), stack, trace.context);
                    delivered += trace.context.getPowerW(BodyPart.TORSO) * interval;
                    if (step == 0) stack = ItemStack.of(stack.save(new net.minecraft.nbt.CompoundTag()));
                }
                if (referenceJ < 0) referenceJ = delivered;
                near(helper, referenceJ, delivered, 1.0e-3D,
                        device + " fuel/heat expiry is independent of cadence and survives partial-second reload");
                helper.assertTrue(delivered > 0, device + " actually supplied heat before expiry");
                near(helper, 0, FHCapabilities.EQUIPMENT_HEATING.getCapability(stack).orElseThrow(IllegalStateException::new)
                        .getMaxPowerW(stack), 1.0e-5D, device + " is exhausted once");
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "equipment_reservoir", timeoutTicks = 100)
    public static void reservoirsRetainSurplusAndConserveEnergyAcrossUnequalBodyParts(GameTestHelper helper) {
        for (Item item : new Item[]{FHBlocks.WARM_STONE.asItem(), FHBlocks.HOT_WATER_BAG.asItem()}) {
            for (double interval : new double[]{1, 20}) {
                var data = new PlayerTemperatureData();
                data.setAllPartsBodyTemp(-2);
                data.setBodyTempByPart(BodyPart.TORSO, 2);
                data.setBodyTempByPart(BodyPart.HANDS, -5);
                data.refreshCoreTemperature();
                ItemStack stack = new ItemStack(item);
                var reservoir = (WearableThermalReservoir) item;
                reservoir.setTemperaturesC(stack, 100, 60);
                // Preserve the existing float-valued version-one item and pack recipe format.
                var oldState = stack.getTag().getCompound(WearableThermalState.ROOT_KEY);
                oldState.putFloat(WearableThermalState.CORE_TEMPERATURE_KEY, 100);
                oldState.putFloat(WearableThermalState.SURFACE_TEMPERATURE_KEY, 60);
                var exchange = new WearableThermalExchangeHandler();
                var raw = new ThreeNodeWearableHeatExchange.MutableResult();
                var scratch = new ThreeNodeWearableHeatExchange.Scratch();
                var profile = reservoir.thermalProfile(stack);
                double roundingAllowanceJ = 0;
                double before = totalEnergy(data, stack);
                for (double seconds = 0; seconds < 1200; seconds += interval) {
                    var state = WearableThermalState.read(stack).orElseThrow();
                    double mean = data.getMeanBodyTemperatureC();
                    double capacity = PlayerThermalModel.WHOLE_BODY_HEAT_CAPACITY_J_PER_K;
                    ThreeNodeWearableHeatExchange.exchangeInto(profile, state.coreTemperatureC(),
                            state.surfaceTemperatureC(), mean, interval,
                            mean + data.getEquipmentWarmingEnergyJ() / capacity, raw, scratch);
                    helper.assertTrue(raw.applied(), "the bounded reservoir kernel advances");
                    double rawEnergyChangeJ = capacity * (
                            profile.coreCapacityRatio() * (raw.reservoirCoreTemperatureC() - state.coreTemperatureC())
                                    + profile.surfaceCapacityRatio() * (raw.reservoirSurfaceTemperatureC() - state.surfaceTemperatureC())
                                    + raw.playerTemperatureC() - mean);
                    near(helper, 0, rawEnergyChangeJ, 1.0e-5D,
                            "double-precision transfer conserves energy before storage rounding");
                    double stepBefore = totalEnergy(data, stack);
                    exchange.exchangeInto(data, stack, 20, interval);
                    double roundingBound = reservoirRoundingBoundJ(stack) + 1.0e-5D;
                    roundingAllowanceJ += roundingBound;
                    near(helper, stepBefore, totalEnergy(data, stack), roundingBound,
                            "each stored exchange stays within the existing float-NBT rounding bound");
                    near(helper, 39, data.getAbsoluteBodyTempByPart(BodyPart.TORSO), 1.0e-5D,
                            "a hot torso receives no warming while cold extremities recover");
                    for (BodyPart part : BodyPart.VALUES) if (part != BodyPart.TORSO) {
                        helper.assertTrue(data.getAbsoluteBodyTempByPart(part) <= 37.00001D,
                                "reservoir warmth stops at each part's normal temperature");
                    }
                }
                helper.assertTrue(data.getAbsoluteBodyTempByPart(BodyPart.HANDS) > 32.5D,
                        "cold hands actually benefit even with an already hot torso");
                near(helper, before, totalEnergy(data, stack), roundingAllowanceJ,
                        "long-run stored energy differs only within accumulated float quantization bounds");
                var reloaded = ItemStack.of(stack.save(new net.minecraft.nbt.CompoundTag()));
                helper.assertTrue(WearableThermalState.read(reloaded).equals(WearableThermalState.read(stack)),
                        "the existing reservoir precision survives ordinary item save/load");
                helper.assertTrue(stack.getTag().getCompound(WearableThermalState.ROOT_KEY)
                                .contains(WearableThermalState.CORE_TEMPERATURE_KEY, net.minecraft.nbt.Tag.TAG_FLOAT)
                                && stack.getTag().getCompound(WearableThermalState.ROOT_KEY)
                                .contains(WearableThermalState.SURFACE_TEMPERATURE_KEY, net.minecraft.nbt.Tag.TAG_FLOAT),
                        "equipment fixes retain the original float storage format");
                data.setAllPartsBodyTemp(0);
                data.refreshCoreTemperature();
                reservoir.setTemperaturesC(stack, 60, 60);
                for (int second = 0; second < 60; second++) exchange.exchangeInto(data, stack, 20, 1);
                near(helper, 60, WearableThermalState.read(stack).orElseThrow().coreTemperatureC(), 1.0e-8D,
                        "unused reservoir warmth remains stored at normal body temperature");
                reservoir.setTemperaturesC(stack, 0, 0);
                before = totalEnergy(data, stack);
                exchange.exchangeInto(data, stack, 20, 10);
                helper.assertTrue(data.getAbsoluteCoreBodyTemp() < 37,
                        "a cold finite reservoir still absorbs body heat");
                near(helper, before, totalEnergy(data, stack), reservoirRoundingBoundJ(stack) + 1.0e-5D,
                        "cooling also conserves energy within the original storage rounding");
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "equipment_player", timeoutTicks = 120)
    public static void actualPlayerTickUsesEquipmentAndHiddenReservoirSlot(GameTestHelper helper) {
        var player = newPlayer(helper);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        connection.setProtocol(ConnectionProtocol.PLAY);
        var field = ThermalFieldKey.of(new ResourceLocation(FHMain.MODID, "equipment_test"), player.getUUID(), 0);
        try {
            player.server.getPlayerList().placeNewPlayer(connection, player);
            for (EquipmentSlot slot : EquipmentSlot.values()) player.setItemSlot(slot, ItemStack.EMPTY);
            player.setGameMode(GameType.SURVIVAL);
            BlockPos pos = helper.absolutePos(new BlockPos(3, 3, 3));
            player.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            player.serverLevel().setBlockAndUpdate(pos.above(3), Blocks.STONE.defaultBlockState());
            MinecraftGameplayFields.upsert(player.serverLevel(), new ThermalAnalyticField(field, 100,
                    ThermalAnalyticField.CombineMode.OVERRIDE, player.getX(), player.getEyeY(), player.getZ(), 8, 20));
            var data = PlayerTemperatureData.getCapability(player).orElseThrow(IllegalStateException::new);
            data.setDifficulty(FHTemperatureDifficulty.normal);
            data.setAllPartsBodyTemp(-1);
            data.refreshCoreTemperature();
            player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(FHItems.hand_stove.get()));
            var slot = CuriosApi.getCuriosInventory(player).resolve().orElseThrow()
                    .getStacksHandler(CuriosCompat.WARM_STONE_SLOT).orElseThrow();
            ItemStack bag = new ItemStack(FHBlocks.HOT_WATER_BAG.asItem());
            ((WearableThermalReservoir) bag.getItem()).setTemperaturesC(bag, 60, 60);
            slot.getStacks().setStackInSlot(0, bag);
            slot.getRenders().set(0, false);
            var back = CuriosApi.getCuriosInventory(player).resolve().orElseThrow()
                    .getStacksHandler("back").orElseThrow();
            ItemStack vest = new ItemStack(FHItems.heater_vest.get());
            supply(vest);
            back.getStacks().setStackInSlot(0, vest);
            float vestBefore = stored(vest);
            for (int second = 0; second < 600; second++) {
                supply(player.getOffhandItem());
                alignTick(player);
                PlayerTemperatureUpdate.updateTemperature(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player));
                helper.assertTrue(data.getAbsoluteBodyTempByPart(BodyPart.TORSO) < 38,
                        "real player update composes stove and the hidden reservoir without overheating");
            }
            helper.assertTrue(data.getAbsoluteCoreBodyTemp() > 36.5,
                    "real player update actually warms the cold body");
            helper.assertTrue(WearableThermalState.read(bag).orElseThrow().coreTemperatureC() < 60,
                    "turning off rendering does not disable reservoir heat transfer");
            near(helper, vestBefore, stored(vest), 0,
                    "a Curios vest visited before vanilla hands must not spend steam already supplied by the stove");
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            slot.getStacks().setStackInSlot(0, ItemStack.EMPTY);
            data.setAllPartsBodyTemp(-2);
            data.refreshCoreTemperature();
            alignTick(player);
            PlayerTemperatureUpdate.updateTemperature(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player));
            helper.assertTrue(stored(vest) < vestBefore && data.thermalContext().getPowerW(BodyPart.TORSO) > 0,
                    "the same Curios vest supplies heat once the other warming devices are removed");
            player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(FHItems.hand_stove.get()));
            supply(player.getOffhandItem());
            slot.getStacks().setStackInSlot(0, bag);
            for (GameType mode : new GameType[]{GameType.CREATIVE, GameType.SPECTATOR}) {
                player.setGameMode(mode);
                int fuel = CoalHandStove.getFuelAmount(player.getOffhandItem());
                var state = WearableThermalState.read(bag).orElseThrow();
                alignTick(player);
                PlayerTemperatureUpdate.updateTemperature(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player));
                helper.assertTrue(CoalHandStove.getFuelAmount(player.getOffhandItem()) == fuel
                                && WearableThermalState.read(bag).orElseThrow().equals(state),
                        "management modes do not consume heating resources or reservoir energy");
            }
            helper.succeed();
        } finally {
            MinecraftGameplayFields.remove(player.serverLevel(), field);
            player.server.getPlayerList().remove(player);
            channel.finishAndReleaseAll();
        }
    }

    private static final class Trace {
        final ServerPlayer player;
        final PlayerTemperatureData data = new PlayerTemperatureData();
        final HeatingDeviceContext context = new HeatingDeviceContext();
        final WearableThermalExchangeHandler exchange = new WearableThermalExchangeHandler();
        ItemStack reservoir = ItemStack.EMPTY;

        Trace(GameTestHelper helper, String device, boolean clothed, double initial) {
            player = newPlayer(helper);
            data.setDifficulty(FHTemperatureDifficulty.normal);
            data.setAllPartsBodyTemp((float) initial - 37);
            data.refreshCoreTemperature();
            if (clothed) {
                player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(FHItems.polar_bear_hat.get()));
                player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(FHItems.polar_bear_jacket.get()));
                player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(FHItems.polar_bear_pants.get()));
                player.setItemSlot(EquipmentSlot.FEET, new ItemStack(FHItems.polar_bear_boots.get()));
                data.setClothesByPart(BodyPart.HANDS, 0, new ItemStack(FHItems.polar_bear_gloves.get()));
            }
            switch (device) {
                case "main_stove" -> player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(FHItems.hand_stove.get()));
                case "off_stove" -> player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(FHItems.hand_stove.get()));
                case "pad" -> player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(FHItems.heating_pad.get()));
                case "steam" -> player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(FHItems.steam_bottle.get()));
                case "candle" -> player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(FHItems.oxygen_candle.get()));
                case "mushroom" -> player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(FHItems.red_mushroombed.get()));
                case "vest", "stacked", "stacked_reservoir" -> {
                    player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(FHItems.heater_vest.get()));
                    if (clothed) data.setClothesByPart(BodyPart.TORSO, 0, new ItemStack(FHItems.polar_bear_jacket.get()));
                    if (!device.equals("vest")) {
                        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(FHItems.heating_pad.get()));
                        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(FHItems.hand_stove.get()));
                    }
                }
            }
            if (device.equals("warm_stone") || device.equals("hot_water_bag") || device.equals("stacked_reservoir")) {
                reservoir = new ItemStack(device.equals("warm_stone")
                        ? FHBlocks.WARM_STONE.asItem() : FHBlocks.HOT_WATER_BAG.asItem());
                ((WearableThermalReservoir) reservoir.getItem()).setTemperaturesC(reservoir, 60, 60);
            }
            for (EquipmentSlot slot : EquipmentSlot.values()) supply(player.getItemBySlot(slot));
        }

        void step(double air, double radiation, double seconds, boolean replenish) {
            if (replenish) for (EquipmentSlot slot : EquipmentSlot.values()) supply(player.getItemBySlot(slot));
            prepare(this, air, radiation, seconds, false);
            double shiver = PlayerThermoregulation.shiveringPowerW(data.getAbsoluteCoreBodyTemp(), 1, true);
            double sweat = PlayerThermoregulation.sweatingPowerW(data.getAbsoluteCoreBodyTemp(), 1, true);
            double evaporation = PlayerThermalModel.evaporationScale(air, 0.5, 0, 1.8, sweat);
            double shared = PlayerThermoregulation.sharedBodyPowerW(player, 1, shiver, sweat * evaporation);
            context.setBodyRegulation(shared, evaporation, false, 0);
            PlayerEquipmentHeating.collect(player, context);
            PlayerThermalModel.integrateBody(data, context, air, false, 0, shared, evaporation,
                    PlayerThermalModel.physiologicalSeconds(seconds), false);
            data.refreshCoreTemperature();
            if (!reservoir.isEmpty()) exchange.exchangeInto(data, reservoir, air, seconds);
        }
    }

    private static void prepare(Trace trace, double air, double radiation, double seconds, boolean wet) {
        trace.context.reset(trace.player, seconds, PlayerThermalModel.physiologicalSeconds(seconds), trace.data);
        for (BodyPart part : BodyPart.VALUES) PlayerThermalModel.preparePart(trace.data, trace.context, part,
                air, PlayerThermalModel.naturalConvectionCoefficientWPerM2K(air), radiation, 0,
                PlayerThermalModel.waterTemperatureC(air), 0, 0, 0, false, wet);
    }

    private static ServerPlayer newPlayer(GameTestHelper helper) {
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "equipment-test"));
        player.setGameMode(GameType.SURVIVAL);
        player.setDeltaMovement(0, 0, 0);
        player.setSprinting(false);
        return player;
    }

    private static void supply(ItemStack stack) {
        if (stack.isEmpty()) return;
        if (stack.getItem() instanceof CoalHandStove) {
            CoalHandStove.setFuelAmount(stack, CoalHandStove.max_fuel);
            CoalHandStove.setAshAmount(stack, 0);
        } else if (stack.getItem() instanceof HeatingPadItem || stack.getItem() instanceof OxygenCandleItem) {
            stack.getOrCreateTag().putInt("fuel", 0);
        } else if (stack.getItem() instanceof com.teammoeg.frostedheart.content.utility.MushroomBed) {
            stack.setDamageValue(stack.getMaxDamage());
        }
        FHCapabilities.ITEM_HEAT.getCapability(stack).ifPresent(value -> value.receiveEnergy(30_000, false));
    }

    private static void limitToTwoSeconds(ItemStack stack, String device) {
        switch (device) {
            case "off_stove" -> {
                CoalHandStove.setFuelAmount(stack, 2);
                CoalHandStove.setAshAmount(stack, CoalHandStove.max_fuel - 2);
            }
            case "pad" -> stack.getOrCreateTag().putInt("fuel", HeatingPadItem.BURN_TIME - 2);
            case "candle" -> stack.getOrCreateTag().putInt("fuel", OxygenCandleItem.BURN_TIME - 2);
            case "mushroom" -> stack.setDamageValue(2);
            case "steam" -> FHCapabilities.ITEM_HEAT.getCapability(stack).ifPresent(value -> {
                value.extractEnergy(value.getEnergyStored(), false);
                value.receiveEnergy(3, false);
            });
        }
    }

    private static float stored(ItemStack stack) {
        return FHCapabilities.ITEM_HEAT.getCapability(stack).orElseThrow(IllegalStateException::new).getEnergyStored();
    }

    private static double bodyEnergy(PlayerTemperatureData data) {
        double sum = 0;
        for (BodyPart part : BodyPart.VALUES) sum += data.getBodyEnergyJ(part);
        return sum;
    }

    private static double totalEnergy(PlayerTemperatureData data, ItemStack stack) {
        var state = WearableThermalState.read(stack).orElseThrow();
        var profile = ((WearableThermalReservoir) stack.getItem()).thermalProfile(stack);
        return bodyEnergy(data) + PlayerThermalModel.WHOLE_BODY_HEAT_CAPACITY_J_PER_K
                * (profile.coreCapacityRatio() * state.coreTemperatureC()
                        + profile.surfaceCapacityRatio() * state.surfaceTemperatureC());
    }

    private static double reservoirRoundingBoundJ(ItemStack stack) {
        var state = WearableThermalState.read(stack).orElseThrow();
        var profile = ((WearableThermalReservoir) stack.getItem()).thermalProfile(stack);
        return PlayerThermalModel.WHOLE_BODY_HEAT_CAPACITY_J_PER_K * 0.5D
                * (profile.coreCapacityRatio() * Math.ulp((float) state.coreTemperatureC())
                        + profile.surfaceCapacityRatio() * Math.ulp((float) state.surfaceTemperatureC()));
    }

    private static void alignTick(ServerPlayer player) {
        int interval = FHConfig.SERVER.CLIMATE.temperatureUpdateIntervalTicks.get();
        long mixed = player.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(player.getUUID().getLeastSignificantBits(), 17);
        player.tickCount = (int) Math.floorMod(mixed, interval);
    }

    private static void near(GameTestHelper helper, double expected, double actual, double tolerance, String message) {
        helper.assertTrue(Double.isFinite(actual) && Math.abs(expected - actual) <= tolerance,
                message + ": expected=" + expected + ", actual=" + actual);
    }
}
