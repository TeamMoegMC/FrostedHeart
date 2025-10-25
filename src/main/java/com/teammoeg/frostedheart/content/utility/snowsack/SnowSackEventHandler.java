package com.teammoeg.frostedheart.content.utility.snowsack;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.teammoeg.frostedheart.FHMain;

import net.minecraft.world.entity.player.Player;

@Mod.EventBusSubscriber(modid = FHMain.MODID)
public class SnowSackEventHandler {
    
    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        ItemStack pickedItem = event.getItem().getItem();
        Item itemType = pickedItem.getItem();
        
        // 检查是否是雪球或雪块
        if (itemType == Items.SNOWBALL || itemType == Items.SNOW_BLOCK) {
            // 查找玩家背包中是否有启用了自动拾取的雪袋
            for (int i = 0; i < event.getEntity().getInventory().getContainerSize(); i++) {
                ItemStack stack = event.getEntity().getInventory().getItem(i);
                if (stack.getItem() instanceof SnowSackItem && SnowSackItem.isAutoPickupEnabled(stack)) {
                    // 尝试将物品转换为雪并存储在雪袋中
                    int count = pickedItem.getCount();
                    int converted = SnowSackItem.convertItemToSnow(stack, itemType, count);
                    
                    if (converted > 0) {
                        // 更新拾取的物品数量
                        int remaining = count - (itemType == Items.SNOWBALL ? converted : converted / 4);
                        if (remaining <= 0) {
                            // 全部转换完成，取消拾取事件
                            event.setCanceled(true);
                            event.getItem().discard();
                            syncSnowAmountToOpenMenus(event.getEntity());
                            return;
                        } else {
                            // 部分转换完成，更新物品数量
                            pickedItem.setCount(remaining);
                            event.getItem().setItem(pickedItem);
                        }
                        syncSnowAmountToOpenMenus(event.getEntity());
                    }
                }
            }
        }
    }
    
    /**
     * 同步雪量到打开的雪袋菜单
     */
    private static void syncSnowAmountToOpenMenus(Player player) {
        if (player.level().isClientSide) return;
        // 只需要同步当前玩家的菜单
        if (player.containerMenu instanceof SnowSackMenu snowSackMenu) {
            // 更新雪量显示
            snowSackMenu.updateSnowAmount();
            // 广播变更到客户端
            snowSackMenu.broadcastChanges();

        }
    }
}