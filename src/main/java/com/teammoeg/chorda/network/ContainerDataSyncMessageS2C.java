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

package com.teammoeg.chorda.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.io.SerializeUtil;
import com.teammoeg.chorda.menu.CBaseMenu;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.NetworkEncoder;
import com.teammoeg.chorda.menu.CCustomMenuSlot.OtherDataSlotEncoder;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public record ContainerDataSyncMessageS2C(List<ContainerDataPair> data) implements CMessage {

	private static record ContainerDataPair(int slotIndex,NetworkEncoder<?> conv,Object data){
		public ContainerDataPair(FriendlyByteBuf buf,int slotIndex,NetworkEncoder<?> conv) {
			this(slotIndex,conv,conv.read(buf));
		}
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public void write(FriendlyByteBuf buffer) {
			buffer.writeVarInt(slotIndex);
			CCustomMenuSlot.Encoders.write(buffer, conv);
			((NetworkEncoder)conv).write(buffer, data);
		}
	}
	
	public ContainerDataSyncMessageS2C() {
		this(new ArrayList<>());
	}
	public void add(int slotIndex,OtherDataSlotEncoder<?> conv,Object data) {
		this.data.add(new ContainerDataPair(slotIndex,conv.getEncoder(),data));
	}
	
	public void forEach(BiConsumer<Integer,Object> t) {
		data.forEach(o->t.accept(o.slotIndex, o.data));
	}
	public boolean hasData() {
		return !this.data.isEmpty();
	}

	public ContainerDataSyncMessageS2C(FriendlyByteBuf buf) {
		this(SerializeUtil.readList(buf, t->new ContainerDataPair(buf,buf.readVarInt(), CCustomMenuSlot.Encoders.read(buf))));
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		SerializeUtil.writeList(buffer, data, ContainerDataPair::write);
	}
	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(()->{
			if(ClientUtils.getPlayer().containerMenu instanceof CBaseMenu container) {
				container.processPacket(this);
				
			}
		});
		context.get().setPacketHandled(true);
	}

}
