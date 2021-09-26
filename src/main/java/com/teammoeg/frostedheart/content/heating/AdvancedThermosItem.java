package com.teammoeg.frostedheart.content.heating;

import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.climate.ITempAdjustFood;
import gloridifice.watersource.common.capability.WaterLevelCapability;
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

public class AdvancedThermosItem extends ThermosItem {

    public AdvancedThermosItem(String name, int capacity, int unit) {
        super(name, capacity, unit);
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
                return getFluid().isEmpty() ? new ItemStack(FHContent.FHItems.advanced_thermos) : this.container;
            }

            @Override
            public boolean isFluidValid(int tank, @Nonnull FluidStack stack)
            {
                for (Fluid fluid : FluidTags.getCollection().get(new ResourceLocation(FHMain.MODID,"hot_drink")).getAllElements()){
                    if (fluid == stack.getFluid()){
                        return true;
                    }
                }
                return false;
            }
        };
    }
}
