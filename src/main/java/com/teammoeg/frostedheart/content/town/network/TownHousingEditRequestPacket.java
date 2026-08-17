/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

/** Server-authoritative single-operation edit of the housing-care plan. */
public final class TownHousingEditRequestPacket implements CMessage {
    public enum Action { MOVE, SET_GUARANTEE }

    private final Action action;
    private final BlockPos building;
    private final Optional<BlockPos> before;
    private final int target;

    private TownHousingEditRequestPacket(
            Action action, BlockPos building, Optional<BlockPos> before, int target
    ) {
        this.action = action;
        this.building = building;
        this.before = before;
        this.target = Math.max(0, target);
    }

    public static TownHousingEditRequestPacket move(BlockPos building, Optional<BlockPos> before) {
        return new TownHousingEditRequestPacket(Action.MOVE, building, before, 0);
    }

    public static TownHousingEditRequestPacket setGuarantee(BlockPos building, int target) {
        return new TownHousingEditRequestPacket(
                Action.SET_GUARANTEE, building, Optional.empty(), target);
    }

    public TownHousingEditRequestPacket(FriendlyByteBuf buffer) {
        int ordinal = buffer.readUnsignedByte();
        if (ordinal >= Action.values().length) throw new IllegalArgumentException("Unknown housing edit");
        action = Action.values()[ordinal];
        building = buffer.readBlockPos();
        before = buffer.readBoolean() ? Optional.of(buffer.readBlockPos()) : Optional.empty();
        target = Math.max(0, buffer.readVarInt());
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeByte(action.ordinal());
        buffer.writeBlockPos(building);
        buffer.writeBoolean(before.isPresent());
        before.ifPresent(buffer::writeBlockPos);
        buffer.writeVarInt(target);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            var teamData = CTeamDataManager.get(player);
            teamData.getOptional(FHSpecialDataTypes.TOWN_DATA).ifPresent(town -> {
                boolean changed = switch (action) {
                    case MOVE -> town.moveHousingEntry(building, before);
                    case SET_GUARANTEE -> town.setHousingGuarantee(building, target);
                };
                TownHousingPlanUpdatePacket packet =
                        new TownHousingPlanUpdatePacket(town.getHousingPlan());
                if (changed) teamData.sendToOnline(FHNetwork.INSTANCE, packet);
                else FHNetwork.INSTANCE.sendPlayer(player, packet);
            });
        });
        context.get().setPacketHandled(true);
    }

    public Action action() { return action; }
    public BlockPos building() { return building; }
    public Optional<BlockPos> before() { return before; }
    public int target() { return target; }
}
