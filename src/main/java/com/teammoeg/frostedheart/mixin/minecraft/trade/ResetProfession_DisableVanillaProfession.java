/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.mixin.minecraft.trade;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.world.entity.ai.behavior.ResetProfession;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.Trigger;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.server.level.ServerLevel;
/**
 * Disable vanilla village profession
 * <p>
 * */
@Mixin(ResetProfession.class)
public abstract class ResetProfession_DisableVanillaProfession {
    /**
     * @author khjxiaogu
     * @reason Disable vanilla profession
     */
    @Overwrite
    public static BehaviorControl<Villager> create() {
        return BehaviorBuilder.create(t->t.point(new Trigger<Villager>() {
			@Override
			public boolean trigger(ServerLevel pLevel, Villager pEntity, long pGameTime) {
				return false;
			}}));
    }

}
