package com.teammoeg.frostedheart.town.Farm;

import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import com.teammoeg.frostedheart.content.steamenergy.steamcore.SteamCoreTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.function.BiFunction;

public class FarmBlock extends FHBaseBlock {
    public FarmBlock(String name, Properties blockProps, BiFunction<Block, Item.Properties, Item> createItemBlock) {
        super(name, blockProps, createItemBlock);
    }

    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return FHTileTypes.FARM.get().create();
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        ActionResultType superResult = super.onBlockActivated(state, world, pos, player, hand, hit);
        if (superResult.isSuccessOrConsume() || player.isSneaking())
            return superResult;
        ItemStack item = player.getHeldItem(hand);
        TileEntity te = Utils.getExistingTileEntity(world, pos);
        if (te instanceof FarmBlockTileEntity) {
            return ((FarmBlockTileEntity) te).onClick(player, item);
        }
        return superResult;
    }
}
/*
【管理员】不咕不咕(1905387052) 2024/1/23 17:14:17
可以先判断是否在能量塔加温区域

【管理员】不咕不咕(1905387052) 2024/1/23 17:14:45
然后可以用那个schedule的东西执行

【管理员】不咕不咕(1905387052) 2024/1/23 17:16:08
对应实体实现IScheduledTaskTE接口

【管理员】不咕不咕(1905387052) 2024/1/23 17:16:27
然后在executeTask写上你的判定代码

【管理员】不咕不咕(1905387052) 2024/1/23 17:16:56
实体初始化的时候或者首次tick在SchedulerQueue里面add

【管理员】不咕不咕(1905387052) 2024/1/23 17:17:09
系统就会自动调度了


 */
