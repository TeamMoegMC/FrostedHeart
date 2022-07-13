package com.teammoeg.frostedheart.mixin.rankine;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.cannolicatfish.rankine.recipe.helper.ConfigHelper;

import net.minecraftforge.common.ForgeConfigSpec;

@Mixin(ConfigHelper.class)
public class ConfigHelperMixin {
	@Shadow(remap = false)
	private static List<ForgeConfigSpec.IntValue> oreConfig;

	public ConfigHelperMixin() {
	}

	@Overwrite(remap = false)
	public static int getOreHarvestLevel(List<String> path) {
		if(path.size()==0)return -1;
		String rpath=path.get(path.size()-1);
		List<ForgeConfigSpec.IntValue> in = oreConfig.stream()
				.filter((config) -> equalsLastPart(config.getPath(), rpath))
				.collect(Collectors.<ForgeConfigSpec.IntValue>toList());
		//System.out.println(String.join(".",path));
		//System.out.println(String.join(".",in.get(0).getPath()));
		if (in.size() >= 1) {
			return in.get(0).get();
		} else {
			return -1;
		}
	}
	private static boolean equalsLastPart(List<String> list1,String l2) {
		if (list1.size() ==0)
			return false;
		
		return list1.get(list1.size()-1).equals(l2);
	}
	private static boolean equalsIgnoreCase(List<String> list1, List<String> list2) {
		if (list1.size() != list2.size())
			return false;
		for (Iterator<String> iter1 = list1.iterator(), iter2 = list2.iterator(); iter1.hasNext();)
			if (!iter1.next().equalsIgnoreCase(iter2.next()))
				return false;
		return true;
	}
}
