package com.teammoeg.frostedheart.mixin.minecraft.accessors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 读取 NoiseChunk.CacheAllInCell 的 values（当前 cell 的逐方块 finalDensity+beardifier 缓存）。 */
@Mixin(targets = "net.minecraft.world.level.levelgen.NoiseChunk$CacheAllInCell")
public interface CacheAllInCellAccessor {
    @Accessor("values")
    double[] fast$values();
}
