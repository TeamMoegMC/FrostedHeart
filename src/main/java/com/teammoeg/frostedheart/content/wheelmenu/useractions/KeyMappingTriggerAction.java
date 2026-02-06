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

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.wheelmenu.Action;
import com.teammoeg.frostedheart.content.wheelmenu.Selection;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;

public record KeyMappingTriggerAction(String name) implements Action{
	public static final MapCodec<KeyMappingTriggerAction> CODEC=RecordCodecBuilder.mapCodec(t->t.group(
		Codec.STRING.fieldOf("key").forGetter(KeyMappingTriggerAction::name)
		).apply(t,KeyMappingTriggerAction::new));
	public String getKey() {
		return name;
	}
	@OnlyIn(Dist.CLIENT)
	@Override
	public void execute(Selection selection) {
		KeyMapping km=KeyMapping.ALL.get(name);
		km.setDown(true);
		km.clickCount++;
		MinecraftForge.EVENT_BUS.post(new InputEvent.Key(0, 0, InputConstants.PRESS, 0));//mock key press
		MinecraftForge.EVENT_BUS.post(new InputEvent.Key(0, 0, InputConstants.RELEASE, 0));
		km.setDown(false);
		//km.consumeClick();
	}

}