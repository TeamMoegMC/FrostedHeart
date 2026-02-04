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

package com.teammoeg.frostedheart.mixin.client.create;

import com.simibubi.create.foundation.events.ClientEvents;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Don’t call TooltipModifier.modify(event) inside
 * ClientEvents.addToItemTooltip(ItemTooltipEvent) but keep the rest
 * of the method intact.
 *
 * This is because we handled this on our own. See FHTooltips.
 */
@Mixin(ClientEvents.class)
public abstract class ClientEventsMixin {

    /**
     * Replace the single INVOKE of TooltipModifier#modify with a no-op.
     *
     * addToItemTooltip signature (official/MojMap):
     *   (Lnet/minecraftforge/event/entity/player/ItemTooltipEvent;)V
     *
     * TooltipModifier.modify signature:
     *   (Lnet/minecraftforge/event/entity/player/ItemTooltipEvent;)V
     */
    @Redirect(
            method = "addToItemTooltip(Lnet/minecraftforge/event/entity/player/ItemTooltipEvent;)V",
            at = @At(
                    value  = "INVOKE",
                    target = "Lcom/simibubi/create/foundation/item/TooltipModifier;" +
                            "modify(Lnet/minecraftforge/event/entity/player/ItemTooltipEvent;)V"
            ),
            remap = false
    )
    private static void create$skipModify(
            TooltipModifier self, ItemTooltipEvent event) {
        /* deliberately empty – we just don't call self.modify(event) */
    }
}
