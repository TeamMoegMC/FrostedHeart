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

package com.teammoeg.frostedheart.trade.network;

import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.trade.ClientTradeHandler;
import com.teammoeg.frostedheart.trade.RelationList;
import com.teammoeg.frostedheart.trade.gui.TradeContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class BargainResponse {
    boolean succeed;
    int discount;
    float rdiscount;
    int bargained;
    private RelationList relation;

    public BargainResponse(TradeContainer trade, boolean state) {
        super();
        this.relation = trade.relations;
        this.rdiscount = trade.discountRatio;
        this.discount = trade.maxdiscount;
        this.bargained = trade.bargained;
        this.succeed = state;
    }

    public BargainResponse(PacketBuffer buffer) {
        relation = new RelationList();
        relation.read(buffer);
        rdiscount = buffer.readFloat();
        discount = buffer.readVarInt();
        bargained = buffer.readVarInt();
        succeed = buffer.readBoolean();
    }

    public void encode(PacketBuffer buffer) {
        relation.write(buffer);
        buffer.writeFloat(rdiscount);
        buffer.writeVarInt(discount);
        buffer.writeVarInt(bargained);
        buffer.writeBoolean(succeed);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            PlayerEntity player = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getPlayer);
            Container cont = player.openContainer;
            if (cont instanceof TradeContainer) {
                TradeContainer trade = (TradeContainer) cont;
                trade.relations.copy(relation);
                trade.maxdiscount = discount;
                trade.discountRatio = rdiscount;
                trade.bargained = bargained;
                DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientTradeHandler::updateBargain);
            }
        });
        context.get().setPacketHandled(true);
    }
}
