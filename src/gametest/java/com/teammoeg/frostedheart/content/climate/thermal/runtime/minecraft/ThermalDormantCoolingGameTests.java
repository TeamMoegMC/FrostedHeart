/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.network.InfraredBrickCodec;
import com.teammoeg.frostedheart.content.climate.thermal.field.*;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.DormantThermalCooling;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.*;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPhaseController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import static com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPhaseController.PhaseAttempt.*;

@GameTestHolder(FHMain.MODID)
@PrefixGameTestTemplate(false)
public final class ThermalDormantCoolingGameTests {
    private static final String TEMPLATE = "phase0a_empty";

    @GameTest(template = TEMPLATE, batch = "dormant_numeric", timeoutTicks = 100)
    public static void sensibleAirAndMaterialUseTheSameClockWithoutSaveDrift(GameTestHelper helper) {
        var state = new DormantChunkThermalState(0, 1);
        state.replace(0, new DormantChunkThermalState.SectionEntry(0, 10, 1,
                new byte[]{0}, new long[]{160}, new long[0]));
        var law = MaterialThermalLaw.sensible(300);
        var sample = new QueryPublication.MutableMaterialSample();
        for (int saves = 0; saves < 20; saves++) {
            CompoundTag tag = new CompoundTag(); state.encode(tag);
            state = DormantChunkThermalState.decode(tag, 0, 1);
            helper.assertTrue(tag.getCompound("FrostedHeartThermal").getInt("version") == 4, "only v4 is written");
            for (long tick : new long[]{0, 36000, 72000}) {
                sample.setStored(6000, law, (byte) 0, 0);
                DormantThermalCooling.project(sample, tick, -20, DormantThermalCooling.rate(1800));
                double air = state.admissionCut(0, tick, 1800, -20).meanTemperatureC(0);
                near(helper, air, sample.temperatureC(), "Air and material share sensible cooling");
                near(helper, tick == 0 ? 20 : tick == 36000 ? 0 : -10, air, "half-life result without save drift");
                helper.assertTrue(sample.source() == QueryPublication.MutableMaterialSample.Source.STORED,
                        "a timestamp must not mislabel a dormant read as live");
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "dormant_latent", timeoutTicks = 100)
    public static void latentHeatUsesFluxAndStopsAtTheWorldTransition(GameTestHelper helper) {
        int target = Block.BLOCK_STATE_REGISTRY.getId(Blocks.WATER.defaultBlockState());
        var law = new MaterialThermalLaw(100, 0, new MaterialThermalLaw.Transition(target, 0, 0, 1000), null);
        double rate = DormantThermalCooling.rate(30);
        var sample = new QueryPublication.MutableMaterialSample();
        sample.setStored(-1000, law, (byte) 0, 0);
        DormantThermalCooling.project(sample, 1000, 10, rate);
        near(helper, 0, sample.temperatureC(), "latent plateau cannot be bypassed by temperature interpolation");
        near(helper, rate * 100 * 10 * 20, sample.enthalpyJ(), "30 seconds sensible, then 20 seconds latent flux");
        sample.setStored(-1000, law, (byte) 0, 0);
        DormantThermalCooling.project(sample, 20L * 86400 * 20, 10, rate);
        near(helper, 1000, sample.enthalpyJ(), "long absence stops at the uncommitted target, without a heat debt");
        sample.setStored(500, law, MaterialThermalLaw.HEATING, 0);
        DormantThermalCooling.project(sample, 100000, 0, rate);
        near(helper, 500, sample.enthalpyJ(), "a bath at the melting point supplies no latent energy");

        // Independent explicit time integration for retreat from a half-complete plateau.
        double reference = 500, step = .0005;
        for (int i = 0; i < 100000; i++) reference += rate * 100 * (-10 - (reference >= 0 ? 0 : reference / 100)) * step;
        sample.setStored(500, law, MaterialThermalLaw.HEATING, 0);
        DormantThermalCooling.project(sample, 1000, -10, rate);
        helper.assertTrue(Math.abs(reference - sample.enthalpyJ()) < .02, "analytic retreat agrees with small-step bath integration");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "dormant_material_ticks", timeoutTicks = 100)
    public static void partialEditsPreserveOtherClocksAndSharedSnapshots(GameTestHelper helper) {
        var level = helper.getLevel(); MinecraftThermalInput.closeActiveLevel(level);
        BlockPos first = center(helper), second = first.east();
        level.setBlockAndUpdate(first, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(second, Blocks.STONE.defaultBlockState());
        var original = retime(ThermalMaterialBehaviorGameTests.storedBrick(helper, first, 20), first.getY() >> 4, 0);
        var editor = new MaterialSectionState.Editor(original);
        int a = original.find(block(first)), b = original.find(block(second));
        editor.update(block(first), original.stateId(a), original.law(a), original.enthalpyJ(a), original.branch(a), 600);
        var edited = editor.snapshot();
        helper.assertTrue(original.savedTick(a) == 0 && edited.savedTick(a) == 600 && edited.savedTick(b) == 0,
                "single edit must not reset the neighbor clock or change the old snapshot");
        var container = new DormantChunkThermalState(first.getY() >> 4, 1);
        container.replaceMaterials(first.getY() >> 4, edited);
        CompoundTag tag = new CompoundTag(); container.encode(tag);
        var decoded = DormantChunkThermalState.decode(tag, first.getY() >> 4, 1).materials(first.getY() >> 4);
        var sample = new QueryPublication.MutableMaterialSample();
        decoded.read(a, decoded.law(a), 600, -20, DormantThermalCooling.rate(30), sample);
        near(helper, 20, sample.temperatureC(), "edited body starts its own interval");
        decoded.read(b, decoded.law(b), 600, -20, DormantThermalCooling.rate(30), sample);
        near(helper, -10, sample.temperatureC(), "neighbor initialized at zero cools for its full interval");
        editor.remove(block(first));
        var compact = editor.snapshot();
        helper.assertTrue(compact.savedTick(compact.find(block(second))) == 0, "tombstone compaction carries the remaining timestamp");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "dormant_phase_world", timeoutTicks = 100)
    public static void randomEntryConvertsWithoutPageAndCarriesExactEnthalpy(GameTestHelper helper) {
        var level = helper.getLevel(); MinecraftThermalInput.closeActiveLevel(level);
        BlockPos position = center(helper);
        level.setBlockAndUpdate(position, Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(position.west(), Blocks.STONE.defaultBlockState());
        var chunk = level.getChunkAt(position);
        var attachment = (MinecraftThermalChunkAttachment) (Object) chunk;
        var previous = attachment.frostedheart$getDormantThermalState();
        var seed = ThermalMaterialBehaviorGameTests.storedBrick(helper, position, 0);
        int index = seed.find(block(position));
        var law = seed.law(index); var edge = law.cooling();
        helper.assertTrue(edge != null, "water fixture needs a freezing law");
        int oldSpeed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(3, level.getServer());
        var editor = new MaterialSectionState.Editor(seed);
        var stored = new DormantChunkThermalState(position.getY() >> 4, 1);
        var field = new ThermalFieldKey(new ResourceLocation("frostedheart", "dormant_freezing_test"), 0, position.asLong(), 0);
        try {
            // Isolate this energy-handoff fixture from analytic fields left elsewhere in the shared test world.
            MinecraftGameplayFields.upsertSphere(level, field, Integer.MAX_VALUE, ThermalAnalyticField.CombineMode.ADD_DELTA,
                    position.getX() + .5, position.getY() + .5, position.getZ() + .5, 2, -1000);
            editor.update(block(position), seed.stateId(index), law,
                    (edge.sourceEnthalpyJ() + edge.targetEnthalpyJ()) / 2, MaterialThermalLaw.COOLING, level.getGameTime());
            stored.replaceMaterials(position.getY() >> 4, editor.snapshot());
            attachment.frostedheart$setDormantThermalState(stored);
            helper.assertTrue(attempt(helper, position) == DEFERRED && level.getBlockState(position).is(Blocks.WATER),
                    "an environment threshold must not bypass half-complete latent heat");
            editor.update(block(position), seed.stateId(index), law, edge.targetEnthalpyJ(), MaterialThermalLaw.COOLING, level.getGameTime());
            stored.replaceMaterials(position.getY() >> 4, editor.snapshot());
            var outcome = attempt(helper, position);
            helper.assertTrue(outcome == CHANGED, "completed dormant freezing must not require a Page: result=" + outcome
                    + ", state=" + level.getBlockState(position) + ", position=" + position
                    + ", loaded=" + level.isAreaLoaded(position, 1)
                    + ", floor=" + MinecraftGameplayFields.guaranteedFloor(level, position));
            helper.assertTrue(Block.BLOCK_STATE_REGISTRY.getId(level.getBlockState(position)) == edge.targetStateId(), "world uses the exact law target");
            var result = stored.materials(position.getY() >> 4);
            int current = result.find(block(position));
            near(helper, edge.targetEnthalpyJ(), result.enthalpyJ(current), "world mutation hands off projected H exactly once");
            helper.assertTrue(result.savedTick(current) == level.getGameTime() && result.branch(current) == 0,
                    "target body starts at the commit tick and sensible branch");
            helper.assertTrue(attempt(helper, position) != CHANGED, "same-tick retry does not replay a completed phase");
            helper.assertTrue(MinecraftThermalInput.gameplayInfraredSnapshot(FakePlayerFactory.getMinecraft(level), true,
                    0, 0, 0, new long[12], false, new long[0], 0, 0).generation() == 0, "dormant mutation must not start a worker");
        } finally {
            MinecraftGameplayFields.remove(level, field);
            attachment.frostedheart$setDormantThermalState(previous);
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(oldSpeed, level.getServer());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "dormant_phase_fields", timeoutTicks = 100)
    public static void explicitFloorCanMeltDormantIceButDeltaCannot(GameTestHelper helper) {
        var level = helper.getLevel(); MinecraftThermalInput.closeActiveLevel(level);
        BlockPos position = center(helper); level.setBlockAndUpdate(position, Blocks.ICE.defaultBlockState());
        var chunk = level.getChunkAt(position); var attachment = (MinecraftThermalChunkAttachment) (Object) chunk;
        var previous = attachment.frostedheart$getDormantThermalState();
        var seed = ThermalMaterialBehaviorGameTests.storedBrick(helper, position, -20);
        var stored = new DormantChunkThermalState(position.getY() >> 4, 1); stored.replaceMaterials(position.getY() >> 4, seed);
        ThermalFieldKey key = new ThermalFieldKey(new ResourceLocation("frostedheart", "dormant_test"), 0, position.asLong(), 0);
        int oldSpeed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(3, level.getServer());
        try {
            attachment.frostedheart$setDormantThermalState(stored);
            MinecraftGameplayFields.upsertSphere(level, key, 0, ThermalAnalyticField.CombineMode.ADD_DELTA,
                    position.getX()+.5, position.getY()+.5, position.getZ()+.5, 2, 100);
            helper.assertTrue(attempt(helper, position) == DEFERRED, "additive field cannot supply missing latent energy");
            MinecraftGameplayFields.upsertSphere(level, key, 0, ThermalAnalyticField.CombineMode.MAX_HEAT,
                    position.getX()+.5, position.getY()+.5, position.getZ()+.5, 2, 100);
            helper.assertTrue(attempt(helper, position) == CHANGED, "explicit gameplay floor works without a Page");
            var result = stored.materials(position.getY() >> 4);
            near(helper, seed.law(seed.find(block(position))).heating().targetEnthalpyJ(),
                    result.enthalpyJ(result.find(block(position))), "forced conversion accounts for its supplied energy");
        } finally {
            MinecraftGameplayFields.remove(level, key); attachment.frostedheart$setDormantThermalState(previous);
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(oldSpeed, level.getServer());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "dormant_ir_time", timeoutTicks = 100)
    public static void elapsedTimeRefreshesInfraredWithoutEditingStoredEnergy(GameTestHelper helper) {
        var level = helper.getLevel(); MinecraftThermalInput.closeActiveLevel(level);
        BlockPos position = center(helper); level.setBlockAndUpdate(position, Blocks.STONE.defaultBlockState());
        var chunk = level.getChunkAt(position); var attachment = (MinecraftThermalChunkAttachment) (Object) chunk;
        var previous = attachment.frostedheart$getDormantThermalState();
        long tick = level.getGameTime();
        var seed = ThermalMaterialBehaviorGameTests.storedBrick(helper, position, 120);
        var stored = new DormantChunkThermalState(position.getY() >> 4, 1); stored.replaceMaterials(position.getY() >> 4, seed);
        ServerPlayer player = FakePlayerFactory.getMinecraft(level); player.setPos(position.getX()+.5, position.getY(), position.getZ()+.5);
        try {
            attachment.frostedheart$setDormantThermalState(stored);
            var full = infrared(player, null);
            helper.assertTrue(infrared(player, full) == null, "same-time requests must not resend stored data");
            ((net.minecraft.world.level.storage.ServerLevelData) level.getLevelData()).setGameTime(tick + 36000);
            var delta = infrared(player, full);
            helper.assertTrue(delta != null && !delta.full() && delta.brickRecords().length != 0, "time alone refreshes material display");
            int address = 364 * 64 + ((position.getX() & 15) >>> 2) + (((position.getZ() & 15) >>> 2) << 2)
                    + (((position.getY() & 15) >>> 2) << 4);
            short[] values = new short[64]; boolean found = false;
            var decoder = new InfraredBrickCodec.Decoder();
            for (byte[] part : delta.brickRecords()) {
                FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(part));
                try {
                    int current;
                    while ((current = decoder.readRecord(buffer, values)) >= 0) if (current == address) {
                        int local = (position.getX() & 3) | (position.getZ() & 3) << 2 | (position.getY() & 3) << 4;
                        helper.assertTrue(values[local] == InfraredBrickCodec.quantize(WorldTemperature.material(level, position)),
                                "IR and thermometer share projected material temperature");
                        found = true;
                    }
                } finally { buffer.release(); }
            }
            helper.assertTrue(found, "elapsed-time delta includes the expected Brick");
            helper.assertTrue(stored.materials(position.getY() >> 4) == seed, "read-only cooling keeps the original checkpoint");
        } finally {
            ((net.minecraft.world.level.storage.ServerLevelData) level.getLevelData()).setGameTime(tick);
            attachment.frostedheart$setDormantThermalState(previous);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "dormant_phase_cost", timeoutTicks = 200)
    public static void dormantAttemptsStayReadOnlyAcrossSparseAndDenseRegions(GameTestHelper helper) {
        var level = helper.getLevel(); MinecraftThermalInput.closeActiveLevel(level);
        var allocation = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        allocation.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().getId();
        BlockPos base = center(helper);
        for (int sections : new int[]{1, 8}) for (int records : new int[]{1, 64}) {
            MinecraftThermalChunkAttachment[] attachments = new MinecraftThermalChunkAttachment[sections];
            DormantChunkThermalState[] previous = new DormantChunkThermalState[sections];
            DormantChunkThermalState[] containers = new DormantChunkThermalState[sections];
            MaterialSectionState[] checkpoints = new MaterialSectionState[sections];
            BlockPos[] positions = new BlockPos[sections * records];
            BlockPos[] corners = new BlockPos[sections];
            net.minecraft.world.level.block.state.BlockState[][] oldBlocks = new net.minecraft.world.level.block.state.BlockState[sections][64];
            try {
                for (int s = 0; s < sections; s++) {
                    BlockPos origin = base.offset(s * 16, 0, 0);
                    BlockPos corner = new BlockPos(origin.getX() & ~3, origin.getY() & ~3, origin.getZ() & ~3);
                    corners[s] = corner;
                    var chunk = level.getChunkAt(corner);
                    attachments[s] = (MinecraftThermalChunkAttachment) (Object) chunk;
                    previous[s] = attachments[s].frostedheart$getDormantThermalState();
                    attachments[s].frostedheart$setDormantThermalState(null);
                    for (int i = 0; i < 64; i++) {
                        BlockPos p = corner.offset(i & 3, i >>> 4, i >>> 2 & 3);
                        oldBlocks[s][i] = chunk.getBlockState(p);
                        level.setBlockAndUpdate(p, i < records ? Blocks.ICE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                    }
                    var seed = ThermalMaterialBehaviorGameTests.storedBrick(helper, corner, -200);
                    var editor = new MaterialSectionState.Editor(seed);
                    for (int i = 0; i < seed.size(); i++) {
                        int p = seed.position(i);
                        positions[s * records + i] = new BlockPos(chunk.getPos().getMinBlockX() + (p & 15),
                                (corner.getY() & ~15) + (p >>> 8), chunk.getPos().getMinBlockZ() + (p >>> 4 & 15));
                        editor.update(p, seed.stateId(i), seed.law(i), seed.law(i).enthalpyAtTemperature(-200), (byte) 0, level.getGameTime());
                    }
                    checkpoints[s] = retime(editor.snapshot(), corner.getY() >> 4, level.getGameTime() - 6000);
                    containers[s] = new DormantChunkThermalState(corner.getY() >> 4, 1);
                    containers[s].replaceMaterials(corner.getY() >> 4, checkpoints[s]);
                    attachments[s].frostedheart$setDormantThermalState(containers[s]);
                }
                for (int i = 0; i < 2048; i++) {
                    int at = i % positions.length;
                    MinecraftThermalInput.tryMaterialPhaseAtRandomTick(level,
                            (net.minecraft.world.level.chunk.LevelChunk) (Object) attachments[at / records], positions[at], Blocks.ICE.defaultBlockState());
                }
                long[] nanos = new long[32];
                long beforeBytes = allocation.getThreadAllocatedBytes(thread);
                int changed = 0;
                for (int round = 0; round < nanos.length; round++) {
                    long start = System.nanoTime();
                    for (int i = 0; i < 256; i++) {
                        int at = (round * 256 + i) % positions.length;
                        if (MinecraftThermalInput.tryMaterialPhaseAtRandomTick(level,
                                (net.minecraft.world.level.chunk.LevelChunk) (Object) attachments[at / records], positions[at], Blocks.ICE.defaultBlockState()) != DEFERRED) changed++;
                    }
                    nanos[round] = System.nanoTime() - start;
                }
                long bytes = allocation.getThreadAllocatedBytes(thread) - beforeBytes;
                helper.assertTrue(changed == 0, "cold dormant ice attempts remain deferred");
                for (int s = 0; s < sections; s++) helper.assertTrue(containers[s].materials(base.getY() >> 4) == checkpoints[s],
                        "random reads do not rewrite checkpoints or publish a new snapshot");
                var edit = new MaterialSectionState.Editor(checkpoints[0]);
                beforeBytes = allocation.getThreadAllocatedBytes(thread);
                edit.update(checkpoints[0].position(0), checkpoints[0].stateId(0), checkpoints[0].law(0),
                        checkpoints[0].enthalpyJ(0), (byte) 0, level.getGameTime());
                long firstWriteBytes = allocation.getThreadAllocatedBytes(thread) - beforeBytes;
                Arrays.sort(nanos);
                FHMain.LOGGER.info("Dormant random attempts: sections={}, records/section={}, p50={} ns, p95={} ns, {} bytes/attempt; first COW+mixed-time write={} bytes",
                        sections, records, nanos[16] / 256.0, nanos[30] / 256.0, bytes / 8192.0, firstWriteBytes);
            } finally {
                for (int s = 0; s < sections; s++) if (attachments[s] != null) {
                    attachments[s].frostedheart$setDormantThermalState(null);
                    for (int i = 0; i < 64; i++) if (oldBlocks[s][i] != null)
                        level.setBlockAndUpdate(corners[s].offset(i & 3, i >>> 4, i >>> 2 & 3), oldBlocks[s][i]);
                    attachments[s].frostedheart$setDormantThermalState(previous[s]);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "dormant_native_phase", timeoutTicks = 100)
    public static void snowCallbackAndSharedLavaTickRespectLatentEnergy(GameTestHelper helper) {
        var level = helper.getLevel(); MinecraftThermalInput.closeActiveLevel(level);
        BlockPos p = center(helper).above(16);
        int oldSpeed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(3, level.getServer());
        var chunk = level.getChunkAt(p); var attachment = (MinecraftThermalChunkAttachment) (Object) chunk;
        var previous = attachment.frostedheart$getDormantThermalState();
        var key = new ThermalFieldKey(new ResourceLocation("frostedheart", "native_dormant_test"), 0, p.asLong(), 0);
        try {
            level.setBlockAndUpdate(p.below(), Blocks.STONE.defaultBlockState());
            for (var state : new net.minecraft.world.level.block.state.BlockState[]{
                    Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 4), Blocks.LAVA.defaultBlockState()}) {
                attachment.frostedheart$setDormantThermalState(null);
                level.setBlockAndUpdate(p, state);
                var seed = ThermalMaterialBehaviorGameTests.storedBrick(helper, p, 0);
                int i = seed.find(block(p)); var law = seed.law(i);
                boolean heating = state.is(Blocks.SNOW);
                var edge = heating ? law.heating() : law.cooling();
                helper.assertTrue(edge != null, "native phase needs a real energy boundary: " + state);
                var editor = new MaterialSectionState.Editor(seed);
                byte branch = heating ? MaterialThermalLaw.HEATING : MaterialThermalLaw.COOLING;
                editor.update(block(p), seed.stateId(i), law, (edge.sourceEnthalpyJ() + edge.targetEnthalpyJ()) / 2, branch, level.getGameTime());
                var stored = new DormantChunkThermalState(p.getY() >> 4, 1); stored.replaceMaterials(p.getY() >> 4, editor.snapshot());
                attachment.frostedheart$setDormantThermalState(stored);
                MinecraftGameplayFields.upsertSphere(level, key, 0, ThermalAnalyticField.CombineMode.ADD_DELTA,
                        p.getX()+.5, p.getY()+.5, p.getZ()+.5, 2, heating ? 1000 : -1000);
                if (heating) state.randomTick(level, p, level.random); else attempt(helper, p);
                helper.assertTrue(level.getBlockState(p) == state, "native threshold must not bypass halfway latent energy");
                editor.update(block(p), seed.stateId(i), law, edge.targetEnthalpyJ(), branch, level.getGameTime());
                stored.replaceMaterials(p.getY() >> 4, editor.snapshot());
                if (heating) state.randomTick(level, p, level.random); else attempt(helper, p);
                helper.assertTrue(Block.BLOCK_STATE_REGISTRY.getId(level.getBlockState(p)) == edge.targetStateId(),
                        "random phase entry commits the completed energy edge: source=" + state + ", position=" + p
                                + ", actual=" + level.getBlockState(p) + ", expected=" + Block.BLOCK_STATE_REGISTRY.byId(edge.targetStateId())
                                + ", materialTemperature=" + WorldTemperature.material(level, p));
                MinecraftGameplayFields.remove(level, key);
            }
            var flow = Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 3);
            var law = MinecraftThermalProfiles.materialLaw(flow);
            helper.assertTrue(law.cooling() != null, "flowing water must retain its native freezing capability when tracked");
            var target = Block.BLOCK_STATE_REGISTRY.byId(law.cooling().targetStateId());
            near(helper, MinecraftThermalProfiles.materialLaw(target).enthalpyAtTemperature(WorldTemperature.WATER_FREEZES),
                    law.cooling().targetEnthalpyJ(), "flowing-water target shares the endpoint energy reference");
        } finally {
            MinecraftGameplayFields.remove(level, key); attachment.frostedheart$setDormantThermalState(previous);
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(oldSpeed, level.getServer());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "dormant_nested_identity", timeoutTicks = 100)
    public static void nestedReplacementDoesNotInheritTheOuterPhaseEnergy(GameTestHelper helper) throws Exception {
        var level = helper.getLevel(); MinecraftThermalInput.closeActiveLevel(level);
        int oldSpeed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(3, level.getServer());
        BlockPos p = center(helper);
        level.setBlockAndUpdate(p, Blocks.STONE.defaultBlockState());
        var chunk = level.getChunkAt(p); var attachment = (MinecraftThermalChunkAttachment) (Object) chunk;
        var previousState = attachment.frostedheart$getDormantThermalState();
        var seed = ThermalMaterialBehaviorGameTests.storedBrick(helper, p, 70);
        var stored = new DormantChunkThermalState(p.getY() >> 4, 1); stored.replaceMaterials(p.getY() >> 4, seed);
        var field = MinecraftPhaseController.class.getDeclaredField("APPLYING"); field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<MinecraftPhaseController.Mutation> applying = (ThreadLocal<MinecraftPhaseController.Mutation>) field.get(null);
        var previousMutation = applying.get();
        try {
            attachment.frostedheart$setDormantThermalState(stored);
            applying.set(new MinecraftPhaseController.Mutation(p.asLong(),
                    com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch.MaterialChanges.THERMAL_TRANSITION,
                    Blocks.STONE.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 100000, level.getGameTime()));
            // A callback changes the same location to a different target within the outer phase scope.
            level.setBlockAndUpdate(p, Blocks.GLASS.defaultBlockState());
            near(helper, WorldTemperature.naturalAir(level, p), WorldTemperature.material(level, p),
                    "the unrelated nested replacement initializes its own body instead of inheriting outer H");
            // The outer setter returns after the inner mutation and reports its original target.
            MinecraftThermalInput.onMaterialBlockChanged(chunk, p, Blocks.STONE.defaultBlockState(), Blocks.DIRT.defaultBlockState());
            near(helper, WorldTemperature.naturalAir(level, p), WorldTemperature.material(level, p),
                    "a late outer callback must not overwrite the nested replacement's material identity");
            var staleSnow = Blocks.SNOW.defaultBlockState();
            var edge = MinecraftThermalProfiles.materialLaw(staleSnow).heating();
            helper.assertTrue(!MinecraftPhaseController.applyDormantTransition(level, p, staleSnow, edge,
                            edge.targetEnthalpyJ(), level.getGameTime()) && level.getBlockState(p).is(Blocks.GLASS),
                    "a stale phase submission must not overwrite the replacement body");
            level.setBlockAndUpdate(p, Blocks.DIRT.defaultBlockState());
            MinecraftThermalInput.onMaterialBlockChanged(chunk, p, Blocks.STONE.defaultBlockState(), Blocks.DIRT.defaultBlockState());
            near(helper, WorldTemperature.naturalAir(level, p), WorldTemperature.material(level, p),
                    "returning to the outer target state must not revive the superseded phase energy");
        } finally {
            if (previousMutation == null) applying.remove(); else applying.set(previousMutation);
            attachment.frostedheart$setDormantThermalState(previousState);
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(oldSpeed, level.getServer());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "dormant_lava_mass_reference", timeoutTicks = 100)
    public static void lavaAmountChangesShareOneSpecificEnthalpyReference(GameTestHelper helper) {
        var source = MinecraftThermalProfiles.materialLaw(Blocks.LAVA.defaultBlockState());
        var changedReference = MaterialThermalLaw.sensible(source.capacityJPerK() / 2);
        near(helper, 700, changedReference.temperatureC(source.afterMassChange(
                        source.enthalpyAtTemperature(700), changedReference, 700), (byte) 0),
                "a changed target energy reference must not turn removing matter into a temperature jump");
        for (int level = 1; level < 16; level++) {
            var flow = MinecraftThermalProfiles.materialLaw(Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, level));
            for (double temperature : new double[]{20, 700, 1000}) {
                double energy = source.afterMassChange(source.enthalpyAtTemperature(temperature), flow, temperature);
                near(helper, temperature, flow.temperatureC(energy, (byte) 0), "removing lava must not change its specific temperature");
                energy = flow.afterMassChange(energy, source, temperature);
                near(helper, temperature, source.temperatureC(energy, (byte) 0), "adding same-temperature lava must preserve temperature");
            }
            helper.assertTrue(flow.cooling() == null, "native basalt conversion remains source-lava-only");
        }
        helper.succeed();
    }

    private static MinecraftThermalInput.InfraredSnapshot infrared(ServerPlayer player, MinecraftThermalInput.InfraredSnapshot old) {
        return MinecraftThermalInput.gameplayInfraredSnapshot(player, old == null, old == null ? 0 : old.generation(),
                old == null ? 0 : SectionPos.asLong(old.centerChunkX(), old.centerSectionY(), old.centerChunkZ()),
                old == null ? 0 : old.infraredEpoch(), old == null ? new long[12] : old.presence(), old != null && old.readable(),
                old == null ? new long[0] : old.fieldPages(), old == null ? 0 : old.storedEpoch(), old == null ? 0 : old.storedSampleTick());
    }

    private static MinecraftPhaseController.PhaseAttempt attempt(GameTestHelper helper, BlockPos position) {
        var level = helper.getLevel();
        return MinecraftThermalInput.tryMaterialPhaseAtRandomTick(level,
                level.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4), position, level.getBlockState(position));
    }

    private static MaterialSectionState retime(MaterialSectionState state, int sectionY, long tick) {
        var container = new DormantChunkThermalState(sectionY, 1); container.replaceMaterials(sectionY, state);
        CompoundTag tag = new CompoundTag(); container.encode(tag);
        var material = tag.getCompound("FrostedHeartThermal").getList("sections", Tag.TAG_COMPOUND).getCompound(0).getCompound("materials");
        material.putLong("tick", tick); material.remove("ticks");
        return DormantChunkThermalState.decode(tag, sectionY, 1).materials(sectionY);
    }

    private static BlockPos center(GameTestHelper helper) {
        BlockPos p = helper.absolutePos(new BlockPos(2, 2, 2));
        return new BlockPos((p.getX() & ~3) + 1, (p.getY() & ~3) + 1, (p.getZ() & ~3) + 1);
    }
    private static int block(BlockPos p) { return (p.getX() & 15) | (p.getZ() & 15) << 4 | (p.getY() & 15) << 8; }
    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) <= 1e-6 * Math.max(1, Math.abs(expected)), message + ": " + expected + " != " + actual);
    }
}
