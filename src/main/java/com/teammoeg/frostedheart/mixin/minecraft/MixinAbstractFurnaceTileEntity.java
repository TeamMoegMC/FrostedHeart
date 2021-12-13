package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.tileentity.AbstractFurnaceTileEntity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableTileEntity;
import net.minecraft.tileentity.TileEntityType;
@Mixin(AbstractFurnaceTileEntity.class)
public abstract class MixinAbstractFurnaceTileEntity extends LockableTileEntity implements ITickableTileEntity {

	protected MixinAbstractFurnaceTileEntity(TileEntityType<?> typeIn) {
		super(typeIn);
	}
	/**
	 * @author khjxiaogu
	 * @reason no more furnace.
	 */
	@Overwrite
	@Override
	public void tick() {};
}
