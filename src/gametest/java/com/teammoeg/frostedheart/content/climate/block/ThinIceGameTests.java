package com.teammoeg.frostedheart.content.climate.block;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalFieldKey;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MinecraftThermalChunkAttachment;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftGameplayFields;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ThermalMaterialBehaviorGameTests;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPhaseController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_thin_ice")
@PrefixGameTestTemplate(false)
public class ThinIceGameTests {
    @GameTest(template = "phase0a_empty", batch = "thin_ice", timeoutTicks = 100)
    public static void cooldownDoesNotAdvanceUnoccupiedIceAndImpactBypassesIt(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 3, 1));
        var block = FHBlocks.THIN_ICE_BLOCK.get();
        level.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        var item = new ItemEntity(level, pos.getX(), pos.getY() + 1, pos.getZ(), new ItemStack(Items.STICK));
        block.stepOn(level, pos, level.getBlockState(pos), item);
        helper.assertTrue(level.getBlockState(pos).getValue(ThinIceBlock.CRACK) == 0, "items do not crack ice");
        block.impact(level, pos, 2); // One certain stage, then a 24-tick cooldown.
        helper.assertTrue(level.getBlockState(pos).getValue(ThinIceBlock.CRACK) == 1, "two-block impact adds exactly one stage");
        block.ordinaryContact(level, pos, 1); // A certain ordinary draw must still obey cooldown.
        helper.assertTrue(level.getBlockState(pos).getValue(ThinIceBlock.CRACK) == 1, "ordinary contact obeys cooldown");
        block.impact(level, pos, 2);
        helper.assertTrue(level.getBlockState(pos).getValue(ThinIceBlock.CRACK) == 2, "impact bypasses cooldown");
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(level.getBlockState(pos).getValue(ThinIceBlock.CRACK) == 2, "cooldown expiration leaves cracks unchanged");
            block.impact(level, pos, 4);
            helper.assertTrue(level.getBlockState(pos).isAir(), "suspended ice fractures to air");
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(1)).isEmpty(), "fracture drops nothing");
            helper.succeed();
        });
    }

    @GameTest(template = "phase0a_empty", batch = "thin_ice", timeoutTicks = 40)
    public static void allSixWaterNeighborsMakeASource(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 3, 1));
        var block = FHBlocks.THIN_ICE_BLOCK.get();
        for (Direction face : Direction.values()) {
            for (Direction clear : Direction.values())
                level.setBlockAndUpdate(pos.relative(clear), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos, block.defaultBlockState());
            level.setBlockAndUpdate(pos.relative(face), Blocks.WATER.defaultBlockState());
            block.impact(level, pos, 8);
            helper.assertTrue(level.getBlockState(pos).is(Blocks.WATER) && level.getFluidState(pos).isSource(),
                    "water on " + face + " produces a source");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "thin_ice", timeoutTicks = 40)
    public static void thicknessSetsFractureCountAndStackingPreservesDamageRatio(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 3, 1));
        var block = FHBlocks.LAYERED_THIN_ICE.get();
        for (Direction face : Direction.values()) level.setBlockAndUpdate(pos.relative(face), Blocks.AIR.defaultBlockState());
        for (int layers = 1; layers <= 8; layers++) {
            var state = block.defaultBlockState().setValue(LayeredThinIceBlock.LAYERS, layers);
            helper.assertTrue(Math.abs(state.getCollisionShape(level, pos).max(Direction.Axis.Y) - layers / 8.0) < 0.0001,
                    "collision matches layer " + layers);
            level.setBlockAndUpdate(pos, state);
            int hits = (layers + 1) / 2;
            for (int hit = 1; hit < hits; hit++) {
                block.impact(level, pos, 2);
                helper.assertTrue(level.getBlockState(pos).getValue(ThinIceBlock.CRACK) == hit, "thickness preserves stage");
            }
            block.impact(level, pos, 2);
            helper.assertTrue(level.getBlockState(pos).isAir(), "layer " + layers + " fractures after " + hits + " hits");
        }
        helper.assertTrue(LayeredThinIceBlock.inheritedCrack(1, 4, 5) == 2, "half damaged 4-layer ice becomes 2/3 damaged 5-layer ice");
        helper.assertTrue(LayeredThinIceBlock.inheritedCrack(2, 6, 7) == 3, "stacking rounds damage up");
        var random = net.minecraft.util.RandomSource.create(381);
        helper.assertTrue(ThinIceBlock.impactDamage(0.99F, random) == 0, "sub-block falls have no impact");
        int sum = 0;
        for (int i = 0; i < 4096; i++) sum += ThinIceBlock.impactDamage(3, random);
        helper.assertTrue(Math.abs(sum / 4096.0 - 1.5) < 0.05, "fractional impact uses the fractional probability");
        helper.succeed();
    }
    @GameTest(template = "phase0a_empty", batch = "thin_ice_phase", timeoutTicks = 100)
    public static void savedPhaseBudgetDrivesWarmCapAndWaterAssistedRepair(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 3, 1));
        var ice = FHBlocks.THIN_ICE_BLOCK.get();
        var key = new com.teammoeg.frostedheart.content.climate.thermal.field.ThermalFieldKey(
                new net.minecraft.resources.ResourceLocation("frostedheart", "thin_ice_test"), 0, pos.asLong(), 0);
        helper.assertTrue(ThinIcePhaseData.warmLimit(0, 4) == 0
                && ThinIcePhaseData.warmLimit(2.99, 4) == 1
                && ThinIcePhaseData.warmLimit(3, 4) == 2
                && ThinIcePhaseData.warmLimit(6, 4) == 3
                && ThinIcePhaseData.warmLimit(9, 4) == 0, "thermal bands include the correct boundaries");
        helper.assertTrue(Math.abs(ThinIcePhaseData.heatingBudgetJ() - 95000) < 0.01, "heat work is a quarter of the compiled melt budget");
        helper.assertTrue(Math.abs(ThinIcePhaseData.coolingBudgetJ() - 90800) < 0.01, "repair work is a quarter of the compiled freezing budget");
        level.setBlockAndUpdate(pos, ice.defaultBlockState());
        com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftGameplayFields.upsertSphere(
                level, key, 100, com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField.CombineMode.OVERRIDE,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.25, 2);
        seedPhase(level, pos, ThinIcePhaseData.heatingBudgetJ() - 1);
        ThinIcePhaseData.observe(level, pos, level.getBlockState(pos));
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(level.getBlockState(pos).getValue(ThinIceBlock.CRACK) == 1, "saved near-complete heat work advances exactly one stage at 2C");
            ice.impact(level, pos, 2);
            helper.assertTrue(level.getBlockState(pos).getValue(ThinIceBlock.CRACK) == 2, "mechanical damage can exceed the warm cap");
            com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftGameplayFields.upsertSphere(
                    level, key, 100, com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField.CombineMode.OVERRIDE,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.25, -6);
            level.setBlockAndUpdate(pos.below(), Blocks.WATER.defaultBlockState());
            seedPhase(level, pos, -ThinIcePhaseData.coolingBudgetJ() + 1);
            ThinIcePhaseData.observe(level, pos, level.getBlockState(pos));
        });
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(level.getBlockState(pos).getValue(ThinIceBlock.CRACK) == 1, "cold adjacent water repairs one stage after its work budget completes");
            com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftGameplayFields.remove(level, key);
            helper.succeed();
        });
    }


    @GameTest(template = "phase0a_empty", batch = "thin_ice_real_landing", timeoutTicks = 70)
    public static void collisionLandingBreaksSingleLayerWhileOrdinaryContactIsCoolingDown(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 3, 1));
        var ice = FHBlocks.LAYERED_THIN_ICE.get();
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, ice.defaultBlockState());
        level.scheduleTick(pos, ice, 200); // Rules out ordinary random contact as the cause.
        var key = new com.teammoeg.frostedheart.content.climate.thermal.field.ThermalFieldKey(
                new net.minecraft.resources.ResourceLocation("frostedheart", "thin_ice_landing_test"), 0, pos.asLong(), 0);
        com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftGameplayFields.upsertSphere(
                level, key, 100, com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField.CombineMode.OVERRIDE,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0);
        var pig = EntityType.PIG.create(level);
        pig.setNoAi(true);
        pig.moveTo(pos.getX() + 0.5, pos.getY() + 9.125, pos.getZ() + 0.5, 0, 0);
        level.addFreshEntity(pig);
        try {
            // A headless GameTest world can suspend automatic entity ticks without players.
            // Drive the real movement/collision/fallOn path, not the impact helper directly.
            pig.fallDistance = 9;
            pig.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(0, -10, 0));
            helper.assertTrue(level.getBlockState(pos).isAir(), "collision landing must fracture single-layer ice despite cooldown");
            helper.succeed();
        } finally {
            com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftGameplayFields.remove(level, key);
            pig.discard();
        }
    }

    @GameTest(template = "phase0a_empty", batch = "thin_ice_body_temperature", timeoutTicks = 100)
    public static void materialTemperatureControlsHeatAndRepairDespiteOppositeEnvironment(GameTestHelper helper) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        var pos = helper.absolutePos(new BlockPos(1, 3, 1));
        var ice = FHBlocks.THIN_ICE_BLOCK.get();
        var attachment = (MinecraftThermalChunkAttachment) (Object) level.getChunkAt(pos);
        var previous = attachment.frostedheart$getDormantThermalState();
        var key = new ThermalFieldKey(new net.minecraft.resources.ResourceLocation("frostedheart", "thin_ice_body_test"), 0, pos.asLong(), 0);
        level.setBlockAndUpdate(pos, ice.defaultBlockState());
        for (Direction face : Direction.values()) level.setBlockAndUpdate(pos.relative(face), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.below(), Blocks.WATER.defaultBlockState());
        MinecraftGameplayFields.upsertSphere(level, key, Integer.MAX_VALUE, ThermalAnalyticField.CombineMode.OVERRIDE,
                pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 2, -10);
        var stale = ThermalMaterialBehaviorGameTests.storedBrick(helper, pos, 2);
        level.setBlockAndUpdate(pos, ice.defaultBlockState().setValue(ThinIceBlock.CRACK, 1));
        var staleContainer = new DormantChunkThermalState(pos.getY() >> 4, 1);
        staleContainer.replaceMaterials(pos.getY() >> 4, stale);
        attachment.frostedheart$setDormantThermalState(staleContainer);
        helper.assertTrue(Double.isNaN(ThinIcePhaseData.temperature(level, pos))
                        && MinecraftPhaseController.tryAtRandomTick(level, level.getChunkAt(pos), pos, level.getBlockState(pos))
                        == MinecraftPhaseController.PhaseAttempt.DEFERRED,
                "both phase paths wait on a mismatched material checkpoint instead of substituting the environment");
        level.setBlockAndUpdate(pos, ice.defaultBlockState());
        seedBody(helper, pos, attachment, 2);
        helper.assertTrue(Math.abs(ThinIcePhaseData.temperature(level, pos) - WorldTemperature.material(level, pos)) < .001,
                "thin ice uses the actual material thermometer");
        helper.assertTrue(ThinIcePhaseData.temperature(level, pos) > 1 && WorldTemperature.block(level, pos) < -9,
                "fixture separates warm material from cold environment");
        seedPhase(level, pos, ThinIcePhaseData.heatingBudgetJ() - 1);
        ThinIcePhaseData.observe(level, pos, level.getBlockState(pos));
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(level.getBlockState(pos).getValue(ThinIceBlock.CRACK) == 1,
                    "a warm body cracks even when its environment is cold");
            seedPhase(level, pos, -ThinIcePhaseData.coolingBudgetJ() + 1);
            ThinIcePhaseData.observe(level, pos, level.getBlockState(pos));
        });
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(level.getBlockState(pos).getValue(ThinIceBlock.CRACK) == 1,
                    "cold environment cannot repair a still-warm body");
            MinecraftGameplayFields.upsertSphere(level, key, Integer.MAX_VALUE, ThermalAnalyticField.CombineMode.OVERRIDE,
                    pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 2, 2);
            seedBody(helper, pos, attachment, -10);
            seedPhase(level, pos, ThinIcePhaseData.heatingBudgetJ() - 1);
            ThinIcePhaseData.observe(level, pos, level.getBlockState(pos));
        });
        helper.runAfterDelay(75, () -> {
            helper.assertTrue(level.getBlockState(pos).getValue(ThinIceBlock.CRACK) == 1,
                    "warm environment cannot heat-crack a still-cold body");
            seedPhase(level, pos, -ThinIcePhaseData.coolingBudgetJ() + 1);
            ThinIcePhaseData.observe(level, pos, level.getBlockState(pos));
        });
        helper.runAfterDelay(95, () -> {
            try {
                helper.assertTrue(level.getBlockState(pos).getValue(ThinIceBlock.CRACK) == 0,
                        "a cold body repairs despite warm environment");
                helper.succeed();
            } finally {
                MinecraftGameplayFields.remove(level, key);
                attachment.frostedheart$setDormantThermalState(previous);
            }
        });
    }

    @GameTest(template = "phase0a_empty", batch = "thin_ice_creative", timeoutTicks = 40)
    public static void creativePlayersCanJumpAndLandOnFullAndSingleLayerIce(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 3, 1));
        var ice = FHBlocks.THIN_ICE_BLOCK.get();
        var player = net.minecraftforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.fromString("c44c4ba1-3eaf-4c7d-ae35-e09b57eb2854"), "ThinIceCreative"));
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        try {
            helper.assertTrue(ThinIceBlock.canCrack(player), "creative players can apply pressure and impacts");
            level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos, ice.defaultBlockState());
            player.moveTo(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5, 0, 0);
            player.setOnGround(true);
            // Repeated independent jump events make the half-chance impact deterministic in practice.
            for (int i = 0; i < 128 && level.getBlockState(pos).getValue(ThinIceBlock.CRACK) == 0; i++)
                ThinIceEvents.jump(new net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent(player));
            helper.assertTrue(level.getBlockState(pos).getValue(ThinIceBlock.CRACK) == 1, "creative jump events crack ice");
            player.fallDistance = 2;
            player.doCheckFallDamage(0, -2, 0, true);
            helper.assertTrue(level.getBlockState(pos).getValue(ThinIceBlock.CRACK) == 2,
                    "creative landing bypasses the cooldown on full ice");
            var layer = FHBlocks.LAYERED_THIN_ICE.get();
            level.setBlockAndUpdate(pos.below(), ice.defaultBlockState());
            level.setBlockAndUpdate(pos, layer.defaultBlockState());
            level.scheduleTick(pos, layer, 200);
            player.moveTo(pos.getX() + .5, pos.getY() + 9.125, pos.getZ() + .5, 0, 0);
            player.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(0, -10, 0));
            player.fallDistance = 9;
            player.doCheckFallDamage(0, -9, 0, true);
            helper.assertTrue(level.getBlockState(pos).isAir(), "creative landing must reach single-layer ice without LivingFallEvent");
            helper.assertTrue(level.getBlockState(pos.below()).is(ice)
                            && level.getBlockState(pos.below()).getValue(ThinIceBlock.CRACK) == 0,
                    "landing must not also damage the lower ice selected by legacy sampling");
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            helper.assertTrue(!ThinIceBlock.canCrack(player), "spectators remain nonphysical");
            helper.succeed();
        } finally {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.moveTo(0, level.getMinBuildHeight() + 1, 0);
        }
    }

    @GameTest(template = "phase0a_empty", batch = "thin_ice_contact_target", timeoutTicks = 40)
    public static void ordinaryContactDoesNotPassThroughSingleLayerIce(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 3, 1));
        var ice = FHBlocks.THIN_ICE_BLOCK.get();
        var layer = FHBlocks.LAYERED_THIN_ICE.get();
        level.setBlockAndUpdate(pos.below(), ice.defaultBlockState());
        level.setBlockAndUpdate(pos, layer.defaultBlockState());
        level.scheduleTick(pos, layer, 200);
        var pig = EntityType.PIG.create(level);
        pig.setNoAi(true);
        pig.moveTo(pos.getX() + .5, pos.getY() + .125, pos.getZ() + .5, 0, 0);
        var endTick = new net.minecraftforge.event.TickEvent.LevelTickEvent(
                net.minecraftforge.fml.LogicalSide.SERVER, net.minecraftforge.event.TickEvent.Phase.END, level, () -> true);
        try {
            // Real collision/stepOn callbacks must only touch the upper surface.
            for (int i = 0; i < 512; i++) {
                pig.move(net.minecraft.world.entity.MoverType.SELF,
                        new net.minecraft.world.phys.Vec3(i % 2 == 0 ? .01 : -.01, -.01, 0));
                ThinIceEvents.endTick(endTick);
            }
            helper.assertTrue(level.getBlockState(pos).is(layer), "upper ice remains under cooldown");
            helper.assertTrue(level.getBlockState(pos.below()).is(ice)
                            && level.getBlockState(pos.below()).getValue(ThinIceBlock.CRACK) == 0,
                    "ordinary contact must not reach ice below the actual supporting layer");
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            pig.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
            for (int i = 0; i < 512 && level.getBlockState(pos.below()).getValue(ThinIceBlock.CRACK) == 0; i++) {
                pig.move(net.minecraft.world.entity.MoverType.SELF,
                        new net.minecraft.world.phys.Vec3(i % 2 == 0 ? .01 : -.01, -.01, 0));
                ThinIceEvents.endTick(endTick);
            }
            helper.assertTrue(level.getBlockState(pos.below()).getValue(ThinIceBlock.CRACK) == 1,
                    "ordinary contact still cracks directly supporting full ice");
            helper.succeed();
        } finally {
            pig.discard();
        }
    }

    private static void seedBody(GameTestHelper helper, BlockPos pos, MinecraftThermalChunkAttachment attachment, double temperature) {
        var state = ThermalMaterialBehaviorGameTests.storedBrick(helper, pos, temperature);
        var stored = new DormantChunkThermalState(pos.getY() >> 4, 1);
        stored.replaceMaterials(pos.getY() >> 4, state);
        attachment.frostedheart$setDormantThermalState(stored);
    }

    private static void seedPhase(net.minecraft.server.level.ServerLevel level, BlockPos pos, double work) {
        var tag = ThinIcePhaseData.get(level).save(new net.minecraft.nbt.CompoundTag());
        var list = tag.getList("progress", net.minecraft.nbt.Tag.TAG_COMPOUND);
        // Round-trip the real save format so this also checks sub-stage persistence.
        list.removeIf(item -> ((net.minecraft.nbt.CompoundTag) item).getLong("position") == pos.asLong());
        var entry = new net.minecraft.nbt.CompoundTag();
        entry.putLong("position", pos.asLong());
        entry.putDouble("work_j", work);
        list.add(entry);
        tag.put("progress", list);
        level.getDataStorage().set("frostedheart_thin_ice", ThinIcePhaseData.load(tag));
    }

}
