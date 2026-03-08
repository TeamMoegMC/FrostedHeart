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
import com.teammoeg.frostedheart.content.town.buildings.warehouse.SimpleItemKey;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.VirtualItemStack;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class WarehouseUpdatePacket implements CMessage {
	private final List<VirtualItemStack> resources;
	private final boolean isIncremental;

	public WarehouseUpdatePacket(List<VirtualItemStack> resources, boolean isIncremental) {
		this.resources = resources;
		this.isIncremental = isIncremental;
	}

	public WarehouseUpdatePacket(FriendlyByteBuf buffer) {
		this.isIncremental = buffer.readBoolean();
		int size = buffer.readVarInt();
		this.resources = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			SimpleItemKey key = readSimpleItemKey(buffer);
			long amount = buffer.readVarLong();
			this.resources.add(new VirtualItemStack(key, amount));
		}
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBoolean(this.isIncremental);
		buffer.writeVarInt(this.resources.size());
		for (VirtualItemStack vStack : this.resources) {
			writeSimpleItemKey(buffer, vStack.getKey());
			buffer.writeVarLong(vStack.getAmount());
		}
	}

	private static void writeSimpleItemKey(FriendlyByteBuf buf, SimpleItemKey key) {
		buf.writeId(BuiltInRegistries.ITEM, key.item());
		buf.writeNbt(key.tag());
	}

	private static SimpleItemKey readSimpleItemKey(FriendlyByteBuf buf) {
		Item item = buf.readById(BuiltInRegistries.ITEM);
		CompoundTag tag = buf.readNbt();
		return new SimpleItemKey(item, tag);
	}

	@Override
	public void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null && player.containerMenu instanceof WarehouseMenu menu) {
				menu.updateResourceList(this.resources, this.isIncremental);
			}
		});
		context.get().setPacketHandled(true);
	}
}
