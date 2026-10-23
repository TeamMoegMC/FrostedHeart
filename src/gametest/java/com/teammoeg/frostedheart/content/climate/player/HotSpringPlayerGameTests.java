package com.teammoeg.frostedheart.content.climate.player;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHAttributes;
import com.teammoeg.frostedheart.bootstrap.common.FHFluids;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.content.climate.block.HotSpringGameTests;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_hot_spring")
@PrefixGameTestTemplate(false)
public class HotSpringPlayerGameTests {
    private static final double AIR_C = -20.0D;

    @GameTest(template = "phase0a_empty", batch = "hot_spring", timeoutTicks = 40)
    public static void actualSpringContactReachesComputationAndLeavesWetClothes(GameTestHelper helper) {
        TestPlayer fixture = makeSurvivalPlayer(helper);
        ServerPlayer player = fixture.player();
        try {
        BlockPos source = helper.absolutePos(new BlockPos(1, 3, 1));
        var level = helper.getLevel();
        level.setBlockAndUpdate(source.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(source, Blocks.WATER.defaultBlockState());
        player.setPos(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D);
        player.baseTick();
        PlayerTemperatureData ordinary = new PlayerTemperatureData();
        PlayerTemperatureComputation.updatePlayer(player, ordinary, 20);

        level.setBlockAndUpdate(source, FHFluids.HOT_SPRING.get().getSource()
                .defaultFluidState().createLegacyBlock());
        player.baseTick();
        HotSpringGameTests.contactDetails(player, source);
        helper.assertTrue(player.getFluidHeight(FluidTags.WATER) == 0.0D,
                "spring is tracked separately from vanilla cold water");
        helper.assertTrue(PlayerTemperatureUpdate.isSoaked(player), "spring counts as soaking");
        PlayerTemperatureData spring = new PlayerTemperatureData();
        PlayerTemperatureComputation.updatePlayer(player, spring, 20);
        helper.assertTrue(spring.getEnvironmentEquivalentTemperatureC()
                        > ordinary.getEnvironmentEquivalentTemperatureC(),
                "actual shallow spring contact reaches the 38 C computation path");

        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        int interval = FHConfig.SERVER.CLIMATE.temperatureUpdateIntervalTicks.get();
        long mixedId = player.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(player.getUUID().getLeastSignificantBits(), 17);
        player.tickCount = (int) Math.floorMod(mixedId, interval);
        PlayerTemperatureUpdate.regulateTemperature(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
        var wet = player.getEffect(FHMobEffects.WET.get());
        helper.assertTrue(wet != null, "real regulation event applies Wet in spring");
        helper.assertTrue(wet.getDuration() == FHConfig.SERVER.CLIMATE.wetEffectDuration.get()
                        * FHConfig.SERVER.CLIMATE.wetClothesDurationMultiplier.get(),
                "spring preserves the existing clothed Wet duration");
        player.setPos(source.getX() + 0.5D, source.getY() + 3.0D, source.getZ() + 0.5D);
        player.baseTick();
        helper.assertTrue(!PlayerTemperatureUpdate.isSoaked(player), "leaving spring clears fluid contact");
        helper.assertTrue(player.hasEffect(FHMobEffects.WET.get()), "Wet persists after leaving spring");
        helper.succeed();
        } finally {
            fixture.close();
        }
    }

    @GameTest(template = "phase0a_empty", batch = "hot_spring", timeoutTicks = 40)
    public static void mixedFluidsAndSpringSwimmingUseRealEntityContact(GameTestHelper helper) {
        TestPlayer fixture = makeSurvivalPlayer(helper);
        ServerPlayer player = fixture.player();
        try {
        BlockPos source = helper.absolutePos(new BlockPos(1, 3, 1));
        var level = helper.getLevel();
        var springBlock = FHFluids.HOT_SPRING.get().getSource().defaultFluidState().createLegacyBlock();
        level.setBlockAndUpdate(source.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(source.east().below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(source, springBlock);
        level.setBlockAndUpdate(source.east(), Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(source.east().above(), Blocks.WATER.defaultBlockState());
        player.setPos(source.getX() + 1.0D, source.getY(), source.getZ() + 0.5D);
        player.baseTick();
        double springHeight = player.getFluidTypeHeight(FHFluids.HOT_SPRING.get().getFluidType());
        HotSpringGameTests.contactDetails(player, source);
        double waterHeight = player.getFluidHeight(FluidTags.WATER);
        helper.assertTrue(springHeight > 0.0D && waterHeight > springHeight,
                "real entity overlaps shallow spring and deeper ordinary water");
        PlayerTemperatureData data = new PlayerTemperatureData();
        PlayerTemperatureComputation.updatePlayer(player, data, 20);
        double air = player.getAttributeValue(FHAttributes.ENV_TEMPERATURE.get());
        double expected = PlayerThermalModel.environmentalEquivalentTemperatureC(air,
                PlayerThermalModel.naturalConvectionCoefficientWPerM2K(air),
                data.getSampledRadiantFluxWPerM2(), data.getSampledLocalWindMPerS(),
                PlayerThermalModel.waterTemperatureC(air),
                Math.min(1.0D, waterHeight / player.getBbHeight()),
                Math.min(1.0D, springHeight / player.getBbHeight()), 0.0D, false, false);
        helper.assertTrue(Math.abs(data.getEnvironmentEquivalentTemperatureC() - expected) < 0.0001D,
                "mixed world contacts reach computation with their separate actual heights");

        level.setBlockAndUpdate(source.above(), springBlock);
        player.setPos(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D);
        player.setSecondsOnFire(10);
        player.setSprinting(true);
        player.baseTick();
        player.updateSwimming();
        helper.assertTrue(!player.isOnFire(), "spring extinguishes a burning player");
        helper.assertTrue(player.isSwimming(), "player can start swimming in deep spring");
        helper.succeed();
        } finally {
            fixture.close();
        }
    }

    @GameTest(template = "phase0a_empty", batch = "hot_spring", timeoutTicks = 40)
    public static void shallowSpringWarmsWithAndWithoutFoodOrClothing(GameTestHelper helper) {
        TestPlayer fixture = makeSurvivalPlayer(helper);
        ServerPlayer player = fixture.player();
        try {
        BlockPos source = helper.absolutePos(new BlockPos(1, 3, 1));
        helper.getLevel().setBlockAndUpdate(source.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(source, FHFluids.HOT_SPRING.get().getSource()
                .defaultFluidState().createLegacyBlock());
        player.setPos(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D);
        player.baseTick();
        double shallowRatio = player.getFluidTypeHeight(FHFluids.HOT_SPRING.get().getFluidType())
                / player.getBbHeight();
        HotSpringGameTests.contactDetails(player, source);
        helper.assertTrue(shallowRatio > 0.4D && shallowRatio < 0.6D,
                "recovery simulation uses the actual one-block source contact height");
        FHMain.LOGGER.info("Spring recovery motion before stationary fixture reset={}", player.getDeltaMovement());
        player.setDeltaMovement(Vec3.ZERO);
        player.setSprinting(false);
        player.setOnGround(true);
        helper.assertTrue(player.getVehicle() == null, "recovery fixture stands independently on the pool floor");
        FHMain.LOGGER.info("Spring recovery stationary basal power={} W; motion={}, sprinting={}, onGround={}",
                PlayerThermoregulation.sharedBodyPowerW(player, 1.0D, 0.0D, 0.0D),
                player.getDeltaMovement(), player.isSprinting(), player.onGround());

        ItemStack coat = insulated(Items.LEATHER_CHESTPLATE, EquipmentSlot.CHEST, 500.0D);
        ItemStack pants = insulated(Items.LEATHER_LEGGINGS, EquipmentSlot.LEGS, 100.0D);
        player.setItemSlot(EquipmentSlot.CHEST, coat);
        PartClothData clothing = new PartClothData();
        new PlayerTemperatureData().fillClothDataByPart(player, BodyPart.TORSO, clothing);
        helper.assertTrue(clothing.waterResistance > 0.0D && clothing.environmentExchangeFactor < 1.0D,
                "test coat actually contributes insulation and water resistance through equipment data");

        boolean fastRecovery = true;
        boolean clothedRecovery = true;
        for (double rate : new double[]{0.5D, 1.0D}) {
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
            Recovery fedDry = simulate(player, 0.0D, 0.0D, true, rate, 1.0D);
            Recovery fedWater = simulate(player, shallowRatio, 0.0D, true, rate, 1.0D);
            Recovery hungryDry = simulate(player, 0.0D, 0.0D, false, rate, 1.0D);
            FHMain.LOGGER.info("Spring recovery baseline rate={} air=-20C initial=35C: fedDry={}, fedWater={}, hungryDry={}",
                    rate, fedDry, fedWater, hungryDry);
            for (double gain : new double[]{1.0D, 8.0D, 16.0D, 32.0D, 64.0D, 128.0D}) {
                player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
                Recovery fedSpring = simulate(player, 0.0D, shallowRatio, true, rate, gain);
                Recovery hungrySpring = simulate(player, 0.0D, shallowRatio, false, rate, gain);
                player.setItemSlot(EquipmentSlot.CHEST, coat);
                Recovery coatedSpring = simulate(player, 0.0D, shallowRatio, false, rate, gain);
                player.setItemSlot(EquipmentSlot.LEGS, pants);
                Recovery clothedSpring = simulate(player, 0.0D, shallowRatio, false, rate, gain);
                FHMain.LOGGER.info("Spring recovery candidates rate={} gain={}: fed={}, hungry={}, coat={}, coatAndPants={}",
                        rate, gain, fedSpring, hungrySpring, coatedSpring, clothedSpring);
                helper.assertTrue(fedSpring.core120() > fedDry.core120()
                                && fedSpring.core120() > fedWater.core120(),
                        "spring improves fed recovery at both source and pack temperature rates");
                helper.assertTrue(hungrySpring.core120() > hungryDry.core120(),
                        "spring improves recovery without shivering fuel");
                if (gain == PlayerThermalModel.HOT_SPRING_EXCHANGE_GAIN) {
                    fastRecovery &= hungrySpring.core120() > 35.5D;
                    clothedRecovery &= clothedSpring.core120() > 35.2D;
                }
            }
        }
        helper.assertTrue(fastRecovery,
                "provisional spring gain restores at least 0.5 C within two minutes without food");
        helper.assertTrue(clothedRecovery,
                "directly immersed insulating pants retain meaningful shallow recovery");
        assertPassiveContactBound(helper, player);
        helper.succeed();
        } finally {
            fixture.close();
        }
    }

    private record TestPlayer(ServerPlayer player, EmbeddedChannel channel) implements AutoCloseable {
        @Override
        public void close() {
            var players = player.server.getPlayerList();
            if (players.getPlayers().contains(player)) players.remove(player);
            channel.finishAndReleaseAll();
        }
    }

    private static TestPlayer makeSurvivalPlayer(GameTestHelper helper) {
        // Vanilla GameTestHelper's mock overrides isCreative() to true even after setGameMode.
        var level = helper.getLevel();
        ServerPlayer player = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "spring-test-player"));
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        connection.setProtocol(ConnectionProtocol.PLAY);
        TestPlayer fixture = new TestPlayer(player, channel);
        try {
            level.getServer().getPlayerList().placeNewPlayer(connection, player);
            player.setGameMode(GameType.SURVIVAL);
            helper.assertTrue(!player.isCreative() && !player.getAbilities().invulnerable,
                    "fixture exercises survival player physiology and Wet cadence");
            return fixture;
        } catch (RuntimeException | Error failure) {
            fixture.close();
            throw failure;
        }
    }

    private static ItemStack insulated(net.minecraft.world.item.Item item, EquipmentSlot slot, double insulation) {
        ItemStack stack = new ItemStack(item);
        stack.addAttributeModifier(FHAttributes.INSULATION.get(), new AttributeModifier(
                UUID.randomUUID(), "spring test insulation", insulation, AttributeModifier.Operation.ADDITION), slot);
        stack.addAttributeModifier(FHAttributes.WIND_PROOF.get(), new AttributeModifier(
                UUID.randomUUID(), "spring test water resistance", 0.5D,
                AttributeModifier.Operation.MULTIPLY_TOTAL), slot);
        return stack;
    }

    private static void assertPassiveContactBound(GameTestHelper helper, ServerPlayer player) {
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        PlayerTemperatureData data = new PlayerTemperatureData();
        data.setAllPartsBodyTemp(-2.0F);
        HeatingDeviceContext context = new HeatingDeviceContext();
        context.reset(player, 120.0D, 960.0D, data);
        for (BodyPart part : BodyPart.VALUES) {
            PlayerThermalModel.preparePart(data, context, part, AIR_C,
                    PlayerThermalModel.naturalConvectionCoefficientWPerM2K(AIR_C), 0.0D, 0.0D,
                    0.0D, 0.0D, 1.0D, 0.0D, false, false);
        }
        PlayerThermalModel.integrateBody(data, context, AIR_C, false, 0.0D, 0.0D, 0.0D, 960.0D, false);
        for (BodyPart part : BodyPart.VALUES) {
            helper.assertTrue(data.getAbsoluteBodyTempByPart(part) >= 35.0D
                            && data.getAbsoluteBodyTempByPart(part) <= 38.0D,
                    "accelerated passive contact remains bounded by the actual 38 C water temperature");
        }
    }

    private record Recovery(double core30, double core60, double core120) { }

    private static Recovery simulate(ServerPlayer player, double waterRatio, double springRatio, boolean fed,
                                     double rate, double springGain) {
        player.setDeltaMovement(Vec3.ZERO);
        player.setSprinting(false);
        player.setOnGround(true);
        PlayerTemperatureData data = new PlayerTemperatureData();
        HeatingDeviceContext context = new HeatingDeviceContext();
        for (BodyPart part : BodyPart.VALUES) data.setBodyTempByPart(part, -2.0F);
        data.refreshCoreTemperature();
        double hNatural = PlayerThermalModel.naturalConvectionCoefficientWPerM2K(AIR_C);
        double physiologicalSeconds = PlayerThermalModel.physiologicalSeconds(1.0D, rate);
        double core30 = 0.0D;
        double core60 = 0.0D;
        for (int second = 0; second < 120; second++) {
            context.reset(player, 1.0D, physiologicalSeconds, data);
            double wetCoolingW = 0.0D;
            double exposedAreaM2 = 0.0D;
            for (BodyPart part : BodyPart.VALUES) {
                PlayerThermalModel.preparePart(data, context, part,
                        AIR_C, hNatural, 0.0D, 0.0D,
                        PlayerThermalModel.waterTemperatureC(AIR_C), waterRatio, springRatio,
                        0.0D, false, waterRatio > 0.0D || springRatio > 0.0D, springGain);
                wetCoolingW += Math.max(0.0D, context.getWetConductanceWPerK(part)
                        * (context.getBodyTemperatureC(part) - context.getOperativeTemperatureC(part)));
                exposedAreaM2 += PlayerThermalModel.exposedAreaM2(part, context.getAirFraction(part));
            }
            double shiver = PlayerThermoregulation.shiveringPowerW(
                    data.getAbsoluteCoreBodyTemp(), 1.0D, fed);
            double sharedPower = PlayerThermoregulation.sharedBodyPowerW(player, 1.0D, shiver, 0.0D);
            double evaporationScale = PlayerThermalModel.evaporationScale(
                    AIR_C, 0.5D, 0.0D, exposedAreaM2, wetCoolingW);
            PlayerThermalModel.integrateBody(data, context, AIR_C,
                    false, 0.0D, sharedPower, evaporationScale, physiologicalSeconds, false);
            data.refreshCoreTemperature();
            if (second == 29) core30 = data.getAbsoluteCoreBodyTemp();
            if (second == 59) core60 = data.getAbsoluteCoreBodyTemp();
        }
        return new Recovery(core30, core60, data.getAbsoluteCoreBodyTemp());
    }
}
