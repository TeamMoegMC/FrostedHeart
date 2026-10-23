/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.block;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_icicle_vibration")
@PrefixGameTestTemplate(false)
public final class IcicleVibrationGameTests {
    @GameTest(template = "phase0a_empty", batch = "vibration_distance", timeoutTicks = 40)
    public static void distanceAttenuatesBothStatesAndStopsAtBoundary(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(1, 18, 1));
        var half = center.offset(6, 0, 0);
        var edge = center.offset(12, 0, 0);
        intact(level, center);
        intact(level, half);
        intact(level, edge);
        IcicleEvents.vibrate(level, Vec3.atCenterOf(center), IcicleEvents.BELL, fixed(.006));
        helper.assertTrue(cracked(level, center) && !cracked(level, half) && !cracked(level, edge),
                "1% near the source attenuates to 0.5% halfway and zero at the edge");
        IcicleEvents.vibrate(level, Vec3.atCenterOf(center), IcicleEvents.BELL, fixed(.004));
        helper.assertTrue(cracked(level, half) && !cracked(level, edge), "halfway can still warn");
        IcicleEvents.vibrate(level, Vec3.atCenterOf(center), IcicleEvents.BELL, fixed(.3));
        helper.assertTrue(level.getBlockState(center).isAir() && cracked(level, half),
                "50% immediate shatter likewise attenuates to 25% halfway");
        IcicleEvents.vibrate(level, Vec3.atCenterOf(center), IcicleEvents.BELL, fixed(0));
        helper.assertTrue(!cracked(level, edge), "even a zero roll cannot trigger the boundary");
        level.removeBlock(half, false);
        level.removeBlock(edge, false);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "vibration_profiles", timeoutTicks = 40)
    public static void explosionAndFireworkKeepDifferentIntactChances(GameTestHelper helper) {
        var level = helper.getLevel();
        var root = helper.absolutePos(new BlockPos(1, 18, 1));
        intact(level, root);
        IcicleEvents.vibrate(level, Vec3.atCenterOf(root), IcicleEvents.EXPLOSION, fixed(.09));
        helper.assertTrue(cracked(level, root), "explosion intact chance is 10% and only warns once");
        IcicleEvents.vibrate(level, Vec3.atCenterOf(root), IcicleEvents.EXPLOSION, fixed(.999));
        helper.assertTrue(level.getBlockState(root).isAir(), "explosion shatters a warned source at 100%");
        intact(level, root);
        IcicleEvents.vibrate(level, Vec3.atCenterOf(root), IcicleEvents.FIREWORK, fixed(.03));
        helper.assertTrue(!cracked(level, root), "firework does not inherit explosion's 10%");
        IcicleEvents.vibrate(level, Vec3.atCenterOf(root), IcicleEvents.FIREWORK, fixed(.019));
        helper.assertTrue(cracked(level, root), "firework intact chance is 2%");
        IcicleEvents.vibrate(level, Vec3.atCenterOf(root), IcicleEvents.FIREWORK, fixed(.999));
        helper.assertTrue(level.getBlockState(root).isAir(), "firework shatter chance is 100% at source");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "vibration_breakeffects", timeoutTicks = 40)
    public static void actualBreakEffectsClassifyIceStoneOreAndIgnoreOtherBlocks(GameTestHelper helper) {
        var level = helper.getLevel();
        var root = helper.absolutePos(new BlockPos(1, 18, 1));
        var source = root.offset(2, 0, 0);
        for (var block : new Block[]{Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE, FHBlocks.THIN_ICE_BLOCK.get()}) {
            intact(level, root);
            level.random.setSeed(seedFor(.002, .003));
            level.levelEvent(2001, source, Block.getId(block.defaultBlockState()));
            helper.assertTrue(cracked(level, root), "ice break effect reaches the server vibration hook: " + block);
        }
        for (var block : new Block[]{Blocks.STONE, Blocks.DEEPSLATE, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE}) {
            intact(level, root);
            level.random.setSeed(seedFor(.002, .003));
            level.levelEvent(2001, source, Block.getId(block.defaultBlockState()));
            helper.assertTrue(!cracked(level, root), "stone does not use the stronger ice chance");
            level.random.setSeed(seedFor(.0005, .001));
            level.levelEvent(2001, source, Block.getId(block.defaultBlockState()));
            helper.assertTrue(cracked(level, root), "stone/ore break effect triggers: " + block);
        }
        for (BlockState state : new BlockState[]{Blocks.GLASS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(),
                FHBlocks.ICICLE.get().defaultBlockState(), FHBlocks.EAVE_ICICLES.get().defaultBlockState()}) {
            intact(level, root);
            FHBlocks.ICICLE.get().warn(level, root, fixed(0));
            level.random.setSeed(seedFor(0, .001));
            level.levelEvent(2001, source, Block.getId(state));
            helper.assertTrue(cracked(level, root), "glass, wood and hanging ice cannot retrigger shattering");
        }
        level.removeBlock(root, false);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "vibration_bellanvil", timeoutTicks = 40)
    public static void actualBellAndAnvilEffectsUseTheirOwnProfile(GameTestHelper helper) {
        var level = helper.getLevel();
        var root = helper.absolutePos(new BlockPos(1, 18, 1));
        var source = root.offset(2, 0, 0);
        for (int effect : new int[]{1029, 1030, 1031}) {
            intact(level, root);
            level.random.setSeed(seedFor(.001, .002));
            level.levelEvent(effect, source, 0);
            helper.assertTrue(cracked(level, root), "anvil break/use/land effect is connected");
        }
        intact(level, root);
        level.setBlockAndUpdate(source, Blocks.BELL.defaultBlockState());
        level.random.setSeed(seedFor(.001, .002));
        level.playSound(null, source, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2, 1);
        helper.assertTrue(cracked(level, root), "bell server sound reaches its existing hook");
        level.removeBlock(root, false);
        level.removeBlock(source, false);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "vibration_fireworkburst", timeoutTicks = 40)
    public static void fireworkBurstRequiresStarsAndUsesTwentyFourBlocks(GameTestHelper helper) {
        var level = helper.getLevel();
        var root = helper.absolutePos(new BlockPos(1, 18, 1));
        var source = Vec3.atCenterOf(root).add(20, 0, 0);
        intact(level, root);
        var empty = new FireworkRocketEntity(level, source.x, source.y, source.z, new ItemStack(Items.FIREWORK_ROCKET));
        level.random.setSeed(seedFor(0, .001));
        level.broadcastEntityEvent(empty, (byte) 17);
        helper.assertTrue(!cracked(level, root), "starless rockets have no burst sound and do not vibrate");
        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
        ListTag explosions = new ListTag();
        explosions.add(new CompoundTag());
        stack.getOrCreateTagElement("Fireworks").put("Explosions", explosions);
        var rocket = new FireworkRocketEntity(level, source.x, source.y, source.z, stack);
        level.random.setSeed(seedFor(0, .001));
        level.broadcastEntityEvent(rocket, (byte) 17);
        helper.assertTrue(cracked(level, root), "real burst packet reaches ice beyond 16 blocks");
        level.random.setSeed(seedFor(0, .001));
        level.broadcastEntityEvent(rocket, (byte) 17);
        helper.assertTrue(level.getBlockState(root).isAir(), "later burst can shatter a warned segment");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "vibration_eave", timeoutTicks = 40)
    public static void eaveIceUsesIntactChanceAndFallsWithoutWarning(GameTestHelper helper) {
        var level = helper.getLevel();
        var root = helper.absolutePos(new BlockPos(1, 18, 1));
        level.setBlockAndUpdate(root.above(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(root, FHBlocks.EAVE_ICICLES.get().defaultBlockState());
        IcicleEvents.vibrate(level, Vec3.atCenterOf(root), IcicleEvents.BELL, fixed(.02));
        helper.assertTrue(EaveIcicleBlock.isEaveIce(level.getBlockState(root)), "eave ice uses 1%, not 50%");
        IcicleEvents.vibrate(level, Vec3.atCenterOf(root), IcicleEvents.BELL, fixed(.009));
        helper.assertTrue(level.getBlockState(root).isAir(), "successful eave roll falls immediately");
        helper.succeed();
    }

    private static void intact(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos.above(), Blocks.PACKED_ICE.defaultBlockState());
        level.setBlockAndUpdate(pos, FHBlocks.ICICLE.get().defaultBlockState());
    }

    private static boolean cracked(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return IcicleBlock.isIcicle(state) && state.getValue(IcicleBlock.CRACKED);
    }

    private static LegacyRandomSource fixed(double roll) {
        return new LegacyRandomSource(0) {
            @Override public double nextDouble() { return roll; }
        };
    }

    private static long seedFor(double min, double max) {
        for (long seed = 0; ; seed++) {
            double value = new LegacyRandomSource(seed).nextDouble();
            if (value >= min && value < max) return seed;
        }
    }
}
