/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.network.InfraredBrickCodec;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.InfraredSnapshot;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;

import static com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ContinuousAirProductionGameTests.room;
import static com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ContinuousAirProductionGameTests.useItem;

/** Replays the reported world actions while retaining two independent infrared client baselines. */
@GameTestHolder("frostedheart_repro")
@PrefixGameTestTemplate(false)
public final class CampfireInfraredLifecycleReproGameTests {
    private CampfireInfraredLifecycleReproGameTests() {}

    @GameTest(template = "phase0a_empty", batch = "a_open_campfire_repro", timeoutTicks = 2450)
    public static void openCampfireBurnoutAndReplacementKeepsInfraredMaterial(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos fire = new BlockPos(1024, 81, 1024);
        level.setChunkForced(fire.getX() >> 4, fire.getZ() >> 4, true);
        BlockPos floor = fire.below();
        level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(floor.east(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        fuel(helper, fire);
        useItem(helper, fire, Items.FLINT_AND_STEEL);
        ServerPlayer player = FakePlayerFactory.getMinecraft(level);
        player.setPos(fire.getX() + .5, fire.getY(), fire.getZ() + .5);
        BlockPos[] observed = {floor, floor.east(), floor.east(8)};
        DisplayProbe fast = new DisplayProbe("open_transition5tick", observed);
        DisplayProbe client = new DisplayProbe("open_40tick", observed);
        String[] stage = {"burning"};

        for (int delay = 40; delay <= 2400; delay += 5) {
            boolean transition = delay <= 200 || delay >= 1980 && delay <= 2160;
            if ((delay - 40) % 40 != 0 && !transition) continue;
            int at = delay;
            helper.runAfterDelay(delay, () -> {
                fast.capture(player, stage[0], at);
                if ((at - 40) % 40 == 0) client.capture(player, stage[0], at);
            });
        }
        helper.runAfterDelay(101, () -> {
            ((ICampfireExtra) level.getBlockEntity(fire)).setLifeTime(1);
            stage[0] = "burning_out";
            FHMain.LOGGER.info("INFRARED_REPRO_ACTION,tick={},stage=open_{}", level.getGameTime(), stage[0]);
        });
        helper.runAfterDelay(106, () -> {
            helper.assertTrue(!level.getBlockState(fire).getValue(CampfireBlock.LIT),
                    "Open campfire must naturally extinguish when fuel runs out");
            stage[0] = "extinguished";
            FHMain.LOGGER.info("INFRARED_REPRO_ACTION,tick={},stage=open_{}", level.getGameTime(), stage[0]);
        });
        helper.runAfterDelay(2001, () -> {
            helper.assertTrue(player.gameMode.destroyBlock(fire), "Player must break the old open campfire");
            stage[0] = "broken";
            FHMain.LOGGER.info("INFRARED_REPRO_ACTION,tick={},stage=open_{}", level.getGameTime(), stage[0]);
        });
        helper.runAfterDelay(2041, () -> {
            placeCampfire(helper, player, fire);
            fuel(helper, fire);
            stage[0] = "replaced_unlit";
            FHMain.LOGGER.info("INFRARED_REPRO_ACTION,tick={},stage=open_{}", level.getGameTime(), stage[0]);
        });
        helper.runAfterDelay(2081, () -> {
            useItem(helper, fire, Items.FLINT_AND_STEEL);
            stage[0] = "relit";
            FHMain.LOGGER.info("INFRARED_REPRO_ACTION,tick={},stage=open_{}", level.getGameTime(), stage[0]);
        });
        helper.runAfterDelay(2405, () -> {
            fast.report();
            client.report();
            helper.assertTrue(fast.hasValidWall() && client.hasValidWall(),
                    "Open campfire must publish its real floor material");
            helper.assertTrue(fast.lostWall == 0 && client.lostWall == 0,
                    "Unchanged open floor material must stay visible through campfire replacement");
            useItem(helper, fire, Items.IRON_SHOVEL);
            level.setChunkForced(fire.getX() >> 4, fire.getZ() >> 4, false);
            helper.succeed();
        });
    }

    @GameTest(template = "phase0a_empty", batch = "b_room_campfire_repro", timeoutTicks = 2450)
    public static void extinguishBreakPlaceAndRelight(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos fire = room(helper, helper.absolutePos(BlockPos.ZERO));
        BlockPos wall = fire.above().east(2);
        BlockPos floor = fire.below();
        BlockPos otherPage = fire.east(8);
        level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        useItem(helper, fire, Items.FLINT_AND_STEEL);
        ServerPlayer player = FakePlayerFactory.getMinecraft(level);
        player.setPos(fire.getX() + .5, fire.getY(), fire.getZ() + .5);
        BlockPos[] observed = {wall, floor, otherPage};
        DisplayProbe fast = new DisplayProbe("transition5tick", observed);
        DisplayProbe client = new DisplayProbe("40tick", observed);
        String[] stage = {"burning"};

        for (int delay = 200; delay <= 2400; delay += 5) {
            boolean transition = delay >= 590 && delay <= 660
                    || delay >= 1980 && delay <= 2160;
            if ((delay - 200) % 40 != 0 && !transition) continue;
            int at = delay;
            helper.runAfterDelay(delay, () -> {
                fast.capture(player, stage[0], at);
                if ((at - 200) % 40 == 0) client.capture(player, stage[0], at);
            });
        }
        helper.runAfterDelay(601, () -> {
            ((ICampfireExtra) level.getBlockEntity(fire)).setLifeTime(1);
            stage[0] = "burning_out";
            FHMain.LOGGER.info("INFRARED_REPRO_ACTION,tick={},stage={}", level.getGameTime(), stage[0]);
        });
        helper.runAfterDelay(606, () -> {
            helper.assertTrue(!level.getBlockState(fire).getValue(CampfireBlock.LIT),
                    "Campfire must naturally extinguish when fuel runs out");
            stage[0] = "extinguished";
            FHMain.LOGGER.info("INFRARED_REPRO_ACTION,tick={},stage={}", level.getGameTime(), stage[0]);
        });
        helper.runAfterDelay(2001, () -> {
            helper.assertTrue(player.gameMode.destroyBlock(fire), "Player must break the old campfire");
            helper.assertTrue(level.getBlockState(fire).isAir(), "Old campfire position must be air");
            stage[0] = "broken";
            FHMain.LOGGER.info("INFRARED_REPRO_ACTION,tick={},stage={}", level.getGameTime(), stage[0]);
        });
        helper.runAfterDelay(2041, () -> {
            placeCampfire(helper, player, fire);
            fuel(helper, fire);
            stage[0] = "replaced_unlit";
            FHMain.LOGGER.info("INFRARED_REPRO_ACTION,tick={},stage={}", level.getGameTime(), stage[0]);
        });
        helper.runAfterDelay(2081, () -> {
            useItem(helper, fire, Items.FLINT_AND_STEEL);
            helper.assertTrue(level.getBlockState(fire).getValue(CampfireBlock.LIT),
                    "Flint and steel must ignite the replacement campfire");
            stage[0] = "relit";
            FHMain.LOGGER.info("INFRARED_REPRO_ACTION,tick={},stage={}", level.getGameTime(), stage[0]);
        });
        helper.runAfterDelay(2405, () -> {
            fast.report();
            client.report();
            helper.assertTrue(fast.hasValidWall() && client.hasValidWall(),
                    "Both infrared baselines must observe real wall material");
            helper.assertTrue(fast.wallValue() != InfraredBrickCodec.INVALID_TEMPERATURE
                            && client.wallValue() != InfraredBrickCodec.INVALID_TEMPERATURE,
                    "Both infrared baselines must recover after relighting");
            helper.assertTrue(fast.lostWall == 0 && client.lostWall == 0,
                    "Unchanged wall material must stay visible throughout campfire replacement");
            helper.succeed();
        });
    }

    private static void placeCampfire(GameTestHelper helper, ServerPlayer player, BlockPos fire) {
        ItemStack previous = player.getMainHandItem();
        ItemStack held = new ItemStack(Items.CAMPFIRE);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        try {
            BlockPos base = fire.below();
            var hit = new BlockHitResult(Vec3.atCenterOf(base.above()), Direction.UP, base, false);
            helper.assertTrue(held.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(),
                    "Player must place the replacement campfire item");
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, previous);
        }
        helper.assertTrue(helper.getLevel().getBlockState(fire).is(Blocks.CAMPFIRE)
                        && !helper.getLevel().getBlockState(fire).getValue(CampfireBlock.LIT),
                "Replacement campfire must start unlit");
    }

    private static void fuel(GameTestHelper helper, BlockPos fire) {
        var fuel = helper.getLevel().getBlockEntity(fire).getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(
                () -> new IllegalStateException("Campfire has no fuel capability"));
        helper.assertTrue(fuel.insertItem(0, new ItemStack(Items.COAL, 4), false).isEmpty(),
                "Campfire must accept real fuel");
    }

    private static final class DisplayProbe {
        private final String cadence;
        private final BlockPos[] positions;
        private final short[] displayed;
        private final short[] decoded = new short[64];
        private final InfraredBrickCodec.Decoder decoder = new InfraredBrickCodec.Decoder();
        private final long[] presence = new long[12];
        private final MaterialSample material = new MaterialSample();
        private long[] fields = new long[0];
        private long generation, center, storedEpoch, storedSampleTick;
        private int epoch, lostWall, recoveredWall;
        private boolean readable, initialized, hasValidWall, wallMissing, everStored;
        private MaterialSample.Source lastMaterialSource = MaterialSample.Source.UNAVAILABLE;

        private DisplayProbe(String cadence, BlockPos[] positions) {
            this.cadence = cadence;
            this.positions = positions;
            displayed = new short[positions.length];
            Arrays.fill(displayed, InfraredBrickCodec.INVALID_TEMPERATURE);
        }

        private void capture(ServerPlayer player, String stage, int elapsed) {
            boolean materialAvailable = MinecraftThermalInput.sampleMaterial(
                    player.serverLevel(), positions[0], material);
            MaterialSample.Source source = materialAvailable ? material.source() : MaterialSample.Source.UNAVAILABLE;
            if (source == MaterialSample.Source.STORED) everStored = true;
            if (source != lastMaterialSource) {
                FHMain.LOGGER.info("INFRARED_REPRO_SOURCE,cadence={},elapsed={},stage={},source={},encoded={}",
                        cadence, elapsed, stage, source, displayed[0]);
                lastMaterialSource = source;
            }
            InfraredSnapshot snapshot = MinecraftThermalInput.gameplayInfraredSnapshot(
                    player, !initialized, generation, center, epoch, presence, readable,
                    fields, storedEpoch, storedSampleTick);
            if (snapshot == null) return;
            short priorWall = displayed[0];
            boolean priorReadable = readable;
            if (snapshot.full()) Arrays.fill(displayed, InfraredBrickCodec.INVALID_TEMPERATURE);
            for (byte[] bytes : snapshot.brickRecords()) {
                FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
                try {
                    int address;
                    while ((address = decoder.readRecord(buffer, decoded)) >= 0) {
                        for (int i = 0; i < positions.length; i++) {
                            if (brickAddress(snapshot, positions[i]) == address)
                                displayed[i] = decoded[blockIndex(positions[i])];
                        }
                    }
                } finally {
                    buffer.release();
                }
            }
            boolean pagePresent = pagePresent(snapshot, positions[0],
                    snapshot.presence().length == 0 ? presence : snapshot.presence());
            initialized = true;
            generation = snapshot.generation();
            center = SectionPos.asLong(snapshot.centerChunkX(), snapshot.centerSectionY(), snapshot.centerChunkZ());
            epoch = snapshot.infraredEpoch();
            if (snapshot.presence().length != 0)
                System.arraycopy(snapshot.presence(), 0, presence, 0, presence.length);
            fields = snapshot.fieldPages();
            storedEpoch = snapshot.storedEpoch();
            storedSampleTick = snapshot.storedSampleTick();
            readable = snapshot.readable();
            if (displayed[0] != InfraredBrickCodec.INVALID_TEMPERATURE) {
                hasValidWall = true;
                if (wallMissing) recoveredWall++;
                wallMissing = false;
            } else if (hasValidWall && !wallMissing) {
                lostWall++;
                wallMissing = true;
            }
            if (elapsed % 40 == 0 || priorWall != displayed[0] &&
                    (priorWall == InfraredBrickCodec.INVALID_TEMPERATURE
                            || displayed[0] == InfraredBrickCodec.INVALID_TEMPERATURE)
                    || priorReadable != readable) {
                var level = player.serverLevel();
                FHMain.LOGGER.info(
                        "INFRARED_REPRO_SAMPLE,cadence={},elapsed={},tick={},stage={},full={},readable={},epoch={},wallPagePresent={},wallEncoded={},floorEncoded={},otherPageEncoded={},wallMaterialC={},wallMaterialSource={},lost={},recovered={}",
                        cadence, elapsed, level.getGameTime(), stage, snapshot.full(), snapshot.readable(),
                        snapshot.infraredEpoch(), pagePresent, displayed[0], displayed[1], displayed[2],
                        WorldTemperature.material(level, positions[0]), source,
                        lostWall, recoveredWall);
            }
        }

        private void report() {
            FHMain.LOGGER.info("INFRARED_REPRO_RESULT,cadence={},validWallSeen={},storedSeen={},lostWall={},recoveredWall={},finalWallEncoded={}",
                    cadence, hasValidWall, everStored, lostWall, recoveredWall, displayed[0]);
        }

        private boolean hasValidWall() { return hasValidWall; }
        private short wallValue() { return displayed[0]; }

        private static int brickAddress(InfraredSnapshot snapshot, BlockPos pos) {
            int x = (pos.getX() >> 4) - snapshot.centerChunkX() + 4;
            int y = (pos.getY() >> 4) - snapshot.centerSectionY() + 4;
            int z = (pos.getZ() >> 4) - snapshot.centerChunkZ() + 4;
            if (x < 0 || x >= 9 || y < 0 || y >= 9 || z < 0 || z >= 9) return -1;
            int page = x + 9 * (z + 9 * y);
            return page * 64 + ((pos.getX() & 15) >> 2)
                    + (((pos.getZ() & 15) >> 2) << 2) + (((pos.getY() & 15) >> 2) << 4);
        }

        private static int blockIndex(BlockPos pos) {
            return (pos.getX() & 3) | ((pos.getZ() & 3) << 2) | ((pos.getY() & 3) << 4);
        }

        private static boolean pagePresent(InfraredSnapshot snapshot, BlockPos pos, long[] mask) {
            int address = brickAddress(snapshot, pos);
            if (address < 0) return false;
            int page = address >>> 6;
            return (mask[page >>> 6] & 1L << (page & 63)) != 0;
        }
    }
}
