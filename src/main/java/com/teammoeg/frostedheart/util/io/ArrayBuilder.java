package com.teammoeg.frostedheart.util.io;

import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.ByteArrayNBT;
import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.DoubleNBT;
import net.minecraft.nbt.FloatNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntArrayNBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.LongArrayNBT;
import net.minecraft.nbt.LongNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.nbt.ShortNBT;
import net.minecraft.nbt.StringNBT;

public class ArrayBuilder<P> {
	private ListNBT nbt;
	private P parent;
	
    public ArrayBuilder(ListNBT nbt, P parent) {
		super();
		this.nbt = nbt;
		this.parent = parent;
	}
	public static ArrayBuilder<Void> create() {
        return new ArrayBuilder<>(new ListNBT(),null);
    }
    public ArrayBuilder<ArrayBuilder<P>> array() {
    	ListNBT data=new ListNBT();
    	nbt.add(data);
    	return new ArrayBuilder<>(data,this);
    }
	public CompoundBuilder<ArrayBuilder<P>> compound() {
		CompoundNBT data=new CompoundNBT();
		nbt.add(data);
        return new CompoundBuilder<>(data,this);
    }
    
    public P end() {
    	return parent;
    }
	
	public ArrayBuilder<P> add(INBT value) {
		nbt.add(value);
        return this;
	}

	public ArrayBuilder<P> addByte(byte value) {
		nbt.add(ByteNBT.valueOf(value));
        return this;
	}

	public ArrayBuilder<P> addShort(short value) {
		nbt.add(ShortNBT.valueOf(value));
        return this;
	}

	public ArrayBuilder<P> addInt(int value) {
		nbt.add(IntNBT.valueOf(value));
        return this;
	}

	public ArrayBuilder<P> addLong(long value) {
		nbt.add(LongNBT.valueOf(value));
        return this;
	}

	public ArrayBuilder<P> addUniqueId(UUID value) {
		nbt.add(NBTUtil.func_240626_a_(value));
        return this;
	}

	public ArrayBuilder<P> addFloat(float value) {
		nbt.add(FloatNBT.valueOf(value));
        return this;
	}

	public ArrayBuilder<P> addDouble(double value) {
		nbt.add(DoubleNBT.valueOf(value));
        return this;
	}

	public ArrayBuilder<P> addString(String value) {
		nbt.add(StringNBT.valueOf(value));
        return this;
	}

	public ArrayBuilder<P> addByteArray(byte[] value) {
		nbt.add(new ByteArrayNBT(value));
        return this;
	}

	public ArrayBuilder<P> addIntArray(int[] value) {
		nbt.add(new IntArrayNBT(value));
        return this;
	}

	public ArrayBuilder<P> addIntArray(List<Integer> value) {
		nbt.add(new IntArrayNBT(value));
        return this;
	}

	public ArrayBuilder<P> addLongArray(long[] value) {
		nbt.add(new LongArrayNBT(value));
        return this;
	}

	public ArrayBuilder<P> addLongArray(List<Long> value) {
		nbt.add(new LongArrayNBT(value));
        return this;
	}

	public ArrayBuilder<P> addBoolean(boolean value) {
		nbt.add(ByteNBT.valueOf(value));
        return this;
	}
	public ListNBT build() {
		return nbt;
	}
}
