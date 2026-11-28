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
        BlockPos pos = CultivatedTestEnvironment.origin(helper);
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
    public static void thermometerSnapshotProtectionAndEmptyTarget(GameTestHelper helper) {
        var level = helper.getLevel();BlockPos pos = plant(helper);
        level.setBlock(pos.below(), fertilizer(FertilizerType.PRESERVED)
                .setValue(FertilizedDirt.GRADE, FertilizerGrade.ADVANCED), 3);
        var packet = com.teammoeg.frostedheart.content.climate.network.SoilThermometerUpdatePacket.sample(level, pos);
        helper.assertTrue(packet.treeInfo() != null && packet.treeInfo().minTemperature() == -5
                && packet.treeInfo().idealMin() == 15 && packet.treeInfo().idealMax() == 25
                && packet.treeInfo().requiredProgress() == 16, "server supplies protected range and actual growth scale");
        level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
        var empty = com.teammoeg.frostedheart.content.climate.network.SoilThermometerUpdatePacket.sample(level, pos);
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            empty.encode(buffer);
            var decoded = new com.teammoeg.frostedheart.content.climate.network.SoilThermometerUpdatePacket(buffer);
            helper.assertTrue(decoded.treeInfo() == null && decoded.plantInfo() == null
                    && !decoded.exposedToBadWeather() && Float.isNaN(decoded.temperature()),
                    "non-tree response clears both snapshot fields after switching target");
        } finally { buffer.release(); }
        level.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), 3);
        level.setBlock(pos, Blocks.WHEAT.defaultBlockState(), 3);
        var crop = com.teammoeg.frostedheart.content.climate.network.SoilThermometerUpdatePacket.sample(level, pos);
        buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            crop.encode(buffer);
            var decoded = new com.teammoeg.frostedheart.content.climate.network.SoilThermometerUpdatePacket(buffer);
            helper.assertTrue(decoded.pos().equals(pos) && decoded.temperature() == crop.temperature()
                    && decoded.exposedToBadWeather() == crop.exposedToBadWeather()
                    && decoded.treeInfo() == null && decoded.plantInfo() == null,
                    "crop temperature/weather and absent tree snapshot survive the combined packet round trip");
        } finally { buffer.release(); }
        helper.succeed();
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
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
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
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
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
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
            level.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 3);
            for (int i = 0; i < 5; i++) sapling(level, pos).advance(level, 20);
            helper.assertTrue(level.getBlockState(pos).is(FHBlocks.CULTIVATED_OAK.get()), "obstacle retains sapling");
            helper.assertTrue(level.getBlockState(pos.below()).getValue(FertilizedDirt.STORAGE) == 8, "retry spends no fertilizer");
            helper.assertTrue(sapling(level, pos).saveWithFullMetadata().getShort("p") == 256 && sapling(level, pos).saveWithFullMetadata().getShort("v") == 256, "late fertilizer cannot change target");
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
            sapling(level, pos).advance(level, 20);
            int count = 0;
            for (int i = 0; i < 32; i++) if (level.getBlockState(pos.above(i)).is(Blocks.OAK_LOG)) count++;
            helper.assertTrue(count >= 3 && count <= 10, "native Fancy trunk shares the expected yield with complete branches, actual=" + count);
            helper.assertTrue(level.getBlockState(pos.below()).getValue(FertilizedDirt.STORAGE) == 4, "successful generation settles the legacy four-unit fertilizer cost");
            helper.assertTrue(Blocks.OAK_SAPLING.defaultBlockState().hasProperty(net.minecraft.world.level.block.SaplingBlock.STAGE),
                    "vanilla sapling retains original stage property");
            helper.succeed();
        });
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void temperatureAndProtection(GameTestHelper helper) {
        var level = helper.getLevel(); BlockPos pos = plant(helper);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                level.getMaxLocalRawBrightness(pos.above()) >= 9, "fixture light must propagate before growth checks"))
                .thenExecute(() -> {
            level.setBlock(pos.below(), fertilizer(FertilizerType.INCREASING), 3);
            sapling(level, pos).advance(level, 0);
            helper.assertTrue(sapling(level, pos).saveWithFullMetadata().getShort("p") == 16, "cold band retains full speed");
            helper.assertTrue(level.getBlockState(pos.below()).getValue(FertilizedDirt.STORAGE) == 8, "cold progress does not spend fertilizer before successful placement");
            level.setBlock(pos.below(), fertilizer(FertilizerType.PRESERVED), 3);
            sapling(level, pos).advance(level, 0);
            helper.assertTrue(sapling(level, pos).saveWithFullMetadata().getShort("p") > 0, "preservation extends cold range: " + sapling(level, pos).saveWithFullMetadata() + " / " + sapling(level, pos).status().getString());
            helper.assertTrue(level.getBlockState(pos.below()).getValue(FertilizedDirt.STORAGE) == 8, "preserved growth keeps fertilizer until successful placement");
            helper.succeed();
        });
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
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
            int allLogs = 0;
            for (int y = 0; y < 30; y++) for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++)
                if (level.getBlockState(pos.offset(x, y, z)).is(Blocks.OAK_LOG)) allLogs++;
            helper.assertTrue(allLogs > count && allLogs >= 8 && allLogs <= 25,
                    "fertilized oak uses complete native Fancy branches around fifteen expected logs: " + allLogs);
            helper.succeed();
        });
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
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
    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
    public static void vanillaCoreClearance(GameTestHelper helper) {
        var level = helper.getLevel(); BlockPos pos = plant(helper);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                level.getMaxLocalRawBrightness(pos.above()) >= 9, "fixture light must propagate before growth checks"))
                .thenExecute(() -> {
            for (int i = 0; i < 16; i++) sapling(level, pos).advance(level, 20);
            BlockPos outer = pos.offset(8, 3, 0), core = pos.above();
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
            for (int y = 0; y < 3; y++) helper.assertTrue(level.getBlockState(pos.above(y)).is(Blocks.OAK_LOG),
                    "clear core must generate full-height trunk despite outer canopy stone; block=" + level.getBlockState(pos));
            helper.assertTrue(level.getBlockState(outer).is(Blocks.STONE), "foliage must not overwrite outer obstacle");
            helper.succeed();
        });
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 600)
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
                helper.assertTrue(decoded.treeInfo().equals(sapling(level, pos).thermometerInfo()),
                        "crop-style thermometer fields survive network round trip without exposing yield credit");
            } finally { buffer.release(); }
            // The real entry must take the Forge area guard and temperature-query path without exceptions.
            FHBlocks.CULTIVATED_OAK.get().randomTick(level.getBlockState(pos), level, pos, level.random);
            helper.succeed();
        });
    }

}
