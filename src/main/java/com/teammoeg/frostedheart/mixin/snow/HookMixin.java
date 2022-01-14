package com.teammoeg.frostedheart.mixin.snow;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.frostedheart.climate.WorldClimate;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import snownee.snow.Hook;

//Mixin into mixin
@Mixin(Hook.class)
public class HookMixin {
	@Inject(at=@At("HEAD"),method="canSurvive",remap=false,cancellable=true)
	private static void canSurvive(BlockState blockState, IWorldReader world, BlockPos pos,CallbackInfoReturnable<Boolean> cbi) {
		float t=ChunkData.getTemperature(world, pos);
		if (t < WorldClimate.HEMP_GROW_TEMPERATURE||t>WorldClimate.VANILLA_PLANT_GROW_TEMPERATURE_MAX)
			cbi.setReturnValue(false);
		
	}
}
