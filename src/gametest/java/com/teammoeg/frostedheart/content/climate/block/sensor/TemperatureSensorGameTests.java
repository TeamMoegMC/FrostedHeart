/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.block.sensor;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_temperature_sensor")
@PrefixGameTestTemplate(false)
public class TemperatureSensorGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void thresholdInversionPersistenceAndPeriodicOutput(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, FHBlocks.TEMPERATURE_SENSOR.get().defaultBlockState());
        var sensor = (TemperatureSensorBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(sensor.getTargetTemperature() == 0 && !sensor.isInverted(), "default 0 Celsius, normal mode");
        var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        var menu = new TemperatureSensorMenu(1, player.getInventory(), sensor);
        helper.assertTrue(menu.stillValid(player), "shared validator accepts nearby player");
        player.setPos(pos.getX() + 9, pos.getY() + .5, pos.getZ() + .5);
        helper.assertTrue(!menu.stillValid(player), "shared validator rejects player outside eight blocks");
        float temperature = WorldTemperature.block(level, pos);
        sensor.configure(temperature, false);
        checkPower(helper, pos, 15);
        sensor.configure(temperature, true);
        checkPower(helper, pos, 0);
        sensor.configure(temperature + 10, true);
        checkPower(helper, pos, 15);
        sensor.configure(temperature + 10, false);
        checkPower(helper, pos, 0);
        sensor.configure(temperature - 10, false);
        checkPower(helper, pos, 15);
        sensor.configure(-42.5F, true);
        var saved = sensor.saveWithoutMetadata();
        var restored = new TemperatureSensorBlockEntity(pos, sensor.getBlockState());
        restored.load(saved);
        helper.assertTrue(restored.getTargetTemperature() == -42.5F && restored.isInverted(), "negative decimal target and mode survive NBT reload");
        sensor.configure(Float.NaN, false);
        helper.assertTrue(sensor.getTargetTemperature() == -42.5F && sensor.isInverted(), "invalid input leaves configuration intact");
        sensor.configure(temperature - 10, false);
        level.setBlockAndUpdate(pos, sensor.getBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED, false));
        helper.runAfterDelay(25, () -> {
            checkPower(helper, pos, 15);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            helper.assertTrue(level.getSignal(pos, Direction.NORTH) == 0, "removed sensor stops emitting");
            helper.succeed();
        });
    }
    private static void checkPower(GameTestHelper helper, BlockPos pos, int expected) {
        var level = helper.getLevel();
        for (Direction side : Direction.values()) {
            helper.assertTrue(level.getSignal(pos, side) == expected, "weak signal " + side + " = " + expected);
            helper.assertTrue(level.getDirectSignal(pos, side) == expected, "strong signal " + side + " = " + expected);
        }
    }
}
