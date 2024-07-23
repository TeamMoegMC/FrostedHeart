/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.decoration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.teammoeg.frostedheart.FHTileTypes;

import blusunrize.immersiveengineering.common.gui.GuiHandler;
import blusunrize.immersiveengineering.common.util.inventory.IEInventoryHandler;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import com.teammoeg.frostedheart.util.TranslateUtils;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import com.teammoeg.frostedheart.base.capability.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

public class RelicChestTileEntity extends LockableLootTileEntity implements IIEInventory {
    protected NonNullList<ItemStack> inventory;
    private LazyOptional<IItemHandler> insertionCap;

    public RelicChestTileEntity() {
        super(FHTileTypes.RELIC_CHEST.get());
        this.inventory = NonNullList.withSize(15, ItemStack.EMPTY);
        this.insertionCap = LazyOptional.of(() -> new IEInventoryHandler(15, this));

    }

    @Override
    protected Container createMenu(int id, PlayerInventory player) {
        return GuiHandler.createContainer(player, this, id);
    }

    @Override
    public void doGraphicalUpdates() {

    }

    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
        return capability == ForgeCapabilities.ITEM_HANDLER ? this.insertionCap.cast() : super.getCapability(capability, facing);
    }

    @Override
    protected ITextComponent getDefaultName() {
        return TranslateUtils.translate("container.relic_chest");
    }

    @Nullable
    @Override
    public NonNullList<ItemStack> getInventory() {
        return inventory;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.inventory;
    }

    @Override
    public int getSizeInventory() {
        return 15;
    }

    @Override
    public int getSlotLimit(int i) {
        return 64;
    }

    @Override
    public boolean isStackValid(int i, ItemStack itemStack) {
        return true;
    }

    @Override
    public void read(BlockState state, CompoundNBT nbt) {
        super.read(state, nbt);
        this.inventory = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
        if (!this.checkLootAndRead(nbt)) {
            ItemStackHelper.loadAllItems(nbt, this.inventory);
        }

    }

    public boolean receiveClientEvent(int id, int type) {
        if (id == 1) {
            BlockState state = this.world.getBlockState(this.pos);
            this.world.notifyBlockUpdate(this.pos, state, state, 3);
            return true;
        }
        return super.receiveClientEvent(id, type);
    }

    @Override
    protected void setItems(NonNullList<ItemStack> itemsIn) {
        this.inventory = itemsIn;
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        super.write(compound);
        if (!this.checkLootAndWrite(compound)) {
            ItemStackHelper.saveAllItems(compound, this.inventory);
        }

        return compound;
    }
}
