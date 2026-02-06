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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Unit;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.cui.editor.BaseEditDialog;
import com.teammoeg.chorda.client.cui.editor.EditListDialog;
import com.teammoeg.chorda.client.cui.editor.EditUtils;
import com.teammoeg.chorda.client.cui.editor.Editor;
import com.teammoeg.chorda.client.cui.editor.EditorDialogBuilder;
import com.teammoeg.chorda.client.cui.editor.EditorSelector;
import com.teammoeg.chorda.client.cui.editor.Editors;
import com.teammoeg.chorda.client.cui.editor.OpenEditorButton;
import com.teammoeg.chorda.client.cui.editor.SelectDialog;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.icon.CIcons.ItemIcon;
import com.teammoeg.chorda.io.ConfigFileUtil;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.chorda.util.CFunctionUtils;
import com.teammoeg.chorda.util.struct.CurryApplicativeTemplate;
import com.teammoeg.frostedheart.content.wheelmenu.useractions.CommandInputAction;
import com.teammoeg.frostedheart.content.wheelmenu.useractions.KeyMappingTriggerAction;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class WheelMenuEditors {
	public static final Editor<KeyMapping> KEY_EDITOR = (p, l, v, c) -> new SelectDialog<>(p, l, v, c,
		() -> Arrays.asList(ClientUtils.getMc().options.keyMappings),
		o -> Components.translatable(o.getCategory()).append(": ").append(Components.translatable(o.getName())).append("(").append(o.getTranslatedKeyMessage()).append(")")).open();
	public static final Editor<KeyMappingTriggerAction> KEY_ACTION_EDITOR = KEY_EDITOR.xmap(t->t.getName(), KeyMapping.ALL::get).xmap(KeyMappingTriggerAction::new, KeyMappingTriggerAction::getKey);

	public static final Editor<CommandInputAction> COMMAND_ACTION_EDITOR = Editors.COMMAND_PROMPT.xmap(CommandInputAction::new, CommandInputAction::getCommand);
	public static final Editor<Action> ACTION_EDITOR = new EditorSelector.EditorSelectorBuilder<Action>(t->t==null||t==Selection.NO_ACTION)
		.addEditor(Components.translatable("gui.wheel_menu.editor.key"), KEY_ACTION_EDITOR, t -> t instanceof KeyMappingTriggerAction)
		.addEditor(Components.translatable("gui.wheel_menu.editor.command"), COMMAND_ACTION_EDITOR, t -> t instanceof CommandInputAction)
		.build();
	//public static final Editor<ResourceLocation> SELECTION_CHOOSER = (p, l, v, c) -> new SelectDialog<>(p, l, v, c,
	//	() -> WheelMenuRenderer.hiddenSelections, o -> WheelMenuRenderer.selections.get(o).getMessage(), null, o -> WheelMenuRenderer.selections.get(o).getIcon()).open();
	public static final Editor<UserSelection> SELECTION_EDITOR = EditorDialogBuilder.create(b->b
		.add(Editors.STRING_ID_HIDDEN.withName("id").forGetter(UserSelection::id))
		.add(Editors.STRING.withName(Components.translatable("gui.wheel_menu.editor.name")).forGetter(UserSelection::message))
		.add(Editors.openDialogLabeled(Editors.EDITOR_SIMPLE_ITEM.<CIcon>xmap(CIcons::getIcon, t -> (t instanceof ItemIcon itc) ? itc.stack : null).withDefault(CIcons::nop), t -> t,t -> Components.empty()).withName(Components.translatable("gui.wheel_menu.editor.icon")).forGetter(UserSelection::icon))
		.add(Editors.openDialog(ACTION_EDITOR).withDefault(()->Selection.NO_ACTION).withName(Component.translatable("gui.wheel_menu.editor.action")).forGetter(UserSelection::selectAction))
		.apply(UserSelection::new));
	public static final Editor<Collection<UserSelection>> USER_SELECTION_LIST = (p, l, v,
		c) -> new EditListDialog<>(p, l, v, null, SELECTION_EDITOR, UserSelection::getParsedMessage, UserSelection::icon, c).open();
	public static final Editor<Collection<ResourceLocation>> SELECTION_ENABLED = 
		EditListDialog.createSetEditor(null, ()->Stream.concat(WheelMenuRenderer.displayedSelections.stream(), WheelMenuRenderer.hiddenSelections.stream()), CFunctionUtils.mapIfMapNullable(WheelMenuRenderer.selections::get, Selection::getMessage, t -> Components.str(t.toString())), CFunctionUtils.mapIfMapNullable(WheelMenuRenderer.selections::get, Selection::getIcon, n -> CIcons.nop()));

		public static class SelectionConfigScreen extends BaseEditDialog{

			public SelectionConfigScreen(UIElement panel) {
				super(panel);
			}

			@Override
			public void onClose() {
				
			}

			@Override
			public void addUIElements() {
				add(EditUtils.getTitle(this, Component.translatable("gui.wheel_menu.editor.edit")));
				add(new OpenEditorButton<>(this, Component.translatable("gui.wheel_menu.editor.enabled"), SELECTION_ENABLED, new ArrayList<>(WheelMenuRenderer.displayedSelections),a->{
					Set<ResourceLocation> aset = new LinkedHashSet<>(a);
					for (ResourceLocation rl : WheelMenuRenderer.displayedSelections) {
						if (!aset.contains(rl))
							WheelMenuRenderer.hiddenSelections.add(rl);
					}
					WheelMenuRenderer.hiddenSelections.removeAll(a);
					WheelMenuRenderer.displayedSelections.clear();
					WheelMenuRenderer.displayedSelections.addAll(a);
					WheelMenuRenderer.saveUserSelectedOptions();
				}));
				add(new OpenEditorButton<>(this, Component.translatable("gui.wheel_menu.editor.useraction"), USER_SELECTION_LIST, new ArrayList<>(WheelMenuRenderer.userSelections),b->{
					Map<String, UserSelection> bset = new HashMap<>();
					for (UserSelection bs : b) {
						bset.put(bs.id(), bs);
					}
					for (UserSelection i : WheelMenuRenderer.userSelections) {
						if (!bset.containsKey(i.id())) {
							WheelMenuRenderer.hiddenSelections.remove(i.userLocation());
							WheelMenuRenderer.displayedSelections.remove(i.userLocation());
							ConfigFileUtil.delete(WheelMenuRenderer.configType, i.id());
						}
					}
					ConfigFileUtil.saveAll(WheelMenuRenderer.configType, bset);
					WheelMenuRenderer.userSelections.clear();
					WheelMenuRenderer.userSelections.addAll(b);
					WheelMenuRenderer.collectSelections();
					WheelMenuRenderer.openIfNewSelection();
				}));
			}
			
		}

	public static void openConfigScreen() {
		WheelMenuRenderer.openIfNewSelection();
		new SelectionConfigScreen(EditUtils.openEditorScreen()).open();
	}
}
