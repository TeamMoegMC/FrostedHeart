package com.teammoeg.chorda.io.registry;

import java.util.function.Function;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.teammoeg.chorda.io.PacketWritable;

import net.minecraft.network.FriendlyByteBuf;

public class CodecPacketBufferSerializerRegistry<A extends PacketWritable> extends PacketBufferSerializerRegistry<A,Dynamic<?>,Dynamic<?>> implements Codec<A>{

	public CodecPacketBufferSerializerRegistry() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		return DataResult.success((T)super.write(input,new Dynamic<>(ops,prefix)).getValue());
	}

	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		return DataResult.success(Pair.of(super.read(new Dynamic<>(ops,input)), input));
	}

	@Override
	protected void writeType(Pair<Integer, String> type, Dynamic<?> obj) {
		obj.merge(obj.createString("type"), obj.createString(type.getSecond()));
	}

	@Override
	protected String readType(Dynamic<?> obj) {
		return obj.asString().result().orElse(null);
	}

	public void register(Class<A> cls, String type, Codec<A> codec, Function<FriendlyByteBuf, A> packet) {
		super.register(cls, type,
			t->codec.decode(t).result().map(Pair::getFirst).orElse(null),
			(t,c)->((Dynamic<Object>)c).map(x->codec.encode(t,((Dynamic<Object>)c).getOps(),x).result().orElse(null)),
			packet);
	}




}
