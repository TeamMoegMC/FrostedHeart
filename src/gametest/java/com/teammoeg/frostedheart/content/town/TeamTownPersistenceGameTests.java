package com.teammoeg.frostedheart.content.town;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import com.teammoeg.chorda.dataholders.team.AbstractTeam;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.dataholders.persistence.PersistenceCoordinator;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.ai_town.AITownManager;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenSimScheduler;
import com.teammoeg.frostedheart.content.town.citizen.sim.TownSimData;
import com.teammoeg.frostedheart.content.town.event.ITownResidentListener;
import com.teammoeg.frostedheart.content.town.provider.TeamTownProvider;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Integration tests executed in a real Forge server and its real save directory. */
@GameTestHolder("frostedheart_persistence")
@PrefixGameTestTemplate(false)
public final class TeamTownPersistenceGameTests {
    record TestTeam(UUID id, String name) implements AbstractTeam {
        public UUID getId() { return id; }
        public UUID getOwner() { return id; }
        public String getName() { return name; }
        public Collection<ServerPlayer> getOnlineMembers() { return List.of(); }
    }
    record Fixture(TeamDataHolder holder, TeamTownData town, Resident resident, TownSimData sim) {}

    static Fixture populatedTown(GameTestHelper helper, String name) {
        var level = helper.getLevel();
        TeamDataHolder holder = CTeamDataManager.INSTANCE.get(new TestTeam(UUID.randomUUID(), name));
        holder.getData(FHSpecialDataTypes.GENERATOR_DATA).dimension = level.dimension();
        TeamTownData data = holder.getData(FHSpecialDataTypes.TOWN_DATA);
        TeamTown town = data.createTeamTown();
        town.setName(name);
        BlockPos home = helper.absolutePos(new BlockPos(1, 1, 1));
        HouseBuilding house = new HouseBuilding(home);
        house.setIsStructureValid(true);
        house.setInitialized(true);
        house.setArea(64);
        house.setVolume(256);
        house.setTemperature(20);
        house.setMaxResidents(4);
        helper.assertTrue(house.isBuildingWorkable(), "persistence fixture must use a habitable house");
        data.getBuildingStore().install(house);
        Resident resident = new Resident("Persistent", "Resident", UUID.randomUUID());
        helper.assertTrue(town.addResident(resident), "the town must recruit into its real housing roster");
        town.getActionExecutorHandler().execute(new TownResourceActions.VirtualResourceAttributeAction(
                VirtualResourceType.MAX_CAPACITY.generateAttribute(0), 1000, ResourceActionType.ADD, ResourceActionMode.ATTEMPT));
        town.getActionExecutorHandler().execute(new TownResourceActions.ItemStackAction(
                new ItemStack(Items.IRON_INGOT, 64), ResourceActionType.ADD, ResourceActionMode.ATTEMPT));
        CitizenSimScheduler.get(level).refreshRegistryNow(level);
        TownSimData sim = AITownManager.peekPlayerSim(holder.getId());
        helper.assertTrue(sim != null && sim.sim().size() == 1, "recruitment must create exactly one simulation resident");
        return new Fixture(holder, data, resident, sim);
    }

    static Path teamFile(TeamDataHolder holder) {
        return CTeamDataManager.INSTANCE.root().resolve("chorda_data").resolve(holder.getId() + ".nbt");
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void nativeSaveCapturesEconomyAndSimulationAndRemoval(GameTestHelper helper) throws Exception {
        Fixture fixture = populatedTown(helper, "paired-record");
        var server = helper.getLevel().getServer();
        server.saveAllChunks(true, false, false);
        helper.assertTrue(fixture.holder().recordState().capturedRevision() > 0, "ordinary native save must capture a team checkpoint");
        fixture.resident().setFirstName("Latest");
        server.saveAllChunks(true, true, true);
        CompoundTag root = NbtIo.readCompressed(teamFile(fixture.holder()).toFile());
        helper.assertTrue(root.getInt("storageVersion") == 1 && root.contains("townSim"), "V1 team file must own townSim");
        TeamTownData restored = TeamTownData.CODEC.parse(NbtOps.INSTANCE, root.getCompound("data").get("town")).result().orElseThrow();
        helper.assertTrue(restored.residents.get(fixture.resident().getUUID()).getFirstName().equals("Latest"), "latest economic mutation must survive the asynchronous checkpoint");
        helper.assertTrue(restored.resources.get(new ItemStack(Items.IRON_INGOT)) == 64, "real resource action inventory must survive saving");
        helper.assertTrue(root.getCompound("townSim").getCompound("sim").getInt("size") == 1, "simulation must share that record");

        FileTime sentinel = FileTime.fromMillis(1234);
        Files.setLastModifiedTime(teamFile(fixture.holder()), sentinel);
        server.saveAllChunks(true, true, true);
        helper.assertTrue(Files.getLastModifiedTime(teamFile(fixture.holder())).equals(sentinel), "a clean team must not be rewritten");

        fixture.holder().removeData(FHSpecialDataTypes.TOWN_DATA);
        helper.assertTrue(new TeamTownProvider(fixture.holder().getId()).getTown() == null, "a query must not recreate a removed town");
        helper.assertTrue(AITownManager.peekPlayerSim(fixture.holder().getId()) == null, "removal must detach player simulation");
        server.saveAllChunks(true, true, true);
        root = NbtIo.readCompressed(teamFile(fixture.holder()).toFile());
        helper.assertTrue(!root.getCompound("data").contains("town") && root.getCompound("townSim").isEmpty(), "removed towns must persist explicit absence");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void replacementTransfersConsumersAndPreservesResidentIdentity(GameTestHelper helper) throws Exception {
        Fixture fixture = populatedTown(helper, "replacement");
        CompoundTag encoded = (CompoundTag) TeamTownData.CODEC.encodeStart(NbtOps.INSTANCE, fixture.town()).result().orElseThrow();
        TeamTownData replacement = TeamTownData.CODEC.parse(NbtOps.INSTANCE, encoded).result().orElseThrow();
        replacement.createTeamTown().setName("replacement-installed");
        fixture.holder().setData(FHSpecialDataTypes.TOWN_DATA, replacement);
        long revision = fixture.holder().recordState().currentRevision();
        fixture.resident().setLastName("Detached old instance");
        helper.assertTrue(fixture.town().tickMorning(helper.getLevel(), fixture.holder()) == TownSettlement.Result.SKIPPED,
                "a retired town instance must not settle or publish effects for its replacement");
        helper.assertTrue(fixture.holder().recordState().currentRevision() == revision, "old resident edits must not dirty the current record");
        helper.assertTrue(replacement.getTownDay() == fixture.town().getTownDay(), "replacement must not settle an extra day");
        helper.assertTrue(fixture.sim().sim().size() == 1 && replacement.residents.get(fixture.resident().getUUID()).getSimId() > 0,
                "replacement must rebind the same resident identity without duplication");
        helper.getLevel().getServer().saveAllChunks(true, true, true);
        CompoundTag root = NbtIo.readCompressed(teamFile(fixture.holder()).toFile());
        helper.assertTrue(root.getCompound("data").getCompound("town").getString("name").equals("replacement-installed"), "replacement must reach the actual file");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void manualCommitSurvivesNotificationFailureAndReentry(GameTestHelper helper) {
        TeamDataHolder holder = CTeamDataManager.INSTANCE.get(new TestTeam(UUID.randomUUID(), "commit-boundary"));
        holder.getData(FHSpecialDataTypes.GENERATOR_DATA).dimension = helper.getLevel().dimension();
        TeamTownData data = holder.getData(FHSpecialDataTypes.TOWN_DATA);
        AutoSettlementState original = data.getAutoSettlement();
        var nestedResult = new java.util.concurrent.atomic.AtomicReference<TownSettlement.Result>();
        data.setResidentListener(new ITownResidentListener() {
            public void onResidentAdded(Resident resident) {}
            public void onResidentRemoved(Resident resident) {}
            public void onTownMorningDone(TeamTownData town) {
                nestedResult.set(town.tickMorning(helper.getLevel(), holder));
                throw new IllegalStateException("injected post-commit notification failure");
            }
        });
        helper.assertTrue(data.tickMorning(helper.getLevel(), holder) == TownSettlement.Result.COMMITTED, "notification failure must retain a committed economic day");
        helper.assertTrue(nestedResult.get() == TownSettlement.Result.IN_PROGRESS, "a notification cannot reenter economics");
        helper.assertTrue(data.getTownDay() == 1 && data.getHistory().size() == 1, "history and town age must commit together");
        helper.assertTrue(original.equals(data.getAutoSettlement()), "manual settlement must not consume an automatic window");
        data.setResidentListener(null);
        helper.assertTrue(data.tickMorning(helper.getLevel(), holder) == TownSettlement.Result.COMMITTED && data.getTownDay() == 2, "a separate manual call must advance a second day");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void teamTransferPersistsReplacementFactsBeforeRetirement(GameTestHelper helper) throws Exception {
        var manager = CTeamDataManager.INSTANCE;
        TestTeam first = new TestTeam(UUID.randomUUID(), "transfer-from");
        TestTeam target = new TestTeam(UUID.randomUUID(), "transfer-to");
        TeamDataHolder kept = manager.get(first);
        TeamDataHolder replaced = manager.get(target);
        replaced.getData(FHSpecialDataTypes.GENERATOR_DATA).dimension = helper.getLevel().dimension();
        kept.getData(FHSpecialDataTypes.TOWN_DATA).createTeamTown().setName("kept-economy");
        replaced.getData(FHSpecialDataTypes.TOWN_DATA).createTeamTown().setName("retired-economy");
        helper.getLevel().getServer().saveAllChunks(true, true, true);
        manager.transfer(first.getId(), target);
        manager.transfer(target.getId(), target);
        helper.assertTrue(manager.findByExternalTeamId(target.getId()) == kept && manager.findByExternalTeamId(first.getId()) == null,
                "transfer must preserve the internal holder and remove its stale external mapping");
        helper.assertTrue(manager.findByHolderId(replaced.getId()) == null, "replaced holder must leave the active registry");
        helper.assertTrue(TeamTownData.executionLevel(replaced) == null
                        && replaced.getData(FHSpecialDataTypes.TOWN_DATA).tickMorning(helper.getLevel(), replaced)
                                == TownSettlement.Result.SKIPPED,
                "a retired holder must not regain an execution world or advance economics");
        helper.getLevel().getServer().saveAllChunks(true, true, true);
        CompoundTag keeper = NbtIo.readCompressed(teamFile(kept).toFile());
        CompoundTag retirement = NbtIo.readCompressed(teamFile(replaced).toFile());
        helper.assertTrue(keeper.getList("supersededHolderIds", 11).size() == 1 && retirement.getBoolean("deleted"),
                "successful keeper and retirement files must close the replacement protocol");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void writeFailureDoesNotCancelVanillaSaveAndNextSaveRecovers(GameTestHelper helper) throws Exception {
        var manager = CTeamDataManager.INSTANCE;
        Path obstruction = manager.root().resolve("persistence-test-obstruction");
        Files.writeString(obstruction, "intentional file where the record directory would be");
        var state = manager.persistence().registerRecord(
                new PersistenceCoordinator.RecordKey(PersistenceCoordinator.Kind.AI_TOWN, UUID.randomUUID()),
                obstruction.resolve("record.nbt"), new PersistenceCoordinator.RecordSource() {
                    public void prepareCheckpoint() {}
                    public CompoundTag capture() { CompoundTag tag = new CompoundTag(); tag.putBoolean("fixture", true); return tag; }
                });
        SavedData vanilla = new SavedData() {
            public CompoundTag save(CompoundTag tag) { tag.putBoolean("saved", true); return tag; }
        };
        helper.getLevel().getDataStorage().set("fh_persistence_vanilla_probe", vanilla);
        vanilla.setDirty();
        boolean result = helper.getLevel().getServer().saveAllChunks(true, true, true);
        helper.assertTrue(!result && state.isDirty(), "native save must report the failed mod record");
        helper.assertTrue(Files.exists(manager.root().resolve("data/fh_persistence_vanilla_probe.dat")), "vanilla SavedData must still be written");
        Files.delete(obstruction);
        helper.getLevel().getServer().saveAllChunks(true, true, true);
        helper.assertTrue(!state.isDirty() && Files.exists(obstruction.resolve("record.nbt")), "the next real save must recover without restarting");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aiProfilesAndSimulationUseTheirOwnRealRecord(GameTestHelper helper) throws Exception {
        var ai = AITownManager.getOrCreate("persistent-ai-" + UUID.randomUUID(), helper.getLevel().dimension());
        Resident resident = new Resident("AI", "Before", UUID.randomUUID());
        resident.setHousePos(helper.absolutePos(new BlockPos(1, 1, 1)));
        ai.addResident(resident);
        CitizenSimScheduler.get(helper.getLevel()).refreshRegistryNow(helper.getLevel());
        helper.getLevel().getServer().saveAllChunks(true, true, true);
        resident.setLastName("After");
        helper.getLevel().getServer().saveAllChunks(true, true, true);
        Path path = CTeamDataManager.INSTANCE.root().resolve("data/fh_ai_towns").resolve(ai.getId() + ".nbt");
        CompoundTag root = NbtIo.readCompressed(path.toFile());
        var restored = com.teammoeg.frostedheart.content.town.ai_town.AITownData.fromNbt(root.getCompound("data"));
        helper.assertTrue(restored.getResident(resident.getUUID()).orElseThrow().getLastName().equals("After"), "nested AI profile edits must invalidate the actual record");
        helper.assertTrue(restored.getSimData().sim().size() == 1, "the AI record must restore its simulation");
        helper.assertTrue(CTeamDataManager.INSTANCE.findByHolderId(ai.getId()) == null, "AI storage must not create a fake player holder");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void realPlayerAutomaticWindowManualAdvanceAndOfflineSkip(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "town-clock-test"));
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        connection.setProtocol(net.minecraft.network.ConnectionProtocol.PLAY);
        Runnable cleanup = () -> {
            if (server.getPlayerList().getPlayers().contains(player)) server.getPlayerList().remove(player);
            channel.finishAndReleaseAll();
        };
        try {
            server.getPlayerList().placeNewPlayer(connection, player);
            TeamDataHolder holder = CTeamDataManager.get(player);
            holder.getData(FHSpecialDataTypes.GENERATOR_DATA).dimension = helper.getLevel().dimension();
            TeamTownData town = holder.getData(FHSpecialDataTypes.TOWN_DATA);
            var world = server.overworld();
            var climate = com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate.getCapability(world).orElseThrow(IllegalStateException::new);
            long logical = climate.getSec() * 20L + com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClockSource.elapsedDayTimeTicks(climate.getClockDayTime(), world.getDayTime());
            long next = (com.teammoeg.frostedheart.content.town.model.TownScheduleModel.window(logical, holder.getId()) + 1L) * 24000L
                    + com.teammoeg.frostedheart.content.town.model.TownScheduleModel.phase(holder.getId());
            world.setDayTime(world.getDayTime() + next - logical + 1L);
            helper.runAfterDelay(3, () -> {
                try {
                    helper.assertTrue(town.getTownDay() == 1, "crossing the real automatic window must settle exactly once");
                    AutoSettlementState automatic = town.getAutoSettlement();
                    helper.assertTrue(town.tickMorning(helper.getLevel(), holder) == TownSettlement.Result.COMMITTED && town.getTownDay() == 2,
                            "a separate manual request must still advance the town");
                    helper.assertTrue(automatic.equals(town.getAutoSettlement()), "manual advance must not alter automatic progress");
                    server.getPlayerList().remove(player);
                    world.setDayTime(world.getDayTime() + 24000L);
                    helper.runAfterDelay(3, () -> {
                        try {
                            helper.assertTrue(town.getTownDay() == 2 && automatic.equals(town.getAutoSettlement()), "an offline window must skip economic work without changing persistent progress");
                            helper.succeed();
                        } finally { cleanup.run(); }
                    });
                } catch (RuntimeException failure) { cleanup.run(); throw failure; }
            });
        } catch (RuntimeException failure) { cleanup.run(); throw failure; }
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void nativeSaveCommandsRespectSaveOffAndFlush(GameTestHelper helper) throws Exception {
        var manager = CTeamDataManager.INSTANCE;
        var server = helper.getLevel().getServer();
        var holder = manager.get(new TestTeam(UUID.randomUUID(), "save-command-test"));
        var town = holder.getData(FHSpecialDataTypes.TOWN_DATA).createTeamTown();
        town.setName("before-save-off");
        server.saveAllChunks(true, true, true);
        long captured = holder.recordState().capturedRevision();
        var commands = server.getCommands();
        var source = server.createCommandSourceStack();
        try {
            commands.performPrefixedCommand(source, "save-off");
            town.setName("after-save-on");
            server.saveAllChunks(true, false, false);
            helper.assertTrue(holder.recordState().capturedRevision() == captured && holder.recordState().isDirty(),
                    "save-off must keep the changed team pending without capturing it");
            commands.performPrefixedCommand(source, "save-on");
            int result = commands.performPrefixedCommand(source, "save-all flush");
            helper.assertTrue(result > 0 && !holder.recordState().isDirty(), "save-all flush must wait for the actual record result");
            helper.assertTrue(NbtIo.readCompressed(teamFile(holder).toFile()).getCompound("data").getCompound("town")
                    .getString("name").equals("after-save-on"), "the real command must update the real file before returning");
            helper.succeed();
        } finally {
            for (var level : server.getAllLevels()) level.noSave = false;
        }
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void generatorRemovalPausesSimulationAndReinstallRestoresIt(GameTestHelper helper) throws Exception {
        Fixture fixture = populatedTown(helper, "generator-rebind");
        var generator = fixture.holder().removeData(FHSpecialDataTypes.GENERATOR_DATA).orElseThrow();
        helper.assertTrue(fixture.holder().getOptional(FHSpecialDataTypes.TOWN_DATA).orElseThrow() == fixture.town(),
                "removing the generator must retain economic facts");
        helper.assertTrue(AITownManager.peekPlayerSim(fixture.holder().getId()) == fixture.sim() && !fixture.sim().isAdopted(),
                "generator removal must pause runtime simulation and retain its stored arrays");
        helper.getLevel().getServer().saveAllChunks(true, true, true);
        helper.assertTrue(NbtIo.readCompressed(teamFile(fixture.holder()).toFile()).getCompound("townSim")
                .getCompound("sim").getInt("size") == 1, "a paused simulation must still be saved");
        fixture.holder().setData(FHSpecialDataTypes.GENERATOR_DATA, generator);
        helper.assertTrue(fixture.sim().isAdopted() && fixture.sim().sim().size() == 1,
                "reinstall must reconnect the retained simulation without duplicating residents");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void failedComponentBindingRestoresThePreviousTownAndSimulation(GameTestHelper helper) {
        Fixture fixture = populatedTown(helper, "rollback-original");
        var rejected = TeamTownData.CODEC.parse(NbtOps.INSTANCE,
                TeamTownData.CODEC.encodeStart(NbtOps.INSTANCE, fixture.town()).result().orElseThrow()).result().orElseThrow();
        rejected.createTeamTown().setName("reject-this-binding");
        var restored = new java.util.concurrent.atomic.AtomicBoolean();
        fixture.town().createTeamTown().registerWarehouseTopologyListener(
                net.minecraft.core.GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(BlockPos.ZERO)),
                new com.teammoeg.frostedheart.content.town.transport.WarehouseTopologyListener() {
                    public void onWarehouseTopologyChanged(com.teammoeg.frostedheart.content.town.transport.WarehouseTopologySnapshot snapshot) {}
                    public void onTownRebound(TeamTown town) {
                        if (town != null && town.getName().equals("reject-this-binding")) throw new IllegalStateException("injected device binding failure");
                        restored.set(town != null && town.getName().equals("rollback-original"));
                    }
                });
        RuntimeException failure = null;
        try { fixture.holder().setData(FHSpecialDataTypes.TOWN_DATA, rejected); }
        catch (RuntimeException expected) { failure = expected; }
        helper.assertTrue(failure != null, "a binding failure must not report replacement success");
        helper.assertTrue(fixture.holder().getOptional(FHSpecialDataTypes.TOWN_DATA).orElseThrow() == fixture.town() && restored.get(),
                "the original component pointer and consumers must be restored");
        helper.assertTrue(fixture.sim().isAdopted() && fixture.sim().sim().size() == 1 && fixture.resident().getSimId() > 0,
                "rollback must recover the original simulation and resident binding");
        helper.succeed();
    }
}
