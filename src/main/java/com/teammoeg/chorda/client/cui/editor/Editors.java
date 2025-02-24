package com.teammoeg.chorda.client.cui.editor;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.simibubi.create.foundation.utility.Components;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TextButton;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.icon.IconEditor;
import com.teammoeg.chorda.util.CFunctionHelper;
import com.teammoeg.frostedheart.content.research.gui.IdBox;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class Editors {

	private Editors() {
		
	}
	public static final EditorWidgetFactory<CIcon, LabeledOpenEditorButton<CIcon>> ICON=openDialogLabeled(IconEditor.EDITOR,t->t,t->Components.empty());
	public static final EditorWidgetFactory<Boolean, LabeledSelection<Boolean>> BOOLEAN=EditorWidgetFactory.create(LabeledSelection::createBool, LabeledSelection::getSelection);
	public static final EditorWidgetFactory<Long, NumberBox> LONG=EditorWidgetFactory.create(NumberBox::new, NumberBox::getNum);
	public static final EditorWidgetFactory<Integer, NumberBox> INT=LONG.xmap(CFunctionHelper.mapNullable(Long::intValue, 0), Integer::longValue);
	public static final EditorWidgetFactory<Short, NumberBox> SHORT=LONG.xmap(CFunctionHelper.mapNullable(Long::shortValue, Short.valueOf((short) 0)), Short::longValue);
	public static final EditorWidgetFactory<Byte, NumberBox> BYTE=LONG.xmap(CFunctionHelper.mapNullable(Long::byteValue, Byte.valueOf((byte) 0)), Byte::longValue);
	public static final EditorWidgetFactory<Double, RealBox> DOUBLE=EditorWidgetFactory.create(RealBox::new, RealBox::getNum);
	public static final EditorWidgetFactory<String, LabeledTextBox> STRING=EditorWidgetFactory.create(LabeledTextBox::new, LabeledTextBox::getText);
	public static final EditorWidgetFactory<String, IdBox> STRING_ID=EditorWidgetFactory.create(IdBox::new, IdBox::getText);
	public static final EditorWidgetFactory<String, HiddenBox<String>> STRING_ID_HIDDEN=hiddenSupplier(()->Long.toHexString(UUID.randomUUID().getMostSignificantBits()));
	public static final EditorWidgetFactory<ResourceLocation, LabeledTextBox> RESOURCELOCATION=EditorWidgetFactory.create(LabeledTextBox::new, LabeledTextBox::getText).flatXmap(ResourceLocation::read,ResourceLocation::toString);
	
	
	public static <T> EditorWidgetFactory<T,OpenEditorButton<T>> openDialog(Editor<T> e,Function<T,CIcon> iconGetter,Function<T,Component> textGetter){
		return EditorWidgetFactory.create((p,l,v)->new OpenEditorButton<>(p,l,e,v,iconGetter,textGetter), OpenEditorButton::getValue);
	}
	public static <T> EditorWidgetFactory<T,OpenEditorButton<T>> openDialog(Editor<T> e){
		return EditorWidgetFactory.create((p,l,v)->new OpenEditorButton<>(p,l,e,v,c->{}), OpenEditorButton::getValue);
	}
	public static <T> EditorWidgetFactory<T,LabeledOpenEditorButton<T>> openDialogLabeled(Editor<T> e,Function<T,CIcon> iconGetter,Function<T,Component> textGetter){
		return EditorWidgetFactory.create((p,l,v)->new LabeledOpenEditorButton<T>(p,l,e,v,iconGetter,textGetter), LabeledOpenEditorButton::getValue);
	}
	public static <T extends Enum<T>> EditorWidgetFactory<T,LabeledSelection<T>> enumBox(Class<T> clazz){
		return EditorWidgetFactory.create((p,l,v)->LabeledSelection.createEnum(p, l, clazz, v), LabeledSelection::getSelection);
	}
	public static <T> EditorWidgetFactory<T,HiddenBox<T>> hiddenSupplier(Supplier<T> def){
		return EditorWidgetFactory.create((p,l,v)->new HiddenBox<>(p,v,def), HiddenBox::getValue);
	}
	public static <T> EditorWidgetFactory<T,TextButton> createAction(CIcon icon,BiConsumer<EditorDialog<T>,T> onClick){
		return EditorWidgetFactory.create((d,p,l,v)->new TextButton(p,l,icon) {
			@Override
			public void onClicked(MouseButton button) {
				onClick.accept(d, v);
			}});
	}
}
