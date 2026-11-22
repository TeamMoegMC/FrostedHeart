/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.SinglePlayerTeam;
import com.teammoeg.chorda.multiblock.CMultiblockHelper;
import com.teammoeg.frostedheart.bootstrap.common.FHFluids;
import com.teammoeg.frostedheart.bootstrap.common.FHMultiblocks;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorData;
import com.teammoeg.frostedheart.content.climate.block.generator.t2.T2GeneratorState;
import com.teammoeg.frostedheart.content.climate.thermal.consumer.TownThermalProjection;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField.CombineMode;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticFieldIndex;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalFieldKey;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Focused generator profile checks and reusable production/lifecycle regressions. */
@GameTestHolder("frostedheart_generator_heat")
@PrefixGameTestTemplate(false)
public final class GeneratorHeatFieldGameTests {
    private static final String TEMPLATE = "phase0a_empty";
    private static final ResourceLocation PROVIDER = new ResourceLocation("frostedheart", "generator_boost_test");

    private GeneratorHeatFieldGameTests() {}

    @GameTest(template = TEMPLATE, batch = "generator_boost_profile", timeoutTicks = 100)
    public static void centerBoostReachesEveryConsumerAndPreservesTheBoundary(GameTestHelper helper) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos center = helper.absolutePos(new BlockPos(2, 3, 2));
        GeneratorData generator = new GeneratorData(null);
        generator.dimension = level.dimension();
        generator.actualPos = center.above(7);
        generator.masterYPosInMB = 7;
        generator.RLevel = 1;
        generator.TLevel = 3;
        var player = FakePlayerFactory.getMinecraft(level);
        var previousPosition = player.position();
        try {
            generator.publishGameplayHeat(level, UUID.randomUUID());
            var fields = MinecraftGameplayFields.existing(level);
            var sample = new ThermalAnalyticFieldIndex.Sample();
            double[] boosts = {5, 4.39453125, 2.8125, .95703125, 0};
            for (Direction direction : Direction.values()) {
                for (int quarter = 0; quarter <= 4; quarter++) {
                    BlockPos point = center.relative(direction, generator.getRadius() * quarter / 4);
                    level.getChunkAt(point);
                    double delta = 30 + boosts[quarter];
                    near(helper, -40 + delta,
                            MinecraftThermalInput.sampleAirTemperature(level, point, -40), "passive radial boost");
                    near(helper, -40 + delta,
                            MinecraftThermalInput.sampleAirTemperature(level, point, -40), "crop radial boost");
                    double natural = WorldTemperature.naturalAir(level, point);
                    near(helper, natural + delta, WorldTemperature.getTempAtBlock(level, point), "block radial boost");
                    TownThermalProjection projection = new TownThermalProjection();
                    projection.include(point);
                    near(helper, natural + delta,
                            MinecraftThermalInput.sampleWeightedAirTemperature(level, projection, natural), "town radial boost");
                    fields.sample(point.getX() + .5, point.getY() + .5, point.getZ() + .5, sample);
                    near(helper, -40 + delta, sample.guaranteedFloor(-40), "phase radial floor");
                    near(helper, 50, fields.compose(point.getX() + .5, point.getY() + .5, point.getZ() + .5,
                            -40, 50), "hotter physical temperature must prevail");
                }
                near(helper, -40, MinecraftThermalInput.sampleAirTemperature(level,
                        center.relative(direction, generator.getRadius() + 1), -40), "outside radius");
            }
            helper.assertTrue(ThermalLoadedWorldGameTests.owner(level, center) == null,
                    "passive, crop, town and phase queries must leave physics absent");
            player.setPos(center.getX() + .5, center.getY() + .5 - player.getEyeHeight(), center.getZ() + .5);
            near(helper, -5, MinecraftThermalInput.samplePlayerAirTemperature(player, -40,
                    new ThermalEnvironmentSample()), "player receives the exact center boost");
            helper.succeed();
        } finally {
            player.setPos(previousPosition.x, previousPosition.y, previousPosition.z);
            generator.removeGameplayHeat(level.getServer());
            MinecraftThermalInput.closeActiveLevel(level);
        }
    }

    @GameTest(template = TEMPLATE, batch = "generator_boost_composition", timeoutTicks = 60)
    public static void overlappingBoostsTakeTheMaximumAndUnchangedReportsReuseDefinitions(GameTestHelper helper) {
        var fields = new ThermalAnalyticFieldIndex();
        var first = new ThermalFieldKey(PROVIDER, 0, 1, 0);
        var second = new ThermalFieldKey(PROVIDER, 0, 2, 0);
        fields.upsertSphere(first, 0, CombineMode.FLOOR_FROM_NATURAL, 0, 0, 0, 16, 30, 5);
        var definition = fields.fieldsAt(0, 0, 0).get(0);
        fields.upsertSphere(first, 0, CombineMode.FLOOR_FROM_NATURAL, 0, 0, 0, 16, 30, 5);
        helper.assertTrue(fields.fieldsAt(0, 0, 0).get(0) == definition, "unchanged boost must retain its definition");
        fields.upsertSphere(second, 0, CombineMode.FLOOR_FROM_NATURAL, 8, 0, 0, 16, 35, 5);
        near(helper, -2.1875, fields.compose(0, 0, 0, -40, -40), "overlap uses maximum local delta");
        var sample = new ThermalAnalyticFieldIndex.Sample();
        fields.sample(0, 0, 0, sample);
        near(helper, -2.1875, sample.compose(-40, -40), "sample and direct overlap composition agree");
        fields.upsert(new ThermalAnalyticField(new ThermalFieldKey(PROVIDER, 0, 3, 0), 0,
                CombineMode.OVERRIDE, 0, 0, 0, 16, 20));
        fields.upsert(new ThermalAnalyticField(new ThermalFieldKey(PROVIDER, 0, 4, 0), 0,
                CombineMode.ADD_DELTA, 0, 0, 0, 16, -3));
        near(helper, 17, fields.compose(0, 0, 0, -40, -40), "command and boss order");
        fields.sample(0, 0, 0, sample);
        near(helper, 17, sample.compose(-40, -40), "sample keeps command and boss order");
        fields.upsertSphere(first, 0, CombineMode.FLOOR_FROM_NATURAL, 0, 0, 0, 16, 30);
        var uniform = fields.fieldsAt(0, 0, 0).get(0);
        helper.assertTrue(uniform != definition, "removing only the boost must replace the definition");
        near(helper, 30, uniform.temperatureCAt(0, 0, 0), "existing unboosted API remains uniform");
        near(helper, 30, uniform.temperatureCAt(8, 0, 0), "uniform sphere has no radial boost");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "generator_boost_infrared", timeoutTicks = 100)
    public static void infraredUsesTheBoostAndUpdatesChangedRanges(GameTestHelper helper) {
        ThermalInfraredGameTests.realGeneratorFieldHeatsDisplayWithoutPhysicsAndClearsItsOldRange(helper);
    }

    @GameTest(template = TEMPLATE, batch = "generator_boost_query_cost", timeoutTicks = 80)
    public static void compareUniformAndBoostedProductionQueryCost(GameTestHelper helper) {
        ThermalGameplayFieldGameTests.sampleProductionQueryCostWithoutStartingRuntime(helper);
    }

    @GameTest(template = TEMPLATE, batch = "generator_boost_inventory", timeoutTicks = 60)
    public static void inventoryAndDroppedReservoirsReceiveTheBoost(GameTestHelper helper) {
        WarmStoneGameplayFieldGameTests.generatorFieldReachesInventoryAndDroppedReservoirsWithoutRuntime(helper);
    }

    @GameTest(template = TEMPLATE, batch = "generator_boost_cache", timeoutTicks = 60)
    public static void cachedItemsObserveUpdatesAndReloads(GameTestHelper helper) {
        WarmStoneGameplayFieldGameTests.cachedItemsObserveCurrentFieldsAndRuntimeReload(helper);
    }

    @GameTest(template = TEMPLATE, batch = "generator_boost_t1", timeoutTicks = 340)
    public static void formedT1KeepsNormalProductionAndAfterheat(GameTestHelper helper) {
        ThermalLoadedWorldGameTests.formedT1RecoversFromItsNormalProductionTick(helper);
    }

    @GameTest(template = TEMPLATE, batch = "generator_boost_t2", timeoutTicks = 340)
    public static void formedT2KeepsNormalProductionAndAfterheat(GameTestHelper helper) {
        ThermalLoadedWorldGameTests.formedT2RecoversFromItsNormalProductionTick(helper);
    }

    @GameTest(template = TEMPLATE, batch = "generator_t2_warmup", timeoutTicks = 100)
    public static void t2WarmupKeepsExactDoseAndShortageBehavior(GameTestHelper helper) {
        var level = helper.getLevel();
        var structure = FHMultiblocks.GENERATOR_T2;
        BlockPos master = ThermalLoadedWorldGameTests.form(helper, structure, helper.absolutePos(new BlockPos(2, 2, 2)));
        var state = (T2GeneratorState) CMultiblockHelper.getBEHelper(level, master).getState();
        ThermalLoadedWorldGameTests.prepareProfileCache(level.getServer());
        UUID id = UUID.randomUUID();
        level.getServer().getProfileCache().add(new GameProfile(id, "Warm" + id.toString().substring(0, 8)));
        var team = CTeamDataManager.INSTANCE.get(new SinglePlayerTeam(id));
        state.setOwner(team.getId());
        state.regist(level, master, (short) structure.getMasterFromOriginOffset().getY());
        var data = state.getData(master).orElseThrow();
        data.isWorking = true;
        data.process = 200;

        int rate = FHConfig.SERVER.CLIMATE.generatorSteamSpeed.get();
        int duration = FHConfig.SERVER.CLIMATE.generatorSteamCost.get();
        CompoundTag before = new CompoundTag();
        state.writeSaveNBT(before);
        before.putInt("steamLevel", 40);
        before.putInt("liquidtick", duration - 1);
        state.readSaveNBT(before);
        var steam = FHFluids.STEAM.get();
        helper.assertTrue(state.tank.fill(new FluidStack(steam, rate), FluidAction.EXECUTE) == rate, "one real steam dose");

        helper.runAfterDelay(1, () -> {
            CompoundTag wet = new CompoundTag();
            state.writeSaveNBT(wet);
            helper.assertTrue(state.tank.isEmpty() && wet.getInt("steamLevel") == 41
                    && wet.getInt("liquidtick") == 0 && wet.getInt("noliquidtick") == 0,
                    "exact dose increments warm-up once and drains exactly that dose; " + wet);
            if (rate > 1) state.tank.fill(new FluidStack(steam, rate - 1), FluidAction.EXECUTE);
            helper.runAfterDelay(1, () -> {
                CompoundTag dry = new CompoundTag();
                state.writeSaveNBT(dry);
                helper.assertTrue(state.tank.isEmpty() && dry.getInt("steamLevel") == 40
                        && dry.getInt("liquidtick") == duration - 1 && dry.getInt("noliquidtick") == 40,
                        "short dose follows the unchanged empty/cooling/cooldown path; " + dry);
                level.destroyBlock(master, false);
                helper.succeed();
            });
        });
    }

    @GameTest(template = TEMPLATE, batch = "generator_boost_enclosure", timeoutTicks = 900)
    public static void physicalHeatingCanExceedTheBoostedFloor(GameTestHelper helper) {
        ThermalGameplayFieldGameTests.generatorPhysicalHeatCanExceedItsAnalyticFloorInAnEnclosure(helper);
    }

    @GameTest(template = TEMPLATE, batch = "generator_boost_unloaded", timeoutTicks = 900)
    public static void unloadedTowersRetainTheBoostWithoutLoadingChunks(GameTestHelper helper) {
        ThermalGameplayFieldGameTests.unloadedTowerRetainsTeamGameplayHeatWithoutReloadingItsChunk(helper);
    }

    @GameTest(template = TEMPLATE, batch = "generator_boost_authority", timeoutTicks = 160)
    public static void rebindAndTeamTransferKeepTheirOriginalCleanup(GameTestHelper helper) {
        ThermalGameplayFieldGameTests.rebindAndTeamTransferRemoveOnlyTheObsoleteGenerator(helper);
    }

    @GameTest(template = TEMPLATE, batch = "generator_four_direction_phase", timeoutTicks = 600)
    public static void fourDirectionSourceKeepsPhaseAcknowledgementsThroughTopologyChanges(GameTestHelper helper) {
        FrostedHeartMinecraftThermalInputGameTests.phaseRequestSurvivesSameBrickTopologyChurn(helper);
    }

    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) < .001, message + ": expected=" + expected + ", actual=" + actual);
    }
}
