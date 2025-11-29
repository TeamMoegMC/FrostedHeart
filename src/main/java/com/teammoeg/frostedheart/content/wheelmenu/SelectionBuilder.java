package com.teammoeg.frostedheart.content.wheelmenu;

import java.util.function.Predicate;

import com.simibubi.create.foundation.utility.Components;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.ui.ColorHelper;
import com.teammoeg.frostedheart.content.wheelmenu.useractions.KeyMappingTriggerAction;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class SelectionBuilder {

	private Predicate<Selection> visibility=Selection.ALWAYS_VISIBLE;
	private Action selectAction=Selection.NO_ACTION;
	private Action hoverAction=Selection.NO_ACTION;
	private CIcon icon=CIcons.nop();
	private Component message=Components.immutableEmpty();
	private boolean autoAdded=true;
	public int color=ColorHelper.CYAN;
	protected SelectionBuilder() {
	}
	public static SelectionBuilder create() {
		return new SelectionBuilder();
	}
	public static SelectionBuilder fromKey(String key,CIcon icon) {
		return new SelectionBuilder().message(Components.translatable(key)).icon(icon).selected(new KeyMappingTriggerAction(key));
	}
	public SelectionBuilder defaultHidden() {
		autoAdded=false;
		return this;
	}
	public SelectionBuilder visibleWhen(Predicate<Selection> condition) {
		visibility=condition;
		return this;
	}
	public SelectionBuilder hovered(Action action) {
		hoverAction=action;
		return this;
	}
	public SelectionBuilder selected(Action action) {
		selectAction=action;
		return this;
	}
	public SelectionBuilder message(Component message) {
		this.message=message;
		return this;
	}
	public SelectionBuilder color(int color) {
		this.color=color;
		return this;
	}
	public SelectionBuilder icon(CIcon icon) {
		this.icon=icon;
		return this;
	}
	public SelectionBuilder icon(ItemLike item) {
		this.icon=CIcons.getIcon(item);
		return this;
	}
	public SelectionBuilder icon(ItemStack item) {
		this.icon=CIcons.getIcon(item);
		return this;
	}
	public Selection build() {
		return new Selection(visibility,selectAction,hoverAction,icon,message,autoAdded,color);
	}
	public void register(WheelMenuSelectionRegisterEvent ev,ResourceLocation name) {
		ev.register(name, build());
	}
}
