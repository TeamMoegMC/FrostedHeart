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

public class CompoundNBTBuilder<P> {
    	private CompoundTag nbt;
        private P parent;
        
        public CompoundNBTBuilder(CompoundTag nbt, P parent) {
			super();
			this.nbt = nbt;
			this.parent = parent;
		}

		public static CompoundNBTBuilder<Void> create() {
            return new CompoundNBTBuilder<>(new CompoundTag(),null);
        }
		public CompoundNBTBuilder<CompoundNBTBuilder<P>> compound(String key) {
			CompoundTag data=new CompoundTag();
			nbt.put(key,data);
            return new CompoundNBTBuilder<>(data,this);
        }
		public ArrayNBTBuilder<CompoundNBTBuilder<P>> array(String key) {
			ListTag data=new ListTag();
			nbt.put(key,data);
            return new ArrayNBTBuilder<>(data,this);
        }
        public P end() {
        	return parent;
        }
        
        public CompoundTag build() {
            return nbt;
        }

		public CompoundNBTBuilder<P> put(String key, Tag value) {
			nbt.put(key, value);
            return this;
		}

		public CompoundNBTBuilder<P> putByte(String key, byte value) {
			nbt.putByte(key, value);
            return this;
		}

		public CompoundNBTBuilder<P> putShort(String key, short value) {
			nbt.putShort(key, value);
            return this;
		}

		public CompoundNBTBuilder<P> putInt(String key, int value) {
			nbt.putInt(key, value);
            return this;
		}

		public CompoundNBTBuilder<P> putLong(String key, long value) {
			nbt.putLong(key, value);
            return this;
		}

		public CompoundNBTBuilder<P> putUniqueId(String key, UUID value) {
			nbt.putUUID(key, value);
            return this;
		}

		public CompoundNBTBuilder<P> putFloat(String key, float value) {
			nbt.putFloat(key, value);
            return this;
		}

		public CompoundNBTBuilder<P> putDouble(String key, double value) {
			nbt.putDouble(key, value);
            return this;
		}

		public CompoundNBTBuilder<P> putString(String key, String value) {
			nbt.putString(key, value);
            return this;
		}

		public CompoundNBTBuilder<P> putByteArray(String key, byte[] value) {
			nbt.putByteArray(key, value);
            return this;
		}

		public CompoundNBTBuilder<P> putIntArray(String key, int[] value) {
			nbt.putIntArray(key, value);
            return this;
		}

		public CompoundNBTBuilder<P> putIntArray(String key, List<Integer> value) {
			nbt.putIntArray(key, value);
            return this;
		}

		public CompoundNBTBuilder<P> putLongArray(String key, long[] value) {
			nbt.putLongArray(key, value);
            return this;
		}

		public CompoundNBTBuilder<P> putLongArray(String key, List<Long> value) {
			nbt.putLongArray(key, value);
            return this;
		}

		public CompoundNBTBuilder<P> putBoolean(String key, boolean value) {
			nbt.putBoolean(key, value);
            return this;
		}
    }
