/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.climate.block;

import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WardrobeBlockEntity extends RandomizableContainerBlockEntity  {
    private NonNullList<ItemStack> wardrobeInventory = NonNullList.withSize(24, ItemStack.EMPTY);

    public WardrobeBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.WARDROBE.get(), pos, state);
    }



    @Override
    public NonNullList<ItemStack> getItems() {
        return wardrobeInventory;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> itemsIn) {
        this.wardrobeInventory = itemsIn;
    }

    public Component getDisplayName() {
        return Component.translatable("container.wardrobe");
    }

    @Override
    protected Component getDefaultName() {
        return null;
    }

    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new WardrobeMenu(
            id,
            playerInventory,
            this
        );
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return null;
    }

    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, wardrobeInventory);
    }

    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, wardrobeInventory);
    }

    @Override
    public int getContainerSize() {
        return wardrobeInventory.size();
    }
}