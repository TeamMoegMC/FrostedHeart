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

package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.chorda.io.codec.ObjectWriter;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.warehouse.VirtualItemStack;
import com.teammoeg.frostedheart.content.town.warehouse.WarehouseMenu;
import com.teammoeg.frostedheart.content.town.warehouse.WarehouseScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class WarehouseC2SRequestPacket implements CMessage {

    public WarehouseC2SRequestPacket() {

    }

	public WarehouseC2SRequestPacket(FriendlyByteBuf buffer) {
	}

    @Override
	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			// 确认玩家打开的是仓库界面
			if (player.containerMenu instanceof WarehouseMenu) {
				// 获取数据
				Map<ItemStack, Double> itemMap = TeamTown.from(player).getResourceHolder().getAllItems();
				// 遍历 Map，提取 Key(物品) 和 Value(数量)
				List<VirtualItemStack> list = VirtualItemStack.toClientVisualList(itemMap);

				// 发回给客户端;
				FHNetwork.INSTANCE.sendPlayer(player, new WarehouseS2CPacket(list));
			}
		});
		ctx.get().setPacketHandled(true);
	}
	@Override
	public void encode(FriendlyByteBuf buffer) {
	}
}
