/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.SinglePlayerTeam;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.multiblock.CMultiblockHelper;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHMultiblocks;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorData;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorState;
import com.teammoeg.frostedheart.content.climate.network.InfraredBrickCodec;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.InfraredSnapshot;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Actual long river geometry, a fuel-consuming tower, a short fire and an untouched control. */
@GameTestHolder("frostedheart_river_thermal")
@PrefixGameTestTemplate(false)
public final class RiverThermalProductionGameTests {
    private static final int LENGTH = 160;
    private static final int WIDTH = 21;
    private static final int DURATION = Integer.parseInt(
            System.getenv().getOrDefault("FH_RIVER_TEST_TICKS", "3600"));
    private static final int[] DISTANCES = {4, 16, 32, 64, 96, 128, 152};

    @GameTest(template = "phase0a_empty", batch = "river_thermal", timeoutTicks = 24000)
    public static void aFuelledTowerMustNotStartASelfPropagatingRiverHeatFront(GameTestHelper helper) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        boolean daylight = level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        int randomTicks = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, level.getServer());
        var rivers = List.of(new River("tower", new BlockPos(4096, 199, 4096)),
                new River("pulse", new BlockPos(4096, 199, 4352)),
                new River("untouched", new BlockPos(4096, 199, 4608)));
        var build = new RiverBuild(helper, rivers);
        GeneratorData[] generator = {null};
        BlockPos[] master = {null};
        int[] initialFuel = {0};
        long[] started = {0};
        double[] maximumFarRise = {0};
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(build.advance(), "Build the actual loaded river strips"))
                .thenExecute(() -> {
                    level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(3, level.getServer());
                    var towerRiver = rivers.get(0);
                    master[0] = ThermalLoadedWorldGameTests.form(helper, FHMultiblocks.GENERATOR_T1,
                            towerRiver.origin.offset(0, 1, 2));
                    ThermalLoadedWorldGameTests.prepareProfileCache(level.getServer());
                    UUID id = UUID.randomUUID();
                    level.getServer().getProfileCache().add(new GameProfile(id, "River" + id.toString().substring(0, 8)));
                    TeamDataHolder team = CTeamDataManager.INSTANCE.get(new SinglePlayerTeam(id));
                    var tower = (GeneratorState) CMultiblockHelper.getBEHelper(level, master[0]).getState();
                    tower.setOwner(team.getId());
                    tower.regist(level, master[0], (short) FHMultiblocks.GENERATOR_T1.getMasterFromOriginOffset().getY());
                    var data = team.getData(FHSpecialDataTypes.GENERATOR_DATA);
                    data.heated = data.ranged = 100;
                    data.TLevel = data.RLevel = 1;
                    data.isWorking = true;
                    data.inventory.setStackInSlot(0, new ItemStack(Items.COAL, 32));
                    generator[0] = data;
                    initialFuel[0] = data.inventory.getStackInSlot(0).getCount();
                    ThermalConvergenceScenes.fire(helper, rivers.get(1).origin.offset(1, 1, 3));
                    for (var river : rivers) helper.assertTrue(level.getBiome(river.water(128)).is(Biomes.FROZEN_RIVER),
                            "The actual long river uses minecraft:frozen_river");
                    started[0] = level.getGameTime();
                    ContinuousAirGameTestPacing.beginMeasurement();
                    FHMain.LOGGER.info("RIVER_THERMAL_START,length={},width={},durationTicks={},naturalC={},towerMaster={}",
                            LENGTH, WIDTH, DURATION, WorldTemperature.naturalAir(level, towerRiver.water(4)), master[0]);
                }).thenExecute(() -> {
                    for (int delay = 100; delay <= DURATION; delay += 100) {
                        int elapsed = delay;
                        helper.runAfterDelay(delay, () -> {
                            if (elapsed == 200) ContinuousAirProductionGameTests.useItem(helper,
                                    rivers.get(1).origin.offset(1, 1, 3), Items.IRON_SHOVEL);
                            observe(helper, rivers, elapsed, maximumFarRise);
                        });
                    }
                }).thenIdle(DURATION + 10).thenExecute(() -> {
                    var data = generator[0];
                    helper.assertTrue(data.isActive && data.inventory.getStackInSlot(0).getCount() < initialFuel[0],
                            "The real tower must consume actual coal throughout the observation");
                    var near = new ThermalEnvironmentSample();
                    BlockPos inside = rivers.get(0).origin.offset(5, 2, 5);
                    MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, inside,
                            WorldTemperature.naturalAir(level, inside), near);
                    helper.assertTrue(near.airTemperatureC() >= WorldTemperature.naturalAir(level, inside) + 9.5,
                            "Normal local tower heating must still hold");
                    for (var river : rivers) {
                        var player = FakePlayerFactory.getMinecraft(level);
                        BlockPos far = river.water(128);
                        player.setPos(far.getX() + .5, far.getY() + 2, far.getZ() + .5);
                        var snapshot = MinecraftThermalInput.gameplayInfraredSnapshot(player,
                                true, 0, 0, 0, new long[12], false, new long[0], 0, 0);
                        helper.assertTrue(snapshot != null, "The actual infrared capture must finish a consistent cut");
                        FHMain.LOGGER.info("RIVER_THERMAL_INFRARED,scene={},distance=128,waterQ={},bankQ={},sampleTick={}",
                                river.name, encoded(snapshot, far), encoded(snapshot, river.bank(128)), level.getGameTime());
                    }
                    FHMain.LOGGER.info("RIVER_THERMAL_RESULT,elapsedTicks={},maximumFarRiseC={},fuelRemaining={},sources={},rootBytes={},worker={}",
                            level.getGameTime() - started[0], maximumFarRise[0], data.inventory.getStackInSlot(0).getCount(),
                            MinecraftThermalInput.enabledPhysicalSourceCount(level), MinecraftThermalInput.thermalBudgetUsedBytes(),
                            MinecraftThermalInput.describeWorkerProgress(level));
                    data.isWorking = false;
                    level.destroyBlock(master[0], false);
                    MinecraftThermalInput.closeActiveLevel(level);
                    for (var river : rivers) force(level, river.origin, false);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(daylight, level.getServer());
                    level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(randomTicks, level.getServer());
                    helper.assertTrue(maximumFarRise[0] < 1,
                            "A tower/fire must not wake cold natural river materials more than 64m away into a self-sustaining hot front: rise=" + maximumFarRise[0]);
                }).thenSucceed();
    }

    private static void observe(GameTestHelper helper, List<River> rivers, int elapsed, double[] maximumFarRise) {
        var level = helper.getLevel();
        var material = new MaterialSample();
        var air = new ThermalEnvironmentSample();
        for (var river : rivers) {
            for (int distance : DISTANCES) {
                for (int which = 0; which < 2; which++) {
                    var point = which == 0 ? river.water(distance) : river.bank(distance);
                    boolean readable = MinecraftThermalInput.sampleMaterial(level, point, material);
                    double natural = natural(level, point);
                    double temperature = readable ? material.temperatureC() : Double.NaN;
                    if (distance >= 96 && !river.name.equals("untouched") && readable)
                        maximumFarRise[0] = Math.max(maximumFarRise[0], temperature - natural);
                    MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, point.above(),
                            WorldTemperature.naturalAir(level, point.above()), air);
                    if (elapsed % 400 == 0 || readable && distance >= 96)
                        FHMain.LOGGER.info("RIVER_THERMAL_SAMPLE,scene={},elapsed={},distance={},material={},state={},source={},naturalC={},temperatureC={},enthalpyJ={},branch={},sampleTick={},airC={},airTick={},capturedMask={}",
                                river.name, elapsed, distance, which == 0 ? "water" : "bank", level.getBlockState(point),
                                readable ? material.source() : MaterialSample.Source.UNAVAILABLE, natural, temperature,
                                readable ? material.enthalpyJ() : Double.NaN, readable ? material.branch() : 0,
                                readable ? material.sampleTick() : -1, air.airTemperatureC(), air.airSampleTick(),
                                Long.toUnsignedString(MinecraftThermalInput.inputProgress(level, SectionPos.asLong(point)).capturedMask()));
                }
            }
        }
    }

    private static double natural(ServerLevel level, BlockPos point) {
        return WorldTemperature.naturalAir(level, new BlockPos((point.getX() & ~15) + 8,
                (point.getY() & ~15) + 8, (point.getZ() & ~15) + 8));
    }

    private record River(String name, BlockPos origin) {
        BlockPos water(int distance) { return origin.offset(distance, 0, 10); }
        BlockPos bank(int distance) { return origin.offset(distance, 1, 7); }
    }

    private static void force(ServerLevel level, BlockPos origin, boolean forced) {
        for (int x = (origin.getX() - 16) >> 4; x <= (origin.getX() + LENGTH + 16) >> 4; x++)
            for (int z = (origin.getZ() - 16) >> 4; z <= (origin.getZ() + WIDTH + 16) >> 4; z++)
                level.setChunkForced(x, z, forced);
    }

    private static final class RiverBuild {
        private final GameTestHelper helper;
        private final List<River> rivers;
        private int riverIndex, index;
        private RiverBuild(GameTestHelper helper, List<River> rivers) {
            this.helper = helper;
            this.rivers = rivers;
            for (var river : rivers) force(helper.getLevel(), river.origin, true);
            var level = helper.getLevel();
            var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.FROZEN_RIVER);
            for (var river : rivers)
                for (int x = (river.origin.getX() - 16) >> 4; x <= (river.origin.getX() + LENGTH + 16) >> 4; x++)
                    for (int z = (river.origin.getZ() - 16) >> 4; z <= (river.origin.getZ() + WIDTH + 16) >> 4; z++) {
                        var chunk = level.getChunk(x, z);
                        chunk.fillBiomesFromNoise((qx, qy, qz, sampler) -> biome, level.getChunkSource().randomState().sampler());
                        chunk.setUnsaved(true);
                    }
        }
        boolean advance() {
            var level = helper.getLevel();
            int side = WIDTH + 8, length = LENGTH + 17, height = 24;
            int count = side * length * height;
            int remaining = 4096;
            while (riverIndex < rivers.size() && remaining-- > 0) {
                var origin = rivers.get(riverIndex).origin;
                int x = index % length - 8, z = index / length % side - 4, y = index / (length * side) - 3;
                boolean channel = z >= 8 && z <= 12;
                var state = y == -3 ? Blocks.STONE.defaultBlockState()
                        : z >= 0 && z < WIDTH && y <= (channel ? 0 : 1)
                            ? (channel ? Blocks.WATER : Blocks.DIRT).defaultBlockState()
                            : Blocks.AIR.defaultBlockState();
                level.setBlock(origin.offset(x, y, z), state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                if (++index == count) { index = 0; riverIndex++; }
            }
            return riverIndex == rivers.size();
        }
    }

    private static short encoded(InfraredSnapshot snapshot, BlockPos point) {
        int x = (point.getX() >> 4) - snapshot.centerChunkX() + 4;
        int y = (point.getY() >> 4) - snapshot.centerSectionY() + 4;
        int z = (point.getZ() >> 4) - snapshot.centerChunkZ() + 4;
        int page = x + 9 * (z + 9 * y);
        int brick = (point.getX() & 15) >>> 2 | (point.getZ() & 15) >>> 2 << 2 | (point.getY() & 15) >>> 2 << 4;
        var decoder = new InfraredBrickCodec.Decoder();
        var values = new short[64];
        for (byte[] part : snapshot.brickRecords()) {
            var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(part));
            try {
                int address;
                while ((address = decoder.readRecord(buffer, values)) >= 0)
                    if (address == page * 64 + brick)
                        return values[(point.getX() & 3) | (point.getZ() & 3) << 2 | (point.getY() & 3) << 4];
            } finally { buffer.release(); }
        }
        return InfraredBrickCodec.INVALID_TEMPERATURE;
    }
}
