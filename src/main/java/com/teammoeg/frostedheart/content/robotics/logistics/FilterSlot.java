package com.teammoeg.frostedheart.content.robotics.logistics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticRequestTask;
import com.teammoeg.frostedheart.util.io.CodecUtil;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class FilterSlot {
	public static final Codec<FilterSlot> CODEC=RecordCodecBuilder.create(t->t.group(
		CodecUtil.ITEMSTACK_CODEC.fieldOf("filter").forGetter(o->o.filter),
		Codec.BOOL.fieldOf("strict").forGetter(o->o.strictNBT)).apply(t,FilterSlot::new));
	ItemStack filter=ItemStack.EMPTY;
	boolean strictNBT;
	public FilterSlot() {
	}
	public FilterSlot(ItemStack filter, boolean strictNBT) {
		super();
		this.filter = filter;
		this.strictNBT = strictNBT;
	}
	public boolean isValidFor(ItemStack stack) {
		if(filter.isEmpty())return true;
		return ((!strictNBT)||ItemStack.tagMatches(stack, filter))&&filter.sameItem(stack);
	}
	public boolean isEmpty() {
		return filter.isEmpty();
	}
	public LogisticRequestTask createTask(BlockEntity target,int size) {
		return new LogisticRequestTask(filter, size, strictNBT, target);
	}
}
