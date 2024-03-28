package com.teammoeg.frostedheart.content.robotics.logistics;

import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LogisticSlot {
	public static final Codec<LogisticSlot> CODEC=RecordCodecBuilder.create(t->t.group(
		BlockPos.CODEC.fieldOf("pos").forGetter(o->o.pos),
		Codec.INT.fieldOf("slot").forGetter(o->o.slot))
		.apply(t,LogisticSlot::new));
	public int slot;
	public BlockPos pos;
	public World w;
	public LogisticSlot(ILogisticsStorage storage, int slot) {
		super();
		this.slot = slot;
		this.pos = storage.getActualPos();
	}
	public LogisticSlot(BlockPos pos, int slot) {
		super();
		this.pos = pos;
		this.slot = slot;
	}
	public void init(World w) {
		this.w=w;
	}
	public ItemStack getItem() {
		if(w!=null) {
			ILogisticsStorage storage=FHUtils.getExistingTileEntity(w, pos, ILogisticsStorage.class);
			if(storage!=null)
				return storage.getInventory().getStackInSlot(slot);
		}
		return ItemStack.EMPTY;
	}
	public void setItem(ItemStack item) {
		if(w!=null) {
			ILogisticsStorage storage=FHUtils.getExistingTileEntity(w, pos, ILogisticsStorage.class);
			if(storage!=null) {
				storage.getInventory().setStackInSlot(slot, item);
			}
		}
	}
	public boolean hasSize(ItemStack stack) {
		if(w!=null) {
			ILogisticsStorage storage=FHUtils.getExistingTileEntity(w, pos, ILogisticsStorage.class);
			if(storage!=null) {
				return Math.min(stack.getMaxDamage()-getItem().getCount(),storage.getInventory().getSlotLimit(slot)-getItem().getCount())>0;
			}
		}
		return false;
	}
	@Override
	public int hashCode() {
		return Objects.hash(pos, slot);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		LogisticSlot other = (LogisticSlot) obj;
		return Objects.equals(pos, other.pos) && slot == other.slot;
	}
}
