package com.teammoeg.frostedheart.mixin.minecraft;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.Key;
import net.minecraft.world.level.GameRules.Value;
@Mixin(GameRules.class)
public interface GameRuleAccessor {
	@Accessor
	Map<Key<?>, Value<?>> getRules();

}
