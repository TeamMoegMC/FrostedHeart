package com.teammoeg.frostedheart.content.climate.heatdevice.generator.tool;

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
    public void checkExistOreAndUpdate(BlockPos lastSupportPos) {

    }
    public GeneratorDriveHandler(World world) {
        this.world = world;
    }
}