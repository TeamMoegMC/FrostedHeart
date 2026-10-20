/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.block;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MaterialSectionState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MinecraftThermalChunkAttachment;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ThermalMaterialBehaviorGameTests;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPhaseController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

@GameTestHolder("frostedheart_icicle")
@PrefixGameTestTemplate(false)
public final class IcicleGameTests {
    private static final RandomSource YES = new LegacyRandomSource(0) {
        @Override public double nextDouble() { return 0; }
        @Override public int nextInt(int bound) { return bound - 1; }
        @Override public double nextGaussian() { return 0; }
    };

    @GameTest(template = "phase0a_empty", batch = "icicle_breakingmiddleretainsupperanddropsunboundedlower", timeoutTicks = 50)
    public static void breakingMiddleRetainsUpperAndDropsUnboundedLower(GameTestHelper helper) {
        var level = helper.getLevel();
        var root = helper.absolutePos(new BlockPos(1, 22, 1));
        chain(level, root, 12);
        var block = FHBlocks.ICICLE.get();
        helper.assertTrue(level.getBlockState(root).getValue(IcicleBlock.SHAPE) == IcicleBlock.ROOT, "root shape");
        helper.assertTrue(level.getBlockState(root.below(10)).getValue(IcicleBlock.SHAPE) == IcicleBlock.TAIL, "tail shape");
        level.destroyBlock(root.below(2), false);
        helper.runAfterDelay(2, () -> {
            for (int i = 0; i < 2; i++) helper.assertTrue(level.getBlockState(root.below(i)).is(block), "upper remains");
            for (int i = 2; i < 12; i++) helper.assertTrue(level.getBlockState(root.below(i)).isAir(), "lower falls including past eight");
            var falling = level.getEntitiesOfClass(FallingBlockEntity.class, new AABB(root.below(15)).expandTowards(1, 20, 1));
            helper.assertTrue(falling.size() == 9, "one entity per lower segment, no duplication");
            helper.assertTrue(falling.stream().anyMatch(e -> e.getBlockState().getValue(IcicleBlock.SHAPE) == IcicleBlock.MIDDLE), "falling shapes retain the original chain");
            falling.forEach(FallingBlockEntity::discard);
            level.removeBlock(root.above(), false);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(level.getBlockState(root).isAir() && level.getBlockState(root.below()).isAir(), "support loss drops all remaining segments");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "phase0a_empty", batch = "icicle_warningtimerssurviveserializationandreplacement", timeoutTicks = 50)
    public static void warningTimersSurviveSerializationAndReplacement(GameTestHelper helper) {
        var level = helper.getLevel();
        var root = helper.absolutePos(new BlockPos(1, 8, 1));
        chain(level, root, 2);
        var ice = FHBlocks.ICICLE.get();
        ice.warn(level, root, YES);
        long now = level.getGameTime();
        var ticks = (LevelChunkTicks<Block>) level.getChunkAt(root).getBlockTicks();
        var original = ticks.getAll().filter(t -> t.pos().equals(root)).findFirst().orElseThrow();
        helper.assertTrue(original.triggerTick() - now == 640, "warning delay is 4..32 seconds inclusive");
        var nbt = ticks.save(now, b -> BuiltInRegistries.BLOCK.getKey(b).toString());
        LevelChunkTicks<Block> restored = LevelChunkTicks.load(nbt,
                id -> Optional.ofNullable(BuiltInRegistries.BLOCK.get(new ResourceLocation(id))), level.getChunkAt(root).getPos());
        restored.unpack(now + 10_000);
        helper.assertTrue(restored.getAll().filter(t -> t.pos().equals(root)).findFirst().orElseThrow().triggerTick() - now - 10_000 == 640,
                "unload pauses remaining time without an ice block entity");
        level.removeBlock(root.above(), false);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(level.getBlockState(root).isAir(), "support loss supersedes the long countdown");
            chain(level, root, 1);
            helper.assertTrue(!level.getBlockTicks().hasScheduledTick(root, ice), "replacement cannot inherit a stale countdown");
            ice.warn(level, root, YES);
            helper.assertTrue(((LevelChunkTicks<Block>) level.getChunkAt(root).getBlockTicks()).getAll().filter(t -> t.pos().equals(root)).findFirst().orElseThrow().triggerTick()
                    - level.getGameTime() == 640, "replacement gets a fresh full warning");
            helper.succeed();
        });
    }

    @GameTest(template = "phase0a_empty", batch = "icicle_vibrationwarnsfirstandrespectssphericalrange", timeoutTicks = 40)
    public static void vibrationWarnsFirstAndRespectsSphericalRange(GameTestHelper helper) {
        var level = helper.getLevel();
        var root = helper.absolutePos(new BlockPos(1, 18, 1));
        var far = root.offset(12, 0, 12);
        chain(level, root, 2);
        chain(level, far, 1);
        Vec3 center = Vec3.atCenterOf(root);
        IcicleEvents.vibrate(level, center, 12, YES);
        helper.assertTrue(level.getBlockState(root).getValue(IcicleBlock.CRACKED)
                && level.getBlockState(root.below()).getValue(IcicleBlock.CRACKED), "one event warns intact segments and never double-rolls them");
        helper.assertTrue(!level.getBlockState(far).getValue(IcicleBlock.CRACKED), "diagonal outside sphere stays intact");
        IcicleEvents.vibrate(level, center, 12, YES);
        helper.assertTrue(level.getBlockState(root).isAir(), "subsequent vibration can shatter a warned segment immediately");
        IcicleEvents.vibrate(level, center, 32, YES);
        helper.assertTrue(level.getBlockState(far).getValue(IcicleBlock.CRACKED), "explosion radius reaches farther segments");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "icicle_temperatureprobabilitiesandmaterialgrowth", timeoutTicks = 40)
    public static void temperatureProbabilitiesAndMaterialGrowth(GameTestHelper helper) {
        near(helper, 1, IcicleBlock.growthMultiplier(0), "growth threshold");
        near(helper, 1.55, IcicleBlock.growthMultiplier(-5.5), "continuous additive cold multiplier");
        near(helper, 2, IcicleBlock.growthMultiplier(-100), "growth cap");
        near(helper, 0, IcicleBlock.growthMultiplier(.01), "warm growth forbidden");
        near(helper, 0, IcicleBlock.fractureChance(4.99, 0, false), "heat threshold");
        near(helper, .125, IcicleBlock.fractureChance(5, 0, false), "heat threshold baseline");
        near(helper, .25, IcicleBlock.fractureChance(10, 0, false), "five degrees doubles heat chance");
        near(helper, .375, IcicleBlock.fractureChance(5, 50, true), "root and capped load multipliers");
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        var root = helper.absolutePos(new BlockPos(1, 18, 1));
        chain(level, root, 1);
        try (var biome = new CaveBiome(level, root, root.below(7))) {
            helper.assertTrue(IcicleBlock.inGrowthBiome(level, root), "growth fixture must sample the cave biome");
            seed(helper, root, 1, MaterialThermalLaw.SENSIBLE);
            var ice = FHBlocks.ICICLE.get();
            ice.randomTick(level.getBlockState(root), level, root, YES);
            helper.assertTrue(level.getBlockState(root.below()).isAir(), "warm material cannot grow even in a cold biome");
            seed(helper, root, 0, MaterialThermalLaw.SENSIBLE);
            near(helper, 0, MinecraftPhaseController.temperature(level, root), "growth fixture must read cold material");
            ice.randomTick(level.getBlockState(root), level, root, YES);
            helper.assertTrue(IcicleBlock.isIcicle(level.getBlockState(root.below())), "cold material grows without water");
            ice.warn(level, root, YES);
            seed(helper, root.below(), -10, MaterialThermalLaw.SENSIBLE);
            ice.randomTick(level.getBlockState(root.below()), level, root.below(), YES);
            helper.assertTrue(level.getBlockState(root.below(2)).isAir(), "a warned upper chain cannot keep extending");
            chain(level, root, 8);
            var tip = root.below(7);
            seed(helper, tip, -20, MaterialThermalLaw.SENSIBLE);
            helper.assertTrue(IcicleBlock.inGrowthBiome(level, tip), "cap fixture must sample the cave biome");
            ice.randomTick(level.getBlockState(tip), level, tip, YES);
            helper.assertTrue(level.getBlockState(tip.below()).isAir(), "natural eight-block cap");
            helper.assertTrue(IcicleBlock.canGrowRootFrom(Blocks.PACKED_ICE.defaultBlockState())
                    && IcicleBlock.canGrowRootFrom(Blocks.BLUE_ICE.defaultBlockState()), "all full ice substrates");
            helper.assertTrue(!IcicleBlock.canGrowRootFrom(FHBlocks.THIN_ICE_BLOCK.get().defaultBlockState())
                    && !IcicleBlock.canGrowRootFrom(FHBlocks.LAYERED_THIN_ICE.get().defaultBlockState()), "both thin forms excluded");
            helper.assertTrue(IcicleBlock.hasNearbyRoot(level, root.offset(2, 0, 0)), "short-range root spacing");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "icicle_dormantmeltingpaysforeachtipandkeepswarning", timeoutTicks = 40)
    public static void dormantMeltingPaysForEachTipAndKeepsWarning(GameTestHelper helper) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        var root = helper.absolutePos(new BlockPos(1, 20, 1));
        chain(level, root, 4);
        var ice = FHBlocks.ICICLE.get();
        var law = MinecraftThermalProfiles.materialLaw(level.getBlockState(root));
        var edge = law.heating();
        var thin = MinecraftThermalProfiles.materialLaw(FHBlocks.THIN_ICE_BLOCK.get().defaultBlockState()).heating();
        double budget = edge.targetEnthalpyJ() - edge.sourceEnthalpyJ();
        near(helper, (thin.targetEnthalpyJ() - thin.sourceEnthalpyJ()) / 3, budget, "shipped recipe matches one third of full thin ice");
        near(helper, 2, edge.transitionTemperatureC(), "material melting plateau");
        ice.warn(level, root, YES);
        seedEnergy(helper, root.below(3), edge.sourceEnthalpyJ() + budget * .3, MaterialThermalLaw.HEATING);
        seedEnergy(helper, root, edge.targetEnthalpyJ(), MaterialThermalLaw.HEATING);
        var state = level.getBlockState(root);
        helper.assertTrue(MinecraftPhaseController.applyDormantTransition(level, root, state, edge, edge.targetEnthalpyJ(), level.getGameTime()), "one completed budget shortens");
        helper.assertTrue(level.getBlockState(root.below(3)).isAir() && IcicleBlock.isIcicle(level.getBlockState(root.below(2))), "only terminal segment removed");
        helper.assertTrue(level.getBlockState(root).getValue(IcicleBlock.CRACKED)
                && level.getBlockTicks().hasScheduledTick(root, ice), "heated upper segment retains its warning");
        var sample = new MaterialSample();
        var chunk = level.getChunkAt(root);
        var stored = ((MinecraftThermalChunkAttachment) (Object) chunk).frostedheart$getDormantThermalState();
        stored.readMaterial(root.getY() >> 4, index(root), Block.BLOCK_STATE_REGISTRY.getId(level.getBlockState(root)), sample);
        near(helper, edge.sourceEnthalpyJ(), sample.enthalpyJ(), "budget consumed without inheriting the removed tip's partial progress");
        helper.assertTrue(level.getBlockState(root).getValue(IcicleBlock.MELT_CYCLE), "phase receipt retained in ordinary block state");
        var checkpoint = new CompoundTag();
        stored.encode(checkpoint);
        var reloaded = DormantChunkThermalState.decode(checkpoint, chunk.getMinSection(), chunk.getSectionsCount());
        reloaded.readMaterial(root.getY() >> 4, index(root), Block.BLOCK_STATE_REGISTRY.getId(level.getBlockState(root)), sample);
        near(helper, edge.sourceEnthalpyJ(), sample.enthalpyJ(), "save/reload cannot restore consumed progress");
        helper.assertTrue(MinecraftPhaseController.tryAtRandomTick(level, chunk, root, level.getBlockState(root)) != MinecraftPhaseController.PhaseAttempt.CHANGED,
                "no immediate free second melt from the same stored enthalpy");
        seedEnergy(helper, root, edge.targetEnthalpyJ(), MaterialThermalLaw.HEATING);
        helper.assertTrue(MinecraftPhaseController.applyDormantTransition(level, root, level.getBlockState(root), edge, edge.targetEnthalpyJ(), level.getGameTime()), "second paid budget shortens once more");
        helper.assertTrue(level.getBlockState(root.below(2)).isAir() && IcicleBlock.isIcicle(level.getBlockState(root.below())), "second tip only");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "icicle_fallingiceshattersinwaterandextinguishescampfire", timeoutTicks = 100)
    public static void fallingIceShattersInWaterAndExtinguishesCampfire(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 4, 1));
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        var campfire = (net.minecraft.world.level.block.entity.CampfireBlockEntity) level.getBlockEntity(pos);
        ((com.teammoeg.frostedheart.util.mixin.ICampfireExtra) campfire).addLifeTime(1000);
        campfire.placeFood(null, new ItemStack(Items.PORKCHOP), 600);
        chain(level, pos.above(4), 1);
        IcicleBlock.fallChain(level, pos.above(4));
        helper.runAfterDelay(35, () -> {
            helper.assertTrue(level.getBlockState(pos).is(Blocks.CAMPFIRE) && !level.getBlockState(pos).getValue(CampfireBlock.LIT), "actual falling contact extinguishes campfire");
            helper.assertTrue(campfire.getItems().get(0).is(Items.PORKCHOP), "campfire contents retained");
            helper.assertTrue(level.getEntitiesOfClass(FallingBlockEntity.class, new AABB(pos).inflate(5)).isEmpty(), "landed ice never places or drops itself");
            level.setBlockAndUpdate(pos.above(), Blocks.WATER.defaultBlockState());
            chain(level, pos.above(4), 1);
            IcicleBlock.fallChain(level, pos.above(4));
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(level.getEntitiesOfClass(FallingBlockEntity.class, new AABB(pos).inflate(5)).isEmpty(), "water destroys falling ice");
                helper.assertTrue(level.getBlockState(pos.above()).is(Blocks.WATER), "water remains water");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "phase0a_empty", batch = "icicle_silklootandallfourfiretypes", timeoutTicks = 40)
    public static void silkLootAndAllFourFireTypes(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 4, 1));
        for (var block : new Block[]{Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE, Blocks.FIRE, Blocks.SOUL_FIRE}) {
            level.setBlockAndUpdate(pos.below(), Blocks.SOUL_SOIL.defaultBlockState());
            level.setBlockAndUpdate(pos, block.defaultBlockState());
            helper.assertTrue(IcicleFalling.extinguish(level, pos), "supported fire type extinguishes: " + block);
            helper.assertTrue(block instanceof CampfireBlock ? level.getBlockState(pos).is(block) && !level.getBlockState(pos).getValue(CampfireBlock.LIT)
                    : level.getBlockState(pos).isAir(), "extinguish result");
        }
        chain(level, pos, 1);
        var state = level.getBlockState(pos);
        helper.assertTrue(Block.getDrops(state, level, pos, null, null, new ItemStack(Items.IRON_PICKAXE)).isEmpty(), "normal mining drops nothing");
        var silk = new ItemStack(Items.DIAMOND_PICKAXE);
        silk.enchant(Enchantments.SILK_TOUCH, 1);
        var drops = Block.getDrops(state, level, pos, null, null, silk);
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(FHBlocks.ICICLE.asItem()) && drops.get(0).getCount() == 1, "silk collects only one segment");
        near(helper, 40.0 / 3, IcicleFalling.damage(100, 40, 40), "damage cap scaled by one third");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "icicle_roots", timeoutTicks = 40)
    public static void existingColdCaveIceGrowsRootsWithoutWater(GameTestHelper helper) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        var root = helper.absolutePos(new BlockPos(1, 10, 1));
        level.setBlockAndUpdate(root.above(), Blocks.BLUE_ICE.defaultBlockState());
        level.setBlockAndUpdate(root, Blocks.AIR.defaultBlockState());
        seed(helper, root.above(), -10, MaterialThermalLaw.SENSIBLE);
        try (var biome = new CaveBiome(level, root)) {
            helper.assertTrue(IcicleBlock.inGrowthBiome(level, root), "root fixture must sample the cave biome");
            IcicleBlock.randomRootTick(level, root.above(), level.getBlockState(root.above()), YES);
            helper.assertTrue(IcicleBlock.isIcicle(level.getBlockState(root)), "existing cave ice can grow a root without world generation or water");
            helper.assertTrue(Blocks.PACKED_ICE.defaultBlockState().isRandomlyTicking()
                    && Blocks.BLUE_ICE.defaultBlockState().isRandomlyTicking(), "shared sampler admits non-vanilla-random ice substrates");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "icicle_missing_body", timeoutTicks = 40)
    public static void missingMaterialUsesOrdinaryFallbackWithoutSeeding(GameTestHelper helper) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        var root = helper.absolutePos(new BlockPos(1, 12, 1));
        chain(level, root, 2);
        var key = new com.teammoeg.frostedheart.content.climate.thermal.field.ThermalFieldKey(new ResourceLocation("frostedheart", "icicle_test"), 1, 2, 0);
        com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftGameplayFields.upsertSphere(level, key, Integer.MAX_VALUE,
                com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField.CombineMode.OVERRIDE,
                root.getX() + .5, root.getY() + .5, root.getZ() + .5, 2, -10);
        try {
            var chunk = level.getChunkAt(root);
            helper.assertTrue(MinecraftPhaseController.tryAtRandomTick(level, chunk, root, level.getBlockState(root))
                    != MinecraftPhaseController.PhaseAttempt.CHANGED, "cold fallback retains the whole chain");
            com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftGameplayFields.upsertSphere(level, key, Integer.MAX_VALUE,
                    com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField.CombineMode.OVERRIDE,
                    root.getX() + .5, root.getY() + .5, root.getZ() + .5, 2, 20);
            var outcome = MinecraftPhaseController.tryAtRandomTick(level, chunk, root, level.getBlockState(root));
            helper.assertTrue(outcome == MinecraftPhaseController.PhaseAttempt.CHANGED
                    && IcicleBlock.isIcicle(level.getBlockState(root)) && level.getBlockState(root.below()).isAir(),
                    "ordinary warm fallback removes the terminal segment and retains the upper segment");
            var stored = ((MinecraftThermalChunkAttachment) (Object) chunk).frostedheart$getDormantThermalState();
            var sample = new MaterialSample();
            helper.assertTrue(stored == null || stored.readMaterial(root.getY() >> 4, index(root),
                    Block.BLOCK_STATE_REGISTRY.getId(level.getBlockState(root)), sample) == DormantChunkThermalState.MaterialRead.MISSING,
                    "icicle fallback does not create a special material record");
            MinecraftPhaseController.tryAtRandomTick(level, chunk, root, level.getBlockState(root));
            helper.assertTrue(level.getBlockState(root).isAir(), "a one-segment chain melts through the same fallback");
        } finally { com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftGameplayFields.remove(level, key); }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "icicle_damage", timeoutTicks = 40)
    public static void onlyFallingTipHurtsAndSurvivesEntityNbt(GameTestHelper helper) {
        var level = helper.getLevel();
        var root = helper.absolutePos(new BlockPos(1, 15, 1));
        chain(level, root, 3);
        IcicleBlock.fallChain(level, root);
        var entities = level.getEntitiesOfClass(FallingBlockEntity.class, new AABB(root.below(3)).expandTowards(1, 5, 1));
        helper.assertTrue(entities.size() == 3, "three falling segments");
        int damaging = 0;
        for (var falling : entities) {
            var tag = falling.saveWithoutId(new CompoundTag());
            if (tag.getBoolean("HurtEntities")) {
                damaging++;
                helper.assertTrue(falling.getStartPos().equals(root.below(2)), "only the original terminal segment hurts");
                var golem = EntityType.IRON_GOLEM.create(level);
                golem.setNoAi(true);
                golem.moveTo(falling.getX(), falling.getY(), falling.getZ(), 0, 0);
                level.addFreshEntity(golem);
                float before = golem.getHealth();
                falling.causeFallDamage(20, 1, level.damageSources().fall());
                near(helper, 40.0 / 3, before - golem.getHealth(), "real damage callback scales capped raw damage");
                golem.discard();
            }
            var restored = new FallingBlockEntity(EntityType.FALLING_BLOCK, level);
            restored.load(tag);
            helper.assertTrue(IcicleBlock.isIcicle(restored.getBlockState()) && restored.saveWithoutId(new CompoundTag()).getBoolean("HurtEntities")
                    == tag.getBoolean("HurtEntities"), "vanilla entity NBT retains icicle identity and terminal damage flag");
            restored.discard();
            falling.discard();
        }
        helper.assertTrue(damaging == 1, "chain segments cannot multiply full damage");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "icicle_worldgen", timeoutTicks = 40)
    public static void ceilingFeatureProducesConnectedIcicles(GameTestHelper helper) {
        var level = helper.getLevel();
        var ceiling = helper.absolutePos(new BlockPos(1, 12, 1));
        level.setBlockAndUpdate(ceiling, Blocks.PACKED_ICE.defaultBlockState());
        for (int i = 1; i <= 9; i++) level.setBlockAndUpdate(ceiling.below(i), Blocks.AIR.defaultBlockState());
        var context = new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(Optional.empty(), level,
                level.getChunkSource().getGenerator(), YES, ceiling, net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration.NONE);
        helper.assertTrue(com.teammoeg.frostedheart.content.world.FHFeatures.ICICLE.get().place(context), "pack ceiling origin places ice below its support");
        helper.assertTrue(level.getBlockState(ceiling).is(Blocks.PACKED_ICE), "support retained");
        helper.assertTrue(level.getBlockState(ceiling.below()).getValue(IcicleBlock.SHAPE) == IcicleBlock.ROOT
                && level.getBlockState(ceiling.below(2)).getValue(IcicleBlock.SHAPE) == IcicleBlock.TIP, "generated chain has connected models");
        helper.assertTrue(level.getBlockState(ceiling.below(9)).isAir(), "worldgen cannot exceed natural cap");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "icicle_burst", timeoutTicks = 80)
    public static void sixteenFiveBlockChainsFallWithoutDuplicateEntities(GameTestHelper helper) {
        var level = helper.getLevel();
        var start = helper.absolutePos(new BlockPos(1, 25, 1));
        for (int x = 0; x < 11; x++) for (int z = 0; z < 11; z++) {
            level.setBlockAndUpdate(start.offset(x, -9, z), Blocks.STONE.defaultBlockState());
            for (int y = -8; y <= 1; y++) level.setBlockAndUpdate(start.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        for (int x = 0; x < 4; x++) for (int z = 0; z < 4; z++) {
            var root = start.offset(x * 3, 0, z * 3);
            chain(level, root, 5);
            FHBlocks.ICICLE.get().warn(level, root, YES);
        }
        long begin = System.nanoTime();
        IcicleEvents.vibrate(level, Vec3.atCenterOf(start.offset(4, -2, 4)), 32, YES);
        long vibrationNanos = System.nanoTime() - begin;
        var bounds = new AABB(start.offset(-1, -10, -1)).expandTowards(13, 13, 13);
        var falling = level.getEntitiesOfClass(FallingBlockEntity.class, bounds);
        helper.assertTrue(falling.size() == 64, "sixteen warned roots each release exactly four lower segments");
        begin = System.nanoTime();
        for (int tick = 0; tick < 60; tick++) for (var entity : falling) if (!entity.isRemoved()) entity.tick();
        long fallingNanos = System.nanoTime() - begin;
        helper.assertTrue(falling.stream().allMatch(FallingBlockEntity::isRemoved), "all sixty-four falling entities eventually shatter");
        com.teammoeg.frostedheart.FHMain.LOGGER.info("ICICLE_BURST_16x5 vibration={} ms, 60 manual physics steps={} ms, falling=64",
                vibrationNanos / 1_000_000.0, fallingNanos / 1_000_000.0);
        helper.succeed();
    }

    /** BiomeManager chooses nearby quart samples, not necessarily the point's own quart. */
    private static final class CaveBiome implements AutoCloseable {
        private record Previous(PalettedContainer<Holder<Biome>> palette, int x, int y, int z, Holder<Biome> biome) {}
        private final java.util.List<Previous> previous = new java.util.ArrayList<>();

        CaveBiome(ServerLevel level, BlockPos... positions) {
            var cave = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                    ResourceKey.create(Registries.BIOME, new ResourceLocation("the_winter_rescue", "ice_caves")));
            for (var pos : positions) for (int x = (pos.getX() >> 2) - 1; x <= (pos.getX() >> 2) + 1; x++)
                for (int z = (pos.getZ() >> 2) - 1; z <= (pos.getZ() >> 2) + 1; z++)
                    for (int y = (pos.getY() >> 2) - 1; y <= (pos.getY() >> 2) + 1; y++) {
                        if (level.isOutsideBuildHeight(y << 2)) continue;
                        @SuppressWarnings("unchecked") var palette = (PalettedContainer<Holder<Biome>>) level.getChunk(x >> 2, z >> 2)
                                .getSection(level.getSectionIndex(y << 2)).getBiomes();
                        previous.add(new Previous(palette, x & 3, y & 3, z & 3, palette.get(x & 3, y & 3, z & 3)));
                        palette.set(x & 3, y & 3, z & 3, cave);
                    }
        }

        @Override public void close() {
            for (int i = previous.size() - 1; i >= 0; i--) {
                var value = previous.get(i);
                value.palette.set(value.x, value.y, value.z, value.biome);
            }
        }
    }

    private static void chain(ServerLevel level, BlockPos root, int length) {
        level.setBlockAndUpdate(root.above(), Blocks.PACKED_ICE.defaultBlockState());
        for (int i = 0; i < length; i++) level.setBlockAndUpdate(root.below(i), FHBlocks.ICICLE.get().defaultBlockState());
    }

    private static int index(BlockPos pos) { return (pos.getX() & 15) | (pos.getZ() & 15) << 4 | (pos.getY() & 15) << 8; }

    static void seed(GameTestHelper helper, BlockPos pos, double temperature, byte branch) {
        seedEnergy(helper, pos, MinecraftThermalProfiles.materialLaw(helper.getLevel().getBlockState(pos)).enthalpyAtTemperature(temperature), branch);
    }

    private static void seedEnergy(GameTestHelper helper, BlockPos pos, double energy, byte branch) {
        var level = helper.getLevel();
        var chunk = level.getChunkAt(pos);
        var attachment = (MinecraftThermalChunkAttachment) (Object) chunk;
        var stored = attachment.frostedheart$getDormantThermalState();
        if (stored == null) {
            stored = new DormantChunkThermalState(chunk.getMinSection(), chunk.getSectionsCount());
            attachment.frostedheart$setDormantThermalState(stored);
        }
        var state = level.getBlockState(pos);
        var law = MinecraftThermalProfiles.materialLaw(state);
        if (!stored.updateMaterial(pos.getY() >> 4, index(pos), Block.BLOCK_STATE_REGISTRY.getId(state), law, energy, branch, level.getGameTime())) {
            var captured = ThermalMaterialBehaviorGameTests.storedBrick(helper, pos, 0);
            var editor = new MaterialSectionState.Editor(captured);
            helper.assertTrue(editor.update(index(pos), Block.BLOCK_STATE_REGISTRY.getId(state), law, energy, branch, level.getGameTime()),
                    "fixture updates a body captured through the ordinary material pipeline");
            stored.mergeMaterials(pos.getY() >> 4, editor.snapshot(), captured.brickMask());
        }
    }

    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) < 1e-6 * Math.max(1, Math.abs(expected)), message + ": " + actual);
    }
}
