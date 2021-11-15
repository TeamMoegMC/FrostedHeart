package com.teammoeg.frostedheart.mixin.rankine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cannolicatfish.rankine.entities.DryMortarItemEntity;
import com.cannolicatfish.rankine.init.RankineItems;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
@Mixin(DryMortarItemEntity.class)
public abstract class MixinDryMortarItemEntity extends ItemEntity {
	int ticksRemain=400;
	@Override
	public void writeAdditional(CompoundNBT compound) {
		super.writeAdditional(compound);
		compound.putInt("process",ticksRemain);
	}

	@Override
	public void readAdditional(CompoundNBT compound) {
		super.readAdditional(compound);
		ticksRemain=compound.getInt("process");
	}

	public MixinDryMortarItemEntity(EntityType<? extends ItemEntity> p_i50217_1_, World world) {
		super(p_i50217_1_, world);
	}

	public MixinDryMortarItemEntity(World worldIn, double x, double y, double z, ItemStack stack) {
		super(worldIn, x, y, z, stack);
	}

	public MixinDryMortarItemEntity(World worldIn, double x, double y, double z) {
		super(worldIn, x, y, z);
	}
	/**
	 * @author khjxiaogu
	 * @reason add process time
	 */
    @Overwrite
    public void tick() {

        if (this.inWater) {
        	ticksRemain--;
        	if(ticksRemain<=0) {
	            BlockPos pos = this.getPosition();
	            World worldIn = this.getEntityWorld();
	            if (!worldIn.isRemote && !worldIn.restoringBlockSnapshots) {
	                double d0 = worldIn.rand.nextFloat() * 0.5F + 0.25D;
	                double d1 = worldIn.rand.nextFloat() * 0.5F + 0.25D;
	                double d2 = worldIn.rand.nextFloat() * 0.5F + 0.25D;
	                ItemEntity itementity = new ItemEntity(worldIn, pos.getX() + d0, pos.getY() + d1, pos.getZ() + d2, new ItemStack(RankineItems.MORTAR.get(),this.getItem().getCount()));
	                itementity.setDefaultPickupDelay();
	                worldIn.addEntity(itementity);
	            }
	            this.remove();
        	}
        }
        super.tick();
    }
}
