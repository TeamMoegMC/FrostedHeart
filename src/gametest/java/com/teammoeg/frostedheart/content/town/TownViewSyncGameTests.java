package com.teammoeg.frostedheart.content.town;

import com.mojang.authlib.GameProfile;
import com.teammoeg.chorda.dataholders.team.*;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.io.codec.DataOps;
import com.teammoeg.chorda.io.codec.ObjectWriter;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.building.TownBuildingStore;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.logistics.TransportStationBuilding;
import com.teammoeg.frostedheart.content.town.buildings.institute.ResearchInstituteBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBlockEntity;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseMenu;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseBlockEntity;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseMenu;
import com.teammoeg.frostedheart.content.town.client.TownClientUpdates;
import com.teammoeg.frostedheart.content.town.event.ITownDataUpdateListener;
import com.teammoeg.frostedheart.content.town.event.TownCommonEvents;
import com.teammoeg.frostedheart.content.town.network.*;
import com.teammoeg.frostedheart.content.town.provider.TeamTownProvider;
import com.teammoeg.frostedheart.content.town.transport.TownTransportSnapshot;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkHooks;

/** Actual server players, menu hooks, stock actions and Forge payloads. */
@GameTestHolder("frostedheart_town_view")
@PrefixGameTestTemplate(false)
public final class TownViewSyncGameTests {
    private TownViewSyncGameTests() {}

    @GameTest(template = "phase0a_empty", batch = "town_view_wire", timeoutTicks = 40)
    public static void compressedBuildingDefaultsSurviveActualWireEncoding(GameTestHelper helper) {
        List<AbstractTownBuilding> buildings = List.of(new HouseBuilding(BlockPos.ZERO),
                new HuntingBaseBuilding(new BlockPos(1, 2, 3)), new MineBuilding(new BlockPos(2, 3, 4)),
                new MineBaseBuilding(new BlockPos(3, 4, 5)), new WarehouseBuilding(new BlockPos(4, 5, 6)),
                new TransportStationBuilding(new BlockPos(5, 6, 7)), new ResearchInstituteBuilding(new BlockPos(6, 7, 8)));
        for (AbstractTownBuilding building : buildings) {
            building.setInitialized(true);
            building.setIsStructureValid(true);
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                CodecUtil.writeCodec(buffer, ITownBuilding.CODEC, building);
                ITownBuilding restored = CodecUtil.readCodec(buffer, ITownBuilding.CODEC);
                helper.assertTrue(restored.getClass() == building.getClass()
                                && ((AbstractTownBuilding) restored).getPos().equals(building.getPos())
                                && ((AbstractTownBuilding) restored).isInitialized(),
                        "sparse optional fields must decode for every real building type");
                helper.assertTrue(!buffer.isReadable(), "the building decoder consumes its full payload");
            } finally { buffer.release(); }
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "town_view_wire", timeoutTicks = 40)
    public static void nestedSparseSlotsRetainNullWithoutChangingTypedLists(GameTestHelper helper) {
        List<Object> expected = Arrays.asList(7, null, "field", List.of(1, 2, 3),
                Arrays.asList("nested", null, 1L), List.of());
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ObjectWriter.writeObject(buffer, expected);
            helper.assertTrue(expected.equals(ObjectWriter.readObject(buffer)),
                    "nested missing slots must remain null while typed and empty lists retain their values");
            CodecUtil.writeCodec(buffer, TownTransportSnapshot.CODEC, TownTransportSnapshot.EMPTY);
            TownTransportSnapshot restored = CodecUtil.readCodec(buffer, TownTransportSnapshot.CODEC);
            helper.assertTrue(restored.equals(TownTransportSnapshot.EMPTY) && !buffer.isReadable(),
                    "sparse transport defaults must round-trip through the same wire reader");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "town_view_client", timeoutTicks = 40)
    public static void sameTownRefreshKeepsDisplayAndSuspendsDeltas(GameTestHelper helper) {
        UUID holder = UUID.randomUUID();
        TeamTownData before = new TeamTownData(null);
        TeamTownData after = new TeamTownData(null);
        int[] notifications = {0};
        ITownDataUpdateListener listener = new ITownDataUpdateListener() {
            @Override public void onBuildingsChanged() { notifications[0]++; }
        };
        try {
            TownClientUpdates.install(holder, before);
            TeamTownData.addClientListener(listener);
            TownClientUpdates.pending(holder);
            TownClientUpdates.pending(holder);
            helper.assertTrue(TownClientUpdates.currentTown().orElseThrow() == before && notifications[0] == 0,
                    "same-town waiting keeps the actual display object and does not rebuild listeners");
            TownClientUpdates.apply(town -> { throw new AssertionError("a pending view must reject deltas"); });
            TownClientUpdates.install(holder, after);
            helper.assertTrue(TownClientUpdates.currentTown().orElseThrow() == after && notifications[0] == 1,
                    "one complete installation replaces the view and notifies once");
            boolean[] applied = {false};
            TownClientUpdates.apply(town -> applied[0] = town == after);
            helper.assertTrue(applied[0], "deltas resume only after complete installation");
        } finally {
            TeamTownData.removeClientListener(listener);
            TownClientUpdates.disconnected(null);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "town_view_client", timeoutTicks = 40)
    public static void ownershipAbsenceAndDisconnectClearTheOldView(GameTestHelper helper) {
        try {
            UUID first = UUID.randomUUID(), second = UUID.randomUUID();
            TownClientUpdates.install(first, new TeamTownData(null));
            TownClientUpdates.pending(second);
            helper.assertTrue(TownClientUpdates.currentTown().isEmpty(), "another holder cannot display the old town");
            TownClientUpdates.install(second, new TeamTownData(null));
            TownClientUpdates.install(second, null);
            helper.assertTrue(TownClientUpdates.currentTown().isEmpty(), "confirmed absence removes the view");
            TownClientUpdates.install(second, new TeamTownData(null));
            TownClientUpdates.disconnected(null);
            helper.assertTrue(TownClientUpdates.currentTown().isEmpty(), "disconnect removes retained display data");
        } finally { TownClientUpdates.disconnected(null); }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "town_view_metadata", timeoutTicks = 40)
    public static void ordinaryRecordsDoNotRequireFullMetadataViews(GameTestHelper helper) {
        TownBuildingStore store = new TownBuildingStore();
        List<Boolean> changes = new ArrayList<>();
        store.setListeners(null, changes::add);
        store.setDimension(helper.getLevel().dimension());
        var house = new com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding(BlockPos.ZERO);
        store.install(house);
        store.removeAt(BlockPos.ZERO);
        helper.assertTrue(changes.equals(List.of(true, false, false)),
                "dimension metadata needs a full view; ordinary add/remove already have building deltas");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "town_view_online_operations", timeoutTicks = 550)
    public static void twoPlayersKeepOpenMenusThroughStockAndBuildingChanges(GameTestHelper helper) {
        Session session = new Session(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)), null);
        try {
            session.warm();
            session.openHouse();
            var originalMenu = session.first.containerMenu;
            int[] deposits = {0};
            helper.startSequence().thenExecuteFor(400, () -> {
                try {
                    int elapsed = ++session.elapsed;
                    if (elapsed % 20 == 0) { session.deposit(); deposits[0]++; }
                    if (elapsed == 80) session.placeExtra();
                    if (elapsed == 140) session.removeExtra();
                    if (elapsed % 40 == 0) {
                        TownCommonEvents.syncTownDataOnGuiOpen(new PlayerContainerEvent.Open(session.first, originalMenu));
                        TownSync.ensureCurrent(session.second);
                    }
                    session.drain();
                    helper.assertTrue(session.first.containerMenu == originalMenu && originalMenu.stillValid(session.first),
                            "the first real player's original house menu must remain open");
                    helper.assertTrue(session.firstProbe.full == 0 && session.secondProbe.full == 0,
                            "stock, ordinary records and repeated GUI checks must not send a full envelope");
                } catch (RuntimeException | Error failure) { session.close(); throw failure; }
            }).thenExecute(() -> {
                try {
                    session.data.tick(session.level, session.holder);
                    session.drain();
                    helper.assertTrue(session.data.resources.get(new ItemStack(Items.COAL)) == deposits[0],
                            "every actual warehouse quick-move action changes authoritative stock once");
                    helper.assertTrue(session.firstProbe.resources > 0 && session.secondProbe.resources > 0
                                    && session.firstProbe.buildings > 0 && session.secondProbe.buildings > 0,
                            "both real connections receive resource and building deltas");
                    FHMain.LOGGER.info("TOWN_VIEW_ONLINE,ticks=400,players=2,deposits={},fullA={},fullB={},resourceA={},resourceB={},buildingA={},buildingB={}",
                            deposits[0], session.firstProbe.full, session.secondProbe.full,
                            session.firstProbe.resources, session.secondProbe.resources,
                            session.firstProbe.buildings, session.secondProbe.buildings);
                    helper.succeed();
                } finally { session.close(); }
            });
        } catch (RuntimeException | Error failure) { session.close(); throw failure; }
    }

    @GameTest(template = "phase0a_empty", batch = "town_view_online_recovery", timeoutTicks = 80)
    public static void repeatedRequestsCoalesceAndAbsentViewsFinish(GameTestHelper helper) {
        Session session = new Session(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)), null);
        try {
            session.warm();
            session.data.inSettlement = true;
            try {
                for (int i = 0; i < 12; i++) TownSync.requestFull(session.first, "test_refresh");
                TownSync.tick(session.level.getServer());
                session.drain();
                helper.assertTrue(session.firstProbe.full == 1 && session.firstProbe.pending == 1
                                && session.holder.getId().equals(session.firstProbe.lastHolder),
                        "twelve requests issue one holder-specific pending envelope while the town is busy");
                for (int i = 0; i < 12; i++) TownSync.ensureCurrent(session.first);
                TownSync.tick(session.level.getServer());
                session.drain();
                helper.assertTrue(session.firstProbe.full == 1, "repeated checks do not restart pending work");
            } finally { session.data.inSettlement = false; }
            // The busy retry is one real server tick later.
            helper.startSequence().thenIdle(2).thenExecute(() -> {
                try {
                    session.drain();
                    helper.assertTrue(session.firstProbe.present == 1 && session.firstProbe.pending == 1,
                            "the accepted pending request completes with exactly one present envelope");
                    session.holder.removeData(FHSpecialDataTypes.TOWN_DATA);
                    TownSync.tick(session.level.getServer());
                    session.drain();
                    helper.assertTrue(session.firstProbe.absent == 1 && session.secondProbe.absent == 1,
                            "component absence reaches both connections");
                    helper.succeed();
                } finally { session.close(); }
            });
        } catch (RuntimeException | Error failure) { session.close(); throw failure; }
    }

    @GameTest(template = "phase0a_empty", batch = "town_view_towerless", timeoutTicks = 80)
    public static void townWithoutTowerStillSendsItsChanges(GameTestHelper helper) {
        Session session = new Session(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)), null, false);
        try {
            helper.assertTrue(session.holder.getOptional(FHSpecialDataTypes.GENERATOR_DATA).isEmpty()
                            && TeamTownData.executionLevel(session.holder) == session.level,
                    "a town that never had a tower runs in its buildings' world");
            session.warm();
            session.placeExtra();
            // No manual tick from here on: the server's own town update has to deliver every change.
            helper.startSequence().thenIdle(3).thenExecute(() -> {
                try {
                    session.drain();
                    helper.assertTrue(session.firstProbe.buildings > 0 && session.secondProbe.buildings > 0,
                            "a building placed before any tower reaches both connections");
                    var tower = session.holder.getData(FHSpecialDataTypes.GENERATOR_DATA);
                    tower.dimension = session.level.dimension();
                    tower.actualPos = session.origin.offset(30, 0, 3);
                    session.firstProbe.clear();
                    session.secondProbe.clear();
                } catch (RuntimeException | Error failure) { session.close(); throw failure; }
            }).thenIdle(3).thenExecute(() -> {
                try {
                    session.drain();
                    helper.assertTrue(session.origin.offset(30, 0, 3).equals(session.data.getTowerPos())
                                    && session.firstProbe.towers > 0 && session.secondProbe.towers > 0,
                            "a tower built later is announced to both connections");
                    session.holder.getData(FHSpecialDataTypes.GENERATOR_DATA).actualPos = null;
                    session.firstProbe.clear();
                    session.secondProbe.clear();
                } catch (RuntimeException | Error failure) { session.close(); throw failure; }
            }).thenIdle(3).thenExecute(() -> {
                try {
                    session.drain();
                    helper.assertTrue(session.data.getTowerPos() == null
                                    && session.firstProbe.towers > 0 && session.secondProbe.towers > 0,
                            "dismantling the tower is announced too, and the town keeps updating");
                    helper.succeed();
                } finally { session.close(); }
            });
        } catch (RuntimeException | Error failure) { session.close(); throw failure; }
    }

    /** Shared by the dedicated GameTest and opt-in rendered client verification. */
    public static final class Session implements AutoCloseable {
        public final ServerLevel level;
        public final ServerPlayer first, second;
        public final TeamDataHolder holder;
        public final TeamTownData data;
        public final BlockPos origin, housePos, warehousePos, extraPos;
        public final Probe firstProbe, secondProbe;
        final TeamsAPIProvider originalProvider;
        final List<ServerPlayer> members = new ArrayList<>();
        final List<EmbeddedChannel> channels = new ArrayList<>();
        final boolean ownsFirst;
        int elapsed;
        boolean closed;

        public Session(ServerLevel level, BlockPos origin, ServerPlayer existingFirst) {
            this(level, origin, existingFirst, true);
        }

        /** {@code withTowerData} false leaves the team without any generator data, as before its first tower. */
        public Session(ServerLevel level, BlockPos origin, ServerPlayer existingFirst, boolean withTowerData) {
            this.level = level; this.origin = origin;
            housePos = origin.offset(3, 0, 3);
            warehousePos = origin.offset(13, 0, 3);
            extraPos = origin.offset(23, 0, 3);
            ownsFirst = existingFirst == null;
            first = ownsFirst ? player("view-a") : existingFirst;
            second = player("view-b");
            members.add(first); members.add(second);
            originalProvider = TeamsAPI.getAPI();
            AbstractTeam team = new AbstractTeam() {
                final UUID id = UUID.randomUUID();
                public UUID getId() { return id; }
                public UUID getOwner() { return first.getUUID(); }
                public String getName() { return "town-view-test"; }
                public Collection<ServerPlayer> getOnlineMembers() { return List.copyOf(members); }
            };
            TeamsAPI.register(new TeamsAPIProvider() {
                public AbstractTeam getTeamByPlayer(ServerPlayer player) {
                    return members.contains(player) ? team : originalProvider.getTeamByPlayer(player);
                }
                public AbstractTeam getTeamByUuid(UUID id) {
                    return team.getId().equals(id) ? team : originalProvider.getTeamByUuid(id);
                }
                public String getProviderName() { return "town-view-test"; }
            });
            holder = CTeamDataManager.INSTANCE.get(team);
            if (withTowerData) holder.getData(FHSpecialDataTypes.GENERATOR_DATA).dimension = level.dimension();
            data = holder.getData(FHSpecialDataTypes.TOWN_DATA);
            firstProbe = ownsFirst ? new Probe(channels.get(0)) : null;
            secondProbe = new Probe(channels.get(channels.size() - 1));
            try {
                if (ownsFirst) level.getServer().getPlayerList().placeNewPlayer(channels.get(0).pipeline().get(Connection.class), first);
                level.getServer().getPlayerList().placeNewPlayer(channels.get(channels.size() - 1).pipeline().get(Connection.class), second);
                buildRoom(origin);
                buildRoom(origin.offset(10, 0, 0));
                place(housePos, FHBlocks.HOUSE.get());
                place(warehousePos, FHBlocks.WAREHOUSE.get());
                var house = (HouseBlockEntity) level.getBlockEntity(housePos);
                house.refresh_safe(house.getBuilding().orElseThrow());
                var warehouse = (WarehouseBlockEntity) level.getBlockEntity(warehousePos);
                warehouse.refresh_safe(warehouse.getBuilding().orElseThrow());
                data.prepareView(level, holder);
                if (data.resources.getCapacityLeft() < 100) throw new AssertionError("the real warehouse room must provide capacity");
                second.setPos(warehousePos.getX() + .5, warehousePos.getY() + 1, warehousePos.getZ() + .5);
                NetworkHooks.openScreen(second, warehouse, warehousePos);
            } catch (RuntimeException | Error failure) { close(); throw failure; }
        }

        private ServerPlayer player(String name) {
            ServerPlayer player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), name));
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            EmbeddedChannel channel = new EmbeddedChannel(connection);
            connection.setProtocol(ConnectionProtocol.PLAY);
            channels.add(channel);
            return player;
        }

        public void warm() {
            data.prepareView(level, holder);
            TownSync.tick(level.getServer());
            data.tick(level, holder);
            drain();
            if (firstProbe != null) firstProbe.clear();
            secondProbe.clear();
        }

        public void openHouse() {
            first.setPos(housePos.getX() + .5, housePos.getY() + 1, housePos.getZ() + .5);
            NetworkHooks.openScreen(first, (HouseBlockEntity) level.getBlockEntity(housePos), housePos);
        }

        public void deposit() {
            double before = data.resources.get(new ItemStack(Items.COAL));
            second.getInventory().setItem(9, new ItemStack(Items.COAL));
            ((WarehouseMenu) second.containerMenu).quickMoveStack(second, 0);
            if (!second.getInventory().getItem(9).isEmpty()
                    || data.resources.get(new ItemStack(Items.COAL)) != before + 1)
                throw new AssertionError("the normal warehouse menu must accept the real inventory stack");
        }

        public AbstractTownBuildingBlockEntity<?> place(BlockPos pos, Block block) {
            level.getChunkAt(pos);
            level.setBlockAndUpdate(pos, block.defaultBlockState());
            var core = (AbstractTownBuildingBlockEntity<?>) level.getBlockEntity(pos);
            core.townProvider = new TeamTownProvider(holder.getId());
            core.setChanged();
            data.createTeamTown().onCorePlaced(level, pos, core);
            return core;
        }

        public void placeExtra() { place(extraPos, FHBlocks.HUNTING_BASE.get()); }
        public void removeExtra() { level.destroyBlock(extraPos, false, second); }
        public void drain() { if (firstProbe != null) firstProbe.drain(); secondProbe.drain(); }

        private void buildRoom(BlockPos start) {
            for (int x = 0; x <= 6; x++) for (int y = 0; y <= 4; y++) for (int z = 0; z <= 6; z++) {
                boolean shell = x == 0 || x == 6 || y == 0 || y == 4 || z == 0 || z == 6;
                level.setBlockAndUpdate(start.offset(x, y, z), (shell ? Blocks.STONE : Blocks.AIR).defaultBlockState());
            }
            BlockPos door = start.offset(3, 1, 0);
            level.setBlockAndUpdate(door, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            level.setBlockAndUpdate(door.above(), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            BlockPos bed = start.offset(2, 1, 3);
            level.setBlockAndUpdate(bed, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.FOOT));
            level.setBlockAndUpdate(bed.south(), Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.HEAD));
        }

        @Override public void close() {
            if (closed) return;
            closed = true;
            data.inSettlement = false;
            TeamsAPI.register(originalProvider);
            members.clear();
            level.getServer().getPlayerList().remove(second);
            if (ownsFirst) level.getServer().getPlayerList().remove(first);
            for (EmbeddedChannel channel : channels) channel.finishAndReleaseAll();
            holder.removeData(FHSpecialDataTypes.TOWN_DATA);
            holder.removeData(FHSpecialDataTypes.GENERATOR_DATA);
        }
    }

    public static final class Probe {
        private final EmbeddedChannel channel;
        private final int fullId, resourceId, buildingId, towerId, staffingId, housingId;
        public int full, pending, present, absent, resources, buildings, towers;
        public UUID lastHolder;
        public TownStaffingPlan lastStaffingPlan;
        public TownHousingPlan lastHousingPlan;
        public int staffingPlans, housingPlans;

        Probe(EmbeddedChannel channel) {
            this.channel = channel;
            fullId = discriminator(TeamTownDataS2CPacket.pending(null));
            resourceId = discriminator(new TownResourceUpdatePacket(Map.of(), 0, TownTransportSnapshot.EMPTY));
            buildingId = discriminator(new TownBuildingUpdatePacket(Map.of(), Set.of()));
            towerId = discriminator(new TownTowerUpdatePacket((BlockPos) null));
            staffingId = discriminator(new com.teammoeg.frostedheart.content.town.network.TownStaffingPlanUpdatePacket(TownStaffingPlan.EMPTY));
            housingId = discriminator(new com.teammoeg.frostedheart.content.town.network.TownHousingPlanUpdatePacket(TownHousingPlan.EMPTY));
        }

        private static int discriminator(CMessage message) {
            var packet = (ClientboundCustomPayloadPacket) FHNetwork.INSTANCE.get().toVanillaPacket(message, NetworkDirection.PLAY_TO_CLIENT);
            FriendlyByteBuf data = packet.getData();
            try { return data.readVarInt(); }
            finally { data.release(); }
        }

        public void drain() {
            channel.runPendingTasks();
            Object packet;
            while ((packet = channel.readOutbound()) != null) {
                if (!(packet instanceof ClientboundCustomPayloadPacket payload)) continue;
                FriendlyByteBuf data = payload.getData();
                try {
                    if (!payload.getIdentifier().toString().equals("frostedheart:network")) continue;
                    int id = data.readVarInt();
                    if (id == fullId) {
                        full++;
                        var state = data.readEnum(TeamTownDataS2CPacket.State.class);
                        lastHolder = data.readBoolean() ? data.readUUID() : null;
                        if (state == TeamTownDataS2CPacket.State.PENDING) pending++;
                        else if (state == TeamTownDataS2CPacket.State.PRESENT) present++;
                        else absent++;
                    } else if (id == resourceId) resources++;
                    else if (id == buildingId) buildings++;
                    else if (id == towerId) towers++;
                    else if (id == staffingId) {
                        lastStaffingPlan = new com.teammoeg.frostedheart.content.town.network.TownStaffingPlanUpdatePacket(data).plan();
                        staffingPlans++;
                    } else if (id == housingId) {
                        lastHousingPlan = new com.teammoeg.frostedheart.content.town.network.TownHousingPlanUpdatePacket(data).plan();
                        housingPlans++;
                    }
                } finally { data.release(); }
            }
        }

        public void clear() {
            full = pending = present = absent = resources = buildings = towers = staffingPlans = housingPlans = 0;
            lastHolder = null;
            lastStaffingPlan = null;
            lastHousingPlan = null;
        }
    }
}
