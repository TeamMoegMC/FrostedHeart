package com.teammoeg.frostedheart.compat;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.block.BlockStressDefaults;
import net.minecraft.block.Blocks;

public class CreateCompat {
    public static void init() {
        BlockStressDefaults.setDefaultImpact(AllBlocks.MECHANICAL_HARVESTER.getId(),4.0);
        BlockStressDefaults.setDefaultImpact(AllBlocks.MECHANICAL_PLOUGH.getId(),4.0);
        BlockStressDefaults.setDefaultImpact(AllBlocks.ANDESITE_FUNNEL.getId(),4.0);
        BlockStressDefaults.setDefaultImpact(AllBlocks.BRASS_FUNNEL.getId(),4.0);
        BlockStressDefaults.setDefaultImpact(Blocks.DISPENSER.getRegistryName(),4.0);
        
    }
}
