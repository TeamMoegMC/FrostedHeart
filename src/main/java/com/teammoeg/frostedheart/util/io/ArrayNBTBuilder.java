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

public class ArrayNBTBuilder<P> {
	private ListNBT nbt;
	private P parent;
	
    public ArrayNBTBuilder(ListNBT nbt, P parent) {
		super();
		this.nbt = nbt;
		this.parent = parent;
	}
	public static ArrayNBTBuilder<Void> create() {
        return new ArrayNBTBuilder<>(new ListNBT(),null);
    }
    public ArrayNBTBuilder<ArrayNBTBuilder<P>> array() {
    	ListNBT data=new ListNBT();
    	nbt.add(data);
    	return new ArrayNBTBuilder<>(data,this);
    }
	public CompoundNBTBuilder<ArrayNBTBuilder<P>> compound() {
		CompoundNBT data=new CompoundNBT();
		nbt.add(data);
        return new CompoundNBTBuilder<>(data,this);
    }
    
    public P end() {
    	return parent;
    }
	
	public ArrayNBTBuilder<P> add(INBT value) {
		nbt.add(value);
        return this;
	}

	public ArrayNBTBuilder<P> addByte(byte value) {
		nbt.add(ByteNBT.valueOf(value));
        return this;
	}

	public ArrayNBTBuilder<P> addShort(short value) {
		nbt.add(ShortNBT.valueOf(value));
        return this;
	}

	public ArrayNBTBuilder<P> addInt(int value) {
		nbt.add(IntNBT.valueOf(value));
        return this;
	}

	public ArrayNBTBuilder<P> addLong(long value) {
		nbt.add(LongNBT.valueOf(value));
        return this;
	}

	public ArrayNBTBuilder<P> addUniqueId(UUID value) {
		nbt.add(NBTUtil.func_240626_a_(value));
        return this;
	}

	public ArrayNBTBuilder<P> addFloat(float value) {
		nbt.add(FloatNBT.valueOf(value));
        return this;
	}

	public ArrayNBTBuilder<P> addDouble(double value) {
		nbt.add(DoubleNBT.valueOf(value));
        return this;
	}

	public ArrayNBTBuilder<P> addString(String value) {
		nbt.add(StringNBT.valueOf(value));
        return this;
	}

	public ArrayNBTBuilder<P> addByteArray(byte[] value) {
		nbt.add(new ByteArrayNBT(value));
        return this;
	}

	public ArrayNBTBuilder<P> addIntArray(int[] value) {
		nbt.add(new IntArrayNBT(value));
        return this;
	}

	public ArrayNBTBuilder<P> addIntArray(List<Integer> value) {
		nbt.add(new IntArrayNBT(value));
        return this;
	}

	public ArrayNBTBuilder<P> addLongArray(long[] value) {
		nbt.add(new LongArrayNBT(value));
        return this;
	}

	public ArrayNBTBuilder<P> addLongArray(List<Long> value) {
		nbt.add(new LongArrayNBT(value));
        return this;
	}

	public ArrayNBTBuilder<P> addBoolean(boolean value) {
		nbt.add(ByteNBT.valueOf(value));
        return this;
	}
	public ListNBT build() {
		return nbt;
	}
}
