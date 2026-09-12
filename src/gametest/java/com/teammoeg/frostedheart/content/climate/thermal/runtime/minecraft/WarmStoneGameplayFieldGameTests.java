/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.climate.player.thermalitem.WearableThermalExchangeHandler;
import com.teammoeg.frostedheart.content.climate.player.thermalitem.WearableThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalFieldKey;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

import static com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ThermalLoadedWorldGameTests.owner;
import static com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ThermalLoadedWorldGameTests.start;

@GameTestHolder(FHMain.MODID)
@PrefixGameTestTemplate(false)
public final class WarmStoneGameplayFieldGameTests {
    @GameTest(template = "phase0a_empty", batch = "warm_stone_generator_no_runtime", timeoutTicks = 60)
    public static void generatorFieldReachesInventoryAndDroppedReservoirsWithoutRuntime(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos position = helper.absolutePos(new BlockPos(2, 3, 2));
        MinecraftThermalInput.closeActiveLevel(level);
        GeneratorData generator = generator(level, position);
        UUID identity = UUID.randomUUID();
        var player = FakePlayerFactory.getMinecraft(level);
        ItemStack previous = player.getInventory().items.get(0);
        int previousTick = player.tickCount;
        try {
            generator.publishGameplayHeat(level, identity);
            double natural = WorldTemperature.naturalAir(level, position);
            double expected = natural + generator.getTempMod();
            player.setPos(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
            player.tickCount = 20;
            for (Item reservoir : new Item[]{FHItems.warm_stone.get(), FHItems.hot_water_bag.get()}) {
                ItemStack inventory = new ItemStack(reservoir);
                player.getInventory().items.set(0, inventory);
                reservoir.inventoryTick(inventory, level, player, 0, false);
                helper.assertTrue(WearableThermalState.read(inventory).isPresent(),
                        "inventory hook must initialize " + reservoir + ", natural=" + natural + ", tick=" + player.tickCount);
                near(helper, expected, WearableThermalState.read(inventory).orElseThrow().surfaceTemperatureC(),
                        "inventory initialization must use the generator floor");

                ItemStack dropped = new ItemStack(reservoir);
                ItemEntity entity = item(level, position, dropped);
                helper.assertTrue(entity.isAlive() && entity.getItem() == dropped && dropped.getCount() == 1,
                        "dropped fixture must expose the exact live single stack for " + reservoir);
                for (entity.tickCount = 20; entity.tickCount < 40; entity.tickCount++) {
                    reservoir.onEntityItemUpdate(dropped, entity);
                }
                helper.assertTrue(WearableThermalState.read(dropped).isPresent(),
                        "dropped hook must initialize " + reservoir + ", natural=" + natural);
                near(helper, expected, WearableThermalState.read(dropped).orElseThrow().surfaceTemperatureC(),
                        "dropped initialization must use the generator floor");
                new WearableThermalState(natural, natural).writeTo(dropped);
                for (; entity.tickCount < 60; entity.tickCount++) reservoir.onEntityItemUpdate(dropped, entity);
                helper.assertTrue(WearableThermalState.read(dropped).orElseThrow().surfaceTemperatureC() > natural,
                        "an initialized dropped reservoir must gain heat from the generator floor");
                entity.discard();
            }
            helper.assertTrue(owner(level, position) == null, "reservoir queries must not start a physical runtime");
            helper.succeed();
        } finally {
            player.getInventory().items.set(0, previous);
            player.tickCount = previousTick;
            generator.removeGameplayHeat(level.getServer());
        }
    }

    @GameTest(template = "phase0a_empty", batch = "warm_stone_generator_cache", timeoutTicks = 60)
    public static void cachedItemsObserveCurrentFieldsAndRuntimeReload(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos position = helper.absolutePos(new BlockPos(2, 3, 2));
        MinecraftThermalInput.closeActiveLevel(level);
        GeneratorData generator = generator(level, position);
        UUID identity = UUID.randomUUID();
        ThermalFieldKey control = new ThermalFieldKey(new ResourceLocation("frostedheart", "warm_stone_test"), 0, position.asLong(), 0);
        ThermalEnvironmentSample sample = new ThermalEnvironmentSample();
        ItemEntity entity = item(level, position, new ItemStack(FHItems.warm_stone.get()));
        try {
            start(level, position);
            MinecraftThermalInput.gameplayItemEnvironment(entity, 110, sample);
            double base = sample.airTemperatureC();
            generator.publishGameplayHeat(level, identity);
            MinecraftThermalInput.gameplayItemEnvironment(entity, 110, sample);
            near(helper, Math.max(base, 110 + generator.getTempMod()), sample.airTemperatureC(), "cached raw air plus floor");
            generator.TLevel = 6;
            generator.publishGameplayHeat(level, identity);
            MinecraftThermalInput.gameplayItemEnvironment(entity, 110, sample);
            near(helper, Math.max(base, 110 + generator.getTempMod()), sample.airTemperatureC(), "same-tick tower update");

            MinecraftGameplayFields.upsertSphere(level, control, 0, ThermalAnalyticField.CombineMode.OVERRIDE,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 0.04, 42);
            MinecraftThermalInput.gameplayItemEnvironment(entity, 110, sample);
            near(helper, 42, sample.airTemperatureC(), "command control after generator floor");
            entity.setPos(entity.getX() + 0.1, entity.getY(), entity.getZ());
            MinecraftThermalInput.gameplayItemEnvironment(entity, 110, sample);
            near(helper, Math.max(base, 110 + generator.getTempMod()), sample.airTemperatureC(),
                    "same quarter-block cache must still respect the exact field boundary");
            MinecraftGameplayFields.remove(level, control);
            generator.removeGameplayHeat(level.getServer());
            MinecraftThermalInput.gameplayItemEnvironment(entity, 110, sample);
            near(helper, base, sample.airTemperatureC(), "field removal must not leave composed heat in the cache");

            generator.publishGameplayHeat(level, identity);
            MinecraftThermalInput.closeActiveLevel(level);
            MinecraftThermalInput.gameplayItemEnvironment(entity, 110, sample);
            near(helper, 110 + generator.getTempMod(), sample.airTemperatureC(), "world field survives physical runtime close");
            near(helper, 0, sample.radiantFluxWPerM2(), "no physical runtime means no direct radiation");
            helper.assertTrue(owner(level, position) == null, "dropped query must leave the closed runtime absent");

            var player = FakePlayerFactory.getMinecraft(level);
            player.setPos(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
            double air = MinecraftThermalInput.gameplayPlayerEnvironment(player, 110, sample);
            for (Item reservoir : new Item[]{FHItems.warm_stone.get(), FHItems.hot_water_bag.get()}) {
                ItemStack worn = new ItemStack(reservoir);
                new WearableThermalExchangeHandler().exchangeInto(new PlayerTemperatureData(), worn, air, 1);
                near(helper, air, WearableThermalState.read(worn).orElseThrow().surfaceTemperatureC(),
                        "worn initialization must receive the composed player air");
            }
            helper.succeed();
        } finally {
            entity.discard();
            MinecraftGameplayFields.remove(level, control);
            generator.removeGameplayHeat(level.getServer());
            MinecraftThermalInput.closeActiveLevel(level);
        }
    }

    private static GeneratorData generator(ServerLevel level, BlockPos position) {
        GeneratorData generator = new GeneratorData(null);
        generator.dimension = level.dimension();
        generator.actualPos = position;
        generator.RLevel = 1;
        generator.TLevel = 3;
        return generator;
    }

    private static ItemEntity item(ServerLevel level, BlockPos position, ItemStack stack) {
        ItemEntity entity = new ItemEntity(level, position.getX() + 0.5, position.getY(), position.getZ() + 0.5, stack);
        entity.setPos(entity.getX(), position.getY() + 0.5 - entity.getBbHeight() * 0.5, entity.getZ());
        return entity;
    }

    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) < 0.001, message + ": expected=" + expected + ", actual=" + actual);
    }
}
