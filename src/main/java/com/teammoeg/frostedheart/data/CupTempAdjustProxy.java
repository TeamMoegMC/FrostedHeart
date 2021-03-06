package com.teammoeg.frostedheart.data;

import com.teammoeg.frostedheart.climate.ITempAdjustFood;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

public class CupTempAdjustProxy implements ITempAdjustFood {
	float efficiency;
	ITempAdjustFood defData;
	public CupTempAdjustProxy(float efficiency,ITempAdjustFood defaultData) {
		this.efficiency = efficiency;
		this.defData=defaultData;
	}
	@Override
	public float getMaxTemp(ItemStack is) {
		if(defData!=null)
			return defData.getMaxTemp(is);
		return ITempAdjustFood.super.getMaxTemp(is);
	}
	@Override
	public float getMinTemp(ItemStack is) {
		if(defData!=null)
			return defData.getMinTemp(is);
		return ITempAdjustFood.super.getMinTemp(is);
	}
	@Override
	public float getHeat(ItemStack is) {
        LazyOptional<IFluidHandlerItem> ih = FluidUtil.getFluidHandler(is);
        if (ih.isPresent()) {
            IFluidHandlerItem f = ih.resolve().get();
            FluidStack fs = f.getFluidInTank(0);
            if (!fs.isEmpty()) {
                return FHDataManager.getDrinkHeat(fs)*efficiency;
            }
        }
        if(defData!=null)
        	return defData.getHeat(is);
        return 0;
	}

}
