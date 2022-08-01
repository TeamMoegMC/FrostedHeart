package com.teammoeg.frostedheart.crash;

import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fml.common.ICrashCallable;

public class ClimateCrash implements ICrashCallable {
    public static ChunkPos Last;

    @Override
    public String call() throws Exception {
        return "last calculating climate chunk: " + Last;
    }

    @Override
    public String getLabel() {
        return "FHClimate";
    }

}
