/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.network.InfraredBrickCodec;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.InfraredSnapshot;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ContinuousAirProductionGameTests.room;
import static com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ContinuousAirProductionGameTests.useItem;

@GameTestHolder("frostedheart_production")
@PrefixGameTestTemplate(false)
public final class ContinuousAirInfraredProductionGameTests {
    private ContinuousAirInfraredProductionGameTests() {}

    @GameTest(template = "phase0a_empty", batch = "thermal_infrared_production", timeoutTicks = 2000)
    public static void twoRealCampfiresKeepBothMaterialPagesVisibleThroughReplacement(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        BlockPos nearFire = room(helper, origin);
        BlockPos farFire = room(helper, origin.east(32));
        BlockPos nearWall = nearFire.above().east(2);
        BlockPos farWall = farFire.above().east(2);
        level.setChunkForced(farFire.getX() >> 4, farFire.getZ() >> 4, true);
        level.setBlockAndUpdate(nearWall, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(farWall, Blocks.STONE.defaultBlockState());
        useItem(helper, nearFire, Items.FLINT_AND_STEEL);
        useItem(helper, farFire, Items.FLINT_AND_STEEL);
        ServerPlayer player = FakePlayerFactory.getMinecraft(level);
        player.setPos(nearFire.getX() + .5, nearFire.getY(), nearFire.getZ() + .5);
        MaterialSample material = new MaterialSample();

        helper.startSequence().thenIdle(200).thenWaitUntil(() -> {
            InfraredSnapshot snapshot = snapshot(player);
            if (level.getGameTime() % 200 == 0) com.teammoeg.frostedheart.FHMain.LOGGER.info(
                    "THERMAL_ARCHITECTURE_IR,tick={},readable={},nearEncoded={},nearC={},farEncoded={},farC={},worker={}",
                    level.getGameTime(), snapshot != null && snapshot.readable(),
                    snapshot == null ? null : infraredTemperature(snapshot, nearWall), WorldTemperature.material(level, nearWall),
                    snapshot == null ? null : infraredTemperature(snapshot, farWall), WorldTemperature.material(level, farWall),
                    MinecraftThermalInput.describeWorkerProgress(level));
            helper.assertTrue(snapshot != null && snapshot.readable()
                            && matchesMaterial(snapshot, level, nearWall)
                            && matchesMaterial(snapshot, level, farWall),
                    "both real campfire Pages must render their live material temperatures");
        }).thenExecute(() -> level.setBlockAndUpdate(nearWall, Blocks.OAK_PLANKS.defaultBlockState()))
                .thenIdle(80).thenWaitUntil(() -> {
            if (level.getGameTime() % 200 == 0) {
                boolean available = MinecraftThermalInput.sampleMaterial(level, nearWall, material);
                com.teammoeg.frostedheart.FHMain.LOGGER.info(
                        "THERMAL_ARCHITECTURE_IR_REPLACEMENT,tick={},near={},available={},source={},nearChunkLoaded={},farChunkLoaded={},worker={},geometry={}",
                        level.getGameTime(), nearWall, available, material.source(),
                        level.getChunkSource().getChunkNow(nearWall.getX() >> 4, nearWall.getZ() >> 4) != null,
                        level.getChunkSource().getChunkNow(farWall.getX() >> 4, farWall.getZ() >> 4) != null,
                        MinecraftThermalInput.describeWorkerProgress(level), MinecraftThermalInput.describeGeometryInvalidations(level));
            }
            helper.assertTrue(MinecraftThermalInput.sampleMaterial(level, nearWall, material)
                            && material.source() == MaterialSample.Source.LIVE,
                    "replacement material must reach the live worker publication");
            InfraredSnapshot snapshot = snapshot(player);
            boolean nearMatches = snapshot != null && snapshot.readable() && matchesMaterial(snapshot, level, nearWall);
            boolean farMatches = nearMatches && matchesMaterial(snapshot, level, farWall);
            if (level.getGameTime() % 200 == 0) com.teammoeg.frostedheart.FHMain.LOGGER.info(
                    "THERMAL_ARCHITECTURE_IR_REPLACEMENT_RECORDS,tick={},snapshotPresent={},readable={},nearEncoded={},nearC={},nearMatches={},farEncoded={},farMatches={},reader={}",
                    level.getGameTime(), snapshot != null, snapshot != null && snapshot.readable(),
                    snapshot == null ? null : infraredTemperature(snapshot, nearWall), material.temperatureC(), nearMatches,
                    snapshot == null ? null : infraredTemperature(snapshot, farWall), farMatches,
                    MinecraftThermalInput.queryPublicationForObservation(level).describeReaderMemory());
            helper.assertTrue(nearMatches && farMatches,
                    "replacing one material must retain both infrared Pages");
        }).thenExecute(() -> {
            useItem(helper, nearFire, Items.IRON_SHOVEL);
            useItem(helper, farFire, Items.IRON_SHOVEL);
            level.setChunkForced(farFire.getX() >> 4, farFire.getZ() >> 4, false);
        }).thenSucceed();
    }

    private static InfraredSnapshot snapshot(ServerPlayer player) {
        return MinecraftThermalInput.gameplayInfraredSnapshot(
                player, true, 0, 0, 0, new long[12], false, new long[0], 0, 0);
    }

    private static boolean matchesMaterial(InfraredSnapshot snapshot, net.minecraft.server.level.ServerLevel level,
                                           BlockPos position) {
        Short encoded = infraredTemperature(snapshot, position);
        double materialC = WorldTemperature.material(level, position);
        return encoded != null && encoded != InfraredBrickCodec.INVALID_TEMPERATURE
                && Double.isFinite(materialC) && Math.abs(encoded * .25D - materialC) <= .3D;
    }

    private static Short infraredTemperature(InfraredSnapshot snapshot, BlockPos position) {
        int pageX = (position.getX() >> 4) - snapshot.centerChunkX() + 4;
        int pageY = (position.getY() >> 4) - snapshot.centerSectionY() + 4;
        int pageZ = (position.getZ() >> 4) - snapshot.centerChunkZ() + 4;
        if (pageX < 0 || pageX >= 9 || pageY < 0 || pageY >= 9 || pageZ < 0 || pageZ >= 9) return null;
        int address = (pageX + 9 * (pageZ + 9 * pageY)) * 64
                + ((position.getX() & 15) >> 2)
                + (((position.getZ() & 15) >> 2) << 2)
                + (((position.getY() & 15) >> 2) << 4);
        int block = (position.getX() & 3) | ((position.getZ() & 3) << 2) | ((position.getY() & 3) << 4);
        short[] values = new short[64];
        var decoder = new InfraredBrickCodec.Decoder();
        for (byte[] bytes : snapshot.brickRecords()) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
            try {
                int record;
                while ((record = decoder.readRecord(buffer, values)) >= 0) {
                    if (record == address) return values[block];
                }
            } finally {
                buffer.release();
            }
        }
        return null;
    }
}
