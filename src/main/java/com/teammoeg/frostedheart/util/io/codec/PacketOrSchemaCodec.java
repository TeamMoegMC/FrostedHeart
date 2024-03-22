package com.teammoeg.frostedheart.util.io.codec;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.teammoeg.frostedheart.util.io.SerializeUtil;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;

public class PacketOrSchemaCodec<A,S> implements Codec<A> {
	Function<A,S> schemaSerialize;
	Function<S,A> schemaDeserialize;
	BiConsumer<A,PacketBuffer> bufferSerialize;
	Function<PacketBuffer,A> bufferDeserialize;
	Codec<S> schemaCodec;

	public PacketOrSchemaCodec(DynamicOps<S> schema, Function<A, S> schemaSerialize, Function<S, A> schemaDeserialize, BiConsumer<A, PacketBuffer> bufferSerialize, Function<PacketBuffer, A> bufferDeserialize) {
		super();
		this.schemaSerialize = schemaSerialize;
		this.schemaDeserialize = schemaDeserialize;
		this.bufferSerialize = bufferSerialize;
		this.bufferDeserialize = bufferDeserialize;
		schemaCodec=SerializeUtil.convertSchema(schema);
	}

	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		if(ops.compressMaps()) {
			PacketBuffer buffer=new PacketBuffer(Unpooled.buffer());
			bufferSerialize.accept(input, buffer);
			T result=ops.createByteList(ByteBuffer.wrap(buffer.array()));
			return DataResult.success(result);
		}
		return schemaCodec.encode(schemaSerialize.apply(input), ops, prefix);
	}

	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		if(ops.compressMaps()) {
			return ops.getByteBuffer(input).map(ByteBuffer::array).map(Unpooled::wrappedBuffer).map(PacketBuffer::new).map(bufferDeserialize).map(k->Pair.of(k, input));
		}
		return schemaCodec.decode(ops, input).map(o->o.mapFirst(schemaDeserialize));
	}

}
