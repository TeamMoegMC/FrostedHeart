package com.teammoeg.frostedheart.content.climate.heatdevice.generator.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class GeneratorDriveHandler {
    protected Level world;
    public void updateBlastFurnace(BlastFurnaceTileEntity blastFurnaceTileEntity, boolean active) {
        blastFurnaceTileEntity.setActive(active);
    }
    public void updateAlloySmelter(AlloySmelterTileEntity alloySmelterTileEntity, boolean active) {
        alloySmelterTileEntity.setActive(active);
    }
    public void checkExistOreAndUpdate(BlockPos lastSupportPos) {

    }
    public GeneratorDriveHandler(Level world) {
        this.world = world;
    }
}