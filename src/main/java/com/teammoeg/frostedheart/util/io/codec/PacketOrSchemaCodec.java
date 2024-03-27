package com.teammoeg.frostedheart.util.io.codec;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.teammoeg.frostedheart.util.io.CodecUtil;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;

public class PacketOrSchemaCodec<A,S> implements Codec<A> {
	Function<A,S> schemaSerialize;
	Function<S,A> schemaDeserialize;
	BiConsumer<A,PacketBuffer> bufferSerialize;
	Function<byte[],A> bufferDeserialize;
	DynamicOps<S> schemaCodec;

	public PacketOrSchemaCodec(DynamicOps<S> schema, Function<A, S> schemaSerialize, Function<S, A> schemaDeserialize, BiConsumer<A, PacketBuffer> bufferSerialize, Function<PacketBuffer, A> bufferDeserialize) {
		super();
		this.schemaSerialize = schemaSerialize;
		this.schemaDeserialize = schemaDeserialize;
		this.bufferSerialize = bufferSerialize;
		this.bufferDeserialize = bs->bufferDeserialize.apply(new PacketBuffer(Unpooled.wrappedBuffer(bs)));
		schemaCodec=schema;
	}

	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		if(ops.compressMaps()) {
			PacketBuffer buffer=new PacketBuffer(Unpooled.buffer());
			bufferSerialize.accept(input, buffer);
			byte[] ba=new byte[buffer.writerIndex()];
			buffer.getBytes(0, ba);
			return CodecUtil.BYTE_ARRAY_CODEC.encode(ba, ops, prefix);
		}
		return CodecUtil.convertSchema(schemaCodec).encode(schemaSerialize.apply(input), ops, prefix);
	}

	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		if(ops.compressMaps()) {
			return CodecUtil.BYTE_ARRAY_CODEC.decode(ops, input).map(t->t.mapFirst(bufferDeserialize));
		}
		DataResult<Pair<S, T>> obj=CodecUtil.convertSchema(schemaCodec).decode(ops, input);
		return obj.map(o->o.mapFirst(schemaDeserialize));
	}

	@Override
	public String toString() {
		return "PacketOrSchema["+schemaCodec+"]";
	}

}
