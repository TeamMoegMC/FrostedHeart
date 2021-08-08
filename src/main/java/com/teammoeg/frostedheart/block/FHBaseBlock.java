package com.teammoeg.frostedheart.block;

import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.item.Item;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

import java.util.function.BiFunction;

public class FHBaseBlock extends Block implements IWaterLoggable {
    public final String name;
    protected int lightOpacity;

    public FHBaseBlock(String name, Properties blockProps, BiFunction<Block, Item.Properties, Item> createItemBlock) {
        super(blockProps.variableOpacity());
        this.name = name;
        lightOpacity = 15;

        this.setDefaultState(getInitDefaultState());
        ResourceLocation registryName = createRegistryName();
        setRegistryName(registryName);

        FHContent.registeredFHBlocks.add(this);
        Item item = createItemBlock.apply(this, new Item.Properties().group(FHMain.itemGroup));
        if (item != null) {
            item.setRegistryName(registryName);
            FHContent.registeredFHItems.add(item);
        }
    }

    public ResourceLocation createRegistryName() {
        return new ResourceLocation(FHMain.MODID, name);
    }

    protected BlockState getInitDefaultState() {
        BlockState state = this.stateContainer.getBaseState();
        if (state.hasProperty(BlockStateProperties.WATERLOGGED))
            state = state.with(BlockStateProperties.WATERLOGGED, Boolean.FALSE);
        return state;
    }

    public FHBaseBlock setLightOpacity(int opacity) {
        lightOpacity = opacity;
        return this;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getOpacity(BlockState state, IBlockReader worldIn, BlockPos pos) {
        if (state.isOpaqueCube(worldIn, pos))
            return lightOpacity;
        else
            return state.propagatesSkylightDown(worldIn, pos) ? 0 : 1;
    }
}
