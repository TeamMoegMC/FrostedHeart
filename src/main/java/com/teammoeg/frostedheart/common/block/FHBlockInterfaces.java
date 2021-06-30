package com.teammoeg.frostedheart.common.block;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import net.minecraft.block.BlockState;
import net.minecraft.state.properties.BlockStateProperties;

public class FHBlockInterfaces {
    public FHBlockInterfaces() {
    }

    public interface IActiveState extends IEBlockInterfaces.BlockstateProvider {
        default boolean getIsActive() {
            BlockState state = this.getState();
            return state.hasProperty(BlockStateProperties.LIT) ? (Boolean) state.get(BlockStateProperties.LIT) : false;
        }

        default void setActive(boolean active) {
            BlockState state = this.getState();
            BlockState newState = (BlockState) state.with(BlockStateProperties.LIT, active);
            this.setState(newState);
        }
    }
}
