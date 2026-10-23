/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.block;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.content.climate.data.ArmorTempData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPhaseController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_icicle")
@PrefixGameTestTemplate(false)
public final class EaveIcicleGameTests {
    private static final RandomSource YES = new LegacyRandomSource(0) {
        @Override public double nextDouble() { return 0; }
        @Override public int nextInt(int bound) { return 0; }
    };

    @GameTest(template = "phase0a_empty", batch = "eave_geometry", timeoutTicks = 60)
    public static void roofGeometryAndSnowSupply(GameTestHelper helper) {
        var level = helper.getLevel();
        var roof = roof(helper);
        helper.assertTrue(EaveIcicleBlock.isRoof(level, roof), "exposed stone is a supported roof: " + roof
                + " height=" + level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, roof.getX(), roof.getZ())
                + " down=" + level.getBlockState(roof).isFaceSturdy(level, roof, Direction.DOWN));
        helper.assertTrue(EaveIcicleBlock.roofEdges(level, roof) == 15, "isolated platform permits four edges");
        helper.assertTrue(!EaveIcicleBlock.hasWater(level, roof, false), "dry roof has no water supply");
        level.setBlockAndUpdate(roof.above(), Blocks.SNOW.defaultBlockState());
        helper.assertTrue(EaveIcicleBlock.hasWater(level, roof, false), "snow supplies water without current precipitation");
        level.setBlockAndUpdate(roof.above(), Blocks.STONE.defaultBlockState());
        helper.assertTrue(!EaveIcicleBlock.isRoof(level, roof), "covered inner roof is ineligible");
        level.removeBlock(roof.above(), false);
        level.setBlockAndUpdate(roof.north(), Blocks.STONE.defaultBlockState());
        helper.assertTrue((EaveIcicleBlock.roofEdges(level, roof) & 1) == 0, "roof continues north, so no north strip");
        level.setBlockAndUpdate(roof, Blocks.OAK_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE, SlabType.TOP));
        helper.assertTrue(!EaveIcicleBlock.isRoof(level, roof), "upper slab underside is above the grid bottom");
        level.setBlockAndUpdate(roof, Blocks.OAK_SLAB.defaultBlockState());
        helper.assertTrue(EaveIcicleBlock.isRoof(level, roof), "lower slab has a full grid-bottom underside");
        level.setBlockAndUpdate(roof, Blocks.OAK_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.HALF, Half.TOP));
        helper.assertTrue(!EaveIcicleBlock.isRoof(level, roof), "upside-down stairs do not have a full grid-bottom underside");
        level.setBlockAndUpdate(roof, Blocks.OAK_STAIRS.defaultBlockState());
        helper.assertTrue(EaveIcicleBlock.isRoof(level, roof), "ordinary stairs provide a full grid-bottom underside");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "eave_growth", timeoutTicks = 60)
    public static void naturalGrowthUsesRoofSnowAndMaterialTemperature(GameTestHelper helper) {
        var level = helper.getLevel();
        com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.closeActiveLevel(level);
        var roof = roof(helper);
        level.setBlockAndUpdate(roof, Blocks.PACKED_ICE.defaultBlockState());
        level.setBlockAndUpdate(roof.above(), Blocks.SNOW.defaultBlockState());
        IcicleGameTests.seed(helper, roof, -5, MaterialThermalLaw.SENSIBLE);
        EaveIcicleBlock.randomRoofTick(level, roof.above(), false, 3, 20, YES);
        helper.assertTrue(level.getBlockState(roof.below()).is(FHBlocks.EAVE_ICICLES.get()), "snow-covered roof grows outside the ice-cave restriction");
        helper.assertTrue(EaveIcicleBlock.mask(level.getBlockState(roof.below())) == 15, "all exposed edges are stored");
        helper.assertTrue(level.getBlockState(roof.below(2)).isAir(), "no downward extension");
        level.removeBlock(roof.below(), false);
        IcicleGameTests.seed(helper, roof, 1, MaterialThermalLaw.SENSIBLE);
        EaveIcicleBlock.randomRoofTick(level, roof.above(), false, 3, 20, YES);
        helper.assertTrue(level.getBlockState(roof.below()).isAir(), "warm material cannot grow even with snow supply");
        helper.assertTrue(Math.abs(EaveIcicleBlock.surfaceChance(3, 20) - .01875) < 1e-9, "surface admission compensates for sampling rate");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "eave_fall", timeoutTicks = 60)
    public static void vibrationDropsImmediatelyAndHitsOnce(GameTestHelper helper) {
        var level = helper.getLevel();
        var roof = roof(helper);
        level.setBlockAndUpdate(roof.below(), FHBlocks.EAVE_ICICLES.get().withEdges(1));
        IcicleEvents.vibrate(level, Vec3.atCenterOf(roof.below()), IcicleEvents.BELL, YES);
        helper.assertTrue(level.getBlockState(roof.below()).isAir(), "no warning phase before falling");
        var falling = level.getEntitiesOfClass(FallingBlockEntity.class, new AABB(roof.below(2)).inflate(2)).stream()
                .filter(e -> EaveIcicleBlock.isEaveIce(e.getBlockState())).findFirst().orElseThrow();
        var target = EntityType.COW.create(level);
        target.setNoAi(true);
        target.setPos(roof.getX() + .5, roof.getY() - 3, roof.getZ() + .5);
        level.addFreshEntity(target);
        float health = target.getHealth();
        for (int i = 0; i < 20 && !falling.isRemoved(); i++) falling.tick();
        helper.assertTrue(falling.isRemoved(), "falling curtain shatters on entity contact");
        helper.assertTrue(Math.abs(health - target.getHealth() - .5F) < 1e-5, "one fixed half-point hit independent of fall distance");
        target.discard();
        level.setBlockAndUpdate(roof.below(), FHBlocks.EAVE_ICICLES.get().defaultBlockState());
        level.removeBlock(roof, false);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(level.getBlockState(roof.below()).isAir(), "loss of support also drops the curtain");
            level.getEntitiesOfClass(FallingBlockEntity.class, new AABB(roof.below(5)).inflate(4)).forEach(FallingBlockEntity::discard);
            helper.succeed();
        });
    }

    @GameTest(template = "phase0a_empty", batch = "eave_melt_loot", timeoutTicks = 60)
    public static void meltingAndSilkLootUseOrdinaryPhaseRules(GameTestHelper helper) {
        var level = helper.getLevel();
        com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.closeActiveLevel(level);
        var pos = roof(helper).below();
        var block = FHBlocks.EAVE_ICICLES.get();
        level.setBlockAndUpdate(pos, block.withEdges(3));
        helper.assertTrue(block.defaultBlockState().isRandomlyTicking(), "heat fracture receives random ticks");
        var law = com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles.materialLaw(level.getBlockState(pos));
        helper.assertTrue(Math.abs(law.enthalpyAtTemperature(0) - com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles.materialLaw(FHBlocks.ICICLE.get().defaultBlockState()).enthalpyAtTemperature(0)) < 1e-5,
                "same data-owned material budget as icicle");
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        helper.assertTrue(Block.getDrops(level.getBlockState(pos), level, pos, null, null, tool).isEmpty(), "ordinary break has no drops");
        tool.enchant(Enchantments.SILK_TOUCH, 1);
        helper.assertTrue(Block.getDrops(level.getBlockState(pos), level, pos, null, null, tool).size() == 1, "silk collects one curtain");
        IcicleGameTests.seed(helper, pos, 1, MaterialThermalLaw.SENSIBLE);
        helper.assertTrue(!IceDripping.melting(level, pos), "below 2 C is dry");
        IcicleGameTests.seed(helper, pos, 2, MaterialThermalLaw.SENSIBLE);
        helper.assertTrue(IceDripping.melting(level, pos), "2 C already permits drips, no latent-progress gate");
        helper.assertTrue(MinecraftPhaseController.applyDormantTransition(level, pos, level.getBlockState(pos),
                law.heating(), law.heating().targetEnthalpyJ(), level.getGameTime()), "completed budget uses the ordinary phase result");
        helper.assertTrue(level.getBlockState(pos).isAir(), "single curtain melts itself instead of redirecting to another block");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "ice_drip_upper", timeoutTicks = 60)
    public static void warmUpperSegmentDrainsThroughColdTerminal(GameTestHelper helper) {
        var level = helper.getLevel();
        com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.closeActiveLevel(level);
        var root = roof(helper).below();
        level.setBlockAndUpdate(root, FHBlocks.ICICLE.get().defaultBlockState());
        level.setBlockAndUpdate(root.below(), FHBlocks.ICICLE.get().defaultBlockState());
        IcicleGameTests.seed(helper, root, 2, MaterialThermalLaw.SENSIBLE);
        IcicleGameTests.seed(helper, root.below(), -5, MaterialThermalLaw.SENSIBLE);
        helper.assertTrue(IceDripping.melting(level, root.below()), "a warm upper segment supplies the single cold terminal outlet");
        IcicleGameTests.seed(helper, root, 1, MaterialThermalLaw.SENSIBLE);
        helper.assertTrue(!IceDripping.melting(level, root.below()), "entirely cold chain does not drip");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "ice_drip_hits", timeoutTicks = 60)
    public static void dripRangeObstructionAndHeadProtection(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(2, 22, 2));
        var player = helper.makeMockSurvivalPlayer();
        Vec3 old = player.position();
        var helmet = player.getItemBySlot(EquipmentSlot.HEAD).copy();
        try {
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            player.removeEffect(FHMobEffects.WET.get());
            IceDripping.wet(player);
            helper.assertTrue(player.getEffect(FHMobEffects.WET.get()).getDuration() == 100, "bare head gets five seconds");
            player.addEffect(new MobEffectInstance(FHMobEffects.WET.get(), 300, 0));
            IceDripping.wet(player);
            helper.assertTrue(player.getEffect(FHMobEffects.WET.get()).getDuration() == 300, "longer wetness is preserved");
            player.removeEffect(FHMobEffects.WET.get());
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            helper.assertTrue(IceDripping.waterproofHead(player), "any ordinary helmet blocks ice drips");
            IceDripping.wet(player);
            helper.assertTrue(!player.hasEffect(FHMobEffects.WET.get()), "helmet prevents new wetness");
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
            helper.assertTrue(!IceDripping.waterproofHead(player), "arbitrary head-slot item is not a helmet");
            helper.assertTrue(IceDripping.warmEnough(2) && !IceDripping.warmEnough(1.999) && !IceDripping.warmEnough(Double.NaN), "inclusive material threshold");
            var cache = ArmorTempData.cacheList;
            try {
                var data = new com.teammoeg.chorda.util.struct.EnumDefaultedMap<BodyPart, ArmorTempData>(BodyPart.class);
                data.put(BodyPart.HEAD, new ArmorTempData(Items.PAPER, java.util.Optional.of(BodyPart.HEAD), 0, 0, .001F, 0));
                ArmorTempData.cacheList = new java.util.HashMap<>(cache);
                ArmorTempData.cacheList.put(Items.PAPER, data);
                var capability = PlayerTemperatureData.getCapability(player).orElseThrow(IllegalStateException::new);
                player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                capability.setClothesByPart(BodyPart.HEAD, 0, new ItemStack(Items.PAPER));
                helper.assertTrue(IceDripping.waterproofHead(player), "any positive resistance on a worn head garment blocks drips");
                capability.clearClothesByPart(BodyPart.HEAD, 0);
            } finally { ArmorTempData.cacheList = cache; }
            for (var pos : BlockPos.betweenClosed(origin.below(20), origin.above())) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            Vec3 start = Vec3.atCenterOf(origin);
            helper.assertTrue(Math.abs(start.y - IceDripping.endOfDrop(level, start).y - 16) < 1e-6, "unobstructed drop ends after exactly sixteen blocks");
            level.setBlockAndUpdate(origin.below(4), Blocks.STONE.defaultBlockState());
            helper.assertTrue(Math.abs(IceDripping.endOfDrop(level, start).y - (origin.getY() - 3)) < 1e-6, "platform blocks wetness below it");
        } finally {
            player.removeEffect(FHMobEffects.WET.get());
            player.setItemSlot(EquipmentSlot.HEAD, helmet);
            player.setPos(old.x, old.y, old.z);
        }
        helper.succeed();
    }

    private static BlockPos roof(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos roof = helper.absolutePos(new BlockPos(3, 24, 3));
        for (var pos : BlockPos.betweenClosed(roof.offset(-2, -6, -2), roof.offset(2, 3, 2)))
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        int sky = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, roof.getX(), roof.getZ());
        com.teammoeg.frostedheart.FHMain.LOGGER.info("EAVE_FIXTURE roof={}, initial surface={}", roof, sky);
        // The empty template clears only twenty blocks; terrain can remain above it.
        for (int y = roof.getY() + 4; y < sky; y++) level.setBlockAndUpdate(new BlockPos(roof.getX(), y, roof.getZ()), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(roof, Blocks.STONE.defaultBlockState());
        return roof;
    }
}
