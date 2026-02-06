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

package com.teammoeg.frostedheart.content.wheelmenu;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.client.StringTextComponentParser;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.io.registry.TypedCodecRegistry;
import com.teammoeg.frostedheart.content.wheelmenu.Action.NoAction;
import com.teammoeg.frostedheart.content.wheelmenu.useractions.CommandInputAction;
import com.teammoeg.frostedheart.content.wheelmenu.useractions.KeyMappingTriggerAction;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public record UserSelection(String id,String message, CIcon icon, Action selectAction) {


	public static TypedCodecRegistry<Action> registry=new TypedCodecRegistry<>();
	static{
		registry.register(KeyMappingTriggerAction.class, "key", KeyMappingTriggerAction.CODEC);
		registry.register(CommandInputAction.class, "command", CommandInputAction.CODEC);
		registry.register(NoAction.class, "none",MapCodec.unit(NoAction.INSTANCE));
	}
	public static final Codec<Action> USER_ACTION_CODEC=registry.codec();
	public static final Codec<List<UserSelection>> USER_SELECTION_LIST=Codec.list(UserSelection.CODEC);
	public static final Codec<UserSelection> CODEC=RecordCodecBuilder.create(t->t.group(
			Codec.STRING.fieldOf("id").forGetter(UserSelection::id),
			Codec.STRING.fieldOf("name").forGetter(UserSelection::message),
			CIcons.CODEC.fieldOf("icon").forGetter(UserSelection::icon),
			USER_ACTION_CODEC.fieldOf("action").forGetter(UserSelection::selectAction)
		).apply(t, UserSelection::new));

	public Component getParsedMessage() {
		return StringTextComponentParser.parse(message);
	}
	public ResourceLocation userLocation() {
		return new ResourceLocation("wheel_menu_user",id());
	}
	public ResourceLocation worldLocation() {
		return new ResourceLocation("wheel_menu_world",id());
	}
}