package com.teammoeg.frostedheart.content.town;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.block.OccupiedZoneInfo;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.RoomPathfinder.OccupiedCell;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.BuildingRecordsCodec;
import com.teammoeg.frostedheart.content.town.building.TownBuildingTypes;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBlockEntity;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBlockEntity;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBlockEntity;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseBuilding;
import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenSimScheduler;
import com.teammoeg.frostedheart.content.town.provider.TeamTownProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Real cores, server lifecycle, native save files and deliberate recovery inputs. */
@GameTestHolder("frostedheart_buildings")
@PrefixGameTestTemplate(false)
public final class TownBuildingGameTests {
    static TeamDataHolder newTeam(GameTestHelper helper, String name) {
        TeamDataHolder holder = CTeamDataManager.INSTANCE.get(
                new TeamTownPersistenceGameTests.TestTeam(UUID.randomUUID(), name));
        holder.getData(FHSpecialDataTypes.GENERATOR_DATA).dimension = helper.getLevel().dimension();
        holder.getData(FHSpecialDataTypes.TOWN_DATA).createTeamTown().setName(name);
        return holder;
    }

    static AbstractTownBuildingBlockEntity<?> placeCore(GameTestHelper helper, TeamDataHolder holder,
            BlockPos position, Block block) {
        helper.getLevel().setBlockAndUpdate(position, block.defaultBlockState());
        var core = (AbstractTownBuildingBlockEntity<?>) helper.getLevel().getBlockEntity(position);
        helper.assertTrue(core != null, "the registered core must have a real block entity");
        core.townProvider = new TeamTownProvider(holder.getId());
        core.setChanged();
        holder.getData(FHSpecialDataTypes.TOWN_DATA).createTeamTown()
                .onCorePlaced(helper.getLevel(), position, core);
        return core;
    }

    static List<AbstractTownBuildingBlockEntity<?>> placeSixCores(GameTestHelper helper,
            TeamDataHolder holder, BlockPos origin) {
        List<Block> blocks = List.of(FHBlocks.HOUSE.get(), FHBlocks.HUNTING_BASE.get(),
                FHBlocks.MINE.get(), FHBlocks.MINE_BASE.get(), FHBlocks.WAREHOUSE.get(),
                FHBlocks.TRANSPORT_STATION.get());
        var result = new java.util.ArrayList<AbstractTownBuildingBlockEntity<?>>();
        for (int index = 0; index < blocks.size(); index++) {
            BlockPos position = origin.offset(index * 2, 0, 0);
            helper.getLevel().getChunkAt(position);
            result.add(placeCore(helper, holder, position, blocks.get(index)));
        }
        return List.copyOf(result);
    }

    static CompoundTag encodedTown(TeamTownData data) {
        return ((CompoundTag) TeamTownData.CODEC.encodeStart(NbtOps.INSTANCE, data)
                .getOrThrow(false, message -> {})).copy();
    }

    static TeamTownData replaceTown(TeamDataHolder holder, CompoundTag raw) {
        TeamTownData restored = TeamTownData.CODEC.parse(NbtOps.INSTANCE, raw)
                .getOrThrow(false, message -> {});
        holder.setData(FHSpecialDataTypes.TOWN_DATA, restored);
        return restored;
    }

    static CompoundTag saveTown(GameTestHelper helper, TeamDataHolder holder) throws Exception {
        helper.assertTrue(helper.getLevel().getServer().saveAllChunks(true, true, true),
                "native forced save must report the actual write result");
        return NbtIo.readCompressed(TeamTownPersistenceGameTests.teamFile(holder).toFile())
                .getCompound("data").getCompound("town");
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void sixCoresSaveAndBindTheSameEconomicObjects(GameTestHelper helper) throws Exception {
        TeamDataHolder holder = newTeam(helper, "six-building-cores");
        var cores = placeSixCores(helper, holder, helper.absolutePos(new BlockPos(2, 2, 2)));
        TeamTownData data = holder.getData(FHSpecialDataTypes.TOWN_DATA);
        HuntingBaseBuilding hunting = (HuntingBaseBuilding) cores.get(1).getBuilding().orElseThrow();
        hunting.setLootRollCarry(0.375);
        CompoundTag saved = saveTown(helper, holder);
        helper.assertTrue(saved.getList("blocks", Tag.TAG_COMPOUND).size() == 6,
                "all six physical core types must reach the native team file");
        TeamTownData restored = replaceTown(holder, saved);
        for (var core : cores) {
            AbstractTownBuilding expected = restored.buildings.get(core.getBlockPos());
            core.onUnloaded();
            core.onLoad();
            restored.createTeamTown().ensureCoreBound(helper.getLevel(), core.getBlockPos(), core);
            helper.assertTrue(core.getBuilding().orElseThrow() == expected,
                    "chunk rebind must keep the restored economic instance");
        }
        helper.assertTrue(((HuntingBaseBuilding) cores.get(1).getBuilding().orElseThrow()).getLootRollCarry() == 0.375,
                "binding must preserve production carry");
        helper.assertTrue(data.getBuildingStore().resolvedMap().size() == 6, "replacement must not mutate the old store");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void mixedFiftyRecordsRetainFailuresAndKeepHealthyBuildings(GameTestHelper helper) throws Exception {
        TeamDataHolder holder = newTeam(helper, "mixed-fifty-records");
        for (int index = 0; index < 50; index++) {
            placeCore(helper, holder, helper.absolutePos(new BlockPos(1 + index % 10, 2, 1 + index / 10)),
                    FHBlocks.HOUSE.get());
        }
        CompoundTag raw = encodedTown(holder.getData(FHSpecialDataTypes.TOWN_DATA));
        ListTag entries = raw.getList("blocks", Tag.TAG_COMPOUND);
        entries.getCompound(1).getCompound("building").putString("type", "future-house");
        entries.getCompound(2).putInt("version", 99);
        entries.getCompound(3).getCompound("building").putString("residentsUUID", "broken roster");
        entries.getCompound(4).getCompound("building").putString("area", "broken snapshot");
        entries.getCompound(5).getCompound("building").putString("dailyReport", "broken report");
        entries.getCompound(6).putString("building", "broken payload with a valid outer position");
        entries.add(entries.getCompound(0).copy());
        CompoundTag conflictingPending = new CompoundTag();
        conflictingPending.putString("mode", "pending");
        conflictingPending.putString("kind", "entry");
        conflictingPending.putString("reason", "CONFLICTING_RECOVERY_RECORD");
        conflictingPending.put("pos", entries.getCompound(0).get("pos").copy());
        conflictingPending.put("raw", entries.getCompound(0).copy());
        ListTag retained = new ListTag();
        retained.add(conflictingPending);
        raw.put("retainedBuildingData", retained);
        TeamTownData restored = replaceTown(holder, raw);
        helper.assertTrue(restored.buildings.size() == 46, "bad records must not clear the other 46 buildings");
        helper.assertTrue(restored.getBuildingStore().retainedCount() == 8,
                "four pending records, two repaired fields and two conflicts must retain their original values");
        BlockPos brokenPayloadPos = BuildingRecordsCodec
                .decodePosition(entries.getCompound(6).get("pos"));
        helper.assertTrue(restored.getBuildingStore().isPending(brokenPayloadPos),
                "a malformed payload must keep its valid outer position and preserve references");
        helper.assertTrue(restored.getHousingPlan().entries().size() == 50,
                "all four unavailable positions must retain their original housing plan entries");
        CompoundTag saved = saveTown(helper, holder);
        helper.assertTrue(saved.getList("retainedBuildingData", Tag.TAG_COMPOUND).stream()
                        .map(tag -> (CompoundTag) tag)
                        .anyMatch(tag -> tag.getString("reason").equals("CONFLICTING_RECOVERY_RECORD")
                                && tag.getString("mode").equals("archive")),
                "normalizing a conflicting pending record must reach the real file as archive");
        TeamTownData reopened = replaceTown(holder, saved);
        helper.assertTrue(reopened.buildings.size() == 46 && reopened.getBuildingStore().retainedCount() == 8,
                "native save and reinstallation must neither drop records nor grow the archive");
        helper.assertTrue(reopened.createTeamTown().getName().equals("mixed-fifty-records"), "other town fields must survive");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void badReportStaysUsableAndBadContainerPreservesHousingIntent(GameTestHelper helper) throws Exception {
        var fixture = TeamTownPersistenceGameTests.populatedTown(helper, "recovery-intent");
        BlockPos home = fixture.resident().getHousePos();
        fixture.town().setHousingGuarantee(home, 2);
        helper.getLevel().setBlockAndUpdate(home, FHBlocks.HOUSE.get().defaultBlockState());
        var core = (HouseBlockEntity) helper.getLevel().getBlockEntity(home);
        core.townProvider = new TeamTownProvider(fixture.holder().getId());
        core.setChanged();
        CompoundTag raw = encodedTown(fixture.town());
        raw.getList("blocks", Tag.TAG_COMPOUND).getCompound(0).getCompound("building")
                .putString("dailyReport", "unreadable display data");
        TeamTownData repaired = replaceTown(fixture.holder(), raw);
        helper.assertTrue(repaired.buildings.get(home).isBuildingWorkable(),
                "repairing a display report must preserve a habitable house");
        CompoundTag brokenContainer = encodedTown(repaired);
        brokenContainer.putString("blocks", "unreadable container");
        TeamTownData pending = replaceTown(fixture.holder(), brokenContainer);
        var resident = pending.residents.get(fixture.resident().getUUID());
        helper.assertTrue(resident.getHousePos() == null && resident.getPendingAssignments().housePos().orElseThrow().equals(home),
                "unresolved housing must move to recovery intent rather than provide a fake home");
        helper.assertTrue(pending.getHousingPlan().entries().stream().anyMatch(entry -> entry.building().equals(home)),
                "an unreadable collection must preserve the original housing queue");
        helper.assertTrue(fixture.sim().sim().size() == 0, "pending coordinates must not produce a simulation anchor");
        pending.createTeamTown().ensureCoreBound(helper.getLevel(), home, core);
        helper.assertTrue(pending.getHousingPlan().guaranteedResidents(home) == 2
                        && resident.getPendingAssignments().housePos().orElseThrow().equals(home),
                "repairing a loaded core must keep the old plan and recovery intent");
        helper.assertTrue(pending.getBuildingStore().retainedCount() == 2,
                "missing-core repair must preserve both collection and field evidence");
        helper.assertTrue(pending.tickMorning(helper.getLevel(), fixture.holder()) == TownSettlement.Result.COMMITTED,
                "a town with unavailable housing must still complete a real daily settlement");
        helper.assertTrue(resident.getHousePos() == null && fixture.sim().sim().size() == 0,
                "daily settlement must keep unavailable homes out of actual assignments and simulation");
        CompoundTag saved = saveTown(helper, fixture.holder());
        helper.assertTrue(saved.getList("retainedBuildingData", Tag.TAG_COMPOUND).size() == 2,
                "both report evidence and the raw collection must persist once");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void recoveryDoesNotStealResidentsFromAnAvailableReplacement(GameTestHelper helper) {
        var fixture = TeamTownPersistenceGameTests.populatedTown(helper, "assignment-recovery");
        BlockPos oldHome = fixture.resident().getHousePos();
        CompoundTag raw = encodedTown(fixture.town());
        CompoundTag original = raw.getList("blocks", Tag.TAG_COMPOUND).getCompound(0).copy();
        raw.getList("blocks", Tag.TAG_COMPOUND).getCompound(0).getCompound("building").putString("type", "future-house");
        TeamTownData pending = replaceTown(fixture.holder(), raw);
        BlockPos alternatePos = oldHome.offset(5, 0, 0);
        var alternateCore = (HouseBlockEntity) placeCore(helper, fixture.holder(), alternatePos, FHBlocks.HOUSE.get());
        HouseBuilding alternate = alternateCore.getBuilding().orElseThrow();
        alternate.setInitialized(true);
        alternate.setIsStructureValid(true);
        alternate.setArea(64);
        alternate.setVolume(256);
        alternate.setTemperature(20);
        alternate.setMaxResidents(4);
        helper.assertTrue(alternate.isBuildingWorkable(), "replacement fixture must provide a habitable real house");
        TownAssignments.allocateHouse(pending);
        var resident = pending.residents.get(fixture.resident().getUUID());
        helper.assertTrue(alternatePos.equals(resident.getHousePos()), "available housing must remain allocatable");
        CompoundTag recoverable = encodedTown(pending);
        ListTag retained = recoverable.getList("retainedBuildingData", Tag.TAG_COMPOUND);
        retained.getCompound(0).put("raw", original);
        TeamTownData recovered = replaceTown(fixture.holder(), recoverable);
        helper.assertTrue(alternatePos.equals(recovered.residents.get(resident.getUUID()).getHousePos()),
                "restoring an old roster must not take a resident from a valid current assignment");
        helper.assertTrue(((HouseBuilding) recovered.buildings.get(oldHome)).getResidentsID().isEmpty(),
                "the recovered roster must not retain a duplicate resident");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void unchangedSaveAndOneBuildingMutationUseTheEncodingCache(GameTestHelper helper) throws Exception {
        TeamDataHolder holder = newTeam(helper, "building-encoding-cache");
        var cores = placeSixCores(helper, holder, helper.absolutePos(new BlockPos(2, 2, 2)));
        TeamTownData data = holder.getData(FHSpecialDataTypes.TOWN_DATA);
        data.prepareView(helper.getLevel(), holder);
        saveTown(helper, holder);
        long before = data.getBuildingStore().codecCalls();
        long revision = holder.recordState().currentRevision();
        WarehouseBuilding warehouse = (WarehouseBuilding) cores.get(4).getBuilding().orElseThrow();
        warehouse.setCapacity(warehouse.getCapacity());
        TownAssignments.reconcile(data);
        helper.assertTrue(holder.recordState().currentRevision() == revision, "unchanged setters and rosters must not dirty the team");
        saveTown(helper, holder);
        helper.assertTrue(data.getBuildingStore().codecCalls() == before, "unchanged native saves must reuse building encodings");
        HouseBuilding house = (HouseBuilding) cores.get(0).getBuilding().orElseThrow();
        long topology = data.getWarehouseTopologyBuildCount();
        house.setTemperature(house.getTemperature() + 1);
        data.prepareView(helper.getLevel(), holder);
        saveTown(helper, holder);
        helper.assertTrue(data.getBuildingStore().codecCalls() == before + 1,
                "editing one house must encode just that house");
        helper.assertTrue(data.getWarehouseTopologyBuildCount() == topology,
                "a house temperature change must not rebuild warehouse topology");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void actualRemovalClearsPendingAndDuplicateRecordsPermanently(GameTestHelper helper) throws Exception {
        TeamDataHolder holder = newTeam(helper, "retained-building-removal");
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        placeCore(helper, holder, pos, FHBlocks.HOUSE.get());
        CompoundTag raw = encodedTown(holder.getData(FHSpecialDataTypes.TOWN_DATA));
        ListTag records = raw.getList("blocks", Tag.TAG_COMPOUND);
        records.getCompound(0).getCompound("building").putString("type", "unknown-house");
        records.add(records.getCompound(0).copy());
        TeamTownData pending = replaceTown(holder, raw);
        helper.assertTrue(pending.getBuildingStore().retainedCount() == 2, "fixture must contain pending and archival records");
        helper.getLevel().destroyBlock(pos, false);
        helper.assertTrue(pending.getBuildingStore().retainedCount() == 0,
                "actual block destruction must clear all locatable records at that core");
        TeamTownData reopened = replaceTown(holder, saveTown(helper, holder));
        reopened.getBuildingStore().retryPending();
        helper.assertTrue(reopened.buildings.isEmpty() && reopened.getBuildingStore().retainedCount() == 0,
                "deleted pending and duplicate records must not resurrect");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void unknownTopLevelFieldsSurviveAndRemovedEntranceDoesNotReturn(GameTestHelper helper) throws Exception {
        TeamDataHolder holder = newTeam(helper, "building-owned-fields");
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        placeCore(helper, holder, pos, FHBlocks.HOUSE.get());
        CompoundTag raw = encodedTown(holder.getData(FHSpecialDataTypes.TOWN_DATA));
        CompoundTag record = raw.getList("blocks", Tag.TAG_COMPOUND).getCompound(0);
        record.putString("futureRecordField", "record-evidence");
        CompoundTag body = record.getCompound("building");
        body.putString("futureBuildingField", "building-evidence");
        body.putLong("entrancePosition", pos.east().asLong());
        TeamTownData restored = replaceTown(holder, raw);
        HouseBuilding house = (HouseBuilding) restored.buildings.get(pos);
        helper.assertTrue(house.hasEntrance(), "fixture must restore a scanned entrance");
        ((HouseBlockEntity) helper.getLevel().getBlockEntity(pos)).refresh_safe(house);
        helper.assertTrue(!house.hasEntrance(), "an invalid real room must clear its previous layout");
        CompoundTag saved = saveTown(helper, holder).getList("blocks", Tag.TAG_COMPOUND).getCompound(0);
        helper.assertTrue(saved.getString("futureRecordField").equals("record-evidence")
                        && saved.getCompound("building").getString("futureBuildingField").equals("building-evidence"),
                "unknown top-level fields must survive a real building update");
        helper.assertTrue(!saved.getCompound("building").contains("entrancePosition"),
                "an omitted known optional field must not be merged back from old raw data");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void managementSnapshotExportsSummariesWithoutRawRecoveryData(GameTestHelper helper) {
        TeamDataHolder holder = newTeam(helper, "building-management-view");
        placeCore(helper, holder, helper.absolutePos(new BlockPos(2, 2, 2)), FHBlocks.HOUSE.get());
        CompoundTag raw = encodedTown(holder.getData(FHSpecialDataTypes.TOWN_DATA));
        raw.getList("blocks", Tag.TAG_COMPOUND).getCompound(0).getCompound("building").putString("type", "future-house");
        TeamTownData pending = replaceTown(holder, raw);
        CompoundTag management = (CompoundTag) TeamTownData.MANAGEMENT_CODEC.encodeStart(NbtOps.INSTANCE, pending)
                .getOrThrow(false, message -> {});
        helper.assertTrue(!management.contains("retainedBuildingData")
                        && management.getList("buildingLoadIssues", Tag.TAG_COMPOUND).size() == 1,
                "the client must receive a recovery summary and no opaque disk payload");
        TeamTownData client = TeamTownData.MANAGEMENT_CODEC.parse(NbtOps.INSTANCE, management)
                .getOrThrow(false, message -> {});
        helper.assertTrue(client.getBuildingStore().issues().size() == 1 && client.buildings.isEmpty(),
                "the management candidate must decode successfully with unavailable buildings");
        BlockPos deltaPos = helper.absolutePos(new BlockPos(4, 2, 2));
        client.applyBuildingUpdate(java.util.Map.of(deltaPos, new HouseBuilding(deltaPos)), Set.of());
        client.applyBuildingUpdate(java.util.Map.of(), Set.of(deltaPos));
        helper.assertTrue(!client.dataSyncCache.hasChangedZone() && !client.dataSyncCache.hasChanges(),
                "client deltas must not accumulate server scan or save work");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void coreReplacementWaitsForTheCurrentTownOperation(GameTestHelper helper) {
        TeamDataHolder holder = newTeam(helper, "deferred-core-replacement");
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var oldCore = placeCore(helper, holder, pos, FHBlocks.HOUSE.get());
        TeamTownData data = holder.getData(FHSpecialDataTypes.TOWN_DATA);
        AbstractTownBuilding oldBuilding = data.buildings.get(pos);
        data.inSettlement = true;
        helper.getLevel().destroyBlock(pos, false);
        var replacement = placeCore(helper, holder, pos, FHBlocks.WAREHOUSE.get());
        helper.assertTrue(data.buildings.get(pos) == oldBuilding, "replacement must wait while production owns the collection");
        data.inSettlement = false;
        TownRuntimeManager.current().finishOperations();
        helper.assertTrue(data.buildings.get(pos) instanceof WarehouseBuilding,
                "the operation boundary must install the final actual core");
        data.createTeamTown().onCoreRemoved(helper.getLevel(), pos, oldCore);
        helper.assertTrue(replacement.getBuilding().orElseThrow() == data.buildings.get(pos),
                "late removal of an old block entity must not delete its replacement");
        AbstractTownBuilding current = data.buildings.get(pos);
        data.createTeamTown().onCorePlaced(helper.getLevel(), pos, replacement);
        helper.assertTrue(data.buildings.get(pos) == current, "duplicate placement callbacks must be idempotent");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void dimensionMismatchSurvivesPeriodicRegistryAndCoreQueries(GameTestHelper helper) {
        var fixture = TeamTownPersistenceGameTests.populatedTown(helper, "building-dimension-mismatch");
        var generator = fixture.holder().getData(FHSpecialDataTypes.GENERATOR_DATA);
        long townDay = fixture.town().getTownDay();
        int buildings = fixture.town().buildings.size();
        var wrongLevel = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(wrongLevel != null, "the real server must provide the other dimension");
        helper.assertTrue(fixture.town().tickMorning(wrongLevel, fixture.holder()) == TownSettlement.Result.SKIPPED
                        && fixture.town().getTownDay() == townDay && fixture.town().buildings.size() == buildings,
                "a direct settlement in the wrong world must preserve the day and all building facts");
        generator.actualPos = helper.absolutePos(new BlockPos(2, 2, 2));
        int wrongWorldChunks = wrongLevel.getChunkSource().getLoadedChunksCount();
        fixture.town().debugSpawnRefugeeBatch(wrongLevel, fixture.holder());
        helper.assertTrue(wrongLevel.getChunkSource().getLoadedChunksCount() == wrongWorldChunks,
                "the debug entry must reject another world before querying terrain or climate");
        generator.actualPos = null;
        generator.dimension = Level.NETHER;
        fixture.holder().setData(FHSpecialDataTypes.GENERATOR_DATA, generator);
        helper.runAfterDelay(25, () -> {
            CitizenSimScheduler.get(helper.getLevel()).refreshRegistryNow(helper.getLevel());
            helper.assertTrue(TeamTownData.executionLevel(fixture.holder()) == null && !fixture.sim().isAdopted(),
                    "periodic registry refresh must not reactivate a town in its tower's wrong dimension");
            helper.assertTrue(!fixture.town().buildings.isEmpty(), "paused execution must retain building facts");
            helper.assertTrue(fixture.town().tickMorning(helper.getLevel(), fixture.holder()) == TownSettlement.Result.SKIPPED
                            && fixture.town().getTownDay() == townDay,
                    "a mismatched tower must also pause direct manual settlement");
            generator.dimension = helper.getLevel().dimension();
            fixture.holder().setData(FHSpecialDataTypes.GENERATOR_DATA, generator);
            TownRuntimeManager.current().install(fixture.holder());
            helper.assertTrue(fixture.sim().isAdopted(), "matching tower context must reconnect the original simulation");
            helper.succeed();
        });
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void realRoomScanTracksDoorBedAndStableRepeat(GameTestHelper helper) throws Exception {
        TeamDataHolder holder = newTeam(helper, "real-room-scan");
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        for (int x = 0; x <= 6; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = 0; z <= 6; z++) {
                    boolean shell = x == 0 || x == 6 || y == 0 || y == 4 || z == 0 || z == 6;
                    helper.getLevel().setBlockAndUpdate(origin.offset(x, y, z),
                            (shell ? Blocks.STONE : Blocks.AIR).defaultBlockState());
                }
            }
        }
        BlockPos door = origin.offset(3, 1, 0);
        helper.getLevel().setBlockAndUpdate(door,
                Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.getLevel().setBlockAndUpdate(door.above(),
                Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        BlockPos bed = origin.offset(2, 1, 3);
        helper.getLevel().setBlockAndUpdate(bed,
                Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.FOOT));
        helper.getLevel().setBlockAndUpdate(bed.south(),
                Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.HEAD));
        var core = (HouseBlockEntity) placeCore(helper, holder, origin.offset(3, 0, 3), FHBlocks.HOUSE.get());
        HouseBuilding house = core.getBuilding().orElseThrow();
        core.refresh_safe(house);
        holder.getData(FHSpecialDataTypes.TOWN_DATA).prepareView(helper.getLevel(), holder);
        helper.assertTrue(house.isBuildingWorkable() && house.getBedCount() == 1 && house.hasEntrance(),
                "a real enclosed room must yield a habitable house with bed and entrance layout");
        saveTown(helper, holder);
        long encodings = holder.getData(FHSpecialDataTypes.TOWN_DATA).getBuildingStore().codecCalls();
        core.refresh_safe(house);
        saveTown(helper, holder);
        helper.assertTrue(holder.getData(FHSpecialDataTypes.TOWN_DATA).getBuildingStore().codecCalls() == encodings,
                "repeating an unchanged real structure scan must reuse the persisted encoding");
        helper.getLevel().destroyBlock(bed.south(), false);
        core.refresh_safe(house);
        helper.assertTrue(house.getBedCount() == 0, "removing the real bed must update the layout");
        helper.getLevel().setBlockAndUpdate(door, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(door.above(), Blocks.STONE.defaultBlockState());
        var huntingCore = (HuntingBaseBlockEntity) placeCore(helper, holder,
                core.getBlockPos(), FHBlocks.HUNTING_BASE.get());
        HuntingBaseBuilding hunting = huntingCore.getBuilding().orElseThrow();
        hunting.setLootRollCarry(0.625);
        huntingCore.refresh_safe(hunting);
        helper.assertTrue(hunting.isInitialized() && !hunting.isStructureValid()
                        && hunting.getArea() == 0 && hunting.getVolume() == 0 && hunting.getRating() == 0.0,
                "a completed invalid-room scan must keep a finite zero rating without prior geometry");
        TeamTownData restored = replaceTown(holder, saveTown(helper, holder));
        helper.assertTrue(restored.getBuildingStore().retainedCount() == 0
                        && ((HuntingBaseBuilding) huntingCore.getBuilding().orElseThrow()).getLootRollCarry() == 0.625,
                "native save and reload must preserve carry without creating a corrupt rating archive");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void unavailableTerrainAndWorldHeightDoNotLoadExtraChunks(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos far = new BlockPos(300000, level.getMinBuildHeight() + 2, 300000);
        int chunks = level.getChunkSource().getLoadedChunksCount();
        helper.assertTrue(level.getChunkSource().getChunkNow(far.getX() >> 4, far.getZ() >> 4) == null,
                "nonloading fixture must start outside loaded terrain");
        helper.assertTrue(BlockScanner.scanRoomDataFromBlock(level, far) == null,
                "a missing chunk must produce an unavailable candidate");
        var geometry = new OccupiedZoneInfo(Set.of(new OccupiedCell(far, 2)));
        helper.assertTrue(Float.isNaN(BlockScanner.sampleTemperature(level, geometry)),
                "unavailable temperature must be rejected before changing stored inputs");
        BlockPos top = helper.absolutePos(new BlockPos(2, 2, 2)).atY(level.getMaxBuildHeight() - 1);
        BlockScanner.scanRoomDataFromBlock(level, top);
        helper.assertTrue(level.getChunkSource().getLoadedChunksCount() == chunks,
                "structure and temperature queries must not request additional chunks");
        var nether = level.getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "the real server must provide the Nether fixture dimension");
        BlockPos samplePos = new BlockPos(8, 64, 8);
        nether.getChunkAt(samplePos);
        float expected = com.teammoeg.frostedheart.content.climate.WorldTemperature.block(nether, samplePos);
        var oneSample = new OccupiedZoneInfo(Set.of(new OccupiedCell(samplePos, 1, 1)));
        float sampled = BlockScanner.sampleTemperature(nether, oneSample);
        helper.assertTrue(Float.isFinite(sampled) && Math.abs(sampled - expected) < 0.0001F,
                "nonloading reads must preserve the actual dimension's temperature baseline");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void legacyMetadataRepairsReachTheNativeFile(GameTestHelper helper) throws Exception {
        TeamDataHolder holder = newTeam(helper, "building-metadata-migration");
        placeCore(helper, holder, helper.absolutePos(new BlockPos(2, 2, 2)), FHBlocks.HOUSE.get());
        CompoundTag legacy = encodedTown(holder.getData(FHSpecialDataTypes.TOWN_DATA));
        legacy.remove("buildingDimension");
        CompoundTag entry = legacy.getList("blocks", Tag.TAG_COMPOUND).getCompound(0);
        entry.remove("version");
        entry.getCompound("building").putInt("type", 0);
        TeamTownData restored = replaceTown(holder, legacy);
        CompoundTag saved = saveTown(helper, holder);
        helper.assertTrue(saved.getString("buildingDimension").equals(helper.getLevel().dimension().location().toString()),
                "legacy dimension confirmation must be persisted without a business mutation");
        CompoundTag migrated = saved.getList("blocks", Tag.TAG_COMPOUND).getCompound(0);
        helper.assertTrue(migrated.getInt("version") == 1 && migrated.getCompound("building").getString("type").equals("house"),
                "frozen legacy integer identities must migrate into named versioned records");
        helper.assertTrue(restored.buildings.size() == 1 && !holder.recordState().isDirty(),
                "the completed migration must leave the actual owner record saved");
        helper.succeed();
    }

    private static final class UnencodableBuilding extends AbstractTownBuilding {
        UnencodableBuilding(BlockPos pos) {
            super(pos);
        }
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void encodingFailurePreservesTheLastCompleteNativeCheckpoint(GameTestHelper helper) throws Exception {
        TeamDataHolder holder = newTeam(helper, "building-encoding-retry");
        placeCore(helper, holder, helper.absolutePos(new BlockPos(2, 2, 2)), FHBlocks.HOUSE.get());
        TeamTownData data = holder.getData(FHSpecialDataTypes.TOWN_DATA);
        CompoundTag complete = saveTown(helper, holder);
        BlockPos brokenPos = helper.absolutePos(new BlockPos(4, 2, 2));
        data.getBuildingStore().install(new UnencodableBuilding(brokenPos));
        helper.assertTrue(!helper.getLevel().getServer().saveAllChunks(true, true, true),
                "a failed building encoding must report an incomplete mod checkpoint");
        CompoundTag disk = NbtIo.readCompressed(TeamTownPersistenceGameTests.teamFile(holder).toFile())
                .getCompound("data").getCompound("town");
        helper.assertTrue(disk.equals(complete) && holder.recordState().isDirty(),
                "encoding failure must retain the previous complete file and the pending owner revision");
        data.getBuildingStore().removeAt(brokenPos);
        saveTown(helper, holder);
        helper.assertTrue(!holder.recordState().isDirty(), "the next native save must recover after the bad object is removed");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void loadedCoreWorkloadsReportCaptureAndSaveCosts(GameTestHelper helper) throws Exception {
        for (int buildingCount : new int[]{10, 100, 500}) {
            TeamDataHolder holder = newTeam(helper, "building-workload-" + buildingCount);
            TeamTownData data = holder.getData(FHSpecialDataTypes.TOWN_DATA);
            BlockPos origin = new BlockPos(1600 + buildingCount * 2, 65, 1600);
            for (int index = 0; index < buildingCount; index++) {
                BlockPos pos = origin.offset(index % 20, 0, index / 20);
                helper.getLevel().getChunkAt(pos);
                placeCore(helper, holder, pos, FHBlocks.HOUSE.get());
            }
            long start = System.nanoTime();
            CompoundTag cold = encodedTown(data);
            long coldNanos = System.nanoTime() - start;
            long encodings = data.getBuildingStore().codecCalls();
            start = System.nanoTime();
            CompoundTag stable = encodedTown(data);
            long cachedNanos = System.nanoTime() - start;
            helper.assertTrue(cold.equals(stable) && data.getBuildingStore().codecCalls() == encodings,
                    "cached captures must return complete identical building data");
            HouseBuilding first = (HouseBuilding) data.buildings.values().iterator().next();
            first.setTemperature(first.getTemperature() + 1);
            start = System.nanoTime();
            encodedTown(data);
            long changedNanos = System.nanoTime() - start;
            helper.assertTrue(data.getBuildingStore().codecCalls() == encodings + 1,
                    "each physical workload must reencode only its edited building");
            start = System.nanoTime();
            saveTown(helper, holder);
            long saveNanos = System.nanoTime() - start;
            long fileBytes = java.nio.file.Files.size(TeamTownPersistenceGameTests.teamFile(holder));
            FHMain.LOGGER.info("TOWN_BUILDING_WORKLOAD buildings={} coldCaptureMs={} cachedCaptureMs={} oneChangedCaptureMs={} forcedSaveMs={} compressedFileBytes={}",
                    buildingCount, coldNanos / 1_000_000.0, cachedNanos / 1_000_000.0,
                    changedNanos / 1_000_000.0, saveNanos / 1_000_000.0, fileBytes);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "townReload", timeoutTicks = 400)
    public static void resourceReloadRetainsPendingDataAndProductionCarry(GameTestHelper helper) {
        TeamDataHolder holder = newTeam(helper, "building-resource-reload");
        BlockPos pendingPos = helper.absolutePos(new BlockPos(2, 2, 2));
        var pendingCore = placeCore(helper, holder, pendingPos, FHBlocks.HOUSE.get());
        var huntingCore = placeCore(helper, holder, pendingPos.east(2), FHBlocks.HUNTING_BASE.get());
        var mineCore = (MineBlockEntity) placeCore(helper, holder, pendingPos.east(4), FHBlocks.MINE.get());
        mineCore.executeTask();
        var biome = mineCore.getBuilding().orElseThrow().getBiomePath();
        var weights = MineBuilding.getWeights(biome);
        ((HuntingBaseBuilding) huntingCore.getBuilding().orElseThrow()).setLootRollCarry(0.625);
        CompoundTag raw = encodedTown(holder.getData(FHSpecialDataTypes.TOWN_DATA));
        raw.getList("blocks", Tag.TAG_COMPOUND).getCompound(0).getCompound("building")
                .putString("type", "future-reload-house");
        TeamTownData restored = replaceTown(holder, raw);
        ListTag retainedBefore = encodedTown(restored).getList("retainedBuildingData", Tag.TAG_COMPOUND);
        long generation = TownBuildingTypes.resourceGeneration();
        var server = helper.getLevel().getServer();
        var reload = server.reloadResources(server.getPackRepository().getSelectedIds());
        helper.succeedWhen(() -> {
            helper.assertTrue(reload.isDone(), "wait for the real server resource reload");
            reload.join();
            helper.assertTrue(TownBuildingTypes.resourceGeneration() > generation,
                    "resource reload must invalidate the town resource generation");
            helper.assertTrue(MineBuilding.getWeights(biome).equals(weights),
                    "a real reload must rebuild the complete recipe cache for the existing mine");
            pendingCore.executeTask();
            helper.assertTrue(pendingCore.getBuilding().isEmpty() && restored.getBuildingStore().isPending(pendingPos),
                    "reload and periodic binding must keep the original unavailable record");
            helper.assertTrue(encodedTown(restored).getList("retainedBuildingData", Tag.TAG_COMPOUND).equals(retainedBefore),
                    "reload retries must neither replace nor grow the retained raw records");
            helper.assertTrue(((HuntingBaseBuilding) huntingCore.getBuilding().orElseThrow()).getLootRollCarry() == 0.625,
                    "a resource reload must preserve the existing production carry");
        });
    }
}
