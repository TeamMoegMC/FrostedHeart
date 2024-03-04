/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.research.network;

import java.util.Objects;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.content.research.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedheart.content.research.gui.drawdesk.game.CardPos;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;

// send when data update
public class FHDrawingDeskOperationPacket implements FHMessage {
    private final BlockPos pos;
    private final byte op;
    private final CardPos pos1;
    private final CardPos pos2;

    public FHDrawingDeskOperationPacket(BlockPos pos) {
        this(pos, 0, null, null);

    }

    public FHDrawingDeskOperationPacket(BlockPos pos, CardPos p1) {
        this(pos, 1, p1, null);
    }

    public FHDrawingDeskOperationPacket(BlockPos pos, CardPos p1, CardPos p2) {
        this(pos, 2, p1, p2);
    }

    public FHDrawingDeskOperationPacket(BlockPos pos, int op) {
        this(pos, op, null, null);

    }

    protected FHDrawingDeskOperationPacket(BlockPos pos, int op, CardPos pos1, CardPos pos2) {
        super();
        this.pos = pos;
        this.op = (byte) op;
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public FHDrawingDeskOperationPacket(PacketBuffer buffer) {
        pos = buffer.readBlockPos();
        op = buffer.readByte();
        if (op < 3) {
            if (op > 0)
                pos1 = CardPos.valueOf(buffer);
            else
                pos1 = null;
            if (op > 1)
                pos2 = CardPos.valueOf(buffer);
            else
                pos2 = null;
        } else pos1 = pos2 = null;
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeByte(op);
        if (op < 3) {
            if (op > 0)
                pos1.write(buffer);
            if (op > 1)
                pos2.write(buffer);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerWorld world = Objects.requireNonNull(context.get().getSender()).getServerWorld();
            TileEntity tile = Utils.getExistingTileEntity(world, pos);
            if (tile instanceof DrawingDeskTileEntity) {
               // ResearchGame rg = ((DrawingDeskTileEntity) tile).getGame();
                boolean flag = true;
                switch (op) {
                    case 0:
                        ((DrawingDeskTileEntity) tile).initGame(context.get().getSender());
                        break;
                    case 1:
                        flag = ((DrawingDeskTileEntity) tile).tryCombine(context.get().getSender(), pos1, null);
                        break;
                    case 2:
                        flag = ((DrawingDeskTileEntity) tile).tryCombine(context.get().getSender(), pos1, pos2);
                        break;
                    case 3:
                        ((DrawingDeskTileEntity) tile).submitItem(context.get().getSender());
                        break;
                }
                if (flag) {
                    ((DrawingDeskTileEntity) tile).updateGame(context.get().getSender());
                    ((DrawingDeskTileEntity) tile).markDirty();
                    ((DrawingDeskTileEntity) tile).markContainingBlockForUpdate(null);
                }
            }

            //ClientUtils.refreshResearchGui();
        });
        context.get().setPacketHandled(true);
    }
}
