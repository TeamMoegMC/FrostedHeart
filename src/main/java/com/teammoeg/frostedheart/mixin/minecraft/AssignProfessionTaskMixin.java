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

package com.teammoeg.frostedheart.mixin.minecraft;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.entity.ai.brain.memory.MemoryModuleStatus;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.AssignProfessionTask;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.world.server.ServerWorld;
/**
 * Disable vanilla village profession
 * <p>
 * */
@Mixin(AssignProfessionTask.class)
public abstract class AssignProfessionTaskMixin extends Task<VillagerEntity> {
    public AssignProfessionTaskMixin(Map<MemoryModuleType<?>, MemoryModuleStatus> requiredMemoryStateIn) {
        super(requiredMemoryStateIn);
    }

    /**
     * @author khjxiaogu
     * @reason Disable vanilla profession
     */
    @Overwrite
    protected boolean shouldExecute(ServerWorld worldIn, VillagerEntity owner) {
        return false;
    }
}
