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

import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.io.codec.DataOps;
import com.teammoeg.chorda.io.codec.ObjectWriter;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlockScreen;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.warehouse.VirtualItemStack;
import com.teammoeg.frostedheart.content.town.warehouse.WarehouseScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class WarehouseS2CPacket implements CMessage {
	private final List<VirtualItemStack> resources;

    public WarehouseS2CPacket(List<VirtualItemStack> resources) {
		this.resources = resources;
    }

	public WarehouseS2CPacket(FriendlyByteBuf buffer) {
		this.resources = buffer.readList(buf -> {
			ItemStack stack = buf.readItem();
			long amount = buf.readLong();
			return new VirtualItemStack(stack, amount);
		});
	}

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			if (Minecraft.getInstance().screen instanceof WarehouseScreen screen) {
				//更新屏幕
				screen.updateResourceList(resources);
			}
		});
		context.get().setPacketHandled(true);
	}
	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeCollection(this.resources, (buf, vStack) -> {
			buf.writeItem(vStack.getStack());
			buf.writeLong(vStack.getAmount());
		});
	}
}
