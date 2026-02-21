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

package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.VirtualItemStack;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
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
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null && player.containerMenu instanceof WarehouseMenu menu) {
				//更新屏幕
				menu.updateResourceList(resources);
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
