package com.teammoeg.frostedheart.mixin.create;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.ClockworkBearingTileEntity;
import com.teammoeg.frostedheart.util.ContraptionCostUtils;
import net.minecraft.tileentity.TileEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClockworkBearingTileEntity.class)
public abstract class MixinClockworkBearingTileEntity extends KineticTileEntity {

    public MixinClockworkBearingTileEntity(TileEntityType<?> typeIn) {
        super(typeIn);
    }

    @Shadow(remap = false)
    protected ControlledContraptionEntity hourHand;
    @Shadow(remap = false)
    protected ControlledContraptionEntity minuteHand;
    private int fh$cooldown;
    @Override
    public float calculateStressApplied() {
        float stress = 1;
        if (hourHand != null&&hourHand.isAlive()) {
            ContraptionCostUtils.setSpeedAndCollect(hourHand, speed / 4F);
            stress += ContraptionCostUtils.getRotationCost(hourHand);
        }
        if (minuteHand != null&&minuteHand.isAlive()) {
            ContraptionCostUtils.setSpeedAndCollect(minuteHand, speed / 4F);
            stress += ContraptionCostUtils.getRotationCost(minuteHand);
        }
        if(stress==1&&lastStressApplied>1) {
        	if(fh$cooldown<=0) {
        		this.lastStressApplied=stress;
        	}else fh$cooldown--;
        }else {
        	fh$cooldown=100;
        	this.lastStressApplied = stress;
        }
        return lastStressApplied;
    }

    @Inject(at = @At("TAIL"), method = "tick")
    public void FH_MICR_tick(CallbackInfo cbi) {
        if ((!world.isRemote) && super.hasNetwork())
            getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
    }
}
