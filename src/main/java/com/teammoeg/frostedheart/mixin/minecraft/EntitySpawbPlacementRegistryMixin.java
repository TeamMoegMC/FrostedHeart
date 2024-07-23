package com.teammoeg.frostedheart.mixin.minecraft;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
/**
 * Remove natural generation for advanced animal
 * <p>
 * */
@Mixin(SpawnPlacements.class)
public class EntitySpawbPlacementRegistryMixin {

	@Inject(at=	@At("HEAD"),method="Lnet/minecraft/entity/EntitySpawnPlacementRegistry;canSpawnEntity(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/IServerWorld;Lnet/minecraft/entity/SpawnReason;Lnet/minecraft/util/math/BlockPos;Ljava/util/Random;)Z")
	private static void canSpawnEntity(EntityType<?> entityType, ServerLevelAccessor world, MobSpawnType reason, BlockPos pos, Random rand,CallbackInfoReturnable<Boolean> cbi) {
		if((reason==MobSpawnType.CHUNK_GENERATION||reason==MobSpawnType.NATURAL)
				&&(entityType==EntityType.PIG||entityType==EntityType.SHEEP||entityType==EntityType.CHICKEN||entityType==EntityType.COW
						)) {
			cbi.setReturnValue(false);
			cbi.cancel();
			
		}
	}
	
}
