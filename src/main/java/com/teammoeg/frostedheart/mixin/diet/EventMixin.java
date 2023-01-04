package com.teammoeg.frostedheart.mixin.diet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.teammoeg.frostedheart.util.FixedDietProvider;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import top.theillusivec4.diet.api.DietCapability;
import top.theillusivec4.diet.api.IDietTracker;
import top.theillusivec4.diet.common.capability.DietCapabilityEventsListener;
import top.theillusivec4.diet.common.capability.PlayerDietTracker;

@Mixin(DietCapabilityEventsListener.class)
public class EventMixin {
	@Overwrite(remap = false)
	public static void attachCapabilities(final AttachCapabilitiesEvent<Entity> evt) {

		if (evt.getObject() instanceof PlayerEntity) {
			final IDietTracker tracker = new PlayerDietTracker((PlayerEntity) evt.getObject());
			final LazyOptional<IDietTracker> capability = LazyOptional.of(() -> tracker);
			evt.addCapability(DietCapability.DIET_TRACKER_ID, new FixedDietProvider(capability));
		}
	}
	
}
