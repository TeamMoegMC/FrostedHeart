package com.teammoeg.frostedheart.mixin.minecraft;

import net.minecraft.tileentity.AbstractFurnaceTileEntity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceTileEntity.class)
public abstract class MixinAbstractFurnaceTileEntity extends LockableTileEntity implements ITickableTileEntity {

    protected MixinAbstractFurnaceTileEntity(TileEntityType<?> typeIn) {
        super(typeIn);
    }

    /**
     * @author khjxiaogu
     * @reason no more furnace.
     */
    @Inject(at = @At("HEAD"), cancellable = true, method = "tick")
    public void NoTick(CallbackInfo cbi) {
        cbi.cancel();
    }

    ;
}
