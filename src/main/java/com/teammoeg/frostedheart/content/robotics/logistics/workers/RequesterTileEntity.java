package com.teammoeg.frostedheart.content.robotics.logistics.workers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.frostedheart.content.robotics.logistics.FilterSlot;
import com.teammoeg.frostedheart.content.robotics.logistics.ItemChangeListener;
import com.teammoeg.frostedheart.content.robotics.logistics.ItemHandlerListener;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticTask;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

public class RequesterTileEntity extends CBlockEntity implements TaskableLogisticStorage,ItemChangeListener, CTickableBlockEntity {
	private static class NumCount{
		public static final Codec<NumCount> CODEC=RecordCodecBuilder.create(t->t.group(
			CodecUtil.INT_ARRAY_CODEC.fieldOf("slot").forGetter(o->o.slot.stream().mapToInt(n->n).toArray()),
			Codec.INT.fieldOf("count").forGetter(o->o.count)
			).apply(t,NumCount::new));
		List<Integer> slot=new ArrayList<>();
		int count;
		public NumCount(int[] list, int count) {
			super();
			IntStream.of(list).forEach(slot::add);
			this.count = count;
		}
		public NumCount() {}
	}
	ItemStackHandler container=new ItemStackHandler(27);
	ItemHandlerListener handler=new ItemHandlerListener(container,this);
	public static final int MAX_FILTER_SLOT=5;
	FilterSlot[] filters=new FilterSlot[MAX_FILTER_SLOT];
	LogisticTask[] tasks=new LogisticTask[MAX_FILTER_SLOT];
	int[] msize=new int[MAX_FILTER_SLOT];
	NumCount[] counts=new NumCount[27];
	public RequesterTileEntity(BlockEntityType<? extends BlockEntity> type,BlockPos pos,BlockState bs) {
		super(type,pos,bs);
		for(int i=0;i<filters.length;i++)
			filters[i]=new FilterSlot();
		for(int i=0;i<counts.length;i++)
			counts[i]=new NumCount();
	}

	@Override
	public ItemStackHandler getInventory() {
		return container;
	}

	@Override
	public boolean isValidFor(ItemStack stack) {
		return false;
	}
	@Override
	public LogisticTask[] getTasks() {
		return tasks;
	}

	@Override
	public void onSlotChange(int slot, ItemStack after) {
		int i=0;
		counts[slot].slot.clear();
		counts[slot].count=after.getCount();
		for(FilterSlot sl:filters) {
			if(!sl.isEmpty()) {
				if(sl.isValidFor(after)) {
					counts[slot].slot.add(i);
					return;
				}
			}
			i++;
		}
	}

	@Override
	public void onSlotClear(int slot) {
		counts[slot].slot.clear();
		counts[slot].count=0;
	}

	@Override
	public void onCountChange(int slot, int before, int after) {
		counts[slot].count=after;
	}

	@Override
	public void readCustomNBT(CompoundTag arg0, boolean arg1) {
		container.deserializeNBT(arg0.getCompound("container"));
	}

	@Override
	public void writeCustomNBT(CompoundTag arg0, boolean arg1) {
		arg0.put("container", container.serializeNBT());
	}

	@Override
	public void tick() {
		int[] sizes=Arrays.copyOf(msize, MAX_FILTER_SLOT);
		for(int i=0;i<filters.length;i++) {
			for(NumCount nc:counts) {
				if(nc.slot.contains(i)) {
					sizes[i]-=nc.count;
				}
			}
		}
		for(int i=0;i<filters.length;i++)
			if(sizes[i]>0)
				tasks[i]=filters[i].createTask(this, sizes[i]);
	}



}
