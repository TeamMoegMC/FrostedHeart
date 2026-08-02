package com.teammoeg.frostedheart.mixin.minecraft.accessors;

import net.minecraft.util.BitStorage;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Predicate;

@Mixin(Heightmap.class)
public interface HeightmapAccessor {
  @Accessor("data")
  BitStorage getStorage();

  @Accessor("isOpaque")
  Predicate<BlockState> getBlockPredicate();
}
