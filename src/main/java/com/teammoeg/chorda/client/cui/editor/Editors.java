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

package com.teammoeg.chorda.client.cui.editor;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TextButton;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.chorda.client.icon.IconEditor;
import com.teammoeg.chorda.util.CFunctionUtils;
import com.teammoeg.chorda.util.CRegistryHelper;

import net.minecraft.advancements.Advancement;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

public class Editors {

	public static final Editor<Boolean> CONFIRM_DIALOG = (p, l, v, c) -> new ConfirmDialog(p, l, v, c).open();
	public static final Editor<String> EDITOR_ITEM_TAGS = (p, l, v, c) -> new EditPromptWithSelect(p, l, v, Components.str("Select Tag"), c, Editors.EDITOR_SELECT_ITEM_TAGS).open();
	public static final Editor<String> TEXT_PROMPT = EditPrompt::open;
	public static final Editor<String> COMMAND_PROMPT = (p, l, v, c) ->  new EditPrompt(p,l,v,c,Verifiers.COMMAND).open();
	public static final Editor<JsonElement> JSON_PROMPT = (p, l, v, c) -> EditPrompt.open(p, l, v == null ? "" : v.toString(), e -> c.accept(new JsonParser().parse(e)));
	public static final Editor<Long> LONG_PROMPT = (p, l, v, c) -> EditPrompt.open(p, l, String.valueOf(v), o -> c.accept(Long.parseLong(o)),Verifiers.LONG_STR);
	public static final Editor<Integer> INT_PROMPT = (p, l, v, c) -> EditPrompt.open(p, l, String.valueOf(v), o -> c.accept(Integer.parseInt(o)),Verifiers.INT_STR);
	public static final Editor<Double> REAL_PROMPT = (p, l, v, c) -> EditPrompt.open(p, l, String.valueOf(v), o -> c.accept(Double.parseDouble(o)),Verifiers.NUMBER_STR);
	public static final Editor<Advancement> EDITOR_ADVANCEMENT = (p, l, v, c) -> {
		ClientAdvancements cam = ClientUtils.getMc().player.connection.getAdvancements();
	
		new SelectDialog<>(p, l, v, c, () -> cam.getAdvancements().getAllAdvancements(),
			Advancement::getChatComponent, advx -> new String[] { advx.getChatComponent().getString(), advx.getId().toString() },
			advx -> CIcons.getIcon(advx.getDisplay().getIcon())).open();
	
	};
	public static final Editor<EntityType<?>> EDITOR_ENTITY = (p, l, v, c) -> new SelectDialog<>(p, l, v, c, CRegistryHelper::getEntities, EntityType::getDescription,
	e -> new String[] { e.getDescription().getString(), CRegistryHelper.getRegistryName(e).toString() }).open();
	public static final Editor<String> EDITOR_SELECT_ITEM_TAGS = (p, l, v,
	c) -> new SelectDialog<>(p, l, v, c, () -> ForgeRegistries.ITEMS.tags().getTagNames().map(t -> t.location()).map(ResourceLocation::toString).collect(Collectors.toSet())).open();
	public static final Editor<ItemStack> EDITOR_FULL_ITEM = (p, l, v, c) -> new SelectStackDialog<ItemStack>(p, l, v, c,SelectStackDialog.itemMode,true,SelectStackDialog.ALL_ITEM,SelectStackDialog.INVENTORY,SelectStackDialog.BLOCKS).open();
	public static final Editor<ItemStack> EDITOR_SIMPLE_ITEM = (p, l, v, c) -> new SelectStackDialog<ItemStack>(p, l, v, c,SelectStackDialog.itemMode,false,SelectStackDialog.ALL_ITEM,SelectStackDialog.INVENTORY,SelectStackDialog.BLOCKS).open();

	public static final Editor<Block> EDITOR_BLOCK = (p, l, v, c) -> new SelectStackDialog<ItemStack>(p, Components.empty().append(l).append(" (Blocks only)"), new ItemStack(v), e -> {
	    Block b = Block.byItem(e.getItem());
	    if (b != Blocks.AIR)
	        c.accept(b);
	},SelectStackDialog.itemMode,false,SelectStackDialog.INVENTORY,SelectStackDialog.BLOCKS).open();
	public static final Editor<Collection<ItemStack>> STACK_LIST = (p, l, v, c) -> new EditListDialog<>(p, l, v, new ItemStack(Items.AIR), EDITOR_FULL_ITEM, SelectStackDialog::fromItemStack, CIcons::getIcon, c).open();
	public static final Editor<Collection<Block>> BLOCK_LIST = (p, l, v, c) -> new EditListDialog<>(p, l, v, Blocks.AIR, EDITOR_BLOCK, e -> e.getName(), e -> CIcons.getIcon(e.asItem()), c).open();

	private Editors() {
		
	}
	public static final EditorWidgetFactory<CIcon, LabeledOpenEditorButton<CIcon>> ICON=openDialogLabeled(IconEditor.EDITOR,t->t,t->Components.empty());
	public static final EditorWidgetFactory<Boolean, LabeledSelection<Boolean>> BOOLEAN=EditorWidgetFactory.create(LabeledSelection::createBool, LabeledSelection::getSelection,(w,v)->{w.setSelection(v);});
	public static final EditorWidgetFactory<Long, NumberBox> LONG=EditorWidgetFactory.create(NumberBox::new, NumberBox::getNum,NumberBox::setNum);
	public static final EditorWidgetFactory<Integer, NumberBox> INT=LONG.xmap(CFunctionUtils.mapNullable(Long::intValue, 0), Integer::longValue);
	public static final EditorWidgetFactory<Short, NumberBox> SHORT=LONG.xmap(CFunctionUtils.mapNullable(Long::shortValue, Short.valueOf((short) 0)), Short::longValue);
	public static final EditorWidgetFactory<Byte, NumberBox> BYTE=LONG.xmap(CFunctionUtils.mapNullable(Long::byteValue, Byte.valueOf((byte) 0)), Byte::longValue);
	public static final EditorWidgetFactory<Double, RealBox> DOUBLE=EditorWidgetFactory.create(RealBox::new, RealBox::getNum,RealBox::setNum);
	public static final EditorWidgetFactory<String, LabeledTextBox> STRING=EditorWidgetFactory.create(LabeledTextBox::new, LabeledTextBox::getText,LabeledTextBox::setText);
	public static final EditorWidgetFactory<String, LabeledTextBox> COMMAND=EditorWidgetFactory.create((p,l,v)->new LabeledTextBox(p,l,v,Verifiers.COMMAND), LabeledTextBox::getText,LabeledTextBox::setText);
	
	
	public static final EditorWidgetFactory<String, IdBox> STRING_ID=EditorWidgetFactory.create(IdBox::new, IdBox::getText,IdBox::setText);
	public static final EditorWidgetFactory<String, HiddenBox<String>> STRING_ID_HIDDEN=hiddenSupplier(()->Long.toHexString(UUID.randomUUID().getMostSignificantBits()));
	public static final EditorWidgetFactory<ResourceLocation, LabeledTextBox> RESOURCELOCATION=EditorWidgetFactory.create(LabeledTextBox::new, LabeledTextBox::getText,LabeledTextBox::setText).flatXmap(ResourceLocation::read,ResourceLocation::toString);
	public static final EditorWidgetFactory<Float, RealBox> FLOAT = DOUBLE.xmap(Double::floatValue, Float::doubleValue);
	public static final EditorWidgetFactory<Pair<Ingredient,Integer>,OpenEditorButton<Pair<Ingredient,Integer>>> SIZED_INGREDIENT=openDialog(IngredientEditor.EDITOR,e->CIcons.getIcon(e.getFirst(), e.getSecond()));
	public static final EditorWidgetFactory<ResourceLocation, LabeledOpenEditorButton<Advancement>> ADVANCEMENT=
		openDialogLabeled(EDITOR_ADVANCEMENT,CFunctionUtils.mapIfMapNullable(e->e.getDisplay(), e->CIcons.getIcon(e.getIcon()), e->CIcons.nop()),CFunctionUtils.mapIfMapNullable(e->e.getDisplay(), r->r.getTitle(), e->Components.str(e.getId().toString()))).xmap(e->e.getId(), ClientUtils.getMc().player.connection.getAdvancements().getAdvancements()::get);
	public static final EditorWidgetFactory<Pair<Advancement,String>, AdvancementEditor> ADVANCEMENT_CITERION=EditorWidgetFactory.create(AdvancementEditor::new, AdvancementEditor::getValue,AdvancementEditor::setValue);
	
	
	public static <T> EditorWidgetFactory<T,OpenEditorButton<T>> openDialog(Editor<T> e,Function<T,CIcon> iconGetter,Function<T,Component> textGetter){
		return EditorWidgetFactory.create((p,l,v)->new OpenEditorButton<>(p,l,e,v,iconGetter,textGetter), OpenEditorButton::getValue,(w,v)->{w.setValue(v);});
	}
	public static <T> EditorWidgetFactory<T,OpenEditorButton<T>> openDialog(Editor<T> e,Function<T,CIcon> iconGetter){
		return EditorWidgetFactory.create((p,l,v)->new OpenEditorButton<>(p,l,e,v,iconGetter,a->l), OpenEditorButton::getValue,(w,v)->{w.setValue(v);});
	}
	public static <T> EditorWidgetFactory<T,OpenEditorButton<T>> openDialog(Editor<T> e){
		return EditorWidgetFactory.create((p,l,v)->new OpenEditorButton<>(p,l,e,v,c->{}), OpenEditorButton::getValue,(w,v)->{w.setValue(v);});
	}
	public static <T> EditorWidgetFactory<T,LabeledOpenEditorButton<T>> openDialogLabeled(Editor<T> e,Function<T,CIcon> iconGetter,Function<T,Component> textGetter){
		return EditorWidgetFactory.create((p,l,v)->new LabeledOpenEditorButton<T>(p,l,e,v,iconGetter,textGetter), LabeledOpenEditorButton::getValue,(w,v)->{w.setValue(v);});
	}
	public static <T extends Enum<T>> EditorWidgetFactory<T,LabeledSelection<T>> enumBox(Class<T> clazz){
		return EditorWidgetFactory.create((p,l,v)->LabeledSelection.createEnum(p, l, clazz, v), LabeledSelection::getSelection,(w,v)->{w.setSelection(v);});
	}
	public static <T> EditorWidgetFactory<T,LabeledSelection<T>> selectBox(List<T> selections){
		return EditorWidgetFactory.create((p,l,v)->new LabeledSelection<>(p, l, v, selections,a->Components.str(String.valueOf(a)),a->CIcons.nop()), LabeledSelection::getSelection,(w,v)->{w.setSelection(v);});
	}
	public static <T extends Enum<T>> EditorWidgetFactory<T,LabeledSelection<T>> enumBox(Class<T> clazz,Function<T,Component> textGetter,Function<T,CIcon> iconGetter){
		return EditorWidgetFactory.create((p,l,v)->LabeledSelection.createEnum(p, l, clazz, v,textGetter,iconGetter), LabeledSelection::getSelection,(w,v)->{w.setSelection(v);});
	}
	public static <T> EditorWidgetFactory<T,HiddenBox<T>> hiddenSupplier(Supplier<T> def){
		return EditorWidgetFactory.create((p,l,v)->new HiddenBox<>(p,v,def), HiddenBox::getValue,(w,v)->{w.setValue(v);});
	}
	public static <T> EditorWidgetFactory<T,TextButton> createAction(CIcon icon,BiConsumer<EditorDialog<T>,T> onClick){
		return EditorWidgetFactory.create((d,p,l,v)->new TextButton(p,l,icon) {
			@Override
			public void onClicked(MouseButton button) {
				onClick.accept(d, v);
			}});
	}
}
