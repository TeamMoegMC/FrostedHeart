package com.teammoeg.frostedheart.content.wheelmenu;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.network.CMessage;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public record WorldUserSelectionSyncMessage(List<UserSelection> selections) implements CMessage {
	
	private static final Codec<List<UserSelection>> CODEC=Codec.list(UserSelection.CODEC);
	public WorldUserSelectionSyncMessage(FriendlyByteBuf buffer) {
		this(CodecUtil.readCodec(buffer, CODEC));
	}
	@Override
	public void encode(FriendlyByteBuf buffer) {
		CodecUtil.writeCodec(buffer, CODEC, selections);
	}

	@Override
	public void handle(Supplier<Context> context) {
		
	}

}
