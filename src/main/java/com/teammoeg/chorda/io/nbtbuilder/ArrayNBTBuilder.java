package com.teammoeg.chorda.io.nbtbuilder;

import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;

public class ArrayNBTBuilder<P> {
	private ListTag nbt;
	private P parent;
	
    public ArrayNBTBuilder(ListTag nbt, P parent) {
		super();
		this.nbt = nbt;
		this.parent = parent;
	}
	public static ArrayNBTBuilder<Void> create() {
        return new ArrayNBTBuilder<>(new ListTag(),null);
    }
    public ArrayNBTBuilder<ArrayNBTBuilder<P>> array() {
    	ListTag data=new ListTag();
    	nbt.add(data);
    	return new ArrayNBTBuilder<>(data,this);
    }
	public CompoundNBTBuilder<ArrayNBTBuilder<P>> compound() {
		CompoundTag data=new CompoundTag();
		nbt.add(data);
        return new CompoundNBTBuilder<>(data,this);
    }
    
    public P end() {
    	return parent;
    }
	
	public ArrayNBTBuilder<P> add(Tag value) {
		nbt.add(value);
        return this;
	}

	public ArrayNBTBuilder<P> addByte(byte value) {
		nbt.add(ByteTag.valueOf(value));
        return this;
	}

	public ArrayNBTBuilder<P> addShort(short value) {
		nbt.add(ShortTag.valueOf(value));
        return this;
	}

	public ArrayNBTBuilder<P> addInt(int value) {
		nbt.add(IntTag.valueOf(value));
        return this;
	}

	public ArrayNBTBuilder<P> addLong(long value) {
		nbt.add(LongTag.valueOf(value));
        return this;
	}

	public ArrayNBTBuilder<P> addUniqueId(UUID value) {
		nbt.add(NbtUtils.createUUID(value));
        return this;
	}

	public ArrayNBTBuilder<P> addFloat(float value) {
		nbt.add(FloatTag.valueOf(value));
        return this;
	}

	public ArrayNBTBuilder<P> addDouble(double value) {
		nbt.add(DoubleTag.valueOf(value));
        return this;
	}

	public ArrayNBTBuilder<P> addString(String value) {
		nbt.add(StringTag.valueOf(value));
        return this;
	}

	public ArrayNBTBuilder<P> addByteArray(byte[] value) {
		nbt.add(new ByteArrayTag(value));
        return this;
	}

	public ArrayNBTBuilder<P> addIntArray(int[] value) {
		nbt.add(new IntArrayTag(value));
        return this;
	}

	public ArrayNBTBuilder<P> addIntArray(List<Integer> value) {
		nbt.add(new IntArrayTag(value));
        return this;
	}

	public ArrayNBTBuilder<P> addLongArray(long[] value) {
		nbt.add(new LongArrayTag(value));
        return this;
	}

	public ArrayNBTBuilder<P> addLongArray(List<Long> value) {
		nbt.add(new LongArrayTag(value));
        return this;
	}

	public ArrayNBTBuilder<P> addBoolean(boolean value) {
		nbt.add(ByteTag.valueOf(value));
        return this;
	}
	public ListTag build() {
		return nbt;
	}
}
