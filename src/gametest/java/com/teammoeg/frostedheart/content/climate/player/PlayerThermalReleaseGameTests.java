package com.teammoeg.frostedheart.content.climate.player;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.teammoeg.chorda.io.SerializeUtil;
import com.teammoeg.frostedheart.bootstrap.common.FHAttributes;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.content.climate.FHTemperatureDifficulty;
import com.teammoeg.frostedheart.content.climate.ClientClimateData;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.network.FHBodyDataSyncPacket;
import com.teammoeg.frostedheart.content.climate.network.FHClimatePacket;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalFieldKey;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftGameplayFields;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.content.utility.HeatingPadItem;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Production body, clothing, damage and item capabilities on an actual Forge server. */
@GameTestHolder("frostedheart_player_release")
@PrefixGameTestTemplate(false)
public final class PlayerThermalReleaseGameTests {
    private static final String TEMPLATE = "phase0a_empty";

    @GameTest(template = TEMPLATE, batch = "player_release", timeoutTicks = 40)
    public static void clothingAttributeModifiersFollowVanillaOperationMeaning(GameTestHelper helper) {
        try (Fixture fixture = player(helper)) {
            var attribute = fixture.player.getAttribute(FHAttributes.INSULATION.get());
            attribute.setBaseValue(0.0D);
            attribute.addTransientModifier(new AttributeModifier(UUID.randomUUID(), "base insulation", 100.0D,
                    AttributeModifier.Operation.ADDITION));
            attribute.addTransientModifier(new AttributeModifier(UUID.randomUUID(), "half more insulation", 0.5D,
                    AttributeModifier.Operation.MULTIPLY_BASE));
            attribute.addTransientModifier(new AttributeModifier(UUID.randomUUID(), "total bonus", 0.2D,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
            near(helper, 180.0D, attribute.getValue(), 1.0e-8D,
                    "vanilla attribute provides the reference for operation semantics");
            near(helper, attribute.getValue(), ClothData.sumAttributes(attribute.getModifiers()), 1.0e-8D,
                    "thermal clothing aggregation must preserve the same operation meaning");
            helper.succeed();
        }
    }

    @GameTest(template = TEMPLATE, batch = "player_release_campfire", timeoutTicks = 180)
    public static void actualCampfireAtTwelveDegreesHasRadiationAndNoPhantomBurn(GameTestHelper helper) {
        Fixture fixture = player(helper);
        var level = helper.getLevel();
        var player = fixture.player;
        BlockPos fire = helper.absolutePos(new BlockPos(2, 2, 2));
        ThermalFieldKey field = ThermalFieldKey.of(new ResourceLocation("frostedheart", "release_test"),
                player.getUUID(), 0);
        try {
            player.setPos(fire.getX() + 2.5D, fire.getY(), fire.getZ() + 0.5D);
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(FHItems.wool_jacket.get()));
            MinecraftGameplayFields.upsert(level, new ThermalAnalyticField(field, 100,
                    ThermalAnalyticField.CombineMode.OVERRIDE, player.getX(), player.getEyeY(), player.getZ(), 8, 12));
            level.setBlockAndUpdate(fire.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
            ((ICampfireExtra) level.getBlockEntity(fire)).setLifeTime(1000);
            helper.runAfterDelay(80, () -> {
                try {
                    PlayerTemperatureData data = new PlayerTemperatureData();
                    data.setDifficulty(FHTemperatureDifficulty.normal);
                    PlayerTemperatureComputation.updatePlayer(player, data, 20);
                    near(helper, 12.0D, data.getSampledAirTemperatureC(), 1.0e-5D,
                            "real gameplay query supplies the reported twelve-degree air");
                    helper.assertTrue(data.getSampledRadiantFluxWPerM2() > 0.0F,
                            "registered and lit campfire supplies actual line-of-sight radiation");
                    helper.assertTrue(data.getDisplayTemperatureC() > data.getEnvTemp(),
                            "the temperature orb includes the real campfire's already sampled radiation");
                    float health = player.getHealth();
                    for (int i = 0; i < 40; i++) PlayerThermalInjury.apply(player, data, 1.0D);
                    near(helper, health, player.getHealth(), 0.0D,
                            "real campfire cannot turn wool insulation into immediate phantom burns");
                    helper.succeed();
                } finally {
                    MinecraftGameplayFields.remove(level, field);
                    fixture.close();
                }
            });
        } catch (RuntimeException | Error failure) {
            MinecraftGameplayFields.remove(level, field);
            fixture.close();
            throw failure;
        }
    }

    @GameTest(template = TEMPLATE, batch = "player_release", timeoutTicks = 40)
    public static void clothedRadiationWarmsWithoutInventingBurnExposure(GameTestHelper helper) {
        try (Fixture fixture = player(helper)) {
            var player = fixture.player;
            var naked = new PlayerTemperatureData();
            var context = new HeatingDeviceContext();
            prepare(player, naked, context, 12.0D, 20.0D);
            double nakedGain = context.getRadiantPowerW(BodyPart.TORSO);
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(FHItems.wool_jacket.get()));
            var dressed = new PlayerTemperatureData();
            prepare(player, dressed, context, 12.0D, 0.0D);
            double withoutFire = context.getPassivePowerW(BodyPart.TORSO);
            prepare(player, dressed, context, 12.0D, 20.0D);
            double dressedGain = context.getRadiantPowerW(BodyPart.TORSO);
            helper.assertTrue(dressedGain > 0.0D && dressedGain < nakedGain * 0.5D,
                    "wool insulates incoming radiation while still allowing campfire warmth: naked="
                            + nakedGain + ", dressed=" + dressedGain + ", insulation="
                            + dressed.getClothDataByPart(player, BodyPart.TORSO).environmentExchangeFactor);
            helper.assertTrue(context.getPassivePowerW(BodyPart.TORSO) > withoutFire,
                    "heater feedback sees radiant watts once, alongside passive cooling");
            for (BodyPart part : BodyPart.VALUES)
                near(helper, 12.0D, dressed.getExposureTemperatureC(part), 1.0e-5D,
                        "radiation must not become an exposure temperature");
            PlayerThermalModel.integrateBody(dressed, context, 12.0D,
                    false, 0.0D, 0.0D, 0.0D, 8.0D, false);
            float health = player.getHealth();
            for (int i = 0; i < 40; i++) PlayerThermalInjury.apply(player, dressed, 1.0D);
            near(helper, health, player.getHealth(), 0.0D,
                    "12 C air and modest radiation cannot directly burn a normally warm body");
            helper.assertTrue(dressed.getAbsoluteBodyTempByPart(BodyPart.TORSO) < 37.01F,
                    "finite radiant input causes a finite integrated body change");
            helper.succeed();
        }
    }

    @GameTest(template = TEMPLATE, batch = "player_release", timeoutTicks = 120)
    public static void severeRealExposureStillDamagesAndInsulationReducesIt(GameTestHelper helper) {
        Fixture fixture = player(helper);
        // A newly logged-in ServerPlayer has 60 ticks of vanilla spawn invulnerability.
        helper.runAfterDelay(70, () -> {
            try {
            var data = new PlayerTemperatureData();
            for (BodyPart part : BodyPart.VALUES) data.setThermalExposure(part, 300.0D, 1.0D);
            PlayerThermalInjury.apply(fixture.player, data, 1.0D);
            helper.assertTrue(fixture.player.getHealth() < 20.0F,
                    "real severe heat retains immediate exposure damage");
            float fullExposureDamage = 20.0F - fixture.player.getHealth();
            fixture.player.setHealth(20.0F);
            fixture.player.invulnerableTime = 0;
            for (BodyPart part : BodyPart.VALUES) data.setThermalExposure(part, -300.0D, 0.0D);
            PlayerThermalInjury.apply(fixture.player, data, 1.0D);
            near(helper, 20.0D, fixture.player.getHealth(), 0.0D,
                    "a blocked transfer does not create direct cold injury");
            for (BodyPart part : BodyPart.VALUES) data.setThermalExposure(part, -300.0D, 0.25D);
            PlayerThermalInjury.apply(fixture.player, data, 4.0D);
            // Both instant damage types scale with vanilla difficulty (raw 4 becomes 3 on Easy).
            near(helper, 20.0D - fullExposureDamage, fixture.player.getHealth(), 1.0e-4D,
                    "four seconds at quarter transmission equals one full exposure hit after vanilla scaling");
            helper.succeed();
            } finally {
                fixture.close();
            }
        });
    }

    @GameTest(template = TEMPLATE, batch = "player_release", timeoutTicks = 40)
    public static void foodNeverReversesItsHeatDirectionOrChangesZeroInput(GameTestHelper helper) {
        var data = new PlayerTemperatureData();
        data.setAllPartsBodyTemp(6.0F);
        data.addUniformBodyEnergyJ(PlayerThermalModel.bodyEnergyForTemperatureDeltaJ(1.0D), -2.0F, 2.0F);
        near(helper, 6.0D, data.getBodyTempByPart(BodyPart.TORSO), 1.0e-6D,
                "hot food cannot cool an already overheated part to its cap");
        data.setAllPartsBodyTemp(-6.0F);
        data.addUniformBodyEnergyJ(PlayerThermalModel.bodyEnergyForTemperatureDeltaJ(-1.0D), -2.0F, 2.0F);
        near(helper, -6.0D, data.getBodyTempByPart(BodyPart.TORSO), 1.0e-6D,
                "cold food cannot warm an already cold part to its cap");
        data.addUniformBodyEnergyJ(0.0D, -2.0F, 2.0F);
        near(helper, -6.0D, data.getBodyTempByPart(BodyPart.TORSO), 1.0e-6D, "zero food energy is inert");
        data.addUniformBodyEnergyJ(PlayerThermalModel.bodyEnergyForTemperatureDeltaJ(1.0D), -2.0F, 2.0F);
        near(helper, -5.0D, data.getBodyTempByPart(BodyPart.TORSO), 1.0e-6D,
                "warming from below the food range adds one degree without teleporting to the range");
        data.setAllPartsBodyTemp(1.8F);
        data.addUniformBodyEnergyJ(PlayerThermalModel.bodyEnergyForTemperatureDeltaJ(1.0D), -2.0F, 2.0F);
        near(helper, 2.0D, data.getBodyTempByPart(BodyPart.TORSO), 1.0e-6D, "warming stops at its upper cap");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "player_release", timeoutTicks = 40)
    public static void bodyIllnessTracksWorseningRecoveryAndHotColdReversal(GameTestHelper helper) {
        try (Fixture fixture = player(helper)) {
            ServerPlayer player = fixture.player;
            PlayerTemperatureData data = PlayerTemperatureData.getCapability(player).orElseThrow(IllegalStateException::new);
            data.setAllPartsBodyTemp(1.5F);
            regulate(player);
            helper.assertTrue(player.getEffect(FHMobEffects.HYPERTHERMIA.get()).getAmplifier() == 0,
                    "mild real body heat produces mild illness");
            data.setAllPartsBodyTemp(4.0F);
            regulate(player);
            helper.assertTrue(player.getEffect(FHMobEffects.HYPERTHERMIA.get()).getAmplifier() == 2,
                    "worsening real heat escalates without waiting for a stale effect expiry");
            data.setAllPartsBodyTemp(-1.5F);
            regulate(player);
            helper.assertTrue(!player.hasEffect(FHMobEffects.HYPERTHERMIA.get())
                            && player.getEffect(FHMobEffects.HYPOTHERMIA.get()).getAmplifier() == 0,
                    "cold body clears old heat illness and selects current cold illness");
            data.setAllPartsBodyTemp(0.0F);
            regulate(player);
            helper.assertTrue(!player.hasEffect(FHMobEffects.HYPERTHERMIA.get())
                            && !player.hasEffect(FHMobEffects.HYPOTHERMIA.get()),
                    "normal body ends temperature-owned illness");
            helper.succeed();
        }
    }

    @GameTest(template = TEMPLATE, batch = "player_release", timeoutTicks = 40)
    public static void firstHeavyEquipmentAddsThenUpdatesAndClearsLoad(GameTestHelper helper) {
        try (Fixture fixture = player(helper)) {
            ItemStack armor = new ItemStack(Items.LEATHER_CHESTPLATE);
            UUID weightId = UUID.randomUUID();
            armor.addAttributeModifier(FHAttributes.EQUIPMENT_WEIGHT.get(), new AttributeModifier(
                    weightId, "release load", 16.0D, AttributeModifier.Operation.ADDITION), EquipmentSlot.CHEST);
            fixture.player.setItemSlot(EquipmentSlot.CHEST, armor);
            var data = new PlayerTemperatureData();
            PlayerTemperatureComputation.updatePlayer(fixture.player, data, 20);
            helper.assertTrue(fixture.player.getEffect(FHMobEffects.HEAVY_LOAD.get()).getAmplifier() == 1,
                    "first heavy item adds the effect without dereferencing a missing instance");
            fixture.player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            PlayerTemperatureComputation.updatePlayer(fixture.player, data, 20);
            helper.assertTrue(!fixture.player.hasEffect(FHMobEffects.HEAVY_LOAD.get()),
                    "removing the item clears its weight instead of accumulating reusable context state");
            helper.succeed();
        }
    }

    @GameTest(template = TEMPLATE, batch = "player_release", timeoutTicks = 40)
    public static void vestStopsHeatingHotBodyAndPadResourceFollowsActualTime(GameTestHelper helper) {
        try (Fixture fixture = player(helper)) {
            ItemStack vest = new ItemStack(FHItems.heater_vest.get());
            var storage = FHCapabilities.ITEM_HEAT.getCapability(vest).orElseThrow(IllegalStateException::new);
            storage.receiveEnergy(1000.0F, false);
            var data = new PlayerTemperatureData();
            data.setAllPartsBodyTemp(5.0F);
            var context = new HeatingDeviceContext();
            prepare(fixture.player, data, context, -20.0D, 0.0D);
            float before = storage.getEnergyStored();
            FHCapabilities.EQUIPMENT_HEATING.getCapability(vest).orElseThrow(IllegalStateException::new)
                    .tickHeating(HeatingDeviceSlot.vanilla(EquipmentSlot.CHEST), vest, context);
            near(helper, before, storage.getEnergyStored(), 0.0D,
                    "hot body does not burn vest energy just to cancel its cooling");
            near(helper, 0.0D, context.getPowerW(BodyPart.TORSO), 0.0D, "hot vest output is zero");
            data.setAllPartsBodyTemp(-2.0F);
            prepare(fixture.player, data, context, -20.0D, 0.0D);
            FHCapabilities.EQUIPMENT_HEATING.getCapability(vest).orElseThrow(IllegalStateException::new)
                    .tickHeating(HeatingDeviceSlot.vanilla(EquipmentSlot.CHEST), vest, context);
            helper.assertTrue(storage.getEnergyStored() < before && context.getPowerW(BodyPart.TORSO) > 0.0D,
                    "cold body consumes real stored heat and receives heater power");

            ItemStack pad = new ItemStack(FHItems.heating_pad.get());
            pad.getOrCreateTag().putInt("fuel", HeatingPadItem.BURN_TIME - 1);
            var heating = FHCapabilities.EQUIPMENT_HEATING.getCapability(pad).orElseThrow(IllegalStateException::new);
            context.reset(fixture.player, 0.5D, 4.0D, data);
            heating.tickHeating(HeatingDeviceSlot.vanilla(EquipmentSlot.OFFHAND), pad, context);
            near(helper, HeatingPadItem.HEATING_POWER_W, context.getPowerW(BodyPart.TORSO), 1.0e-6D,
                    "first half-second uses a partial second without premature expiry");
            context.reset(fixture.player, 1.0D, 8.0D, data);
            heating.tickHeating(HeatingDeviceSlot.vanilla(EquipmentSlot.OFFHAND), pad, context);
            near(helper, HeatingPadItem.HEATING_POWER_W * 0.5D, context.getPowerW(BodyPart.TORSO), 1.0e-6D,
                    "remaining half-second only supplies half of the next interval");
            helper.assertTrue(HeatingPadItem.getBurnTime(pad) == 0, "pad resource ends exactly once");
            helper.succeed();
        }
    }

    @GameTest(template = TEMPLATE, batch = "player_release", timeoutTicks = 40)
    public static void currentBodySaveRestoresEnergyAndLegacyBodyIntentionallyResets(GameTestHelper helper) {
        var original = new PlayerTemperatureData();
        original.setAllPartsBodyTemp(-2.0F);
        original.setBodyTempByPart(BodyPart.HANDS, -4.0F);
        CompoundTag saved = new CompoundTag();
        original.save(saved, false);
        var restored = new PlayerTemperatureData();
        restored.load(saved, false);
        near(helper, -2.0D, restored.getBodyTempByPart(BodyPart.TORSO), 1.0e-6D,
                "current-schema torso energy survives save/load");
        near(helper, -4.0D, restored.getBodyTempByPart(BodyPart.HANDS), 1.0e-6D,
                "independent current-schema hand energy survives save/load");
        saved.remove("thermal_schema");
        restored.load(saved, false);
        near(helper, 37.0D, restored.getAbsoluteCoreBodyTemp(), 1.0e-6D,
                "old body temperature intentionally resets per release policy");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "player_release", timeoutTicks = 40)
    public static void radiantDisplayChangesWithoutChangingAirBodyOrExposure(GameTestHelper helper) {
        var data = new PlayerTemperatureData();
        data.setThermalExposure(BodyPart.TORSO, 12.0D, 1.0D);
        data.applyThermalObservation(600.0D, 0.0D, 12.0D, 75.0D, 0.0D, 0.0D, false);
        near(helper, 22.0D, data.getDisplayTemperatureC(), 1.0e-5D,
                "75 W/m2 adds ten degrees to the orb's fixed-reference reading");
        near(helper, 12.0D, data.getEnvTemp(), 0.0D, "physical air is unchanged");
        near(helper, 12.0D, data.getExposureTemperatureC(BodyPart.TORSO), 0.0D,
                "display conversion cannot become a direct injury temperature");
        near(helper, 37.0D, data.getAbsoluteCoreBodyTemp(), 0.0D,
                "publishing an observation cannot apply radiant energy twice");
        helper.assertTrue(data.shouldSyncThermalState() && !data.shouldSyncThermalState(),
                "the first state syncs, an unchanged state does not");

        data.applyThermalObservation(600.0D, 0.0D, 12.0D, 150.0D, 0.0D, 0.0D, false);
        near(helper, 32.0D, data.getDisplayTemperatureC(), 1.0e-5D,
                "a radiation-only change updates the number at constant air and core");
        helper.assertTrue(data.shouldSyncThermalState() && !data.shouldSyncThermalState(),
                "radiation-only changes must reach the client");
        data.applyThermalObservation(12.0D, 0.0D, 12.0D, 0.0D, 0.0D, 0.0D, false);
        near(helper, 12.0D, data.getDisplayTemperatureC(), 0.0D,
                "extinguished or occluded radiation leaves exactly the air reading");
        helper.assertTrue(data.shouldSyncThermalState() && !data.shouldSyncThermalState(),
                "returning to zero radiation also syncs");
        data.applyThermalObservation(2.0D, 0.0D, 2.0D, 75.0D, 0.0D, 0.0D, false);
        near(helper, 12.0D, data.getDisplayTemperatureC(), 1.0e-5D,
                "different physical air and radiation can produce the same orb reading");
        helper.assertTrue(data.shouldSyncThermalState(),
                "air consumers still update when the final orb reading stays constant");
        data.forceThermalSync();
        helper.assertTrue(data.shouldSyncThermalState(), "lifecycle hooks can force the complete state");
        data.setEnvTemp(-8.0F);
        near(helper, 2.0D, data.getDisplayTemperatureC(), 1.0e-5D,
                "the existing air command also refreshes the server's final orb reading");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "player_release", timeoutTicks = 40)
    public static void thermalPacketCarriesFinalOrbAirAndCore(GameTestHelper helper) {
        try (Fixture fixture = player(helper)) {
            var data = PlayerTemperatureData.getCapability(fixture.player).orElseThrow(IllegalStateException::new);
            data.applyThermalObservation(600.0D, 0.0D, 12.0D, 95.5D, 0.0D, 0.0D, false);
            FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
            FriendlyByteBuf roundTrip = new FriendlyByteBuf(Unpooled.buffer());
            try {
                new FHBodyDataSyncPacket(fixture.player).encode(encoded);
                helper.assertTrue(encoded.readableBytes() == 7, "body payload is exactly seven bytes");
                new FHBodyDataSyncPacket(encoded).encode(roundTrip);
                helper.assertTrue(!encoded.isReadable(), "decoder consumes the complete payload");
                helper.assertTrue(roundTrip.readByte() == 2, "the final orb reading uses the updated body protocol");
                float airC = roundTrip.readShort() / 10.0F;
                float coreC = roundTrip.readShort() / 100.0F;
                float displayC = roundTrip.readShort() / 10.0F;
                near(helper, 12.0D, airC, 0.0D, "air retains its own Celsius field");
                near(helper, 37.0D, coreC, 0.0D, "core retains its own Celsius field");
                near(helper, 24.7D, displayC, 1.0e-5D, "the packet contains the final orb value at 0.1 C");
                helper.assertTrue(!roundTrip.isReadable(), "no extra packet data is required");
                var client = new PlayerTemperatureData();
                client.applyClientThermalSync(airC, coreC, displayC);
                near(helper, 12.0D, client.getEnvTemp(), 0.0D, "other client air consumers keep actual air");
                near(helper, 24.7D, client.getDisplayTemperatureC(), 1.0e-5D,
                        "the client displays the server's result without a radiant calculation");
                near(helper, 0.0D, client.getSampledRadiantFluxWPerM2(), 0.0D,
                        "raw radiation is not part of client synchronization");
                near(helper, 37.0D, client.getAbsoluteCoreBodyTemp(), 0.0D, "core sync remains intact");
                client.applyClientThermalSync(airC, coreC, airC);
                near(helper, 12.0D, client.getDisplayTemperatureC(), 0.0D,
                        "the later unheated result clears the client's display increment");
            } finally {
                encoded.release();
                roundTrip.release();
            }
            helper.succeed();
        }
    }

    @GameTest(template = TEMPLATE, batch = "player_release", timeoutTicks = 40)
    public static void thermalDisplayClearsRadiationOnResetAndLoad(GameTestHelper helper) {
        var data = new PlayerTemperatureData();
        data.applyThermalObservation(12.0D, 0.0D, 12.0D, 75.0D, 0.0D, 0.0D, false);
        data.deathResetTemperature();
        near(helper, 0.0D, data.getSampledRadiantFluxWPerM2(), 0.0D, "death clears the previous heat source");
        near(helper, data.getEnvTemp(), data.getDisplayTemperatureC(), 0.0D, "death leaves no display bonus");
        data.applyClientThermalSync(12.0F, 37.0F, 22.0F);
        data.reset();
        near(helper, data.getEnvTemp(), data.getDisplayTemperatureC(), 0.0D, "full reset leaves no display bonus");
        data.applyThermalObservation(12.0D, 0.0D, 12.0D, 75.0D, 0.0D, 0.0D, false);
        CompoundTag saved = new CompoundTag();
        data.save(saved, false);
        data.load(saved, false);
        near(helper, 0.0D, data.getSampledRadiantFluxWPerM2(), 0.0D, "saved body energy does not restore old radiation");
        near(helper, data.getEnvTemp(), data.getDisplayTemperatureC(), 0.0D, "load waits for a fresh observation");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "player_release", timeoutTicks = 40)
    public static void forecastCarriesClimateInsteadOfTheHeatedRoomReading(GameTestHelper helper) {
        try (Fixture fixture = player(helper)) {
            WorldClimate climate = WorldClimate.get(helper.getLevel());
            helper.assertTrue(climate != null, "the real server provides its climate capability");
            float expected = Math.round(climate.getTemp(fixture.player.chunkPosition()) * 10.0F) / 10.0F;
            var body = PlayerTemperatureData.getCapability(fixture.player).orElseThrow(IllegalStateException::new);
            body.applyThermalObservation(expected + 40.0D, 0.0D, expected + 40.0D, 75.0D,
                    0.0D, 0.0D, false);
            FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
            FriendlyByteBuf roundTrip = new FriendlyByteBuf(Unpooled.buffer());
            try {
                new FHClimatePacket(climate, fixture.player).encode(encoded);
                new FHClimatePacket(encoded).encode(roundTrip);
                helper.assertTrue(!encoded.isReadable(), "climate decoder consumes the added reading");
                encoded.readerIndex(0);
                helper.assertTrue(encoded.equals(roundTrip), "all existing climate fields survive round-trip");
                SerializeUtil.readShortArray(roundTrip);
                roundTrip.readVarLong();
                roundTrip.readVarLong();
                roundTrip.readByte();
                roundTrip.readByte();
                roundTrip.readVarInt();
                roundTrip.readFloat();
                near(helper, expected, roundTrip.readShort() / 10.0F, 1.0e-5D,
                        "the forecast uses actual chunk climate, independent of room heating and radiation");
                helper.assertTrue(!roundTrip.isReadable(), "the forecast adds only its final temperature");
            } finally {
                encoded.release();
                roundTrip.release();
            }
            ClientClimateData.climateTemperatureC = 123.0F;
            ClientClimateData.clear();
            near(helper, 0.0D, ClientClimateData.climateTemperatureC, 0.0D,
                    "leaving a world clears the previous forecast reading");
            helper.succeed();
        }
    }

    @GameTest(template = TEMPLATE, batch = "player_release", timeoutTicks = 40)
    public static void foodTemperatureRateDoesNotScaleBodyOrShiveringFoodCost(GameTestHelper helper) {
        double previousRate = FHConfig.SERVER.CLIMATE.tempSpeed.get();
        try (Fixture fixture = player(helper)) {
            var player = fixture.player;
            ThermalFieldKey field = ThermalFieldKey.of(new ResourceLocation("frostedheart", "body_rate_test"),
                    player.getUUID(), 0);
            MinecraftGameplayFields.upsert(player.serverLevel(), new ThermalAnalyticField(field, 100,
                    ThermalAnalyticField.CombineMode.OVERRIDE, player.getX(), player.getEyeY(), player.getZ(), 8, -50));
            try {
                var half = new PlayerTemperatureData();
                var full = new PlayerTemperatureData();
                half.setAllPartsBodyTemp(-2.0F);
                full.setAllPartsBodyTemp(-2.0F);
                player.getFoodData().setExhaustion(0.0F);
                FHConfig.SERVER.CLIMATE.tempSpeed.set(0.5D);
                PlayerTemperatureComputation.updatePlayer(player, half, 20);
                float halfCost = player.getFoodData().getExhaustionLevel();
                player.getFoodData().setExhaustion(0.0F);
                FHConfig.SERVER.CLIMATE.tempSpeed.set(1.0D);
                PlayerTemperatureComputation.updatePlayer(player, full, 20);
                for (BodyPart part : BodyPart.VALUES) near(helper,
                        half.getAbsoluteBodyTempByPart(part), full.getAbsoluteBodyTempByPart(part), 1.0e-6D,
                        "food multiplier cannot change ordinary body integration");
                helper.assertTrue(halfCost > 0.0F, "the cold fixture actually consumes shivering fuel");
                near(helper, halfCost, player.getFoodData().getExhaustionLevel(), 1.0e-6D,
                        "food multiplier cannot change the ordinary shivering cost");
            } finally {
                MinecraftGameplayFields.remove(player.serverLevel(), field);
            }
            helper.succeed();
        } finally {
            FHConfig.SERVER.CLIMATE.tempSpeed.set(previousRate);
        }
    }

    private static void prepare(ServerPlayer player, PlayerTemperatureData data, HeatingDeviceContext context,
                                double air, double radiation) {
        context.reset(player, 1.0D, 8.0D, data);
        for (BodyPart part : BodyPart.VALUES) PlayerThermalModel.preparePart(data, context, part,
                air, PlayerThermalModel.naturalConvectionCoefficientWPerM2K(air), radiation, 0.0D,
                0.0D, 0.0D, 0.0D, 0.0D, false, false);
    }

    private static void regulate(ServerPlayer player) {
        int interval = FHConfig.SERVER.CLIMATE.temperatureUpdateIntervalTicks.get();
        long mixed = player.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(player.getUUID().getLeastSignificantBits(), 17);
        player.tickCount = (int) Math.floorMod(mixed, interval);
        PlayerTemperatureUpdate.regulateTemperature(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
    }

    private static Fixture player(GameTestHelper helper) {
        var level = helper.getLevel();
        ServerPlayer player = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "thermal-release"));
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        connection.setProtocol(ConnectionProtocol.PLAY);
        Fixture fixture = new Fixture(player, channel);
        try {
            level.getServer().getPlayerList().placeNewPlayer(connection, player);
            player.setGameMode(GameType.SURVIVAL);
            // The real login hook grants enchanted starting armor. Body fixtures select their own clothing.
            for (EquipmentSlot slot : EquipmentSlot.values()) player.setItemSlot(slot, ItemStack.EMPTY);
            player.getFoodData().setFoodLevel(20);
            player.setPos(helper.absolutePos(new BlockPos(3, 3, 3)).getX() + 0.5D,
                    helper.absolutePos(new BlockPos(3, 3, 3)).getY(),
                    helper.absolutePos(new BlockPos(3, 3, 3)).getZ() + 0.5D);
            PlayerTemperatureData.getCapability(player).orElseThrow(IllegalStateException::new).setDifficulty(FHTemperatureDifficulty.normal);
            return fixture;
        } catch (RuntimeException | Error failure) {
            fixture.close();
            throw failure;
        }
    }

    private record Fixture(ServerPlayer player, EmbeddedChannel channel) implements AutoCloseable {
        @Override public void close() {
            player.server.getPlayerList().remove(player);
            channel.finishAndReleaseAll();
        }
    }

    private static void near(GameTestHelper helper, double expected, double actual, double tolerance, String message) {
        helper.assertTrue(Double.isFinite(actual) && Math.abs(expected - actual) <= tolerance,
                message + ": expected=" + expected + ", actual=" + actual);
    }
}
