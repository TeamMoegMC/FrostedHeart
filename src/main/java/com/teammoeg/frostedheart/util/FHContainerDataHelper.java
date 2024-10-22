package com.teammoeg.frostedheart.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.world.inventory.DataSlot;

public class FHContainerDataHelper {
	public static class FHDataSlot<T> extends DataSlot{
		Function<Integer,T> convert;
		DataSlot valueSlot=DataSlot.standalone();
		
		public FHDataSlot(Function<Integer, T> convert) {
			super();
			this.convert = convert;
		}
		@Override
		public int get() {
			return valueSlot.get();
		}
		@Override
		public void set(int pValue) {
			valueSlot.set(pValue);
		}
		public T getValue() {
			return convert.apply(get());
		}
		
	}
	public static class IntDataSlot extends DataSlot{
		final Supplier<Integer> get;
		final Consumer<Integer> set;
		
		public IntDataSlot(Supplier<Integer> get, Consumer<Integer> set) {
			super();
			this.get = get;
			this.set = set;
		}
		public IntDataSlot(Supplier<Integer> get) {
			super();
			this.get = get;
			this.set = null;
		}
		@Override
		public int get() {
			return get.get();
		}

		@Override
		public void set(int pValue) {
			if(set!=null)
				set.accept(pValue);
		}
	}
	
	public static class BoolDataSlot extends IntDataSlot{
		public BoolDataSlot(Supplier<Boolean> get, Consumer<Boolean> set) {
			super(()->get.get()?1:0,t-> set.accept(t>0));
		}

		public BoolDataSlot(Supplier<Boolean> get) {
			super(()->get.get()?1:0);
		}
		public boolean getValue() {
			return super.get()>0;
		}
	}
	public static class FixedDataSlot extends IntDataSlot{
		public FixedDataSlot(Supplier<Float> get, Consumer<Float> set) {
			super(()->(int)(get.get()*100),t-> set.accept(t/100f));
		}

		public FixedDataSlot(Supplier<Float> get) {
			super(()->(int)(get.get()*100));
		}
		public float getValue() {
			return super.get()/100f;
		}
	}
	public static class FloatDataSlot extends IntDataSlot{
		public FloatDataSlot(Supplier<Float> get, Consumer<Float> set) {
			super(()->Float.floatToRawIntBits(get.get()),t->set.accept(Float.intBitsToFloat(t)));
		}

		public FloatDataSlot(Supplier<Float> get) {
			super(()->Float.floatToRawIntBits(get.get()));
		}
		public float getValue() {
			return Float.intBitsToFloat(super.get());
		}
	}
	public static IntDataSlot ofInt(Supplier<Integer> get,Consumer<Integer> set) {
		return new IntDataSlot(get,set);
	}
	public static BoolDataSlot ofBool(Supplier<Boolean> get,Consumer<Boolean> set) {
		return new BoolDataSlot(get,set);
	}
	public static FixedDataSlot ofFixed(Supplier<Float> get,Consumer<Float> set) {
		return new FixedDataSlot(get,set);
	}
	public static FloatDataSlot ofFloat(Supplier<Float> get,Consumer<Float> set) {
		return new FloatDataSlot(get,set);
	}
}
