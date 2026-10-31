/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Real fuel, cooking, redstone and disk lifecycle; never assigns a fuel lifetime or thermal state. */
@GameTestHolder("frostedheart_campfire_fuel")
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = FHMain.MODID)
public final class CampfireFuelProductionGameTests {
    private static volatile DiskFuel observation;

    @GameTest(template = "phase0a_empty", batch = "campfire_countdown", timeoutTicks = 1400)
    public static void realSticksCookFoodAndExtinguishAfterTheLastFuelTick(GameTestHelper helper) throws java.io.IOException {
        ContinuousAirPrecisionProductionGameTests.restoreReferenceClimate(helper, true);
        var level = helper.getLevel();
        BlockPos relativeFire = new BlockPos(8, 2, 8);
        BlockPos fire = helper.absolutePos(relativeFire);
        placeFire(helper, fire, new ItemStack(Items.STICK, 3));
        int initial = ((ICampfireExtra) level.getBlockEntity(fire)).getLifeTime();
        helper.assertTrue(initial > 600 && initial < 1200, "Actual sticks must cover one ordinary cooking recipe");
        BlockPos comparator = fire.east();
        level.setBlockAndUpdate(comparator, Blocks.COMPARATOR.defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.WEST));
        // Item and research callbacks require a server player on the dedicated GameTest server.
        var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.BEEF));
        helper.useBlock(relativeFire, player);
        var entity = (CampfireBlockEntity) level.getBlockEntity(fire);
        helper.assertTrue(entity.getItems().stream().anyMatch(stack -> stack.is(Items.BEEF)),
                "A real block interaction must put beef into the campfire inventory");
        long started = level.getGameTime();
        int[] previousFuel = {-1}, zeroObserved = {-1};
        boolean[] previousLit = {true};
        var air = new com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample();
        ContinuousAirProductionTiming.eachTick(helper, initial + 40, elapsed -> {
            int remaining = ((ICampfireExtra) level.getBlockEntity(fire)).getLifeTime();
            boolean lit = level.getBlockState(fire).getValue(CampfireBlock.LIT);
            if (previousFuel[0] >= 0) {
                helper.assertTrue(remaining == Math.max(0, previousFuel[0] - (previousLit[0] ? 1 : 0)),
                        "Real ticking must consume exactly one fuel tick while lit; elapsed=" + elapsed);
                if (previousFuel[0] == 1) helper.assertTrue(lit,
                        "The tick that consumes the final fuel unit remains lit, as before");
                if (previousFuel[0] == 0) helper.assertTrue(!lit,
                        "The next real tick must extinguish the actual campfire");
            }
            if (remaining == 0 && zeroObserved[0] < 0) zeroObserved[0] = elapsed;
            helper.assertTrue(level.getBlockState(comparator).getSignal(level, comparator, Direction.WEST) == 0,
                    "An ordinary campfire has no comparator output during fuel countdown");
            previousFuel[0] = remaining;
            previousLit[0] = lit;
            if (elapsed % 40 == 0) {
                MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, fire.above(),
                        com.teammoeg.frostedheart.content.climate.WorldTemperature.naturalAir(level, fire.above()), air);
                FHMain.LOGGER.info("THERMAL_CAMPFIRE_FUEL_TEMPERATURE,elapsedTicks={},sampleTick={},airC={},available={},lit={}",
                        elapsed, air.airSampleTick(), air.airTemperatureC(), air.airAvailable(), lit);
            }
            if (elapsed != initial + 40) return;
            helper.assertTrue(zeroObserved[0] > 0 && !lit && entity.getItems().stream().allMatch(ItemStack::isEmpty),
                    "Real fuel expires and the ordinary cooking inventory empties");
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(fire).inflate(5))
                            .stream().anyMatch(item -> item.getItem().is(Items.COOKED_BEEF)),
                    "The real cooking recipe must emit cooked beef");
            FHMain.LOGGER.info("THERMAL_CAMPFIRE_FUEL_RESULT,initialFuelTicks={},zeroAtElapsedTicks={},elapsedGameTicks={},cooked=true,comparatorOutput=0",
                    initial, zeroObserved[0], level.getGameTime() - started);
            helper.succeed();
        });
    }

    @GameTest(template = "phase0a_empty", batch = "campfire_disk", timeoutTicks = 1800)
    public static void actualSaveUnloadReloadRetainsRemainingFuel(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos fire = new BlockPos(3079, 81, 4103);
        level.setChunkForced(fire.getX() >> 4, fire.getZ() >> 4, true);
        placeFire(helper, fire, new ItemStack(Items.COAL, 3));
        DiskFuel disk = new DiskFuel(helper, fire);
        observation = disk;
        helper.startSequence().thenIdle(120).thenExecute(() -> {
            level.getServer().saveEverything(true, true, true);
            helper.assertTrue(disk.savedFuel > 0 && disk.saveMatchesLive,
                    "Actual vanilla NBT serialization must write the current live fuel");
            level.setChunkForced(fire.getX() >> 4, fire.getZ() >> 4, false);
        }).thenWaitUntil(() -> helper.assertTrue(disk.unloadedChunk != null
                        && level.getChunkSource().getChunkNow(fire.getX() >> 4, fire.getZ() >> 4) == null,
                "The real fueled campfire chunk must unload"))
                .thenExecute(() -> level.setChunkForced(fire.getX() >> 4, fire.getZ() >> 4, true))
                .thenWaitUntil(() -> helper.assertTrue(disk.loadedFuel >= 0 && disk.loadedGameTick >= 0
                        && level.getChunkSource().getChunkNow(fire.getX() >> 4, fire.getZ() >> 4) != null,
                        "The fueled chunk must actually return through disk loading"))
                .thenExecute(() -> {
                    int live = ((ICampfireExtra) level.getBlockEntity(fire)).getLifeTime();
                    long elapsedLoaded = level.getGameTime() - disk.loadedGameTick;
                    helper.assertTrue(disk.savedFuel == disk.unloadedFuel && disk.loadedFuel == disk.savedFuel,
                            "Last live fuel, actual unload NBT and disk reload must match exactly");
                    helper.assertTrue(Math.abs(live - (disk.loadedFuel - elapsedLoaded)) <= 1,
                            "Fuel resumes on real loaded ticks without burning while absent");
                    helper.assertTrue(level.getBlockState(fire).getValue(CampfireBlock.LIT),
                            "Reloaded real fuel remains lit");
                    FHMain.LOGGER.info("THERMAL_CAMPFIRE_FUEL_DISK,savedFuelTicks={},unloadedFuelTicks={},loadedFuelTicks={},liveFuelTicks={},loadedElapsedTicks={}",
                            disk.savedFuel, disk.unloadedFuel, disk.loadedFuel, live, elapsedLoaded);
                    observation = null;
                    ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
                    level.setChunkForced(fire.getX() >> 4, fire.getZ() >> 4, false);
                }).thenSucceed();
    }

    private static void placeFire(GameTestHelper helper, BlockPos fire, ItemStack fuel) {
        var level = helper.getLevel();
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++)
            level.setBlockAndUpdate(fire.offset(x, -1, z), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        int expected = ForgeHooks.getBurnTime(fuel, RecipeType.CAMPFIRE_COOKING) * 3 * fuel.getCount();
        var handler = level.getBlockEntity(fire).getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(
                () -> new IllegalStateException("Actual campfire fuel capability is missing"));
        helper.assertTrue(handler.insertItem(0, fuel, false).isEmpty(), "Real fuel capability must consume the actual items");
        helper.assertTrue(((ICampfireExtra) level.getBlockEntity(fire)).getLifeTime() == expected,
                "Actual item burn time determines fuel, with the existing multiplier");
        ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
    }

    @SubscribeEvent
    public static void onUnload(ChunkEvent.Unload event) {
        DiskFuel disk = observation;
        if (disk == null || !disk.matches(event)) return;
        disk.unloadedChunk = event.getChunk();
        disk.unloadedFuel = ((ICampfireExtra) event.getChunk().getBlockEntity(disk.fire)).getLifeTime();
    }

    @SubscribeEvent
    public static void onSave(ChunkDataEvent.Save event) {
        DiskFuel disk = observation;
        if (disk == null || !disk.matches(event)) return;
        disk.savedFuel = fuelIn(event.getData(), disk.fire);
        disk.saveMatchesLive = disk.savedFuel == ((ICampfireExtra) event.getChunk().getBlockEntity(disk.fire)).getLifeTime();
    }

    @SubscribeEvent
    public static void onLoad(ChunkDataEvent.Load event) {
        DiskFuel disk = observation;
        if (disk == null || disk.unloadedChunk == null || !disk.matchesPosition(event)) return;
        disk.loadedFuel = fuelIn(event.getData(), disk.fire);
    }

    @SubscribeEvent
    public static void onLoaded(ChunkEvent.Load event) {
        DiskFuel disk = observation;
        if (disk == null || disk.unloadedChunk == null || !disk.matches(event)) return;
        disk.loadedGameTick = disk.helper.getLevel().getGameTime();
    }

    private static int fuelIn(CompoundTag root, BlockPos fire) {
        for (Tag tag : root.getList("block_entities", Tag.TAG_COMPOUND)) {
            CompoundTag entity = (CompoundTag) tag;
            if (entity.getInt("x") == fire.getX() && entity.getInt("y") == fire.getY() && entity.getInt("z") == fire.getZ())
                return entity.getInt("LifeTime");
        }
        return -1;
    }

    private static final class DiskFuel {
        final GameTestHelper helper;
        final BlockPos fire;
        volatile ChunkAccess unloadedChunk;
        int savedFuel = -1, unloadedFuel = -1;
        volatile int loadedFuel = -1;
        volatile long loadedGameTick = -1;
        boolean saveMatchesLive;
        DiskFuel(GameTestHelper helper, BlockPos fire) { this.helper = helper; this.fire = fire; }
        boolean matches(ChunkEvent event) {
            return event.getLevel() == helper.getLevel() && matchesPosition(event);
        }
        boolean matchesPosition(ChunkEvent event) {
            return event.getChunk().getPos().x == fire.getX() >> 4
                    && event.getChunk().getPos().z == fire.getZ() >> 4;
        }
    }
}
