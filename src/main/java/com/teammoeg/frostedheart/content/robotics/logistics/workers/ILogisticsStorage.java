package com.teammoeg.frostedheart.content.robotics.logistics.workers;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemStackHandler;

public interface ILogisticsStorage {
	ItemStackHandler getInventory();
	boolean isValidFor(ItemStack stack);
}
