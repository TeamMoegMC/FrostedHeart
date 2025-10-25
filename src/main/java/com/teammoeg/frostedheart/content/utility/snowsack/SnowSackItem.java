package com.teammoeg.frostedheart.content.utility.snowsack;

import com.teammoeg.frostedheart.item.FHBaseItem;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Getter
public class SnowSackItem extends FHBaseItem {
    private static final String SNOW_AMOUNT_KEY = "SnowAmount";
    private static final String AUTO_PICKUP_KEY = "AutoPickup";
    public final int maxSnowAmount; // 雪的最大存储量
    private static final Map<Item, Integer> SNOW_PER_ITEM = Map.of(
            Items.SNOWBALL, 1,
            Items.SNOW_BLOCK, 4
    );

    /**
     * @param maxSnowAmount 雪的最大存储量。1点相当于可存储1个雪球。
     */
    public SnowSackItem(int maxSnowAmount, Properties properties) {
        super(properties.stacksTo(1)); // 雪袋只能堆叠到1个
        this.maxSnowAmount = maxSnowAmount;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide) {
            // 打开GUI
            NetworkHooks.openScreen((ServerPlayer) player, new SnowSackMenuProvider(stack), buf -> {
                // 可以在这里传递额外的数据
            });
        }
        
        return InteractionResultHolder.success(stack);
    }

    /**
     * 获取物品中的雪数量
     */
    public static int getSnowAmount(ItemStack stack) {
        if (stack.getItem() instanceof SnowSackItem) {
            CompoundTag tag = stack.getOrCreateTag();
            return tag.getInt(SNOW_AMOUNT_KEY);
        }
        return 0;
    }

    /**
     * 设置物品中的雪数量
     */
    public static void setSnowAmount(ItemStack stack, int amount) {
        if (stack.getItem() instanceof SnowSackItem snowSackItem) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putInt(SNOW_AMOUNT_KEY, Math.max(0, Math.min(amount, snowSackItem.maxSnowAmount())));
        }
    }

    /**
     * 增加雪数量
     * @return 实际增加的数量
     */
    public static int addSnow(ItemStack stack, int amount) {
        if (stack.getItem() instanceof SnowSackItem snowSackItem) {
            int current = getSnowAmount(stack);
            int newAmount = Math.min(current + amount, snowSackItem.maxSnowAmount());
            setSnowAmount(stack, newAmount);
            return newAmount - current;
        }
        return 0;
    }

    /**
     * 减少雪数量
     * @return 实际减少的数量
     */
    public static int removeSnow(ItemStack stack, int amount) {
        if (stack.getItem() instanceof SnowSackItem) {
            int current = getSnowAmount(stack);
            int newAmount = Math.max(0, current - amount);
            setSnowAmount(stack, newAmount);
            return current - newAmount;
        }
        return 0;
    }

    /**
     * 检查是否可以添加指定数量的雪
     */
    public static boolean canAddSnow(ItemStack stack, int amount) {
        if (stack.getItem() instanceof SnowSackItem snowSackItem) {
            int current = getSnowAmount(stack);
            return current + amount <= snowSackItem.maxSnowAmount();
        }
        return false;
    }

    /**
     * 获取自动拾取设置
     */
    public static boolean isAutoPickupEnabled(ItemStack stack) {
        if (stack.getItem() instanceof SnowSackItem) {
            CompoundTag tag = stack.getOrCreateTag();
            System.out.println("tag: " + tag);
            return tag.getByte(AUTO_PICKUP_KEY) != 0;
        }
        return false;
    }

    /**
     * 设置自动拾取
     */
    public static void setAutoPickup(ItemStack stack, boolean enabled) {
        if(FMLEnvironment.dist.isClient()){
            System.out.println("SnowSackItem.setAutoPickup: Client Side");
        } else {
            System.out.println("SnowSackItem.setAutoPickup: Server Side");
        }
        if (stack.getItem() instanceof SnowSackItem) {
            System.out.println("SnowSackItem.setAutoPickup: " + enabled);
            CompoundTag tag = stack.getOrCreateTag();
            tag.putByte(AUTO_PICKUP_KEY, enabled? (byte)1:(byte)0);
            System.out.println("tag: " + tag);
        }
    }

    /**
     * 尝试将雪球或雪块转换为雪存储在袋子中
     * @param stack 雪袋物品
     * @param itemToConvert 要转换的物品（雪球或雪块）
     * @param count 要转换的数量
     * @return 成功转换出的雪值的数量
     */
    public static int convertItemToSnow(ItemStack stack, Item itemToConvert, int count) {
        if (stack.getItem() instanceof SnowSackItem snowSackItem) {
            int snowPerItem = snowPerItem(itemToConvert);

            if (snowPerItem > 0) {
                int totalSnowToAdd = count * snowPerItem;
                if (canAddSnow(stack, totalSnowToAdd)) {
                    return addSnow(stack, totalSnowToAdd);
                } else {
                    // 尝试部分转换
                    int maxAddable = snowSackItem.maxSnowAmount() - getSnowAmount(stack);
                    int itemsToConvert = Math.min(count, maxAddable / snowPerItem);
                    int snowToAdd = itemsToConvert * snowPerItem;
                    addSnow(stack, snowToAdd);
                    return snowToAdd;
                }
            }
        }
        return 0;
    }

    /**
     * 从雪袋中取出雪并转换为物品（雪球或雪块）
     * @param stack 雪袋物品
     * @param itemToCreate 要创建的物品（雪球或雪块）
     * @param count 要创建的数量
     * @return 创建的物品堆
     */
    public static ItemStack createSnowItems(ItemStack stack, Item itemToCreate, int count) {
        if (stack.getItem() instanceof SnowSackItem) {
            int snowPerItem = snowPerItem(itemToCreate);
            
            if (snowPerItem > 0) {
                int availableSnow = getSnowAmount(stack);
                int actualCount = Math.min(count, availableSnow / snowPerItem);
                int snowToRemove = actualCount * snowPerItem;
                
                if (actualCount > 0) {
                    removeSnow(stack, snowToRemove);
                    return new ItemStack(itemToCreate, actualCount);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        int snowAmount = getSnowAmount(stack);
        return Component.translatable(this.getDescriptionId(stack), snowAmount, maxSnowAmount);
    }

    public int maxSnowAmount(){
        return this.maxSnowAmount;
    }

    public static int getMaxSnowAmount(ItemStack stack){
        if(stack.getItem() instanceof SnowSackItem snowSackItem){
            return snowSackItem.maxSnowAmount();
        }
        return 0;
    }

    /**
     * 获取物品对应的雪数量。
     * 若物品不能转化为SnowSack中的雪，则返回0。
     * @param item 需要检测的物品
     * @return 该物品可转化为的雪袋中雪数量
     */
    public static int snowPerItem(Item item){
        return SNOW_PER_ITEM.getOrDefault(item, 0);
    }
}