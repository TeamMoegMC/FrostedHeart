package com.teammoeg.frostedheart.content.utility.snowsack;

import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

public class SnowSackMenu extends AbstractContainerMenu {
    private final ItemStack snowSack;
    private final DataSlot snowAmount;
    private final DataSlot autoPickupEnabled;

    // 用于客户端构造
    public SnowSackMenu(int windowId, Inventory playerInventory, FriendlyByteBuf data) {
        this(windowId, playerInventory, playerInventory.player.getMainHandItem());
    }

    public SnowSackMenu(int windowId, Inventory playerInventory, ItemStack snowSack) {
        super(FHMenuTypes.SNOW_SACK.get(), windowId);
        this.snowSack = snowSack;
        
        // 添加雪球和雪块槽位（横向排列）
        this.addSlot(new SnowSackSlot(null, snowSack, this, Items.SNOWBALL, 70, 35)); // 雪球槽位
        this.addSlot(new SnowSackSlot(null, snowSack, this, Items.SNOW_BLOCK, 98, 35)); // 雪块槽位
        
        // 添加玩家物品栏槽位
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // 添加玩家快捷栏槽位
        for (int k = 0; k < 9; k++) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
        
        // 创建数据槽用于同步雪的数量和自动拾取设置
        this.snowAmount = DataSlot.standalone();
        this.autoPickupEnabled = DataSlot.standalone();
        this.addDataSlot(this.snowAmount);
        this.addDataSlot(this.autoPickupEnabled);
        
        // 初始化数据
        this.snowAmount.set(SnowSackItem.getSnowAmount(snowSack));
        this.autoPickupEnabled.set(SnowSackItem.isAutoPickupEnabled(snowSack) ? 1 : 0);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        System.out.println("quickMoveStack");
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemStack = itemStack1.copy();

            // 如果点击的是雪球或雪块槽位
            if (index == 0 || index == 1) {
                // 尝试将物品移入玩家物品栏
                ItemStack itemStack2 = itemStack1.copy();
                if (!this.moveItemStackTo(itemStack2, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
                
                // 从雪袋中实际扣除已转移的物品对应的雪量
                int transferredAmount = itemStack1.getCount() - itemStack2.getCount();
                if (transferredAmount > 0) {
                    SnowSackItem.removeSnow(snowSack, transferredAmount * SnowSackItem.snowPerItem(itemStack.getItem()));
                }

                // 确保雪的数量同步更新
                this.snowAmount.set(SnowSackItem.getSnowAmount(snowSack));
                
                // 返回EMPTY以防止重复调用
                return ItemStack.EMPTY;
            } 
            // 如果点击的是玩家物品栏中的槽位
            else if (index >= 2) {
                // 检查是否是雪球或雪块
                int snowPerItem = SnowSackItem.snowPerItem(itemStack1.getItem());
                if (snowPerItem > 0) {
                    // 尝试将雪球或雪块转换为雪并存储
                    int count = itemStack1.getCount();
                    int converted = SnowSackItem.convertItemToSnow(snowSack, itemStack1.getItem(), count);
                    int remaining = count - (converted / snowPerItem);
                    
                    if (remaining > 0) {
                        itemStack1.setCount(remaining);
                    } else {
                        slot.set(ItemStack.EMPTY);
                    }
                    
                    // 更新显示的雪数量
                    this.snowAmount.set(SnowSackItem.getSnowAmount(snowSack));
                    return ItemStack.EMPTY;
                }
                
                // 尝试将物品移入玩家物品栏的其他位置
                if (index < 29) {
                    if (!this.moveItemStackTo(itemStack1, 29, 38, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 38) {
                    if (!this.moveItemStackTo(itemStack1, 2, 29, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (itemStack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemStack1.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemStack1);
        }

        return itemStack;
    }
    
    // 更新雪的数量显示
    public void updateSnowAmount() {
        this.snowAmount.set(SnowSackItem.getSnowAmount(snowSack));
    }
    
    // 获取当前雪的数量
    public int getSnowAmount() {
        return this.snowAmount.get();
    }
    
    // 获取自动拾取设置状态
    public boolean isAutoPickupEnabled() {
        return this.autoPickupEnabled.get() == 1;
    }
    
    // 切换自动拾取设置
    public void toggleAutoPickup() {
        boolean newValue = !isAutoPickupEnabled();
        SnowSackItem.setAutoPickup(snowSack, newValue);
        this.autoPickupEnabled.set(newValue ? 1 : 0);
    }

    @Override
    public void clicked(int pSlotId, int pButton, ClickType pClickType, Player pPlayer) {
        if(pSlotId < 0 || pSlotId >= this.slots.size()){
            super.clicked(pSlotId, pButton, pClickType, pPlayer);
            return;
        }
        if(! (getSlot(pSlotId) instanceof SnowSackSlot)){
            super.clicked(pSlotId, pButton, pClickType, pPlayer);
            return;
        }
        if(pClickType == ClickType.PICKUP){
            doSnowSackSlotPickupClick(pSlotId, pButton, pPlayer);
            return;
        }
        if(pClickType == ClickType.SWAP){
            doSnowSackSlotSwapClick(pSlotId, pButton, pPlayer);
            return;
        }
        super.clicked(pSlotId, pButton, pClickType, pPlayer);
    }

    /**
     * 对AbstractContainerMenu.clicked(pSlotId, pButton, pClickType, pPlayer);进行一些修改
     */
    private void doSnowSackSlotPickupClick(int pSlotId, int pButton, Player pPlayer) {
        if (pButton == 0 || pButton == 1) {
            ClickAction clickaction = pButton == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
            if (pSlotId < 0) {
                return;
            }
            Slot slot7 = this.slots.get(pSlotId);
            ItemStack itemstack9 = slot7.getItem();
            ItemStack itemstack10 = this.getCarried();
            pPlayer.updateTutorialInventoryAction(itemstack10, slot7.getItem(), clickaction);

            if (!net.minecraftforge.common.ForgeHooks.onItemStackedOn(itemstack9, itemstack10, slot7, clickaction, pPlayer,
                    new SlotAccess() {//AbstractContainerMenu的createCarriedSlotAccess()是私有的！我只好把方法内容也复制来了
                public ItemStack get() {
                    return SnowSackMenu.this.getCarried();
                }
                public boolean set(ItemStack p_150452_) {
                    SnowSackMenu.this.setCarried(p_150452_);
                    return true;
                }
            }/*createCarriedSlotAccess()*/))
                if (itemstack9.isEmpty()) {
                    if (!itemstack10.isEmpty()) {
                        int i3 = clickaction == ClickAction.PRIMARY ? itemstack10.getCount() : 1;
                        this.setCarried(slot7.safeInsert(itemstack10, i3));
                    }
                } else if (slot7.mayPickup(pPlayer)) {
                    if (itemstack10.isEmpty()) {
                        int j3 = clickaction == ClickAction.PRIMARY ? itemstack9.getCount() : (itemstack9.getCount() + 1) / 2;
                        Optional<ItemStack> optional1 = slot7.tryRemove(j3, Integer.MAX_VALUE, pPlayer);
                        optional1.ifPresent((p_150421_) -> {
                            this.setCarried(p_150421_);
                            slot7.onTake(pPlayer, p_150421_);
                        });
                    } else if (slot7.mayPlace(itemstack10)) {
                        //不管手上是否与格子里的物品相同，统统尝试变成雪塞进袋子
                        int k3 = clickaction == ClickAction.PRIMARY ? itemstack10.getCount() : 1;
                        this.setCarried(slot7.safeInsert(itemstack10, k3));
                    } else if (ItemStack.isSameItemSameTags(itemstack9, itemstack10)) {
                        Optional<ItemStack> optional = slot7.tryRemove(itemstack9.getCount(), itemstack10.getMaxStackSize() - itemstack10.getCount(), pPlayer);
                        optional.ifPresent((p_150428_) -> {
                            itemstack10.grow(p_150428_.getCount());
                            slot7.onTake(pPlayer, p_150428_);
                        });
                    }
                }

            slot7.setChanged();

        }
    }

    private void doSnowSackSlotSwapClick(int pSlotId, int pButton, Player pPlayer) {
        Inventory inventory = pPlayer.getInventory();
        Slot slot2 = this.slots.get(pSlotId);
        ItemStack itemstack3 = inventory.getItem(pButton);
        ItemStack itemstack6 = slot2.getItem();//SnowSackSlot的物品
        if (!itemstack3.isEmpty() || !itemstack6.isEmpty()) {
            if (itemstack3.isEmpty()) {
                if (slot2.mayPickup(pPlayer)) {
                    inventory.setItem(pButton, itemstack6);
                    slot2.tryRemove(itemstack6.getCount(), slot2.getItem().getCount(), pPlayer);
                    slot2.onTake(pPlayer, itemstack6);
                }
            }
            /*
            我只需要利用swap快速取出物品的功能，其它的不需要
            else if (itemstack6.isEmpty()) {
                if (slot2.mayPlace(itemstack3)) {
                    int i2 = slot2.getMaxStackSize(itemstack3);
                    if (itemstack3.getCount() > i2) {
                        slot2.setByPlayer(itemstack3.split(i2));
                    } else {
                        inventory.setItem(pButton, ItemStack.EMPTY);
                        slot2.setByPlayer(itemstack3);
                    }
                }
            } else if (slot2.mayPickup(pPlayer) && slot2.mayPlace(itemstack3)) {
                int j2 = slot2.getMaxStackSize(itemstack3);
                if (itemstack3.getCount() > j2) {
                    slot2.setByPlayer(itemstack3.split(j2));
                    slot2.onTake(pPlayer, itemstack6);
                    if (!inventory.add(itemstack6)) {
                        pPlayer.drop(itemstack6, true);
                    }
                } else {
                    inventory.setItem(pButton, itemstack6);
                    slot2.setByPlayer(itemstack3);
                    slot2.onTake(pPlayer, itemstack6);
                }
            }
            */
        }
    }
}