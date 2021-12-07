package com.teammoeg.frostedheart.mixin.create;

import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
@Mixin(SmartTileEntity.class)
public abstract class MixinSmartTileEntity extends TileEntity {

	public MixinSmartTileEntity(TileEntityType<?> tileEntityTypeIn) {
		super(tileEntityTypeIn);
	}

	/**
	 * @author khjxiaogu
	 * @reason fixed crash in dev environment.
	 */
	@Overwrite(remap=false)
	public World getWorld() {
		return world;
	}
}
