package com.teammoeg.frostedheart.content.steamenergy;

import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.FHFluids;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.climate.ITempAdjustFood;
import com.teammoeg.frostedheart.data.FHDataManager;

import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import blusunrize.immersiveengineering.common.util.Utils;
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ITag;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.ItemFluidContainer;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;

import static net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack.FLUID_NBT_KEY;

public class HeatDebugItem extends Item  {
    public HeatDebugItem(String name) {
        super(new Properties().maxStackSize(1).setNoRepair().group(FHMain.itemGroup));
        setRegistryName(FHMain.MODID, name);
        FHContent.registeredFHItems.add(this);
        
    }

    public int getUseDuration(ItemStack stack) {
        return 1;
    }

    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }



    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        RayTraceResult raytraceresult = rayTrace(worldIn, playerIn, RayTraceContext.FluidMode.SOURCE_ONLY);
        ItemStack itemstack = playerIn.getHeldItem(handIn);
		if (raytraceresult.getType() == RayTraceResult.Type.BLOCK) {
		    BlockPos blockpos = ((BlockRayTraceResult)raytraceresult).getPos();
		    TileEntity te=Utils.getExistingTileEntity(worldIn,blockpos);
		    if(te instanceof HeatProvider) {
		    	playerIn.sendMessage(new StringTextComponent("HeatProvider network="+((HeatProvider) te).getNetwork()),playerIn.getUniqueID());
		    }else if(te instanceof EnergyNetworkProvider) {
		    	playerIn.sendMessage(new StringTextComponent("EnergyNetworkProvider network="+((EnergyNetworkProvider) te).getNetwork()),playerIn.getUniqueID());
		    }
		    try {
				if(te!=null&&te.getClass().getMethod("getNetwork")!=null) {
					Object nw=te.getClass().getMethod("getNetwork").invoke(te);
					playerIn.sendMessage(new StringTextComponent("Other tile network="+nw),playerIn.getUniqueID());
				}
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				// TODO Auto-generated catch block
			}
		    return ActionResult.resultSuccess(itemstack);
		}
		return ActionResult.resultFail(itemstack);
    }
}
