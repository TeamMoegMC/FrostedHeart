package com.teammoeg.frostedheart.util.io.codec;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.nbt.CollectionNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.INBTType;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTypes;
import net.minecraft.util.text.ITextComponent;

public class GeneralNBTList extends CollectionNBT<INBT> implements INBT {
	List<INBT> nbt;
	public GeneralNBTList() {
		super();
		nbt=new ArrayList<>();
	}

	public GeneralNBTList(Collection<? extends INBT> c) {
		nbt=new ArrayList<>(c);
	}

	public GeneralNBTList(int initialCapacity) {
		nbt=new ArrayList<>(initialCapacity);
	}

	public static final INBTType TYPE = new INBTType() {

		@Override
		public INBT readNBT(DataInput input, int depth, NBTSizeTracker accounter) throws IOException {
			int size = input.readInt();
			GeneralNBTList gnl = new GeneralNBTList();
			byte b0;
			for (int i = 0; i < size; i++) {
				accounter.read(8);
				b0 = input.readByte();
				gnl.add(NBTTypes.getGetTypeByID(b0).readNBT(input, depth+1, accounter));
			}
			return gnl;
		}

		@Override
		public String getName() {
			return "GENLIST";
		}

		@Override
		public String getTagName() {
			return "tag_GeneralList";
		}

	};

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeInt(size());
		for (INBT nbt : this)
			nbt.write(output);
	}

	@Override
	public String toString() {
		return super.toString();
	}

	@Override
	public byte getId() {
		return 20;
	}

	@Override
	public INBTType<?> getType() {
		return TYPE;
	}

	@Override
	public INBT copy() {
		return new GeneralNBTList(this);
	}

	@Override
	public ITextComponent toFormattedComponent(String indentation, int indentDepth) {
		return TranslateUtils.str("");
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return this.nbt.size();
	}

	@Override
	public boolean addAll(Collection<? extends INBT> c) {
		return this.nbt.addAll(c);
	}

	@Override
	public INBT set(int p_set_1_, INBT p_set_2_) {
		return this.nbt.set(p_set_1_, p_set_2_);
	}

	@Override
	public void add(int p_add_1_, INBT p_add_2_) {
		this.nbt.add(p_add_1_, p_add_2_);
	}

	@Override
	public INBT remove(int p_remove_1_) {
		return this.nbt.remove(p_remove_1_);
	}

	@Override
	public boolean setNBTByIndex(int index, INBT nbt) {
		this.nbt.set(index, nbt);
		return false;
	}

	@Override
	public boolean addNBTByIndex(int index, INBT nbt) {
		this.nbt.add(index, nbt);
		return false;
	}

	@Override
	public byte getTagType() {
		return 0;
	}

	@Override
	public INBT get(int index) {
		return nbt.get(index);
	}

}
