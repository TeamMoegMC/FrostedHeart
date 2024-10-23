package com.teammoeg.frostedheart.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import com.google.common.base.Objects;
import com.teammoeg.frostedheart.FHBaseContainer;
import com.teammoeg.frostedheart.util.io.registry.IdRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;
import net.minecraftforge.fluids.FluidStack;

public class FHContainerData {
	public static final IdRegistry<OtherDataSlotEncoder<?>> encoders=new IdRegistry<>();
	public static interface DataSlotConverter<A> extends IntFunction<A>{
		int apply(A a);
		A getDefault();
		default FHDataSlot<A> create(FHBaseContainer container) {
			return FHContainerData.create(container,this);
		}
	}
	public static interface OtherDataSlotEncoder<A>{
		A read(FriendlyByteBuf network);
		void write(FriendlyByteBuf network,A data);
		A copy(A data);
		A getDefault();
		default FHDataSlot<A> create(FHBaseContainer container) {
			return FHContainerData.create(container,this);
		}
	}
	public static interface MultipleDataSlotConverter<A>{
		void encode(A a,int[] values);
		A decode(int[] values);
		int getCount();
		A getDefault();
		default FHDataSlot<A> create(FHBaseContainer container) {
			return FHContainerData.create(container,this);
		}
	}
	public interface FHDataSlot<T>{
		T getValue();
		void setValue(T t);
		void bind(Supplier<T> sup);
		void bind(Supplier<T> sup,Consumer<T> con);
		default Supplier<T> asSupplier(){
			return ()->getValue();
		}
		default <R> Supplier<R> map(Function<T,R> mapper){
			return ()->mapper.apply(getValue());
		}
	}
	private static class SingleDataSlot<T> extends DataSlot implements FHDataSlot<T>{
		T value;
		DataSlotConverter<T> conv;
		Supplier<T> getter;
		Consumer<T> setter;
		public SingleDataSlot(DataSlotConverter<T> conv) {
			super();
			this.conv = conv;
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
			value=conv.apply(pValue);
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

	}
	public static interface SyncableDataSlot<T>{
		boolean checkForUpdate();
		void setValue(T t);
		T getValue();
		OtherDataSlotEncoder<T> getConverter();
		
	}
	private static class OtherDataSlot<T> implements FHDataSlot<T>,SyncableDataSlot<T>{
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
		public boolean checkForUpdate() {
			T curval=getValue();
			if(!Objects.equal(oldValue, curval)) {
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
	private static class MultiDataSlot<T> implements ContainerData,FHDataSlot<T>{
		T value;
		T lastValue;
		int[] values;
		MultipleDataSlotConverter<T> conv;
		Supplier<T> getter;
		Consumer<T> setter;
		
		public MultiDataSlot(MultipleDataSlotConverter<T> conv) {
			super();
			this.conv = conv;
			values=new int[conv.getCount()];
		}
		private void updateIfNeeded() {
			if(!Objects.equal(value, lastValue)) {
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
			value=lastValue=conv.decode(values);
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
			return (int)(t*100);
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
		public FluidStack read(FriendlyByteBuf network) {
			return FluidStack.readFromPacket(network);
		}

		@Override
		public void write(FriendlyByteBuf network, FluidStack data) {
			data.writeToPacket(network);
		}

		@Override
		public FluidStack copy(FluidStack data) {
			return data.copy();
		}

		@Override
		public FluidStack getDefault() {
			return FluidStack.EMPTY;
		}

	};
	public static <T> FHDataSlot<T> create(FHBaseContainer container,DataSlotConverter<T> type) {
		SingleDataSlot<T> slot=new SingleDataSlot<>(type);
		container.addDataSlot(slot);
		return slot;
		
	}
	public static <T> FHDataSlot<T> create(FHBaseContainer container,MultipleDataSlotConverter<T> type) {
		MultiDataSlot<T> slot=new MultiDataSlot<>(type);
		container.addDataSlots(slot);
		return slot;
	}
	public static <T> FHDataSlot<T> create(FHBaseContainer container,OtherDataSlotEncoder<T> type) {
		OtherDataSlot<T> slot=new OtherDataSlot<>(type);
		container.addDataSlot(slot);
		return slot;
	}
	
}
