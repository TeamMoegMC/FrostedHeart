package com.teammoeg.frostedheart.content.scenario.network;

import java.util.function.Supplier;

import com.teammoeg.chorda.dataholders.client.CClientDataStorage;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.scenario.client.ClientStoredFlags;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public record S2CSetCookiesMessage(CompoundTag tag) implements CMessage{
	public S2CSetCookiesMessage(FriendlyByteBuf buffer) {
		this(buffer.readNbt());
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeNbt(tag);
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(()->{
			ClientStoredFlags data=CClientDataStorage.getData().getData(FHSpecialDataTypes.SCENARIO_COOKIES);
			for(String i:tag.getAllKeys()) {
				data.getData().put(i, tag.get(i));
			}
			CClientDataStorage.getData().markDirty();
		});
		context.get().setPacketHandled(true);
	}

}
