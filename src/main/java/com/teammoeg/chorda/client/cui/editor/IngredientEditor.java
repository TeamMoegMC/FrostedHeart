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

package com.teammoeg.chorda.client.cui.editor;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.cui.editor.EditorSelector.EditorSelectorBuilder;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.chorda.util.CUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Ingredient.ItemValue;
import net.minecraft.world.item.crafting.Ingredient.TagValue;
import net.minecraft.world.item.crafting.Ingredient.Value;
import net.minecraftforge.common.crafting.CompoundIngredient;
import net.minecraftforge.common.crafting.PartialNBTIngredient;
import net.minecraftforge.registries.ForgeRegistries;

public class IngredientEditor extends BaseEditDialog {
    public static final Editor<Ingredient> EDITOR_JSON = (p, l, v, c) -> Editors.JSON_PROMPT.open(p, l, v == null ? null : v.toJson(), e -> c.accept(Ingredient.fromJson(e)));
    public static final Editor<Pair<Ingredient,Integer>> EDITOR = (p, l, v, c) -> new IngredientEditor(p, l, v, c).open();
    public static final Editor<List<Pair<Ingredient,Integer>>> LIST_EDITOR = (p, l, v, c) -> new EditListDialog<>(p, l, v, null, EDITOR, IngredientEditor::getDesc, e -> CIcons.getIcon(e.getFirst(),e.getSecond()), e -> c.accept(new ArrayList<>(e))).open();

    public static final Editor<ItemValue> EDITOR_ITEMLIST = (p, l, v, c) -> Editors.EDITOR_FULL_ITEM.open(p, l, (v == null || v.item == null) ? new ItemStack(Items.AIR) : v.item, s -> {
        s = s.copy();
        s.setCount(1);
        c.accept(new ItemValue(s));
    });
    public static final Editor<TagValue> EDITOR_TAGLIST = (p, l, v, c) -> {

        String vx = "";
        if (v != null) {
            TagKey<Item> tag = v.tag;
            try {
                vx = tag.location().toString();
            } catch (Exception ex) {
            	Chorda.LOGGER.error("Error creating editor tag list", ex);
            }
        }
        Editors.EDITOR_ITEM_TAGS.open(p, l, vx, s -> c.accept(new TagValue(ItemTags.create(new ResourceLocation(s)))));
    };
    public static final Editor<Value> EDITOR_LIST = 
    	new EditorSelectorBuilder<Value>()
    	.addEditor("Tag", EDITOR_TAGLIST,v->v instanceof TagValue)
    	.addEditor("Stack", EDITOR_ITEMLIST,v->v instanceof ItemValue)
    	.build();


    public static final Editor<Collection<Value>> EDITOR_LIST_LIST = (p, l, v, c) -> new EditListDialog<>(p, l, v, EDITOR_LIST, IngredientEditor::getText, c).open();
    public static final Editor<Ingredient> EDITOR_MULTIPLE = (p, l, v, c) -> {
        Collection<Value> list = null;
        if (v != null) list = Arrays.asList(v.values);
        EDITOR_LIST_LIST.open(p, l, list, e -> c.accept(Ingredient.fromValues(e.stream())));
    };
    public static final Editor<Ingredient> EDITOR_SIMPLE = (p, l, v, c) -> {
        Value vx = null;
        if (v != null)
            vx = v.values[0];
        EDITOR_LIST.open(p, l, vx, e -> c.accept(Ingredient.fromValues(Stream.of(e))));
    };
    public static final Editor<Ingredient> VANILLA_EDITOR = 
    	new EditorSelectorBuilder<Ingredient>()
    	.addEditor("Single", EDITOR_SIMPLE,v->v.values.length==1)
    	.addEditor("Multiple", EDITOR_MULTIPLE)
    	.build();
    public static final Editor<Ingredient> NBT_EDITOR = (p, l, v, c) -> {
        if (v == null || v.isEmpty()) {
            Editors.EDITOR_FULL_ITEM.open(p, l, new ItemStack(Items.AIR), e -> c.accept(CUtils.createIngredient(e)));
        } else {
            Editors.EDITOR_FULL_ITEM.open(p, l, v.getItems()[0], e -> c.accept(CUtils.createIngredient(e)));
        }
    };
    public static final Editor<Ingredient> TAG_EDITOR = (p, l, v, c) -> {

        if (v != null && v.values.length > 0) {
        	if(v.values[0] instanceof TagValue) {
        		EDITOR_TAGLIST.open(p, l, (TagValue) v.values[0], e -> c.accept(Ingredient.fromValues(Stream.of(e))));
        		return;
        	}else if(v.values[0] instanceof ItemValue) {
        		EDITOR_TAGLIST.open(p, l, ((ItemValue)v.values[0]).item.getTags().findFirst().map(TagValue::new).orElse(null), e -> c.accept(Ingredient.fromValues(Stream.of(e))));
        	}
        }
            EDITOR_TAGLIST.open(p, l, null, e -> c.accept(Ingredient.fromValues(Stream.of(e))));
    };
    public static final Editor<Ingredient> ITEM_EDITOR = (p, l, v, c) -> {
    	if(v != null && v.values.length > 0) {
	        if ( v.values[0] instanceof ItemValue) {
	        	EDITOR_ITEMLIST.open(p, l, (ItemValue) v.values[0], e -> c.accept(Ingredient.fromValues(Stream.of(e))));
	        	return;
	        } else if(v.values[0] instanceof TagValue) {
	        	EDITOR_ITEMLIST.open(p, l, ForgeRegistries.ITEMS.tags().getTag(((TagValue) v.values[0]).tag).stream().findFirst().map(ItemStack::new).map(ItemValue::new).orElse(null), e -> c.accept(Ingredient.fromValues(Stream.of(e))));
	        	return;
	        }
    	}
        	EDITOR_ITEMLIST.open(p, l, null, e -> c.accept(Ingredient.fromValues(Stream.of(e))));
    };
    public static final Editor<Ingredient> EDITOR_INGREDIENT = 
    	new EditorSelectorBuilder<Ingredient>()
    	.addEditor("ItemStack", NBT_EDITOR,v->v instanceof PartialNBTIngredient)
    	.addEditorWhenEmpty("Tag", TAG_EDITOR)
    	.addEditor("Advanced", VANILLA_EDITOR,v->v.isVanilla())
    	.addEditorWhenEmpty("Json", EDITOR_JSON)
    	.buildEdit();
    public static final Editor<Ingredient> EDITOR_INGREDIENT_EXTERN = new EditorSelectorBuilder<Ingredient>()
    	.addEditor("Edit", EDITOR_INGREDIENT)
    	.addEditor("Change to Multiple",  EDITOR_MULTIPLE,v->v!=null||v.values.length == 1)
    	.addEditor("Change to Tag",  TAG_EDITOR,v->v!=null&&v.values.length == 1&&v.values[0] instanceof TagValue)
    	.addEditor("Change to Item", ITEM_EDITOR,v->v!=null&&v.values.length == 1&&v.values[0] instanceof ItemValue)
    	.addEditor("Change to Single",  EDITOR_SIMPLE,v->v!=null&&v.values.length != 1)
    	.addEditor("Add NBT", NBT_EDITOR,v->v instanceof PartialNBTIngredient)
    	.addEditor("Edit as JSON", EDITOR_JSON)
    	.build();
    
    
    
 

    Component label;

    Consumer<Pair<Ingredient,Integer>> callback;
    int cnt;
    Ingredient orig;
    NumberBox count;

    public IngredientEditor(UIElement panel, Component label, Pair<Ingredient,Integer> i, Consumer<Pair<Ingredient,Integer>> callback) {
        super(panel);
        this.label = label;
        if (i != null) {
            this.cnt = i.getSecond();
            this.orig = i.getFirst();
        } else {
            this.cnt = 1;
        }
        this.callback = callback;
        count = new NumberBox(this, Components.str("Count"), cnt);
    }

    public static Component getDesc(Pair<Ingredient,Integer> w) {
        return getODesc(w.getFirst()).append(" x " + w.getSecond());
    }

    public static MutableComponent getODesc(Ingredient i) {
        if (i instanceof PartialNBTIngredient) {
            if (!i.isEmpty())
                return Components.str("Stack").append(i.getItems()[0].getHoverName());
            return Components.str("NBT empty");
        } else if (i instanceof CompoundIngredient) {
            if (!i.isEmpty())
                return Components.str("Compound ").append(i.getItems()[0].getHoverName());
            return Components.str("Compound empty");
        } else if (i.isVanilla()) {
            if (i.values.length > 0 && !i.isEmpty()) {
                return Components.str( "Item ").append(i.getItems()[0].getHoverName());
            }
            return Components.str("Vanilla empty");
        } else
            return Components.str("Custom Ingredient");

    }

    private static MutableComponent getText(Value li) {
        if (li instanceof TagValue) {
            try {
                return Components.str("Tag:").append(((TagValue) li).tag.location().toString());
            } catch (Exception ex) {
                return Components.str("Unknown tag list");
            }
        } else if (li instanceof ItemValue)
            return Components.str("Item: " ).append(((ItemValue) li).item.getHoverName().getString());
        else
            return Components.str("Unknown item list");
    }

    @Override
    public void addUIElements() {
        add(new OpenEditorButton<>(this, Components.str("Edit Ingredient"), EDITOR_INGREDIENT, orig, orig == null ? CIcons.nop() : CIcons.getIcon(orig), e -> orig = e));
        if (orig != null) {
            if (orig.values.length == 1) {
            	if(!(orig.values[0] instanceof TagValue))
            		add(new OpenEditorButton<>(this, Components.str("Change to Tag"), TAG_EDITOR, orig, e -> orig = e));
               if(!(orig.values[0] instanceof ItemValue))
            	   add(new OpenEditorButton<>(this, Components.str("Change to Item"), ITEM_EDITOR, orig, e -> orig = e));
                add(new OpenEditorButton<>(this, Components.str("Change to Multiple"), EDITOR_MULTIPLE, orig, e -> orig = e));
            }else
                add(new OpenEditorButton<>(this, Components.str("Change to Single"), EDITOR_SIMPLE, orig, e -> orig = e));
            if (!(orig instanceof PartialNBTIngredient))
                add(new OpenEditorButton<>(this, Components.str("Add NBT"), NBT_EDITOR, orig, e -> orig = e));
        }

        add(new OpenEditorButton<>(this, Components.str("Edit as JSON"), EDITOR_JSON, orig, e -> orig = e));
        add(count);
    }

    @Override
    public void onClose() {
        cnt = (int) count.getNum();
        if (orig != null)
            callback.accept(Pair.of(orig, cnt));
    }


}
