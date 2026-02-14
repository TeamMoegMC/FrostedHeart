/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.item.snowsack;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.math.Colors;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.item.FHBaseItem;
import com.teammoeg.frostedheart.item.snowsack.network.C2SOpenSnowSackScreenMessage;
import com.teammoeg.frostedheart.item.snowsack.ui.SnowSackMenuProvider;
import com.teammoeg.frostedheart.item.snowsack.ui.SnowSackScreen;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@Getter
public class SnowSackItem extends FHBaseItem {
    public static final String SNOW_AMOUNT_KEY = "SnowAmount";
    public static final String AUTO_PICKUP_KEY = "AutoPickup";
    public static final String DELETE_OVERFLOW_KEY = "DeleteOverflow";
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
        // 雪不足
        if (getSnowAmount(stack) < 4) {
            openScreen(player, stack);
            return InteractionResultHolder.success(stack);
        }
        // 获取目标位置
        var hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (level.getBlockState(hitResult.getBlockPos()).isAir()) {
            if (!player.isCrouching()) {
                openScreen(player, stack);
            }
            return InteractionResultHolder.fail(stack);
        }
        var pos = hitResult.getBlockPos();
        var state = level.getBlockState(pos);
        if (!state.canBeReplaced()) {
            pos = pos.relative(hitResult.getDirection());
            if (!level.getBlockState(pos).canBeReplaced()) {
                return super.use(level, player, hand);
            }
        }
        // 尝试放置雪块
        var snowBlock = Blocks.SNOW_BLOCK.defaultBlockState();
        CollisionContext cc = CollisionContext.of(player);
        if (level.isUnobstructed(snowBlock, pos, cc) && level.setBlockAndUpdate(pos, snowBlock)) {
            setSnowAmount(stack, getSnowAmount(stack) - 4);
            level.playSound(null, pos, snowBlock.getSoundType().getPlaceSound(), SoundSource.BLOCKS);
            return InteractionResultHolder.success(stack);
        }

        return super.use(level, player, hand);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        var item = slot.getItem();
        if (action == ClickAction.SECONDARY && isSnow(item)) {
            insertSnow(stack, item, player);
            return true;
        }
        return false;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (!(ClientUtils.getMc().screen instanceof SnowSackScreen) && action == ClickAction.SECONDARY) {
            if (other.isEmpty()) {
                openScreenFromClient(player, stack);
                return true;
            } else if (isSnow(other)) {
                insertSnow(stack, other, player);
                return true;
            }
        }
        return false;
    }

    private static void insertSnow(ItemStack sack, ItemStack item, Player player) {
        int converted = convertItemToSnow(sack, item);
        int remaining = item.getCount() - (converted / snowPerItem(item.getItem()));
        item.setCount(Math.max(remaining, 0));
        player.playSound(SoundEvents.POWDER_SNOW_PLACE, 0.8F, 0.8F + player.getRandom().nextFloat()*0.2F);
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.4F, 0.8F + player.getRandom().nextFloat()*0.4F);
    }


    private static void openScreenFromClient(Player player, ItemStack stack) {
        if (player.level().isClientSide) {
            var slot = CUtils.getItemSlotInPlayerInv(player, stack);
            // 防止在合成栏打开
            if (slot == null || (slot.container instanceof CraftingContainer && slot.index <= 4)) {
                return;
            }
            FHNetwork.INSTANCE.sendToServer(new C2SOpenSnowSackScreenMessage(slot.index));
        }
    }

    private static void openScreen(Player player, ItemStack stack) {
        if (!player.level().isClientSide) {
            var slot = CUtils.getItemSlotInPlayerInv(player, stack);
            if (slot == null) {
                return;
            }
            NetworkHooks.openScreen((ServerPlayer) player, new SnowSackMenuProvider((ServerPlayer) player, slot.index), buf -> buf.writeInt(slot.index));
        }
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
            int count = Math.max(0, Math.min(amount, snowSackItem.maxSnowAmount()));
            tag.putInt(SNOW_AMOUNT_KEY, count);
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
            return isDeleteOverflowEnabled(stack) ? amount : newAmount - current;
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
            if (isDeleteOverflowEnabled(stack)) {
                return true;
            }
            int current = getSnowAmount(stack);
            return current + amount <= snowSackItem.maxSnowAmount();
        }
        return false;
    }

    /**
     * 获取自动拾取设置
     */
    public static boolean isAutoPickupEnabled(ItemStack stack) {
        return stack.getItem() instanceof SnowSackItem
                && stack.getOrCreateTag().getByte(AUTO_PICKUP_KEY) != 0;
    }

    public static boolean isDeleteOverflowEnabled(ItemStack stack) {
        return stack.getItem() instanceof SnowSackItem
                && stack.getOrCreateTag().getByte(DELETE_OVERFLOW_KEY) != 0;
    }

    /**
     * 设置自动拾取
     */
    public static void setAutoPickup(ItemStack stack, boolean enabled) {
        if (stack.getItem() instanceof SnowSackItem) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putByte(AUTO_PICKUP_KEY, enabled? (byte)1:(byte)0);
        }
    }

    public static void setDeleteOverflow(ItemStack stack, boolean enabled) {
        if (stack.getItem() instanceof SnowSackItem) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putByte(DELETE_OVERFLOW_KEY, enabled? (byte)1:(byte)0);
        }
    }

    public static boolean isSnow(Item item) {
        return item == Items.SNOWBALL || item == Items.SNOW_BLOCK;
    }

    public static boolean isSnow(ItemStack stack) {
        return stack.is(Items.SNOWBALL) || stack.is(Items.SNOW_BLOCK);
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

    public static int convertItemToSnow(ItemStack stack, ItemStack other) {
        return convertItemToSnow(stack, other.getItem(), other.getCount());
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

    @Override
    public boolean isBarVisible(ItemStack pStack) {
        return getSnowAmount(pStack) != 0;
    }

    @Override
    public int getBarWidth(ItemStack pStack) {
        return Math.round((float)getSnowAmount(pStack) * 13.0F / (float)maxSnowAmount);
    }

    @Override
    public int getBarColor(ItemStack pStack) {
        return Colors.WHITE;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltips, TooltipFlag isAdvanced) {
        if (stack.getItem() instanceof SnowSackItem) {
            tooltips.add(FlatIcon.INFO.toCTextIcon().copy()
                    .append(" ")
                    .append(Component.translatable("tooltip.frostedheart.snow_sack.tip"))
                    .withStyle(ChatFormatting.GRAY));

            var count = Component.literal("x ")
                    .append(Items.SNOWBALL.getDescription())
                    .append(Component.literal(" ≈ " + getSnowAmount(stack)/4 + "x "))
                    .append(Items.SNOW_BLOCK.getDescription());
            tooltips.add(Component.translatable("gui.frostedheart.snow_sack.stored_snow", getSnowAmount(stack))
                    .append(count)
                    .withStyle(ChatFormatting.GRAY));

            tooltips.add(getAutoPickupText(stack).withStyle(ChatFormatting.GRAY));
            tooltips.add(getDeleteOverflowText(stack).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, level, tooltips, isAdvanced);
    }

    private MutableComponent getAutoPickupText(ItemStack stack) {
        boolean autoPickup = isAutoPickupEnabled(stack);
        var state = autoPickup ? Component.translatable("gui.frostedheart.enabled") : Component.translatable("gui.frostedheart.disabled");
        state.withStyle(autoPickup ? ChatFormatting.GREEN : ChatFormatting.GRAY);
        return Component.translatable("gui.frostedheart.snow_sack.auto_pickup", state);
    }

    private MutableComponent getDeleteOverflowText(ItemStack stack) {
        boolean deleteOverflow = isDeleteOverflowEnabled(stack);
        var state = deleteOverflow ? Component.translatable("gui.frostedheart.enabled") : Component.translatable("gui.frostedheart.disabled");
        state.withStyle(deleteOverflow ? ChatFormatting.GREEN : ChatFormatting.GRAY);
        return Component.translatable("gui.frostedheart.snow_sack.delete_overflow", state);
    }
}