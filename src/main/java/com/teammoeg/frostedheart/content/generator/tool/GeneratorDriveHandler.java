package com.teammoeg.frostedheart.content.generator.tool;

import blusunrize.immersiveengineering.common.blocks.stone.AlloySmelterTileEntity;
import blusunrize.immersiveengineering.common.blocks.stone.BlastFurnaceTileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class GeneratorDriveHandler {
    protected World world;
    public void updateBlastFurnace(BlastFurnaceTileEntity blastFurnaceTileEntity, boolean active) {
        blastFurnaceTileEntity.setActive(active);
    }
    public void updateAlloySmelter(AlloySmelterTileEntity alloySmelterTileEntity, boolean active) {
        alloySmelterTileEntity.setActive(active);
    }
    public void checkExistOreAndUpdate(BlockPos lastSupportPos, NeighborTypeEnum neighborType) {
        switch (neighborType) {
            case BlastFurnaceTileEntity:
                /*
                code here
                Method to update BlastFurnaceTileEntity From lastSupportPos to active
                */
//                world.setBlockState(lastSupportPos, (BlockState)world.getBlockState(lastSupportPos).with(new BlastFurnaceTileEntity.BlastFurnaceState(), true));
                break;
            case AlloySmelterTileEntity:
                /*
                code here
                Method to update AlloySmelterTileEntity From lastSupportPos to active
                */
                break;
            default:
                throw new IllegalArgumentException("Unknown NeighborTypeEnum");
        }
    }
    public GeneratorDriveHandler(World world) {
        this.world = world;
    }
}