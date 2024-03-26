package com.teammoeg.frostedheart.util.io;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.EitherMapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.util.io.codec.AlternativeCodecBuilder;
import com.teammoeg.frostedheart.util.io.codec.BooleansCodec.BooleanCodecBuilder;
import com.teammoeg.frostedheart.util.io.codec.CompressDifferCodec;
import com.teammoeg.frostedheart.util.io.codec.DataOps;
import com.teammoeg.frostedheart.util.io.codec.IntOrIdCodec;
import com.teammoeg.frostedheart.util.io.codec.DefaultValueCodec;
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
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.SimpleRegistry;

public class CodecUtil {

	public static class DispatchNameCodecBuilder<A>{
		Map<Class<? extends A>,String> classes=new LinkedHashMap<>();
		Map<String,Codec<? extends A>> codecs=new LinkedHashMap<>();
		public <T extends A> DispatchNameCodecBuilder<A> type(String type,Class<T> clazz,Codec<T> codec){
			classes.put(clazz, type);
			codecs.put(type, codec);
			return this;
		}
		public <T extends A> DispatchNameCodecBuilder<A> type(Class<T> clazz,Codec<T> codec){
			String type="n"+classes.size();
			classes.put(clazz, type);
			codecs.put(type, codec);
			return this;
		}
		public Codec<A> buildByName(){
			return Codec.STRING.dispatch(o->ImmutableMap.copyOf(classes).get(o.getClass()), ImmutableMap.copyOf(codecs)::get);
		}
		public Codec<A> buildByInt(){
			List<Class<? extends A>> classes=new ArrayList<>();
			List<Codec<? extends A>> codecs=new ArrayList<>();
			for(Entry<Class<? extends A>, String> name:this.classes.entrySet()) {
				classes.add(name.getKey());
				codecs.add(this.codecs.get(name.getValue()));
			}
			return Codec.INT.dispatch(o->ImmutableList.copyOf(classes).indexOf(o.getClass()), ImmutableList.copyOf(codecs)::get);
		}
		public Codec<A> build(){
			return new CompressDifferCodec<>(buildByName(),buildByInt());
		}
	}
	public static final Codec<long[]> LONG_ARRAY_CODEC=new Codec<long[]>() {
	
		@Override
		public <T> DataResult<T> encode(long[] input, DynamicOps<T> ops, T prefix) {
			DataResult<T> dr=DataResult.success(prefix);
			for(long inp:input)
				dr.flatMap(v->ops.mergeToList(v, ops.createLong(inp)));
			return dr;
		}
	
		@Override
		public <T> DataResult<Pair<long[], T>> decode(DynamicOps<T> ops, T input) {
			return ops.getLongStream(input).map(t->Pair.of(t.toArray(), input));
		}
		
	};
	public static final Codec<int[]> INT_ARRAY_CODEC=new Codec<int[]>() {
	
		@Override
		public <T> DataResult<T> encode(int[] input, DynamicOps<T> ops, T prefix) {
			DataResult<T> dr=DataResult.success(prefix);
			for(int inp:input)
				dr.flatMap(v->ops.mergeToList(v, ops.createInt(inp)));
			return dr;
		}
	
		@Override
		public <T> DataResult<Pair<int[], T>> decode(DynamicOps<T> ops, T input) {
			return ops.getIntStream(input).map(t->Pair.of(t.toArray(), input));
		}
		
	};
	public static final Codec<byte[]> BYTE_ARRAY_CODEC=new Codec<byte[]>() {
	
		@Override
		public <T> DataResult<T> encode(byte[] input, DynamicOps<T> ops, T prefix) {
			DataResult<T> dr=DataResult.success(prefix);
			for(byte inp:input)
				dr.flatMap(v->ops.mergeToList(v, ops.createByte(inp)));
			return dr;
		}
	
		@Override
		public <T> DataResult<Pair<byte[], T>> decode(DynamicOps<T> ops, T input) {
			return ops.getByteBuffer(input).map(t->Pair.of(t.array(), input));
		}
		
	};
	public static final Codec<ItemStack> ITEMSTACK_CODEC = RecordCodecBuilder.create(t -> t.group(
			CodecUtil.registryCodec(Registry.ITEM).fieldOf("id").forGetter(ItemStack::getItem),
			Codec.INT.fieldOf("Count").forGetter(ItemStack::getCount),
			CodecUtil.defaultSupply(CompoundNBT.CODEC,()->new CompoundNBT()).fieldOf("tag").forGetter(ItemStack::getTag))
		.apply(t, ItemStack::new));
	public static final Codec<Ingredient> INGREDIENT_CODEC = new PacketOrSchemaCodec<>(JsonOps.INSTANCE,Ingredient::serialize,Ingredient::deserialize,Ingredient::write,Ingredient::read);
	public static final Codec<IngredientWithSize> INGREDIENT_SIZE_CODEC=new PacketOrSchemaCodec<>(JsonOps.INSTANCE,IngredientWithSize::serialize,IngredientWithSize::deserialize,IngredientWithSize::write,IngredientWithSize::read);
	static final Function<DynamicOps<?>, Codec<?>> schCodec=SerializeUtil.cached(CodecUtil::scCodec);
	private static final Function<Registry<?>, Codec<?>> regCodec = SerializeUtil.cached(IntOrIdCodec::new);
	public static final Codec<boolean[]> BOOLEANS = Codec.BYTE.xmap(SerializeUtil::readBooleans, SerializeUtil::writeBooleans);

	public static <A> DefaultValueCodec<A> defaultValue(Codec<A> val, A def) {
		return defaultSupply(val, () -> def);
	}
	public static <A> DefaultValueCodec<A> defaultSupply(Codec<A> val, Supplier<A> def) {
		return new DefaultValueCodec<A>(val, def);
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
		return Codec.list(CodecUtil.pairCodec(nkey, keyCodec, nval, valueCodec)).xmap(pl -> pl.stream().collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)),
			pl -> pl.entrySet().stream().map(ent -> Pair.of(ent.getKey(), ent.getValue())).collect(Collectors.toList()));
	}
	/*
	public static <A,B> Codec<Map<A,B>> toMap(Codec<List<Pair<A,B>>> codec){
		return codec.xmap(l->l.stream().collect(Collectors.toMap(Pair::getFirst, Pair::getSecond,(k1,k2)->k2,LinkedHashMap::new)), l->l.entrySet().stream().map(t->Pair.of(t.getKey(), t.getValue())).collect(Collectors.toList()));
	}*/
	public static <A> Codec<A> createIntCodec(SimpleRegistry<A> registry) {
		return Codec.INT.xmap(registry::getByValue, registry::getId);
	}
	static <K> Codec<K> scCodec(DynamicOps<K> op){
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
	public static <I> Codec<I> convertSchema(DynamicOps<I> op){
		return (Codec<I>) schCodec.apply(op);
	}
	public static <S> AlternativeCodecBuilder<S> alternative(Class<S> type){
		return new AlternativeCodecBuilder<>(type);
	}
	public static <K> Codec<K> registryCodec(Registry<K> func) {
		return (Codec<K>) regCodec.apply(func);
	}
	public static <T extends Enum<T>> Codec<T> enumCodec(Class<T> en){
		Map<String,T> maps=new HashMap<>();
		T[] values=en.getEnumConstants();
		for(T val:values)
			maps.put(val.name().toLowerCase(), val);
		return new CompressDifferCodec<>(Codec.STRING.xmap(maps::get, v->v.name().toLowerCase()),Codec.BYTE.xmap(o->values[o], v->(byte)v.ordinal()));
	}
	public static <O> BooleanCodecBuilder<O> booleans(String flag){
		return new BooleanCodecBuilder<O>(flag);
	}
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
	public static <T> ListNBT toNBTList(Collection<T> stacks, Codec<T> codec) {
		ArrayNBTBuilder<Void> arrayBuilder = ArrayNBTBuilder.create();
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
	public static <A> DispatchNameCodecBuilder<A> dispatch(){
		return new DispatchNameCodecBuilder<A>();
	}
	public static <A> DispatchNameCodecBuilder<A> dispatch(Class<A> clazz){
		return new DispatchNameCodecBuilder<A>();
	}

}
