package com.teammoeg.frostedheart.content.wheelmenu;


import java.util.Arrays;
import java.util.Collection;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.editor.EditListDialog;
import com.teammoeg.chorda.client.cui.editor.EditPrompt;
import com.teammoeg.chorda.client.cui.editor.Editor;
import com.teammoeg.chorda.client.cui.editor.EditorDialogBuilder;
import com.teammoeg.chorda.client.cui.editor.EditorSelector;
import com.teammoeg.chorda.client.cui.editor.Editors;
import com.teammoeg.chorda.client.cui.editor.SelectDialog;
import com.teammoeg.chorda.client.cui.editor.SelectStackDialog;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.icon.CIcons.ItemIcon;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.chorda.util.CFunctionHelper;
import com.teammoeg.frostedheart.content.wheelmenu.Selection.CommandInputAction;
import com.teammoeg.frostedheart.content.wheelmenu.Selection.KeyMappingTriggerAction;
import com.teammoeg.frostedheart.content.wheelmenu.Selection.UserSelection;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

public class WheelMenuEditors {
	public static final Editor<KeyMapping> KEY_EDITOR= (p, l, v, c) -> new SelectDialog<KeyMapping>(p, l, v, c,()->Arrays.asList(ClientUtils.mc().options.keyMappings),
			o->Components.translatable(o.getName())
    ).open();
	public static final Editor<KeyMappingTriggerAction> KEY_ACTION_EDITOR=KEY_EDITOR.xmap(CFunctionHelper.mapNullable(KeyMappingTriggerAction::new, null), KeyMappingTriggerAction::getKey);
	
	public static final Editor<CommandInputAction> COMMAND_ACTION_EDITOR=EditPrompt.TEXT_EDITOR.xmap(CommandInputAction::new, CommandInputAction::getCommand);
	public static final Editor<Selection.Action> ACTION_EDITOR= new EditorSelector.EditorSelectorBuilder<Selection.Action>()
			.addEditor(Components.translatable("gui.wheel_menu.editor.key"),KEY_ACTION_EDITOR, t->t instanceof KeyMappingTriggerAction)
			.addEditor(Components.translatable("gui.wheel_menu.editor.command"),COMMAND_ACTION_EDITOR, t->t instanceof CommandInputAction)
			.build();
	public static final Editor<UserSelection> SELECTION_EDITOR=EditorDialogBuilder.builder()
			.add(Editors.STRING.withName(Components.translatable("gui.wheel_menu.editor.name")),UserSelection::message)
			.add(Editors.openDialogLabeled(SelectStackDialog.EDITOR_SIMPLE_ITEM.<CIcon>xmap(CIcons::getIcon, t->(t instanceof ItemIcon itc)?itc.stack:null).withDefault(CIcons::nop),t->t,t->Components.translatable("gui.chorda.editor.select")).withName(Components.translatable("gui.wheel_menu.editor.icon")), UserSelection::icon)
			.add(Editors.openDialog(ACTION_EDITOR).withName(Component.translatable("gui.wheel_menu.editor.action")), UserSelection::selectAction)
			.apply(UserSelection::new);
	public static final Editor<Collection<UserSelection>> SELECTION_LIST_EDITOR=(p,l,v,c)->new EditListDialog<UserSelection>(p,l,v,null,SELECTION_EDITOR,UserSelection::getParsedMessage,UserSelection::icon,c).open();
}
