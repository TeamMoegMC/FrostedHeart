/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.data.BiomeTempData;
import com.teammoeg.frostedheart.content.climate.network.*;
import com.teammoeg.frostedheart.content.climate.thermal.field.*;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField.CombineMode;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField.Shape;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.*;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.*;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.minecraft.BlockRadiationIndex;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPageManager;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.InfraredSnapshot;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.lang.reflect.Field;
import java.util.*;
import static com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ThermalLoadedWorldGameTests.read;
import static com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ThermalLoadedWorldGameTests.start;

@GameTestHolder(FHMain.MODID)
@PrefixGameTestTemplate(false)
public final class ThermalInfraredGameTests {
    private static final String TEMPLATE = "phase0a_empty";
    private static final long[] EMPTY = new long[12];
    private static final ResourceLocation PROVIDER = new ResourceLocation("frostedheart", "gametest_infrared_surface");

    @GameTest(template = TEMPLATE, batch = "surface_empty", timeoutTicks = 100)
    public static void missingSurfaceKeepsBluePlaceholderWithoutCreatingPhysics(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel();
        // Closing another test's runtime preserves its material checkpoints. Use
        // a loaded window away from those fixtures when asserting truly empty data.
        BlockPos anchor = helper.absolutePos(BlockPos.ZERO);
        BlockPos center = new BlockPos(8192 + anchor.getX(),
                ((level.getMaxBuildHeight() - 64) & ~15) + 8, 8192 + anchor.getZ());
        level.getChunkAt(center);
        ServerPlayer player = observer(level, center);
        MinecraftThermalInput.closeActiveLevel(level);
        InfraredSnapshot full = sample(player, null, true, EMPTY);
        helper.assertTrue(full != null && !full.readable() && full.full() && full.brickRecords().length == 0,
                "empty physical window must commit all-INVALID display, not natural estimates: "
                        + (full == null ? "no snapshot" : "readable=" + full.readable() + ", records=" + full.brickRecords().length));
        helper.assertTrue(full.generation() == 0 && Arrays.stream(full.presence()).allMatch(v -> v == 0)
                && full.fieldPages().length == 0, "empty display cannot manufacture physical or field presence");
        helper.assertTrue(sample(player, full, false, full.presence()) == null, "stable unreadable empty display sends nothing");
        Field active = MinecraftThermalInput.class.getDeclaredField("ACTIVE"); active.setAccessible(true);
        helper.assertTrue(!((Map<?, ?>)active.get(null)).containsKey(level), "display cannot start a runtime");
        wire(helper, full);
        player.setPos(player.getX()+16, player.getY(), player.getZ());
        InfraredSnapshot moved = sample(player, full, false, full.presence());
        helper.assertTrue(moved.full() && moved.centerChunkX() == full.centerChunkX()+1, "server detects moved baseline");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "surface_stored", timeoutTicks = 160)
    public static void storedMaterialUsesTheSameTemperatureAndOnlySendsChanges(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel();
        BlockPos p = loadedCenter(helper);
        MinecraftThermalInput.closeActiveLevel(level);
        level.setBlockAndUpdate(p, Blocks.STONE.defaultBlockState());
        var chunk = level.getChunkAt(p);
        var attachment = (MinecraftThermalChunkAttachment) (Object) chunk;
        var previous = attachment.frostedheart$getDormantThermalState();
        var saved = ThermalMaterialBehaviorGameTests.storedBrick(helper, p, 20.12);
        var stored = new DormantChunkThermalState(chunk.getSectionYFromSectionIndex(0), chunk.getSections().length);
        int section = p.getY() >> 4, block = (p.getX() & 15) | (p.getZ() & 15) << 4 | (p.getY() & 15) << 8;
        stored.replaceMaterials(section, saved);
        ThermalFieldKey field = new ThermalFieldKey(PROVIDER, 0, p.asLong(), 101);
        try {
            attachment.frostedheart$setDormantThermalState(stored);
            ServerPlayer player = observer(level, p);
            InfraredSnapshot full = sample(player, null, true, EMPTY);
            helper.assertTrue(full != null && !full.readable() && present(full, p), "stored materials need no live publication");
            expectTemperature(helper, full, p, WorldTemperature.material(level, p));
            helper.assertTrue(sample(player, full, false, full.presence()) == null, "unchanged stored records send no data");
            measureSurfaceRequests(player, full);
            MinecraftThermalInput.upsertGameplayAnalyticField(level, new ThermalAnalyticField(field, 0,
                    CombineMode.ADD_DELTA, p.getX() + .5, p.getY() + .5, p.getZ() + .5, 2, .12));
            InfraredSnapshot composed = sample(player, full, false, full.presence());
            helper.assertTrue(Objects.equals(temperature(composed, p), InfraredBrickCodec.quantize(20.24)),
                    "stored temperature and analytic field must be quantized together once");
            MinecraftThermalInput.removeGameplayAnalyticField(level, field);
            InfraredSnapshot restored = sample(player, composed, false, full.presence());
            expectTemperature(helper, restored, p, WorldTemperature.material(level, p));
            int index = saved.find(block);
            var edit = new MaterialSectionState.Editor(saved);
            edit.update(block, saved.stateId(index), saved.law(index), saved.law(index).enthalpyAtTemperature(51.37), (byte) 0, level.getGameTime());
            saved = edit.snapshot();
            stored.replaceMaterials(section, saved);
            InfraredSnapshot changed = sample(player, restored, false, full.presence());
            helper.assertTrue(changed != null && !changed.full() && changed.storedEpoch() > restored.storedEpoch(),
                    "stored temperature changes use the existing incremental transaction");
            expectTemperature(helper, changed, p, WorldTemperature.material(level, p));
            wire(helper, changed);
            level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(p, Blocks.STONE.defaultBlockState());
            InfraredSnapshot removed = sample(player, changed, false, full.presence());
            expectMissing(helper, removed, p, true);
            helper.assertTrue(!Double.isFinite(WorldTemperature.material(level, p)), "replacement cannot inherit the saved body's temperature");
            Field active = MinecraftThermalInput.class.getDeclaredField("ACTIVE"); active.setAccessible(true);
            helper.assertTrue(!((Map<?, ?>) active.get(null)).containsKey(level), "stored infrared requests cannot start physics");
        } finally {
            MinecraftThermalInput.removeGameplayAnalyticField(level, field);
            attachment.frostedheart$setDormantThermalState(previous);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "surface_stored_close", timeoutTicks = 700)
    public static void warmedMaterialRemainsVisibleAfterTheRuntimeCloses(GameTestHelper helper) {
        LiveFixture f = new LiveFixture(helper);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(Double.isFinite(material(f.input, f.wall)),
                "source must publish material before closing")).thenExecute(() -> {
            try {
                InfraredSnapshot live = sample(f.player, null, true, EMPTY);
                MinecraftThermalInput.closeActiveLevel(f.level);
                double saved = WorldTemperature.material(f.level, f.wall);
                helper.assertTrue(Double.isFinite(saved), "closing must checkpoint the warmed body");
                InfraredSnapshot closed = sample(f.player, live, false, live.presence());
                helper.assertTrue(closed.full() && !closed.readable() && present(closed, f.wall),
                        "closing the live runtime retains stored infrared coverage");
                expectTemperature(helper, closed, f.wall, saved);
                helper.assertTrue(sample(f.player, closed, false, closed.presence()) == null,
                        "the closed runtime's stored image stays incremental");
            } finally { f.close(); }
        }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "surface_stored_mixed", timeoutTicks = 700)
    public static void liveMaterialsWinAndStoredBricksCanBeRemovedWithinTheSamePage(GameTestHelper helper) {
        LiveFixture f = new LiveFixture(helper);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(Double.isFinite(material(f.input, f.wall)),
                "live material must be published")).thenExecute(() -> {
            var chunk = f.level.getChunkAt(f.wall);
            var attachment = (MinecraftThermalChunkAttachment) (Object) chunk;
            var previous = attachment.frostedheart$getDormantThermalState();
            try {
                MinecraftPageManager pages = read(f.input, "pages");
                var page = pages.handle(SectionPos.asLong(f.wall)).currentPublication();
                int remoteBrick = 0;
                while (remoteBrick < 64 && page.brick(remoteBrick).firstSlot() >= 0) remoteBrick++;
                helper.assertTrue(remoteBrick < 64, "fixture needs a nonresident Brick in the live Page");
                BlockPos remote = new BlockPos((f.wall.getX() & ~15) + (remoteBrick & 3) * 4 + 1,
                        (f.wall.getY() & ~15) + (remoteBrick >>> 4) * 4 + 1,
                        (f.wall.getZ() & ~15) + (remoteBrick >>> 2 & 3) * 4 + 1);
                f.level.setBlockAndUpdate(remote, Blocks.STONE.defaultBlockState());
                var savedLive = ThermalMaterialBehaviorGameTests.storedBrick(helper, f.wall, 99);
                var savedRemote = ThermalMaterialBehaviorGameTests.storedBrick(helper, remote, 45);
                var saved = MaterialSectionState.merge(savedLive, savedRemote, 1L << remoteBrick);
                var stored = new DormantChunkThermalState(chunk.getSectionYFromSectionIndex(0), chunk.getSections().length);
                int section = remote.getY() >> 4;
                stored.replaceMaterials(section, saved);
                attachment.frostedheart$setDormantThermalState(stored);
                InfraredSnapshot full = sample(f.player, null, true, EMPTY);
                expectTemperature(helper, full, f.wall, material(f.input, f.wall));
                expectTemperature(helper, full, remote, 45);
                expectTemperature(helper, full, remote, WorldTemperature.material(f.level, remote));
                int block = (remote.getX() & 15) | (remote.getZ() & 15) << 4 | (remote.getY() & 15) << 8;
                var edit = new MaterialSectionState.Editor(saved);
                edit.remove(block);
                stored.replaceMaterials(section, edit.snapshot());
                InfraredSnapshot removed = sample(f.player, full, false, full.presence());
                helper.assertTrue(removed != null && !removed.full(), "stored deletion cannot depend on live Page presence changing");
                expectMissing(helper, removed, remote, true);
                expectTemperature(helper, removed, f.wall, material(f.input, f.wall));
                attachment.frostedheart$setDormantThermalState(null);
                InfraredSnapshot cleared = sample(f.player, removed, false, full.presence());
                expectMissing(helper, cleared, remote, true);
                expectTemperature(helper, cleared, f.wall, material(f.input, f.wall));
            } finally {
                f.close();
                attachment.frostedheart$setDormantThermalState(previous);
            }
        }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "surface_generator", timeoutTicks = 100)
    public static void realGeneratorFieldHeatsDisplayWithoutPhysicsAndClearsItsOldRange(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = loadedCenter(helper);
        ServerPlayer player = observer(level, center);
        MinecraftThermalInput.closeActiveLevel(level);
        var generator = new com.teammoeg.frostedheart.content.climate.block.generator.GeneratorData(null);
        generator.dimension = level.dimension(); generator.actualPos = center.above(7); generator.masterYPosInMB = 7;
        generator.RLevel = 2; generator.TLevel = 1;
        UUID owner = UUID.fromString("98fbb244-3360-4a0b-a918-d28c2b3ff852");
        try {
            generator.publishGameplayHeat(level, owner);
            InfraredSnapshot first = sample(player, null, true, EMPTY);
            helper.assertTrue(first.full() && !first.readable() && first.fieldPages().length == 12,
                    "generator field-only display must commit without physical publication");
            expectTemperature(helper, first, center, WorldTemperature.naturalAir(level, center)+generator.getTempMod());
            expectMissing(helper, first, center.east(generator.getRadius()+1), false);
            measureSurfaceRequests(player, first);
            generator.RLevel = 1; generator.TLevel = 2;
            BlockPos removed = center.east(generator.getRadius()+1);
            generator.publishGameplayHeat(level, owner);
            InfraredSnapshot changed = sample(player, first, false, first.presence());
            helper.assertTrue(changed != null && !changed.full() && changed.infraredEpoch() == first.infraredEpoch(),
                    "field changes must update display without material epoch or repeated full");
            expectTemperature(helper, changed, center, WorldTemperature.naturalAir(level, center)+generator.getTempMod());
            expectMissing(helper, changed, removed, true);
            wire(helper, changed);
            generator.removeGameplayHeat(level.getServer());
            InfraredSnapshot cleared = sample(player, changed, false, first.presence());
            helper.assertTrue(cleared.fieldPages().length == 0, "field deletion must clear echoed footprint");
            expectMissing(helper, cleared, center, true);
            helper.assertTrue(sample(player, cleared, false, first.presence()) == null, "removed field stops refreshing");
            helper.succeed();
        } finally { generator.removeGameplayHeat(level.getServer()); }
    }

    @GameTest(template = TEMPLATE, batch = "surface_field_order", timeoutTicks = 100)
    public static void orderedFieldsPreserveZeroAndQuantizeOnlyTheFinalValue(GameTestHelper helper) {
        ServerLevel level = helper.getLevel(); BlockPos p = loadedCenter(helper);
        MinecraftThermalInput.closeActiveLevel(level);
        ServerPlayer player = observer(level, p);
        ThermalFieldKey override = new ThermalFieldKey(PROVIDER, 0, p.asLong(), 10);
        ThermalFieldKey delta = new ThermalFieldKey(PROVIDER, 0, p.asLong(), 11);
        try {
            MinecraftThermalInput.upsertGameplayAnalyticField(level, new ThermalAnalyticField(override, 0,
                    CombineMode.OVERRIDE, Shape.PILLAR, p.getX()+.5, p.getY()+.5, p.getZ()+.5, 3, 1, 2, 0));
            InfraredSnapshot zero = sample(player, null, true, EMPTY);
            expectTemperature(helper, zero, p, 0);
            expectMissing(helper, zero, p.above(2), false);
            expectMissing(helper, zero, p.offset(3,0,3), false); // AABB corner is outside the pillar.
            MinecraftThermalInput.upsertGameplayAnalyticField(level, new ThermalAnalyticField(override, 0,
                    CombineMode.OVERRIDE, p.getX()+.5, p.getY()+.5, p.getZ()+.5, 3, .12));
            MinecraftThermalInput.upsertGameplayAnalyticField(level, new ThermalAnalyticField(delta, 0,
                    CombineMode.ADD_DELTA, p.getX()+.5, p.getY()+.5, p.getZ()+.5, 3, .02));
            InfraredSnapshot fractional = sample(player, zero, false, zero.presence());
            helper.assertTrue(Objects.equals(temperature(fractional,p),(short)1), ".12 + .02 must quantize once to .25 C");
            expectMissing(helper, fractional, p.below(4), false);
            wire(helper, fractional);
            helper.succeed();
        } finally {
            MinecraftThermalInput.removeGameplayAnalyticField(level, override);
            MinecraftThermalInput.removeGameplayAnalyticField(level, delta);
        }
    }
    @GameTest(template = TEMPLATE, batch = "surface_live", timeoutTicks = 700)
    public static void campfireSurfaceUsesMaterialAndLocalAvailability(GameTestHelper helper) {
        LiveFixture f = new LiveFixture(helper);
        InfraredSnapshot[] baseline = new InfraredSnapshot[1];
        ThermalFieldKey field = new ThermalFieldKey(PROVIDER, 0, f.wall.asLong(), 2);
        helper.startSequence().thenWaitUntil(() -> {
            double wall = material(f.input, f.wall), air = rawAir(f.input, f.target);
            helper.assertTrue(Double.isFinite(wall) && Double.isFinite(material(f.input, f.other.east()))
                    && air > wall+1 && wall > WorldTemperature.naturalAir(f.level, f.wall)+.125,
                    "real campfire must produce independently readable warmed material and hotter air");
            InfraredSnapshot result = sample(f.player, null, true, EMPTY);
            helper.assertTrue(result != null && result.readable() && present(result, f.wall) && present(result, f.other.east()),
                    "both source Pages must be readable");
            expectTemperature(helper, result, f.wall, wall);
            helper.assertTrue(temperature(result, f.target) == null
                    || temperature(result, f.target) == InfraredBrickCodec.INVALID_TEMPERATURE, "Air is not a thermal surface");
            baseline[0] = result;
        }).thenExecute(() -> {
            MinecraftThermalInput.upsertGameplayAnalyticField(f.level, new ThermalAnalyticField(field, 0,
                    CombineMode.FLOOR_FROM_NATURAL, Shape.SPHERE, f.wall.getX()+.5, f.wall.getY()+.5, f.wall.getZ()+.5,
                    8, 8, 8, 30));
            InfraredSnapshot result = sample(f.player, baseline[0], true, baseline[0].presence());
            expectTemperature(helper, result, f.wall, Math.max(material(f.input, f.wall), WorldTemperature.naturalAir(f.level,f.wall)+30));
            helper.assertTrue(MinecraftThermalInput.gameplayPassiveEnvironment(f.level, f.wall, -40) >= -10,
                    "generator-style field remains a gameplay floor");
            wire(helper, result);
            baseline[0] = result;
            measureSurfaceRequests(f.player, result);
            f.level.setBlockAndUpdate(f.wall, Blocks.OAK_PLANKS.defaultBlockState());
            InfraredSnapshot gap = sample(f.player, result, true, result.presence());
            helper.assertTrue(gap != null && gap.readable()
                    && present(gap, f.wall) && present(gap, f.other.east()),
                    "replacement must preserve the other material bodies in both Pages");
            expectTemperature(helper, gap, f.wall, WorldTemperature.naturalAir(f.level,f.wall)+30);
            baseline[0] = gap;
        }).thenWaitUntil(() -> {
            helper.assertTrue(Double.isFinite(material(f.input, f.wall)), "replacement body must be republished");
            InfraredSnapshot recovered = sample(f.player, baseline[0], true, baseline[0].presence());
            helper.assertTrue(recovered != null && recovered.readable() && present(recovered, f.wall), "changed Page must return");
            expectTemperature(helper, recovered, f.wall, Math.max(material(f.input, f.wall), WorldTemperature.naturalAir(f.level,f.wall)+30));
            baseline[0] = recovered;
        }).thenExecute(() -> {
            try {
                InfraredSnapshot prior = baseline[0];
                QueryPublication queries = read(f.input, "queryPublication");
                queries.close();
                InfraredSnapshot unavailable = sample(f.player, prior, false, prior.presence());
                helper.assertTrue(unavailable != null && !unavailable.readable() && unavailable.full(),
                        "closed publication must commit a field-only full");
                expectTemperature(helper, unavailable, f.wall, WorldTemperature.naturalAir(f.level,f.wall)+30);
                expectMissing(helper, unavailable, f.other.east(), false);
                wire(helper, unavailable);
                InfraredSnapshot steady = sample(f.player, unavailable, false, unavailable.presence());
                helper.assertTrue(steady != null && !steady.full(), "field-only baseline must allow delta while physics stays closed");
                MinecraftThermalInput.removeGameplayAnalyticField(f.level, field);
                InfraredSnapshot cleared = sample(f.player, steady, false, unavailable.presence());
                expectMissing(helper, cleared, f.wall, true);
                helper.assertTrue(sample(f.player, cleared, false, unavailable.presence()) == null,
                        "closed physics and removed field need no repeated packets");            } finally {
                MinecraftThermalInput.removeGameplayAnalyticField(f.level, field);
                f.close();
            }
        }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "surface_mutation", timeoutTicks = 700)
    public static void brickMutationKeepsUnchangedMaterialTemperatures(GameTestHelper helper) {
        LiveFixture f = new LiveFixture(helper);
        BlockPos sameBrick = f.target.south(), otherBrick = f.target.west();
        helper.startSequence().thenWaitUntil(() -> {
            helper.assertTrue(Double.isFinite(material(f.input, f.wall))
                    && Double.isFinite(material(f.input, sameBrick)) && Double.isFinite(material(f.input, otherBrick)),
                    "fixture must publish the original material bodies");
        }).thenExecute(() -> {
            try {
                InfraredSnapshot before = sample(f.player, null, true, EMPTY);
                double sameTemperature = WorldTemperature.material(f.level, sameBrick);
                double otherTemperature = WorldTemperature.material(f.level, otherBrick);
                f.level.setBlockAndUpdate(f.wall, Blocks.AIR.defaultBlockState());
                helper.assertTrue(!Double.isFinite(WorldTemperature.material(f.level, f.wall)),
                        "removed body must not retain its material reading");
                helper.assertTrue(Double.isFinite(WorldTemperature.material(f.level, sameBrick))
                        && Double.isFinite(WorldTemperature.material(f.level, otherBrick)),
                        "geometry mutation must not invalidate unchanged thermometer readings");
                InfraredSnapshot delta = sample(f.player, before, false, before.presence());
                helper.assertTrue(delta != null && !delta.full(), "pending replacement must send a local delta");
                helper.assertTrue(delta.presence().length == 0 || present(delta, sameBrick),
                        "pending replacement must not remove the material Page presence");
                expectMissing(helper, delta, f.wall, true);
                expectTemperature(helper, delta, sameBrick, sameTemperature);
                helper.assertTrue(temperature(delta, otherBrick) == null
                        || temperature(delta, otherBrick) != InfraredBrickCodec.INVALID_TEMPERATURE,
                        "unchanged neighbor Brick must not be cleared");
                InfraredSnapshot full = sample(f.player, before, true, before.presence());
                expectMissing(helper, full, f.wall, false);
                expectTemperature(helper, full, sameBrick, sameTemperature);
                expectTemperature(helper, full, otherBrick, otherTemperature);
                // A->Air->A must still be treated as a replacement while geometry is pending.
                f.level.setBlockAndUpdate(f.wall, Blocks.STONE.defaultBlockState());
                InfraredSnapshot replaced = sample(f.player, before, true, before.presence());
                expectMissing(helper, replaced, f.wall, false);
                expectTemperature(helper, replaced, sameBrick, sameTemperature);
                wire(helper, delta);
            } finally { f.close(); }
        }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "surface_restart", timeoutTicks = 700)
    public static void restartedRuntimeCannotReuseTheOldMaterialBaseline(GameTestHelper helper) {
        LiveFixture f = new LiveFixture(helper);
        InfraredSnapshot[] before = new InfraredSnapshot[1];
        helper.startSequence().thenWaitUntil(() -> {
            var result = sample(f.player, null, true, EMPTY);
            helper.assertTrue(result != null && result.readable() && present(result, f.wall), "first runtime must publish");
            before[0] = result;
        }).thenExecute(() -> {
            MinecraftThermalInput.closeActiveLevel(f.level);
            f.input = start(f.level, f.target);
        }).thenWaitUntil(() -> {
            var result = sample(f.player, before[0], false, before[0].presence());
            helper.assertTrue(result != null && result.readable() && present(result, f.wall), "restarted runtime must publish");
            helper.assertTrue(result.generation() != before[0].generation() && result.full(), "new generation requires full even at same center");
            wire(helper, result);
        }).thenExecute(f::close).thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "surface_wire", timeoutTicks = 100)
    public static void fullWindowPartitionsPreserveEveryBrickAndFieldFootprint(GameTestHelper helper) {
        byte[][] parts;
        short[] values = new short[64];
        try (var builder = new InfraredBrickCodec.Builder()) {
            for (int page=0;page<729;page++) {
                builder.beginPage();
                for (int brick=0;brick<64;brick++) {
                    int address=page*64+brick;
                    for (int block=0;block<64;block++) values[block]=(short)(address*73+block*461);
                    builder.writeBrick(address, values, false);
                }
            }
            parts=builder.finishParts();
        }
        long[] presence = new long[12]; Arrays.fill(presence,-1L); presence[11]=(1L<<25)-1;
        InfraredSnapshot full = new InfraredSnapshot(-3,5,7,93,11,true,true,presence,presence,parts, 123456789012L, 9876543210L);
        helper.assertTrue(parts.length > 1, "fixture must cross the real wire limit");
        int expected=0;
        for (byte[] part:parts) {
            FriendlyByteBuf b=new FriendlyByteBuf(Unpooled.wrappedBuffer(part));
            var decoder=new InfraredBrickCodec.Decoder();
            try {
                int address;
                while((address=decoder.readRecord(b,values))>=0) {
                    helper.assertTrue(address==expected++, "partition must preserve ordered Brick addresses");
                    for(int block=0;block<64;block++) helper.assertTrue(values[block]==(short)(address*73+block*461), "RAW values must survive");
                }
            } finally { b.release(); }
        }
        helper.assertTrue(expected==729*64, "all Bricks must survive partitioning");
        wire(helper, full);
        helper.succeed();
    }
    @GameTest(template = TEMPLATE, batch = "thermal_biome_zero_cache", timeoutTicks = 100)
    public static void zeroBiomeTemperatureIsCachedUntilExplicitInvalidation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos position = helper.absolutePos(new BlockPos(2, 3, 2));
        var biome = level.getBiome(position).value();
        var key = level.registryAccess().registryOrThrow(Registries.BIOME).getKey(biome);
        var previous = BiomeTempData.cacheList;
        var temperatures = new HashMap<>(previous);
        try {
            temperatures.put(key, new BiomeTempData(key, 0));
            BiomeTempData.cacheList = Map.copyOf(temperatures);
            WorldTemperature.clear();
            helper.assertTrue(WorldTemperature.biome(level, position) == 0, "zero must enter the ordinary biome cache");
            temperatures.put(key, new BiomeTempData(key, 15));
            BiomeTempData.cacheList = Map.copyOf(temperatures);
            helper.assertTrue(WorldTemperature.biome(level, position) == 0,
                    "a cached zero must behave like other cached values until invalidated");
            WorldTemperature.clear();
            helper.assertTrue(WorldTemperature.biome(level, position) == 15, "invalidation must expose updated biome data");
            helper.succeed();
        } finally {
            BiomeTempData.cacheList = previous;
            WorldTemperature.clear();
        }
    }

    @GameTest(template = TEMPLATE, batch = "thermal_lava_section_edges", timeoutTicks = 200)
    public static void lavaSurfaceReadsDiagonalSectionsAtUpperChunkEdges(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos anchor = loadedCenter(helper);
        int cx = anchor.getX() >> 4, cz = anchor.getZ() >> 4;
        BlockPos corner = new BlockPos(cx * 16, (anchor.getY() & ~15) + 15, cz * 16);
        BlockPos[] lava = {corner.south(7), corner.south(7).west(), corner.east(7), corner.east(7).north()};
        for (int[] offset : new int[][]{{0, 0}, {-1, 0}, {0, -1}}) level.setChunkForced(cx + offset[0], cz + offset[1], true);
        MinecraftThermalInput.closeActiveLevel(level);
        for (BlockPos pos : lava) level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
        MinecraftThermalInput input = start(level, corner.offset(4, 2, 4));
        BlockRadiationIndex index = read(input, "blockRadiation");
        helper.assertTrue(index != null, "static lava radiation must be enabled");
        helper.startSequence().thenWaitUntil(() -> {
            boolean[] found = {false, false};
            index.visitNearby(corner.getX() + 4.5, corner.getY() + 2, corner.getZ() + 4.5, 128,
                    (key, revision, x, y, z, power, bound) -> {
                        if (power > 0) {
                            found[0] |= Math.abs(z - corner.getZ() - 7.5) < 1;
                            found[1] |= Math.abs(x - corner.getX() - 7.5) < 1;
                        }
                        return true;
                    });
            helper.assertTrue(found[0] && found[1], "real lava surface compilation must complete across X/Y and Z/Y edges");
        }).thenExecute(() -> {
            for (BlockPos pos : lava) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            MinecraftThermalInput.closeActiveLevel(level);
            for (int[] offset : new int[][]{{0, 0}, {-1, 0}, {0, -1}}) level.setChunkForced(cx + offset[0], cz + offset[1], false);
        }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "surface_depth", timeoutTicks = 100)
    public static void depthReconstructionSelectsSixMaterialSidesAtNearAndFarCoordinates(GameTestHelper helper) {
        int cases = 0;
        double maximumDisplacement = 0;
        for (double origin : new double[]{0, -16777200, 29999000})
            for (int axis = 0; axis < 3; axis++) for (int sign : new int[]{-1, 1})
                for (float distance : new float[]{1, 4, 16, 64}) for (float slope : new float[]{0, .7f}) {
                    var hit = new org.joml.Vector3d(origin + .5, 200.5, origin + .5);
                    hit.setComponent(axis, Math.floor(hit.get(axis)));
                    var ray = new org.joml.Vector3f().setComponent(axis, sign);
                    ray.setComponent((axis + 1) % 3, slope).normalize().mul(distance);
                    var camera = new org.joml.Vector3d(hit).sub(ray.x, ray.y, ray.z);
                    var vp = new org.joml.Matrix4f().perspective((float)Math.toRadians(70), 16f/9, .05f, 1024)
                            .lookAt(new org.joml.Vector3f(), ray, new org.joml.Vector3f().setComponent((axis+2)%3, 1));
                    var inverse = new org.joml.Matrix4f(vp).invert();
                    var clip = vp.transform(new org.joml.Vector4f(ray, 1)); clip.div(clip.w);
                    float depth = (float)(Math.round((clip.z*.5+.5)*16777215)/16777215.0);
                    var behind = inverse.transform(new org.joml.Vector4f(clip.x, clip.y, (depth+2f/16777215)*2-1, 1));
                    behind.div(behind.w);
                    float fraction = (float)(camera.get(axis)-Math.floor(camera.get(axis)));
                    long selected = (long)Math.floor(camera.get(axis)) + (long)Math.floor(fraction+behind.get(axis));
                    long expected = (long)hit.get(axis) - (sign < 0 ? 1 : 0);
                    helper.assertTrue(selected == expected, "depth selected wrong side: axis="+axis+", sign="+sign
                            +", distance="+distance+", origin="+origin+", selected="+selected+", expected="+expected);
                    double displacement = new org.joml.Vector3f(behind.x, behind.y, behind.z).distance(ray);
                    helper.assertTrue(displacement < .5, "ordinary plane must remain inside material lookup displacement limit");
                    maximumDisplacement = Math.max(maximumDisplacement, displacement);
                    cases++;
                }
        FHMain.LOGGER.info("Surface depth: {} six-face/angle/distance/world-origin cases; max displacement {} blocks",
                cases, maximumDisplacement);
        helper.succeed();
    }

    private static void measureSurfaceRequests(ServerPlayer player, InfraredSnapshot baseline) {
        // One real source scene, 100 sequential client-carried baselines; no server observer state.
        for (boolean full : new boolean[]{false, true}) {
            long[] nanos = new long[100];
            long bytes = 0;
            int responses = 0;
            FriendlyByteBuf wire = new FriendlyByteBuf(Unpooled.buffer());
            try {
                for (int i = 0; i < nanos.length; i++) {
                    long start = System.nanoTime();
                    InfraredSnapshot result = sample(player, baseline, full, baseline.presence());
                    nanos[i] = System.nanoTime() - start;
                    if (result == null) continue;
                    responses++;
                    for (int part = 0; part < Math.max(1, result.brickRecords().length); part++) {
                        wire.clear();
                        new FHResponseInfraredViewDataSyncPacket(i, result, part).encode(wire);
                        bytes += wire.readableBytes();
                    }
                }
            } finally { wire.release(); }
            Arrays.sort(nanos);
            FHMain.LOGGER.info("Surface request sample: full={}, count=100, capture median={} ms, p95={} ms, responses={}, wire={} bytes",
                    full, nanos[50]/1e6, nanos[94]/1e6, responses, bytes);
        }
    }
    private static BlockPos loadedCenter(GameTestHelper helper) {
        BlockPos anchor = helper.absolutePos(BlockPos.ZERO);
        BlockPos center = new BlockPos(-128 - (anchor.getX() & ~15) + 8,
                ((helper.getLevel().getMaxBuildHeight() - 64) & ~15) + 8,
                -128 - (anchor.getZ() & ~15) + 8);
        for (int dz = -3; dz <= 3; dz++) {
            for (int dx = -3; dx <= 3; dx++) {
                helper.getLevel().getChunk((center.getX() >> 4) + dx, (center.getZ() >> 4) + dz);
            }
        }
        return center;
    }

    private static ServerPlayer observer(ServerLevel level, BlockPos center) {
        ServerPlayer result = FakePlayerFactory.getMinecraft(level);
        result.setPos(center.getX()+.5, center.getY(), center.getZ()+.5);
        return result;
    }
    private static long center(InfraredSnapshot s) { return SectionPos.asLong(s.centerChunkX(),s.centerSectionY(),s.centerChunkZ()); }
    private static InfraredSnapshot sample(ServerPlayer p, InfraredSnapshot previous, boolean full, long[] presence) {
        return MinecraftThermalInput.gameplayInfraredSnapshot(p,full,previous==null?0:previous.generation(),
                previous==null?0:center(previous),previous==null?0:previous.infraredEpoch(),presence,
                previous!=null&&previous.readable(),previous==null?EMPTY:previous.fieldPages(), previous==null?0:previous.storedEpoch(),
                previous==null?0:previous.storedSampleTick());
    }
    private static int page(InfraredSnapshot s, BlockPos p) {
        int x=(p.getX()>>4)-s.centerChunkX()+4, y=(p.getY()>>4)-s.centerSectionY()+4, z=(p.getZ()>>4)-s.centerChunkZ()+4;
        return x+9*(z+9*y);
    }
    private static boolean present(InfraredSnapshot s, BlockPos p) {
        int index=page(s,p);
        return s.presence().length==12 && (s.presence()[index>>>6] & 1L<<(index&63))!=0;
    }
    private static Short temperature(InfraredSnapshot s, BlockPos p) {
        int address=page(s,p)*64+((p.getX()&15)>>>2)+(((p.getZ()&15)>>>2)<<2)+(((p.getY()&15)>>>2)<<4);
        int block=(p.getX()&3)|((p.getZ()&3)<<2)|((p.getY()&3)<<4);
        short[] values=new short[64]; var decoder=new InfraredBrickCodec.Decoder();
        for(byte[] bytes:s.brickRecords()) {
            FriendlyByteBuf b=new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
            try { int a; while((a=decoder.readRecord(b,values))>=0) if(a==address) return values[block]; }
            finally { b.release(); }
        }
        return null;
    }
    private static void expectTemperature(GameTestHelper h, InfraredSnapshot s, BlockPos p, double raw) {
        Short encoded=temperature(s,p);
        h.assertTrue(encoded!=null && encoded!=InfraredBrickCodec.INVALID_TEMPERATURE && Math.abs(encoded*.25-raw)<=.3,
                "material position must match solver surface: position="+p+", encoded="+encoded+", solver="+raw);
    }
    private static double material(MinecraftThermalInput input, BlockPos p) {
        MinecraftPageManager pages=read(input,"pages");
        var owner=pages.loadedSectionOrAttach(SectionPos.asLong(p));
        var handle=owner==null?null:owner.page();
        var publication=handle==null?null:handle.currentPublication();
        if(publication==null) return Double.NaN;
        var brick=publication.brickAt(p.getX()&15,p.getY()&15,p.getZ()&15);
        if(brick.blockLayout()==null || brick.firstSlot()<0) return Double.NaN;
        int node=brick.blockLayout().nodeAt((p.getX()&3)|(p.getZ()&3)<<2|(p.getY()&3)<<4);
        if(node<0 || (brick.blockLayout().surfaceNodeMask() & 1L<<node)==0) return Double.NaN;
        QueryPublication queries=read(input,"queryPublication"); var sample=new QueryPublication.MutableSample();
        return queries.tryRead(brick.firstSlot()+node,brick.arenaGeneration(),publication.topologyGeneration(),sample)
                ? sample.temperatureC() : Double.NaN;
    }
    private static double rawAir(MinecraftThermalInput input, ServerLevel level, BlockPos p) { return rawAir(input,p); }
    private static double rawAir(MinecraftThermalInput input, BlockPos p) {
        MinecraftPageManager pages=read(input,"pages"); var owner=pages.loadedSectionOrAttach(SectionPos.asLong(p));
        var publication=owner==null||owner.page()==null?null:owner.page().currentPublication();
        if(publication==null) return Double.NaN;
        int slot=publication.resolveAirPoint(p.getX()&15,p.getY()&15,p.getZ()&15);
        if(slot<0) return Double.NaN;
        var brick=publication.brickAt(p.getX()&15,p.getY()&15,p.getZ()&15);
        QueryPublication queries=read(input,"queryPublication"); var sample=new QueryPublication.MutableSample();
        return queries.tryRead(slot,brick.arenaGeneration(),publication.topologyGeneration(),sample)?sample.temperatureC():Double.NaN;
    }
    private static void expectMissing(GameTestHelper h, InfraredSnapshot s, BlockPos p, boolean explicit) {
        Short value = temperature(s,p);
        h.assertTrue((value == null && !explicit) || Objects.equals(value, InfraredBrickCodec.INVALID_TEMPERATURE),
                "missing display must be INVALID: " + p + ", value=" + value);
    }    private static void wire(GameTestHelper h, InfraredSnapshot s) {
        FriendlyByteBuf b=new FriendlyByteBuf(Unpooled.buffer()), copy=new FriendlyByteBuf(Unpooled.buffer());
        try {
            for (boolean full : new boolean[]{false,true}) {
                b.clear();
                var request=new FHRequestInfraredViewDataSyncPacket(17,full,s.generation(),center(s),s.infraredEpoch(),
                        s.presence().length==12?s.presence():EMPTY,s.readable(),s.fieldPages(),s.storedEpoch(),s.storedSampleTick());
                request.encode(b); var decoded=new FHRequestInfraredViewDataSyncPacket(b);
                h.assertTrue(!b.isReadable() && decoded.knownReadable()==s.readable()
                        && decoded.knownStoredEpoch()==s.storedEpoch()
                        && Arrays.equals(decoded.knownFieldPages(),full?new long[0]:s.fieldPages()),"request field footprint round trip");
            }            int count=Math.max(1,s.brickRecords().length);
            for(int part=0;part<count;part++) {
                b.clear(); copy.clear();
                new FHResponseInfraredViewDataSyncPacket(17,s,part).encode(b);
                h.assertTrue(b.readableBytes()<=InfraredBrickCodec.MAX_PACKET_BYTES,"whole response exceeds wire budget");
                byte[] original=ByteBufUtil.getBytes(b);
                var response=new FHResponseInfraredViewDataSyncPacket(b);
                h.assertTrue(!b.isReadable() && response.firstPart()==(part==0) && response.lastPart()==(part==count-1),"part boundaries");
                h.assertTrue(response.snapshot().readable()==s.readable(),"independent control flags");
                h.assertTrue(response.snapshot().storedEpoch()==s.storedEpoch(),"stored material revision survives each part");
                h.assertTrue(Arrays.equals(response.snapshot().fieldPages(),s.fieldPages()),"each part retains the final field footprint");
                response.encode(copy);
                h.assertTrue(Arrays.equals(original,ByteBufUtil.getBytes(copy)),"response must preserve complete wire data");
            }
        } finally { b.release();copy.release(); }
    }
    private static final class LiveFixture implements AutoCloseable {
        final ServerLevel level; final BlockPos target, wall, other; final ServerPlayer player;
        MinecraftThermalInput input;
        LiveFixture(GameTestHelper h) {
            level=h.getLevel(); target=loadedCenter(h); wall=target.east(); other=target.east(32);
            MinecraftThermalInput.closeActiveLevel(level);
            for(BlockPos air:new BlockPos[]{target,other}) {
                level.setChunkForced(air.getX()>>4,air.getZ()>>4,true);
                level.setBlockAndUpdate(air,Blocks.AIR.defaultBlockState());
                for(Direction d:Direction.values()) if(d!=Direction.DOWN) level.setBlockAndUpdate(air.relative(d),Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(air.below(),Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT,true));
                ((ICampfireExtra)level.getBlockEntity(air.below())).setLifeTime(20_000);
            }
            player=observer(level,target); input=start(level,target);
        }
        @Override public void close() {
            MinecraftThermalInput.closeActiveLevel(level);
            for(BlockPos air:new BlockPos[]{target,other}) {
                level.setBlockAndUpdate(air.below(),Blocks.AIR.defaultBlockState());
                level.setChunkForced(air.getX()>>4,air.getZ()>>4,false);
            }
        }
    }
}
