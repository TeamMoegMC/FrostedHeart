package com.teammoeg.frostedheart.mixin.minecraft;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IServerWorld;
/**
 * Remove natural generation for advanced animal
 * <p>
 * */
@Mixin(EntitySpawnPlacementRegistry.class)
public class EntitySpawbPlacementRegistryMixin {

	@Inject(at=	@At("HEAD"),method="Lnet/minecraft/entity/EntitySpawnPlacementRegistry;canSpawnEntity(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/IServerWorld;Lnet/minecraft/entity/SpawnReason;Lnet/minecraft/util/math/BlockPos;Ljava/util/Random;)Z")
	private static void canSpawnEntity(EntityType<?> entityType, IServerWorld world, SpawnReason reason, BlockPos pos, Random rand,CallbackInfoReturnable<Boolean> cbi) {
		if((reason==SpawnReason.CHUNK_GENERATION||reason==SpawnReason.NATURAL)
				&&(entityType==EntityType.PIG||entityType==EntityType.SHEEP||entityType==EntityType.CHICKEN||entityType==EntityType.COW
						)) {
			cbi.setReturnValue(false);
			cbi.cancel();
			
		}
	}
	
}
