/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import java.util.List;
import java.util.UUID;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.agriculture.FertilizedDirt;
import com.teammoeg.frostedheart.content.agriculture.Fertilizer.FertilizerGrade;
import com.teammoeg.frostedheart.content.agriculture.Fertilizer.FertilizerType;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorData;
import com.teammoeg.frostedheart.content.climate.data.PlantTempData;
import com.teammoeg.frostedheart.content.climate.network.SoilThermometerUpdatePacket;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MaterialSectionState;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Exercises the in-world wheat readout with real soil, a lit campfire and a tower field. */
@GameTestHolder("frostedheart_crop_temperature")
@PrefixGameTestTemplate(false)
public final class ThermalCropGameTests {
    @GameTest(template = "phase0a_empty", batch = "crop_temperature_sources", timeoutTicks = 600)
    public static void wheatSamplesHeatAtTheCrop(GameTestHelper helper) {
        assertCropMaterialDefaults(helper);
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos crop = helper.absolutePos(new BlockPos(2, 4, 2));
        BlockPos soil = crop.below();
        BlockPos fire = crop.west();
        // Enclose the local air with glass to retain skylight. Keep the fire's upward port clear.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    if (dy == -1 || dy == 2 || Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                        level.setBlockAndUpdate(crop.offset(dx, dy, dz), Blocks.GLASS.defaultBlockState());
                    }
                }
            }
        }
        level.setBlockAndUpdate(crop.east(2), Blocks.GLOWSTONE.defaultBlockState());
        var farmland = Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7);
        level.setBlockAndUpdate(soil, farmland);
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        helper.runAfterDelay(5, () -> testWheatHeating(helper, level, crop, soil, fire));
    }

    private static void testWheatHeating(GameTestHelper helper, ServerLevel level, BlockPos crop, BlockPos soil, BlockPos fire) {
        level.setBlockAndUpdate(crop, Blocks.WHEAT.defaultBlockState());
        helper.assertTrue(level.isLoaded(crop) && level.getBlockState(crop).is(Blocks.WHEAT), "the planted wheat must be queryable");

        var chunk = level.getChunkAt(soil);
        var previous = MinecraftThermalInput.dormantState(chunk);
        var checkpoint = new DormantChunkThermalState(chunk.getMinSection(), chunk.getSectionsCount());
        var soilRecords = new MaterialSectionState.Editor(ThermalMaterialBehaviorGameTests.storedBrick(helper, soil, -15));
        if (soil.getY() >> 4 == crop.getY() >> 4) {
            soilRecords.remove((crop.getX() & 15) | (crop.getZ() & 15) << 4 | (crop.getY() & 15) << 8);
        }
        checkpoint.replaceMaterials(soil.getY() >> 4, soilRecords.snapshot());
        MinecraftThermalInput.setDormantState(chunk, checkpoint);

        var data = PlantTempData.getPlantData(Blocks.WHEAT);
        float natural = WorldTemperature.naturalAir(level, crop);
        var generator = new GeneratorData(null);
        generator.dimension = level.dimension();
        generator.actualPos = crop.north(3);
        generator.RLevel = 1;
        generator.TLevel = 2;
        UUID owner = UUID.randomUUID();
        try {
            near(helper, natural, SoilThermometerUpdatePacket.sample(level, crop).temperature(), "unheated wheat");
            double storedSoil = MinecraftThermalInput.materialTemperature(level, soil);
            helper.assertTrue(storedSoil < data.minGrow(), "the stored soil must be too cold for wheat growth");
            generator.publishGameplayHeat(level, owner);
            double floor = towerFloor(level, crop, natural);
            near(helper, floor, SoilThermometerUpdatePacket.sample(level, crop).temperature(), "wheat with only the tower");
            near(helper, storedSoil, MinecraftThermalInput.materialTemperature(level, soil), "tower leaves stored soil temperature alone");
            helper.assertTrue(WorldTemperature.checkPlantStatus(level, crop, data).canGrow(),
                    "wheat without a material read uses the ambient temperature at the crop");

            checkpoint.replaceMaterials(crop.getY() >> 4, ThermalMaterialBehaviorGameTests.storedBrick(helper, crop, -12));
            near(helper, -12, SoilThermometerUpdatePacket.sample(level, crop).temperature(),
                    "the crop panel prefers the crop material record over the warm field");
            helper.assertTrue(WorldTemperature.checkPlantStatus(level, crop, data).willDie(),
                    "the server growth check uses the same cold material reading");
        } finally {
            generator.removeGameplayHeat(level.getServer());
            MinecraftThermalInput.setDormantState(chunk, previous);
        }

        var randomTicks = level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING);
        int previousSpeed = randomTicks.get();
        randomTicks.set(0, level.getServer());
        level.setBlockAndUpdate(fire, level.getBlockState(fire).setValue(CampfireBlock.LIT, true));
        ((ICampfireExtra) level.getBlockEntity(fire)).setLifeTime(20_000);
        var input = ThermalLoadedWorldGameTests.start(level, fire);
        helper.startSequence().thenWaitUntil(() -> {
            double raw = ThermalGameplayFieldGameTests.rawAir(input, crop);
            double vent = ThermalGameplayFieldGameTests.rawAir(input, fire.above());
            helper.assertTrue(raw > natural + 1.0,
                    "wheat should read warmed air: crop=" + raw + ", vent=" + vent + ", natural=" + natural);
        }).thenExecute(() -> {
            try {
                helper.assertTrue(level.getBlockState(crop).is(Blocks.WHEAT), "the test cell must still hold planted wheat: "
                        + level.getBlockState(crop) + ", soil=" + level.getBlockState(soil) + ", brightness=" + level.getRawBrightness(crop, 0));
                double raw = ThermalGameplayFieldGameTests.rawAir(input, crop);
                float growth = SoilThermometerUpdatePacket.sample(level, crop).temperature();
                double material = MinecraftThermalInput.materialTemperature(level, crop);
                near(helper, Double.isFinite(material) ? material : raw, growth, "wheat with only the campfire");
                helper.assertTrue(WorldTemperature.checkPlantStatus(level, crop, data)
                                == WorldTemperature.plantTemperatureStatus(data, growth),
                        "the wheat judgment must classify the same temperature sent to the client");
                generator.publishGameplayHeat(level, owner);
                double floor = towerFloor(level, crop, WorldTemperature.naturalAir(level, crop));
                float combined = SoilThermometerUpdatePacket.sample(level, crop).temperature();
                near(helper, Double.isFinite(material) ? material : Math.max(raw, floor), combined,
                        "campfire and tower on together");
                FHMain.LOGGER.info("Planted wheat temperature: natural={}, campfire={}, tower floor={}, combined={}",
                        natural, growth, floor, combined);
            } finally {
                generator.removeGameplayHeat(level.getServer());
                level.removeBlock(fire, false);
                MinecraftThermalInput.closeActiveLevel(level);
                randomTicks.set(previousSpeed, level.getServer());
            }
        }).thenSucceed();
    }

    @GameTest(template = "phase0a_empty", batch = "crop_temperature_fertilizer", timeoutTicks = 100)
    public static void fertilizerChangesWheatSurvivalAndGrowthTogether(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos crop = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlockAndUpdate(crop.above(2), Blocks.GLASS.defaultBlockState());
        level.setBlockAndUpdate(crop.east(2), Blocks.GLOWSTONE.defaultBlockState());
        level.setBlockAndUpdate(crop.below(), Blocks.FARMLAND.defaultBlockState());
        helper.runAfterDelay(5, () -> testPreservedWheat(helper, level, crop));
    }

    private static void testPreservedWheat(GameTestHelper helper, ServerLevel level, BlockPos crop) {
        level.setBlockAndUpdate(crop, Blocks.WHEAT.defaultBlockState());
        var data = PlantTempData.getPlantData(Blocks.WHEAT);
        try {
            heatCommand(level, crop, "set", " 2 -10 sphere");
            near(helper, -10, SoilThermometerUpdatePacket.sample(level, crop).temperature(), "plain wheat reading");
            helper.assertTrue(WorldTemperature.checkPlantStatus(level, crop, data).willDie(), "ordinary wheat is too cold");
            level.setBlockAndUpdate(crop.below(), FHBlocks.FERTILIZED_FARMLAND.get().defaultBlockState()
                    .setValue(FertilizedDirt.FERTILIZER, FertilizerType.PRESERVED)
                    .setValue(FertilizedDirt.GRADE, FertilizerGrade.ULTIMATE));
            helper.assertTrue(level.getBlockState(crop).is(Blocks.WHEAT), "wheat must survive the soil replacement: "
                    + level.getBlockState(crop) + ", brightness=" + level.getRawBrightness(crop, 0));
            near(helper, -10, SoilThermometerUpdatePacket.sample(level, crop).temperature(), "fertilizer changes the thresholds");
            helper.assertTrue(WorldTemperature.checkPlantStatus(level, crop, data).canGrow(), "protected wheat can grow at -10 C");
            heatCommand(level, crop, "set", " 2 40 sphere");
            helper.assertTrue(WorldTemperature.checkPlantStatus(level, crop, data).willDie(), "protection does not raise the heat limit");
            helper.succeed();
        } finally {
            heatCommand(level, crop, "remove", "");
        }
    }

    private static double towerFloor(ServerLevel level, BlockPos crop, float natural) {
        var field = MinecraftThermalInput.analyticFieldsAt(level, crop).get(0);
        return natural + field.temperatureCAt(crop.getX() + 0.5, crop.getY() + 0.5, crop.getZ() + 0.5);
    }

    private static void heatCommand(ServerLevel level, BlockPos crop, String action, String suffix) {
        level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack()
                .withLevel(level).withSuppressedOutput(),
                "heat_adjust " + action + " " + crop.getX() + " " + crop.getY() + " " + crop.getZ() + suffix);
    }

    /** Verifies the production material profiles used by the real heating scene. */
    private static void assertCropMaterialDefaults(GameTestHelper helper) {
        var snapshot = MinecraftThermalProfiles.prepare();
        for (var block : List.of(Blocks.WHEAT, FHBlocks.RYE_BLOCK.get(), Blocks.WHITE_WOOL, Blocks.SPRUCE_LEAVES)) {
            int signature = snapshot.states().signatureId(block.defaultBlockState());
            var profile = snapshot.materials().profileOrNull(snapshot.signatures().materialProfileId(signature));
            boolean crop = block == Blocks.WHEAT || block == FHBlocks.RYE_BLOCK.get();
            near(helper, crop ? 2.5 : 0.12, profile.faceConductanceWPerK(), "conductance for " + block);
            near(helper, 720, profile.thermalLaw().capacityJPerK(), "heat capacity for " + block);
        }
    }

    private static void near(GameTestHelper helper, double expected, double actual, String description) {
        helper.assertTrue(Math.abs(expected - actual) < 0.001, description + ": expected=" + expected + ", actual=" + actual);
    }
}
