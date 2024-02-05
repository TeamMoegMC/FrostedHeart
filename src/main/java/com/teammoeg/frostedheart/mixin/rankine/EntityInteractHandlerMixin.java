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

package com.teammoeg.frostedheart.mixin.rankine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.cannolicatfish.rankine.events.handlers.common.EntityInteractHandler;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;

@Mixin(EntityInteractHandler.class)
public class EntityInteractHandlerMixin {
    /**
     * @param event
     * @author khjxiaogu
     * @reason cancel rankine breed
     */
    @Overwrite(remap = false)
    public static void onBreedEvent(PlayerInteractEvent.EntityInteract event) {

    }
}
