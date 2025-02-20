package com.teammoeg.frostedheart.content.research.gui.editor;

import java.util.function.Function;

import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.simibubi.create.foundation.utility.Components;
import com.teammoeg.chorda.client.icon.IconEditor;
import com.teammoeg.chorda.util.CFunctionHelper;
import com.teammoeg.frostedheart.content.research.research.ResearchCategory;

import net.minecraft.network.chat.Component;

public class Editors {

	private Editors() {
		
	}
	public static final EditorWidgetFactory<CIcon, LabeledOpenEditorButton<CIcon>> ICON=openDialogLabeled(IconEditor.EDITOR,t->t,t->Components.empty());
	public static final EditorWidgetFactory<Boolean, LabeledSelection<Boolean>> BOOLEAN=EditorWidgetFactory.create(LabeledSelection::createBool, LabeledSelection::getSelection);
	public static final EditorWidgetFactory<Long, NumberBox> LONG=EditorWidgetFactory.create(NumberBox::new, NumberBox::getNum);
	public static final EditorWidgetFactory<Integer, NumberBox> INT=LONG.xmap(CFunctionHelper.mapNullable(Long::intValue, 0), Integer::longValue);
	public static final EditorWidgetFactory<Short, NumberBox> SHORT=LONG.xmap(CFunctionHelper.mapNullable(Long::shortValue, Short.valueOf((short) 0)), Short::longValue);
	public static final EditorWidgetFactory<Byte, NumberBox> BYTE=LONG.xmap(CFunctionHelper.mapNullable(Long::byteValue, Byte.valueOf((byte) 0)), Byte::longValue);
	public static final EditorWidgetFactory<String, LabeledTextBox> STRING=EditorWidgetFactory.create(LabeledTextBox::new, LabeledTextBox::getText);
	public static <T> EditorWidgetFactory<T,OpenEditorButton<T>> openDialog(Editor<T> e,Function<T,CIcon> iconGetter,Function<T,Component> textGetter){
		return EditorWidgetFactory.create((p,l,v)->new OpenEditorButton<>(p,l,e,v,iconGetter,textGetter), OpenEditorButton::getValue);
	}
	public static <T> EditorWidgetFactory<T,LabeledOpenEditorButton<T>> openDialogLabeled(Editor<T> e,Function<T,CIcon> iconGetter,Function<T,Component> textGetter){
		return EditorWidgetFactory.create((p,l,v)->new LabeledOpenEditorButton<T>(p,l,e,v,iconGetter,textGetter), LabeledOpenEditorButton::getValue);
	}
	public static <T extends Enum<T>> EditorWidgetFactory<T,LabeledSelection<T>> enumBox(Class<T> clazz){
		return EditorWidgetFactory.create((p,l,v)->LabeledSelection.createEnum(p, l, clazz, v), LabeledSelection::getSelection);
	}

}
