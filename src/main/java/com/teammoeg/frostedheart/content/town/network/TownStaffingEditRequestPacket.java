/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

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

/** Server-authoritative single-operation edit of the sender's town staffing plan. */
public final class TownStaffingEditRequestPacket implements CMessage {
    public enum Action {
        MOVE,
        SET_TARGET
    }

    private final Action action;
    private final BlockPos building;
    private final Optional<BlockPos> before;
    private final int target;

    private TownStaffingEditRequestPacket(
            Action action, BlockPos building, Optional<BlockPos> before, int target
    ) {
        this.action = action;
        this.building = building;
        this.before = before;
        this.target = target;
    }

    public static TownStaffingEditRequestPacket move(
            BlockPos building, Optional<BlockPos> before
    ) {
        return new TownStaffingEditRequestPacket(Action.MOVE, building, before, 0);
    }

    public static TownStaffingEditRequestPacket setTarget(BlockPos building, int target) {
        return new TownStaffingEditRequestPacket(
                Action.SET_TARGET, building, Optional.empty(), Math.max(0, target));
    }

    public TownStaffingEditRequestPacket(FriendlyByteBuf buffer) {
        int actionId = buffer.readUnsignedByte();
        if (actionId >= Action.values().length) {
            throw new IllegalArgumentException("Unknown staffing edit action: " + actionId);
        }
        this.action = Action.values()[actionId];
        this.building = buffer.readBlockPos();
        this.before = buffer.readBoolean()
                ? Optional.of(buffer.readBlockPos()) : Optional.empty();
        this.target = buffer.readVarInt();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeByte(action.ordinal());
        buffer.writeBlockPos(building);
        buffer.writeBoolean(before.isPresent());
        before.ifPresent(buffer::writeBlockPos);
        buffer.writeVarInt(Math.max(0, target));
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            var teamData = CTeamDataManager.get(player);
            teamData.getOptional(FHSpecialDataTypes.TOWN_DATA).ifPresent(town -> {
                boolean changed = switch (action) {
                    case MOVE -> town.moveStaffingEntry(building, before);
                    case SET_TARGET -> town.setStaffingTarget(building, target);
                };
                TownStaffingPlanUpdatePacket response =
                        new TownStaffingPlanUpdatePacket(town.getStaffingPlan());
                if (changed) {
                    teamData.sendToOnline(FHNetwork.INSTANCE, response);
                } else {
                    // The panel edits an optimistic local draft. Always return
                    // the authoritative value to the sender so a rejected or
                    // concurrently-obsolete operation cannot leave that draft
                    // stuck on screen indefinitely.
                    FHNetwork.INSTANCE.sendPlayer(player, response);
                }
            });
        });
        context.get().setPacketHandled(true);
    }

    public Action action() {
        return action;
    }

    public BlockPos building() {
        return building;
    }

    public Optional<BlockPos> before() {
        return before;
    }

    public int target() {
        return target;
    }
}
