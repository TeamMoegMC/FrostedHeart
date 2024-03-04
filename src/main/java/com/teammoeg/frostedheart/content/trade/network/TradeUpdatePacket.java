/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.trade.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.content.trade.RelationList;
import com.teammoeg.frostedheart.content.trade.gui.TradeContainer;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

public class TradeUpdatePacket implements FHMessage {
    CompoundNBT data;
    CompoundNBT player;
    RelationList relations;
    boolean isReset;


    public TradeUpdatePacket(CompoundNBT data, CompoundNBT player, RelationList relations, boolean isReset) {
        super();
        this.data = data;
        this.player = player;
        this.relations = relations;
        this.isReset = isReset;
    }

    public TradeUpdatePacket(PacketBuffer buffer) {
        data = buffer.readCompoundTag();
        player = buffer.readCompoundTag();
        relations = new RelationList();
        relations.read(buffer);
        isReset = buffer.readBoolean();
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeCompoundTag(data);
        buffer.writeCompoundTag(player);
        relations.write(buffer);
        buffer.writeBoolean(isReset);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            PlayerEntity player = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getPlayer);
            Container cont = player.openContainer;
            if (cont instanceof TradeContainer) {
                TradeContainer trade = (TradeContainer) cont;
                trade.update(data, this.player, relations, isReset);
            }
        });
        context.get().setPacketHandled(true);
    }
}
