/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.experiment;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedresearch.mixinutil.IOwnerTile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 实验台界面的操作；服务端按当前队伍状态重新判断。 / Experiment table screen actions, judged again on the server.
 */
public record ExperimentTableActionPacket(BlockPos pos, Action action, String run, String requirement) implements CMessage {
    public enum Action {SELECT, CHECK, START, STOP}

    public ExperimentTableActionPacket(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readEnum(Action.class), buffer.readUtf(64), buffer.readUtf(64));
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeEnum(action);
        buffer.writeUtf(run, 64);
        buffer.writeUtf(requirement, 64);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            ExperimentTableBlockEntity table = player == null ? null : table(player);
            if (table == null) return;
            switch (action) {
                case SELECT -> {
                    UUID id = uuid(run);
                    if (id != null) table.select(player, id, requirement);
                }
                case CHECK -> table.check(player);
                case START -> table.start(player);
                case STOP -> table.stop();
            }
            table.setChanged();
            table.syncData();
        });
        context.get().setPacketHandled(true);
    }

    private ExperimentTableBlockEntity table(ServerPlayer player) {
        if (!(player.containerMenu instanceof ExperimentTableMenu menu)) return null;
        ExperimentTableBlockEntity table = menu.getBlock();
        if (table == null || table.isRemoved() || !pos.equals(table.getBlockPos()) || table.getLevel() != player.serverLevel()
                || player.distanceToSqr(Vec3.atCenterOf(pos)) > 64) return null;
        var team = CTeamDataManager.get(player);
        return team != null && team.getId().equals(IOwnerTile.getOwner(table)) ? table : null;
    }

    private static UUID uuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }
}
