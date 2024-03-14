package com.teammoeg.frostedheart.util.io;

import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import com.mojang.serialization.DataResult;

import net.minecraft.nbt.ByteArrayNBT;
import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.CollectionNBT;
import net.minecraft.nbt.EndNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntArrayNBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.LongArrayNBT;
import net.minecraft.nbt.LongNBT;
import net.minecraft.nbt.NBTDynamicOps;

public class NBTOps extends NBTDynamicOps {
	public static final NBTDynamicOps INSTANCE = NBTDynamicOps.INSTANCE;
	public static final NBTDynamicOps COMPRESSED = new NBTOps();

	public NBTOps() {

	}

	@Override
	public boolean compressMaps() {
		return true;
	}

	private static CollectionNBT<?> func_240602_a_(byte p_240602_0_, byte p_240602_1_) {
		if (func_240603_a_(p_240602_0_, p_240602_1_, (byte) 4)) {
			return new LongArrayNBT(new long[0]);
		} else if (func_240603_a_(p_240602_0_, p_240602_1_, (byte) 1)) {
			return new ByteArrayNBT(new byte[0]);
		} else {
			return (CollectionNBT<?>) (func_240603_a_(p_240602_0_, p_240602_1_, (byte) 3) ? new IntArrayNBT(new int[0]) : new GeneralNBTList());
		}
	}

	private static boolean func_240603_a_(byte p_240603_0_, byte p_240603_1_, byte p_240603_2_) {
		return p_240603_0_ == p_240603_2_ && (p_240603_1_ == p_240603_2_ || p_240603_1_ == 0);
	}

	private static <T extends INBT> void func_240609_a_(CollectionNBT<T> p_240609_0_, INBT p_240609_1_, INBT p_240609_2_) {
		if (p_240609_1_ instanceof CollectionNBT) {
			CollectionNBT<?> collectionnbt = (CollectionNBT) p_240609_1_;
			collectionnbt.forEach((p_240616_1_) -> {
				p_240609_0_.add((T) p_240616_1_);
			});
		}

		p_240609_0_.add((T) p_240609_2_);
	}

	private static <T extends INBT> void func_240608_a_(CollectionNBT<T> p_240608_0_, INBT p_240608_1_, List<INBT> p_240608_2_) {
		if (p_240608_1_ instanceof CollectionNBT) {
			CollectionNBT<?> collectionnbt = (CollectionNBT) p_240608_1_;
			collectionnbt.forEach((p_240614_1_) -> {
				p_240608_0_.add((T) p_240614_1_);
			});
		}

		p_240608_2_.forEach((p_240607_1_) -> {
			p_240608_0_.add((T) p_240607_1_);
		});
	}

	public DataResult<INBT> mergeToList(INBT p_mergeToList_1_, INBT p_mergeToList_2_) {
		if (!(p_mergeToList_1_ instanceof CollectionNBT) && !(p_mergeToList_1_ instanceof EndNBT)) {
			return DataResult.error("mergeToList called with not a list: " + p_mergeToList_1_, p_mergeToList_1_);
		} else {
			CollectionNBT<?> collectionnbt = new GeneralNBTList();
			func_240609_a_(collectionnbt, p_mergeToList_1_, p_mergeToList_2_);
			return DataResult.success(collectionnbt);
		}
	}

	public DataResult<INBT> mergeToList(INBT p_mergeToList_1_, List<INBT> p_mergeToList_2_) {
		if (!(p_mergeToList_1_ instanceof CollectionNBT) && !(p_mergeToList_1_ instanceof EndNBT)) {
			return DataResult.error("mergeToList called with not a list: " + p_mergeToList_1_, p_mergeToList_1_);
		} else {
			CollectionNBT<?> collectionnbt = func_240602_a_(p_mergeToList_1_ instanceof CollectionNBT ? ((CollectionNBT) p_mergeToList_1_).getTagType() : 0,
				p_mergeToList_2_.stream().findFirst().map(INBT::getId).orElse((byte) 0));
			func_240608_a_(collectionnbt, p_mergeToList_1_, p_mergeToList_2_);
			return DataResult.success(collectionnbt);
		}
	}

	public INBT createList(Stream<INBT> p_createList_1_) {
		PeekingIterator<INBT> peekingiterator = Iterators.peekingIterator(p_createList_1_.iterator());
		if (!peekingiterator.hasNext()) {
			return new GeneralNBTList();
		} else {
			INBT inbt = peekingiterator.peek();
			if (inbt instanceof ByteNBT) {
				List<Byte> list2 = Lists.newArrayList(Iterators.transform(peekingiterator, (p_210815_0_) -> {
					return ((ByteNBT) p_210815_0_).getByte();
				}));
				return new ByteArrayNBT(list2);
			} else if (inbt instanceof IntNBT) {
				List<Integer> list1 = Lists.newArrayList(Iterators.transform(peekingiterator, (p_210818_0_) -> {
					return ((IntNBT) p_210818_0_).getInt();
				}));
				return new IntArrayNBT(list1);
			} else if (inbt instanceof LongNBT) {
				List<Long> list = Lists.newArrayList(Iterators.transform(peekingiterator, (p_210816_0_) -> {
					return ((LongNBT) p_210816_0_).getLong();
				}));
				return new LongArrayNBT(list);
			} else {
				GeneralNBTList listnbt = new GeneralNBTList();

				while (peekingiterator.hasNext()) {
					INBT inbt1 = peekingiterator.next();
					if (!(inbt1 instanceof EndNBT)) {
						listnbt.add(inbt1);
					}
				}

				return listnbt;
			}
		}
	}
}
