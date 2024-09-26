package com.teammoeg.frostedheart.content.robotics.logistics.workers;

import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.base.block.FHTickableBlockEntity;
import com.teammoeg.frostedheart.base.blockentity.FHBaseTileEntity;
import com.teammoeg.frostedheart.content.robotics.logistics.ItemChangeListener;
import com.teammoeg.frostedheart.content.robotics.logistics.ItemHandlerListener;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticInternalPushTask;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticTask;
import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

@SuppressWarnings("unused")
public class SupplierTileEntity extends FHBaseTileEntity implements TaskableLogisticStorage,ItemChangeListener,FHTickableBlockEntity {
	ItemStackHandler container=new ItemStackHandler(27);
	ItemHandlerListener handler=new ItemHandlerListener(container,this);
	LogisticTask[] tasks=new LogisticTask[27];

	public SupplierTileEntity(BlockEntityType<? extends BlockEntity> type,BlockPos pos,BlockState bs) {
		super(type,pos,bs);
	}

	@Override
	public ItemStackHandler getInventory() {
		return container;
	}



	@Override
	public LogisticTask[] getTasks() {
		return tasks;
	}

	@Override
	public void readCustomNBT(CompoundTag arg0, boolean arg1) {
		if(!arg1) {
			arg0.put("container", container.serializeNBT());
		}
	}

	@Override
	public void writeCustomNBT(CompoundTag arg0, boolean arg1) {
		container.deserializeNBT(arg0.getCompound("container"));
	}

	@Override
	public void onSlotChange(int slot, ItemStack after) {
		tasks[slot]=new LogisticInternalPushTask(this,slot);
	}

	@Override
	public void onSlotClear(int slot) {
		tasks[slot]=null;
	}

	@Override
	public void onCountChange(int slot, int before, int after) {
	}

	@Override
	public void tick() {
		FHCapabilities.ROBOTIC_LOGISTIC_CHUNK.getCapability(this.level).resolve()
		.map(t->t.getNetworkFor(level,worldPosition))
		.ifPresent(t->t.update(this));
	}

	@Override
	public boolean isValidFor(ItemStack stack) {
		return false;
	}

}
