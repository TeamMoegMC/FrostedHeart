package com.teammoeg.frostedheart.util.mixin;

import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface MultiBlockAccess {
    public abstract void callForm(World world, BlockPos pos, Rotation rot, Mirror mirror, Direction sideHit);
}

