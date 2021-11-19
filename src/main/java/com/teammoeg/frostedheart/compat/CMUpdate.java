package com.teammoeg.frostedheart.compat;

import com.teammoeg.frostedheart.FHContent.FHBlocks;
import com.teammoeg.frostedheart.FHContent.FHTileTypes;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
@Mod.EventBusSubscriber
public class CMUpdate {
	   @SubscribeEvent
	    public void onMissingTE(final RegistryEvent.MissingMappings<TileEntityType<?>> ev) {
	    	ev.getMappings("custommachinery").forEach(e->e.remap(FHTileTypes.CMUPDATE.get()));;
	    	System.out.println("mmtfired");
	    }
	    @SubscribeEvent
	    public void onMissing(final RegistryEvent.MissingMappings<Block> ev) {
	    	ev.getMappings("custommachinery").forEach(e->{
	    		e.remap(FHBlocks.cmupdate);
	    		System.out.println(e.key);
	    		});;
	    		System.out.println("mmbfired");
	    }
	    @SubscribeEvent
	    public void onMissingIT(final RegistryEvent.MissingMappings<Item> ev) {
	    	ev.getMappings("custommachinery").forEach(e->{
	    		e.remap(ForgeRegistries.ITEMS.getValue(FHBlocks.cmupdate.getRegistryName()));
	    		System.out.println(e.key);
	    		});;
	    		System.out.println("mmbfired");
	    }
}
