package com.teammoeg.frostedheart.content.utility.snowsack;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraftforge.items.ItemStackHandler;

public class SnowSackSlot extends Slot {
    private final ItemStack snowSack;
    private final Item slotItem;
    private final SnowSackMenu menu;

    public SnowSackSlot(ItemStackHandler itemHandler, ItemStack snowSack, SnowSackMenu menu, Item slotItem, int xPosition, int yPosition) {
        super(new SimpleContainer(2), 0, xPosition, yPosition);
        this.snowSack = snowSack;
        this.slotItem = slotItem;
        this.menu = menu;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // 检查是否是雪相关物品
        int snowPerItem = SnowSackItem.snowPerItem(stack.getItem());
        if(snowPerItem == 0){
            //不是雪的不准放
            return false;
        }
        
        //检查雪袋是否已满
        if(snowSack.getItem() instanceof SnowSackItem snowSackItem){
            //袋子不满才可以放
            return SnowSackItem.getSnowAmount(snowSack) <= (snowSackItem.maxSnowAmount() - snowPerItem);
        }
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        // 只有当雪袋中有足够的雪时才能取走物品
        return SnowSackItem.getSnowAmount(snowSack) >= SnowSackItem.snowPerItem(slotItem);
    }

    @Override
    public ItemStack getItem() {
        // 根据槽位索引返回对应的物品
        int snowAmount = SnowSackItem.getSnowAmount(snowSack);
        int maxStackSize = slotItem.getMaxStackSize();
        // 显示可以取出的物品数量
        int itemCount = Math.min(maxStackSize, snowAmount / SnowSackItem.snowPerItem(slotItem));
        return new ItemStack(slotItem, itemCount);
    }

    /**
     * 仅用于{@link SnowSackMenu}中的quickMoveStack方法
     * @param stack
     */
    @Override
    public void set(ItemStack stack) {
        // 对于SnowSackSlot，我们不直接设置物品，而是根据数量变化调整雪量
        int snowAmount = SnowSackItem.getSnowAmount(snowSack);
        int maxStackSize = slotItem.getMaxStackSize();
        
        // 获取原来显示的数量
        int oldCount = Math.min(maxStackSize, snowAmount / SnowSackItem.snowPerItem(slotItem));
        
        // 计算数量差并相应调整雪量
        int countDiff = oldCount - stack.getCount();
        if (countDiff != 0) {
            SnowSackItem.removeSnow(snowSack, countDiff * SnowSackItem.snowPerItem(slotItem));
        }
        
        this.setChanged();
    }

    // 添加setByPlayer方法以处理玩家放置物品的情况
    @Override
    public void setByPlayer(ItemStack stack) {
        // 如果是雪球或雪块，将其转换为雪并存储到雪袋中
        if (SnowSackItem.snowPerItem(stack.getItem()) > 0) {
            int count = stack.getCount();
            int converted = SnowSackItem.convertItemToSnow(snowSack, stack.getItem(), count);
            int remaining = count - (converted / SnowSackItem.snowPerItem(stack.getItem()));
            
            if (remaining > 0) {
                stack.setCount(remaining);
            } else {
                stack = ItemStack.EMPTY;
            }
            
            // 更新菜单中的雪数量显示
            if (menu != null) {
                menu.updateSnowAmount();
            }
        }
        // 如果不是雪相关物品，则使用默认行为
        else {
            super.setByPlayer(stack);
        }
    }

    @Override
    public ItemStack remove(int amount) {
        // 从雪袋中取出雪并转换为物品
        ItemStack result = SnowSackItem.createSnowItems(snowSack, slotItem, amount);
        
        // 更新菜单中的雪数量显示
        if (menu != null) {
            menu.updateSnowAmount();
        }
        
        return result;
    }
    
    // 重写onTake方法，确保取出物品时正确更新雪量
    @Override
    public void onTake(Player player, ItemStack stack) {
        super.onTake(player, stack);
        // 更新菜单中的雪数量显示
        if (menu != null) {
            menu.updateSnowAmount();
        }
    }
    
    @Override
    public void setChanged() {
        super.setChanged();
        // 当槽位发生变化时更新雪数量显示
        if (menu != null) {
            menu.updateSnowAmount();
        }
    }

    @Override
    public ItemStack safeInsert(ItemStack pStack, int pIncrement) {
        if (pStack.isEmpty() || !this.mayPlace(pStack)) {
            return pStack;
        }
        if(this.snowSack.getItem() instanceof SnowSackItem snowSackItem){
            int spaceLeft = snowSackItem.maxSnowAmount() - SnowSackItem.getSnowAmount(snowSack);
            //计算可以放进袋子的最大物品数量
            int itemAmountCanPut = Math.min(pIncrement, Math.min(pStack.getCount(), spaceLeft / SnowSackItem.snowPerItem(pStack.getItem())));
            //将雪放入sack

            int snowAdded = SnowSackItem.addSnow(snowSack, itemAmountCanPut * SnowSackItem.snowPerItem(pStack.getItem()));
            pStack.shrink(snowAdded / SnowSackItem.snowPerItem(pStack.getItem()));
            this.setChanged();
        }
        return pStack;
    }
}