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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;

/**
 * NBT复合标签的流式构建器。支持嵌套构建，可链式添加各种类型的键值对到复合标签中。
 * 使用泛型参数P跟踪父级构建器，支持通过{@link #end()}方法返回到父级。
 * <p>
 * Fluent builder for NBT compound tags. Supports nested building and chained addition of various
 * key-value pair types to a compound tag. Uses generic parameter P to track the parent builder,
 * supporting return to parent via the {@link #end()} method.
 *
 * @param <P> 父级构建器类型，顶层构建器为Void / the parent builder type, Void for top-level builders
 */
public class CompoundNBTBuilder<P> {
    	/** 正在构建的NBT复合标签 / The NBT compound tag being built */
    	private CompoundTag nbt;
    	/** 父级构建器引用 / Reference to the parent builder */
        private P parent;

        /**
         * 构造一个复合NBT构建器。
         * <p>
         * Constructs a compound NBT builder.
         *
         * @param nbt 要操作的复合标签 / the compound tag to operate on
         * @param parent 父级构建器 / the parent builder
         */
        public CompoundNBTBuilder(CompoundTag nbt, P parent) {
			super();
			this.nbt = nbt;
			this.parent = parent;
		}

		/**
		 * 创建一个新的顶层复合NBT构建器。
		 * <p>
		 * Creates a new top-level compound NBT builder.
		 *
		 * @return 新的顶层复合NBT构建器 / a new top-level compound NBT builder
		 */
		public static CompoundNBTBuilder<Void> create() {
            return new CompoundNBTBuilder<>(new CompoundTag(),null);
        }
		/**
		 * 开始构建一个嵌套的子复合标签。
		 * <p>
		 * Begins building a nested sub-compound tag.
		 *
		 * @param key 子复合标签的键名 / the key for the sub-compound tag
		 * @return 嵌套的复合标签构建器 / a nested compound tag builder
		 */
		public CompoundNBTBuilder<CompoundNBTBuilder<P>> compound(String key) {
			CompoundTag data=new CompoundTag();
			nbt.put(key,data);
            return new CompoundNBTBuilder<>(data,this);
        }
		/**
		 * 开始构建一个嵌套的列表标签。
		 * <p>
		 * Begins building a nested list tag.
		 *
		 * @param key 列表标签的键名 / the key for the list tag
		 * @return 嵌套的数组构建器 / a nested array builder
		 */
		public ArrayNBTBuilder<CompoundNBTBuilder<P>> array(String key) {
			ListTag data=new ListTag();
			nbt.put(key,data);
            return new ArrayNBTBuilder<>(data,this);
        }
        /**
         * 结束当前复合标签构建并返回父级构建器。
         * <p>
         * Ends the current compound tag building and returns to the parent builder.
         *
         * @return 父级构建器 / the parent builder
         */
        public P end() {
        	return parent;
        }
        
        /**
         * 构建并返回最终的NBT复合标签。
         * <p>
         * Builds and returns the final NBT compound tag.
         *
         * @return 构建完成的复合标签 / the built compound tag
         */
        public CompoundTag build() {
            return nbt;
        }

		/**
		 * 向复合标签添加一个NBT标签。
		 * <p>
		 * Adds an NBT tag to the compound tag.
		 *
		 * @param key 键名 / the key name
		 * @param value 要添加的NBT标签 / the NBT tag to add
		 * @return 此构建器，用于链式调用 / this builder for chaining
		 */
		public CompoundNBTBuilder<P> put(String key, Tag value) {
			nbt.put(key, value);
            return this;
		}

		/**
		 * 向复合标签添加一个字节值。
		 * <p>
		 * Adds a byte value to the compound tag.
		 *
		 * @param key 键名 / the key name
		 * @param value 要添加的字节值 / the byte value to add
		 * @return 此构建器，用于链式调用 / this builder for chaining
		 */
		public CompoundNBTBuilder<P> putByte(String key, byte value) {
			nbt.putByte(key, value);
            return this;
		}

		/**
		 * 向复合标签添加一个短整型值。
		 * <p>
		 * Adds a short value to the compound tag.
		 *
		 * @param key 键名 / the key name
		 * @param value 要添加的短整型值 / the short value to add
		 * @return 此构建器，用于链式调用 / this builder for chaining
		 */
		public CompoundNBTBuilder<P> putShort(String key, short value) {
			nbt.putShort(key, value);
            return this;
		}

		/**
		 * 向复合标签添加一个整型值。
		 * <p>
		 * Adds an integer value to the compound tag.
		 *
		 * @param key 键名 / the key name
		 * @param value 要添加的整型值 / the integer value to add
		 * @return 此构建器，用于链式调用 / this builder for chaining
		 */
		public CompoundNBTBuilder<P> putInt(String key, int value) {
			nbt.putInt(key, value);
            return this;
		}

		/**
		 * 向复合标签添加一个长整型值。
		 * <p>
		 * Adds a long value to the compound tag.
		 *
		 * @param key 键名 / the key name
		 * @param value 要添加的长整型值 / the long value to add
		 * @return 此构建器，用于链式调用 / this builder for chaining
		 */
		public CompoundNBTBuilder<P> putLong(String key, long value) {
			nbt.putLong(key, value);
            return this;
		}

		/**
		 * 向复合标签添加一个UUID值。
		 * <p>
		 * Adds a UUID value to the compound tag.
		 *
		 * @param key 键名 / the key name
		 * @param value 要添加的UUID / the UUID to add
		 * @return 此构建器，用于链式调用 / this builder for chaining
		 */
		public CompoundNBTBuilder<P> putUniqueId(String key, UUID value) {
			nbt.putUUID(key, value);
            return this;
		}

		/**
		 * 向复合标签添加一个浮点值。
		 * <p>
		 * Adds a float value to the compound tag.
		 *
		 * @param key 键名 / the key name
		 * @param value 要添加的浮点值 / the float value to add
		 * @return 此构建器，用于链式调用 / this builder for chaining
		 */
		public CompoundNBTBuilder<P> putFloat(String key, float value) {
			nbt.putFloat(key, value);
            return this;
		}

		/**
		 * 向复合标签添加一个双精度浮点值。
		 * <p>
		 * Adds a double value to the compound tag.
		 *
		 * @param key 键名 / the key name
		 * @param value 要添加的双精度浮点值 / the double value to add
		 * @return 此构建器，用于链式调用 / this builder for chaining
		 */
		public CompoundNBTBuilder<P> putDouble(String key, double value) {
			nbt.putDouble(key, value);
            return this;
		}

		/**
		 * 向复合标签添加一个字符串值。
		 * <p>
		 * Adds a string value to the compound tag.
		 *
		 * @param key 键名 / the key name
		 * @param value 要添加的字符串 / the string value to add
		 * @return 此构建器，用于链式调用 / this builder for chaining
		 */
		public CompoundNBTBuilder<P> putString(String key, String value) {
			nbt.putString(key, value);
            return this;
		}

		/**
		 * 向复合标签添加一个字节数组。
		 * <p>
		 * Adds a byte array to the compound tag.
		 *
		 * @param key 键名 / the key name
		 * @param value 要添加的字节数组 / the byte array to add
		 * @return 此构建器，用于链式调用 / this builder for chaining
		 */
		public CompoundNBTBuilder<P> putByteArray(String key, byte[] value) {
			nbt.putByteArray(key, value);
            return this;
		}

		/**
		 * 向复合标签添加一个整型数组。
		 * <p>
		 * Adds an integer array to the compound tag.
		 *
		 * @param key 键名 / the key name
		 * @param value 要添加的整型数组 / the integer array to add
		 * @return 此构建器，用于链式调用 / this builder for chaining
		 */
		public CompoundNBTBuilder<P> putIntArray(String key, int[] value) {
			nbt.putIntArray(key, value);
            return this;
		}

		/**
		 * 向复合标签添加一个整型列表作为整型数组。
		 * <p>
		 * Adds an integer list as an integer array to the compound tag.
		 *
		 * @param key 键名 / the key name
		 * @param value 要添加的整型列表 / the integer list to add
		 * @return 此构建器，用于链式调用 / this builder for chaining
		 */
		public CompoundNBTBuilder<P> putIntArray(String key, List<Integer> value) {
			nbt.putIntArray(key, value);
            return this;
		}

		/**
		 * 向复合标签添加一个长整型数组。
		 * <p>
		 * Adds a long array to the compound tag.
		 *
		 * @param key 键名 / the key name
		 * @param value 要添加的长整型数组 / the long array to add
		 * @return 此构建器，用于链式调用 / this builder for chaining
		 */
		public CompoundNBTBuilder<P> putLongArray(String key, long[] value) {
			nbt.putLongArray(key, value);
            return this;
		}

		/**
		 * 向复合标签添加一个长整型列表作为长整型数组。
		 * <p>
		 * Adds a long list as a long array to the compound tag.
		 *
		 * @param key 键名 / the key name
		 * @param value 要添加的长整型列表 / the long list to add
		 * @return 此构建器，用于链式调用 / this builder for chaining
		 */
		public CompoundNBTBuilder<P> putLongArray(String key, List<Long> value) {
			nbt.putLongArray(key, value);
            return this;
		}

		/**
		 * 向复合标签添加一个布尔值。
		 * <p>
		 * Adds a boolean value to the compound tag.
		 *
		 * @param key 键名 / the key name
		 * @param value 要添加的布尔值 / the boolean value to add
		 * @return 此构建器，用于链式调用 / this builder for chaining
		 */
		public CompoundNBTBuilder<P> putBoolean(String key, boolean value) {
			nbt.putBoolean(key, value);
            return this;
		}
    }
