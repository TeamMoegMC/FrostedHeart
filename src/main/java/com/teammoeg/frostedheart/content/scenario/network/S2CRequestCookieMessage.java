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

package com.teammoeg.frostedheart.content.scenario.network;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.teammoeg.chorda.dataholders.client.CClientDataStorage;
import com.teammoeg.chorda.io.SerializeUtil;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.scenario.client.ClientStoredFlags;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public record S2CRequestCookieMessage(int id,List<String> keys) implements CMessage{
	public S2CRequestCookieMessage(FriendlyByteBuf buffer) {
		this(buffer.readVarInt(),SerializeUtil.readList(buffer, FriendlyByteBuf::readUtf));
	}
	@Override
	public void encode(FriendlyByteBuf buffer) {
		//System.out.println(keys);
		buffer.writeVarInt(id);
		SerializeUtil.writeList2(buffer, keys, FriendlyByteBuf::writeUtf);
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(()->{
			final CompoundTag tag=new CompoundTag();
			Optional<ClientStoredFlags> odata=CClientDataStorage.getData().getOptional(FHSpecialDataTypes.SCENARIO_COOKIES);
			if(odata.isPresent()) {
				ClientStoredFlags data=odata.get();
				for(String s:keys) {
					Tag eValue=data.getData().get(s);
					if(eValue!=null)
						tag.put(s, eValue);
				}
			}
			//System.out.println(tag);
			FHNetwork.INSTANCE.sendToServer(new C2SScenarioCookies(id,tag));
			
		});
		context.get().setPacketHandled(true);
	}

}
