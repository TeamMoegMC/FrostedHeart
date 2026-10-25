package com.teammoeg.frostedheart.content.town;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.ai_town.AITownManager;
import com.teammoeg.frostedheart.content.town.citizen.sim.TownSimData;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.building.TownBuildingTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** seed -> migrate -> read, each in a separate server process and the same test world. */
@GameTestHolder("frostedheart_persistence_restart")
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = FHMain.MODID)
public final class TeamTownPersistenceRestartGameTests {
    private static Path legacyTeamPath;
    private static Path legacyMetadataPath;
    private static CompoundTag legacyTeam;
    private static CompoundTag legacyMetadata;
    private static Path legacyAIPath;

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void restartRestoresTheActualPriorProcessCheckpoint(GameTestHelper helper) throws Exception {
        String phase = System.getProperty("frostedheart.persistenceTestPhase", "read");
        var manager = CTeamDataManager.INSTANCE;
        Path manifestPath = manager.root().resolve("data/fh_team_town_test_manifest.nbt");
        if (phase.equals("seed")) {
            var fixture = TeamTownPersistenceGameTests.populatedTown(helper, "restart-economy");
            fixture = installPhysicalBuildingCheckpoint(helper, fixture);
            var ai = AITownManager.getOrCreate("restart-ai", helper.getLevel().dimension());
            var aiResident = new com.teammoeg.frostedheart.content.town.resident.Resident("Restart", "AI", UUID.randomUUID());
            aiResident.setHousePos(helper.absolutePos(new net.minecraft.core.BlockPos(3, 1, 1)));
            ai.addResident(aiResident);
            com.teammoeg.frostedheart.content.town.citizen.sim.CitizenSimScheduler.get(helper.getLevel()).refreshRegistryNow(helper.getLevel());
            fixture.town().townDay = 48;
            fixture.town().persistentChanged();
            helper.getLevel().getServer().saveAllChunks(true, true, true);
            CompoundTag manifest = new CompoundTag();
            manifest.putUUID("holder", fixture.holder().getId());
            manifest.putUUID("resident", fixture.resident().getUUID());
            manifest.putString("firstName", fixture.resident().getFirstName());
            manifest.putLong("buildingOrigin", new BlockPos(400, 70, 400).asLong());
            manifest.putUUID("ai", ai.getId());
            manifest.putUUID("aiResident", aiResident.getUUID());
            NbtIo.writeCompressed(manifest, manifestPath.toFile());

            // Capture real gameplay data now; only the file envelope is converted
            // into the predecessor's documented layout after normal stop completes.
            legacyTeamPath = TeamTownPersistenceGameTests.teamFile(fixture.holder());
            legacyTeam = NbtIo.readCompressed(legacyTeamPath.toFile());
            CompoundTag simulation = legacyTeam.getCompound("townSim").copy();
            legacyTeam.remove("townSim");
            legacyTeam.remove("storageVersion");
            legacyTeam.getCompound("data").getCompound("town").remove("autoSettlement");
            CompoundTag legacyTown = legacyTeam.getCompound("data").getCompound("town");
            legacyTown.remove("buildingDimension");
            for (Tag raw : legacyTown.getList("blocks", Tag.TAG_COMPOUND)) {
                CompoundTag record = (CompoundTag) raw;
                record.remove("version");
                CompoundTag building = record.getCompound("building");
                int legacyType = switch (building.getString("type")) {
                    case "house" -> 0;
                    case "huntingBase" -> 1;
                    case "mine" -> 2;
                    case "mineBase" -> 3;
                    case "warehouse" -> 4;
                    case "transportStation" -> 5;
                    default -> throw new IllegalStateException("Unexpected fixture building type");
                };
                building.putInt("type", legacyType);
            }
            legacyTeam.putString("futureField", "preserved");
            CompoundTag unknownComponent = new CompoundTag(); unknownComponent.putString("value", "opaque");
            legacyTeam.getCompound("data").put("future_component", unknownComponent);
            legacyMetadataPath = manager.root().resolve("data/fh_ai_towns.dat");
            legacyMetadata = new CompoundTag();
            CompoundTag oldData = new CompoundTag();
            CompoundTag sims = new CompoundTag(); sims.put(fixture.holder().getId().toString(), simulation);
            oldData.put("playerSims", sims);
            CompoundTag towns = new CompoundTag();
            CompoundTag aiPayload = ai.save(); aiPayload.putString("futureAiField", "preserved-ai");
            towns.put(ai.getId().toString(), aiPayload);
            oldData.put("towns", towns);
            oldData.putUUID("lastAIId", ai.getId());
            legacyMetadata.put("data", oldData);
            legacyAIPath = manager.root().resolve("data/fh_ai_towns").resolve(ai.getId() + ".nbt");
            helper.succeed();
            return;
        }
        helper.assertTrue(Files.exists(manifestPath), "read phase must use the previous process's world, never recreate expected data");
        CompoundTag manifest = NbtIo.readCompressed(manifestPath.toFile());
        UUID holderId = manifest.getUUID("holder");
        var holder = manager.findByHolderId(holderId);
        helper.assertTrue(holder != null, "startup must restore the saved holder");
        Path aiPath = manager.root().resolve("data/fh_ai_towns").resolve(manifest.getUUID("ai") + ".nbt");
        if (phase.equals("migrate")) {
            helper.assertTrue(NbtIo.readCompressed(TeamTownPersistenceGameTests.teamFile(holder).toFile()).getInt("storageVersion") == 0
                    && !Files.exists(aiPath), "migration must begin from the predecessor's actual file layout");
        }
        TeamTownData town = holder.getOptional(FHSpecialDataTypes.TOWN_DATA).orElseThrow();
        var resident = town.residents.get(manifest.getUUID("resident"));
        helper.assertTrue(resident != null && resident.getFirstName().equals(manifest.getString("firstName")), "economic resident identity must survive the actual restart");
        helper.assertTrue(town.getTownDay() == 48, "loading must not perform economic settlement");
        helper.assertTrue(town.resources.get(new ItemStack(Items.IRON_INGOT)) == 64, "inventory must survive migration and restart");
        BlockPos origin = BlockPos.of(manifest.getLong("buildingOrigin"));
        helper.assertTrue(town.buildings.size() == 6 && town.getBuildingStore().retainedCount() == 2,
                "six healthy building types and two retained records must survive both real restarts");
        String[] types = {"house", "huntingBase", "mine", "mineBase", "warehouse", "transportStation"};
        for (int index = 0; index < types.length; index++) {
            BlockPos pos = origin.offset(index * 2, 0, 0);
            helper.getLevel().getChunkAt(pos);
            var core = (com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity<?>)
                    helper.getLevel().getBlockEntity(pos);
            helper.assertTrue(core != null && core.getBuilding().orElseThrow() == town.buildings.get(pos),
                    "the actual prior-process core must bind the restored business object");
            helper.assertTrue(TownBuildingTypes.of(town.buildings.get(pos)).id().equals(types[index]),
                    "each frozen legacy integer must restore its correct building type");
        }
        helper.assertTrue(((HuntingBaseBuilding) town.buildings.get(origin.east(2))).getLootRollCarry() == 0.625,
                "production carry must survive independent-process migration and read");
        helper.assertTrue(origin.equals(resident.getHousePos())
                        && ((HouseBuilding) town.buildings.get(origin)).getResidentsID().contains(resident.getUUID()),
                "the actual housing roster and resident mirror must reconnect across processes");
        TownSimData sim = AITownManager.peekPlayerSim(holderId);
        helper.assertTrue(sim != null && sim.sim().size() == 1 && resident.getSimId() > 0, "startup must reconnect the saved simulation by resident UUID");
        var ai = AITownManager.lastAI();
        helper.assertTrue(ai != null && ai.getId().equals(manifest.getUUID("ai"))
                && ai.getResident(manifest.getUUID("aiResident")).isPresent() && ai.getSimData().sim().size() == 1,
                "startup must restore the real legacy AI profile, simulation and last-AI identity");
        helper.getLevel().getServer().saveAllChunks(true, true, true);
        CompoundTag record = NbtIo.readCompressed(TeamTownPersistenceGameTests.teamFile(holder).toFile());
        helper.assertTrue(record.getInt("storageVersion") == 1 && record.getCompound("townSim").getCompound("sim").getInt("size") == 1,
                "migration must produce a complete V1 owner record");
        helper.assertTrue(record.getString("futureField").equals("preserved") && record.getCompound("data").contains("future_component"),
                "unrecognized payloads must survive real save/load");
        CompoundTag savedTown = record.getCompound("data").getCompound("town");
        helper.assertTrue(savedTown.getList("retainedBuildingData", Tag.TAG_COMPOUND).size() == 2,
                "pending and duplicate archives must not grow on the second process restart");
        CompoundTag savedHouse = savedTown.getList("blocks", Tag.TAG_COMPOUND).getCompound(0);
        helper.assertTrue(savedHouse.getString("futureRecordField").equals("preserved-building-record")
                        && savedHouse.getCompound("building").getString("futureBuildingField").equals("preserved-building-value"),
                "unknown building fields must survive migration and another real save");
        CompoundTag metadata = NbtIo.readCompressed(manager.root().resolve("data/fh_ai_towns.dat").toFile());
        helper.assertTrue(!metadata.getCompound("data").getCompound("playerSims").contains(holderId.toString()),
                "legacy source cleanup must follow confirmed V1 persistence");
        helper.assertTrue(!metadata.getCompound("data").getCompound("towns").contains(ai.getId().toString())
                && NbtIo.readCompressed(aiPath.toFile()).getCompound("data").getString("futureAiField").equals("preserved-ai"),
                "confirmed AI migration must retain unknown payload fields and clean only its old source");
        helper.succeed();
    }

    private static TeamTownPersistenceGameTests.Fixture installPhysicalBuildingCheckpoint(GameTestHelper helper,
            TeamTownPersistenceGameTests.Fixture fixture) {
        BlockPos oldHome = fixture.resident().getHousePos();
        fixture.town().createTeamTown().removeTownBlock(helper.getLevel(), oldHome);
        BlockPos origin = new BlockPos(400, 70, 400);
        var cores = TownBuildingGameTests.placeSixCores(helper, fixture.holder(), origin);
        HouseBuilding house = (HouseBuilding) cores.get(0).getBuilding().orElseThrow();
        house.setInitialized(true);
        house.setIsStructureValid(true);
        house.setArea(64);
        house.setVolume(256);
        house.setTemperature(20);
        house.setMaxResidents(4);
        house.addResident(fixture.resident());
        ((HuntingBaseBuilding) cores.get(1).getBuilding().orElseThrow()).setLootRollCarry(0.625);
        TownBuildingGameTests.placeCore(helper, fixture.holder(), origin.south(4), FHBlocks.HOUSE.get());
        CompoundTag raw = TownBuildingGameTests.encodedTown(fixture.town());
        ListTag records = raw.getList("blocks", Tag.TAG_COMPOUND);
        for (Tag entry : records) {
            CompoundTag record = (CompoundTag) entry;
            BlockPos pos = com.teammoeg.frostedheart.content.town.building.BuildingRecordsCodec.decodePosition(record.get("pos"));
            if (origin.south(4).equals(pos)) {
                record.putInt("version", 99);
                record.getCompound("building").putString("type", "future-restart-house");
            } else if (origin.equals(pos)) {
                record.putString("futureRecordField", "preserved-building-record");
                record.getCompound("building").putString("futureBuildingField", "preserved-building-value");
            }
        }
        records.add(records.getCompound(0).copy());
        TeamTownData restored = TownBuildingGameTests.replaceTown(fixture.holder(), raw);
        return new TeamTownPersistenceGameTests.Fixture(fixture.holder(), restored,
                restored.residents.get(fixture.resident().getUUID()), fixture.sim());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void installLegacyFixtureAfterNormalStop(ServerStoppedEvent event) {
        if (legacyTeam == null) return;
        try {
            NbtIo.writeCompressed(legacyTeam, legacyTeamPath.toFile());
            NbtIo.writeCompressed(legacyMetadata, legacyMetadataPath.toFile());
            Files.deleteIfExists(legacyAIPath);
        } catch (Exception failure) { throw new IllegalStateException("Unable to install the legacy restart fixture", failure); }
        finally { legacyTeam = null; legacyMetadata = null; legacyTeamPath = null; legacyMetadataPath = null; legacyAIPath = null; }
    }
}
