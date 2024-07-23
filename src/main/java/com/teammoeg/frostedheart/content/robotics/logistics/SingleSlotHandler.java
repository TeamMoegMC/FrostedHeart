package com.teammoeg.frostedheart.content.robotics.logistics;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

public class SingleSlotHandler implements IItemHandler, IItemHandlerModifiable
{
    protected ItemStack stack;

    public SingleSlotHandler(){
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack){
        validateSlotIndex(slot);
        this.stack=stack;
        onContentsChanged(slot);
    }

    @Override
    public int getSlots(){
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot){
        validateSlotIndex(slot);
        return this.stack;
    }
    public int getSlotCount() {
    	return stack.getCount();
    }
    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate){
        if (stack.isEmpty())
            return ItemStack.EMPTY;
        validateSlotIndex(slot);
        int limit = getStackLimit(slot, stack);
        if (!this.stack.isEmpty()){
            limit -= this.stack.getCount();
        }

        if (limit <= 0)
            return stack;

        boolean reachedLimit = stack.getCount() > limit;

        if (!simulate){
            if (this.stack.isEmpty()){
            	this.stack=reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, limit) : stack;
            }else{
            	this.stack.grow(reachedLimit ? limit : stack.getCount());
            }
            onContentsChanged(slot);
        }

        return reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, stack.getCount()- limit) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate){
        if (amount == 0)
            return ItemStack.EMPTY;
        validateSlotIndex(slot);
        if (this.stack.isEmpty())
            return ItemStack.EMPTY;
        int toExtract = Math.min(amount, this.stack.getMaxStackSize());
        if (this.stack.getCount() <= toExtract){
            if (!simulate){
                this.stack=ItemStack.EMPTY;
                onContentsChanged(slot);
                return this.stack;
            }else{
                return this.stack.copy();
            }
        }else{
            if (!simulate){
                this.stack=ItemHandlerHelper.copyStackWithSize(this.stack, this.stack.getCount() - toExtract);
                onContentsChanged(slot);
            }
            return ItemHandlerHelper.copyStackWithSize(this.stack, toExtract);
        }
    }

    @Override
    public int getSlotLimit(int slot){return 64;}

    protected int getStackLimit(int slot, ItemStack stack){return Math.min(getSlotLimit(slot), stack.getMaxStackSize());}

    @Override
    public boolean isItemValid(int slot, ItemStack stack){return true;}

    protected void validateSlotIndex(int slot){
        if (slot != 0)
            throw new RuntimeException("Slot " + slot + " not in valid range - [0,1)");
    }

    protected void onLoad(){

    }

    protected void onContentsChanged(int slot){

    }
}
