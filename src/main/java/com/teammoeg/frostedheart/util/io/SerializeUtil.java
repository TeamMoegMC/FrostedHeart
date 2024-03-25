/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.util.io;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.EitherMapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.RegistryUtils;
import com.teammoeg.frostedheart.util.io.codec.AlternativeCodec;
import com.teammoeg.frostedheart.util.io.codec.CompressDifferCodec;
import com.teammoeg.frostedheart.util.io.codec.DataOps;
import com.teammoeg.frostedheart.util.io.codec.IntOrIdCodec;
import com.teammoeg.frostedheart.util.io.codec.NullableCodec;
import com.teammoeg.frostedheart.util.io.codec.ObjectWriter;
import com.teammoeg.frostedheart.util.io.codec.PacketOrSchemaCodec;
import com.teammoeg.frostedheart.util.io.codec.StreamCodec;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.SimpleRegistry;

public class SerializeUtil {

	public static class Deserializer<T extends JsonElement, U extends Writeable> {
		private int id;
		public Function<T, U> fromJson;
		public Function<PacketBuffer, U> fromPacket;

		public Deserializer(Function<T, U> fromJson, Function<PacketBuffer, U> fromPacket) {
			super();
			this.fromJson = fromJson;
			this.fromPacket = fromPacket;
		}

		public U read(PacketBuffer packet) {
			return fromPacket.apply(packet);
		}

		public U read(T json) {
			return fromJson.apply(json);
		}

		public JsonElement serialize(U obj) {
			return obj.serialize();
		}

		public void write(PacketBuffer packet, U obj) {
			packet.writeVarInt(id);
			obj.write(packet);
		}
	}

	public static ItemStack fromJson(JsonElement elm) {
		if (elm.isJsonPrimitive())
			return new ItemStack(RegistryUtils.getItem(new ResourceLocation(elm.getAsString())));
		else if (elm.isJsonObject()) {
			JsonObject jo = elm.getAsJsonObject();
			ItemStack ret = new ItemStack(RegistryUtils.getItem(new ResourceLocation(jo.get("id").getAsString())));
			if (jo.has("count"))
				ret.setCount(jo.get("count").getAsInt());
			if (jo.has("nbt"))
				try {
					ret.setTag(JsonToNBT.getTagFromJson(jo.get("nbt").getAsString()));
				} catch (CommandSyntaxException e) {
					FHMain.LOGGER.warn(e.getMessage());
				}
			return ret;
		}
		return ItemStack.EMPTY;
	}

	public static <A> NullableCodec<A> nullableCodecValue(Codec<A> val, A def) {
		return nullableCodec(val, () -> def);
	}

	public static <A> NullableCodec<A> nullableCodec(Codec<A> val, Supplier<A> def) {
		return new NullableCodec<A>(val, def);
	}

	public static <A> Codec<Stream<A>> streamCodec(Codec<A> codec) {
		return new StreamCodec<>(codec);
	}

	public static <K, V> Codec<Pair<K, V>> pairCodec(String nkey, Codec<K> key, String nval, Codec<V> val) {
		return RecordCodecBuilder.create(t -> t.group(key.fieldOf(nkey).forGetter(Pair::getFirst), val.fieldOf(nval).forGetter(Pair::getSecond))
			.apply(t, Pair::of));
	}

	public static <K, V> Codec<Map<K, V>> mapCodec(Codec<K> keyCodec, Codec<V> valueCodec) {
		return Codec.compoundList(keyCodec, valueCodec).xmap(pl -> pl.stream().collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)),
			pl -> pl.entrySet().stream().map(ent -> Pair.of(ent.getKey(), ent.getValue())).collect(Collectors.toList()));
	}

	public static <K, V> Codec<Map<K, V>> mapCodec(String nkey, Codec<K> keyCodec, String nval, Codec<V> valueCodec) {
		return Codec.list(SerializeUtil.pairCodec(nkey, keyCodec, nval, valueCodec)).xmap(pl -> pl.stream().collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)),
			pl -> pl.entrySet().stream().map(ent -> Pair.of(ent.getKey(), ent.getValue())).collect(Collectors.toList()));
	}

	public static <A> Codec<A> createIntCodec(SimpleRegistry<A> registry) {
		return Codec.INT.xmap(registry::getByValue, registry::getId);
	}
	private static <K> Codec<K> scCodec(DynamicOps<K> op){
		return new Codec<K>(){
			@Override
			public <T> DataResult<T> encode(K input, DynamicOps<T> ops, T prefix) {
				return DataResult.success(op.convertTo(ops, input));
			}

			@Override
			public <T> DataResult<Pair<K, T>> decode(DynamicOps<T> ops, T input) {
				return DataResult.success(Pair.of(ops.convertTo(op, input), input));
			}
			
		};
	}
	private static final Function<DynamicOps<?>, Codec<?>> schCodec=cached(SerializeUtil::scCodec);
	public static <I> Codec<I> convertSchema(DynamicOps<I> op){
		return (Codec<I>) schCodec.apply(op);
	}
	public static final Function<Registry<?>, Codec<?>> regCodec = cached(IntOrIdCodec::new);
	public static final Codec<ItemStack> ITEMSTACK_CODEC = RecordCodecBuilder.create(t -> 
		t.group(registryCodec(Registry.ITEM).fieldOf("id").forGetter(ItemStack::getItem),
		Codec.INT.fieldOf("Count").forGetter(ItemStack::getCount),
		nullableCodec(CompoundNBT.CODEC,()->new CompoundNBT()).fieldOf("tag").forGetter(ItemStack::getTag)).apply(t, ItemStack::new));
	
	public static final Codec<Ingredient> INGREDIENT_CODEC = new PacketOrSchemaCodec<>(JsonOps.INSTANCE,Ingredient::serialize,Ingredient::deserialize,Ingredient::write,Ingredient::read);
	public static final Codec<IngredientWithSize> INGREDIENT_SIZE_CODEC=new PacketOrSchemaCodec<>(JsonOps.INSTANCE,IngredientWithSize::serialize,IngredientWithSize::deserialize,IngredientWithSize::write,IngredientWithSize::read);
	public static <S> Codec<S> alternativeCodec(Class<? extends S> first,Codec<S> prim,Class<? extends S> second,Codec<S> iferr){
		return new AlternativeCodec<>(Pair.of(first, prim),Pair.of(second, iferr));
	}
	@SafeVarargs
	public static <S> Codec<S> alternativeCodecs(Pair<Class<? extends S>,Codec<? extends S>>... prim){
		return new AlternativeCodec<>(prim);
	}
	public static <K, V> Function<K, V> cached(Function<K, V> func) {
		Map<K, V> map = new HashMap<>();
		return k -> map.computeIfAbsent(k, func);
	}

	public static <K> Codec<K> registryCodec(Registry<K> func) {
		return (Codec<K>) regCodec.apply(func);
	}

	public static <T extends Enum> Codec<T> enumCodec(T[] values){
		Map<String,T> maps=new HashMap<>();
		for(T val:values)
			maps.put(val.name().toLowerCase(), val);
		return new CompressDifferCodec<>(Codec.STRING.xmap(maps::get, v->v.name().toLowerCase()),Codec.BYTE.xmap(o->values[o], v->(byte)v.ordinal()));
	}
	public static <T> List<T> parseJsonElmList(JsonElement elm, Function<JsonElement, T> mapper) {
		if (elm == null)
			return Lists.newArrayList();
		if (elm.isJsonArray())
			return StreamSupport.stream(elm.getAsJsonArray().spliterator(), false).map(mapper)
				.collect(Collectors.toList());
		return Lists.newArrayList(mapper.apply(elm));
	}

	public static <T> List<T> parseJsonList(JsonElement elm, Function<JsonObject, T> mapper) {
		if (elm == null)
			return Lists.newArrayList();
		if (elm.isJsonArray())
			return StreamSupport.stream(elm.getAsJsonArray().spliterator(), false).map(JsonElement::getAsJsonObject)
				.map(mapper).collect(Collectors.toList());
		return Lists.newArrayList(mapper.apply(elm.getAsJsonObject()));
	}

	public static boolean[] readBooleans(PacketBuffer buffer) {
		return readBooleans(buffer.readByte());
	}

	public static boolean[] readBooleans(byte in) {
		boolean[] ret = new boolean[8];
		for (int i = ret.length - 1; i >= 0; i--) {
			ret[i] = (in & 1) != 0;
			in >>= 1;
		}
		return ret;
	}

	public static <T> List<T> readListNullable(PacketBuffer buffer, Function<PacketBuffer, T> func) {
		if (!buffer.readBoolean())
			return null;
		return readList(buffer, func);
	}

	public static <T> List<T> readList(PacketBuffer buffer, Function<PacketBuffer, T> func) {
		int cnt = buffer.readVarInt();
		List<T> nums = new ArrayList<>(cnt);
		for (int i = 0; i < cnt; i++)
			nums.add(func.apply(buffer));
		return nums;
	}

	public static <K, V> Map<K, V> readMap(PacketBuffer buffer, Map<K, V> map, Function<PacketBuffer, K> keyreader, Function<PacketBuffer, V> valuereader) {
		map.clear();
		if (!buffer.readBoolean())
			return map;
		int cnt = buffer.readVarInt();
		for (int i = 0; i < cnt; i++)
			map.put(keyreader.apply(buffer), valuereader.apply(buffer));
		return map;
	}

	public static <K, V> Map<K, V> readEntry(PacketBuffer buffer, Map<K, V> map, BiConsumer<PacketBuffer, BiConsumer<K, V>> reader) {
		map.clear();
		int cnt = buffer.readVarInt();
		for (int i = 0; i < cnt; i++)
			reader.accept(buffer, map::put);
		return map;
	}

	public static <T> Optional<T> readOptional(PacketBuffer buffer, Function<PacketBuffer, T> func) {
		if (buffer.readBoolean())
			return Optional.ofNullable(func.apply(buffer));
		return Optional.empty();
	}

	public static short[] readShortArray(PacketBuffer buffer) {
		if (!buffer.readBoolean())
			return null;
		int cnt = buffer.readVarInt();
		short[] nums = new short[cnt];
		for (int i = 0; i < cnt; i++)
			nums[i] = buffer.readShort();
		return nums;
	}

	public static <V> Map<String, V> readStringMap(PacketBuffer buffer, Map<String, V> map, Function<PacketBuffer, V> valuereader) {
		return readMap(buffer, map, PacketBuffer::readString, valuereader);
	}

	public static JsonElement toJson(ItemStack stack) {
		boolean hasCount = stack.getCount() > 1, hasTag = stack.hasTag();
		if (!hasCount && !hasTag)
			return new JsonPrimitive(RegistryUtils.getRegistryName(stack.getItem()).toString());
		JsonObject jo = new JsonObject();
		jo.addProperty("id", RegistryUtils.getRegistryName(stack.getItem()).toString());
		if (hasCount)
			jo.addProperty("count", stack.getCount());
		if (hasTag)
			jo.addProperty("nbt", stack.getTag().toString());
		return jo;
	}

	public static <T> JsonArray toJsonList(Collection<T> li, Function<T, JsonElement> mapper) {
		JsonArray ja = new JsonArray();
		li.stream().map(mapper).forEach(ja::add);
		return ja;
	}

	public static <T, B> JsonArray toJsonStringList(Collection<T> li, Function<T, B> mapper) {
		JsonArray ja = new JsonArray();
		li.stream().map(mapper).map(B::toString).forEach(ja::add);
		return ja;
	}

	public static <T> ListNBT toNBTList(Collection<T> stacks, Codec<T> codec) {
		ArrayBuilder<Void> arrayBuilder = ArrayBuilder.create();
		stacks.stream().forEach(t -> arrayBuilder.add(encodeOrThrow(codec.encodeStart(NBTDynamicOps.INSTANCE, t))));
		return arrayBuilder.build();
	}

	public static <T> List<T> fromNBTList(ListNBT list, Codec<T> codec) {
		List<T> al = new ArrayList<>();
		for (INBT nbt : list) {
			al.add(decodeOrThrow(codec.decode(NBTDynamicOps.INSTANCE, nbt)));
		}
		return al;
	}

	public static <T> ListNBT toNBTList(Collection<T> stacks, BiConsumer<T, ArrayBuilder<Void>> mapper) {
		ArrayBuilder<Void> arrayBuilder = ArrayBuilder.create();
		stacks.stream().forEach(t -> mapper.accept(t, arrayBuilder));
		return arrayBuilder.build();
	}

	public static <T> CompoundNBT toNBTMap(Collection<T> stacks, BiConsumer<T, CompoundBuilder<Void>> mapper) {
		CompoundBuilder<Void> compoundBuilder = CompoundBuilder.create();
		stacks.stream().forEach(t -> mapper.accept(t, compoundBuilder));
		return compoundBuilder.build();
	}

	/**
	 * Write boolean as a byte into buffer
	 *
	 * @param elms elements to write, 8 elements max
	 */
	public static void writeBooleans(PacketBuffer buffer, boolean... elms) {
		buffer.writeByte(writeBooleans(elms));
	}

	public static byte writeBooleans(boolean... elms) {
		if (elms.length > 8) {
			throw new IllegalArgumentException("count of boolean must not excess 8");
		}
		byte b = 0;
		for (int i = 0; i < 8; i++) {
			boolean bl = elms.length > i && elms[i];
			b <<= 1;
			b |= (byte) (bl ? 1 : 0);

		}
		return b;
	}

	public static final Codec<boolean[]> BOOLEANS = Codec.BYTE.xmap(SerializeUtil::readBooleans, SerializeUtil::writeBooleans);
	public static <S,A,B> RecordCodecBuilder<S, Either<A, B>> either(MapCodec<A> a,MapCodec<B> b,Function<S,A> fa,Function<S,B> fb){
		return new EitherMapCodec<>(a,b).forGetter(o->{
			A va=fa.apply(o);
			if(va!=null)
				return Either.left(va);
			return Either.right(fb.apply(o));
		});
	}
	public static <O,A,B> Function<O,Either<A,B>> leftRight(Function<O,A> a,Function<O,B> b){
		return o->{
		A va=a.apply(o);
		if(va!=null)
			return Either.left(va);
		return Either.right(b.apply(o));
		};
	}
	public static <A,B> MapCodec<Either<A,B>> either(MapCodec<A> a,MapCodec<B> b){
		return new EitherMapCodec<>(a,b);
	}
	
	public static <T> Codec<T[]> array(Codec<T> codec, T[] arr) {
		return Codec.list(codec).xmap(l -> l.toArray(arr), Arrays::asList);
	}

	public static <T> Codec<T> array(Codec<Object> codec, IntFunction<T> arr) {
		return Codec.list(codec).xmap(l -> {
			Object[] obj = l.toArray();
			T ar = arr.apply(obj.length);
			for (int i = 0; i < obj.length; i++)
				Array.set(ar, i, obj[i]);
			return ar;
		}, Arrays::asList);
	}

	public static <T> void writeListNullable(PacketBuffer buffer, Collection<T> elms, BiConsumer<T, PacketBuffer> func) {
		if (elms == null) {
			buffer.writeBoolean(false);
			return;
		}
		buffer.writeBoolean(true);
		writeList(buffer, elms, func);
	}

	public static <T> void writeListNullable2(PacketBuffer buffer, Collection<T> elms, BiConsumer<PacketBuffer, T> func) {
		if (elms == null) {
			buffer.writeBoolean(false);
			return;
		}
		buffer.writeBoolean(true);
		writeList2(buffer, elms, func);
	}

	public static <T> void writeList(PacketBuffer buffer, Collection<T> elms, BiConsumer<T, PacketBuffer> func) {
		buffer.writeVarInt(elms.size());
		elms.forEach(e -> func.accept(e, buffer));
	}

	public static <K, V> void writeEntry(PacketBuffer buffer, Map<K, V> elms, BiConsumer<Map.Entry<K, V>, PacketBuffer> func) {
		buffer.writeVarInt(elms.size());
		elms.entrySet().forEach(e -> func.accept(e, buffer));
	}

	public static <T> void writeList2(PacketBuffer buffer, Collection<T> elms, BiConsumer<PacketBuffer, T> func) {
		buffer.writeVarInt(elms.size());
		elms.forEach(e -> func.accept(buffer, e));
	}

	public static <K, V> void writeMap(PacketBuffer buffer, Map<K, V> elms, BiConsumer<K, PacketBuffer> keywriter, BiConsumer<V, PacketBuffer> valuewriter) {
		writeListNullable(buffer, elms.entrySet(), (p, b) -> {
			keywriter.accept(p.getKey(), b);
			valuewriter.accept(p.getValue(), b);
		});
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public static <T> void writeOptional(PacketBuffer buffer, Optional<T> data, BiConsumer<T, PacketBuffer> func) {
		if (data.isPresent()) {
			buffer.writeBoolean(true);
			func.accept(data.get(), buffer);
			return;
		}
		buffer.writeBoolean(false);
	}

	public static <T> void writeOptional(PacketBuffer buffer, T data, BiConsumer<T, PacketBuffer> func) {
		writeOptional(buffer, Optional.ofNullable(data), func);
	}

	public static <T> void writeOptional2(PacketBuffer buffer, T data, BiConsumer<PacketBuffer, T> func) {
		writeOptional(buffer, data, (a, b) -> func.accept(b, a));
	}

	public static void writeShortArray(PacketBuffer buffer, short[] arr) {
		if (arr == null) {
			buffer.writeBoolean(false);
			return;
		}
		buffer.writeBoolean(true);
		buffer.writeVarInt(arr.length);
		for (short s : arr)
			buffer.writeShort(s);
	}

	public static <V> void writeStringMap(PacketBuffer buffer, Map<String, V> elms, BiConsumer<V, PacketBuffer> valuewriter) {
		writeMap(buffer, elms, (p, b) -> b.writeString(p), valuewriter);
	}

	public static <T> void writeCodec(PacketBuffer pb, Codec<T> codec, T obj) {
		DataResult<Object> ob = codec.encodeStart(DataOps.COMPRESSED, obj);
		Optional<Object> ret = ob.resultOrPartial(t->{throw new EncoderException(t);});
		ObjectWriter.writeObject(pb, ret.get());
	}

	public static <T> T readCodec(PacketBuffer pb, Codec<T> codec) {
		Object readed = ObjectWriter.readObject(pb);
		DataResult<Pair<T, Object>> ob = codec.decode(DataOps.COMPRESSED, readed);
		
		Optional<Pair<T, Object>> ret = ob.resultOrPartial(t->{throw new DecoderException(t);});
		return ret.get().getFirst();
	}

	public static <T> void writeCodecNBT(PacketBuffer pb, Codec<T> codec, T obj) {
		DataResult<INBT> ob = codec.encodeStart(NBTDynamicOps.INSTANCE, obj);
		Optional<INBT> ret = ob.resultOrPartial(EncoderException::new);
		pb.writeCompoundTag((CompoundNBT) ret.get());
	}

	public static <T> T readCodecNBT(PacketBuffer pb, Codec<T> codec) {
		INBT readed = pb.readCompoundTag();
		DataResult<Pair<T, INBT>> ob = codec.decode(NBTDynamicOps.INSTANCE, readed);
		Optional<Pair<T, INBT>> ret = ob.resultOrPartial(DecoderException::new);
		return ret.get().getFirst();
	}

	public static <T> T encodeOrThrow(DataResult<T> result) {
		return result.getOrThrow(false, EncoderException::new);
	}

	public static <T, A> T decodeOrThrow(DataResult<Pair<T, A>> result) {
		return result.getOrThrow(false, DecoderException::new).getFirst();
	}

	private SerializeUtil() {

	}
}
