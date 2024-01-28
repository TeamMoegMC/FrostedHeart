package com.teammoeg.frostedheart.scenario.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.scenario.Param;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;
import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class MCCommands {

	public MCCommands() {
		// TODO Auto-generated constructor stub
	}
	public void giveItem(ScenarioConductor runner,@Param("i")String item,@Param("n")String nbt,@Param("c")int count) throws CommandSyntaxException {
		Item i=ForgeRegistries.ITEMS.getValue(new ResourceLocation(item));
		if(count==0)count=1;
		ItemStack is=new ItemStack(i,count);
		if(nbt!=null)
			is.setTag(JsonToNBT.getTagFromJson(nbt));
		FHUtils.giveItem(runner.getPlayer(), is);
	}
	public void setResearchAttribute(ScenarioConductor runner,@Param("k")String key,@Param("v")double value) {
		 ResearchDataAPI.putVariantDouble(runner.getPlayer(), key, value);
	}
}
