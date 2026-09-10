/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.data.BiomeTempData;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorData;
import com.teammoeg.frostedheart.content.climate.network.FHRequestInfraredViewDataSyncPacket;
import com.teammoeg.frostedheart.content.climate.network.FHResponseInfraredViewDataSyncPacket;
import com.teammoeg.frostedheart.content.climate.network.InfraredBrickCodec;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField.CombineMode;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField.Shape;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalFieldKey;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.minecraft.BlockRadiationIndex;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.InfraredSnapshot;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.PhysicalSourceSpatialIndex;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ThermalLoadedWorldGameTests.read;
import static com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ThermalLoadedWorldGameTests.start;

@GameTestHolder(FHMain.MODID)
@PrefixGameTestTemplate(false)
public final class ThermalInfraredGameTests {
    private static final String TEMPLATE = "phase0a_empty";
    private static final ResourceLocation PROVIDER = new ResourceLocation("frostedheart", "gametest_infrared");
    private static final long[] EMPTY_PAGES = new long[12];

    @GameTest(template = TEMPLATE, batch = "thermal_infrared_generator", timeoutTicks = 100)
    public static void generatorDisplayMovesAndExpiresWithoutStartingPhysicalPages(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = loadedCenter(helper);
        ServerPlayer player = observer(level, center);
        MinecraftThermalInput.closeActiveLevel(level);
        GeneratorData generator = new GeneratorData(null);
        generator.actualPos = center;
        generator.dimension = level.dimension();
        generator.RLevel = 1;
        generator.TLevel = 3;
        UUID owner = UUID.randomUUID();
        BlockPos otherCenter = center.south(10);
        ThermalFieldKey other = key(center, 0);
        try {
            generator.publishGameplayHeat(level, owner);
            register(level, other, CombineMode.OVERRIDE, Shape.SPHERE, otherCenter, 1, 1, 1, 77);
            InfraredSnapshot first = capture(helper, player, null, "generator r16");
            temperature(helper, first, center, WorldTemperature.naturalAir(level, center) + generator.getTempMod());
            BlockPos oldEdge = center.west(generator.getRadius());
            temperature(helper, first, oldEdge, WorldTemperature.naturalAir(level, oldEdge) + generator.getTempMod());
            temperature(helper, first, otherCenter, 77);
            noRuntime(helper, level);
            wireRoundTrip(helper, first);

            generator.actualPos = center.east(4);
            generator.publishGameplayHeat(level, owner);
            InfraredSnapshot moved = capture(helper, player, first, "generator same-Page edge exit");
            invalid(helper, moved, oldEdge);
            helper.assertTrue(page(first, oldEdge) == page(moved, oldEdge)
                    && bit(moved.refreshPages(), page(moved, oldEdge)),
                    "the old edge must be restored inside a Page that still contains the moved field");
            temperature(helper, moved, otherCenter, 77);

            generator.RLevel = 2;
            generator.publishGameplayHeat(level, owner);
            InfraredSnapshot expanded = capture(helper, player, moved, "generator r24");
            temperature(helper, expanded, oldEdge, WorldTemperature.naturalAir(level, oldEdge) + generator.getTempMod());

            generator.RLevel = 0.125F;
            generator.publishGameplayHeat(level, owner);
            helper.assertTrue(generator.getRadius() < 4, "fixture requires a small generator field after shrinking");
            InfraredSnapshot shrunk = capture(helper, player, expanded, "generator shrink");
            invalid(helper, shrunk, center);

            BlockPos previousCenter = generator.actualPos;
            generator.actualPos = center.east(24);
            generator.publishGameplayHeat(level, owner);
            InfraredSnapshot crossed = capture(helper, player, shrunk, "generator cross-Page move");
            invalid(helper, crossed, previousCenter);
            temperature(helper, crossed, generator.actualPos,
                    WorldTemperature.naturalAir(level, generator.actualPos) + generator.getTempMod());

            generator.removeGameplayHeat(level.getServer());
            InfraredSnapshot removed = capture(helper, player, crossed, "generator removal");
            invalid(helper, removed, generator.actualPos);
            temperature(helper, removed, otherCenter, 77);
            MinecraftThermalInput.removeGameplayAnalyticField(level, other);
            InfraredSnapshot empty = capture(helper, player, removed, "last field removal");
            invalid(helper, empty, otherCenter);
            noRuntime(helper, level);
            helper.succeed();
        } finally {
            generator.removeGameplayHeat(level.getServer());
            MinecraftThermalInput.removeGameplayAnalyticField(level, other);
        }
    }

    @GameTest(template = TEMPLATE, batch = "thermal_infrared_shapes", timeoutTicks = 100)
    public static void bossShapesAndOrderedModesReachTheExistingBrickCodec(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = loadedCenter(helper);
        ServerPlayer player = observer(level, center);
        MinecraftThermalInput.closeActiveLevel(level);
        ThermalFieldKey[] keys = {key(center, 1), key(center, 2), key(center, 3), key(center, 4)};
        try {
            register(level, keys[0], CombineMode.OVERRIDE, Shape.CUBE, center, 8, 8, 8, 40);
            register(level, keys[1], CombineMode.MAX_HEAT, Shape.SPHERE, center, 4, 4, 4, 50);
            register(level, keys[2], CombineMode.MIN_COOL, Shape.PILLAR, center, 3, 2, 6, 45);
            register(level, keys[3], CombineMode.ADD_DELTA, Shape.SPHERE, center, 2, 2, 2, -7);
            InfraredSnapshot snapshot = capture(helper, player, null, "Boss shapes and composition");
            temperature(helper, snapshot, center, 38);
            temperature(helper, snapshot, center.east(5), 40);
            temperature(helper, snapshot, center.east(4), 50);
            temperature(helper, snapshot, center.above(4), 50);
            temperature(helper, snapshot, center.below(4), 45);
            temperature(helper, snapshot, center.offset(7, 7, 7), 40);
            invalid(helper, snapshot, center.east(9));
            noRuntime(helper, level);
            for (ThermalFieldKey fieldKey : keys) MinecraftThermalInput.removeGameplayAnalyticField(level, fieldKey);
            invalid(helper, capture(helper, player, snapshot, "Boss removal"), center);
            helper.succeed();
        } finally {
            for (ThermalFieldKey fieldKey : keys) MinecraftThermalInput.removeGameplayAnalyticField(level, fieldKey);
        }
    }

    @GameTest(template = TEMPLATE, batch = "thermal_infrared_large_payload", timeoutTicks = 1000)
    public static void largeGeneratorFieldFitsTheProductionDisplayPayload(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos loaded = loadedCenter(helper);
        BlockPos center = new BlockPos(loaded.getX(), 240, loaded.getZ());
        for (int dz = -5; dz <= 5; dz++) {
            for (int dx = -5; dx <= 5; dx++) {
                level.getChunk((center.getX() >> 4) + dx, (center.getZ() >> 4) + dz);
            }
        }
        ServerPlayer player = observer(level, center);
        MinecraftThermalInput.closeActiveLevel(level);
        GeneratorData generator = new GeneratorData(null);
        generator.actualPos = center;
        generator.dimension = level.dimension();
        generator.RLevel = 15;
        generator.TLevel = 3;
        try {
            generator.publishGameplayHeat(level, UUID.randomUUID());
            helper.assertTrue(generator.getRadius() == 128, "large-field fixture requires radius128");
            FHMain.LOGGER.info("Infrared large-field fixture: radius={}, center={}, loaded Chunk square=11x11",
                    generator.getRadius(), center);
            InfraredSnapshot snapshot = capture(helper, player, null, "generator r128 full display window");
            helper.assertTrue(snapshot.brickRecords().length > 1, "r128 regression must exercise multiple bounded packets");
            temperature(helper, snapshot, center,
                    WorldTemperature.naturalAir(level, center) + generator.getTempMod());
            temperature(helper, snapshot, center.above(64),
                    WorldTemperature.naturalAir(level, center.above(64)) + generator.getTempMod());
            wireRoundTrip(helper, snapshot);
            for (int repeat = 1; repeat <= 2; repeat++) {
                snapshot = capture(helper, player, snapshot, "generator r128 repeat " + repeat);
                temperature(helper, snapshot, center.above(64),
                        WorldTemperature.naturalAir(level, center.above(64)) + generator.getTempMod());
                wireRoundTrip(helper, snapshot);
            }
            noRuntime(helper, level);
            helper.succeed();
        } finally {
            generator.removeGameplayHeat(level.getServer());
        }
    }

    @GameTest(template = TEMPLATE, batch = "thermal_infrared_live", timeoutTicks = 400)
    public static void movingDisplayFieldRestoresHotterCampfirePublication(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos target = loadedCenter(helper);
        BlockPos fire = target.below();
        ServerPlayer player = observer(level, target);
        MinecraftThermalInput.closeActiveLevel(level);
        int chunkX = target.getX() >> 4;
        int chunkZ = target.getZ() >> 4;
        level.setChunkForced(chunkX, chunkZ, true);
        level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        for (Direction direction : Direction.values()) {
            if (direction != Direction.DOWN) level.setBlockAndUpdate(target.relative(direction), Blocks.STONE.defaultBlockState());
        }
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState());
        ((ICampfireExtra) level.getBlockEntity(fire)).setLifeTime(20_000);
        ThermalFieldKey floor = key(target, 5);
        ThermalFieldKey override = key(target, 6);
        register(level, floor, CombineMode.FLOOR_FROM_NATURAL, Shape.SPHERE, target, 1, 1, 1, 1);
        MinecraftThermalInput input = start(level, target);
        PhysicalSourceSpatialIndex sources = read(input, "physicalSources");
        Long2IntOpenHashMap sourceSlots = read(sources, "slotsById");
        MinecraftThermalInput.gameplayInfraredSnapshot(player, true, 0, EMPTY_PAGES, 0, EMPTY_PAGES, EMPTY_PAGES);
        InfraredSnapshot[] ready = new InfraredSnapshot[1];
        helper.startSequence().thenWaitUntil(() -> {
            double natural = WorldTemperature.naturalAir(level, target);
            double raw = rawAir(input, level, target);
            var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
            helper.assertTrue(Double.isFinite(raw) && raw > natural + 1.5,
                    "actual campfire publication must warm above the analytic floor: raw=" + raw + ", natural=" + natural
                            + ", chunkLoaded=" + (chunk != null) + ", sourcePresent=" + sourceSlots.containsKey(fire.asLong())
                            + ", target=" + (chunk == null ? "unloaded" : chunk.getBlockState(target))
                            + ", fire=" + (chunk == null ? "unloaded" : chunk.getBlockState(fire)));
            InfraredSnapshot snapshot = MinecraftThermalInput.gameplayInfraredSnapshot(
                    player, true, 0, EMPTY_PAGES, 0, EMPTY_PAGES, EMPTY_PAGES);
            helper.assertTrue(snapshot != null, "infrared publication must become readable");
            Short value = encodedTemperature(snapshot, target);
            helper.assertTrue(value != null && value != InfraredBrickCodec.INVALID_TEMPERATURE
                            && value * .25 > natural + 1.25,
                    "encoded physical temperature must exceed the generator-style floor");
            ready[0] = snapshot;
        }).thenExecute(() -> {
            try {
                register(level, override, CombineMode.OVERRIDE, Shape.SPHERE, target, 1, 1, 1, 200);
                InfraredSnapshot covered = capture(helper, player, ready[0], "override over live campfire");
                temperature(helper, covered, target, 200);
                register(level, override, CombineMode.OVERRIDE, Shape.SPHERE, target.east(4), 1, 1, 1, 200);
                InfraredSnapshot restored = capture(helper, player, covered, "restore physical after same-Page move",
                        ready[0].presence());
                Short value = encodedTemperature(restored, target);
                double raw = rawAir(input, level, target);
                helper.assertTrue(value != null && value != InfraredBrickCodec.INVALID_TEMPERATURE
                                && Math.abs(value * .25 - raw) < .5,
                        "field exit must restore actual hotter physical temperature: encoded=" + value + ", raw=" + raw);
                MinecraftThermalInput.removeGameplayAnalyticField(level, floor);
                MinecraftThermalInput.removeGameplayAnalyticField(level, override);
                InfraredSnapshot physicalDisplay = capture(helper, player, null, "physical mixed Brick without fields");
                Short physicalValue = encodedTemperature(physicalDisplay, target);
                helper.assertTrue(physicalValue != null && physicalValue != InfraredBrickCodec.INVALID_TEMPERATURE
                                && Math.abs(physicalValue * .25 - rawAir(input, level, target)) < .5,
                        "ordinary physical encoding must retain the actual hot air temperature");
                invalid(helper, physicalDisplay, target.east());
                wireRoundTrip(helper, physicalDisplay);
                register(level, floor, CombineMode.FLOOR_FROM_NATURAL, Shape.SPHERE, target, 1, 1, 1, 1);
                QueryPublication queries = read(input, "queryPublication");
                // A closed publication is a real unavailable read, not an empty
                // physical world. A full response must not clear the old display.
                queries.close();
                InfraredSnapshot incomplete = MinecraftThermalInput.gameplayInfraredSnapshot(
                        player, true, restored.infraredEpoch(), ready[0].presence(),
                        restored.dormantRevision(), EMPTY_PAGES, restored.refreshPages());
                helper.assertTrue(incomplete == null, "an incomplete full capture must wait instead of clearing the client");
                scratchReleased(helper);
                MinecraftThermalInput.removeGameplayAnalyticField(level, floor);
                MinecraftThermalInput.removeGameplayAnalyticField(level, override);
                helper.assertTrue(!MinecraftThermalInput.hasGameplayAnalyticFieldAt(level, target),
                        "the second failed full read must exercise a physical-only target");
                InfraredSnapshot physicalOnly = MinecraftThermalInput.gameplayInfraredSnapshot(
                        player, true, restored.infraredEpoch(), ready[0].presence(),
                        restored.dormantRevision(), EMPTY_PAGES, EMPTY_PAGES);
                helper.assertTrue(physicalOnly == null, "unreadable physical-only full capture must also preserve the old display");
                scratchReleased(helper);
            } finally {
                MinecraftThermalInput.removeGameplayAnalyticField(level, floor);
                MinecraftThermalInput.removeGameplayAnalyticField(level, override);
                level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
                for (Direction direction : Direction.values()) {
                    if (direction != Direction.DOWN) level.setBlockAndUpdate(target.relative(direction), Blocks.AIR.defaultBlockState());
                }
                MinecraftThermalInput.closeActiveLevel(level);
                level.setChunkForced(chunkX, chunkZ, false);
            }
        }).thenSucceed();
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

    @GameTest(template = TEMPLATE, batch = "thermal_infrared_biomes", timeoutTicks = 100)
    public static void naturalLayerReuseMatchesEveryBlockAcrossRealBiomeBoundaries(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = loadedCenter(helper);
        ServerPlayer player = observer(level, center);
        MinecraftThermalInput.closeActiveLevel(level);
        var chunk = level.getChunkAt(center);
        int sectionY = center.getY() >> 4;
        var section = chunk.getSection(chunk.getSectionIndex(center.getY()));
        var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        var plains = registry.getHolderOrThrow(Biomes.PLAINS);
        var desert = registry.getHolderOrThrow(Biomes.DESERT);
        var sampler = level.getChunkSource().randomState().sampler();
        ArrayList<Holder<Biome>> saved = new ArrayList<>(64);
        for (int i = 0; i < 64; i++) saved.add(section.getNoiseBiome(i & 3, i >>> 4, i >>> 2 & 3));
        var previousTemperatures = BiomeTempData.cacheList;
        var temperatures = new HashMap<>(previousTemperatures);
        temperatures.put(Biomes.PLAINS.location(), new BiomeTempData(Biomes.PLAINS.location(), -10));
        temperatures.put(Biomes.DESERT.location(), new BiomeTempData(Biomes.DESERT.location(), 20));
        ThermalFieldKey field = key(center, 9);
        try {
            BiomeTempData.cacheList = Map.copyOf(temperatures);
            WorldTemperature.clear();
            register(level, field, CombineMode.FLOOR_FROM_NATURAL, Shape.SPHERE, center, 8, 8, 8, 7);
            InfraredSnapshot previous = null;
            for (int pass = 0; pass < 2; pass++) {
                boolean mixed = pass == 0;
                section.fillBiomesFromNoise((qx, qy, qz, climate) -> mixed && (qx & 1) != 0 ? desert : plains,
                        sampler, (center.getX() >> 4) * 4, sectionY * 4, (center.getZ() >> 4) * 4);
                InfraredSnapshot snapshot = capture(helper, player, previous, mixed ? "mixed biome boundary" : "uniform biome layers");
                boolean sawCold = false, sawHot = false;
                for (int block = 0; block < 64; block++) {
                    BlockPos pos = new BlockPos((center.getX() & ~3) + (block & 3),
                            (center.getY() & ~3) + (block >>> 4), (center.getZ() & ~3) + (block >>> 2 & 3));
                    temperature(helper, snapshot, pos, WorldTemperature.naturalAir(level, pos) + 7);
                    float biome = WorldTemperature.biome(level, pos);
                    sawCold |= biome == -10;
                    sawHot |= biome == 20;
                }
                helper.assertTrue(sawCold && (!mixed || sawHot), "fixture must exercise actual biome variation within one Brick");
                previous = snapshot;
            }
            noRuntime(helper, level);
            helper.succeed();
        } finally {
            section.fillBiomesFromNoise((qx, qy, qz, climate) -> saved.get((qx & 3) | (qz & 3) << 2 | (qy & 3) << 4),
                    sampler, (center.getX() >> 4) * 4, sectionY * 4, (center.getZ() >> 4) * 4);
            BiomeTempData.cacheList = previousTemperatures;
            WorldTemperature.clear();
            MinecraftThermalInput.removeGameplayAnalyticField(level, field);
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
        ServerPlayer player = FakePlayerFactory.getMinecraft(level);
        player.setPos(center.getX() + .5, center.getY(), center.getZ() + .5);
        return player;
    }

    private static ThermalFieldKey key(BlockPos center, int id) {
        return new ThermalFieldKey(PROVIDER, 0, center.asLong(), id);
    }

    private static void register(ServerLevel level, ThermalFieldKey key, CombineMode mode, Shape shape,
            BlockPos center, double radius, double upper, double lower, double temperature) {
        MinecraftThermalInput.upsertGameplayAnalyticField(level, new ThermalAnalyticField(key, 0, mode, shape,
                center.getX() + .5, center.getY() + .5, center.getZ() + .5, radius, upper, lower, temperature));
    }

    private static InfraredSnapshot capture(GameTestHelper helper, ServerPlayer player, InfraredSnapshot previous, String label) {
        return capture(helper, player, previous, label,
                previous == null || previous.presence().length == 0 ? EMPTY_PAGES : previous.presence());
    }

    private static InfraredSnapshot capture(GameTestHelper helper, ServerPlayer player, InfraredSnapshot previous,
            String label, long[] knownPresence) {
        long started = System.nanoTime();
        InfraredSnapshot snapshot = MinecraftThermalInput.gameplayInfraredSnapshot(player, previous == null,
                previous == null ? 0 : previous.infraredEpoch(),
                knownPresence,
                previous == null ? 0 : previous.dormantRevision(), EMPTY_PAGES,
                previous == null ? EMPTY_PAGES : previous.refreshPages());
        helper.assertTrue(snapshot != null, label + " must produce a display update");
        scratchReleased(helper);
        int bytes = 0;
        for (byte[] part : snapshot.brickRecords()) {
            helper.assertTrue(part.length <= InfraredBrickCodec.MAX_PAYLOAD_BYTES,
                    label + " part must fit the production payload");
            bytes += part.length;
        }
        FHMain.LOGGER.info("Infrared production capture {}: {} ms, {} bytes in {} parts, {} refresh Pages", label,
                (System.nanoTime() - started) / 1_000_000.0, bytes, snapshot.brickRecords().length,
                Arrays.stream(snapshot.refreshPages()).map(Long::bitCount).sum());
        return snapshot;
    }

    private static void scratchReleased(GameTestHelper helper) {
        try {
            Field captureField = MinecraftThermalInput.class.getDeclaredField("infraredCapture");
            captureField.setAccessible(true);
            Object capture = captureField.get(null);
            Object[] handles = read(capture, "infraredHandles");
            for (Object handle : handles) helper.assertTrue(handle == null, "capture must release borrowed Page handles");
            QueryPublication.InfraredReadCursor cursor = read(capture, "infraredCursor");
            helper.assertTrue(!cursor.valid() && read(cursor, "owner") == null
                            && read(cursor, "temperaturesC") == null,
                    "capture must release borrowed publication buffers after every response");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static int page(InfraredSnapshot snapshot, BlockPos position) {
        int x = (position.getX() >> 4) - snapshot.centerChunkX() + 4;
        int y = (position.getY() >> 4) - snapshot.centerSectionY() + 4;
        int z = (position.getZ() >> 4) - snapshot.centerChunkZ() + 4;
        if (x < 0 || x >= 9 || y < 0 || y >= 9 || z < 0 || z >= 9) throw new AssertionError("sample outside display window");
        return (y * 9 + z) * 9 + x;
    }

    private static boolean bit(long[] bits, int page) {
        return (bits[page >>> 6] & 1L << (page & 63)) != 0;
    }

    private static Short encodedTemperature(InfraredSnapshot snapshot, BlockPos position) {
        int page = page(snapshot, position);
        int brick = (position.getX() & 15) / 4 | (position.getZ() & 15) / 4 << 2 | (position.getY() & 15) / 4 << 4;
        int block = (position.getX() & 3) | (position.getZ() & 3) << 2 | (position.getY() & 3) << 4;
        InfraredBrickCodec.Decoder decoder = new InfraredBrickCodec.Decoder();
        short[] values = new short[64];
        Short result = null;
        for (byte[] part : snapshot.brickRecords()) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(part));
            try {
                int address;
                while ((address = decoder.readRecord(buffer, values)) >= 0) {
                    if (decoder.isDormantSection()) {
                        if (address == page * 64 && (decoder.dormantBrickMask() & 1L << brick) != 0) result = values[brick];
                    } else if (address == page * 64 + brick) result = values[block];
                }
            } finally {
                buffer.release();
            }
        }
        return result;
    }

    private static void temperature(GameTestHelper helper, InfraredSnapshot snapshot, BlockPos position, double expected) {
        Short actual = encodedTemperature(snapshot, position);
        helper.assertTrue(actual != null && actual != InfraredBrickCodec.INVALID_TEMPERATURE
                        && Math.abs(actual * .25 - expected) <= .126,
                "encoded temperature at " + position + ": expected=" + expected + ", quantized=" + actual);
    }

    private static void invalid(GameTestHelper helper, InfraredSnapshot snapshot, BlockPos position) {
        Short actual = encodedTemperature(snapshot, position);
        helper.assertTrue(actual != null && actual == InfraredBrickCodec.INVALID_TEMPERATURE,
                "old display must explicitly restore INVALID at " + position + ", got=" + actual);
    }

    private static void noRuntime(GameTestHelper helper, ServerLevel level) {
        try {
            Field field = MinecraftThermalInput.class.getDeclaredField("ACTIVE");
            field.setAccessible(true);
            helper.assertTrue(!((Map<?, ?>) field.get(null)).containsKey(level),
                    "field display must not create a physical runtime or its Pages");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void wireRoundTrip(GameTestHelper helper, InfraredSnapshot snapshot) {
        FriendlyByteBuf wire = new FriendlyByteBuf(Unpooled.buffer());
        FriendlyByteBuf copy = new FriendlyByteBuf(Unpooled.buffer());
        try {
            for (boolean full : new boolean[]{false, true}) {
                wire.clear();
                copy.clear();
                new FHRequestInfraredViewDataSyncPacket(17, full, snapshot.infraredEpoch(),
                        EMPTY_PAGES, snapshot.dormantRevision(), EMPTY_PAGES, snapshot.refreshPages()).encode(wire);
                byte[] bytes = ByteBufUtil.getBytes(wire);
                var request = new FHRequestInfraredViewDataSyncPacket(wire);
                helper.assertTrue(!wire.isReadable(), "request decoder must consume the exact packet");
                long[] refresh = read(request, "knownRefreshPages");
                helper.assertTrue(Arrays.equals(refresh, full ? EMPTY_PAGES : snapshot.refreshPages()),
                        "full requests drop the old window's refresh mask; delta requests preserve it");
                request.encode(copy);
                helper.assertTrue(Arrays.equals(bytes, ByteBufUtil.getBytes(copy)), "request wire roundtrip");
            }
            int count = Math.max(1, snapshot.brickRecords().length);
            for (int part = 0; part < count; part++) {
                wire.clear();
                copy.clear();
                new FHResponseInfraredViewDataSyncPacket(17, snapshot, part).encode(wire);
                helper.assertTrue(wire.readableBytes() < 1024 * 1024, "complete wire packet must fit Minecraft's custom payload limit");
                byte[] bytes = ByteBufUtil.getBytes(wire);
                var response = new FHResponseInfraredViewDataSyncPacket(wire);
                helper.assertTrue(!wire.isReadable(), "response decoder must consume the exact packet");
                helper.assertTrue((boolean) read(response, "firstPart") == (part == 0)
                                && (boolean) read(response, "lastPart") == (part == count - 1),
                        "only the first and last parts delimit the display transaction");
                helper.assertTrue(Arrays.equals(snapshot.refreshPages(), read(response, "refreshPages")), "response refresh mask");
                response.encode(copy);
                helper.assertTrue(Arrays.equals(bytes, ByteBufUtil.getBytes(copy)), "response wire roundtrip");
            }
        } finally {
            wire.release();
            copy.release();
        }
    }

    private static double rawAir(MinecraftThermalInput input, ServerLevel level, BlockPos position) {
        try {
            Method method = MinecraftThermalInput.class.getDeclaredMethod("sampleAir",
                    double.class, double.class, double.class, long.class, int.class, ThermalEnvironmentSample.class);
            method.setAccessible(true);
            ThermalEnvironmentSample sample = new ThermalEnvironmentSample();
            method.invoke(input, position.getX() + .5, position.getY() + .5, position.getZ() + .5,
                    level.getGameTime(), 40, sample);
            return sample.airTemperatureC();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
