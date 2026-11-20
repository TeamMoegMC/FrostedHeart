package com.teammoeg.frostedheart.content.agriculture.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

/** Open-sky scene above natural terrain, independent of the template's underground origin. */
final class CultivatedTestEnvironment {
    private CultivatedTestEnvironment() {}

    static BlockPos origin(GameTestHelper helper) {
        var level = helper.getLevel();
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        BlockPos corner = helper.absolutePos(BlockPos.ZERO);
        int baseY = level.getMaxBuildHeight() - 48;
        for (int x = 0; x < 20; x++) for (int z = 0; z < 20; z++)
            for (int y = baseY; y < level.getMaxBuildHeight(); y++) {
                BlockPos pos = new BlockPos(corner.getX() + x, y, corner.getZ() + z);
                if (!level.isEmptyBlock(pos)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        return new BlockPos(corner.getX() + 9, baseY, corner.getZ() + 9);
    }
}
