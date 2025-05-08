package com.teammoeg.frostedheart.content.agriculture.biogassystem.screen;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.agriculture.biogassystem.block.BiogasDigesterControllerBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BiogasDigesterControllerMenu extends AbstractContainerMenu {
    private final ContainerData propertyDelegate;
    private final Level level;
    public final BiogasDigesterControllerBlockEntity blockEntity;
    public BiogasDigesterControllerMenu(int syncId, Inventory inv, FriendlyByteBuf buf){
        this(syncId, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(4));
    }
    public BiogasDigesterControllerMenu(int syncId, Inventory playerInventory,
                                        BlockEntity blockEntity, ContainerData propertyDelegate){
        super(FHMenuTypes.BIOGAS_DIGESTER_CONTROLLER.get(),syncId);
        this.propertyDelegate = propertyDelegate;
        this.blockEntity = (BiogasDigesterControllerBlockEntity) blockEntity;
        this.level = playerInventory.player.level();

        addPlayerHotbar(playerInventory);
        addPlayerInventory(playerInventory);

        addDataSlots(propertyDelegate);
    }


    public int getChecked(){
        return this.propertyDelegate.get(0);
    }

    public int getSize(){
        return this.propertyDelegate.get(1);
    }

    public int getGasValue(){
        int gasValue = this.propertyDelegate.get(2);
        if (this.propertyDelegate.get(3) != 0){
            gasValue = gasValue * 19;
        }
        return gasValue;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot2 = this.slots.get(slot);
        if (slot2.hasItem()) {
            ItemStack itemStack2 = slot2.getItem();
            itemStack = itemStack2.copy();
            if (slot >= 0 && slot < 27) {
                if (!this.moveItemStackTo(itemStack2, 27, 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slot >= 27 && slot < 36 && !this.moveItemStackTo(itemStack2, 0, 27, false)) {
                return ItemStack.EMPTY;
            }
            if (itemStack2.isEmpty()) {
                slot2.set(ItemStack.EMPTY);
            }
            slot2.setChanged();
            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot2.onTake(player, itemStack2);
            this.broadcastChanges();
        }
        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level,blockEntity.getBlockPos()), player,
                FHBlocks.BIOGAS_DIGESTER_CONTROLLER.get());
    }
    private void addPlayerInventory(Inventory playerInventory){
        for (int i = 0; i < 3; ++i){
            for (int l = 0; l < 9; ++l){
                this.addSlot(new Slot(playerInventory, l + i * 9 +9, 8 +l *18, 84 +i * 18));
            }
        }
    }
    private void addPlayerHotbar(Inventory playerInventory){
        for (int i = 0; i < 9; ++i){
            this.addSlot(new Slot (playerInventory, i, 8 + i * 18, 142));
        }
    }

}
