package com.teammoeg.frostedheart.content.climate.player;

import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WardrobeBlockEntity extends RandomizableContainerBlockEntity implements IIEInventory {
    private NonNullList<ItemStack> wardrobeInventory = NonNullList.withSize(13, ItemStack.EMPTY);

    public WardrobeBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.WARDROBE.get(), pos, state);
    }

    @Override
    public NonNullList<ItemStack> getInventory() {
        return wardrobeInventory;
    }

    @Override
    public boolean isStackValid(int i, ItemStack itemStack) {
        return false;
    }

    @Override
    public int getSlotLimit(int i) {
        return 1;
    }

    @Override
    public void doGraphicalUpdates() {

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
        return new WardrobeContainer(
            id,
            playerInventory,
            PlayerTemperatureData.getCapability(player).resolve().get(),
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
        return 13;
    }
}