package com.teammoeg.frostedheart.content.utility.vawt;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_vawt")
@PrefixGameTestTemplate(false)
public final class VAWTNetworkGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 160)
    public static void brokenPlacementAndExhaustionLeaveNoRotation(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos turbine = h.absolutePos(new BlockPos(2, 5, 2)).atY(level.getMaxBuildHeight() - 8);
        BlockPos shaft = turbine.below();
        int range = com.teammoeg.frostedheart.infrastructure.config.FHConfig.SERVER.VAWT.vawtEmptyAreaRange.get();
        for (BlockPos clear : BlockPos.betweenClosed(turbine.offset(-range, 0, -range),
                turbine.offset(range, 0, range).atY(level.getMaxBuildHeight() - 1))) {
            level.setBlockAndUpdate(clear, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
        var block = FHBlocks.FABRIC_VAWT.get();
        level.setBlockAndUpdate(shaft, AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
        level.setBlockAndUpdate(turbine, block.defaultBlockState());
        var be = (VAWTBlockEntity) level.getBlockEntity(turbine);
        ItemStack broken = new ItemStack(block);
        VAWTItem.setWear(broken, block.type.getDurability());
        block.setPlacedBy(level, turbine, level.getBlockState(turbine), null, broken);
        h.assertTrue(be.isDamaged() && be.getGeneratedSpeed() == 0, "broken item must be inert immediately on placement");
        be.lazyTick();
        h.assertTrue(be.getSpeed() == 0, "broken item must not start a network");
        h.runAfterDelay(25, () -> {
            var axle = (KineticBlockEntity) level.getBlockEntity(shaft);
            h.assertTrue(axle.getSpeed() == 0, "shaft must remain stationary after broken placement");
            level.removeBlock(turbine, false);
            level.setBlockAndUpdate(turbine, block.defaultBlockState());
            var healthy = (VAWTBlockEntity) level.getBlockEntity(turbine);
            healthy.lazyTick();
            h.assertTrue(healthy.getGeneratedSpeed() != 0 && healthy.getSpeed() != 0, "healthy turbine must actually generate in this fixture: " + healthy.getReason());
            h.assertTrue(((KineticBlockEntity) level.getBlockEntity(shaft)).getSpeed() != 0, "healthy turbine must drive the shaft");
            healthy.setDamage(block.type.getDurability() - 1);
            healthy.lazyTick();
            h.assertTrue(healthy.isDamaged() && healthy.getGeneratedSpeed() == 0 && healthy.getSpeed() == 0,
                    "exhaustion must stop the turbine during the same update");
            h.runAfterDelay(25, () -> {
                h.assertTrue(((KineticBlockEntity) level.getBlockEntity(shaft)).getSpeed() == 0,
                        "shaft must lose rotation after source exhaustion");
                h.succeed();
            });
        });
    }
}
