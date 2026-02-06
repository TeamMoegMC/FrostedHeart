/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.wheelmenu.useractions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.wheelmenu.Action;
import com.teammoeg.frostedheart.content.wheelmenu.Selection;

import net.minecraft.SharedConstants;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public record CommandInputAction(String command) implements Action{
	public static final MapCodec<CommandInputAction> CODEC=RecordCodecBuilder.mapCodec(t->t.group(
		Codec.STRING.fieldOf("command").forGetter(CommandInputAction::command)
		).apply(t,CommandInputAction::new));
	public String getCommand() {
		return command;
	}
	@OnlyIn(Dist.CLIENT)
	@Override
	public void execute(Selection selection) {
		 String s1 = SharedConstants.filterText(command);
         if (s1.startsWith("/")) {
            if (!ClientUtils.getLocalPlayer().connection.sendUnsignedCommand(s1.substring(1))) {
               FHMain.LOGGER.error("Not allowed to run unsigned command '{}' from wheelmenu action", s1);
            }
         } else {
        	 FHMain.LOGGER.error("Failed to run command without '/' prefix from wheelmenu action: '{}'", (Object)s1);
         }
	}

}