/*
 * Copyright (c) 2026 TeamMoeg
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

/**
 * NBT列表标签的流式构建器。支持嵌套构建，可链式添加各种类型的NBT元素到列表中。
 * 使用泛型参数P跟踪父级构建器，支持通过{@link #end()}方法返回到父级。
 * <p>
 * Fluent builder for NBT list tags. Supports nested building and chained addition of various
 * NBT element types to a list. Uses generic parameter P to track the parent builder,
 * supporting return to parent via the {@link #end()} method.
 *
 * @param <P> 父级构建器类型，顶层构建器为Void / the parent builder type, Void for top-level builders
 */
public class ArrayNBTBuilder<P> {
	/** 正在构建的NBT列表标签 / The NBT list tag being built */
	private ListTag nbt;
	/** 父级构建器引用 / Reference to the parent builder */
	private P parent;

	/**
	 * 构造一个数组NBT构建器。
	 * <p>
	 * Constructs an array NBT builder.
	 *
	 * @param nbt 要操作的列表标签 / the list tag to operate on
	 * @param parent 父级构建器 / the parent builder
	 */
    public ArrayNBTBuilder(ListTag nbt, P parent) {
		super();
		this.nbt = nbt;
		this.parent = parent;
	}
	/**
	 * 创建一个新的顶层数组NBT构建器。
	 * <p>
	 * Creates a new top-level array NBT builder.
	 *
	 * @return 新的顶层数组NBT构建器 / a new top-level array NBT builder
	 */
	public static ArrayNBTBuilder<Void> create() {
        return new ArrayNBTBuilder<>(new ListTag(),null);
    }
    /**
     * 开始构建一个嵌套的子数组。
     * <p>
     * Begins building a nested sub-array.
     *
     * @return 嵌套的数组构建器 / a nested array builder
     */
    public ArrayNBTBuilder<ArrayNBTBuilder<P>> array() {
    	ListTag data=new ListTag();
    	nbt.add(data);
    	return new ArrayNBTBuilder<>(data,this);
    }
	/**
	 * 开始构建一个嵌套的复合标签。
	 * <p>
	 * Begins building a nested compound tag.
	 *
	 * @return 嵌套的复合标签构建器 / a nested compound tag builder
	 */
	public CompoundNBTBuilder<ArrayNBTBuilder<P>> compound() {
		CompoundTag data=new CompoundTag();
		nbt.add(data);
        return new CompoundNBTBuilder<>(data,this);
    }
    
    /**
     * 结束当前数组构建并返回父级构建器。
     * <p>
     * Ends the current array building and returns to the parent builder.
     *
     * @return 父级构建器 / the parent builder
     */
    public P end() {
    	return parent;
    }
	
	/**
	 * 向列表添加一个NBT标签。
	 * <p>
	 * Adds an NBT tag to the list.
	 *
	 * @param value 要添加的NBT标签 / the NBT tag to add
	 * @return 此构建器，用于链式调用 / this builder for chaining
	 */
	public ArrayNBTBuilder<P> add(Tag value) {
		nbt.add(value);
        return this;
	}

	/**
	 * 向列表添加一个字节值。
	 * <p>
	 * Adds a byte value to the list.
	 *
	 * @param value 要添加的字节值 / the byte value to add
	 * @return 此构建器，用于链式调用 / this builder for chaining
	 */
	public ArrayNBTBuilder<P> addByte(byte value) {
		nbt.add(ByteTag.valueOf(value));
        return this;
	}

	/**
	 * 向列表添加一个短整型值。
	 * <p>
	 * Adds a short value to the list.
	 *
	 * @param value 要添加的短整型值 / the short value to add
	 * @return 此构建器，用于链式调用 / this builder for chaining
	 */
	public ArrayNBTBuilder<P> addShort(short value) {
		nbt.add(ShortTag.valueOf(value));
        return this;
	}

	/**
	 * 向列表添加一个整型值。
	 * <p>
	 * Adds an integer value to the list.
	 *
	 * @param value 要添加的整型值 / the integer value to add
	 * @return 此构建器，用于链式调用 / this builder for chaining
	 */
	public ArrayNBTBuilder<P> addInt(int value) {
		nbt.add(IntTag.valueOf(value));
        return this;
	}

	/**
	 * 向列表添加一个长整型值。
	 * <p>
	 * Adds a long value to the list.
	 *
	 * @param value 要添加的长整型值 / the long value to add
	 * @return 此构建器，用于链式调用 / this builder for chaining
	 */
	public ArrayNBTBuilder<P> addLong(long value) {
		nbt.add(LongTag.valueOf(value));
        return this;
	}

	/**
	 * 向列表添加一个UUID值。
	 * <p>
	 * Adds a UUID value to the list.
	 *
	 * @param value 要添加的UUID / the UUID to add
	 * @return 此构建器，用于链式调用 / this builder for chaining
	 */
	public ArrayNBTBuilder<P> addUniqueId(UUID value) {
		nbt.add(NbtUtils.createUUID(value));
        return this;
	}

	/**
	 * 向列表添加一个浮点值。
	 * <p>
	 * Adds a float value to the list.
	 *
	 * @param value 要添加的浮点值 / the float value to add
	 * @return 此构建器，用于链式调用 / this builder for chaining
	 */
	public ArrayNBTBuilder<P> addFloat(float value) {
		nbt.add(FloatTag.valueOf(value));
        return this;
	}

	/**
	 * 向列表添加一个双精度浮点值。
	 * <p>
	 * Adds a double value to the list.
	 *
	 * @param value 要添加的双精度浮点值 / the double value to add
	 * @return 此构建器，用于链式调用 / this builder for chaining
	 */
	public ArrayNBTBuilder<P> addDouble(double value) {
		nbt.add(DoubleTag.valueOf(value));
        return this;
	}

	/**
	 * 向列表添加一个字符串值。
	 * <p>
	 * Adds a string value to the list.
	 *
	 * @param value 要添加的字符串 / the string value to add
	 * @return 此构建器，用于链式调用 / this builder for chaining
	 */
	public ArrayNBTBuilder<P> addString(String value) {
		nbt.add(StringTag.valueOf(value));
        return this;
	}

	/**
	 * 向列表添加一个字节数组。
	 * <p>
	 * Adds a byte array to the list.
	 *
	 * @param value 要添加的字节数组 / the byte array to add
	 * @return 此构建器，用于链式调用 / this builder for chaining
	 */
	public ArrayNBTBuilder<P> addByteArray(byte[] value) {
		nbt.add(new ByteArrayTag(value));
        return this;
	}

	/**
	 * 向列表添加一个整型数组。
	 * <p>
	 * Adds an integer array to the list.
	 *
	 * @param value 要添加的整型数组 / the integer array to add
	 * @return 此构建器，用于链式调用 / this builder for chaining
	 */
	public ArrayNBTBuilder<P> addIntArray(int[] value) {
		nbt.add(new IntArrayTag(value));
        return this;
	}

	/**
	 * 向列表添加一个整型列表作为整型数组。
	 * <p>
	 * Adds an integer list as an integer array to the list.
	 *
	 * @param value 要添加的整型列表 / the integer list to add
	 * @return 此构建器，用于链式调用 / this builder for chaining
	 */
	public ArrayNBTBuilder<P> addIntArray(List<Integer> value) {
		nbt.add(new IntArrayTag(value));
        return this;
	}

	/**
	 * 向列表添加一个长整型数组。
	 * <p>
	 * Adds a long array to the list.
	 *
	 * @param value 要添加的长整型数组 / the long array to add
	 * @return 此构建器，用于链式调用 / this builder for chaining
	 */
	public ArrayNBTBuilder<P> addLongArray(long[] value) {
		nbt.add(new LongArrayTag(value));
        return this;
	}

	/**
	 * 向列表添加一个长整型列表作为长整型数组。
	 * <p>
	 * Adds a long list as a long array to the list.
	 *
	 * @param value 要添加的长整型列表 / the long list to add
	 * @return 此构建器，用于链式调用 / this builder for chaining
	 */
	public ArrayNBTBuilder<P> addLongArray(List<Long> value) {
		nbt.add(new LongArrayTag(value));
        return this;
	}

	/**
	 * 向列表添加一个布尔值（存储为字节标签）。
	 * <p>
	 * Adds a boolean value to the list (stored as a byte tag).
	 *
	 * @param value 要添加的布尔值 / the boolean value to add
	 * @return 此构建器，用于链式调用 / this builder for chaining
	 */
	public ArrayNBTBuilder<P> addBoolean(boolean value) {
		nbt.add(ByteTag.valueOf(value));
        return this;
	}
	/**
	 * 构建并返回最终的NBT列表标签。
	 * <p>
	 * Builds and returns the final NBT list tag.
	 *
	 * @return 构建完成的列表标签 / the built list tag
	 */
	public ListTag build() {
		return nbt;
	}
}
