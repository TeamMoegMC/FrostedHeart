/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.player.thermalitem.ThermalReservoirBlock;
import com.teammoeg.frostedheart.content.climate.player.thermalitem.ThermalReservoirBlockEntity;
import com.teammoeg.frostedheart.content.climate.player.thermalitem.WearableThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalFieldKey;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FHMain.MODID)
@PrefixGameTestTemplate(false)
public final class PlacedThermalReservoirGameTests {
    @GameTest(template = "phase0a_empty", batch = "placed_reservoir_lifecycle")
    public static void placementReloadAndDropsPreserveBothReservoirs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int offset = 2;
        for (Item item : items()) {
            BlockPos pos = helper.absolutePos(new BlockPos(offset, 3, 2));
            ItemStack original = new ItemStack(item);
            original.setHoverName(Component.literal("Stored heat"));
            new WearableThermalState(73.5, 41.25).writeTo(original);
            ItemStack expected = original.copy();
            place(helper, pos, original, false);
            helper.assertTrue(original.isEmpty(), "survival placement consumes exactly one item");
            ThermalReservoirBlockEntity placed = reservoir(level, pos);
            helper.assertTrue(ItemStack.matches(expected, placed.copyStoredStack()), "placement preserves all item NBT");
            var state = level.getBlockState(pos);
            helper.assertTrue(state.getValue(ThermalReservoirBlock.FACING) == Direction.NORTH, "placement follows player facing");
            BlockEntity restored = BlockEntity.loadStatic(pos, state, placed.saveWithFullMetadata());
            helper.assertTrue(restored instanceof ThermalReservoirBlockEntity, "block entity type reloads correctly");
            level.removeBlockEntity(pos);
            level.setBlockEntity(restored);
            helper.assertTrue(ItemStack.matches(expected, reservoir(level, pos).copyStoredStack()), "save/reload preserves temperatures");
            helper.assertTrue(ItemStack.matches(expected, state.getBlock().getCloneItemStack(level, pos, state)), "pick block keeps temperatures");
            level.destroyBlock(pos, true);
            assertSingleDrop(helper, pos, expected);

            BlockPos supported = pos.south(4);
            place(helper, supported, expected.copy(), false);
            level.destroyBlock(supported.below(), false);
            helper.assertTrue(level.isEmptyBlock(supported), "losing support removes the placed reservoir");
            assertSingleDrop(helper, supported, expected);

            ItemStack creative = expected.copy();
            BlockPos creativePos = pos.south(8);
            place(helper, creativePos, creative, true);
            helper.assertTrue(ItemStack.matches(expected, creative), "creative placement keeps the held item");
            helper.assertTrue(ItemStack.matches(expected, reservoir(level, creativePos).copyStoredStack()), "creative placement copies heat state");
            offset += 5;
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "placed_reservoir_air", timeoutTicks = 100)
    public static void placedReservoirsWarmAndCoolOnLoadedTicks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos[] positions = new BlockPos[4];
        ThermalFieldKey[] fields = new ThermalFieldKey[4];
        int index = 0;
        for (Item item : items()) {
            for (double air : new double[]{100, -50}) {
                BlockPos pos = helper.absolutePos(new BlockPos(2 + index * 3, 3, 2));
                positions[index] = pos;
                fields[index] = field(level, pos, air);
                ItemStack stack = new ItemStack(item);
                new WearableThermalState(20, 20).writeTo(stack);
                place(helper, pos, stack, false);
                index++;
            }
        }
        helper.runAfterDelay(10, () -> {
            for (BlockPos pos : positions) {
                near(helper, 20, temperature(level, pos).surfaceTemperatureC(), "no exchange before the first loaded second");
            }
        });
        helper.runAfterDelay(65, () -> {
            try {
                for (int i = 0; i < positions.length; i++) {
                    WearableThermalState state = temperature(level, positions[i]);
                    helper.assertTrue(i % 2 == 0 ? state.surfaceTemperatureC() > 20 : state.surfaceTemperatureC() < 20,
                            "placed reservoir must exchange with hot and cold environments");
                    helper.assertTrue(i % 2 == 0 ? state.coreTemperatureC() > 20 : state.coreTemperatureC() < 20,
                            "core and surface must both participate in exchange");
                }
            } finally {
                for (ThermalFieldKey key : fields) MinecraftGameplayFields.remove(level, key);
            }
            helper.succeed();
        });
    }

    @GameTest(template = "phase0a_empty", batch = "placed_reservoir_initialization", timeoutTicks = 70)
    public static void freshPlacementInitializesFromTheEnvironment(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 3, 2));
        ThermalFieldKey key = field(level, pos, 45);
        place(helper, pos, new ItemStack(FHItems.hot_water_bag.get()), false);
        helper.runAfterDelay(45, () -> {
            try {
                WearableThermalState state = temperature(level, pos);
                near(helper, 45, state.coreTemperatureC(), "fresh core initializes from composed air");
                near(helper, 45, state.surfaceTemperatureC(), "fresh surface initializes from composed air");
            } finally {
                MinecraftGameplayFields.remove(level, key);
            }
            helper.succeed();
        });
    }

    @GameTest(template = "phase0a_empty", batch = "placed_reservoir_radiation", timeoutTicks = 100)
    public static void placedReservoirsReceiveTheDroppedItemRadiationBoundary(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos source = helper.absolutePos(new BlockPos(5, 3, 5));
        level.setBlockAndUpdate(source, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        ((ICampfireExtra) level.getBlockEntity(source)).setLifeTime(20_000);
        BlockPos[] positions = {source.east(2), source.west(2)};
        Item[] items = items();
        for (int i = 0; i < items.length; i++) {
            ItemStack stack = new ItemStack(items[i]);
            new WearableThermalState(-100, -100).writeTo(stack);
            place(helper, positions[i], stack, false);
        }
        var player = FakePlayerFactory.getMinecraft(level);
        player.setPos(source.getX() + 8.5, source.getY(), source.getZ() + 0.5);
        MinecraftThermalInput.gameplayPlayerEnvironment(player,
                WorldTemperature.naturalAir(level, player.blockPosition()), new ThermalEnvironmentSample());
        helper.runAfterDelay(70, () -> {
            try {
                for (BlockPos pos : positions) {
                    ThermalEnvironmentSample placed = new ThermalEnvironmentSample();
                    ThermalEnvironmentSample dropped = new ThermalEnvironmentSample();
                    double natural = WorldTemperature.naturalAir(level, pos);
                    MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, pos, natural, placed);
                    ItemEntity probe = new ItemEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                            reservoir(level, pos).copyStoredStack());
                    probe.setPos(probe.getX(), pos.getY() + 0.3125 - probe.getBbHeight() * 0.5, probe.getZ());
                    MinecraftThermalInput.gameplayItemEnvironment(probe, natural, dropped);
                    probe.discard();
                    helper.assertTrue(placed.radiantFluxWPerM2() > 0, "campfire radiation must reach the placed reservoir");
                    near(helper, dropped.radiantFluxWPerM2(), placed.radiantFluxWPerM2(), "placed and dropped radiation agree");
                    near(helper, dropped.airTemperatureC(), placed.airTemperatureC(), "placed and dropped composed air agree");
                    helper.assertTrue(temperature(level, pos).surfaceTemperatureC() > -100, "real block ticks advance stored heat");
                }
            } finally {
                MinecraftThermalInput.closeActiveLevel(level);
            }
            helper.succeed();
        });
    }

    private static Item[] items() {
        return new Item[]{FHItems.warm_stone.get(), FHItems.hot_water_bag.get()};
    }

    private static void place(GameTestHelper helper, BlockPos pos, ItemStack stack, boolean creative) {
        ServerLevel level = helper.getLevel();
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        var player = FakePlayerFactory.getMinecraft(level);
        ItemStack previous = player.getMainHandItem();
        GameType previousMode = player.gameMode.getGameModeForPlayer();
        try {
            player.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
            player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() - 2);
            player.setYRot(0);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var result = stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false)));
            helper.assertTrue(result.consumesAction() && level.getBlockEntity(pos) instanceof ThermalReservoirBlockEntity,
                    "right-click must place the reservoir block");
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, previous);
            player.setGameMode(previousMode);
        }
    }

    private static ThermalReservoirBlockEntity reservoir(ServerLevel level, BlockPos pos) {
        return (ThermalReservoirBlockEntity) level.getBlockEntity(pos);
    }

    private static WearableThermalState temperature(ServerLevel level, BlockPos pos) {
        return WearableThermalState.read(reservoir(level, pos).copyStoredStack()).orElseThrow();
    }

    private static ThermalFieldKey field(ServerLevel level, BlockPos pos, double air) {
        ThermalFieldKey key = new ThermalFieldKey(new ResourceLocation(FHMain.MODID, "placed_reservoir_test"), 0, pos.asLong(), 0);
        MinecraftGameplayFields.upsertSphere(level, key, 0, ThermalAnalyticField.CombineMode.OVERRIDE,
                pos.getX() + 0.5, pos.getY() + 0.3125, pos.getZ() + 0.5, 0.5, air);
        return key;
    }

    private static void assertSingleDrop(GameTestHelper helper, BlockPos pos, ItemStack expected) {
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(0.5));
        helper.assertTrue(drops.size() == 1 && ItemStack.matches(expected, drops.get(0).getItem()),
                "breaking must drop exactly one item with its original temperatures and name");
        drops.forEach(ItemEntity::discard);
    }

    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) < 0.001, message + ": expected=" + expected + ", actual=" + actual);
    }
}
