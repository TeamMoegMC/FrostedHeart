package com.teammoeg.frostedheart.compat;

import com.simibubi.create.foundation.block.BlockStressDefaults;
import net.minecraft.util.ResourceLocation;

public class CreateCompat {
    public static void init() {
        BlockStressDefaults.setDefaultImpact(new ResourceLocation("create","mechanical_harvester"),4.0);
        BlockStressDefaults.setDefaultImpact(new ResourceLocation("create","mechanical_plough"),4.0);
    }
}
