/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.concurrent.locks.LockSupport;

/** World actions and public observations only; never owns an engine or writes thermal state. */
@GameTestHolder("frostedheart_production")
@PrefixGameTestTemplate(false)
public final class ContinuousAirProductionGameTests {
    private ContinuousAirProductionGameTests() {}

    private static boolean pacingRegistered;
    private static long nextTickNanos;

    /** GameTest otherwise fast-forwards world time while the production worker uses real CPU time. */
    private static void useServerTickRate() {
        if (pacingRegistered) return;
        pacingRegistered = true;
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase != TickEvent.Phase.START) return;
            long remaining;
            while ((remaining = nextTickNanos - System.nanoTime()) > 0) LockSupport.parkNanos(remaining);
            nextTickNanos = System.nanoTime() + 50_000_000L;
        });
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 1300)
    public static void campfireHeatsPublishedAirAndRetainsHeatAfterExtinguishing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        BlockPos fire = room(helper, origin);
        useItem(helper, fire, Items.FLINT_AND_STEEL);
        helper.assertTrue(level.getBlockState(fire).getValue(CampfireBlock.LIT),
                "Flint and steel ignites the actual campfire");

        ThermalEnvironmentSample sample = new ThermalEnvironmentSample();
        double[] peakRiseC = {Double.NEGATIVE_INFINITY};
        int[] maximumBasisTerms = {0};
        long startedTick = level.getGameTime();
        for (int delay = 20; delay <= 1200; delay += 20) {
            int elapsedTicks = delay;
            helper.runAfterDelay(delay, () -> {
                BlockPos near = fire.above();
                double naturalC = WorldTemperature.naturalAir(level, near);
                MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, near, naturalC, sample);
                double nearC = sample.airAvailable() ? sample.airTemperatureC() : Double.NaN;
                double farC = MinecraftThermalInput.gameplayPassiveEnvironment(
                        level, near.east(4), WorldTemperature.naturalAir(level, near.east(4)));
                double materialC = WorldTemperature.material(level, fire.below());
                // Page initialization samples its section baseline, which need not equal the
                // point-local natural formula. Compare actual Air at the same height instead.
                double riseC = nearC - farC;
                if (elapsedTicks <= 600 && sample.airAvailable() && sample.airBasisTerms() > 0) {
                    peakRiseC[0] = Math.max(peakRiseC[0], riseC);
                    maximumBasisTerms[0] = Math.max(maximumBasisTerms[0], sample.airBasisTerms());
                }
                FHMain.LOGGER.info(
                        "THERMAL_PRODUCTION,elapsedTicks={},worldTick={},naturalC={},nearC={},farC={},materialC={},available={},basisTerms={},sampleTick={},lit={}",
                        level.getGameTime() - startedTick, level.getGameTime(), naturalC,
                        nearC, farC, materialC, sample.airAvailable(), sample.airBasisTerms(), sample.airSampleTick(),
                        level.getBlockState(fire).getValue(CampfireBlock.LIT));
                if (elapsedTicks == 600) {
                    if (MinecraftThermalProfiles.prepare().tuning().continuousAir()) {
                        helper.assertTrue(maximumBasisTerms[0] > 8, "Actual published Air includes near-source modes; terms=" + maximumBasisTerms[0]);
                    }
                    helper.assertTrue(peakRiseC[0] > 0.05,
                            "Real campfire must heat published Air; peak rise=" + peakRiseC[0]);
                    helper.assertTrue(Double.isFinite(materialC), "Real floor material is published");
                    useItem(helper, fire, Items.IRON_SHOVEL);
                    helper.assertTrue(!level.getBlockState(fire).getValue(CampfireBlock.LIT),
                            "Shovel extinguishes the actual source");
                }
                if (elapsedTicks == 640) {
                    helper.assertTrue(sample.airAvailable() && riseC > 0.005,
                            "Extinguishing retains actual Air heat; rise=" + riseC);
                }
                if (elapsedTicks == 1200) {
                    helper.assertTrue(!level.getBlockState(fire).getValue(CampfireBlock.LIT),
                            "Source stays off throughout the cooling observation");
                    helper.succeed();
                }
            });
        }
    }

    @GameTest(template = "phase0a_empty", batch = "thermal_spatial_production", timeoutTicks = 1800)
    public static void publishedFieldHasDetailInsideBrickAndContinuityAcrossItsFace(GameTestHelper helper) {
        BlockPos fire = room(helper, helper.absolutePos(BlockPos.ZERO));
        useItem(helper, fire, Items.FLINT_AND_STEEL);
        ThermalEnvironmentSample near = new ThermalEnvironmentSample(), far = new ThermalEnvironmentSample();
        ThermalEnvironmentSample left = new ThermalEnvironmentSample(), right = new ThermalEnvironmentSample();
        helper.startSequence().thenIdle(400).thenWaitUntil(() -> {
            double y = fire.getY() + 1.5, z = fire.getZ() + 0.5;
            int brickX = fire.getX() & ~3;
            double fartherX = (fire.getX() & 3) < 2 ? brickX + 3.5 : brickX + 0.5;
            sampleAt(helper.getLevel(), fire.getX() + 0.5, y, z, near);
            sampleAt(helper.getLevel(), fartherX, y, z, far);
            helper.assertTrue(near.airBasisTerms() > 8 && far.airBasisTerms() >= 8,
                    "Both actual player samples must read a published continuous field");
            helper.assertTrue(near.airSampleTick() == far.airSampleTick(), "Compare positions in the same actual cut");
            helper.assertTrue(near.airTemperatureC() - far.airTemperatureC() > 0.01,
                    "One Brick must retain near-source detail: " + near.airTemperatureC() + " vs " + far.airTemperatureC());
            sampleAt(helper.getLevel(), brickX + 4 - 0.0001, y, z, left);
            sampleAt(helper.getLevel(), brickX + 4 + 0.0001, y, z, right);
            helper.assertTrue(left.airBasisTerms() >= 8 && right.airBasisTerms() >= 8
                    && left.airSampleTick() == right.airSampleTick(), "Both sides of the open face are current");
            helper.assertTrue(Math.abs(left.airTemperatureC() - right.airTemperatureC()) < 0.01,
                    "Actual player movement across a Brick face must not switch temperature: "
                            + left.airTemperatureC() + " vs " + right.airTemperatureC());
        }).thenExecute(() -> {
            useItem(helper, fire, Items.IRON_SHOVEL);
            FHMain.LOGGER.info("THERMAL_PRODUCTION_SPATIAL,nearC={},sameBrickFarC={},faceLeftC={},faceRightC={}",
                    near.airTemperatureC(), far.airTemperatureC(), left.airTemperatureC(), right.airTemperatureC());
        }).thenSucceed();
    }

    @GameTest(template = "phase0a_empty", batch = "thermal_save_production", timeoutTicks = 5000)
    public static void actualChunkUnloadAndReloadKeepSpatialAir(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = new BlockPos(1024, 80, 1024);
        BlockPos fire = origin.offset(8, 1, 8);
        int chunkX = fire.getX() >> 4, chunkZ = fire.getZ() >> 4;
        level.setChunkForced(chunkX, chunkZ, true);
        room(helper, origin);
        useItem(helper, fire, Items.FLINT_AND_STEEL);
        ThermalEnvironmentSample sample = new ThermalEnvironmentSample();
        double[] before = new double[2];
        helper.startSequence().thenIdle(600).thenWaitUntil(() -> {
            sampleAt(level, fire.getX() + 0.5, fire.getY() + 1.5, fire.getZ() + 0.5, sample);
            helper.assertTrue(sample.airBasisTerms() > 8, "Source room has actual local modes before saving");
        }).thenExecute(() -> useItem(helper, fire, Items.IRON_SHOVEL)).thenIdle(60).thenWaitUntil(() -> {
            sampleAt(level, fire.getX() + 0.5, fire.getY() + 1.5, fire.getZ() + 0.5, sample);
            helper.assertTrue(sample.airBasisTerms() > 8, "Extinguished field is republished before saving");
        }).thenExecute(() -> {
            level.getServer().saveEverything(true, true, true);
            before[0] = MinecraftThermalInput.gameplayPassiveEnvironment(level, fire.above(), WorldTemperature.naturalAir(level, fire.above()));
            before[1] = MinecraftThermalInput.gameplayPassiveEnvironment(level, fire.above().east(2), WorldTemperature.naturalAir(level, fire.above().east(2)));
            level.setChunkForced(chunkX, chunkZ, false);
        }).thenWaitUntil(() -> {
            helper.assertTrue(level.getChunkSource().getChunkNow(chunkX, chunkZ) == null, "The real chunk must unload");
        }).thenExecute(() -> level.setChunkForced(chunkX, chunkZ, true)).thenWaitUntil(() -> {
            helper.assertTrue(level.getChunkSource().getChunkNow(chunkX, chunkZ) != null, "The saved chunk must actually reload");
        }).thenExecute(() -> {
            double near = MinecraftThermalInput.gameplayPassiveEnvironment(level, fire.above(), WorldTemperature.naturalAir(level, fire.above()));
            double far = MinecraftThermalInput.gameplayPassiveEnvironment(level, fire.above().east(2), WorldTemperature.naturalAir(level, fire.above().east(2)));
            helper.assertTrue(Math.abs(near - before[0]) < 0.25 && Math.abs(far - before[1]) < 0.25,
                    "Reload must retain spatial history: before=" + before[0] + "," + before[1] + " after=" + near + "," + far);
            helper.assertTrue(near > far + 0.01, "Reload does not collapse near and far points to a Brick mean");
            useItem(helper, fire, Items.FLINT_AND_STEEL);
        }).thenWaitUntil(() -> {
            sampleAt(level, fire.getX() + 0.5, fire.getY() + 1.5, fire.getZ() + 0.5, sample);
            helper.assertTrue(sample.airBasisTerms() > 8, "Normal source discovery restores the spatial solver after reload");
        }).thenExecute(() -> {
            useItem(helper, fire, Items.IRON_SHOVEL);
            level.setChunkForced(chunkX, chunkZ, false);
            FHMain.LOGGER.info("THERMAL_PRODUCTION_RELOAD,beforeNearC={},beforeFarC={},restoredNearC={}", before[0], before[1], sample.airTemperatureC());
        }).thenSucceed();
    }

    private static BlockPos room(GameTestHelper helper, BlockPos origin) {
        useServerTickRate();
        ServerLevel level = helper.getLevel();
        for (int y = 0; y <= 8; y++) for (int z = 0; z <= 16; z++) for (int x = 0; x <= 16; x++) {
            boolean wall = x == 0 || x == 16 || z == 0 || z == 16 || y == 0 || y == 8;
            level.setBlockAndUpdate(origin.offset(x, y, z), (wall ? Blocks.STONE : Blocks.AIR).defaultBlockState());
        }
        BlockPos fire = origin.offset(8, 1, 8);
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        var fuel = level.getBlockEntity(fire).getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(
                () -> new IllegalStateException("Campfire must accept fuel through its production capability"));
        helper.assertTrue(fuel.insertItem(0, new ItemStack(Items.COAL, 4), false).isEmpty(), "Campfire accepts actual coal fuel");
        return fire;
    }

    @GameTest(template = "phase0a_empty", batch = "thermal_route_production", timeoutTicks = 2200)
    public static void realSlabOutletRebindsWhenRemoved(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fire = room(helper, helper.absolutePos(BlockPos.ZERO));
        level.setBlockAndUpdate(fire.above(), Blocks.STONE_SLAB.defaultBlockState());
        useItem(helper, fire, Items.FLINT_AND_STEEL);
        ThermalEnvironmentSample near = new ThermalEnvironmentSample();
        ThermalEnvironmentSample far = new ThermalEnvironmentSample();
        double[] routedC = {Double.NaN};
        helper.startSequence().thenIdle(600).thenWaitUntil(() -> {
            sampleAt(level, fire.getX() + 0.5, fire.getY() + 2.5, fire.getZ() + 0.5, near);
            sampleAt(level, fire.getX() + 4.5, fire.getY() + 2.5, fire.getZ() + 0.5, far);
            helper.assertTrue(near.airBasisTerms() > 8 && far.airBasisTerms() >= 8
                    && near.airSampleTick() == far.airSampleTick(), "Actual slab passage supplies a published local Air field");
            helper.assertTrue(near.airTemperatureC() > far.airTemperatureC() + 0.01,
                    "Real campfire heat leaves the ventilated slab through actual Air outlets");
            routedC[0] = near.airTemperatureC();
        }).thenExecute(() -> level.setBlockAndUpdate(fire.above(), Blocks.AIR.defaultBlockState()))
                .thenIdle(200).thenWaitUntil(() -> {
            sampleAt(level, fire.getX() + 0.5, fire.getY() + 1.5, fire.getZ() + 0.5, near);
            sampleAt(level, fire.getX() + 4.5, fire.getY() + 1.5, fire.getZ() + 0.5, far);
            helper.assertTrue(near.airBasisTerms() > 8 && far.airBasisTerms() >= 8
                    && near.airSampleTick() == far.airSampleTick(), "Removing the slab republishes the direct source face");
            helper.assertTrue(near.airTemperatureC() > far.airTemperatureC() + 0.01,
                    "Actual source rebinding keeps heating after the world geometry changes");
        }).thenExecute(() -> {
            useItem(helper, fire, Items.IRON_SHOVEL);
            FHMain.LOGGER.info("THERMAL_PRODUCTION_ROUTE,routedC={},directC={},farC={}",
                    routedC[0], near.airTemperatureC(), far.airTemperatureC());
        }).thenSucceed();
    }

    @GameTest(template = "phase0a_empty", batch = "thermal_material_production", timeoutTicks = 24000)
    public static void realCampfiresDriveMaterialEnthalpyThroughWorldPhaseChange(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = room(helper, helper.absolutePos(BlockPos.ZERO));
        BlockPos[] fires = {center.north(2), center.south(2), center.east(2), center.west(2)};
        for (BlockPos fire : fires) {
            level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
            level.getBlockEntity(fire).getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(
                    () -> new IllegalStateException("Campfire fuel capability is missing")).insertItem(0, new ItemStack(Items.COAL, 4), false);
            useItem(helper, fire, Items.FLINT_AND_STEEL);
        }
        BlockPos material = center.above(2);
        level.setBlockAndUpdate(material.below(), Blocks.STONE.defaultBlockState());
        MaterialSample sample = new MaterialSample();
        double[] initialH = {Double.NaN};
        long[] started = {0};
        helper.runAfterDelay(10000, () -> {
            for (BlockPos fire : fires) {
                var entity = level.getBlockEntity(fire);
                if (entity != null) entity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(
                        fuel -> fuel.insertItem(0, new ItemStack(Items.COAL, 4), false));
            }
        });
        helper.startSequence().thenIdle(300).thenExecute(() -> {
            level.setBlockAndUpdate(material, Blocks.POWDER_SNOW.defaultBlockState());
            started[0] = level.getGameTime();
        }).thenWaitUntil(() -> {
            helper.assertTrue(MinecraftThermalInput.sampleMaterial(level, material, sample)
                    && sample.source() == MaterialSample.Source.LIVE,
                    "Placed powder snow enters the actual material publication");
            initialH[0] = sample.enthalpyJ();
        }).thenWaitUntil(() -> {
            helper.assertTrue(MinecraftThermalInput.sampleMaterial(level, material, sample)
                    && sample.source() == MaterialSample.Source.LIVE
                    && sample.enthalpyJ() > initialH[0] + 10,
                    "Actual heat exchange must increase stored material H before conversion");
        }).thenWaitUntil(() -> {
            helper.assertTrue(level.getBlockState(material).isAir(), "Actual phase controller eventually melts powder snow into Air");
        }).thenExecute(() -> {
            for (BlockPos fire : fires) if (level.getBlockState(fire).getValue(CampfireBlock.LIT)) useItem(helper, fire, Items.IRON_SHOVEL);
            FHMain.LOGGER.info("THERMAL_PRODUCTION_PHASE,initialH={},conversionTicks={}", initialH[0], level.getGameTime() - started[0]);
        }).thenSucceed();
    }

    private static void sampleAt(ServerLevel level, double x, double y, double z, ThermalEnvironmentSample out) {
        var player = FakePlayerFactory.getMinecraft(level);
        Vec3 previous = player.position();
        try {
            player.setPos(x, y - player.getEyeHeight(), z);
            MinecraftThermalInput.gameplayPlayerEnvironment(player,
                    WorldTemperature.naturalAir(level, BlockPos.containing(x, y, z)), out);
        } finally {
            player.setPos(previous);
        }
    }

    private static void useItem(GameTestHelper helper, BlockPos position, net.minecraft.world.item.Item item) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        ItemStack previous = player.getMainHandItem();
        ItemStack held = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        try {
            var hit = new BlockHitResult(Vec3.atCenterOf(position), Direction.UP, position, false);
            helper.assertTrue(held.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(),
                    "Production item interaction must succeed: " + item);
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, previous);
        }
    }
}
