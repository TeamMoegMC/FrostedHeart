package com.teammoeg.frostedheart.compat;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;

import moze_intel.projecte.api.mapper.EMCMapper;
import moze_intel.projecte.api.mapper.IEMCMapper;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.minecraft.item.Item;
import net.minecraft.resources.DataPackRegistries;
import net.minecraft.resources.IResourceManager;
import net.minecraftforge.registries.ForgeRegistries;
@EMCMapper
public class PECompat implements IEMCMapper<NormalizedSimpleStack,Long> {

	@Override
	public void addMappings(IMappingCollector<NormalizedSimpleStack, Long> arg0, CommentedFileConfig arg1,
			DataPackRegistries arg2, IResourceManager arg3) {
		for(Item i:ForgeRegistries.ITEMS.getValues())
		arg0.setValueBefore(NSSItem.createItem(i),0L);
	}

	@Override
	public String getDescription() {
		return "projecte compat";
	}

	@Override
	public String getName() {
		return "projectec";
	}

}
