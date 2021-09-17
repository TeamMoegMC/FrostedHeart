package com.teammoeg.frostedheart.data;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.climate.IWarmKeepingEquipment;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.biome.Biome.RainType;

public class ArmorTempData extends JsonDataHolder implements IWarmKeepingEquipment {

	public ArmorTempData(JsonObject data) {
		super(data);
	}

	@Override
	public float getFactor(ServerPlayerEntity pe,ItemStack stack) {
		float base=this.getFloatOrDefault("factor",0F);
		if(pe.isBurning()) 
			base+=this.getFloatOrDefault("fire",0F);
		if(pe.isInWaterOrBubbleColumn())
			base+=this.getFloatOrDefault("water",0F);
		else if(pe.isInWaterRainOrBubbleColumn()) {
			if(pe.getServerWorld().getBiome(pe.getPosition()).getPrecipitation()==RainType.SNOW)
				base+=this.getFloatOrDefault("snow",0F);
			else
				base+=this.getFloatOrDefault("rain",0F);
		}
		if(false) {//further implement wet
			base+=this.getFloatOrDefault("wet",0F);
		}
		float min=this.getFloatOrDefault("min",Float.NEGATIVE_INFINITY);
		if(base<min) {
			base=min;
		}else {
			float max=this.getFloatOrDefault("max",1F);
			if(base>max)
				base=max;
			
		}
		return base;
	}

}
