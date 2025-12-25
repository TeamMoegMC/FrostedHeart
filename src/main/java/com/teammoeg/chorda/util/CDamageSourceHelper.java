package com.teammoeg.chorda.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
/**
 * Convenience method for constructing damage source
 * 
 */
public class CDamageSourceHelper {

	private CDamageSourceHelper() {
	}

	// Fully construct damageSource, used when overwriting attacking position, maybe one entity summon some attack from another side?
	// change in pos would affect blocking and knockback
	public static DamageSource source(Level level, ResourceKey<DamageType> type, Entity source, Entity dest,Vec3 pos) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type),dest,source,pos);
	}

	// Source and destination entity are different, used in attack, use source entity's position as position
	public static DamageSource source(Level level, ResourceKey<DamageType> type, Entity source, Entity dest) {
		return source(level,type,source,dest,null);
	}

	// Destination and source entity are the same, self-hurting or from poison
	public static DamageSource source(Level level, ResourceKey<DamageType> type, Entity dest) {
		return source(level,type,dest,dest);
	}

	// Environmental damage with no knockback, typically from block
	public static DamageSource source(Level level, ResourceKey<DamageType> type, Vec3 pos) {
		return source(level,type,null,null,pos);
	}

	// Environmental damage with no knockback, typically from environment
	public static DamageSource source(Level level, ResourceKey<DamageType> type) {
	    return source(level,type,(Vec3)null);
	}

}
