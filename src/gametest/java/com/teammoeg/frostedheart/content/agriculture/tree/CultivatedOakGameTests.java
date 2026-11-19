package com.teammoeg.frostedheart.content.agriculture.tree;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.agriculture.FertilizedDirt;
import com.teammoeg.frostedheart.content.agriculture.Fertilizer.FertilizerType;
import com.teammoeg.frostedheart.content.agriculture.Fertilizer.FertilizerGrade;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Real block placement, growth ledgers, data-pack decoding, vanilla tree placement and obstacle retry. */
@GameTestHolder("frostedheart_cultivated_oak")
@PrefixGameTestTemplate(false)
public class CultivatedOakGameTests {
    private static BlockPos plant(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(9, 3, 9));
        helper.getLevel().setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 3);
        helper.getLevel().setBlock(pos.east(), Blocks.GLOWSTONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(pos, FHBlocks.CULTIVATED_OAK.getDefaultState(), 3);
        return pos;
    }
    private static CultivatedOakBlockEntity sapling(ServerLevel level, BlockPos pos) {
        return (CultivatedOakBlockEntity)level.getBlockEntity(pos);
    }
    private static BlockState fertilizer(FertilizerType type) {
        return FHBlocks.FERTILIZED_DIRT.getDefaultState().setValue(FertilizedDirt.FERTILIZER, type)
                .setValue(FertilizedDirt.GRADE, FertilizerGrade.BASIC).setValue(FertilizedDirt.STORAGE, 8);
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void matureYieldWaitAndRecovery(GameTestHelper helper) {
        var level = helper.getLevel(); BlockPos pos = plant(helper);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                level.getMaxLocalRawBrightness(pos.above()) >= 9, "fixture light must propagate before growth checks"))
                .thenExecute(() -> {
            for (int i = 0; i < 40; i++) sapling(level, pos).advance(level, -100);
            helper.assertTrue(sapling(level, pos).saveWithFullMetadata().getShort("p") == 256
                    && sapling(level, pos).saveWithFullMetadata().getShort("v") == 0,
                    "zero yield reaches full progress but never generates");
            // Near the band edge each random tick earns one fixed-point credit.
            sapling(level, pos).advance(level, -21.25f);
            helper.assertTrue(sapling(level, pos).saveWithFullMetadata().getShort("v") == 1,
                    "mature sapling continues earning yield credit");
            var saved = sapling(level, pos).saveWithFullMetadata();
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(pos, FHBlocks.CULTIVATED_OAK.getDefaultState().setValue(CultivatedOakBlock.AGE, 4), 3);
            sapling(level, pos).load(saved);
            for (int i = 0; i < 30; i++) sapling(level, pos).advance(level, -21.25f);
            helper.assertTrue(sapling(level, pos).saveWithFullMetadata().getShort("v") == 31,
                    "sub-block credit survives reload and waits without rounding up");
            sapling(level, pos).advance(level, -21.25f);
            helper.assertTrue(level.getBlockState(pos).is(Blocks.OAK_LOG)
                    && !level.getBlockState(pos.above()).is(Blocks.OAK_LOG), "32 credits generate exactly one trunk block");
            helper.succeed();
        });
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void temperatureCurve(GameTestHelper helper) {
        helper.runAfterDelay(10, () -> {
            var p = CultivatedOakProfile.get();
            for (float t : new float[]{5, 10, 15, 20, 25, 30, 35})
                helper.assertTrue(p.speed(t, 0) == 1, "growing range always full speed: " + t);
            helper.assertTrue(p.suitability(15, 0) == 1 && p.suitability(25, 0) == 1, "ideal endpoints inclusive");
            helper.assertTrue(p.suitability(5, 0) == .5 && p.suitability(35, 0) == .5, "growing endpoints yield");
            helper.assertTrue(p.speed(-25, 0) == 1 && p.speed(65, 0) == 1, "outer band endpoints");
            helper.assertTrue(p.suitability(-25, 0) == 0 && p.suitability(65, 0) == 0, "outer band zero yield");
            helper.assertTrue(p.speed(-100, 0) == 1 && p.suitability(100, 0) == 0, "extreme climate");
            helper.assertTrue(p.earnedHeight(0) == 0 && p.earnedHeight(.5) == 4 && p.earnedHeight(1) == 12,
                    "unfertilized height references retained");
            double scale = CultivatedOakBlockEntity.INCREASING_BONUS_SCALE;
            helper.assertTrue(p.earnedHeight(1 + scale) == 15 && p.earnedHeight(1 + 2 * scale) == 18
                    && p.earnedHeight(1 + 3 * scale) == 21, "fertilizer bonus is proportionally reduced");
            helper.assertTrue(p.earnedHeight(304.0 / 256) == 15 && p.earnedHeight(352.0 / 256) == 18
                    && p.earnedHeight(416.0 / 256) == 22, "actual rounded per-tick credit for all fertilizer grades");
            helper.assertTrue(p.earnedHeight(2) == 28 && p.earnedHeight(10) == 156,
                    "yield conversion has no saturation cap");
            int previous = 0;
            for (int i = 0; i <= 1000; i++) {
                int height = p.earnedHeight(i / 10.0);
                helper.assertTrue(height >= previous, "height increases monotonically without a cap");
                previous = height;
            }
            helper.succeed();
        });
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void stagesBlockedRetryAndSave(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = plant(helper);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                level.getMaxLocalRawBrightness(pos.above()) >= 9, "fixture light must propagate before growth checks"))
                .thenExecute(() -> {
            var profile = CultivatedOakProfile.get();
            helper.assertTrue(profile != null, "built-in data-pack profile must load");
            helper.assertTrue(profile.height(8.1) == 9 && profile.height(100) == 100, "data-driven ceil and height clamp");
            for (int stage = 1; stage <= 4; stage++) {
                for (int i = 0; i < profile.progressPerStage(); i++) sapling(level, pos).advance(level, 20);
                helper.assertTrue(level.getBlockState(pos).getValue(CultivatedOakBlock.AGE) == stage, "visible stage " + stage + ": " + sapling(level, pos).saveWithFullMetadata() + " / " + sapling(level, pos).status().getString());
            }
            var saved = sapling(level, pos).saveWithFullMetadata();
            helper.assertTrue(saved.getShort("p") == 256 && saved.getShort("v") == 256, "ideal unamended target is twelve logs");
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(pos, FHBlocks.CULTIVATED_OAK.getDefaultState().setValue(CultivatedOakBlock.AGE, 4), 3);
            sapling(level, pos).load(saved);
            level.setBlock(pos.below(), fertilizer(FertilizerType.INCREASING), 3);
            level.setBlock(pos.above(5), Blocks.STONE.defaultBlockState(), 3);
            for (int i = 0; i < 5; i++) sapling(level, pos).advance(level, 20);
            helper.assertTrue(level.getBlockState(pos).is(FHBlocks.CULTIVATED_OAK.get()), "obstacle retains sapling");
            helper.assertTrue(level.getBlockState(pos.below()).getValue(FertilizedDirt.STORAGE) == 8, "retry spends no fertilizer");
            helper.assertTrue(sapling(level, pos).saveWithFullMetadata().getShort("p") == 256 && sapling(level, pos).saveWithFullMetadata().getShort("v") == 256, "late fertilizer cannot change target");
            level.setBlock(pos.above(5), Blocks.AIR.defaultBlockState(), 3);
            sapling(level, pos).advance(level, 20);
            int count = 0;
            for (int i = 0; i < 32; i++) if (level.getBlockState(pos.above(i)).is(Blocks.OAK_LOG)) count++;
            helper.assertTrue(count >= 10 && count <= 11, "vanilla generator must deduct branch budget from the trunk, actual=" + count);
            helper.assertTrue(level.getBlockState(pos.below()).getValue(FertilizedDirt.STORAGE) == 8, "generation preserves paid soil");
            helper.assertTrue(Blocks.OAK_SAPLING.defaultBlockState().hasProperty(net.minecraft.world.level.block.SaplingBlock.STAGE),
                    "vanilla sapling retains original stage property");
            helper.succeed();
        });
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void temperatureAndProtection(GameTestHelper helper) {
        var level = helper.getLevel(); BlockPos pos = plant(helper);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                level.getMaxLocalRawBrightness(pos.above()) >= 9, "fixture light must propagate before growth checks"))
                .thenExecute(() -> {
            level.setBlock(pos.below(), fertilizer(FertilizerType.INCREASING), 3);
            sapling(level, pos).advance(level, 0);
            helper.assertTrue(sapling(level, pos).saveWithFullMetadata().getShort("p") == 16, "cold band retains full speed");
            helper.assertTrue(level.getBlockState(pos.below()).getValue(FertilizedDirt.STORAGE) == 7, "cold growth spends nutrient");
            level.setBlock(pos.below(), fertilizer(FertilizerType.PRESERVED), 3);
            sapling(level, pos).advance(level, 0);
            helper.assertTrue(sapling(level, pos).saveWithFullMetadata().getShort("p") > 0, "preservation extends cold range: " + sapling(level, pos).saveWithFullMetadata() + " / " + sapling(level, pos).status().getString());
            helper.assertTrue(level.getBlockState(pos.below()).getValue(FertilizedDirt.STORAGE) == 7, "earned growth pays one nutrient");
            helper.succeed();
        });
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void increasingYieldIsEarned(GameTestHelper helper) {
        var level = helper.getLevel(); BlockPos pos = plant(helper);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                level.getMaxLocalRawBrightness(pos.above()) >= 9, "fixture light must propagate before growth checks"))
                .thenExecute(() -> {
            for (int i = 0; i < 16; i++) {
                level.setBlock(pos.below(), fertilizer(FertilizerType.INCREASING), 3);
                sapling(level, pos).advance(level, 20);
            }
            helper.assertTrue(sapling(level, pos).saveWithFullMetadata().getShort("v") == 304, "basic fertilizer retains twenty percent of its bonus with per-step rounding: " + sapling(level, pos).saveWithFullMetadata() + " / " + sapling(level, pos).status().getString());
            sapling(level, pos).advance(level, 20);
            int count = 0;
            for (int i = 0; i < 32; i++) if (level.getBlockState(pos.above(i)).is(Blocks.OAK_LOG)) count++;
            int branchLogs = 0;
            for (int y = 0; y < 36; y++) for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL)
                if (level.getBlockState(pos.above(y).relative(direction)).is(Blocks.OAK_LOG)) branchLogs++;
            helper.assertTrue(branchLogs >= 1 && count + branchLogs * CultivatedOakBlockEntity.SMALL_BRANCH_COST <= 15,
                    "mandatory oak branches spend the same yield budget");
            helper.assertTrue(count >= 13 && count <= 14, "fertilized tree places a budgeted trunk with branches, actual=" + count);
            helper.succeed();
        });
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void acceleratedAndMarginalClimate(GameTestHelper helper) {
        var level = helper.getLevel(); BlockPos pos = plant(helper);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                level.getMaxLocalRawBrightness(pos.above()) >= 9, "fixture light must propagate before growth checks"))
                .thenExecute(() -> {
            level.setBlock(pos.below(), fertilizer(FertilizerType.ACCELERATED), 3);
            for (int i = 0; i < 8; i++) sapling(level, pos).advance(level, 20);
            helper.assertTrue(sapling(level, pos).saveWithFullMetadata().getShort("p") == 256 && sapling(level, pos).saveWithFullMetadata().getShort("v") == 256, "acceleration halves growth opportunities without multiplying yield");
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(pos, FHBlocks.CULTIVATED_OAK.getDefaultState(), 3);
            level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 3);
            for (int i = 0; i < 16; i++) sapling(level, pos).advance(level, 5);
            helper.assertTrue(CultivatedOakProfile.get().earnedHeight(1.0 * sapling(level, pos).saveWithFullMetadata().getShort("v") / sapling(level, pos).saveWithFullMetadata().getShort("p")) == 4, "marginal temperature reduces yield to lower height bound");
            helper.succeed();
        });
    }
    /** Outer canopy obstacles are allowed, but vanilla's 3x3 upper core must stay clear. */
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void vanillaCoreClearance(GameTestHelper helper) {
        var level = helper.getLevel(); BlockPos pos = plant(helper);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                level.getMaxLocalRawBrightness(pos.above()) >= 9, "fixture light must propagate before growth checks"))
                .thenExecute(() -> {
            for (int i = 0; i < 16; i++) sapling(level, pos).advance(level, 20);
            BlockPos outer = pos.offset(2, 3, 0), core = pos.offset(1, 3, 0);
            level.setBlock(outer, Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(core, Blocks.STONE.defaultBlockState(), 3);
            var before = sapling(level, pos).saveWithFullMetadata();
            sapling(level, pos).advance(level, 20);
            helper.assertTrue(level.getBlockState(pos).is(FHBlocks.CULTIVATED_OAK.get()),
                    "vanilla upper core obstruction must retain mature sapling");
            var after = sapling(level, pos).saveWithFullMetadata();
            helper.assertTrue(after.getShort("v") == before.getShort("v")
                    && after.getShort("p") == before.getShort("p"), "failed core check preserves growth target");
            level.setBlock(core, Blocks.AIR.defaultBlockState(), 3);
            sapling(level, pos).advance(level, 20);
            for (int y = 0; y < 6; y++) helper.assertTrue(level.getBlockState(pos.above(y)).is(Blocks.OAK_LOG),
                    "clear core must generate full-height trunk despite outer canopy stone; block=" + level.getBlockState(pos));
            helper.assertTrue(level.getBlockState(outer).is(Blocks.STONE), "foliage must not overwrite outer obstacle");
            helper.succeed();
        });
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void compactLedgerMigrationAndTickEntry(GameTestHelper helper) {
        var level = helper.getLevel(); BlockPos pos = plant(helper);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                level.getMaxLocalRawBrightness(pos.above()) >= 9, "fixture light must propagate before growth checks"))
                .thenExecute(() -> {
            var legacy = new net.minecraft.nbt.CompoundTag();
            legacy.putDouble("Progress", 4.25); legacy.putDouble("YieldCredit", 6.125);
            legacy.putInt("TargetHeight", 12); legacy.putLong("TreeSeed", 42);
            sapling(level, pos).load(legacy);
            var saved = sapling(level, pos).saveWithFullMetadata();
            helper.assertTrue(saved.contains("p", net.minecraft.nbt.Tag.TAG_SHORT)
                    && saved.contains("v", net.minecraft.nbt.Tag.TAG_SHORT), "both ledgers must be short tags: " + saved);
            helper.assertTrue(saved.getShort("p") == 68 && saved.getShort("v") == 98, "legacy fractions survive fixed-point migration");
            for (String key : java.util.List.of("Progress", "YieldCredit", "TargetHeight", "TargetLogs", "TreeSeed"))
                helper.assertTrue(!saved.contains(key), "obsolete ledger/target fields must not be saved: " + key);
            var status = (net.minecraft.network.chat.contents.TranslatableContents)sapling(level, pos).status().getContents();
            helper.assertTrue(status.getArgs().length == 3, "thermometer reports only age, progress and waiting reason");
            helper.assertTrue(saved.getInt("y") == pos.getY(), "short ledger must not overwrite the reserved y coordinate");
            var packet = com.teammoeg.frostedheart.content.climate.network.SoilThermometerUpdatePacket.sample(level, pos);
            var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                packet.encode(buffer);
                var decoded = new com.teammoeg.frostedheart.content.climate.network.SoilThermometerUpdatePacket(buffer);
                helper.assertTrue(decoded.pos().equals(pos) && decoded.plantInfo().getString().equals(sapling(level, pos).status().getString()),
                        "thermometer packet round-trip carries the server's matching sapling status");
            } finally { buffer.release(); }
            // The real entry must take the Forge area guard and temperature-query path without exceptions.
            FHBlocks.CULTIVATED_OAK.get().randomTick(level.getBlockState(pos), level, pos, level.random);
            helper.succeed();
        });
    }

}
