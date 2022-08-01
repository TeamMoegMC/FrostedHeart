package com.teammoeg.frostedheart.mixin.create;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.gantry.GantryContraption;
import com.simibubi.create.content.contraptions.relays.advanced.GantryShaftTileEntity;
import com.teammoeg.frostedheart.util.ContraptionCostUtils;
import com.teammoeg.frostedheart.util.IGantryShaft;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GantryShaftTileEntity.class)
public abstract class MixinGantryShaftTileEntity extends KineticTileEntity implements ITickableTileEntity, IGantryShaft {
    @Override
    public void setEntity(AbstractContraptionEntity comp) {
        currentComp = comp;
    }

    public MixinGantryShaftTileEntity(TileEntityType<?> typeIn) {
        super(typeIn);
    }

    public AbstractContraptionEntity currentComp;

    @Override
    public float calculateStressApplied() {
        if (currentComp != null && currentComp.isAlive()) {
            //float impact = currentComp.getContraption().getBlocks().size()*4;
            Direction facing = ((GantryContraption) currentComp.getContraption()).getFacing();
            Vector3d currentPosition = currentComp.getAnchorVec().add(.5, .5, .5);
            BlockPos gantryShaftPos = new BlockPos(currentPosition).offset(facing.getOpposite());
            if (gantryShaftPos.equals(this.pos)) {
                ContraptionCostUtils.setSpeedAndCollect(currentComp, (int) speed);
                this.lastStressApplied = ContraptionCostUtils.getCost(currentComp) + 0.5F;
                return lastStressApplied;
            }
        }
        currentComp = null;
        return 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!world.isRemote && super.hasNetwork() && currentComp != null) {
            this.getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
        }
    }
}
