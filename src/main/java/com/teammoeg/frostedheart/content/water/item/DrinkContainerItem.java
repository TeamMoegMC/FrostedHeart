package com.teammoeg.frostedheart.content.water.item;

import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHTags;
import com.teammoeg.frostedheart.content.water.util.FluidHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.ItemFluidContainer;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

import static net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack.FLUID_NBT_KEY;

public class DrinkContainerItem extends ItemFluidContainer {
    boolean canDrink = false;

    public DrinkContainerItem(Properties properties, int capacity) {
        super(properties, capacity);
    }

    protected ItemStack getContainerItem(ItemStack itemStack){
        return itemStack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        BlockHitResult blockhitresult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        BlockPos pos = blockhitresult.getBlockPos();
        IFluidHandlerItem fluidHandlerItem = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (fluidHandlerItem.getFluidInTank(0).isEmpty() || fluidHandlerItem.getFluidInTank(0).getFluid() == Fluids.WATER) {
            if (level.getFluidState(pos).getType() == Fluids.WATER) {
                if (stack.getCount() > 1){
                    if (!player.getInventory().add(getFilledItem(stack, player))) {
                        player.drop(getFilledItem(stack, player), false);
                    }
                    stack.shrink(1);
                    return InteractionResultHolder.success(stack);
                }else return InteractionResultHolder.success(getFilledItem(stack, player));
            }
        }
        if (canDrink(player, stack)) return ItemUtils.startUsingInstantly(level, player, hand);
        return InteractionResultHolder.fail(player.getItemInHand(hand));

    }

    public ItemStack getFilledItem(ItemStack stack, Player player) {
        ItemStack copy = stack.copy();
        IFluidHandlerItem fluidHandlerItem = copy.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        player.playSound(SoundEvents.BUCKET_FILL, 1.0f, 1.0f);
        player.awardStat(Stats.ITEM_USED.get(this));

        if (hasDrinkItem()) {
            return FluidHelper.fillContainer(getDrinkItem(), Fluids.WATER);
        } else {
            fluidHandlerItem.fill(new FluidStack(Fluids.WATER, capacity), IFluidHandler.FluidAction.EXECUTE);
            upDateDamage(copy);
            return copy;
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack itemStack, Level level, LivingEntity livingEntity) {
        IFluidHandler handler = itemStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        handler.drain(250, IFluidHandler.FluidAction.EXECUTE);
        if (livingEntity instanceof Player player) {
            if (itemStack.getCount() == 1){
                if (handler.getFluidInTank(0).isEmpty()) {
                    return getContainerItem(itemStack);
                }else {
                    upDateDamage(itemStack);
                }
            }else {
                if (!player.getInventory().add(getContainerItem(itemStack))) {
                    player.drop(getContainerItem(itemStack), false);
                }
                itemStack.shrink(1);
                return itemStack;
            }
        }
        return itemStack;
    }

    public int getUseDuration(ItemStack stack) {
        return canDrink ? 32 : 0;
    }

    public UseAnim getUseAnimation(ItemStack p_42997_) {
        return UseAnim.DRINK;
    }

    public void upDateDamage(ItemStack stack) {
        if (stack.isDamageableItem()) {
            IFluidHandlerItem fluidHandlerItem = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
            stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getMaxDamage() - fluidHandlerItem.getFluidInTank(0).getAmount()));
        }
    }

    public boolean canDrink(Player playerIn, ItemStack stack) {
        if (playerIn.isOnFire()) return true;
        canDrink = false;
        IFluidHandlerItem fluidHandlerItem = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        canDrink = !fluidHandlerItem.getFluidInTank(0).isEmpty() && fluidHandlerItem.getFluidInTank(0).getAmount() >= 250;
        playerIn.getCapability(FHCapabilities.PLAYER_WATER_LEVEL.capability()).ifPresent(data -> {
            canDrink = canDrink && data.getWaterLevel() < 20;
        });
        return canDrink;
    }

    @Override
    public ICapabilityProvider initCapabilities(@Nonnull ItemStack stack, @Nullable CompoundTag nbt) {
        return new FluidHandlerItemStack(stack, this.capacity) {
            @Override
            public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
                for (Fluid fluid : ForgeRegistries.FLUIDS.tags().getTag(FHTags.Fluids.DRINK).stream().collect(Collectors.toList())) {
                    if (fluid == stack.getFluid()) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> components, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, level, components, tooltipFlag);
        MutableComponent textComponent = Component.literal(Component.translatable("tooltip.watersource.empty").getString());
        int amount = 0;
        if (itemStack.getTagElement(FLUID_NBT_KEY) != null) {
            IFluidHandlerItem fluidHandlerItem = FluidUtil.getFluidHandler(itemStack).orElse(null);
            textComponent = Component.literal(fluidHandlerItem.getFluidInTank(0).getDisplayName().getString());
            amount = fluidHandlerItem.getFluidInTank(0).getAmount();
        }
        components.add(textComponent.copy().append(String.format(": %d / %dmB", amount, capacity)).setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
        components.add(Component.translatable("tooltip.watersource.drink_unit").append(" : 250mB").setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
    }

    public ItemStack getDrinkItem() {
        return ItemStack.EMPTY;
    }

    public boolean hasDrinkItem() {
        return !this.getDrinkItem().isEmpty();
    }
}
