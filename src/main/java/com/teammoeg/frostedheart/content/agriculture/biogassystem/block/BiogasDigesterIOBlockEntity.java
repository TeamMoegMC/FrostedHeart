package com.teammoeg.frostedheart.content.agriculture.biogassystem.block;

import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.agriculture.biogassystem.screen.BiogasDigesterIOMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BiogasDigesterIOBlockEntity extends BlockEntity implements Nameable, MenuProvider, WorldlyContainer {
    public BiogasDigesterIOBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.BIOGAS_DIGESTER_IO.get(), pos, state);
        this.propertyDelegate = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> BiogasDigesterIOBlockEntity.this.progress;
                    case 1 -> BiogasDigesterIOBlockEntity.this.maxProgress;
                    case 2 -> BiogasDigesterIOBlockEntity.this.shortGasValue;
                    case 3 -> BiogasDigesterIOBlockEntity.this.checked;
                    case 4 -> BiogasDigesterIOBlockEntity.this.isSplit;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 5;
            }
        };
    }
    private final ItemStackHandler inventory = new ItemStackHandler(12);

    private static final Item DIGESTATE = Items.BONE_MEAL;

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private int progress = 0;
    private int maxProgress = 0;
    private int gasValue = 0;
    private int checked = 0;
    private int counter = 0;
    private int shortGasValue = 0;
    private int isSplit = 0;
    protected final ContainerData propertyDelegate;

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
        if (cap == ForgeCapabilities.ITEM_HANDLER){
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> inventory);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        nbt.put("Inventory", inventory.serializeNBT());
        nbt.putInt("Progress",progress);
        nbt.putInt("MaxProgress", maxProgress);
        nbt.putBoolean("IsCrafting", isCrafting);
        nbt.putInt("TempGasValue", tempGasValue);
        nbt.putInt("Counter", counter);
        super.saveAdditional(nbt);
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);
        inventory.deserializeNBT(nbt.getCompound("Inventory"));
        progress = nbt.getInt("Progress");
        maxProgress = nbt.getInt("MaxProgress");
        isCrafting = nbt.getBoolean("IsCrafting");
        tempGasValue = nbt.getInt("TempGasValue");
        counter = nbt.getInt("Counter");
    }

    @Override
    public int[] getSlotsForFace(@NotNull Direction side) {
        int[] result = new int[inventory.getSlots()];
        for (int i = 0; i < result.length; i++) {
            result[i] = i;
        }
        return result;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, @NotNull ItemStack stack, @Nullable Direction dir) {
        return slot >= 0 && slot <= 8;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, @NotNull ItemStack stack, @NotNull Direction dir) {
        return slot >= 9 && slot <= 11;
    }

    @Override
    public Component getName() {
        return FHBlocks.BIOGAS_DIGESTER_IO.get().getName();
    }

    @Override
    public Component getDisplayName() {
        return FHBlocks.BIOGAS_DIGESTER_IO.get().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new BiogasDigesterIOMenu(syncId, playerInventory,this,this.propertyDelegate);
    }

    private boolean isCrafting = false;
    private int tempGasValue = 0;
    public void tick(Level world, BlockPos pos) {
        if (world.getBlockEntity(pos.below()) instanceof BiogasDigesterControllerBlockEntity entity){
            if (entity.isChecked()){
                checked = 1;
                gasValue = entity.getGasValue();
                maxProgress = entity.getCurrentSize();
                if (gasValue > Short.MAX_VALUE){
                    shortGasValue = gasValue/19;
                    isSplit = 1;
                } else {
                    shortGasValue = gasValue;
                    isSplit = 0;
                }
                if (!isCrafting){
                    for (int i = 0;i<9;i++){
                        Item item = inventory.getStackInSlot(i).getItem().asItem();
                        if (isFood(item)){
                            FoodProperties foodProperties = item.getFoodProperties();
                            tempGasValue = (int) (foodProperties.getNutrition() * 10 + foodProperties.getSaturationModifier() * 200);
                            inventory.extractItem(i,1,false);
                            isCrafting = true;
                            break;
                        }
                    }
                } else {
                    inCreaseProgress();
                    if (progress == maxProgress){
                        entity.addGas(tempGasValue);
                        resetProgress();
                        counter += world.random.nextIntBetweenInclusive(0,4);
                        if (counter >= 32){
                            putItem(world);
                            counter = 0;
                        }
                        tempGasValue = 0;
                        isCrafting = false;
                    }
                }
            }
        } else {
            checked = 0;
            gasValue = 0;
            maxProgress = 0;
            tempGasValue = 0;
            isCrafting = false;
            resetProgress();
        }
    }

    public void drops(){
        SimpleContainer inv = new SimpleContainer(inventory.getSlots());
        for(int i = 0; i < inventory.getSlots(); i++){
            inv.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(this.level,this.worldPosition,inv);
    }

    private void putItem(Level world) {
        boolean hasSpawn = false;
        for (int i = 9;i<12;i++){
            if (inventory.getStackInSlot(i).isEmpty()){
                inventory.setStackInSlot(i, DIGESTATE.getDefaultInstance());
                hasSpawn = true;
                break;
            } else if (inventory.getStackInSlot(i).getItem().equals(DIGESTATE)){
                int count = inventory.getStackInSlot(i).getCount();
                if (count < inventory.getStackInSlot(i).getMaxStackSize()){
                    inventory.setStackInSlot(i,new ItemStack(DIGESTATE,count+1));
                    hasSpawn = true;
                    break;
                }
            }
        }
        if (!hasSpawn){
            BlockPos pos = getBlockPos().above();
            ItemEntity item = new ItemEntity(world,pos.getX(),pos.getY(),pos.getZ(),new ItemStack(DIGESTATE));
            item.setDeltaMovement(0.0,0.5,0.0);
            world.addFreshEntity(item);
        }
    }

    private void resetProgress() {
        progress = 0;
    }

    private void inCreaseProgress() {
        progress++;
    }

    public boolean isFood(Item item){
        return item.isEdible();
    }

    @Override
    public int getContainerSize() {
        return 12;
    }

    @Override
    public boolean isEmpty() {
        boolean empty = true;
        for(int i = 0; i < inventory.getSlots(); i++){
            if (!inventory.getStackInSlot(i).isEmpty()){
                empty = false;
                break;
            }
        }
        return empty;
    }

    @Override
    public ItemStack getItem(int i) {
        return inventory.getStackInSlot(i);
    }

    @Override
    public ItemStack removeItem(int pIndex, int pCount) {
        List<ItemStack> list = new ArrayList<>(inventory.getSlots());
        for(int i = 0; i < inventory.getSlots(); i++){
            list.add(i,inventory.getStackInSlot(i));
        }
        ItemStack removeItem = ContainerHelper.removeItem(list, pIndex, pCount);
        if (!removeItem.isEmpty()) {
            this.setChanged();
        }
        return removeItem;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        ItemStack itemStack = this.inventory.getStackInSlot(i);
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.inventory.setStackInSlot(i, ItemStack.EMPTY);
            return itemStack;
        }
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        inventory.setStackInSlot(i,itemStack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for(int i = 0;i < inventory.getSlots();i++){
            inventory.setStackInSlot(i,ItemStack.EMPTY);
        }
        this.setChanged();
    }
}
