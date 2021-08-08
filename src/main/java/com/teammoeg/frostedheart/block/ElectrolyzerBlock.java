package com.teammoeg.frostedheart.block;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.FHContent;
import com.teammoeg.frostedheart.tileentity.ElectrolyzerTileEntity;
import electrodynamics.api.IWrenchItem;
import electrodynamics.common.block.BlockGenericMachine;
import electrodynamics.prefab.tile.GenericTile;
import electrodynamics.prefab.tile.components.ComponentType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.stats.Stats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import java.util.function.BiFunction;

public class ElectrolyzerBlock extends BlockGenericMachine {
    public final String name;

    public ElectrolyzerBlock(String name, BiFunction<Block, Item.Properties, Item> createItemBlock) {
        this.name = name;
        FHContent.registeredFHBlocks.add(this);
        ResourceLocation registryName = createRegistryName();
        setRegistryName(registryName);
        Item item = createItemBlock.apply(this, new Item.Properties().group(FHMain.itemGroup));
        if (item != null) {
            item.setRegistryName(registryName);
            FHContent.registeredFHItems.add(item);
        }
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new ElectrolyzerTileEntity();
    }

    public ResourceLocation createRegistryName() {
        return new ResourceLocation(FHMain.MODID, name);
    }

    @Override
    @Deprecated
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
                                             BlockRayTraceResult hit) {
        if (worldIn.isRemote) {
            return ActionResultType.SUCCESS;
        } else if (!(player.getHeldItem(handIn).getItem() instanceof IWrenchItem)) {
            TileEntity tile = worldIn.getTileEntity(pos);
            if (tile instanceof GenericTile) {
                GenericTile generic = (GenericTile) tile;
                if (generic.hasComponent(ComponentType.ContainerProvider)) {
                    player.openContainer(generic.getComponent(ComponentType.ContainerProvider));
                }
            }
            player.addStat(Stats.INTERACT_WITH_FURNACE);
            return ActionResultType.CONSUME;
        }
        return ActionResultType.FAIL;
    }
}
