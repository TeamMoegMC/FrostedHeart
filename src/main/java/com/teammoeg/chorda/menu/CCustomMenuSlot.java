package com.teammoeg.chorda.menu;

import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.io.registry.IdRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;
import net.minecraftforge.fluids.FluidStack;
/**
 * CContainerData
 * a utility class for menu data sync and type conversion
 * 
 * */
public class CCustomMenuSlot {
	public static class Encoders{
		public static final IdRegistry<NetworkEncoder<?>> encoders=new IdRegistry<>();
		static final NetworkEncoder<FluidStack> fluidEncoder=encoders.register(new NetworkEncoder<>() {

			@Override
			public FluidStack read(FriendlyByteBuf network) {
				return FluidStack.readFromPacket(network);
			}

			@Override
			public void write(FriendlyByteBuf network, FluidStack data) {
				data.writeToPacket(network);
			}
			
		});
		
		static final NetworkEncoder<List<Component>> textEncoder=encoders.register(codec(Codec.list(CodecUtil.convertSchema(JsonOps.INSTANCE).xmap(Component.Serializer::fromJson, Component.Serializer::toJsonTree))));
		static final NetworkEncoder<BitSet> bitSetEncoder=encoders.register(new NetworkEncoder<>() {

			@Override
			public BitSet read(FriendlyByteBuf network) {
		
				return BitSet.valueOf(network.readByteArray());
			}

			@Override
			public void write(FriendlyByteBuf network, BitSet data) {
				byte[] ba=data.toByteArray();
				network.writeByteArray(ba);
			}
			
		});
		public static <T> NetworkEncoder<T> codec(Codec<T> type){
			return new NetworkEncoder<>(){

				@Override
				public T read(FriendlyByteBuf network) {
					return CodecUtil.readCodec(network, type);
				}

				@Override
				public void write(FriendlyByteBuf network, T data) {
					CodecUtil.writeCodec(network, type, data);
				}
			};
		}
	}
	
	public static interface DataSlotConverter<A> extends IntFunction<A>{
		int apply(A a);
		A getDefault();
		default CDataSlot<A> create(CBaseMenu container) {
			return CCustomMenuSlot.create(container,this);
		}
	}
	public static interface NetworkEncoder<T>{
		public T read(FriendlyByteBuf network);
		public void write(FriendlyByteBuf network, T data);
	};
	public static interface OtherDataSlotEncoder<A>{
		NetworkEncoder<A> getEncoder();
		A copy(A data);
		A getDefault();
		default boolean isSame(A data,A data2) {
			return Objects.equals(data, data2);
		};
		default CDataSlot<A> create(CBaseMenu container) {
			return CCustomMenuSlot.create(container,this);
		}
	}
	public static interface MultipleDataSlotConverter<A>{
		void encode(A a,int[] values);
		A decode(int[] values);
		int getCount();
		A getDefault();
		default boolean isSame(A data,A data2) {
			return Objects.equals(data, data2);
		};
		default CDataSlot<A> create(CBaseMenu container) {
			return CCustomMenuSlot.create(container,this);
		}
	}
	public interface CDataSlot<T>{
		T getValue();
		
		void setValue(T t);
		void bind(Supplier<T> sup);
		void bind(Consumer<T> setter);
		void bind(Supplier<T> sup,Consumer<T> con);
		default Supplier<T> asSupplier(){
			return ()->getValue();
		}
		default <R> Supplier<R> map(Function<T,R> mapper){
			return ()->mapper.apply(getValue());
		}
	}
	private static class SingleDataSlot<T> extends DataSlot implements CDataSlot<T> {
		T value;
		DataSlotConverter<T> conv;
		Supplier<T> getter;
		Consumer<T> setter;
		public SingleDataSlot(DataSlotConverter<T> conv) {
			super();
			this.conv = conv;
			this.value=conv.getDefault();
		}
		@Override
		public int get() {
			return conv.apply(getter!=null?getter.get():value);
		}
		/**
		 * Should only used by vanilla
		 * */
		@Override
		public void set(int pValue) {
			setValue(conv.apply(pValue));
		}
		@Override
		public T getValue() {
			return value;
		}

		@Override
		public void setValue(T t) {
			if(setter!=null) {
				setter.accept(t);
			}
			value=t;
		}
		@Override
		public void bind(Supplier<T> getter) {
			this.getter = getter;
		}
		@Override
		public void bind(Supplier<T> getter, Consumer<T> setter) {
			this.getter=getter;
			this.setter=setter;
		}
		@Override
		public void bind(Consumer<T> setter) {
			this.setter=setter;
		}
	}
	public static interface SyncableDataSlot<T>{
		boolean checkForUpdate();
		void setValue(T t);
		T getValue();
		OtherDataSlotEncoder<T> getConverter();
		
	}
	private static class OtherDataSlot<T> implements CDataSlot<T>,SyncableDataSlot<T>{
		T value;
		T oldValue;
		OtherDataSlotEncoder<T> conv;
		Supplier<T> getter;
		Consumer<T> setter;
		public OtherDataSlot(OtherDataSlotEncoder<T> conv) {
			super();
			this.conv = conv;
			value=conv.getDefault();
			oldValue=conv.getDefault();
		}
		@Override
		public T getValue() {
			return getter!=null?getter.get():value;
		}

		@Override
		public void setValue(T t) {
			
			if(setter!=null) {
				setter.accept(t);
			}
			value=t;
		}
		@Override
		public void bind(Supplier<T> getter) {
			this.getter = getter;
		}
		@Override
		public void bind(Supplier<T> getter, Consumer<T> setter) {
			this.getter=getter;
			this.setter=setter;
		}
		@Override
		public void bind(Consumer<T> setter) {
			this.setter=setter;
		}
		public boolean checkForUpdate() {
			T curval=getValue();
			if(!conv.isSame(oldValue, curval)) {
				oldValue=conv.copy(curval);
				return true;
			}
			return false;
		}
		@Override
		public OtherDataSlotEncoder<T> getConverter() {
			return conv;
		}

	}
	private static class MultiDataSlot<T> implements ContainerData, CDataSlot<T> {
		T value;
		T lastValue;
		int[] values;
		MultipleDataSlotConverter<T> conv;
		Supplier<T> getter;
		Consumer<T> setter;
		
		public MultiDataSlot(MultipleDataSlotConverter<T> conv) {
			super();
			this.conv = conv;
			this.value=conv.getDefault();
			values=new int[conv.getCount()];
		}
		private void updateIfNeeded() {
			if(!conv.isSame(value, lastValue)) {
				conv.encode(value, values);
				lastValue=value;
			}
		}
		@Override
		public int get(int pIndex) {
			if(getter!=null)
				value=getter.get();
			updateIfNeeded();
			return values[pIndex];
		}
		@Override
		public void set(int pIndex, int pValue) {
			values[pIndex]=pValue;
			setValue(conv.decode(values));
			lastValue=value;
		}
		@Override
		public int getCount() {
			return conv.getCount();
		}
		@Override
		public T getValue() {
			return value;
		}

		@Override
		public void setValue(T t) {
			if(setter!=null) {
				setter.accept(t);
			}
			value=t;
		}
		@Override
		public void bind(Supplier<T> getter) {
			this.getter = getter;
		}
		@Override
		public void bind(Supplier<T> getter, Consumer<T> setter) {
			this.getter=getter;
			this.setter=setter;
		}
		@Override
		public void bind(Consumer<T> setter) {
			this.setter=setter;
		}

	}
	public static final DataSlotConverter<Integer> SLOT_INT=new DataSlotConverter<>(){
		@Override
		public Integer apply(int value) {
			return value;
		}

		@Override
		public int apply(Integer t) {
			return t;
		}

		@Override
		public Integer getDefault() {
			return 0;
		}
	};
	public static final DataSlotConverter<Boolean> SLOT_BOOL=new DataSlotConverter<>(){

		@Override
		public Boolean apply(int value) {
			return value!=0;
		}

		@Override
		public int apply(Boolean t) {
			return t?1:0;
		}

		@Override
		public Boolean getDefault() {
			return false;
		}

	};
	public static final DataSlotConverter<Float> SLOT_FIXED=new DataSlotConverter<>(){
		@Override
		public Float apply(int value) {
			return value/100f;
		}
		@Override
		public int apply(Float t) {
			return (int)((t==null?0:t)*100);
		}
		@Override
		public Float getDefault() {
			return 0f;
		}
	};
	public static final DataSlotConverter<Float> SLOT_FLOAT=new DataSlotConverter<>(){
		@Override
		public Float apply(int value) {
			return Float.intBitsToFloat(value);
		}

		@Override
		public int apply(Float a) {
			return Float.floatToRawIntBits(a);
		}

		@Override
		public Float getDefault() {
			return 0f;
		}
	};
	public static final DataSlotConverter<BitSet> SLOT_BITSET32=new DataSlotConverter<>(){
		@Override
		public BitSet apply(int value) {
	        BitSet bits = new BitSet();
	        int index = 0;
	        while (value != 0) {
	            if (value % 2 != 0) {
	                bits.set(index);
	            }
	            ++index;
	            value = value >>> 1;
	        }

	        return bits;
		}

		@Override
		public int apply(BitSet a) {
			int val = 0;
	        int bitval = 1;
	        for (int i = 0; i < a.length(); i++) {
	            if (a.get(i))
	                val += bitval;
	            bitval += bitval;
	        }

	        return val;
		}

		@Override
		public BitSet getDefault() {
			return new BitSet();
		}
	};
	public static final MultipleDataSlotConverter<Long> SLOT_LONG=new MultipleDataSlotConverter<>(){
		@Override
		public void encode(Long a, int[] values) {
			values[0] = (int)((long)a >> 32);
			values[1] = (int)(long)a;
		}
		@Override
		public Long decode(int[] values) {
			return  (long)values[0] << 32 | values[1] & 0xFFFFFFFFL;
		}

		@Override
		public int getCount() {
			return 2;
		}
		@Override
		public Long getDefault() {
			return 0L;
		}
	};
	public static final MultipleDataSlotConverter<BlockPos> SLOT_BLOCKPOS=new MultipleDataSlotConverter<>(){
		@Override
		public void encode(BlockPos a, int[] values) {
			long v=a.asLong();
			values[0] = (int)(v >> 32);
			values[1] = (int)v;
		}
		@Override
		public BlockPos decode(int[] values) {
			return  BlockPos.of((long)values[0] << 32 | values[1] & 0xFFFFFFFFL);
		}

		@Override
		public int getCount() {
			return 2;
		}
		@Override
		public BlockPos getDefault() {
			return BlockPos.ZERO;
		}
	};
	public static final MultipleDataSlotConverter<Vec3i> SLOT_V3I=new MultipleDataSlotConverter<>(){
		@Override
		public void encode(Vec3i a, int[] values) {
			values[0] = a.getX();
			values[1] = a.getY();
			values[2] = a.getZ();
		}
		@Override
		public Vec3i decode(int[] values) {
			return new Vec3i(values[0],values[1],values[2]);
		}

		@Override
		public int getCount() {
			return 3;
		}
		@Override
		public Vec3i getDefault() {
			return Vec3i.ZERO;
		}
	};

	public static final OtherDataSlotEncoder<FluidStack> SLOT_TANK=new OtherDataSlotEncoder<>(){

		@Override
		public FluidStack copy(FluidStack data) {
			return data.copy();
		}

		@Override
		public FluidStack getDefault() {
			return FluidStack.EMPTY;
		}
		@Override
		public boolean isSame(FluidStack data, FluidStack data2) {
			if(data==null) {
				return data==data2;
			}
			if(data2==null)
				return false;
			return data.isFluidStackIdentical(data2);
		}

		@Override
		public NetworkEncoder<FluidStack> getEncoder() {
			return Encoders.fluidEncoder;
		}

	};
	public static final OtherDataSlotEncoder<List<Component>> SLOT_TEXT=new OtherDataSlotEncoder<>(){

		@Override
		public List<Component> copy(List<Component> data) {
			return List.copyOf(data);
		}

		@Override
		public List<Component> getDefault() {
			return List.of();
		}
		@Override
		public boolean isSame(List<Component> data, List<Component> data2) {
			if(data.size()!=data2.size())
				return false;
			for(int i=0;i<data.size();i++) {
				if(!Objects.equals(data.get(i), data2.get(i)))
					return false;
			}
			return true;
		}

		@Override
		public NetworkEncoder<List<Component>> getEncoder() {
			return Encoders.textEncoder;
		}

	};
	public static final OtherDataSlotEncoder<BitSet> SLOT_VAR_BITSET=new OtherDataSlotEncoder<>(){

		@Override
		public BitSet copy(BitSet data) {
			return BitSet.valueOf(data.toLongArray());
		}

		@Override
		public BitSet getDefault() {
			return new BitSet();
		}
		@Override
		public boolean isSame(BitSet data,BitSet data2) {
			return data.equals(data2);
		}

		@Override
		public NetworkEncoder<BitSet> getEncoder() {
			return Encoders.bitSetEncoder;
		}

	};


	public static <T> CDataSlot<T> create(CBaseMenu container, DataSlotConverter<T> type) {
		SingleDataSlot<T> slot=new SingleDataSlot<>(type);
		container.addDataSlot(slot);
		return slot;
		
	}
	public static <T> CDataSlot<T> create(CBaseMenu container, MultipleDataSlotConverter<T> type) {
		MultiDataSlot<T> slot=new MultiDataSlot<>(type);
		container.addDataSlots(slot);
		return slot;
	}
	public static <T> CDataSlot<T> create(CBaseMenu container, OtherDataSlotEncoder<T> type) {
		OtherDataSlot<T> slot=new OtherDataSlot<>(type);
		container.addDataSlot(slot);
		return slot;
	}
	
}
