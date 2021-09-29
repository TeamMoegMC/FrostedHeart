package com.teammoeg.frostedheart.content.heating;

import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.FHFluids;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.climate.ITempAdjustFood;
import gloridifice.watersource.WaterSource;
import gloridifice.watersource.common.capability.WaterLevelCapability;
import gloridifice.watersource.helper.FluidHelper;
import gloridifice.watersource.registry.ItemRegistry;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.ItemFluidContainer;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack.FLUID_NBT_KEY;

public class ThermosItem extends ItemFluidContainer implements ITempAdjustFood {
    boolean canDrink = false;
    boolean canFill = false;
    final int unit;

    public ThermosItem(String name, int capacity, int unit) {
        super(new Properties().maxStackSize(1).setNoRepair().maxDamage(capacity).group(FHMain.itemGroup), capacity);
        this.unit = unit;
        setRegistryName(FHMain.MODID, name);
        FHContent.registeredFHItems.add(this);
    }

    public int getUseDuration(ItemStack stack) {
        return canDrink ? 40 : 0;
    }

    public UseAction getUseAction(ItemStack stack) {
        return canDrink ? UseAction.DRINK : UseAction.NONE;
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving) {
        PlayerEntity entityplayer = entityLiving instanceof PlayerEntity ? (PlayerEntity) entityLiving : null;
        stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).ifPresent(data -> {
            data.drain(unit, IFluidHandler.FluidAction.EXECUTE);
            if (entityplayer != null) {
                entityplayer.addStat(Stats.ITEM_USED.get(this));
            }
        });
        updateDamage(stack);
        return stack;
    }

    public void updateDamage(ItemStack stack){
        stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).ifPresent(data ->{
            int i = this.capacity - data.getFluidInTank(0).getAmount() >= 0 ? this.capacity - data.getFluidInTank(0).getAmount() : 0;
            stack.setDamage(i);
        });
    }

    public int getUnit() {
        return unit;
    }

    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);
        updateDamage(stack);
    }

    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        canFill = false;
        RayTraceResult raytraceresult = rayTrace(worldIn, playerIn, RayTraceContext.FluidMode.SOURCE_ONLY);
        ItemStack itemstack = playerIn.getHeldItem(handIn);
        if (raytraceresult.getType() == RayTraceResult.Type.MISS) {
            playerIn.setActiveHand(handIn);
            return canDrink(playerIn,playerIn.getHeldItem(handIn)) ? ActionResult.resultSuccess(playerIn.getHeldItem(handIn)) : ActionResult.resultFail(playerIn.getHeldItem(handIn));
        } else {
            if (raytraceresult.getType() == RayTraceResult.Type.BLOCK) {
                BlockPos blockpos = ((BlockRayTraceResult)raytraceresult).getPos();
                if (worldIn.getFluidState(blockpos).isTagged(FluidTags.WATER)) {
                    itemstack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).ifPresent(data ->{
                        if (data.getFluidInTank(0).isEmpty() || data.getFluidInTank(0).getFluid() == Fluids.WATER){
                            canFill = true;
                        }
                    });
                    if (canFill){
                        worldIn.playSound(playerIn, playerIn.getPosX(), playerIn.getPosY(), playerIn.getPosZ(), SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                        itemstack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).ifPresent(data -> {
                            data.fill(new FluidStack(Fluids.WATER,data.getTankCapacity(0)), IFluidHandler.FluidAction.EXECUTE);
                        });
                        return ActionResult.resultSuccess(itemstack);
                    }
                }
                playerIn.setActiveHand(handIn);
                return canDrink(playerIn,playerIn.getHeldItem(handIn)) ? ActionResult.resultSuccess(playerIn.getHeldItem(handIn)) : ActionResult.resultFail(playerIn.getHeldItem(handIn));
            }
            return ActionResult.resultFail(itemstack);
        }
    }

    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
        if (this.isInGroup(group)) {
            ITag<Fluid> tag = FluidTags.getCollection().get(new ResourceLocation(FHMain.MODID,"drink"));
            if (tag == null) return;
            for (Fluid fluid : tag.getAllElements()) {
                ItemStack itemStack = new ItemStack(this);
                items.add(FluidHelper.fillContainer(itemStack,fluid));
            }
            items.add(new ItemStack(this));
        }
    }

    public boolean canDrink(PlayerEntity playerIn, ItemStack stack){
        canDrink = false;
        if (this.getDamage(stack) <= this.getMaxDamage(stack) - getUnit()){
            playerIn.getCapability(WaterLevelCapability.PLAYER_WATER_LEVEL).ifPresent(data -> {
                canDrink = data.getWaterLevel() < 20;
            });
        }
        return canDrink;
    }

    public SoundEvent getDrinkSound() {
        return SoundEvents.ENTITY_GENERIC_DRINK;
    }

    @Override
    public ICapabilityProvider initCapabilities(@Nonnull ItemStack stack, @Nullable CompoundNBT nbt)
    {
        return new FluidHandlerItemStack(stack, capacity)
        {
            @Nonnull
            @Override
            @SuppressWarnings("deprecation")
            public ItemStack getContainer()
            {
                return getFluid().isEmpty() ? new ItemStack(FHContent.FHItems.thermos) : this.container;
            }

            @Override
            public boolean isFluidValid(int tank, @Nonnull FluidStack stack)
            {
                for (Fluid fluid : FluidTags.getCollection().get(new ResourceLocation(FHMain.MODID,"drink")).getAllElements()){
                    if (fluid == stack.getFluid()){
                        return true;
                    }
                }
                return false;
            }
        };
    }

    @Override
    public ItemStack getContainerItem(ItemStack itemStack) {
        ItemStack itemStack1 = itemStack.copy();
        itemStack1.setDamage(capacity);
        return itemStack1;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        if (stack.getChildTag(FLUID_NBT_KEY) != null)
        {
            FluidUtil.getFluidHandler(stack).ifPresent(f ->{
                tooltip.add(((TextComponent)f.getFluidInTank(0).getDisplayName()).appendString(String.format(": %d / %dmB", f.getFluidInTank(0).getAmount(), capacity)).mergeStyle(TextFormatting.GRAY));
                tooltip.add(new TranslationTextComponent("tooltip.watersource.drink_unit").appendString(" : "+ this.getUnit() + "mB").mergeStyle(TextFormatting.GRAY));
            });
        }
    }

    @Override
    public float getHeat(ItemStack is) {
        int heat = 0;
//        FluidUtil.getFluidHandler(is).ifPresent(f ->{
//            if (FluidTags.getCollection().get(FHMain.rl("hot_drink")).contains(f.getFluidInTank(0).getFluid())) {
//                heat += 10;
//            }
//        });
        return 0.25F;
    }

    @Override
    public float getMaxTemp(ItemStack is) {
        return 1;
    }

    @Override
    public float getMinTemp(ItemStack is) {
        return -1;
    }
}
