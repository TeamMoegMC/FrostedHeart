package com.teammoeg.frostedheart.mixin.rankine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.cannolicatfish.rankine.events.handlers.common.EntityInteractHandler;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
@Mixin(EntityInteractHandler.class)
public class EntityInteractHandlerMixin {
	/**
	 * @author khjxiaogu
	 * @param event 
	 * @reason cancel rankine breed
	 * */
	@Overwrite(remap=false)
	public static void onBreedEvent( PlayerInteractEvent.EntityInteract event) {
		
	}
}
