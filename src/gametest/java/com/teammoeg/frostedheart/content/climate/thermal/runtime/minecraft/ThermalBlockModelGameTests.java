/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftSignatureCapture;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionEngine;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionLimits;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceBatch;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSolver;
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.topology.ThermalTopologyParameters;
import com.teammoeg.frostedheart.content.climate.thermal.topology.FarFieldSettings;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ThermalLoadedWorldGameTests.*;

/** Real world capture, source binding, worker publication and chunk checkpoint. */
@GameTestHolder(FHMain.MODID)
@PrefixGameTestTemplate(false)
public final class ThermalBlockModelGameTests {
    @GameTest(template = "phase0a_empty", batch = "thermal_block_world_faces", timeoutTicks = 120)
    public static void capturedWorldFacesPreserveResistanceAreaAndEnthalpyAcrossPages(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 6, 2));
        BlockPos origin = new BlockPos((anchor.getX() & ~15) + 12,
                (anchor.getY() & ~3) + 4, anchor.getZ() & ~3);
        fill(level, origin, Blocks.STONE.defaultBlockState());
        BlockPos material = origin.offset(3, 1, 1);
        level.setBlockAndUpdate(material.below(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(1, 2, 2), Blocks.OAK_STAIRS.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(2, 2, 2), Blocks.MOVING_PISTON.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(3, 2, 2), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(4, 2, 2), Blocks.OAK_STAIRS.defaultBlockState());
        var profiles = MinecraftThermalProfiles.prepare();
        var capture = new MinecraftSignatureCapture(level, profiles.states(), profiles.signatures());
        ThermalCellArena arena = new ThermalCellArena(256);
        QueryPublication query = QueryPublication.tryCreate(new ThermalMemoryBudget(8L << 20)
                .createDimensionBudget(8L << 20), 256, 16);
        var tuning = profiles.tuning();
        var engine = new ThermalDimensionEngine(1, 0, arena, profiles.signatures(), profiles.materials(),
                new ThermalTopologyParameters(tuning.airHeatCapacityJPerBlockK(), 0,
                        tuning.airMixingWPerBlockK(),
                        new BuoyancyConductance.Parameters(0.25, 4, 10), 1024, 8),
                new FarFieldSettings(tuning.farFieldConductanceWPerK(), 32, 16), tuning.campfire(),
                new ThermalDimensionLimits(16, 128, 256, 4096, 2048, 4096, 4096, 4096, 200, 1e-6), query);
        ThermalPageHandle[] pages = {new ThermalPageHandle(SectionPos.asLong(origin), 1),
                new ThermalPageHandle(SectionPos.asLong(origin.east(4)), 1)};
        int[] bricks = {brickIndex(origin), brickIndex(origin.east(4))};
        byte[] sky = new byte[256];
        java.util.Arrays.fill(sky, (byte) 16);
        try {
            ThermalInputBatch.PageAdmission[] admissions = new ThermalInputBatch.PageAdmission[2];
            for (int i = 0; i < 2; i++) {
                long mask = 1L << bricks[i];
                admissions[i] = new ThermalInputBatch.PageAdmission(pages[i], 0, mask, mask,
                        capture.captureBricks(pages[i].sectionKey(), null, capture.unresolvedPage(), mask), 0, sky, null);
            }
            complete(helper, engine.process(cut(1, admissions, ResolvedGeometryBatch.EMPTY)));
            ThermalSolver solver = read(engine, "solver");
            int a = transportSlot(pages[0], origin.offset(1, 2, 2));
            int b = transportSlot(pages[0], origin.offset(2, 2, 2));
            int c = transportSlot(pages[0], origin.offset(3, 2, 2));
            int d = transportSlot(pages[1], origin.offset(4, 2, 2));
            var pairs = solver.fragment(pages[0].currentPublication().workerPageSlot() * 64 + bricks[0]).airPairs();
            near(helper, 0.75 * tuning.airMixingWPerBlockK(), conductance(pairs, a, b), "75/75 face resistance");
            near(helper, 6.0 / 7 * tuning.airMixingWPerBlockK(), conductance(pairs, b, c), "75/100 internal face resistance");
            near(helper, 6.0 / 7 * tuning.airMixingWPerBlockK(), conductance(pairs, c, d), "100/75 cross-Page face resistance");
            int materialSlot = materialSlot(pages[0], material);
            int profile = profiles.signatures().materialProfileId(profiles.states().signatureId(level.getBlockState(material)));
            double faceCapacity = profiles.materials().profileOrNull(profile).surfaceCapacityJPerK();
            near(helper, faceCapacity, arena.capacityJPerK(materialSlot), "one exposed face must provide one face capacity");
            arena.addEnthalpyJ(materialSlot, 10_000);
            double before = totalEnthalpy(arena);
            BlockPos newlyExposed = material.east();
            level.setBlockAndUpdate(newlyExposed, Blocks.AIR.defaultBlockState());
            ResolvedGeometryBatch.Builder geometry = new ResolvedGeometryBatch.Builder();
            geometry.addResolvedCenter(pages[1], pages[1].beginGeometryMutation(), pageBlock(newlyExposed),
                    capture.resolveSignatureId(newlyExposed.getX(), newlyExposed.getY(), newlyExposed.getZ()));
            complete(helper, engine.process(cut(2, ThermalInputBatch.NO_ADMISSIONS, geometry.buildAndReset())));
            near(helper, faceCapacity * 2, arena.capacityJPerK(materialSlot(pages[0], material)),
                    "a neighbor-only Page mutation must update exposed capacity");
            near(helper, before, totalEnthalpy(arena), "topology migration and internal exchange must conserve retained enthalpy");

            fill(level, origin, Blocks.AIR.defaultBlockState());
            for (int i = 0; i < 2; i++) {
                long revision = pages[i].beginGeometryMutation();
                BlockPos first = origin.east(i * 4);
                for (int block = 0; block < 64; block++) {
                    BlockPos position = first.offset(block & 3, block >>> 4, block >>> 2 & 3);
                    geometry.addResolvedCenter(pages[i], revision, pageBlock(position),
                            capture.resolveSignatureId(position.getX(), position.getY(), position.getZ()));
                }
            }
            complete(helper, engine.process(cut(3, ThermalInputBatch.NO_ADMISSIONS, geometry.buildAndReset())));
            helper.assertTrue(arena.liveCellCount() == 2, "two full-Air Bricks must have exactly two nodes");
            for (int i = 0; i < 2; i++) helper.assertTrue(pages[i].currentPublication().brick(bricks[i]).blockLayout() == null,
                    "full-Air Brick must not retain a mixed layout");
            var regular = solver.fragment(pages[0].currentPublication().workerPageSlot() * 64 + bricks[0]).airPairs();
            helper.assertTrue(regular.size() == 1, "full-Air shared face must compile one edge");
            near(helper, 4 * tuning.airMixingWPerBlockK(), regular.conductance(0), "regular face area and distance");
            FHMain.LOGGER.info("Block model production faces: 75/75 and 75/100 verified; exposed capacity {} -> {}; full-Air nodes=2, edges=1",
                    faceCapacity, 2 * faceCapacity);
            helper.succeed();
        } finally {
            engine.close();
        }
    }

    private static void fill(ServerLevel level, BlockPos origin, net.minecraft.world.level.block.state.BlockState state) {
        for (int x = 0; x < 8; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            level.setBlockAndUpdate(origin.offset(x, y, z), state);
    }
    private static int brickIndex(BlockPos pos) { return (pos.getX() & 15) >>> 2 | ((pos.getZ() & 15) >>> 2) << 2 | ((pos.getY() & 15) >>> 2) << 4; }
    private static int pageBlock(BlockPos pos) { return (pos.getX() & 15) | (pos.getZ() & 15) << 4 | (pos.getY() & 15) << 8; }
    private static int transportSlot(ThermalPageHandle page, BlockPos pos) { return page.currentPublication().resolveAirPoint(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15); }
    private static int materialSlot(ThermalPageHandle page, BlockPos pos) {
        var brick = page.currentPublication().brick(brickIndex(pos));
        return brick.coverageSlot() + brick.blockLayout().nodeAt((pos.getX() & 3) | (pos.getZ() & 3) << 2 | (pos.getY() & 3) << 4);
    }
    private static double totalEnthalpy(ThermalCellArena arena) {
        double sum = 0;
        for (int slot = arena.nextLiveSlot(0); slot >= 0; slot = arena.nextLiveSlot(slot + 1)) sum += arena.enthalpyJ(slot);
        return sum;
    }
    private static double conductance(com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalFragment.AirPairs pairs, int a, int b) {
        double sum = 0;
        for (int i = 0; i < pairs.size(); i++) if (pairs.first(i) == a && pairs.second(i) == b || pairs.first(i) == b && pairs.second(i) == a) sum += pairs.conductance(i);
        return sum;
    }
    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) < 1e-6, message + ": expected=" + expected + ", actual=" + actual);
    }
    private static void complete(GameTestHelper helper, ThermalCompletion completion) {
        helper.assertTrue(completion.status() == ThermalCompletion.Status.COMPLETED, "production engine cut failed: " + completion.failure());
    }
    private static ThermalInputBatch cut(long sequence, ThermalInputBatch.PageAdmission[] admissions, ResolvedGeometryBatch geometry) {
        return new ThermalInputBatch(1, sequence, sequence * 20, admissions, ThermalInputBatch.NO_RETIREMENTS,
                ThermalInputBatch.NO_RESIDENCY_UPDATES, geometry, ThermalSourceBatch.EMPTY,
                ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN);
    }

    @GameTest(template = "phase0a_empty", batch = "thermal_block_ventilation", timeoutTicks = 900)
    public static void stairAndUnknownBlockAcceptHeatWithoutInternalSubdivision(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos anchor = helper.absolutePos(new BlockPos(2, 4, 2));
        BlockPos target = new BlockPos((anchor.getX() & ~3) + 1,
                (anchor.getY() & ~3) + 2, (anchor.getZ() & ~3) + 1);
        for (Direction direction : Direction.values()) {
            level.setBlockAndUpdate(target.relative(direction), Blocks.STONE.defaultBlockState());
        }
        BlockPos fire = target.below();
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        ((ICampfireExtra) level.getBlockEntity(fire)).setLifeTime(20_000);
        level.setBlockAndUpdate(target, Blocks.OAK_STAIRS.defaultBlockState());
        BlockPos unknown = target.east();
        level.setBlockAndUpdate(unknown, Blocks.MOVING_PISTON.defaultBlockState());
        MinecraftThermalInput input = start(level, target);
        var profiles = MinecraftThermalProfiles.prepare();
        helper.assertTrue(profiles.signatures().ventilation(profiles.states().signatureId(level.getBlockState(target))) == 75,
                "stair must use numeric ventilation 75");
        helper.assertTrue(profiles.signatures().ventilation(profiles.states().signatureId(level.getBlockState(unknown))) == 75,
                "unsupported moving-piston shape must use numeric ventilation 75");
        QueryPublication queries = read(input, "queryPublication");
        QueryPublication.MutableSample sample = new QueryPublication.MutableSample();
        long[] previousRevision = {-1};
        helper.startSequence().thenWaitUntil(() -> {
            PagePublication publication = publication(helper, level, target);
            PagePublication.Brick brick = publication.brickAt(target.getX() & 15, target.getY() & 15, target.getZ() & 15);
            int stairSlot = publication.resolveAirPoint(target.getX() & 15, target.getY() & 15, target.getZ() & 15);
            int unknownSlot = publication.resolveAirPoint(unknown.getX() & 15, unknown.getY() & 15, unknown.getZ() & 15);
            helper.assertTrue(stairSlot >= 0 && unknownSlot >= 0 && stairSlot != unknownSlot,
                    "each ventilation-75 block must retain its own transport resistance");
            helper.assertTrue(queries.tryRead(stairSlot, brick.arenaGeneration(), publication.topologyGeneration(), sample),
                    "stair temperature must be published");
            helper.assertTrue(sample.temperatureC() > 20, "normal campfire source must heat the stair node: " + sample.temperatureC());
        }).thenExecute(() -> {
            PagePublication publication = publication(helper, level, target);
            previousRevision[0] = publication.geometryRevision();
            captureRoundTrip(helper, level, target, publication, queries, sample);
            // Two centers in one Brick exercise grouped capture; ordinary Air may
            // merge again only after the ventilation restrictions are removed.
            level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(unknown, Blocks.AIR.defaultBlockState());
        }).thenWaitUntil(() -> {
            PagePublication publication = publication(helper, level, target);
            helper.assertTrue(publication.geometryRevision() > previousRevision[0], "both block changes must reach the worker");
            int a = publication.resolveAirPoint(target.getX() & 15, target.getY() & 15, target.getZ() & 15);
            int b = publication.resolveAirPoint(unknown.getX() & 15, unknown.getY() & 15, unknown.getZ() & 15);
            helper.assertTrue(a >= 0 && a == b, "adjacent ordinary Air must merge after the actual world mutation");
        }).thenExecute(() -> {
            level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
            MinecraftThermalInput.closeActiveLevel(level);
        }).thenSucceed();
    }

    private static PagePublication publication(GameTestHelper helper, ServerLevel level, BlockPos position) {
        var handle = owner(level, position).page();
        helper.assertTrue(handle != null, "source must admit the target Page");
        var publication = handle.currentPublication();
        helper.assertTrue(publication != null, "latest world mutation must have a coherent publication");
        return publication;
    }

    private static void captureRoundTrip(GameTestHelper helper, ServerLevel level, BlockPos position,
            PagePublication publication, QueryPublication queries, QueryPublication.MutableSample sample) {
        var capture = DormantChunkThermalState.capture(publication, queries, sample, 0,
                new DormantChunkThermalState.CaptureScratch());
        helper.assertTrue(capture.valid() && capture.entry() != null, "heated production publication must be checkpointed");
        int sectionY = position.getY() >> 4;
        var state = new DormantChunkThermalState(level.getMinSection(), level.getSectionsCount());
        state.replace(sectionY, capture.entry());
        CompoundTag tag = new CompoundTag();
        state.encode(tag);
        var restored = DormantChunkThermalState.decode(tag, level.getMinSection(), level.getSectionsCount());
        CompoundTag encodedAgain = new CompoundTag();
        restored.encode(encodedAgain);
        helper.assertTrue(tag.equals(encodedAgain), "new chunk format must round-trip spatial masks and residuals");
        var sections = tag.getCompound("FrostedHeartThermal").getList("sections", 10);
        var section = sections.getCompound(0);
        int bytes = 8 * (section.getLongArray("blocks").length + section.getLongArray("residuals").length);
        helper.assertTrue(bytes <= 640, "checkpoint numeric payload exceeds the section budget: " + bytes);
        FHMain.LOGGER.info("Block model production checkpoint: {} numeric bytes, {} spatial masks", bytes,
                section.getLongArray("blocks").length);
    }
}
