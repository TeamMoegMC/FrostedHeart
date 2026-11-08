/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.network.InfraredBrickCodec;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.InfraredSnapshot;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_retirement")
@PrefixGameTestTemplate(false)
public final class ThermalRetirementProductionGameTests {
    private ThermalRetirementProductionGameTests() { }

    @GameTest(template = "phase0a_empty", batch = "retirement_a_last_material", timeoutTicks = 500)
    public static void coldStoredMaterialClearsTwoIndependentInfraredBaselines(GameTestHelper helper) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos p = helper.absolutePos(new BlockPos(2, 3, 2));
        double natural = natural(helper, p);
        var chunk = level.getChunkAt(p);
        var stored = fixture(helper, new BlockPos[]{p}, new double[]{natural + .2});
        MinecraftThermalInput.setDormantState(chunk, stored);
        ServerPlayer player = FakePlayerFactory.getMinecraft(level);
        player.setPos(p.getX() + .5, p.getY(), p.getZ() + .5);
        var first = infrared(player, null);
        var second = infrared(player, null);
        helper.assertTrue(encoded(first, p) != InfraredBrickCodec.INVALID_TEMPERATURE,
                "The actual stored body must be visible before maintenance publishes its removal");
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.dormantState(chunk) == null,
                "Normal server ticks must release the final cold material and the empty Chunk attachment"))
                .thenExecute(() -> {
                    var a = infrared(player, first); var b = infrared(player, second);
                    helper.assertTrue(a != null && b != null && encoded(a, p) == InfraredBrickCodec.INVALID_TEMPERATURE
                                    && encoded(b, p) == InfraredBrickCodec.INVALID_TEMPERATURE,
                            "Both independent previous baselines receive actual INVALID records after the final deletion");
                    helper.assertTrue(!Double.isFinite(WorldTemperature.material(level, p)), "Thermometer follows the same committed deletion");
                    level.getServer().saveEverything(true, true, true);
                    FHMain.LOGGER.info("THERMAL_RETIREMENT_LAST,statistics={}", DormantThermalMaintenance.statistics());
                }).thenSucceed();
    }

    @GameTest(template = "phase0a_empty", batch = "retirement_b_partial", timeoutTicks = 500)
    public static void partialCleanupKeepsHotEnergyAndOriginalSampleTickAcrossSerialization(GameTestHelper helper) {
        var level = helper.getLevel(); MinecraftThermalInput.closeActiveLevel(level);
        BlockPos p = helper.absolutePos(new BlockPos(1, 3, 1));
        p = new BlockPos((p.getX() & ~15) + 1, p.getY(), (p.getZ() & ~15) + 1);
        BlockPos hot = p.east(), cold = p.east(2);
        double natural = natural(helper, p);
        var chunk = level.getChunkAt(p);
        var stored = fixture(helper, new BlockPos[]{p, hot, cold}, new double[]{natural + .2, natural + 12, natural - .2});
        int section = p.getY() >> 4, local = local(hot);
        var original = stored.materials(section);
        int originalIndex = original.find(local);
        long originalTick = original.savedTick(originalIndex);
        long energyBits = Double.doubleToRawLongBits(original.enthalpyJ(originalIndex));
        MinecraftThermalInput.setDormantState(chunk, stored);
        helper.startSequence().thenWaitUntil(() -> {
            var current = MinecraftThermalInput.dormantState(chunk);
            helper.assertTrue(current != null && current.materials(section) != null && current.materials(section).size() == 1,
                    "Cold positive and negative residuals disappear while the hotter body remains");
        }).thenExecute(() -> {
            var current = MinecraftThermalInput.dormantState(chunk);
            var tag = new CompoundTag(); current.encode(tag);
            var decoded = DormantChunkThermalState.decode(tag, chunk.getMinSection(), chunk.getSections().length);
            var remaining = decoded.materials(section);
            helper.assertTrue(remaining.position(0) == local && remaining.savedTick(0) == originalTick
                            && Double.doubleToRawLongBits(remaining.enthalpyJ(0)) == energyBits,
                    "Compression and v6 serialization preserve the surviving body's exact energy and original clock");
            helper.assertTrue(original.size() == 3, "A previous immutable reader retains its original snapshot");
            MinecraftThermalInput.setDormantState(chunk, null);
        }).thenSucceed();
    }

    @GameTest(template = "phase0a_empty", batch = "retirement_c_unobserved", timeoutTicks = 1900)
    public static void unobservedWarmCheckpointCoolsAndDisappearsWithoutAWorker(GameTestHelper helper) {
        var level = helper.getLevel(); MinecraftThermalInput.closeActiveLevel(level);
        boolean daylight = level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        BlockPos p = helper.absolutePos(new BlockPos(2, 3, 2));
        var chunk = level.getChunkAt(p);
        var stored = fixture(helper, new BlockPos[]{p}, new double[]{natural(helper, p) + 2});
        MinecraftThermalInput.setDormantState(chunk, stored);
        helper.startSequence().thenIdle(1500).thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.dormantState(chunk) == null,
                "A two-degree body must cool below half a degree and release itself without thermometer/infrared queries"))
                .thenExecute(() -> {
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(daylight, level.getServer());
                    FHMain.LOGGER.info("THERMAL_RETIREMENT_UNOBSERVED,statistics={}", DormantThermalMaintenance.statistics());
                }).thenSucceed();
    }

    @GameTest(template = "phase0a_empty", batch = "retirement_d_latent", timeoutTicks = 3800)
    public static void dormantSoilFinishesFreezingThenReleasesItsSensibleHeat(GameTestHelper helper) {
        var level = helper.getLevel(); MinecraftThermalInput.closeActiveLevel(level);
        boolean daylight = level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        BlockPos p = new BlockPos(2048, 196, 2048);
        level.setChunkForced(p.getX() >> 4, p.getZ() >> 4, true);
        level.setBlockAndUpdate(p, Blocks.DIRT.defaultBlockState());
        var seed = ThermalMaterialBehaviorGameTests.storedBrick(helper, p, -10);
        int section = p.getY() >> 4, index = seed.find(local(p));
        var law = seed.law(index); var edge = law.cooling();
        helper.assertTrue(edge != null && natural(helper, p) < edge.transitionTemperatureC(),
                "Actual dirt has a freezing edge and a colder natural environment");
        var edit = new com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MaterialSectionState.Editor(seed);
        double energy = edge.targetEnthalpyJ() + Math.min(1000, Math.abs(edge.targetEnthalpyJ() - edge.sourceEnthalpyJ()) * .01);
        edit.update(local(p), seed.stateId(index), law, energy,
                com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw.COOLING, level.getGameTime());
        var chunk = level.getChunkAt(p);
        var stored = new DormantChunkThermalState(chunk.getMinSection(), chunk.getSections().length);
        stored.replaceMaterials(section, edit.snapshot());
        MinecraftThermalInput.setDormantState(chunk, stored);
        helper.assertTrue(stored.materials(section).branch(index) == -1, "The checkpoint really holds uncompleted latent energy");
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                net.minecraft.world.level.block.Block.getId(level.getBlockState(p)) == edge.targetStateId(),
                "Ordinary server maintenance must complete the actual soil freezing mutation without a source"))
                .thenExecute(() -> helper.assertTrue(Double.isFinite(WorldTemperature.material(level, p)),
                        "The changed body retains its actual sensible heat after the latent handoff"))
                .thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.dormantState(chunk) == null,
                        "The frozen body's ordinary residual must eventually cool and release the final checkpoint"))
                .thenExecute(() -> {
                    level.setChunkForced(p.getX() >> 4, p.getZ() >> 4, false);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(daylight, level.getServer());
                    FHMain.LOGGER.info("THERMAL_RETIREMENT_LATENT,block={},statistics={}", level.getBlockState(p), DormantThermalMaintenance.statistics());
                }).thenSucceed();
    }

    public static void realCampfireStopsExpandingCoolsAndRelights(GameTestHelper helper) {
        var level = helper.getLevel();
        boolean daylight = level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        // A normal-terrain room at y=80 also captures naturally freezing soil. Its latent state
        // must survive; this fixture isolates ordinary campfire heat above that terrain.
        BlockPos origin = new BlockPos(4096, 196, 4096);
        ThermalConvergenceScenes.force(helper, origin, 8, true);
        for (int y = 0; y <= 4; y++) for (int z = 0; z <= 6; z++) for (int x = 0; x <= 6; x++)
            level.setBlockAndUpdate(origin.offset(x, y, z), (x == 0 || x == 6 || z == 0 || z == 6 || y == 0 || y == 4
                    ? Blocks.STONE : Blocks.AIR).defaultBlockState());
        // This retirement fixture has a real entrance. A fully sealed stone box may correctly
        // retain more than 0.5 C long after extinguishing and is not a fixed-time expiry case.
        level.setBlockAndUpdate(origin.offset(3, 1, 0), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(origin.offset(3, 2, 0), Blocks.AIR.defaultBlockState());
        BlockPos fire = origin.offset(3, 1, 3), probe = fire.above();
        ThermalConvergenceScenes.fire(helper, fire);
        var sample = new ThermalEnvironmentSample();
        int[] pages = {0}; long[] extinguished = {0}; double[] peakRise = {0};
        var chunk = level.getChunkAt(fire);
        for (int delay = 200; delay <= 5200; delay += 200) {
            int elapsed = delay;
            helper.runAfterDelay(delay, () -> observeCooling(helper, elapsed));
        }
        helper.startSequence().thenIdle(600).thenWaitUntil(() -> {
            helper.assertTrue(ThermalConvergenceScenes.fresh(helper, probe, sample), "A real fueled fire must establish the published field");
        }).thenExecute(() -> pages[0] = livePages(level)).thenIdle(600).thenExecute(() -> {
            helper.assertTrue(livePages(level) <= pages[0] + 8,
                    "A fixed single-fire room must not keep allocating rings of Pages merely because neighbors became resident");
            ThermalConvergenceScenes.fresh(helper, probe, sample);
            peakRise[0] = sample.airTemperatureC() - natural(helper, probe);
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
            extinguished[0] = level.getGameTime();
        }).thenIdle(1200).thenExecute(() -> {
            ThermalConvergenceScenes.readable(helper, probe, sample);
            double residual = sample.airAvailable() ? sample.airTemperatureC() - natural(helper, probe) : 0;
            helper.assertTrue(residual < peakRise[0] - .05,
                    "A real ventilated room must lose heat after extinguishing; peak=" + peakRise[0] + ", remaining=" + residual);
            FHMain.LOGGER.info("THERMAL_RETIREMENT_ROOM_COOLING,peakRiseC={},remainingRiseC={},onlineCoolingTicks={}",
                    peakRise[0], residual, level.getGameTime() - extinguished[0]);
            ThermalConvergenceScenes.force(helper, origin, 8, false);
        }).thenWaitUntil(() -> helper.assertTrue(level.getChunkSource().getChunkNow(chunk.getPos().x, chunk.getPos().z) == null
                        && MinecraftThermalInput.observedNativeCheckpoints(level).pending() == 0,
                "The still-warm real room must finish its actual native chunk unload"))
                .thenExecute(() -> {
                    var saved = MinecraftThermalInput.dormantState(chunk);
                    helper.assertTrue(saved != null && saved.oldestAirSavedTick() >= extinguished[0],
                            "The actual unload preserves a field sampled after extinguishing");
                    FHMain.LOGGER.info("THERMAL_RETIREMENT_ROOM_UNLOADED,tick={},coldPendingRecords={}",
                            level.getGameTime(), saved.coldPending().size());
                }).thenIdle(2000).thenExecute(() -> {
                    level.getChunkAt(fire);
                    ThermalConvergenceScenes.force(helper, origin, 8, true);
                }).thenWaitUntil(() -> {
            var reloaded = level.getChunkSource().getChunkNow(chunk.getPos().x, chunk.getPos().z);
            helper.assertTrue(reloaded != null && reloaded != chunk, "The room is read back as a new actual Chunk instance");
            helper.assertTrue(MinecraftThermalInput.dormantState(reloaded) == null,
                    "The returned room must finish its stored ordinary heat tail without relighting");
        }).thenExecute(() -> {
            FHMain.LOGGER.info("THERMAL_RETIREMENT_CAMPFIRE,coolingTicks={},pages={},statistics={}",
                    level.getGameTime() - extinguished[0], livePages(level), DormantThermalMaintenance.statistics());
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
        }).thenWaitUntil(() -> helper.assertTrue(ThermalConvergenceScenes.fresh(helper, probe, sample),
                "A new ignition must recreate the released field from the real source"))
                .thenExecute(() -> {
                    ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
                    ThermalConvergenceScenes.force(helper, origin, 8, false);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(daylight, level.getServer());
                }).thenSucceed();
    }

    private static int livePages(net.minecraft.server.level.ServerLevel level) {
        try {
            var active = MinecraftThermalInput.class.getDeclaredField("ACTIVE"); active.setAccessible(true);
            var input = ((java.util.Map<?, ?>) active.get(null)).get(level);
            if (input == null) return 0;
            var manager = MinecraftThermalInput.class.getDeclaredField("pages"); manager.setAccessible(true);
            var pages = manager.get(input).getClass().getDeclaredField("pages"); pages.setAccessible(true);
            return ((java.util.Map<?, ?>) pages.get(manager.get(input))).size();
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }

    private static void observeCooling(GameTestHelper helper, int elapsed) {
        var level = helper.getLevel();
        var cursor = new com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication.ReadCursor();
        try {
            var active = MinecraftThermalInput.class.getDeclaredField("ACTIVE"); active.setAccessible(true);
            var input = ((java.util.Map<?, ?>) active.get(null)).get(level);
            if (input == null) return;
            var queryField = MinecraftThermalInput.class.getDeclaredField("queryPublication"); queryField.setAccessible(true);
            var query = (com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication) queryField.get(input);
            if (!query.beginRead(cursor)) return;
            var leaseField = cursor.getClass().getDeclaredField("regionalLease"); leaseField.setAccessible(true);
            var lease = leaseField.get(cursor); var forSlot = lease.getClass().getDeclaredMethod("forSlot", int.class); forSlot.setAccessible(true);
            var managerField = MinecraftThermalInput.class.getDeclaredField("pages"); managerField.setAccessible(true);
            var manager = managerField.get(input); var pagesField = manager.getClass().getDeclaredField("pages"); pagesField.setAccessible(true);
            double maximumMaterial = 0, maximumAir = 0, materialTemperature = 0, materialNatural = 0;
            int hotAir = 0, hotMaterial = 0, bodies = 0;
            BlockPos hottest = BlockPos.ZERO;
            for (var value : ((java.util.Map<?, ?>) pagesField.get(manager)).values()) {
                var handleField = value.getClass().getDeclaredField("handle"); handleField.setAccessible(true);
                var handle = (com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle) handleField.get(value);
                if (handle == null || handle.lastPublication() == null) continue;
                long section = handle.sectionKey();
                double natural = WorldTemperature.naturalAir(level, new BlockPos(SectionPos.x(section) * 16 + 8,
                        SectionPos.y(section) * 16 + 8, SectionPos.z(section) * 16 + 8));
                var publication = handle.lastPublication();
                for (int brick = 0; brick < 64; brick++) {
                    var payload = publication.brick(brick);
                    if (payload.firstSlot() < 0) continue;
                    int count = payload.airNodeCount() + (payload.blockLayout() == null ? 0 : Long.bitCount(payload.blockLayout().materialNodeMask()));
                    for (int offset = 0; offset < count; offset++) {
                        int slot = payload.firstSlot() + offset;
                        var field = (com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot) forSlot.invoke(lease, slot);
                        if (field == null) continue;
                        if (field.componentForSlot(slot) != null) {
                            if (field.activeAtSlot(slot, natural, .5)) hotAir++;
                            maximumAir = Math.max(maximumAir, Math.max(Math.abs(field.minimumAtSlotC(slot) - natural),
                                    Math.abs(field.maximumAtSlotC(slot) - natural)));
                        } else {
                            bodies++;
                            if (field.activeAtSlot(slot, natural, .5)) hotMaterial++;
                            double temperature = field.materialTemperatureAtSlotC(slot);
                            if (Math.abs(temperature - natural) > maximumMaterial) {
                                maximumMaterial = Math.abs(temperature - natural); materialTemperature = temperature; materialNatural = natural;
                                hottest = new BlockPos(SectionPos.x(section) * 16 + (brick & 3) * 4,
                                        SectionPos.y(section) * 16 + (brick >>> 4) * 4, SectionPos.z(section) * 16 + (brick >>> 2 & 3) * 4);
                            }
                        }
                    }
                }
            }
            FHMain.LOGGER.info("THERMAL_RETIREMENT_CURVE,elapsed={},pages={},enabledSources={},hotAir={},hotMaterials={},materials={},maximumAirResidualC={},maximumMaterialResidualC={},materialC={},naturalC={},brick={},worker={}",
                    elapsed, livePages(level), MinecraftThermalInput.enabledPhysicalSourceCount(level), hotAir, hotMaterial, bodies,
                    maximumAir, maximumMaterial, materialTemperature, materialNatural, hottest, MinecraftThermalInput.describeWorkerProgress(level));
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
        finally { cursor.clear(); }
    }

    private static double natural(GameTestHelper helper, BlockPos p) {
        return WorldTemperature.naturalAir(helper.getLevel(), new BlockPos((p.getX() & ~15) + 8, (p.getY() & ~15) + 8, (p.getZ() & ~15) + 8));
    }

    private static int local(BlockPos p) { return (p.getX() & 15) | (p.getZ() & 15) << 4 | (p.getY() & 15) << 8; }

    /** Real material law and solver capture, narrowed to the deliberate stored-body fixture. */
    static DormantChunkThermalState fixture(GameTestHelper helper, BlockPos[] positions, double[] temperatures) {
        for (var p : positions) helper.getLevel().setBlockAndUpdate(p, Blocks.STONE.defaultBlockState());
        var seed = ThermalMaterialBehaviorGameTests.storedBrick(helper, positions[0], temperatures[0]);
        int section = positions[0].getY() >> 4, original = seed.find(local(positions[0]));
        var container = new DormantChunkThermalState(section, 1); container.replaceMaterials(section, seed);
        var tag = new CompoundTag(); container.encode(tag);
        var material = tag.getCompound("FrostedHeartThermal").getList("sections", Tag.TAG_COMPOUND).getCompound(0).getCompound("materials");
        int palette = material.getIntArray("palette_indexes")[original];
        byte[] bytes = new byte[positions.length * 2]; long[] energy = new long[positions.length]; int[] palettes = new int[positions.length];
        for (int i = 0; i < positions.length; i++) {
            int local = local(positions[i]); bytes[2 * i] = (byte) (local >>> 8); bytes[2 * i + 1] = (byte) local;
            energy[i] = Double.doubleToRawLongBits(seed.law(original).enthalpyAtTemperature(temperatures[i])); palettes[i] = palette;
        }
        material.putByteArray("positions", bytes); material.putLongArray("enthalpy", energy);
        material.putIntArray("palette_indexes", palettes); material.putByteArray("branches", new byte[positions.length]);
        material.remove("ticks"); material.putLong("tick", helper.getLevel().getGameTime());
        var chunk = helper.getLevel().getChunkAt(positions[0]);
        return DormantChunkThermalState.decode(tag, chunk.getMinSection(), chunk.getSections().length);
    }

    private static InfraredSnapshot infrared(ServerPlayer player, InfraredSnapshot previous) {
        return MinecraftThermalInput.gameplayInfraredSnapshot(player, previous == null,
                previous == null ? 0 : previous.generation(), previous == null ? 0 : SectionPos.asLong(
                        previous.centerChunkX(), previous.centerSectionY(), previous.centerChunkZ()),
                previous == null ? 0 : previous.infraredEpoch(), previous == null ? new long[12] : previous.presence(),
                previous != null && previous.readable(), previous == null ? new long[0] : previous.fieldPages(),
                previous == null ? 0 : previous.storedEpoch(), previous == null ? 0 : previous.storedSampleTick());
    }

    private static short encoded(InfraredSnapshot snapshot, BlockPos p) {
        int x = (p.getX() >> 4) - snapshot.centerChunkX() + 4, z = (p.getZ() >> 4) - snapshot.centerChunkZ() + 4;
        int y = (p.getY() >> 4) - snapshot.centerSectionY() + 4;
        int page = x + 9 * (z + 9 * y);
        int brick = (p.getX() & 15) >>> 2 | (p.getZ() & 15) >>> 2 << 2 | (p.getY() & 15) >>> 2 << 4;
        var decoder = new InfraredBrickCodec.Decoder(); short[] values = new short[64];
        for (byte[] part : snapshot.brickRecords()) {
            var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(part));
            try {
                int address;
                while ((address = decoder.readRecord(buffer, values)) >= 0)
                    if (address == page * 64 + brick) return values[(p.getX() & 3) | (p.getZ() & 3) << 2 | (p.getY() & 3) << 4];
            } finally { buffer.release(); }
        }
        throw new IllegalStateException("Expected an actual infrared record for " + p);
    }
}
