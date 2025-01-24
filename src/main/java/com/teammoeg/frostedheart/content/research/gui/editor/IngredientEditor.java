/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.research.gui.editor;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;

import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.research.gui.FHIcons;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Widget;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class IngredientEditor extends BaseEditDialog {
    public static final Editor<Ingredient> EDITOR_JSON = (p, l, v, c) -> EditPrompt.JSON_EDITOR.open(p, l, v == null ? null : v.toJson(), e -> c.accept(Ingredient.fromJson(e)));
    public static final Editor<IngredientWithSize> EDITOR = (p, l, v, c) -> new IngredientEditor(p, l, v, c).open();
    public static final Editor<List<IngredientWithSize>> LIST_EDITOR = (p, l, v, c) -> new EditListDialog<>(p, l, v, null, EDITOR, IngredientEditor::getDesc, e -> FHIcons.getIcon(e).asFtbIcon(), e -> c.accept(new ArrayList<>(e))).open();

    public static final Editor<ItemValue> EDITOR_ITEMLIST = (p, l, v, c) -> SelectItemStackDialog.EDITOR.open(p, l, (v == null || v.item == null) ? new ItemStack(Items.AIR) : v.item, s -> {
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
                FHMain.LOGGER.error("Error creating editor tag list", ex);
            }
        }
        EditBtnDialog.EDITOR_ITEM_TAGS.open(p, l, vx, s -> c.accept(new TagValue(ItemTags.create(new ResourceLocation(s)))));
    };
    public static final Editor<Value> EDITOR_LIST = (p, l, v, c) -> {
        if (v == null)
            new EditorSelector<>(p, l, c).addEditor("Tag", EDITOR_TAGLIST).addEditor("Stack", EDITOR_ITEMLIST).open();
        else if (v instanceof TagValue)
            EDITOR_TAGLIST.open(p, l, (TagValue) v, c::accept);
        else if (v instanceof ItemValue)
            EDITOR_ITEMLIST.open(p, l, (ItemValue) v, c::accept);
    };


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
    public static final Editor<Ingredient> VANILLA_EDITOR_CHOICE = (p, l, v, c) -> new EditorSelector<>(p, l, (t, s) -> true, v, c).addEditor("Single", EDITOR_SIMPLE).addEditor("Multiple", EDITOR_MULTIPLE).open();
    public static final Editor<Ingredient> VANILLA_EDITOR = (p, l, v, c) -> {
        if (v == null || v.values.length == 0) {
            VANILLA_EDITOR_CHOICE.open(p, l, null, c);
        } else if (v.values.length == 1) {
            EDITOR_SIMPLE.open(p, l, v, c);
        } else {
            EDITOR_MULTIPLE.open(p, l, v, c);
        }
    };
    public static final Editor<Ingredient> NBT_EDITOR = (p, l, v, c) -> {
        if (v == null || v.isEmpty()) {
            SelectItemStackDialog.EDITOR.open(p, l, new ItemStack(Items.AIR), e -> c.accept(CUtils.createIngredient(e)));
        } else {
            SelectItemStackDialog.EDITOR.open(p, l, v.getItems()[0], e -> c.accept(CUtils.createIngredient(e)));
        }
    };
    public static final Editor<Ingredient> TAG_EDITOR = (p, l, v, c) -> {

        if (v != null && v.values.length > 0 && v.values[0] instanceof TagValue)
            EDITOR_TAGLIST.open(p, l, (TagValue) v.values[0], e -> c.accept(Ingredient.fromValues(Stream.of(e))));
        else
            EDITOR_TAGLIST.open(p, l, null, e -> c.accept(Ingredient.fromValues(Stream.of(e))));
    };
    public static final Editor<Ingredient> EDITOR_INGREDIENT = (p, l, v, c) -> {
        if (v == null) {
            new EditorSelector<>(p, l, c).addEditor("ItemStack", NBT_EDITOR).addEditor("TAG", TAG_EDITOR).addEditor("Advanced", VANILLA_EDITOR).open();
        } else if (v instanceof PartialNBTIngredient) {
            NBT_EDITOR.open(p, l, v, c);
        } else if (v.isVanilla()) {
            VANILLA_EDITOR.open(p, l, v, c);
        } else
            EDITOR_JSON.open(p, l, v, c);
    };
    public static final Editor<Ingredient> EDITOR_INGREDIENT_EXTERN = (p, l, v, c) -> {
        EditorSelector<Ingredient> igd = new EditorSelector<>(p, l, (o, t) -> true, v, c);
        igd.addEditor("Edit", EDITOR_INGREDIENT);
        if (v != null) {
            if (v.values.length == 1)
                igd.addEditor("Change to Multiple", EDITOR_MULTIPLE);
            else
                igd.addEditor("Change to Single", EDITOR_SIMPLE);
            if (!(v instanceof PartialNBTIngredient))
                igd.addEditor("Add NBT", NBT_EDITOR);
        }
        igd.addEditor("Edit as JSON", EDITOR_JSON);
        igd.open();
    };

    String label;

    Consumer<IngredientWithSize> callback;
    int cnt;
    Ingredient orig;
    NumberBox count;

    public IngredientEditor(Widget panel, String label, IngredientWithSize i, Consumer<IngredientWithSize> callback) {
        super(panel);
        this.label = label;
        if (i != null) {
            this.cnt = i.getCount();
            this.orig = i.getBaseIngredient();
        } else {
            this.cnt = 1;
        }
        this.callback = callback;
        count = new NumberBox(this, "Count", cnt);
    }

    public static String getDesc(IngredientWithSize w) {
        return getODesc(w.getBaseIngredient()) + " x " + w.getCount();
    }

    public static String getODesc(Ingredient i) {
        if (i instanceof PartialNBTIngredient) {
            if (!i.isEmpty())
                return "Stack " + i.getItems()[0].getHoverName().getString();
            return "NBT empty";
        } else if (i instanceof CompoundIngredient) {
            if (!i.isEmpty())
                return "Compound " + i.getItems()[0].getHoverName().getString();
            return "Compound empty";
        } else if (i.isVanilla()) {
            if (i.values.length > 0 && !i.isEmpty()) {
                return "Item " + i.getItems()[0].getHoverName().getString();
            }
            return "Vanilla empty";
        } else
            return "Custom Ingredient";

    }

    private static String getText(Value li) {
        if (li instanceof TagValue) {
            try {
                return "Tag:" + ((TagValue) li).tag.location();
            } catch (Exception ex) {
                return "Unknown tag list";
            }
        } else if (li instanceof ItemValue)
            return "Item: " + ((ItemValue) li).item.getHoverName().getString();
        else
            return "Unknown item list";
    }

    @Override
    public void addWidgets() {
        add(new OpenEditorButton<>(this, "Edit Ingredient", EDITOR_INGREDIENT, orig, orig == null ? Icon.empty() : FHIcons.getIcon(orig).asFtbIcon(), e -> orig = e));
        if (orig != null) {
            if (orig.values.length == 1)
                add(new OpenEditorButton<>(this, "Change to Multiple", EDITOR_MULTIPLE, orig, e -> orig = e));
            else
                add(new OpenEditorButton<>(this, "Change to Single", EDITOR_SIMPLE, orig, e -> orig = e));
            if (!(orig instanceof PartialNBTIngredient))
                add(new OpenEditorButton<>(this, "Add NBT", NBT_EDITOR, orig, e -> orig = e));
        }

        add(new OpenEditorButton<>(this, "Edit as JSON", EDITOR_JSON, orig, e -> orig = e));
        add(count);
    }

    @Override
    public void onClose() {
        cnt = (int) count.getNum();
        if (orig != null)
            callback.accept(new IngredientWithSize(orig, cnt));
    }


}
