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