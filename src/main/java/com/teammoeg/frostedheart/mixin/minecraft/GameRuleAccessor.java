package com.teammoeg.frostedheart.mixin.minecraft;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.GameRules;
import net.minecraft.world.GameRules.RuleKey;
import net.minecraft.world.GameRules.RuleValue;
@Mixin(GameRules.class)
public interface GameRuleAccessor {
	@Accessor
	Map<RuleKey<?>, RuleValue<?>> getRules();

}
