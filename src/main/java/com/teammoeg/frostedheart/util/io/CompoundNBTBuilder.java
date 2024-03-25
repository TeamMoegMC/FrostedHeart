package com.teammoeg.frostedheart.util.io;

import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;

public class CompoundNBTBuilder<P> {
    	private CompoundNBT nbt;
        private P parent;
        
        public CompoundNBTBuilder(CompoundNBT nbt, P parent) {
			super();
			this.nbt = nbt;
			this.parent = parent;
		}

		public static CompoundNBTBuilder<Void> create() {
            return new CompoundNBTBuilder<>(new CompoundNBT(),null);
        }
		public CompoundNBTBuilder<CompoundNBTBuilder<P>> compound(String key) {
			CompoundNBT data=new CompoundNBT();
			nbt.put(key,data);
            return new CompoundNBTBuilder<>(data,this);
        }
		public ArrayNBTBuilder<CompoundNBTBuilder<P>> array(String key) {
			ListNBT data=new ListNBT();
			nbt.put(key,data);
            return new ArrayNBTBuilder<>(data,this);
        }
        public P end() {
        	return parent;
        }
        
        public CompoundNBT build() {
            return nbt;
        }

		public CompoundNBTBuilder<P> put(String key, INBT value) {
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
			nbt.putUniqueId(key, value);
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
