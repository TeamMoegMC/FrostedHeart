package com.teammoeg.frostedheart.mixin.watersource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import gloridifice.watersource.common.recipe.WaterLevelFluidRecipe;
import gloridifice.watersource.common.recipe.WaterLevelItemRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

@Mixin(WaterLevelFluidRecipe.class)
public class MixinWaterLevelFluidRecipe extends WaterLevelItemRecipe {


    public MixinWaterLevelFluidRecipe(ResourceLocation idIn, String groupIn, Ingredient ingredient, int waterLevel,
			int waterSaturationLevel) {
		super(idIn, groupIn, ingredient, waterLevel, waterSaturationLevel);
	}
    /**
     * 
     * @author khjxiaogu
     * @reason fix nbt checking bug
     * 
     * */
	@Overwrite(remap=false)
	@Override
    public boolean conform(ItemStack stack) {
        if(ingredient.test(stack)) {
        	LazyOptional<IFluidHandlerItem> handler=FluidUtil.getFluidHandler(stack);
        	LazyOptional<IFluidHandlerItem> handler2=FluidUtil.getFluidHandler(ingredient.getMatchingStacks()[0]);
            if (handler != null&&handler.isPresent()) {
                if (handler.map(data -> handler2.map(data1 -> data1.getFluidInTank(0).getFluid()==data.getFluidInTank(0).getFluid()).orElse(false)).orElse(false)) {
                    return true;
                }
            }
        }
        return false;
    }
}