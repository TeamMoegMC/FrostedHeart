package com.teammoeg.frostedheart.scenario.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.scenario.EventTriggerType;
import com.teammoeg.frostedheart.scenario.Param;
import com.teammoeg.frostedheart.scenario.runner.ScenarioVM;
import com.teammoeg.frostedheart.scenario.runner.target.VariantTargetTrigger;
import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.registries.ForgeRegistries;

public class MCCommands {

	public MCCommands() {
		// TODO Auto-generated constructor stub
	}
	public void giveItem(ScenarioVM runner,@Param("i")String item,@Param("n")String nbt,@Param("c")int count) throws CommandSyntaxException {
		Item i=ForgeRegistries.ITEMS.getValue(new ResourceLocation(item));
		if(count==0)count=1;
		ItemStack is=new ItemStack(i,count);
		if(nbt!=null)
			is.setTag(JsonToNBT.getTagFromJson(nbt));
		FHUtils.giveItem(runner.getPlayer(), is);
	}
	public void setResearchAttribute(ScenarioVM runner,@Param("k")String key,@Param("v")double value) {
		 ResearchDataAPI.putVariantDouble(runner.getPlayer(), key, value);
	}
	public void waitPlayerStart(ScenarioVM runner,@Param("s")String s,@Param("l")String l) {
		runner.addTrigger(new VariantTargetTrigger(runner,s,l).setSync().register(runner.getPlayer(),EventTriggerType.PLAYER_INTERACT));
	}
}
