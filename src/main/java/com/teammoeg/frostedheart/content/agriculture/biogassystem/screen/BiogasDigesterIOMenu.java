package com.teammoeg.frostedheart.content.agriculture.biogassystem.screen;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.agriculture.biogassystem.block.BiogasDigesterIOBlockEntity;
import com.teammoeg.frostedheart.content.agriculture.biogassystem.screen.utils.OnlyExtractSlot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

import java.util.concurrent.atomic.AtomicBoolean;

public class BiogasDigesterIOMenu extends AbstractContainerMenu {
    private final ContainerData propertyDelegate;
    public final BiogasDigesterIOBlockEntity blockEntity;
    private final Level level;
    public BiogasDigesterIOMenu(int syncId, Inventory inventory, FriendlyByteBuf buf){
        this(syncId, inventory, inventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(5));
    }
    public BiogasDigesterIOMenu(int syncId, Inventory inventory,
                                BlockEntity blockEntity, ContainerData arrayPropertyDelegate){
        super(FHMenuTypes.BIOGAS_DIGESTER_IO.get(),syncId);
        checkContainerSize(inventory,12);
        this.level = inventory.player.level();
        this.propertyDelegate = arrayPropertyDelegate;
        this.blockEntity = ((BiogasDigesterIOBlockEntity) blockEntity);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
            this.addSlot(new SlotItemHandler(iItemHandler,0,8,19));
            this.addSlot(new SlotItemHandler(iItemHandler,1,26,19));
            this.addSlot(new SlotItemHandler(iItemHandler,2,44,19));
            this.addSlot(new SlotItemHandler(iItemHandler,3,8,37));
            this.addSlot(new SlotItemHandler(iItemHandler,4,26,37));
            this.addSlot(new SlotItemHandler(iItemHandler,5,44,37));
            this.addSlot(new SlotItemHandler(iItemHandler,6,8,55));
            this.addSlot(new SlotItemHandler(iItemHandler,7,26,55));
            this.addSlot(new SlotItemHandler(iItemHandler,8,44,55));

            this.addSlot(new OnlyExtractSlot(iItemHandler,9,152,19));
            this.addSlot(new OnlyExtractSlot(iItemHandler,10,152,37));
            this.addSlot(new OnlyExtractSlot(iItemHandler,11,152,55));
        });
        addPlayerHotbar(inventory);
        addPlayerInventory(inventory);

        addDataSlots(arrayPropertyDelegate);
    }
    public boolean isCrafting(){
        return propertyDelegate.get(0) > 0;
    }
    public int getGasValue(){
        int gasValue = this.propertyDelegate.get(2);
        if (this.propertyDelegate.get(4) != 0){
            gasValue = gasValue * 19;
        }
        return gasValue;
    }
    public boolean isChecked(){
        return this.propertyDelegate.get(3) != 0;
    }
    public int getScaledProgress(){
        int progress = this.propertyDelegate.get(0);
        int maxProgress = this.propertyDelegate.get(1); // Max Progress
        int progressArrowSize = 84;// Arrow's Width

        return maxProgress != 0 && progress != 0 ? progress * progressArrowSize / maxProgress : 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        AtomicBoolean r = new AtomicBoolean(true);
        if (slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();
            if (!this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()){
                return ItemStack.EMPTY;
            }
            this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
                if (invSlot < iItemHandler.getSlots()) {
                    if (!moveItemStackTo(originalStack, iItemHandler.getSlots(), this.slots.size(), true)) {
                        r.set(false);
                        return;
                    }
                } else if (!moveItemStackTo(originalStack, 0, iItemHandler.getSlots(), false)) {
                    r.set(false);
                    return;
                }
                if (originalStack.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
            });
        }
        if (r.get()){
            return newStack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level,blockEntity.getBlockPos()), player,
                FHBlocks.BIOGAS_DIGESTER_IO.get());
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
