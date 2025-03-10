package com.teammoeg.chorda.network;

import java.util.function.Function;

import net.minecraft.network.FriendlyByteBuf;

public interface MessageReader<T extends CMessage> extends Function<FriendlyByteBuf,T>{
	public T read(FriendlyByteBuf bb);
	default T apply(FriendlyByteBuf buffer) {
		return read(buffer);
	}
}
